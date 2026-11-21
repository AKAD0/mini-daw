#pragma once

#include <span>
#include <vector>

#include "minidaw/analysis/frame.h"
#include "minidaw/analysis/pitch_detector.h"

namespace minidaw::analysis {

// The own pYIN, written after the paper of Mauch and Dixon, 2014 (technology_stack.md 8.2).
// Window 1,024 samples, step 16 ms, range 60 to 2,094 Hz (feature_specifications.md Section 4.2.4).
class Pyin final : public PitchDetector {
 public:
  [[nodiscard]] Candidate candidate() const override { return Candidate::kPyin; }
  [[nodiscard]] float minConfidence() const override { return 0.5F; }
  [[nodiscard]] std::vector<Frame> detect(std::span<const float> audio) override;
};

}  // namespace minidaw::analysis
