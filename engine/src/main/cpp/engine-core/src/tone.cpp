#include "minidaw/engine/tone.h"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

#include "minidaw/engine/kernels.h"

namespace minidaw::engine {
namespace {

constexpr double kSemitones = 12.0;

}  // namespace

std::vector<float> renderTone(std::span<const ToneNote> notes, const std::vector<float>& sample,
                              double rootNote, double releaseSeconds) {
  double last = 0.0;
  for (const ToneNote& note : notes) {
    last = std::max(last, note.end + releaseSeconds);
  }
  const auto total = static_cast<size_t>(std::ceil(last * kSampleRate));
  std::vector<float> out(total, 0.0F);
  if (sample.empty()) {
    return out;
  }
  const SincTable table;
  const GuardedSample guarded(sample, sample);
  const auto release = static_cast<size_t>(std::max(releaseSeconds, 0.0) * kSampleRate);
  std::vector<float> left;
  std::vector<float> right;
  for (const ToneNote& note : notes) {
    const auto first = static_cast<size_t>(std::max(note.start, 0.0) * kSampleRate);
    const auto gateEnd = static_cast<size_t>(std::max(note.end, note.start) * kSampleRate);
    const size_t end = std::min(gateEnd + release, total);
    if (first >= end) {
      continue;
    }
    const size_t frames = end - first;
    left.assign(frames, 0.0F);
    right.assign(frames, 0.0F);
    VoiceState voice;
    voice.step = stepOfSpeed(std::pow(2.0, (std::round(note.pitch) - rootNote) / kSemitones));
    renderVoice(table, guarded, voice, left.data(), right.data(), static_cast<int>(frames));
    const size_t gate = gateEnd - first;
    for (size_t i = 0; i < frames; ++i) {
      float gain = 1.0F;
      if (i >= gate) {
        const double faded = 1.0 - static_cast<double>(i - gate) / static_cast<double>(release);
        gain = static_cast<float>(std::max(faded, 0.0));
      }
      out[first + i] += left[i] * gain;
    }
  }
  return out;
}

}  // namespace minidaw::engine
