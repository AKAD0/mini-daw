#include "latency_probe.h"

#include <oboe/Oboe.h>
#include <oboe/OboeExtensions.h>
#include <time.h>

#include <algorithm>
#include <array>
#include <chrono>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <span>
#include <string>
#include <thread>
#include <vector>

#include "json_text.h"
#include "minidaw/engine/take_place.h"

namespace minidaw::engine::glue {
namespace {

using Clock = std::chrono::steady_clock;
using StreamPtr = std::shared_ptr<oboe::AudioStream>;

constexpr int32_t kRate = 48000;
constexpr size_t kClicks = 8;
constexpr int64_t kFirstClick = kRate;     // 1 s after the first callback
constexpr int64_t kClickDistance = kRate;  // one click per second
constexpr int64_t kSearch = kRate / 2;     // the click must arrive within 0.5 s
constexpr int64_t kCapacity = kFirstClick + static_cast<int64_t>(kClicks + 1) * kClickDistance;
constexpr int32_t kChunk = 1024;
constexpr int32_t kMaxReads = 8;
constexpr auto kRunTime = std::chrono::milliseconds(11000);
constexpr auto kPoll = std::chrono::milliseconds(20);
constexpr size_t kMaxPairs = 1024;
constexpr double kMillisPerSecond = 1000.0;
constexpr double kClarityLimit = 5.0;
constexpr double kDifferenceLimitMillis = 10.0;

// Plays the clicks and reads the input in the same callback. Each recorded frame keeps its position
// in the input stream: the position before a read is the position of the first frame it returns.
class Duplex : public oboe::AudioStreamDataCallback {
 public:
  Duplex(std::span<const float> click, oboe::AudioStream* input)
      : click_(click),
        input_(input),
        channels_(input->getChannelCount()),
        chunk_(static_cast<size_t>(kChunk) * static_cast<size_t>(channels_)),
        recorded_(static_cast<size_t>(kCapacity), 0.0F) {
    clickFrames_.fill(-1);
    inputAtClick_.fill(-1);
  }

  oboe::DataCallbackResult onAudioReady(oboe::AudioStream* output, void* audioData,
                                        int32_t numFrames) override {
    readInput();
    const int64_t start = output->getFramesWritten();
    if (origin_ < 0) {
      origin_ = start;
    }
    auto* out = static_cast<float*>(audioData);
    const int32_t channels = output->getChannelCount();
    for (int32_t i = 0; i < numFrames; ++i) {
      const float value = valueAt(start + i);
      for (int32_t c = 0; c < channels; ++c) {
        out[static_cast<size_t>(i) * channels + c] = value;
      }
    }
    for (size_t k = 0; k < kClicks; ++k) {
      const int64_t frame = origin_ + kFirstClick + static_cast<int64_t>(k) * kClickDistance;
      if (frame >= start && frame < start + numFrames) {
        clickFrames_[k] = frame;
        inputAtClick_[k] = inputNext_;
      }
    }
    return oboe::DataCallbackResult::Continue;
  }

  [[nodiscard]] std::span<const float> recorded() const { return recorded_; }
  [[nodiscard]] int64_t inputOrigin() const { return inputOrigin_; }
  [[nodiscard]] int64_t clickFrame(size_t k) const { return clickFrames_[k]; }
  [[nodiscard]] int64_t inputAtClick(size_t k) const { return inputAtClick_[k]; }

 private:
  float valueAt(int64_t frame) const {
    const int64_t offset = frame - origin_ - kFirstClick;
    if (offset < 0 || offset >= static_cast<int64_t>(kClicks) * kClickDistance) {
      return 0.0F;
    }
    const auto inClick = static_cast<size_t>(offset % kClickDistance);
    if (inClick >= click_.size()) {
      return 0.0F;
    }
    return click_[inClick];
  }

  void readInput() {
    for (int32_t round = 0; round < kMaxReads; ++round) {
      const int64_t position = input_->getFramesRead();
      const auto read = input_->read(chunk_.data(), kChunk, 0);
      if (!read || read.value() <= 0) {
        return;
      }
      if (inputOrigin_ < 0) {
        inputOrigin_ = position;
      }
      for (int32_t j = 0; j < read.value(); ++j) {
        const int64_t index = position + j - inputOrigin_;
        if (index >= 0 && index < kCapacity) {
          recorded_[static_cast<size_t>(index)] = chunk_[static_cast<size_t>(j) * channels_];
        }
      }
      inputNext_ = position + read.value();
      if (read.value() < kChunk) {
        return;
      }
    }
  }

