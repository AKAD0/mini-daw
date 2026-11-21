#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <vector>

namespace minidaw::engine {

constexpr double kSampleRate = 48000.0;

// The windowed sinc of the sampler: 8 taps for each of 64 positions between two frames (D-085).
// The table holds a 65th row, the next frame, so that a Voice can blend two neighbouring positions.
class SincTable {
 public:
  static constexpr int kTaps = 8;
  static constexpr int kPositions = 64;

  SincTable();

  [[nodiscard]] const float* taps(int position) const {
    return &table_[static_cast<size_t>(position) * kTaps];
  }

 private:
  std::array<float, static_cast<size_t>(kTaps) * (kPositions + 1)> table_{};
};

// A stereo Sample with 3 guard frames of zero before its first frame and 4 after its last one, so
// that the 8 taps never need a test of the ends.
class GuardedSample {
 public:
  GuardedSample(const std::vector<float>& left, const std::vector<float>& right);

  [[nodiscard]] const float* left() const { return left_.data() + kBefore; }
  [[nodiscard]] const float* right() const { return right_.data() + kBefore; }
  [[nodiscard]] int64_t frames() const { return frames_; }

  static constexpr int kBefore = 3;
  static constexpr int kAfter = 4;

 private:
  std::vector<float> left_;
  std::vector<float> right_;
  int64_t frames_;
};

// The running values of a Voice. The position counts frames in 32.32 fixed point.
struct VoiceState {
  uint64_t position = 0;
  // The speed: 1.0 plays the Sample at its own rate.
  uint64_t step = uint64_t{1} << 32;
  float gainLeft = 1.0F;
  float gainRight = 1.0F;
  // Only for the check V-04: real Voices end at the end of the Sample.
  bool loop = false;
  bool active = true;
};

// The step for a speed, for example 2^(semitones / 12).
uint64_t stepOfSpeed(double speed);

// The inner loop of the Voice: adds `frames` frames of the Sample into both outputs. The taps are
// blended linearly between the two nearest of the 64 positions.
void renderVoice(const SincTable& table, const GuardedSample& sample, VoiceState& state,
                 float* outLeft, float* outRight, int frames);

// A second-order section by the formulas of the "Audio EQ Cookbook", transposed direct form II.
class Biquad {
 public:
  enum class Kind : uint8_t { kLowShelf, kPeak, kHighShelf };

  void set(Kind kind, double frequency, double gainDb, double q);
  void process(float* values, int frames, int side);

 private:
  double b0_ = 1.0;
  double b1_ = 0.0;
  double b2_ = 0.0;
  double a1_ = 0.0;
  double a2_ = 0.0;
  std::array<double, 2> z1_{};
  std::array<double, 2> z2_{};
};

// The effect eq3: a low shelf, a peak and a high shelf in a row (feature_specifications.md 3.10).
class Equalizer {
 public:
  struct Values {
    double lowGainDb = 0.0;
    double lowFrequency = 120.0;
    double midGainDb = 0.0;
    double midFrequency = 1000.0;
    double midQ = 0.9;
    double highGainDb = 0.0;
    double highFrequency = 8000.0;
  };

  void set(const Values& values);
  void process(float* left, float* right, int frames);

 private:
  std::array<Biquad, 3> bands_{};
};

// The effect delay: one line per side with a low-pass filter in the feedback path.
class Delay {
 public:
  struct Values {
    double timeMs = 350.0;
    // From 0 to 0.85.
    double feedback = 0.35;
    double dampingFrequency = 6000.0;
    double mix = 0.3;
  };

  Delay();
  void set(const Values& values);
  void process(float* left, float* right, int frames);

 private:
  std::array<std::vector<float>, 2> lines_;
  std::array<float, 2> lowPass_{};
  size_t write_ = 0;
  float delayFrames_ = 0.0F;
  float feedback_ = 0.0F;
  float damping_ = 0.0F;
  float wet_ = 0.0F;
  float dry_ = 1.0F;
};

// The effect reverb: eight parallel comb filters and four all-pass filters per side, after the
// Freeverb design.
class Reverb {
 public:
  struct Values {
    // The feedback of the combs goes from 0.70 to 0.98.
    double size = 0.5;
    double damping = 0.5;
    double width = 1.0;
    double mix = 0.25;
  };

  Reverb();
  void set(const Values& values);
  void process(float* left, float* right, int frames);

  static constexpr int kCombs = 8;
  static constexpr int kAllPasses = 4;

 private:
  struct Line {
    std::vector<float> buffer;
    size_t index = 0;
    float store = 0.0F;
  };

  std::array<std::array<Line, kCombs>, 2> combs_;
  std::array<std::array<Line, kAllPasses>, 2> allPasses_;
  float feedback_ = 0.0F;
  float damp1_ = 0.0F;
  float damp2_ = 0.0F;
  float wet1_ = 0.0F;
  float wet2_ = 0.0F;
  float dry_ = 1.0F;
};

}  // namespace minidaw::engine
