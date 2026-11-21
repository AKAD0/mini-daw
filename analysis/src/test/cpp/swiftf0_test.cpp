// The criteria AC-H2M-01, AC-H2M-10 and AC-H2M-13 on the test melodies (feature_specifications.md
// Sections 4.8 and 4.9) for the candidate SwiftF0, and its pitch for pure tones.
#include "minidaw/analysis/swiftf0.h"

#include <gtest/gtest.h>

#include <cmath>

#include "melody_checks.h"
#include "test_melodies.h"

namespace minidaw::analysis {
namespace {

// The CMake file of the tests names the model file of the repository.
constexpr const char* kModel = MINIDAW_SWIFTF0_MODEL;

TEST(MelodiesSwiftF0, EveryNoteIsFound) {
  SwiftF0 detector(kModel);
  fixtures::expectEveryNote(detector);
}

TEST(MelodiesSwiftF0, TwoConversionsGiveTheSameNotes) {
  SwiftF0 detector(kModel);
  fixtures::expectSameNotesTwice(detector);
}

TEST(MelodiesSwiftF0, VoiceKeepsItsPitchAndWhistleGoesTwoOctavesDown) {
  SwiftF0 detector(kModel);
  fixtures::expectVoiceAndWhistle(detector);
}

TEST(MelodiesSwiftF0, SilenceAndNoiseGiveNoNotes) {
  SwiftF0 detector(kModel);
  fixtures::expectNothingFromSilenceAndNoise(detector);
}

// Pure tones have no overtones. SwiftF0 gives the right pitch for them, but for a pure tone at
// 65 Hz its confidence stays near 0.42 (TK-008, run "Check" 44849904976). The check therefore
// asks for voiced frames from 110 Hz up only, and for the right pitch in every voiced frame.
TEST(SwiftF0, PureTonesGiveTheirFrequency) {
  SwiftF0 detector(kModel);
  for (const double f : {65.0, 110.0, 220.0, 440.0, 880.0, 1760.0, 2000.0}) {
    const auto frames = detector.detect(fixtures::sine(f, 1.0, kAnalysisRate, 0.3));
    ASSERT_EQ(frames.size(), frameCount(kAnalysisRate));
    // The model looks 2,815 samples to each side, about 11 frames. The frames near the ends of
    // the signal lack this context, so the check starts and ends 12 frames inside.
    double worst = 0.0;
    std::size_t inner = 0;
    std::size_t voiced = 0;
    for (std::size_t i = 12; i + 12 < frames.size(); ++i) {
      ++inner;
      if (frames[i].confidence >= detector.minConfidence()) {
        ++voiced;
        worst = std::fmax(worst, std::abs(1200.0 * std::log2(frames[i].frequency / f)));
      }
    }
    const double share = static_cast<double>(voiced) / static_cast<double>(inner);
    if (f >= 110.0) {
      EXPECT_GE(share, 0.9) << f << " Hz";
    }
    EXPECT_LT(worst, 25.0) << f << " Hz, voiced share " << share;
  }
}

}  // namespace
}  // namespace minidaw::analysis
