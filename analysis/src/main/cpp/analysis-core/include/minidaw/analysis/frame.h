#pragma once

#include <cstddef>
#include <span>

namespace minidaw::analysis {

// Rate and step of the detection of pitch (feature_specifications.md Sections 4.2.3 and 4.2.4).
inline constexpr int kAnalysisRate = 16000;
inline constexpr std::size_t kHop = 256;
inline constexpr double kStepSeconds = 0.016;

// Loudness of a frame without sound.
inline constexpr float kSilenceDb = -120.0F;

// One frame of the detection of pitch. Frame i lies at the time i * kStepSeconds.
struct Frame {
  // The pitch in Hz. It is 0 when the frame has no pitch.
  float frequency = 0.0F;
  // A number from 0 to 1.
  float confidence = 0.0F;
  // The level of the frame in dBFS.
  float loudness = kSilenceDb;
};

// Number of frames for a signal at 16 kHz. Frame i is centered on the sample i * kHop.
std::size_t frameCount(std::size_t samples);

// Level in dBFS of the 32 ms of audio centered on the frame. Both candidates use it.
float frameLoudness(std::span<const float> audio, std::size_t frame);

}  // namespace minidaw::analysis
