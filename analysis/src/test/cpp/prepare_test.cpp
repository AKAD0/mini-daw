// Tests of step 1 of the conversion (feature_specifications.md Section 4.2.3).
#include "minidaw/analysis/prepare.h"

#include <gtest/gtest.h>

#include <cmath>
#include <stdexcept>
#include <vector>

#include "minidaw/analysis/frame.h"
#include "test_melodies.h"

namespace minidaw::analysis {
namespace {

double rms(const std::vector<float>& x, std::size_t from) {
  double sum = 0.0;
  for (std::size_t i = from; i < x.size(); ++i) {
    sum += static_cast<double>(x[i]) * x[i];
  }
  return std::sqrt(sum / static_cast<double>(x.size() - from));
}

TEST(Prepare, AudioAt48KHzBecomes16KHzWithItsPitchAndLevel) {
  const auto in = fixtures::sine(1000.0, 1.0, 48000, 0.5);
  const auto out = prepare(in, 48000);
  ASSERT_EQ(out.size(), 16000U);
  std::size_t crossings = 0;
  for (std::size_t i = 1001; i < out.size(); ++i) {
    if ((out[i - 1] < 0.0F) != (out[i] < 0.0F)) {
      ++crossings;
    }
  }
  const double seconds = static_cast<double>(out.size() - 1001) / kAnalysisRate;
  EXPECT_NEAR(static_cast<double>(crossings) / 2.0 / seconds, 1000.0, 3.0);
  EXPECT_NEAR(rms(out, 1000) / rms(in, 3000), 1.0, 0.03);
}

TEST(Prepare, TheHighPassFilterRemovesRumble) {
  const auto low = prepare(fixtures::sine(20.0, 2.0, 48000, 0.5), 48000);
  EXPECT_LT(rms(low, 8000) / (0.5 / std::sqrt(2.0)), 0.15);
  const auto voice = prepare(fixtures::sine(200.0, 2.0, 16000, 0.5), 16000);
  EXPECT_NEAR(rms(voice, 8000) / (0.5 / std::sqrt(2.0)), 1.0, 0.05);
}

TEST(Prepare, ARateThatIsNoMultipleOf16KHzIsRefused) {
  const std::vector<float> x(441, 0.0F);
  EXPECT_THROW(prepare(x, 44100), std::invalid_argument);
}

}  // namespace
}  // namespace minidaw::analysis
