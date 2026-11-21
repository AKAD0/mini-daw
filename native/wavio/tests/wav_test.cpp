// Tests of the shared WAV library (architecture.md Section 2.1, rule 7).
#include <gtest/gtest.h>

#include <cstdint>
#include <stdexcept>
#include <string>
#include <vector>

#include "minidaw/wav/wav.h"

namespace minidaw::wav {
namespace {

void put16(std::vector<std::uint8_t>& b, unsigned v) {
  b.push_back(static_cast<std::uint8_t>(v & 0xFFU));
  b.push_back(static_cast<std::uint8_t>((v >> 8U) & 0xFFU));
}

void put32(std::vector<std::uint8_t>& b, unsigned v) {
  put16(b, v & 0xFFFFU);
  put16(b, v >> 16U);
}

void tag(std::vector<std::uint8_t>& b, const char* t) {
  for (int i = 0; i < 4; ++i) {
    b.push_back(static_cast<std::uint8_t>(t[i]));
  }
}

// A WAV file with PCM data; `extra` adds a chunk before the data, with an odd size.
std::vector<std::uint8_t> pcm(int bits, int channels, const std::vector<int>& values,
                              bool extensible = false, bool extra = false) {
  const unsigned bytesPerSample = static_cast<unsigned>(bits) / 8U;
  std::vector<std::uint8_t> data;
  for (const int v : values) {
    for (unsigned k = 0; k < bytesPerSample; ++k) {
      data.push_back(static_cast<std::uint8_t>((static_cast<unsigned>(v) >> (8U * k)) & 0xFFU));
    }
  }
  std::vector<std::uint8_t> b;
  tag(b, "RIFF");
  put32(b, 0);
  tag(b, "WAVE");
  tag(b, "fmt ");
  put32(b, extensible ? 40 : 16);
  put16(b, extensible ? 0xFFFEU : 1U);
  put16(b, static_cast<unsigned>(channels));
  put32(b, 48000);
  put32(b, 48000U * bytesPerSample * static_cast<unsigned>(channels));
  put16(b, bytesPerSample * static_cast<unsigned>(channels));
  put16(b, static_cast<unsigned>(bits));
  if (extensible) {
    put16(b, 22);
    put16(b, static_cast<unsigned>(bits));
    put32(b, 0);
    put16(b, 1);
    for (int i = 0; i < 14; ++i) {
      b.push_back(0);
    }
  }
  if (extra) {
    tag(b, "LIST");
    put32(b, 3);
    b.insert(b.end(), {'a', 'b', 'c', 0});
  }
  tag(b, "data");
  put32(b, static_cast<unsigned>(data.size()));
  b.insert(b.end(), data.begin(), data.end());
  return b;
}

TEST(Wav, Pcm16IsReadAsValuesFromMinusOneToOne) {
  const Audio a = parse(pcm(16, 1, {0, 16384, -32768, 32767}));
  EXPECT_EQ(a.sampleRate, 48000);
  EXPECT_EQ(a.channels, 1);
  ASSERT_EQ(a.samples.size(), 4U);
  EXPECT_FLOAT_EQ(a.samples[0], 0.0F);
  EXPECT_FLOAT_EQ(a.samples[1], 0.5F);
  EXPECT_FLOAT_EQ(a.samples[2], -1.0F);
  EXPECT_NEAR(a.samples[3], 1.0F, 1e-4F);
}

TEST(Wav, Pcm24AndExtensibleAndOtherChunksAreRead) {
  const Audio a = parse(pcm(24, 2, {4194304, -8388608}, true, true));
  EXPECT_EQ(a.channels, 2);
  ASSERT_EQ(a.samples.size(), 2U);
  EXPECT_FLOAT_EQ(a.samples[0], 0.5F);
  EXPECT_FLOAT_EQ(a.samples[1], -1.0F);
  EXPECT_FLOAT_EQ(mono(a)[0], -0.25F);
}

TEST(Wav, FloatDataSurvivesAWriteAndARead) {
  Audio a;
  a.sampleRate = 16000;
  a.channels = 1;
  a.samples = {0.0F, 0.25F, -0.75F, 1.0F};
  const Audio b = parse(encodeFloat(a));
  EXPECT_EQ(b.sampleRate, 16000);
  EXPECT_EQ(b.channels, 1);
  EXPECT_EQ(b.samples, a.samples);
}

TEST(Wav, BrokenDataIsRefused) {
  const std::vector<std::uint8_t> noise(64, 7);
  EXPECT_THROW(parse(noise), std::runtime_error);
  auto truncated = pcm(16, 1, {1, 2, 3});
  truncated.resize(truncated.size() - 4);
  EXPECT_THROW(parse(truncated), std::runtime_error);
  EXPECT_THROW(parse(pcm(8, 1, {1, 2})), std::runtime_error);
}

}  // namespace
}  // namespace minidaw::wav
