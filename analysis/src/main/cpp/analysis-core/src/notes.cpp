#include "minidaw/analysis/notes.h"

#include <algorithm>
#include <cmath>
#include <cstddef>

#include "minidaw/analysis/prepare.h"

namespace minidaw::analysis {
namespace {

double median(std::vector<double> values) {
  const auto middle = values.begin() + static_cast<std::ptrdiff_t>(values.size() / 2);
  std::nth_element(values.begin(), middle, values.end());
  if (values.size() % 2 == 1) {
    return *middle;
  }
  const double upper = *middle;
  const double lower = *std::max_element(values.begin(), middle);
  return (lower + upper) / 2.0;
}

double pitchNumber(double frequency) { return 69.0 + 12.0 * std::log2(frequency / 440.0); }

std::size_t framesFor(double seconds) {
  return static_cast<std::size_t>(std::lround(seconds / kStepSeconds));
}

// The lowest loudness of a voiced frame: the fixed limit, and at most a fixed distance below the
// loud parts of the Take. The loud parts are a high percentile of the loudness of all frames.
double lowestLoudness(std::span<const Frame> frames, const NoteParams& params) {
  std::vector<double> levels;
  levels.reserve(frames.size());
  for (const Frame& f : frames) {
    if (f.loudness > kSilenceDb) {
      levels.push_back(f.loudness);
    }
  }
  if (levels.empty()) {
    return params.minLoudnessDb;
  }
  std::sort(levels.begin(), levels.end());
  const double place = params.loudPartsPercentile * static_cast<double>(levels.size() - 1);
  const double loudParts = levels[static_cast<std::size_t>(std::lround(place))];
  return std::max(params.minLoudnessDb, loudParts - params.belowLoudPartsDb);
}

bool passes(const Frame& f, float minConfidence, double minLoudness) {
  return f.frequency > 0.0F && f.confidence >= minConfidence && f.loudness >= minLoudness;
}

// The values of one Note from the indices of its voiced frames (Section 4.2.5).
Note noteOf(const std::vector<std::size_t>& indices, const std::vector<double>& pitch,
            std::span<const Frame> frames) {
  Note note;
  note.start = static_cast<double>(indices.front()) * kStepSeconds;
  note.end = static_cast<double>(indices.back() + 1) * kStepSeconds;
  const std::size_t tenth = indices.size() / 10;
  std::vector<double> inner;
  inner.reserve(indices.size());
  for (std::size_t k = tenth; k < indices.size() - tenth; ++k) {
    inner.push_back(pitch[indices[k]]);
  }
  note.pitch = median(inner);
  double loudness = 0.0;
  for (const std::size_t i : indices) {
    loudness += frames[i].loudness;
  }
  note.loudness = loudness / static_cast<double>(indices.size());
  return note;
}

}  // namespace

NotesResult findNotes(std::span<const Frame> frames, float minConfidence,
                      const NoteParams& params) {
  NotesResult result;
  const double minLoudness = lowestLoudness(frames, params);

  // Choice of the range of pitch: the median frequency of all frames that pass the limits.
  std::vector<double> passing;
  for (const Frame& f : frames) {
    if (passes(f, minConfidence, minLoudness)) {
      passing.push_back(f.frequency);
    }
  }
  if (passing.empty()) {
    return result;
  }
  const bool whistle = median(passing) > params.whistleBorderHz;
  result.range = whistle ? Range::kWhistle : Range::kVoice;
  result.octaveShift = whistle ? kWhistleShiftSemitones : 0;
  const double low = whistle ? params.whistleMinHz : params.voiceMinHz;
  const double high = whistle ? params.whistleMaxHz : params.voiceMaxHz;

  // Steps 1 and 2: voiced frames and their pitch numbers.
  const std::size_t n = frames.size();
  std::vector<bool> voiced(n, false);
  std::vector<double> pitch(n, 0.0);
  for (std::size_t i = 0; i < n; ++i) {
    const Frame& f = frames[i];
    if (passes(f, minConfidence, minLoudness) && f.frequency >= low && f.frequency <= high) {
      voiced[i] = true;
      pitch[i] = pitchNumber(f.frequency);
    }
  }

  // Step 3: a median over the voiced frames among the 5 frames around each frame.
  const auto half = static_cast<std::size_t>(params.medianWidth / 2);
  std::vector<double> smooth(n, 0.0);
  for (std::size_t i = 0; i < n; ++i) {
    if (!voiced[i]) {
      continue;
    }
    std::vector<double> near;
    for (std::size_t j = i >= half ? i - half : 0; j <= std::min(n - 1, i + half); ++j) {
      if (voiced[j]) {
        near.push_back(pitch[j]);
      }
    }
    smooth[i] = median(near);
  }

  // Steps 4 to 7: one open Note at a time.
  const std::size_t bridged = framesFor(params.longestBridgedGapSeconds);
  const std::size_t change = framesFor(params.changeSeconds);
  std::vector<std::vector<std::size_t>> segments;
  std::vector<std::size_t> open;
  std::vector<double> openPitches;
  std::vector<std::size_t> deviating;
  std::size_t gap = 0;
  auto keepDeviating = [&] {
    for (const std::size_t d : deviating) {
      open.push_back(d);
      openPitches.push_back(smooth[d]);
    }
    deviating.clear();
  };
  auto close = [&] {
    keepDeviating();
    if (!open.empty()) {
      segments.push_back(open);
    }
    open.clear();
    openPitches.clear();
  };
  for (std::size_t i = 0; i < n; ++i) {
    if (!voiced[i]) {
      if (!open.empty()) {
        ++gap;
        if (gap > bridged) {
          close();
        }
      }
      continue;
    }
    gap = 0;
    if (open.empty()) {
      open.push_back(i);
      openPitches.push_back(smooth[i]);
      continue;
    }
    if (std::abs(smooth[i] - median(openPitches)) > params.changeSemitones) {
      deviating.push_back(i);
      if (deviating.size() >= change) {
        std::vector<std::size_t> next = deviating;
        deviating.clear();
        close();
        for (const std::size_t d : next) {
          open.push_back(d);
          openPitches.push_back(smooth[d]);
        }
      }
      continue;
    }
    keepDeviating();
    open.push_back(i);
    openPitches.push_back(smooth[i]);
  }
  close();

  // Step 9 and the values of each Note.
  for (const std::vector<std::size_t>& indices : segments) {
    const Note note = noteOf(indices, pitch, frames);
    if (note.end - note.start >= params.shortestNoteSeconds - 1e-9) {
      result.notes.push_back(note);
    }
  }
  return result;
}

NotesResult convertToNotes(std::span<const float> audio, int sampleRate, PitchDetector& detector,
                           const NoteParams& params) {
  const std::vector<float> prepared = prepare(audio, sampleRate);
  const std::vector<Frame> frames = detector.detect(prepared);
  return findNotes(frames, detector.minConfidence(), params);
}

}  // namespace minidaw::analysis
