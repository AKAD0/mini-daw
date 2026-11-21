#pragma once

#include <cstdint>
#include <span>
#include <string>
#include <vector>

namespace minidaw::wav {

// Audio of a WAV file: samples from -1 to 1, interleaved by channel.
struct Audio {
  std::vector<float> samples;
  int sampleRate = 0;
  int channels = 0;
};

// Reads WAV data with PCM of 16, 24 or 32 bits or with float of 32 bits, also in the format
// WAVE_FORMAT_EXTENSIBLE. Other data throws std::runtime_error.
Audio parse(std::span<const std::uint8_t> bytes);
Audio read(const std::string& path);

// The mean of all channels.
std::vector<float> mono(const Audio& audio);

// WAV data with float of 32 bits.
std::vector<std::uint8_t> encodeFloat(const Audio& audio);
void writeFloat(const std::string& path, const Audio& audio);

}  // namespace minidaw::wav
