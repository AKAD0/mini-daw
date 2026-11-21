#include "minidaw/engine/kernels.h"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <numbers>
#include <vector>

namespace minidaw::engine {
namespace {

constexpr double kKaiserBeta = 7.0;
// The full band: at the speed 1.0 the Voice passes the Sample unchanged. The copies at half and at
// quarter rate keep the aliasing low at higher speeds (D-085).
constexpr double kCutoff = 1.0;
// The taps reach from 3 frames before the position to 4 frames after it.
constexpr int kCenter = 3;
constexpr double kFixedOne = 4294967296.0;
// The upper 6 bits of the fraction choose one of the 64 positions; the lower 26 bits blend.
constexpr int kPositionShift = 26;
constexpr uint64_t kPositionMask = SincTable::kPositions - 1;
constexpr uint64_t kBlendMask = (uint64_t{1} << kPositionShift) - 1;
constexpr float kBlendScale = 1.0F / static_cast<float>(uint64_t{1} << kPositionShift);
constexpr double kHalfWidth = 4.0;

// The modified Bessel function of order zero, by its series.
double besselI0(double x) {
  double sum = 1.0;
  double term = 1.0;
  for (int k = 1; k < 32; ++k) {
    term *= (x / (2.0 * k)) * (x / (2.0 * k));
    sum += term;
  }
  return sum;
}

double windowedSinc(double x) {
  const double ratio = x / kHalfWidth;
  if (std::abs(ratio) >= 1.0) {
    return 0.0;
  }
  const double shape = besselI0(kKaiserBeta * std::sqrt(1.0 - ratio * ratio));
  const double window = shape / besselI0(kKaiserBeta);
  const double arg = std::numbers::pi * kCutoff * x;
  const double sinc = x == 0.0 ? 1.0 : std::sin(arg) / arg;
  return kCutoff * sinc * window;
}

// Freeverb tunings for 44.1 kHz, scaled to 48 kHz.
constexpr std::array kCombTunings{1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
constexpr std::array kAllPassTunings{556, 441, 341, 225};
static_assert(kCombTunings.size() == Reverb::kCombs);
static_assert(kAllPassTunings.size() == Reverb::kAllPasses);
constexpr int kStereoSpread = 23;
constexpr double kTuningScale = kSampleRate / 44100.0;
constexpr float kReverbInputGain = 0.015F;
constexpr float kAllPassFeedback = 0.5F;
constexpr double kCombLeast = 0.70;
constexpr double kCombRange = 0.28;
constexpr double kDampingScale = 0.4;
constexpr double kWetScale = 3.0;
constexpr double kMaxDelaySeconds = 1.0;
constexpr int kInterpolationGuard = 2;

}  // namespace

SincTable::SincTable() {
  for (int position = 0; position <= kPositions; ++position) {
    const double fraction = static_cast<double>(position) / kPositions;
    double sum = 0.0;
    std::array<double, kTaps> values{};
    for (int tap = 0; tap < kTaps; ++tap) {
      const double x = static_cast<double>(tap - kCenter) - fraction;
      values[static_cast<size_t>(tap)] = windowedSinc(x);
      sum += values[static_cast<size_t>(tap)];
    }
    for (int tap = 0; tap < kTaps; ++tap) {
      table_[static_cast<size_t>(position) * kTaps + tap] =
          static_cast<float>(values[static_cast<size_t>(tap)] / sum);
    }
  }
}

GuardedSample::GuardedSample(const std::vector<float>& left, const std::vector<float>& right)
    : frames_(static_cast<int64_t>(std::min(left.size(), right.size()))) {
  const auto size = static_cast<size_t>(frames_) + kBefore + kAfter;
  left_.assign(size, 0.0F);
  right_.assign(size, 0.0F);
  std::copy_n(left.begin(), frames_, left_.begin() + kBefore);
  std::copy_n(right.begin(), frames_, right_.begin() + kBefore);
}

uint64_t stepOfSpeed(double speed) {
  return static_cast<uint64_t>(std::llround(speed * kFixedOne));
}

void renderVoice(const SincTable& table, const GuardedSample& sample, VoiceState& state,
                 float* outLeft, float* outRight, int frames) {
  const auto end = static_cast<uint64_t>(sample.frames()) << 32U;
  const float* left = sample.left() - kCenter;
  const float* right = sample.right() - kCenter;
  for (int i = 0; i < frames && state.active; ++i) {
    if (state.position >= end) {
      if (!state.loop) {
        state.active = false;
        break;
      }
      state.position -= end;
    }
    const auto index = static_cast<size_t>(state.position >> 32U);
    const auto position = static_cast<int>((state.position >> kPositionShift) & kPositionMask);
    const float blend = static_cast<float>(state.position & kBlendMask) * kBlendScale;
    const float* near = table.taps(position);
    const float* next = table.taps(position + 1);
    const float* l = left + index;
    const float* r = right + index;
    float sumLeft = 0.0F;
    float sumRight = 0.0F;
    for (int k = 0; k < SincTable::kTaps; ++k) {
      const float tap = near[k] + (next[k] - near[k]) * blend;
      sumLeft += tap * l[k];
      sumRight += tap * r[k];
    }
    outLeft[i] += sumLeft * state.gainLeft;
    outRight[i] += sumRight * state.gainRight;
    state.position += state.step;
  }
}

void Biquad::set(Kind kind, double frequency, double gainDb, double q) {
  const double a = std::pow(10.0, gainDb / 40.0);
  const double w0 = 2.0 * std::numbers::pi * frequency / kSampleRate;
  const double cosW = std::cos(w0);
  const double alpha = std::sin(w0) / (2.0 * q);
  const double rootA = 2.0 * std::sqrt(a) * alpha;
  double b0 = 1.0;
  double b1 = 0.0;
  double b2 = 0.0;
  double a0 = 1.0;
  double a1 = 0.0;
  double a2 = 0.0;
  switch (kind) {
    case Kind::kPeak:
      b0 = 1.0 + alpha * a;
      b1 = -2.0 * cosW;
      b2 = 1.0 - alpha * a;
      a0 = 1.0 + alpha / a;
      a1 = -2.0 * cosW;
      a2 = 1.0 - alpha / a;
      break;
    case Kind::kLowShelf:
      b0 = a * ((a + 1.0) - (a - 1.0) * cosW + rootA);
      b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW);
      b2 = a * ((a + 1.0) - (a - 1.0) * cosW - rootA);
      a0 = (a + 1.0) + (a - 1.0) * cosW + rootA;
      a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW);
      a2 = (a + 1.0) + (a - 1.0) * cosW - rootA;
      break;
    case Kind::kHighShelf:
      b0 = a * ((a + 1.0) + (a - 1.0) * cosW + rootA);
      b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW);
      b2 = a * ((a + 1.0) + (a - 1.0) * cosW - rootA);
      a0 = (a + 1.0) - (a - 1.0) * cosW + rootA;
      a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosW);
      a2 = (a + 1.0) - (a - 1.0) * cosW - rootA;
      break;
  }
  b0_ = b0 / a0;
  b1_ = b1 / a0;
  b2_ = b2 / a0;
  a1_ = a1 / a0;
  a2_ = a2 / a0;
}

