#include "minidaw/engine/tone.h"

#include <gtest/gtest.h>

#include <cmath>
#include <cstddef>
#include <numbers>
#include <vector>

namespace minidaw::engine {
namespace {

// Rising zero crossings of a part of the audio.
int crossings(const std::vector<float>& audio, size_t from, size_t to) {
  int count = 0;
  for (size_t i = from + 1; i < to; ++i) {
    if (audio[i - 1] < 0.0F && audio[i] >= 0.0F) {
      ++count;
    }
  }
  return count;
}

std::vector<float> sine(double frequency, double seconds) {
  std::vector<float> values(static_cast<size_t>(seconds * 48000.0));
  for (size_t i = 0; i < values.size(); ++i) {
    const double phase = 2.0 * std::numbers::pi * frequency * static_cast<double>(i) / 48000.0;
    values[i] = static_cast<float>(0.5 * std::sin(phase));
  }
  return values;
}

TEST(Tone, ANoteAnOctaveAboveTheRootNotePlaysAtTwiceTheFrequency) {
  const std::vector<ToneNote> notes{{0.0, 0.5, 60.0}, {0.5, 1.0, 72.2}};
  const std::vector<float> audio = renderTone(notes, sine(200.0, 2.0), 60.0, 0.05);
  ASSERT_EQ(audio.size(), 50400U);
  // 0.4 s at 200 Hz, then 0.4 s at 400 Hz.
  EXPECT_NEAR(crossings(audio, 2400, 21600), 80, 1);
  EXPECT_NEAR(crossings(audio, 26400, 45600), 160, 1);
}

TEST(Tone, ANoteEndsWithinItsRelease) {
  const std::vector<ToneNote> notes{{0.1, 0.3, 64.0}};
  const std::vector<float> audio = renderTone(notes, sine(300.0, 2.0), 60.0, 0.05);
  double before = 0.0;
  double after = 0.0;
  for (size_t i = 0; i < audio.size(); ++i) {
    if (i < 4800) {
      before = std::max(before, static_cast<double>(std::abs(audio[i])));
    }
    if (i >= 16800) {
      after = std::max(after, static_cast<double>(std::abs(audio[i])));
    }
  }
  EXPECT_EQ(before, 0.0);
  EXPECT_LT(after, 1e-6);
  EXPECT_EQ(audio.size(), 16800U);
}

TEST(Tone, NoNotesGiveNoAudio) {
  EXPECT_TRUE(renderTone({}, sine(300.0, 1.0), 60.0, 0.05).empty());
}

}  // namespace
}  // namespace minidaw::engine
