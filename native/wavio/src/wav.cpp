#include "minidaw/wav/wav.h"

#include <cstddef>
#include <cstring>
#include <fstream>
#include <iterator>
#include <stdexcept>

namespace minidaw::wav {
namespace {

constexpr std::uint16_t kPcm = 1;
constexpr std::uint16_t kFloat = 3;
constexpr std::uint16_t kExtensible = 0xFFFE;

std::uint32_t le32(std::span<const std::uint8_t> b, std::size_t at) {
  if (at + 4 > b.size()) {
    throw std::runtime_error("WAV: the data ends too early");
  }
  return static_cast<std::uint32_t>(b[at]) | (static_cast<std::uint32_t>(b[at + 1]) << 8U) |
         (static_cast<std::uint32_t>(b[at + 2]) << 16U) |
         (static_cast<std::uint32_t>(b[at + 3]) << 24U);
}

std::uint16_t le16(std::span<const std::uint8_t> b, std::size_t at) {
  if (at + 2 > b.size()) {
    throw std::runtime_error("WAV: the data ends too early");
  }
  return static_cast<std::uint16_t>(b[at] | (b[at + 1] << 8U));
}

bool isTag(std::span<const std::uint8_t> b, std::size_t at, const char* tag) {
  return at + 4 <= b.size() && std::memcmp(b.data() + at, tag, 4) == 0;
}

float sampleAt(std::span<const std::uint8_t> b, std::size_t at, std::uint16_t format, int bits) {
  if (format == kFloat) {
    const std::uint32_t raw = le32(b, at);
    float value = 0.0F;
    std::memcpy(&value, &raw, sizeof value);
    return value;
  }
  if (bits == 16) {
    return static_cast<float>(static_cast<std::int16_t>(le16(b, at))) / 32768.0F;
  }
  if (bits == 24) {
    std::uint32_t raw = static_cast<std::uint32_t>(b[at]) |
                        (static_cast<std::uint32_t>(b[at + 1]) << 8U) |
                        (static_cast<std::uint32_t>(b[at + 2]) << 16U);
    if ((raw & 0x800000U) != 0U) {
      raw |= 0xFF000000U;
    }
    return static_cast<float>(static_cast<std::int32_t>(raw)) / 8388608.0F;
  }
  return static_cast<float>(static_cast<double>(static_cast<std::int32_t>(le32(b, at))) /
                            2147483648.0);
}

void append16(std::vector<std::uint8_t>& b, std::uint16_t v) {
  b.push_back(static_cast<std::uint8_t>(v & 0xFFU));
  b.push_back(static_cast<std::uint8_t>(v >> 8U));
}

void append32(std::vector<std::uint8_t>& b, std::uint32_t v) {
  append16(b, static_cast<std::uint16_t>(v & 0xFFFFU));
  append16(b, static_cast<std::uint16_t>(v >> 16U));
}

void appendTag(std::vector<std::uint8_t>& b, const char* tag) { b.insert(b.end(), tag, tag + 4); }

}  // namespace

Audio parse(std::span<const std::uint8_t> bytes) {
  if (!isTag(bytes, 0, "RIFF") || !isTag(bytes, 8, "WAVE")) {
    throw std::runtime_error("WAV: no RIFF file of the type WAVE");
  }
  std::uint16_t format = 0;
  int bits = 0;
  Audio audio;
  std::size_t at = 12;
  while (at + 8 <= bytes.size()) {
    const std::uint32_t size = le32(bytes, at + 4);
    const std::size_t body = at + 8;
    if (body + size > bytes.size()) {
      throw std::runtime_error("WAV: a chunk is longer than the data");
    }
    if (isTag(bytes, at, "fmt ")) {
      format = le16(bytes, body);
      audio.channels = le16(bytes, body + 2);
      audio.sampleRate = static_cast<int>(le32(bytes, body + 4));
      bits = le16(bytes, body + 14);
      if (format == kExtensible) {
        format = le16(bytes, body + 24);
      }
    } else if (isTag(bytes, at, "data")) {
      const bool known = (format == kPcm && (bits == 16 || bits == 24 || bits == 32)) ||
                         (format == kFloat && bits == 32);
      if (!known || audio.channels < 1 || audio.sampleRate < 1) {
        throw std::runtime_error("WAV: only PCM of 16, 24 or 32 bits and float of 32 bits");
      }
      const auto step = static_cast<std::size_t>(bits / 8);
      const std::size_t count = size / step;
      audio.samples.resize(count);
      for (std::size_t i = 0; i < count; ++i) {
        audio.samples[i] = sampleAt(bytes, body + i * step, format, bits);
      }
      return audio;
    }
    at = body + size + (size % 2);
  }
  throw std::runtime_error("WAV: no chunk with data");
}

Audio read(const std::string& path) {
  std::ifstream in(path, std::ios::binary);
  if (!in) {
    throw std::runtime_error("WAV: cannot open " + path);
  }
  const std::vector<std::uint8_t> bytes((std::istreambuf_iterator<char>(in)),
                                        std::istreambuf_iterator<char>());
  return parse(bytes);
}

std::vector<float> mono(const Audio& audio) {
  const auto channels = static_cast<std::size_t>(audio.channels);
  if (channels <= 1) {
    return audio.samples;
  }
  std::vector<float> out(audio.samples.size() / channels, 0.0F);
  for (std::size_t i = 0; i < out.size(); ++i) {
    double sum = 0.0;
    for (std::size_t c = 0; c < channels; ++c) {
      sum += audio.samples[i * channels + c];
    }
    out[i] = static_cast<float>(sum / static_cast<double>(channels));
  }
  return out;
}

std::vector<std::uint8_t> encodeFloat(const Audio& audio) {
  const auto channels = static_cast<std::uint16_t>(audio.channels);
  const auto rate = static_cast<std::uint32_t>(audio.sampleRate);
  const auto dataSize = static_cast<std::uint32_t>(audio.samples.size() * sizeof(float));
  std::vector<std::uint8_t> b;
  b.reserve(44 + dataSize);
  appendTag(b, "RIFF");
  append32(b, 36 + dataSize);
  appendTag(b, "WAVE");
  appendTag(b, "fmt ");
  append32(b, 16);
  append16(b, kFloat);
  append16(b, channels);
  append32(b, rate);
  append32(b, rate * channels * 4U);
  append16(b, static_cast<std::uint16_t>(channels * 4U));
  append16(b, 32);
  appendTag(b, "data");
  append32(b, dataSize);
  for (const float s : audio.samples) {
    std::uint32_t raw = 0;
    std::memcpy(&raw, &s, sizeof raw);
    append32(b, raw);
  }
  return b;
}

void writeFloat(const std::string& path, const Audio& audio) {
  const std::vector<std::uint8_t> bytes = encodeFloat(audio);
  std::ofstream out(path, std::ios::binary);
  const auto size = static_cast<std::streamsize>(bytes.size());
  out.write(reinterpret_cast<const char*>(bytes.data()), size);
  if (!out) {
    throw std::runtime_error("WAV: cannot write " + path);
  }
}

}  // namespace minidaw::wav
