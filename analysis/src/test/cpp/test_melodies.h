#pragma once

#include <string>
#include <vector>

namespace minidaw::analysis::fixtures {

// Rate of the test melodies: the rate of the Library (D-037).
inline constexpr int kMelodyRate = 48000;

// A Note that a test melody holds: its start in seconds and its whole pitch (69 is 440 Hz).
struct ExpectedNote {
  double start = 0.0;
  int pitch = 0;
};

struct TestMelody {
  std::string name;
  bool whistle = false;
  std::vector<float> audio;
  std::vector<ExpectedNote> notes;
};

// The six kinds of test melodies of feature_specifications.md Section 4.8: tones with overtones,
// pure tones of a whistle, vibrato, slides, noise and pauses.
std::vector<TestMelody> testMelodies();

// Sine at one frequency, for tests of single parts.
std::vector<float> sine(double frequency, double seconds, int rate, double amplitude);

}  // namespace minidaw::analysis::fixtures