void Biquad::process(float* values, int frames, int side) {
  double z1 = z1_[static_cast<size_t>(side)];
  double z2 = z2_[static_cast<size_t>(side)];
  for (int i = 0; i < frames; ++i) {
    const double x = values[i];
    const double y = b0_ * x + z1;
    z1 = b1_ * x - a1_ * y + z2;
    z2 = b2_ * x - a2_ * y;
    values[i] = static_cast<float>(y);
  }
  z1_[static_cast<size_t>(side)] = z1;
  z2_[static_cast<size_t>(side)] = z2;
}

void Equalizer::set(const Values& values) {
  // The shelves use the slope S = 1, which is Q = 1 / sqrt(2) in the formulas.
  const double shelfQ = std::numbers::sqrt2 / 2.0;
  bands_[0].set(Biquad::Kind::kLowShelf, values.lowFrequency, values.lowGainDb, shelfQ);
  bands_[1].set(Biquad::Kind::kPeak, values.midFrequency, values.midGainDb, values.midQ);
  bands_[2].set(Biquad::Kind::kHighShelf, values.highFrequency, values.highGainDb, shelfQ);
}

void Equalizer::process(float* left, float* right, int frames) {
  for (Biquad& band : bands_) {
    band.process(left, frames, 0);
    band.process(right, frames, 1);
  }
}

