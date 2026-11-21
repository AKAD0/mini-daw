#include "minidaw/engine/kernels.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <numbers>
#include <vector>

namespace minidaw::engine {
namespace {

std::vector<float> sine(double frequency, size_t frames, double amplitude = 0.5) {
  std::vector<float> values(frames);
  for (size_t i = 0; i < frames; ++i) {
    const double phase = 2.0 * std::numbers::pi * frequency * static_cast<double>(i) / kSampleRate;
    values[i] = static_cast<float>(amplitude * std::sin(phase));
  }
  return values;
}

double peak(const std::vector<float>& values, size_t from, size_t to) {
  double most = 0.0;
  for (size_t i = from; i < to && i < values.size(); ++i) {
    most = std::max(most, std::abs(static_cast<double>(values[i])));
  }
  return most;
}

TEST(Kernels, TheTapsOfEachPositionAddUpToOne) {
  const SincTable table;
  for (int position = 0; position <= SincTable::kPositions; ++position) {
    double sum = 0.0;
    for (int k = 0; k < SincTable::kTaps; ++k) {
      sum += table.taps(position)[k];
    }
    EXPECT_NEAR(sum, 1.0, 1e-6);
  }
}

TEST(Kernels, AtTheSpeedOneTheVoicePassesTheSampleUnchanged) {
  const SincTable table;
  const std::vector<float> values = sine(997.0, 4800);
  const GuardedSample sample(values, values);
  VoiceState state;
  std::vector<float> left(4800, 0.0F);
  std::vector<float> right(4800, 0.0F);
  renderVoice(table, sample, state, left.data(), right.data(), 4800);
  for (size_t i = 0; i < values.size(); ++i) {
    ASSERT_LT(std::abs(left[i] - values[i]), 1e-6F);
  }
}

TEST(Kernels, BetweenTheFramesTheSincFollowsTheSine) {
  // At the speed 2^(1/12) a sine of 1 kHz becomes a sine of 1,059.5 Hz. The error of the
  // interpolation stays at least 60 dB below the signal (AC-SM-02 asks this of the whole sampler).
  const SincTable table;
  const double speed = std::pow(2.0, 1.0 / 12.0);
  const std::vector<float> values = sine(1000.0, 48000);
  const GuardedSample sample(values, values);
  VoiceState state;
  state.step = stepOfSpeed(speed);
  std::vector<float> left(24000, 0.0F);
  std::vector<float> right(24000, 0.0F);
  renderVoice(table, sample, state, left.data(), right.data(), 24000);
  double error = 0.0;
  double signal = 0.0;
  for (size_t i = 100; i < left.size(); ++i) {
    const double t = static_cast<double>(i) * speed / kSampleRate;
    const double expected = 0.5 * std::sin(2.0 * std::numbers::pi * 1000.0 * t);
    error += (left[i] - expected) * (left[i] - expected);
    signal += expected * expected;
  }
  EXPECT_LT(10.0 * std::log10(error / signal), -60.0);
}

TEST(Kernels, AVoiceEndsAtTheEndOfItsSample) {
  const SincTable table;
  const std::vector<float> values(100, 0.25F);
  const GuardedSample sample(values, values);
  VoiceState state;
  std::vector<float> left(200, 0.0F);
  std::vector<float> right(200, 0.0F);
  renderVoice(table, sample, state, left.data(), right.data(), 200);
  EXPECT_FALSE(state.active);
  EXPECT_EQ(peak(left, 101, 200), 0.0);
}

TEST(Kernels, TheEqualizerAtNeutralValuesChangesNothing) {
  Equalizer equalizer;
  equalizer.set({});
  std::vector<float> left = sine(440.0, 4800);
  std::vector<float> right = sine(5000.0, 4800);
  const std::vector<float> before = left;
  equalizer.process(left.data(), right.data(), 4800);
  for (size_t i = 0; i < left.size(); ++i) {
    ASSERT_LT(std::abs(left[i] - before[i]), 1e-5F);
  }
}

TEST(Kernels, ThePeakOfTheEqualizerRaisesItsFrequency) {
  Equalizer equalizer;
  Equalizer::Values values;
  values.midGainDb = 6.0;
  equalizer.set(values);
  std::vector<float> left = sine(1000.0, 48000, 0.25);
  std::vector<float> right = left;
  equalizer.process(left.data(), right.data(), 48000);
  const double gainDb = 20.0 * std::log10(peak(left, 24000, 48000) / 0.25);
  EXPECT_NEAR(gainDb, 6.0, 0.1);
}

TEST(Kernels, TheDelayAnswersAnImpulseWithFallingEchoes) {
  Delay delay;
  Delay::Values values;
  values.timeMs = 100.0;
  values.mix = 1.0;
  values.dampingFrequency = 20000.0;
  delay.set(values);
  std::vector<float> left(24000, 0.0F);
  std::vector<float> right(24000, 0.0F);
  left[0] = 1.0F;
  delay.process(left.data(), right.data(), 24000);
  const double first = peak(left, 4790, 4810);
  const double second = peak(left, 9590, 9610);
  EXPECT_NEAR(first, 1.0, 1e-6);
  EXPECT_GT(second, 0.2);
  EXPECT_LT(second, first * values.feedback + 1e-3);
  EXPECT_EQ(peak(left, 1, 4790), 0.0);
}

TEST(Kernels, TheReverbKeepsSilenceAndItsTailEnds) {
  Reverb reverb;
  reverb.set({});
  std::vector<float> left(48000 * 6, 0.0F);
  std::vector<float> right(48000 * 6, 0.0F);
  reverb.process(left.data(), right.data(), 4800);
  EXPECT_EQ(peak(left, 0, 4800), 0.0);
  left[4800] = 1.0F;
  right[4800] = 1.0F;
  reverb.process(left.data() + 4800, right.data() + 4800, 48000 * 6 - 4800);
  EXPECT_GT(peak(left, 4801, 48000), 1e-3);
  EXPECT_LT(peak(left, 48000 * 5, 48000 * 6), 1e-4);
}

}  // namespace
}  // namespace minidaw::engine
