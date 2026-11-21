// The criteria AC-H2M-01, AC-H2M-10 and AC-H2M-13 on the test melodies (feature_specifications.md
// Sections 4.8 and 4.9) for the candidate pYIN.
#include <gtest/gtest.h>

#include "melody_checks.h"
#include "minidaw/analysis/pyin.h"

namespace minidaw::analysis {
namespace {

TEST(MelodiesPyin, EveryNoteIsFound) {
  Pyin pyin;
  fixtures::expectEveryNote(pyin);
}

TEST(MelodiesPyin, TwoConversionsGiveTheSameNotes) {
  Pyin pyin;
  fixtures::expectSameNotesTwice(pyin);
}

TEST(MelodiesPyin, VoiceKeepsItsPitchAndWhistleGoesTwoOctavesDown) {
  Pyin pyin;
  fixtures::expectVoiceAndWhistle(pyin);
}

TEST(MelodiesPyin, SilenceAndNoiseGiveNoNotes) {
  Pyin pyin;
  fixtures::expectNothingFromSilenceAndNoise(pyin);
}

}  // namespace
}  // namespace minidaw::analysis
