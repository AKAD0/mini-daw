#pragma once

#include <span>
#include <vector>

namespace minidaw::engine {

// One Note to play: times in seconds and a pitch (69 is the A at 440 Hz).
struct ToneNote {
  double start = 0.0;
  double end = 0.0;
  double pitch = 60.0;
};

// Renders Notes with one Sample in the play mode gate into mono audio at 48 kHz: each Note plays
// the Sample at the speed of its pitch, rounded to a semitone, until its end and then fades out
// within `releaseSeconds`. The check V-05 plays the results of both pitch candidates so with the
// default Sampler instrument "Keys" (feature_specifications.md Section 7.1, Root note 60).
std::vector<float> renderTone(std::span<const ToneNote> notes, const std::vector<float>& sample,
                              double rootNote, double releaseSeconds);

}  // namespace minidaw::engine
