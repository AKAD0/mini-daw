#pragma once

#include "minidaw/analysis/pitch_detector.h"

namespace minidaw::analysis::fixtures {

// AC-H2M-01: every Note is found, each pitch is exact, each start lies within 50 ms.
void expectEveryNote(PitchDetector& detector);

// AC-H2M-10: two conversions of the same Take give the same Notes.
void expectSameNotesTwice(PitchDetector& detector);

// AC-H2M-13: a voice gives Notes at its own pitch, a whistle gives the melody two octaves lower.
void expectVoiceAndWhistle(PitchDetector& detector);

// AC-H2M-02, the part of `:analysis`: silence and noise give no Notes.
void expectNothingFromSilenceAndNoise(PitchDetector& detector);

}  // namespace minidaw::analysis::fixtures
