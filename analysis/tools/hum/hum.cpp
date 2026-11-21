// minidaw-hum converts WAV files to Notes with one pitch candidate (TK-009).
//
//   minidaw-hum pyin FILE...
//   minidaw-hum swiftf0 MODEL FILE...
//
// The rate of each file must be a multiple of 16 kHz. The tool prints one line of JSON per file:
// the file, the range of pitch, the shift by octaves, the time of the work, the length of the
// audio, and the Notes as [start in s, end in s, pitch with a fraction].
#include <chrono>
#include <cstdio>
#include <exception>
#include <memory>
#include <string>
#include <string_view>
#include <vector>

#include "minidaw/analysis/notes.h"
#include "minidaw/analysis/pyin.h"
#include "minidaw/wav/wav.h"
#if defined(MINIDAW_HAVE_SWIFTF0)
#include "minidaw/analysis/swiftf0.h"
#endif

namespace {

using minidaw::analysis::NotesResult;
using minidaw::analysis::PitchDetector;
using minidaw::analysis::Range;

std::string jsonText(std::string_view text) {
  std::string out = "\"";
  for (const char c : text) {
    if (c == '"' || c == '\\') {
      out += '\\';
    }
    out += c;
  }
  return out + "\"";
}

void printResult(const std::string& file, const NotesResult& r, double seconds, double length) {
  std::printf("{\"file\": %s, \"range\": \"%s\", \"octaveShift\": %d, \"seconds\": %.4f, "
              "\"audioSeconds\": %.4f, \"notes\": [",
              jsonText(file).c_str(), r.range == Range::kWhistle ? "whistle" : "voice",
              r.octaveShift, seconds, length);
  for (std::size_t k = 0; k < r.notes.size(); ++k) {
    std::printf("%s[%.6f, %.6f, %.6f]", k == 0 ? "" : ", ", r.notes[k].start, r.notes[k].end,
                r.notes[k].pitch);
  }
  std::printf("]}\n");
}

int run(const std::vector<std::string>& args) {
  if (args.size() < 2) {
    std::fprintf(stderr, "usage: minidaw-hum pyin FILE... | minidaw-hum swiftf0 MODEL FILE...\n");
    return 2;
  }
  std::unique_ptr<PitchDetector> detector;
  std::size_t first = 1;
  if (args[0] == "pyin") {
    detector = std::make_unique<minidaw::analysis::Pyin>();
  } else if (args[0] == "swiftf0") {
#if defined(MINIDAW_HAVE_SWIFTF0)
    detector = std::make_unique<minidaw::analysis::SwiftF0>(args[1]);
    first = 2;
#else
    std::fprintf(stderr, "minidaw-hum: this build has no SwiftF0\n");
    return 2;
#endif
  } else {
    std::fprintf(stderr, "minidaw-hum: unknown candidate %s\n", args[0].c_str());
    return 2;
  }
  for (std::size_t i = first; i < args.size(); ++i) {
    const minidaw::wav::Audio audio = minidaw::wav::read(args[i]);
    const std::vector<float> samples = minidaw::wav::mono(audio);
    const auto start = std::chrono::steady_clock::now();
    const NotesResult r = minidaw::analysis::convertToNotes(samples, audio.sampleRate, *detector);
    const std::chrono::duration<double> took = std::chrono::steady_clock::now() - start;
    const double length = static_cast<double>(samples.size()) / audio.sampleRate;
    printResult(args[i], r, took.count(), length);
  }
  return 0;
}

}  // namespace

int main(int argc, char** argv) {
  try {
    const std::vector<std::string> args(argv + 1, argv + argc);
    return run(args);
  } catch (const std::exception& e) {
    std::fprintf(stderr, "minidaw-hum: %s\n", e.what());
  } catch (...) {
    std::fprintf(stderr, "minidaw-hum: unknown error\n");
  }
  return 1;
}