Delay::Delay() {
  const auto size = static_cast<size_t>(kMaxDelaySeconds * kSampleRate) + kInterpolationGuard;
  for (auto& line : lines_) {
    line.assign(size, 0.0F);
  }
}

void Delay::set(const Values& values) {
  const double frames = std::clamp(values.timeMs, 1.0, 1000.0) * kSampleRate / 1000.0;
  delayFrames_ = static_cast<float>(frames);
  feedback_ = static_cast<float>(std::clamp(values.feedback, 0.0, 0.85));
  const double omega = 2.0 * std::numbers::pi * values.dampingFrequency / kSampleRate;
  damping_ = static_cast<float>(1.0 - std::exp(-omega));
  wet_ = static_cast<float>(values.mix);
  dry_ = static_cast<float>(1.0 - values.mix);
}

void Delay::process(float* left, float* right, int frames) {
  const size_t size = lines_[0].size();
  const auto whole = static_cast<size_t>(delayFrames_);
  const float fraction = delayFrames_ - static_cast<float>(whole);
  std::array<float*, 2> sides{left, right};
  for (size_t side = 0; side < 2; ++side) {
    std::vector<float>& line = lines_[side];
    float* values = sides[side];
    float low = lowPass_[side];
    size_t write = write_;
    for (int i = 0; i < frames; ++i) {
      const size_t read0 = (write + size - whole) % size;
      const size_t read1 = (read0 + size - 1) % size;
      const float delayed = line[read0] + (line[read1] - line[read0]) * fraction;
      low += (delayed - low) * damping_;
      line[write] = values[i] + low * feedback_;
      values[i] = values[i] * dry_ + delayed * wet_;
      write = write + 1 == size ? 0 : write + 1;
    }
    lowPass_[side] = low;
  }
  write_ = (write_ + static_cast<size_t>(frames)) % size;
}

Reverb::Reverb() {
  for (size_t side = 0; side < 2; ++side) {
    const int spread = side == 0 ? 0 : kStereoSpread;
    for (size_t c = 0; c < kCombs; ++c) {
      const auto length = static_cast<size_t>((kCombTunings[c] + spread) * kTuningScale);
      combs_[side][c].buffer.assign(length, 0.0F);
    }
    for (size_t a = 0; a < kAllPasses; ++a) {
      const auto length = static_cast<size_t>((kAllPassTunings[a] + spread) * kTuningScale);
      allPasses_[side][a].buffer.assign(length, 0.0F);
    }
  }
}

void Reverb::set(const Values& values) {
  feedback_ = static_cast<float>(kCombLeast + kCombRange * std::clamp(values.size, 0.0, 1.0));
  damp1_ = static_cast<float>(std::clamp(values.damping, 0.0, 1.0) * kDampingScale);
  damp2_ = 1.0F - damp1_;
  const double wet = values.mix * kWetScale;
  wet1_ = static_cast<float>(wet * (values.width / 2.0 + 0.5));
  wet2_ = static_cast<float>(wet * ((1.0 - values.width) / 2.0));
  dry_ = static_cast<float>(1.0 - values.mix);
}

void Reverb::process(float* left, float* right, int frames) {
  for (int i = 0; i < frames; ++i) {
    const float input = (left[i] + right[i]) * kReverbInputGain;
    std::array<float, 2> out{};
    for (size_t side = 0; side < 2; ++side) {
      float sum = 0.0F;
      for (Line& comb : combs_[side]) {
        const float value = comb.buffer[comb.index];
        comb.store = value * damp2_ + comb.store * damp1_;
        comb.buffer[comb.index] = input + comb.store * feedback_;
        comb.index = comb.index + 1 == comb.buffer.size() ? 0 : comb.index + 1;
        sum += value;
      }
      for (Line& pass : allPasses_[side]) {
        const float buffered = pass.buffer[pass.index];
        pass.buffer[pass.index] = sum + buffered * kAllPassFeedback;
        sum = buffered - sum;
        pass.index = pass.index + 1 == pass.buffer.size() ? 0 : pass.index + 1;
      }
      out[side] = sum;
    }
    const float l = out[0] * wet1_ + out[1] * wet2_;
    const float r = out[1] * wet1_ + out[0] * wet2_;
    left[i] = left[i] * dry_ + l;
    right[i] = right[i] * dry_ + r;
  }
}

}  // namespace minidaw::engine
