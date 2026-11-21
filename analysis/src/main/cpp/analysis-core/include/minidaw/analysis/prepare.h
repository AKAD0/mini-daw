#pragma once

#include <span>
#include <vector>

namespace minidaw::analysis {

// Step 1 of the conversion (feature_specifications.md Section 4.2.3): the audio goes to 16 kHz,
// and a high-pass filter at 60 Hz removes rumble. The rate must be a multiple of 16 kHz, for
// example the 48 kHz of the Library (D-037). Another rate throws std::invalid_argument.
std::vector<float> prepare(std::span<const float> audio, int sampleRate);

}  // namespace minidaw::analysis
