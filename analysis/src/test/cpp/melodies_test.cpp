// The criteria AC-H2M-01, AC-H2M-10 and AC-H2M-13 on the test melodies (feature_specifications.md
// Sections 4.8 and 4.9), for each pitch candidate.
#include <gtest/gtest.h>

#include <cmath>
#include <vector>

#include "minidaw/analysis/notes.h"
#include "minidaw/analysis/pyin.h"
#include "test_melodies.h"

namespace minidaw::analysis {
namespace {

constexpr double kStartTolerance = 0.050;

// AC-H2M-01: every Note is found, each pitch is exact, each start lies within 50 ms.
void expectEveryNote(PitchDetector& detector) {
  for (const auto& m : fixtures::testMelodies()) {
    const auto r = convertToNotes(m.audio, fixtures::kMelodyRate, detector);
    ASSERT_EQ(r.notes.size(), m.notes.size()) << m.name;
    for (std::size_t k = 0; k < m.notes.size(); ++k) {
      EXPECT_EQ(std::lround(r.notes[k].pitch), m.notes[k].pitch) << m.name << ", Note " << k;
      EXPECT_NEAR(r.notes[k].start, m.notes[k].start, kStartTolerance) << m.name << ", Note " << k;
    }
  }
}

// AC-H2M-10: two conversions of the same Take give the same Notes.
void expectSameNotesTwice(PitchDetector& detector) {
  for (const auto& m : fixtures::testMelodies()) {
    const auto a = convertToNotes(m.audio, fixtures::kMelodyRate, detector);
    const auto b = convertToNotes(m.audio, fixtures::kMelodyRate, detector);
    ASSERT_EQ(a.notes.size(), b.notes.size()) << m.name;
    for (std::size_t k = 0; k < a.notes.size(); ++k) {
      EXPECT_EQ(a.notes[k].start, b.notes[k].start) << m.name;
      EXPECT_EQ(a.notes[k].end, b.notes[k].end) << m.name;
      EXPECT_EQ(a.notes[k].pitch, b.notes[k].pitch) << m.name;
      EXPECT_EQ(a.notes[k].loudness, b.notes[k].loudness) << m.name;
    }
  }
}

// AC-H2M-13: a voice gives Notes at its own pitch, a whistle gives the melody two octaves lower.
void expectVoiceAndWhistle(PitchDetector& detector) {
  for (const auto& m : fixtures::testMelodies()) {
    const auto r = convertToNotes(m.audio, fixtures::kMelodyRate, detector);
    EXPECT_EQ(r.range, m.whistle ? Range::kWhistle : Range::kVoice) << m.name;
    const int shift = m.whistle ? -24 : 0;
    ASSERT_EQ(r.notes.size(), m.notes.size()) << m.name;
    for (std::size_t k = 0; k < m.notes.size(); ++k) {
      EXPECT_EQ(std::lround(r.notes[k].pitch) + r.octaveShift, m.notes[k].pitch + shift) << m.name;
    }
  }
}

// AC-H2M-02, the part of `:analysis`: silence and noise give no Notes.
void expectNothingFromSilenceAndNoise(PitchDetector& detector) {
  const std::vector<float> silence(3 * fixtures::kMelodyRate, 0.0F);
  EXPECT_TRUE(convertToNotes(silence, fixtures::kMelodyRate, detector).notes.empty());
  std::vector<float> noise(3 * fixtures::kMelodyRate);
  unsigned state = 12345U;
  for (float& x : noise) {
    state = state * 1664525U + 1013904223U;
    x = 0.2F * (static_cast<float>(state >> 8U) / 8388608.0F - 1.0F);
  }
  EXPECT_TRUE(convertToNotes(noise, fixtures::kMelodyRate, detector).notes.empty());
}

TEST(MelodiesPyin, EveryNoteIsFound) {
  Pyin pyin;
  expectEveryNote(pyin);
}

TEST(MelodiesPyin, TwoConversionsGiveTheSameNotes) {
  Pyin pyin;
  expectSameNotesTwice(pyin);
}

TEST(MelodiesPyin, VoiceKeepsItsPitchAndWhistleGoesTwoOctavesDown) {
  Pyin pyin;
  expectVoiceAndWhistle(pyin);
}

TEST(MelodiesPyin, SilenceAndNoiseGiveNoNotes) {
  Pyin pyin;
  expectNothingFromSilenceAndNoise(pyin);
}

}  // namespace
}  // namespace minidaw::analysis
