// Tests of the code for Notes on exact frames (feature_specifications.md Section 4.2.5).
#include "minidaw/analysis/notes.h"

#include <gtest/gtest.h>

#include <cmath>
#include <vector>

namespace minidaw::analysis {
namespace {

constexpr float kMin = 0.5F;

double hz(double pitch) { return 440.0 * std::exp2((pitch - 69.0) / 12.0); }

// One frame per value: a pitch number gives a clear voiced frame, a value below 0 a silent frame.
std::vector<Frame> framesOf(const std::vector<double>& pitches, float loudness = -20.0F) {
  std::vector<Frame> frames;
  for (const double p : pitches) {
    Frame f;
    if (p >= 0.0) {
      f.frequency = static_cast<float>(hz(p));
      f.confidence = 1.0F;
      f.loudness = loudness;
    }
    frames.push_back(f);
  }
  return frames;
}

std::vector<double> run(double pitch, int count) { return std::vector<double>(count, pitch); }

std::vector<double> join(std::initializer_list<std::vector<double>> parts) {
  std::vector<double> all;
  for (const auto& part : parts) {
    all.insert(all.end(), part.begin(), part.end());
  }
  return all;
}

std::size_t notesIn(const std::vector<double>& pitches) {
  return findNotes(framesOf(pitches), kMin).notes.size();
}

TEST(Notes, OneToneGivesOneNoteWithItsTimes) {
  const auto r = findNotes(framesOf(join({run(-1, 5), run(69, 20), run(-1, 5)})), kMin);
  ASSERT_EQ(r.notes.size(), 1U);
  EXPECT_DOUBLE_EQ(r.notes[0].start, 5 * kStepSeconds);
  EXPECT_DOUBLE_EQ(r.notes[0].end, 25 * kStepSeconds);
  EXPECT_NEAR(r.notes[0].pitch, 69.0, 1e-4);
  EXPECT_NEAR(r.notes[0].loudness, -20.0, 1e-9);
}

TEST(Notes, AGapOf48MsIsBridgedAndALongerGapClosesTheNote) {
  EXPECT_EQ(notesIn(join({run(69, 10), run(-1, 3), run(69, 10)})), 1U);
  EXPECT_EQ(notesIn(join({run(69, 10), run(-1, 4), run(69, 10)})), 2U);
}

TEST(Notes, AChangeOfPitchSplitsAtItsFirstFrame) {
  const auto r = findNotes(framesOf(join({run(69, 15), run(71, 15)})), kMin);
  ASSERT_EQ(r.notes.size(), 2U);
  EXPECT_DOUBLE_EQ(r.notes[1].start, 15 * kStepSeconds);
  EXPECT_NEAR(r.notes[0].pitch, 69.0, 1e-4);
  EXPECT_NEAR(r.notes[1].pitch, 71.0, 1e-4);
}

TEST(Notes, AShortDeviationAndASingleOctaveJumpDoNotSplit) {
  EXPECT_EQ(notesIn(join({run(69, 10), run(71, 2), run(69, 10)})), 1U);
  const auto r = findNotes(framesOf(join({run(69, 10), run(81, 1), run(69, 10)})), kMin);
  ASSERT_EQ(r.notes.size(), 1U);
  EXPECT_NEAR(r.notes[0].pitch, 69.0, 1e-4);
}

// The open Note compares with the median of its frames so far. At the start of a Note this median
// can lie off the center of a vibrato, so a vibrato of 0.5 semitones is the widest that is safe.
TEST(Notes, AVibratoWithinTheLimitDoesNotSplit) {
  std::vector<double> pitches;
  for (int i = 0; i < 60; ++i) {
    pitches.push_back(69.0 + 0.5 * std::sin(i * 0.6));
  }
  EXPECT_EQ(findNotes(framesOf(pitches), kMin).notes.size(), 1U);
}

TEST(Notes, ANoteShorterThan80MsIsDropped) {
  EXPECT_TRUE(findNotes(framesOf(join({run(-1, 3), run(69, 4), run(-1, 3)})), kMin).notes.empty());
  EXPECT_EQ(findNotes(framesOf(join({run(-1, 3), run(69, 5), run(-1, 3)})), kMin).notes.size(), 1U);
}

TEST(Notes, QuietFramesAndFramesOfLowConfidenceAreNotVoiced) {
  EXPECT_TRUE(findNotes(framesOf(run(69, 20), -60.0F), kMin).notes.empty());
  // The second tone lies 35 dB below the loud parts: above -50 dBFS, but more than 30 dB below.
  auto frames = framesOf(join({run(69, 20), run(-1, 5), run(72, 20)}), -10.0F);
  for (std::size_t i = 25; i < frames.size(); ++i) {
    frames[i].loudness = -45.0F;
  }
  EXPECT_EQ(findNotes(frames, kMin).notes.size(), 1U);
  auto doubtful = framesOf(run(69, 20));
  for (Frame& f : doubtful) {
    f.confidence = 0.4F;
  }
  EXPECT_TRUE(findNotes(doubtful, kMin).notes.empty());
}

TEST(Notes, TheMedianFrequencyChoosesVoiceOrWhistle) {
  const auto voice = findNotes(framesOf(run(57, 20)), kMin);
  EXPECT_EQ(voice.range, Range::kVoice);
  EXPECT_EQ(voice.octaveShift, 0);
  const auto whistle = findNotes(framesOf(run(86, 20)), kMin);
  EXPECT_EQ(whistle.range, Range::kWhistle);
  EXPECT_EQ(whistle.octaveShift, -24);
  ASSERT_EQ(whistle.notes.size(), 1U);
  EXPECT_NEAR(whistle.notes[0].pitch, 86.0, 1e-4);
}

TEST(Notes, FramesOutsideTheChosenRangeCountAsWithoutPitch) {
  // Pitch 30 is 46 Hz, below the range of a voice.
  const auto r = findNotes(framesOf(join({run(57, 30), run(-1, 5), run(30, 10)})), kMin);
  EXPECT_EQ(r.notes.size(), 1U);
}

}  // namespace
}  // namespace minidaw::analysis
