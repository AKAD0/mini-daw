#include "minidaw/engine/take_place.h"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <span>

namespace minidaw::engine {
namespace {

constexpr double kNanosPerSecond = 1e9;

}  // namespace

double timeOfFrame(const FramePair& pair, int64_t frame, double rate) {
  return static_cast<double>(pair.nanos) +
         static_cast<double>(frame - pair.frame) * kNanosPerSecond / rate;
}

double frameAtTime(const FramePair& pair, double nanos, double rate) {
  return static_cast<double>(pair.frame) +
         (nanos - static_cast<double>(pair.nanos)) * rate / kNanosPerSecond;
}

const FramePair& nearestByFrame(std::span<const FramePair> pairs, int64_t frame) {
  return *std::ranges::min_element(pairs, {}, [frame](const FramePair& pair) {
    return std::abs(pair.frame - frame);
  });
}

const FramePair& nearestByTime(std::span<const FramePair> pairs, double nanos) {
  return *std::ranges::min_element(pairs, {}, [nanos](const FramePair& pair) {
    return std::abs(static_cast<double>(pair.nanos) - nanos);
  });
}

ClickPlace findClick(std::span<const float> recording, std::span<const float> click, int64_t from,
                     int64_t length) {
  const auto size = static_cast<int64_t>(recording.size());
  const auto clickSize = static_cast<int64_t>(click.size());
  const int64_t first = std::max<int64_t>(from, 0);
  const int64_t last = std::min(from + length, size - clickSize + 1);
  if (clickSize == 0 || first >= last) {
    return {};
  }
  ClickPlace best;
  double peak = 0.0;
  double sumOfSquares = 0.0;
  for (int64_t start = first; start < last; ++start) {
    double sum = 0.0;
    for (int64_t j = 0; j < clickSize; ++j) {
      sum += static_cast<double>(click[static_cast<size_t>(j)]) *
             static_cast<double>(recording[static_cast<size_t>(start + j)]);
    }
    sumOfSquares += sum * sum;
    if (std::abs(sum) > peak) {
      peak = std::abs(sum);
      best.start = start;
    }
  }
  const double rms = std::sqrt(sumOfSquares / static_cast<double>(last - first));
  best.clarity = rms > 0.0 ? peak / rms : 0.0;
  if (peak == 0.0) {
    best.start = -1;
  }
  return best;
}

}  // namespace minidaw::engine
