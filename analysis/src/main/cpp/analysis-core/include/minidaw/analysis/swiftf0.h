#pragma once

#include <memory>
#include <span>
#include <string>
#include <vector>

#include "minidaw/analysis/frame.h"
#include "minidaw/analysis/pitch_detector.h"

namespace minidaw::analysis {

// The candidate SwiftF0 0.3.0 through ONNX Runtime (technology_stack.md Section 8.2). The model
// reads audio at 16 kHz and gives one pitch and one confidence per 16 ms, from 46.875 Hz to
// 2,093.75 Hz. The work follows the reference code of the model: windows of 30 s with 11 frames
// before and 10 frames after, and confidence 0 for a frame whose audio peaks below 10^-3.
class SwiftF0 final : public PitchDetector {
 public:
  // Loads the model file. One thread, so that two runs give the same frames (AC-H2M-10).
  explicit SwiftF0(const std::string& modelPath);
  ~SwiftF0() override;
  SwiftF0(const SwiftF0&) = delete;
  SwiftF0& operator=(const SwiftF0&) = delete;
  SwiftF0(SwiftF0&&) = delete;
  SwiftF0& operator=(SwiftF0&&) = delete;

  [[nodiscard]] Candidate candidate() const override { return Candidate::kSwiftF0; }
  // A frame is voiced at a confidence of 0.5 or more since SwiftF0 0.2.0.
  [[nodiscard]] float minConfidence() const override { return 0.5F; }
  [[nodiscard]] std::vector<Frame> detect(std::span<const float> audio) override;

 private:
  struct Model;
  std::unique_ptr<Model> model_;
};

}  // namespace minidaw::analysis
