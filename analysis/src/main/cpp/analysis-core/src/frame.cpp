#include "minidaw/analysis/frame.h"

#include <algorithm>
#include <cmath>

namespace minidaw::analysis {

std::size_t frameCount(std::size_t samples) { return (samples + kHop - 1) / kHop; }

float frameLoudness(std::span<const float> audio, std::size_t frame) {
  constexpr std::size_t kHalf = 256;
  const std::size_t center = frame * kHop;
  const std::size_t begin = center >= kHalf ? center - kHalf : 0;
  const std::size_t end = std::min(audio.size(), center + kHalf);
  if (begin >= end) {
    return kSilenceDb;
  }
  double sum = 0.0;
  for (std::size_t i = begin; i < end; ++i) {
    const double x = audio[i];
    sum += x * x;
  }
  const double rms = std::sqrt(sum / static_cast<double>(end - begin));
  if (rms <= 0.0) {
    return kSilenceDb;
  }
  return std::max(kSilenceDb, static_cast<float>(20.0 * std::log10(rms)));
}

}  // namespace minidaw::analysis
