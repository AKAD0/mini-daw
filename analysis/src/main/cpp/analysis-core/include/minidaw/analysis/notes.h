#pragma once

#include <cstdint>
#include <span>
#include <vector>

#include "minidaw/analysis/frame.h"
#include "minidaw/analysis/pitch_detector.h"

namespace minidaw::analysis {

// Parameters of the code for Notes (feature_specifications.md Section 4.2.5). All values are
// provisional. The check V-05 tunes them.
struct NoteParams {
  double longestBridgedGapSeconds = 0.048;
  double changeSemitones = 0.8;
  double changeSeconds = 0.048;
  double shortestNoteSeconds = 0.080;
  double minLoudnessDb = -50.0;
  double belowLoudPartsDb = 30.0;
  double loudPartsPercentile = 0.95;
  double voiceMinHz = 60.0;
  double voiceMaxHz = 1047.0;
  double whistleMinHz = 500.0;
  double whistleMaxHz = 2094.0;
  double whistleBorderHz = 800.0;
  int medianWidth = 5;
};

// One Note with times in seconds and a pitch with a fraction (69 is the A at 440 Hz).
struct Note {
  double start = 0.0;
  double end = 0.0;
  double pitch = 0.0;
  double loudness = 0.0;
};

// The range of pitch that the Take uses (FR-H2M-01).
enum class Range : std::uint8_t { kVoice, kWhistle };

// Shift that `:core` applies to the Notes of a whistle (Section 4.2.6): two octaves down.
inline constexpr int kWhistleShiftSemitones = -24;

struct NotesResult {
  std::vector<Note> notes;
  Range range = Range::kVoice;
  // Semitones that `:core` adds to each Note: 0 for a voice, kWhistleShiftSemitones for a whistle.
  int octaveShift = 0;
};

// Step 3 of the conversion: cuts the frames into Notes (Section 4.2.5).
NotesResult findNotes(std::span<const Frame> frames, float minConfidence,
                      const NoteParams& params = {});

// Steps 1 to 3 of the conversion (Section 4.2.3) for mono audio at the given rate.
NotesResult convertToNotes(std::span<const float> audio, int sampleRate, PitchDetector& detector,
                           const NoteParams& params = {});

}  // namespace minidaw::analysis
