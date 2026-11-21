// Tests of the own pYIN (feature_specifications.md Section 4.2.4).
#include "minidaw/analysis/pyin.h"

#include <gtest/gtest.h>

#include <cmath>
#include <vector>

#include "test_melodies.h"

namespace minidaw::analysis {
namespace {

TEST(Pyin, PureTonesGiveTheirFrequency) {
  for (const double f : {65.0, 110.0, 220.0, 440.0, 880.0, 1760.0, 2000.0}) {
    Pyin pyin;
    const auto frames = pyin.detect(fixtures::sine(f, 1.0, kAnalysisRate, 0.3));
    ASSERT_EQ(frames.size(), frameCount(kAnalysisRate));
    for (std::size_t i = 5; i + 5 < frames.size(); ++i) {
      ASSERT_GE(frames[i].confidence, pyin.minConfidence()) << f << " Hz, frame " << i;
      const double cents = 1200.0 * std::log2(frames[i].frequency / f);
      ASSERT_LT(std::abs(cents), 10.0) << f << " Hz, frame " << i;
    }
  }
}

TEST(Pyin, SilenceGivesNoPitch) {
  Pyin pyin;
  const std::vector<float> silence(kAnalysisRate, 0.0F);
  for (const Frame& f : pyin.detect(silence)) {
    EXPECT_EQ(f.frequency, 0.0F);
    EXPECT_EQ(f.confidence, 0.0F);
    EXPECT_EQ(f.loudness, kSilenceDb);
  }
}

TEST(Pyin, LoudnessIsTheLevelOfTheFrame) {
  Pyin pyin;
  const auto frames = pyin.detect(fixtures::sine(440.0, 1.0, kAnalysisRate, 0.3));
  const double expected = 20.0 * std::log10(0.3 / std::sqrt(2.0));
  EXPECT_NEAR(frames[30].loudness, expected, 0.2);
}

}  // namespace
}  // namespace minidaw::analysis