  std::span<const float> click_;
  oboe::AudioStream* input_;
  int32_t channels_;
  std::vector<float> chunk_;
  std::vector<float> recorded_;
  int64_t origin_ = -1;
  int64_t inputOrigin_ = -1;
  int64_t inputNext_ = -1;
  std::array<int64_t, kClicks> clickFrames_{};
  std::array<int64_t, kClicks> inputAtClick_{};
};

void lowLatency(oboe::AudioStreamBuilder& builder, oboe::Direction direction) {
  builder.setDirection(direction);
  builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
  builder.setSharingMode(oboe::SharingMode::Exclusive);
  builder.setFormat(oboe::AudioFormat::Float);
  builder.setSampleRate(kRate);
}

void describe(Json& json, const char* prefix, oboe::AudioStream& stream) {
  const std::string p(prefix);
  json.number((p + "SampleRate").c_str(), stream.getSampleRate());
  json.number((p + "FramesPerBurst").c_str(), stream.getFramesPerBurst());
  json.number((p + "DeviceId").c_str(), stream.getDeviceId());
  json.text((p + "Mmap").c_str(), oboe::OboeExtensions::isMMapUsed(&stream) ? "yes" : "no");
  const auto xruns = stream.getXRunCount();
  json.number((p + "Xruns").c_str(), xruns ? xruns.value() : -1);
}

void poll(oboe::AudioStream& stream, std::vector<FramePair>& pairs) {
  const auto stamp = stream.getTimestamp(CLOCK_MONOTONIC);
  if (stamp && pairs.size() < kMaxPairs) {
    pairs.push_back({stamp.value().position, stamp.value().timestamp});
  }
}

double median(std::vector<double> values) {
  if (values.empty()) {
    return NAN;
  }
  const auto middle = values.begin() + static_cast<std::ptrdiff_t>(values.size() / 2);
  std::nth_element(values.begin(), middle, values.end());
  return *middle;
}

// Adds the result of each click and the summary.
void evaluate(Json& json, const Duplex& duplex, std::span<const float> click,
              const std::vector<FramePair>& outputPairs, const std::vector<FramePair>& inputPairs,
              double outputRate, double inputRate) {
  std::vector<double> byTimestamps;
  std::vector<double> byLoopback;
  double worst = 0.0;
  const bool stamps = !outputPairs.empty() && !inputPairs.empty();
  json.text("timestamps", stamps ? "available" : "unavailable");
  for (size_t k = 0; k < kClicks; ++k) {
    const std::string name = "click" + std::to_string(k + 1);
    const int64_t written = duplex.inputAtClick(k);
    if (duplex.clickFrame(k) < 0 || written < 0 || duplex.inputOrigin() < 0) {
      json.text(name.c_str(), "not played");
      continue;
    }
    const int64_t from = written - duplex.inputOrigin();
    const ClickPlace place = findClick(duplex.recorded(), click, from, kSearch);
    json.decimal((name + ".clarity").c_str(), place.clarity);
    const bool found = place.start >= 0 && place.clarity >= kClarityLimit;
    double loopback = NAN;
    if (found) {
      const auto frames = static_cast<double>(place.start + duplex.inputOrigin() - written);
      loopback = frames / inputRate * kMillisPerSecond;
    }
    json.decimal((name + ".loopbackMs").c_str(), loopback);
    double timestamp = NAN;
    if (stamps) {
      const int64_t frame = duplex.clickFrame(k);
      const double heard = timeOfFrame(nearestByFrame(outputPairs, frame), frame, outputRate);
      const double captured = frameAtTime(nearestByTime(inputPairs, heard), heard, inputRate);
      timestamp = (captured - static_cast<double>(written)) / inputRate * kMillisPerSecond;
    }
    json.decimal((name + ".timestampMs").c_str(), timestamp);
    if (found && stamps) {
      byTimestamps.push_back(timestamp);
      byLoopback.push_back(loopback);
      worst = std::max(worst, std::abs(timestamp - loopback));
    }
  }
  json.number("clicksCompared", static_cast<int64_t>(byLoopback.size()));
  json.decimal("medianTimestampMs", median(byTimestamps));
  json.decimal("medianLoopbackMs", median(byLoopback));
  json.decimal("largestDifferenceMs", byLoopback.empty() ? NAN : worst);
  const bool within = !byLoopback.empty() && worst <= kDifferenceLimitMillis;
  json.text("differenceWithin10ms", within ? "yes" : "no");
}

}  // namespace

std::string probeTakeLatency(const std::vector<float>& click) {
  Json json;
  oboe::AudioStreamBuilder inputBuilder;
  lowLatency(inputBuilder, oboe::Direction::Input);
  inputBuilder.setChannelCount(oboe::ChannelCount::Mono);
  inputBuilder.setInputPreset(oboe::InputPreset::Unprocessed);
  StreamPtr input;
  const oboe::Result inputOpened = inputBuilder.openStream(input);
  json.text("inputOpen", oboe::convertToText(inputOpened));
  if (inputOpened != oboe::Result::OK) {
    return json.str();
  }
  Duplex duplex(click, input.get());
  oboe::AudioStreamBuilder outputBuilder;
  lowLatency(outputBuilder, oboe::Direction::Output);
  outputBuilder.setChannelCount(oboe::ChannelCount::Stereo);
  outputBuilder.setDataCallback(&duplex);
  StreamPtr output;
  const oboe::Result outputOpened = outputBuilder.openStream(output);
  json.text("outputOpen", oboe::convertToText(outputOpened));
  if (outputOpened != oboe::Result::OK) {
    input->close();
    return json.str();
  }
  std::vector<FramePair> outputPairs;
  std::vector<FramePair> inputPairs;
  outputPairs.reserve(kMaxPairs);
  inputPairs.reserve(kMaxPairs);
  input->requestStart();
  output->requestStart();
  const auto end = Clock::now() + kRunTime;
  while (Clock::now() < end) {
    std::this_thread::sleep_for(kPoll);
    poll(*output, outputPairs);
    poll(*input, inputPairs);
  }
  output->requestStop();
  input->requestStop();
  describe(json, "output", *output);
  describe(json, "input", *input);
  output->close();
  input->close();
  evaluate(json, duplex, click, outputPairs, inputPairs, output->getSampleRate(),
           input->getSampleRate());
  return json.str();
}

}  // namespace minidaw::engine::glue
