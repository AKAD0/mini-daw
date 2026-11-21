#include "audio_path_probe.h"

#include <oboe/Oboe.h>
#include <oboe/OboeExtensions.h>

#include <atomic>
#include <chrono>
#include <cstring>
#include <memory>
#include <string>
#include <thread>

#include "json_text.h"

namespace minidaw::engine::glue {
namespace {

using Clock = std::chrono::steady_clock;
using StreamPtr = std::shared_ptr<oboe::AudioStream>;

constexpr int32_t kRate = 48000;
constexpr int32_t kBurstsInBuffer = 2;
constexpr auto kFirstCallbackLimit = std::chrono::seconds(2);
constexpr auto kPoll = std::chrono::milliseconds(1);
constexpr double kMicrosPerMilli = 1000.0;

int64_t microsSince(Clock::time_point start) {
  return std::chrono::duration_cast<std::chrono::microseconds>(Clock::now() - start).count();
}

// Writes silence and notes the first call.
class Probe : public oboe::AudioStreamDataCallback {
 public:
  oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData,
                                        int32_t numFrames) override {
    if (stream->getDirection() == oboe::Direction::Output) {
      const auto bytes = static_cast<size_t>(numFrames) * stream->getBytesPerFrame();
      std::memset(audioData, 0, bytes);
    }
    called_.store(true, std::memory_order_release);
    return oboe::DataCallbackResult::Continue;
  }

  [[nodiscard]] bool called() const { return called_.load(std::memory_order_acquire); }

 private:
  std::atomic<bool> called_{false};
};

// Opens and starts the stream, waits for the first callback, and adds the facts of the stream.
StreamPtr openAndStart(oboe::AudioStreamBuilder& builder, const Probe& probe, bool performanceHint,
                       Json& json) {
  StreamPtr stream;
  const auto opening = Clock::now();
  const oboe::Result opened = builder.openStream(stream);
  json.number("openMicros", microsSince(opening));
  json.text("open", oboe::convertToText(opened));
  if (opened != oboe::Result::OK) {
    return nullptr;
  }
  stream->setPerformanceHintEnabled(performanceHint);
  const auto starting = Clock::now();
  stream->requestStart();
  while (!probe.called() && Clock::now() - starting < kFirstCallbackLimit) {
    std::this_thread::sleep_for(kPoll);
  }
  json.number("firstCallbackMicros", probe.called() ? microsSince(starting) : -1);
  json.number("sampleRate", stream->getSampleRate());
  json.number("framesPerBurst", stream->getFramesPerBurst());
  json.number("bufferCapacityInFrames", stream->getBufferCapacityInFrames());
  json.text("performanceMode", oboe::convertToText(stream->getPerformanceMode()));
  json.text("sharingMode", oboe::convertToText(stream->getSharingMode()));
  json.text("audioApi", oboe::convertToText(stream->getAudioApi()));
  json.text("mmap", oboe::OboeExtensions::isMMapUsed(stream.get()) ? "yes" : "no");
  return stream;
}

// The settings that the engine will ask for (D-035).
void common(oboe::AudioStreamBuilder& builder, oboe::Direction direction, Probe& probe) {
  builder.setDirection(direction);
  builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
  builder.setSharingMode(oboe::SharingMode::Exclusive);
  builder.setFormat(oboe::AudioFormat::Float);
  builder.setSampleRate(kRate);
  builder.setDataCallback(&probe);
}

}  // namespace

std::string probeOutput(bool performanceHint, int32_t seconds) {
  Probe probe;
  oboe::AudioStreamBuilder builder;
  common(builder, oboe::Direction::Output, probe);
  builder.setChannelCount(oboe::ChannelCount::Stereo);
  Json json;
  json.text("performanceHint", performanceHint ? "on" : "off");
  json.number("seconds", seconds);
  const auto stream = openAndStart(builder, probe, performanceHint, json);
  if (!stream) {
    return json.str();
  }
  const auto size = stream->setBufferSizeInFrames(kBurstsInBuffer * stream->getFramesPerBurst());
  json.number("bufferSizeInFrames", size ? size.value() : -1);
  std::this_thread::sleep_for(std::chrono::seconds(seconds));
  const auto xruns = stream->getXRunCount();
  json.number("underruns", xruns ? xruns.value() : -1);
  const auto latency = stream->calculateLatencyMillis();
  const double micros = latency ? latency.value() * kMicrosPerMilli : -1.0;
  json.number("latencyMicros", static_cast<int64_t>(micros));
  stream->requestStop();
  stream->close();
  return json.str();
}

std::string probeInput() {
  Probe probe;
  oboe::AudioStreamBuilder builder;
  common(builder, oboe::Direction::Input, probe);
  builder.setChannelCount(oboe::ChannelCount::Mono);
  builder.setInputPreset(oboe::InputPreset::Unprocessed);
  Json json;
  json.text("inputPreset", oboe::convertToText(oboe::InputPreset::Unprocessed));
  const auto stream = openAndStart(builder, probe, false, json);
  if (stream) {
    stream->requestStop();
    stream->close();
  }
  return json.str();
}

}  // namespace minidaw::engine::glue
