#include "minidaw/engine/load_bench.h"

#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstddef>
#include <string>
#include <vector>

#include "minidaw/engine/kernels.h"

namespace minidaw::engine {
namespace {

using Clock = std::chrono::steady_clock;

constexpr int kKindEqualizer = 1;
constexpr int kKindDelay = 2;
constexpr int kKindReverb = 3;
// The Audio clips play a stereo file of 2 s in a loop.
constexpr size_t kAudioFrames = 96000;
constexpr double kSemitones = 12.0;
constexpr int kPitchStep = 3;
constexpr int kPitchLow = -6;
constexpr float kChannelGain = 0.5F;
constexpr double kMicrosPerSecond = 1e6;

// A long stereo Audio clip made of the Sample, repeated.
std::vector<float> repeated(const std::vector<float>& sample, size_t frames) {
  std::vector<float> out(frames, 0.0F);
  if (sample.empty()) {
    return out;
  }
  for (size_t i = 0; i < frames; ++i) {
    out[i] = sample[i % sample.size()];
  }
  return out;
}

}  // namespace

TestCounts mvpCounts() { return {}; }

TestCounts version10Counts() {
  TestCounts counts;
  counts.audioTracks *= 2;
  counts.patternTracks *= 2;
  counts.channels *= 2;
  counts.equalizers *= 2;
  counts.delays *= 2;
  counts.reverbs *= 2;
  return counts;
}

LoadBench::LoadBench(const TestCounts& counts, const std::vector<float>& sample)
    : sample_(sample, sample),
      audioLeft_(repeated(sample, kAudioFrames)),
      audioRight_(repeated(sample, kAudioFrames)),
      channels_(static_cast<size_t>(std::max(counts.channels, 1))) {
  for (Channel& channel : channels_) {
    channel.left.assign(kBlock, 0.0F);
    channel.right.assign(kBlock, 0.0F);
    channel.gainLeft = kChannelGain;
    channel.gainRight = kChannelGain;
  }
  const size_t channelCount = channels_.size();
  size_t track = 0;
  for (int t = 0; t < counts.audioTracks; ++t, ++track) {
    streamers_.push_back({track % channelCount, (track * kAudioFrames / 7) % kAudioFrames, 1.0F});
  }
  for (int t = 0; t < counts.patternTracks; ++t, ++track) {
    for (int v = 0; v < counts.voicesPerPatternTrack; ++v) {
      Voice voice;
      voice.channel = track % channelCount;
      const int semitones = kPitchLow + kPitchStep * v + t;
      voice.state.step = stepOfSpeed(std::pow(2.0, semitones / kSemitones));
      voice.state.loop = true;
      voices_.push_back(voice);
    }
  }
  size_t next = 0;
  const auto assign = [&](int count, int kind) {
    for (int i = 0; i < count; ++i, ++next) {
      Channel& channel = channels_[next % channelCount];
      channel.kind = kind;
      if (kind == kKindEqualizer) {
        channel.effect = equalizers_.size();
        equalizers_.emplace_back().set({3.0, 120.0, -2.0, 1000.0, 0.9, 2.0, 8000.0});
      } else if (kind == kKindDelay) {
        channel.effect = delays_.size();
        delays_.emplace_back().set({});
      } else {
        channel.effect = reverbs_.size();
        reverbs_.emplace_back().set({});
      }
    }
  };
  equalizers_.reserve(static_cast<size_t>(counts.equalizers));
  delays_.reserve(static_cast<size_t>(counts.delays));
  reverbs_.reserve(static_cast<size_t>(counts.reverbs));
  assign(counts.equalizers, kKindEqualizer);
  assign(counts.delays, kKindDelay);
  assign(counts.reverbs, kKindReverb);
}

size_t LoadBench::effects() const { return equalizers_.size() + delays_.size() + reverbs_.size(); }

void LoadBench::render(float* left, float* right, int frames) {
  for (int done = 0; done < frames; done += kBlock) {
    const int count = std::min(kBlock, frames - done);
    renderBlock(left + done, right + done, count);
  }
}

void LoadBench::renderBlock(float* left, float* right, int frames) {
  const auto size = static_cast<size_t>(frames);
  for (Channel& channel : channels_) {
    std::fill_n(channel.left.begin(), size, 0.0F);
    std::fill_n(channel.right.begin(), size, 0.0F);
  }
  for (Voice& voice : voices_) {
    Channel& channel = channels_[voice.channel];
    renderVoice(table_, sample_, voice.state, channel.left.data(), channel.right.data(), frames);
  }
  for (Streamer& streamer : streamers_) {
    Channel& channel = channels_[streamer.channel];
    for (size_t i = 0; i < size; ++i) {
      channel.left[i] += audioLeft_[streamer.position] * streamer.gain;
      channel.right[i] += audioRight_[streamer.position] * streamer.gain;
      streamer.position = streamer.position + 1 == kAudioFrames ? 0 : streamer.position + 1;
    }
  }
  std::fill_n(left, size, 0.0F);
  std::fill_n(right, size, 0.0F);
  for (Channel& channel : channels_) {
    if (channel.kind == kKindEqualizer) {
      equalizers_[channel.effect].process(channel.left.data(), channel.right.data(), frames);
    } else if (channel.kind == kKindDelay) {
      delays_[channel.effect].process(channel.left.data(), channel.right.data(), frames);
    } else if (channel.kind == kKindReverb) {
      reverbs_[channel.effect].process(channel.left.data(), channel.right.data(), frames);
    }
    for (size_t i = 0; i < size; ++i) {
      left[i] += channel.left[i] * channel.gainLeft;
      right[i] += channel.right[i] * channel.gainRight;
    }
  }
}

std::vector<KernelCost> measureKernels(const std::vector<float>& sample, int blocks) {
  std::vector<float> left(LoadBench::kBlock, 0.0F);
  std::vector<float> right(LoadBench::kBlock, 0.0F);
  const auto perBlock = [blocks](const auto& kernel) {
    const auto start = Clock::now();
    for (int b = 0; b < blocks; ++b) {
      kernel();
    }
    const std::chrono::duration<double> spent = Clock::now() - start;
    return spent.count() * kMicrosPerSecond / std::max(blocks, 1);
  };
  const SincTable table;
  const GuardedSample guarded(sample, sample);
  VoiceState voice;
  voice.step = stepOfSpeed(std::pow(2.0, 4.0 / kSemitones));
  voice.loop = true;
  const auto renderOneVoice = [&] {
    renderVoice(table, guarded, voice, left.data(), right.data(), LoadBench::kBlock);
  };
  const std::vector<float> audio = repeated(sample, kAudioFrames);
  size_t position = 0;
  const auto streamOne = [&] {
    for (size_t i = 0; i < LoadBench::kBlock; ++i) {
      left[i] += audio[position];
      right[i] += audio[position];
      position = position + 1 == kAudioFrames ? 0 : position + 1;
    }
  };
  Equalizer equalizer;
  equalizer.set({3.0, 120.0, -2.0, 1000.0, 0.9, 2.0, 8000.0});
  const auto equalize = [&] { equalizer.process(left.data(), right.data(), LoadBench::kBlock); };
  Delay delay;
  delay.set({});
  const auto echo = [&] { delay.process(left.data(), right.data(), LoadBench::kBlock); };
  Reverb reverb;
  reverb.set({});
  const auto reverberate = [&] { reverb.process(left.data(), right.data(), LoadBench::kBlock); };
  std::vector<KernelCost> costs;
  costs.push_back({"voice", perBlock(renderOneVoice)});
  costs.push_back({"streamer", perBlock(streamOne)});
  costs.push_back({"eq3", perBlock(equalize)});
  costs.push_back({"delay", perBlock(echo)});
  costs.push_back({"reverb", perBlock(reverberate)});
  return costs;
}

}  // namespace minidaw::engine
