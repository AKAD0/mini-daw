// The JNI bridge of the check V-05 (TK-010). Kotlin class: app.minidaw.check.humming.HumProbe.
#include <jni.h>

#include <array>
#include <chrono>
#include <cstddef>
#include <cstdio>
#include <exception>
#include <memory>
#include <string>
#include <vector>

#include "minidaw/analysis/notes.h"
#include "minidaw/analysis/pitch_detector.h"
#include "minidaw/analysis/pyin.h"
#include "minidaw/analysis/swiftf0.h"

namespace {

using Clock = std::chrono::steady_clock;
using minidaw::analysis::NotesResult;

constexpr int kSwiftF0 = 1;
constexpr double kMillisPerSecond = 1000.0;

std::string number(double value) {
  std::array<char, 32> buffer{};
  std::snprintf(buffer.data(), buffer.size(), "%.4f", value);
  return buffer.data();
}

// One JSON object: the candidate, the times, the range and the Notes as [start, end, pitch].
std::string resultText(int candidate, double loadMillis, double convertMillis,
                       const NotesResult& result) {
  std::string out = "{\"candidate\":\"";
  out += candidate == kSwiftF0 ? "SwiftF0" : "pYIN";
  out += "\",\"loadMs\":" + number(loadMillis);
  out += ",\"convertMs\":" + number(convertMillis);
  out += ",\"octaveShift\":" + std::to_string(result.octaveShift);
  out += ",\"notes\":[";
  for (size_t i = 0; i < result.notes.size(); ++i) {
    const auto& note = result.notes[i];
    out += i == 0 ? "[" : ",[";
    out += number(note.start) + "," + number(note.end) + "," + number(note.pitch) + "]";
  }
  return out + "]}";
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL Java_app_minidaw_check_humming_HumProbe_convert(
    JNIEnv* env, jobject /* self */, jfloatArray audio, jint rate, jint candidate, jstring model) {
  const jsize size = env->GetArrayLength(audio);
  std::vector<float> samples(static_cast<size_t>(size));
  env->GetFloatArrayRegion(audio, 0, size, samples.data());
  const char* path = env->GetStringUTFChars(model, nullptr);
  const std::string modelPath(path);
  env->ReleaseStringUTFChars(model, path);
  std::string text;
  try {
    const auto loading = Clock::now();
    std::unique_ptr<minidaw::analysis::PitchDetector> detector;
    if (candidate == kSwiftF0) {
      detector = std::make_unique<minidaw::analysis::SwiftF0>(modelPath);
    } else {
      detector = std::make_unique<minidaw::analysis::Pyin>();
    }
    const std::chrono::duration<double> loaded = Clock::now() - loading;
    const auto converting = Clock::now();
    const NotesResult result = minidaw::analysis::convertToNotes(samples, rate, *detector);
    const std::chrono::duration<double> converted = Clock::now() - converting;
    text = resultText(candidate, loaded.count() * kMillisPerSecond,
                      converted.count() * kMillisPerSecond, result);
  } catch (const std::exception& error) {
    text = std::string("{\"error\":\"") + error.what() + "\"}";
  }
  return env->NewStringUTF(text.c_str());
}
