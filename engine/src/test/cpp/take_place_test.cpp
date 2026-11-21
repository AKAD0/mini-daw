#include "minidaw/engine/take_place.h"

#include <gtest/gtest.h>

#include <cmath>
#include <cstddef>
#include <cstdint>
#include <numbers>
#include <vector>

namespace minidaw::engine {
namespace {

constexpr double kRate = 48000.0;

// A click like "Click high": a sine of 2,000 Hz that falls by 60 dB within 25 ms.
std::vector<float> clickHigh() {
  std::vector<float> click(1440);
  for (size_t i = 0; i < click.size(); ++i) {
    const double t = static_cast<double>(i) / kRate;
    click[i] = static_cast<float>(std::sin(2.0 * std::numbers::pi * 2000.0 * t) *
                                  std::pow(10.0, -3.0 * t / 0.025));
  }
  return click;
}

TEST(TakePlace, TheFrameAtTheTimeOfAFrameIsTheFrameAgain) {
  const FramePair pair{1000, 5'000'000'000};
  const double nanos = timeOfFrame(pair, 49000, kRate);
  EXPECT_DOUBLE_EQ(nanos, 6'000'000'000.0);
  EXPECT_DOUBLE_EQ(frameAtTime(pair, nanos, kRate), 49000.0);
}

TEST(TakePlace, TheOutputFrameMapsToTheInputFrameCapturedAtTheSameTime) {
  // The input stream started 0.5 s after the output stream, on the same clock (Section 4.2.2).
  const FramePair output{0, 1'000'000'000};
  const FramePair input{0, 1'500'000'000};
  const double heard = timeOfFrame(output, 96000, kRate);  // 2 s after the start of the output
  EXPECT_DOUBLE_EQ(frameAtTime(input, heard, kRate), 72000.0);
}

TEST(TakePlace, TheNearestPairIsChosen) {
  const std::vector<FramePair> pairs{{0, 0}, {4800, 100'000'000}, {9600, 200'000'000}};
  EXPECT_EQ(nearestByFrame(pairs, 5000).frame, 4800);
  EXPECT_EQ(nearestByTime(pairs, 190'000'000.0).frame, 9600);
}

TEST(TakePlace, AClickIsFoundAtItsStart) {
  const std::vector<float> click = clickHigh();
  std::vector<float> recording(24000, 0.0F);
  const int64_t start = 7321;
  for (size_t j = 0; j < click.size(); ++j) {
    // Quieter, with the other polarity, and with a little noise.
    const auto noise = static_cast<float>(static_cast<int64_t>((j * 7919) % 101) - 50) * 1e-4F;
    recording[static_cast<size_t>(start) + j] = -0.3F * click[j] + noise;
  }
  const ClickPlace place = findClick(recording, click, 0, 20000);
  EXPECT_EQ(place.start, start);
  EXPECT_GT(place.clarity, 5.0);
}

TEST(TakePlace, SilenceHoldsNoClick) {
  const std::vector<float> recording(4800, 0.0F);
  const ClickPlace place = findClick(recording, clickHigh(), 0, 2000);
  EXPECT_EQ(place.start, -1);
  EXPECT_EQ(place.clarity, 0.0);
}

TEST(TakePlace, TheSearchStaysInsideTheRecording) {
  const std::vector<float> click = clickHigh();
  const std::vector<float> recording(1000, 0.5F);  // shorter than the click
  EXPECT_EQ(findClick(recording, click, 0, 500).start, -1);
  EXPECT_EQ(findClick(recording, click, -100, 50).start, -1);
}

}  // namespace
}  // namespace minidaw::engine
