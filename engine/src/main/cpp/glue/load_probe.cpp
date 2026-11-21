#include "load_probe.h"

#include <oboe/Oboe.h>
#include <oboe/OboeExtensions.h>

#include <algorithm>
#include <chrono>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <memory>
#include <span>
#include <string>
#include <thread>
#include <vector>

#include "json_text.h"
#include "minidaw/engine/kernels.h"
#include "minidaw/engine/load_bench.h"
#include "minidaw/engine/snapshot.h"

namespace minidaw::engine::glue {
namespace {

using Clock = std::chrono::steady_clock;
using StreamPtr = std::shared_ptr<oboe::AudioStream>;

constexpr int32_t kRate = 48000;
constexpr int32_t kBurstsInBuffer = 2;
constexpr size_t kScratchFrames = 8192;
constexpr size_t kMaxCallbacks = 200000;
constexpr size_t kWarmUpCallbacks = 50;
constexpr double kPercent = 100.0;
constexpr double kHighShare = 0.99;
constexpr int kKernelBlocks = 20000;
constexpr double kMicrosPerSecond = 1e6;

// Renders the test Project in each callback and notes its load. The output stays silent.
class LoadCallback : public oboe::AudioStreamDataCallback {
 public:
  LoadCallback(const TestCounts& counts, const std::vector<float>& sample)
      : bench_(counts, sample),
        left_(kScratchFrames),
        right_(kScratchFrames),
        loads_(kMaxCallbacks) {}

  oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData,
                                        int32_t numFrames) override {
    const auto start = Clock::now();
    for (int32_t done = 0; done < numFrames;) {
      const auto count = std::min<int32_t>(numFrames - done, static_cast<int32_t>(kScratchFrames));
      bench_.render(left_.data(), right_.data(), count);
      done += count;
    }
    const auto bytes = static_cast<size_t>(numFrames) * stream->getBytesPerFrame();
    std::memset(audioData, 0, bytes);
    const std::chrono::duration<double> spent = Clock::now() - start;
    if (count_ < loads_.size()) {
      loads_[count_++] = spent.count() * stream->getSampleRate() / numFrames;
    }
    return oboe::DataCallbackResult::Continue;
  }

  [[nodiscard]] std::vector<double> loads() const {
    const size_t from = std::min(kWarmUpCallbacks, count_);
    const auto first = loads_.begin() + static_cast<std::ptrdiff_t>(from);
    const auto last = loads_.begin() + static_cast<std::ptrdiff_t>(count_);
    return std::vector<double>(first, last);
  }

  [[nodiscard]] const LoadBench& bench() const { return bench_; }

 private:
  LoadBench bench_;
  std::vector<float> left_;
  std::vector<float> right_;
  std::vector<double> loads_;
  size_t count_ = 0;
};

double medianOf(std::vector<double> values) {
  if (values.empty()) {
    return -1.0;
  }
  const auto middle = values.begin() + static_cast<std::ptrdiff_t>(values.size() / 2);
  std::nth_element(values.begin(), middle, values.end());
  return *middle;
}

}  // namespace

std::string probeLoad(int32_t project, int32_t seconds, const std::vector<float>& sample) {
  const TestCounts counts = project == 0 ? mvpCounts() : version10Counts();
  LoadCallback callback(counts, sample);
  Json json;
  json.text("project", project == 0 ? "MVP" : "version 1.0");
  json.number("voices", static_cast<int64_t>(callback.bench().voices()));
  json.number("streamers", static_cast<int64_t>(callback.bench().streamers()));
  json.number("effects", static_cast<int64_t>(callback.bench().effects()));
  oboe::AudioStreamBuilder builder;
  builder.setDirection(oboe::Direction::Output);
  builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
  builder.setSharingMode(oboe::SharingMode::Exclusive);
  builder.setFormat(oboe::AudioFormat::Float);
  builder.setChannelCount(oboe::ChannelCount::Stereo);
  builder.setSampleRate(kRate);
  builder.setDataCallback(&callback);
  StreamPtr stream;
  const oboe::Result opened = builder.openStream(stream);
  json.text("open", oboe::convertToText(opened));
  if (opened != oboe::Result::OK) {
    return json.str();
  }
  const auto size = stream->setBufferSizeInFrames(kBurstsInBuffer * stream->getFramesPerBurst());
  stream->requestStart();
  std::this_thread::sleep_for(std::chrono::seconds(seconds));
  stream->requestStop();
  const auto xruns = stream->getXRunCount();
  json.number("sampleRate", stream->getSampleRate());
  json.number("framesPerBurst", stream->getFramesPerBurst());
  json.number("bufferSizeInFrames", size ? size.value() : -1);
  json.text("performanceMode", oboe::convertToText(stream->getPerformanceMode()));
  json.text("mmap", oboe::OboeExtensions::isMMapUsed(stream.get()) ? "yes" : "no");
  json.number("underruns", xruns ? xruns.value() : -1);
  stream->close();
  std::vector<double> loads = callback.loads();
  json.number("callbacks", static_cast<int64_t>(loads.size()));
  if (loads.empty()) {
    return json.str();
  }
  double sum = 0.0;
  for (const double load : loads) {
    sum += load;
  }
  std::ranges::sort(loads);
  const auto high = static_cast<size_t>(kHighShare * static_cast<double>(loads.size() - 1));
  json.decimal("meanLoadPercent", kPercent * sum / static_cast<double>(loads.size()));
  json.decimal("load99Percent", kPercent * loads[high]);
  json.decimal("maxLoadPercent", kPercent * loads.back());
  return json.str();
}

std::string probeKernels(const std::vector<float>& sample) {
  Json json;
  json.number("blocks", kKernelBlocks);
  for (const KernelCost& cost : measureKernels(sample, kKernelBlocks)) {
    json.decimal((cost.name + "Micros").c_str(), cost.microsPerBlock);
  }
  const double blockMicros = LoadBench::kBlock * kMicrosPerSecond / kRate;
  json.decimal("blockMicros", blockMicros);
  return json.str();
}

std::string probeSnapshotLoad(std::span<const uint8_t> bytes, int32_t runs) {
  Json json;
  json.number("bytes", static_cast<int64_t>(bytes.size()));
  const uint64_t native = snapshot::fingerprint(snapshot::writeStandard(version10Counts()));
  json.text("sameBytesAsEngineCore", snapshot::fingerprint(bytes) == native ? "yes" : "no");
  std::vector<double> times;
  snapshot::Error error = snapshot::Error::kNone;
  for (int32_t i = 0; i < runs; ++i) {
    snapshot::Loaded loaded;
    const auto start = Clock::now();
    error = snapshot::load(bytes, loaded);
    const std::chrono::duration<double> spent = Clock::now() - start;
    times.push_back(spent.count() * kMicrosPerSecond);
  }
  json.text("result", snapshot::errorText(error));
  json.decimal("medianLoadMicros", medianOf(times));
  std::vector<double> writes;
  for (int32_t i = 0; i < runs; ++i) {
    const auto start = Clock::now();
    const std::vector<uint8_t> written = snapshot::writeStandard(version10Counts());
    const std::chrono::duration<double> spent = Clock::now() - start;
    writes.push_back(written.empty() ? -1.0 : spent.count() * kMicrosPerSecond);
  }
  json.decimal("medianNativeWriteMicros", medianOf(writes));
  return json.str();
}

}  // namespace minidaw::engine::glue
