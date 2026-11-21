#pragma once

#include <cstdint>
#include <span>
#include <vector>

#include "minidaw/analysis/frame.h"

namespace minidaw::analysis {

// The candidates of D-044. Their numbers are those of the cache of the pitch
// (feature_specifications.md Section 6.5), which stores them as u16.
enum class Candidate : std::uint8_t { kSwiftF0 = 1, kPyin = 2 };

// Both candidates sit behind this interface (architecture.md Section 9, rule 6).
// The code for Notes does not know which one runs.
class PitchDetector {
 public:
  virtual ~PitchDetector() = default;

  [[nodiscard]] virtual Candidate candidate() const = 0;

  // Start value of the lowest confidence that counts as pitch (Section 4.2.4).
  [[nodiscard]] virtual float minConfidence() const = 0;

  // Frames of mono audio at 16 kHz, one per kHop samples.
  [[nodiscard]] virtual std::vector<Frame> detect(std::span<const float> audio) = 0;
};

}  // namespace minidaw::analysis
