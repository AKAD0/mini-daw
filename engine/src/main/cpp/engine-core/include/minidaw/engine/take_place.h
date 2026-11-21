#pragma once

#include <cstdint>
#include <span>

namespace minidaw::engine {

// A frame of a stream and the time on the monotonic clock at which it passed the hardware
// (feature_specifications.md Section 4.2.2). Oboe reports such pairs for each stream.
struct FramePair {
  int64_t frame = 0;
  int64_t nanos = 0;
};

// The time at which the frame `frame` of a stream passes the hardware, by the pair `pair`.
double timeOfFrame(const FramePair& pair, int64_t frame, double rate);

// The frame of a stream that passes the hardware at the time `nanos`, by the pair `pair`.
double frameAtTime(const FramePair& pair, double nanos, double rate);

// The pair whose frame lies nearest to `frame`. The list must not be empty.
const FramePair& nearestByFrame(std::span<const FramePair> pairs, int64_t frame);

// The pair whose time lies nearest to `nanos`. The list must not be empty.
const FramePair& nearestByTime(std::span<const FramePair> pairs, double nanos);

// The place of a click in a recording, found by the peak of the cross-correlation.
struct ClickPlace {
  int64_t start = -1;    // The index of the recording where the click starts, or -1.
  double clarity = 0.0;  // The peak divided by the RMS of the correlation over the range.
};

// Searches the starts `from` to `from + length - 1` of the recording for the click (the check
// V-07 and the calibration by loopback, D-040). The polarity of the click does not matter.
ClickPlace findClick(std::span<const float> recording, std::span<const float> click, int64_t from,
                     int64_t length);

}  // namespace minidaw::engine
