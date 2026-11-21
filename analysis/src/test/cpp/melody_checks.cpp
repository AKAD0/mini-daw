// Checks of the criteria on the test melodies, shared by both pitch candidates.
#include "melody_checks.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <cmath>
#include <sstream>
#include <string>
#include <vector>

#include "minidaw/analysis/notes.h"
#include "minidaw/analysis/prepare.h"
#include "test_melodies.h"

namespace minidaw::analysis::fixtures {
namespace {

constexpr double kStartTolerance = 0.050;

float medianOf(std::vector<float> v) {
  if (v.empty()) {
    return 0.0F;
  }
  std::nth_element(v.begin(), v.begin() + static_cast<std::ptrdiff_t>(v.size() / 2), v.end());
  return v[v.size() / 2];
}

// What the detector saw, for the message of a failed check: over the frames with sound, and in
// the middle of each of the first tones.
std::string summary(PitchDetector& detector, const TestMelody& m) {
  const auto frames = detector.detect(prepare(m.audio, kMelodyRate));
  std::vector<float> confidence;
  std::size_t voiced = 0;
  for (const Frame& f : frames) {
    if (f.loudness > -40.0F) {
      confidence.push_back(f.confidence);
      voiced += f.confidence >= detector.minConfidence() ? 1 : 0;
    }
  }
  std::ostringstream out;
  out << "; frames with sound " << confidence.size() << ", voiced " << voiced
      << ", median confidence " << medianOf(confidence);
  for (std::size_t k = 0; k < std::min<std::size_t>(3, m.notes.size()); ++k) {
    const auto first = static_cast<std::size_t>((m.notes[k].start + 0.08) / kStepSeconds);
    std::vector<float> hz;
    std::vector<float> conf;
    for (std::size_t i = first; i < std::min(frames.size(), first + 8); ++i) {
      hz.push_back(frames[i].frequency);
      conf.push_back(frames[i].confidence);
    }
    out << "; tone " << k << ": expected " << 440.0 * std::exp2((m.notes[k].pitch - 69) / 12.0)
        << " Hz, found " << medianOf(hz) << " Hz at confidence " << medianOf(conf);
  }
  return out.str();
}

}  // namespace

// AC-H2M-01: every Note is found, each pitch is exact, each start lies within 50 ms.
void expectEveryNote(PitchDetector& detector) {
  for (const auto& m : testMelodies()) {
    const auto r = convertToNotes(m.audio, kMelodyRate, detector);
    ASSERT_EQ(r.notes.size(), m.notes.size()) << m.name << summary(detector, m);
    for (std::size_t k = 0; k < m.notes.size(); ++k) {
      EXPECT_EQ(std::lround(r.notes[k].pitch), m.notes[k].pitch) << m.name << ", Note " << k;
      EXPECT_NEAR(r.notes[k].start, m.notes[k].start, kStartTolerance) << m.name << ", Note " << k;
    }
  }
}

// AC-H2M-10: two conversions of the same Take give the same Notes.
void expectSameNotesTwice(PitchDetector& detector) {
  for (const auto& m : testMelodies()) {
    const auto a = convertToNotes(m.audio, kMelodyRate, detector);
    const auto b = convertToNotes(m.audio, kMelodyRate, detector);
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
  for (const auto& m : testMelodies()) {
    const auto r = convertToNotes(m.audio, kMelodyRate, detector);
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
  const std::vector<float> silence(3 * kMelodyRate, 0.0F);
  EXPECT_TRUE(convertToNotes(silence, kMelodyRate, detector).notes.empty());
  std::vector<float> noise(3 * kMelodyRate);
  unsigned state = 12345U;
  for (float& x : noise) {
    state = state * 1664525U + 1013904223U;
    x = 0.2F * (static_cast<float>(state >> 8U) / 8388608.0F - 1.0F);
  }
  EXPECT_TRUE(convertToNotes(noise, kMelodyRate, detector).notes.empty());
}

}  // namespace minidaw::analysis::fixtures
