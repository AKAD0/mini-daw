// The JNI bridge of the playback of the check V-05 (architecture.md Section 12.2). Kotlin class:
// app.minidaw.engine.TonePlayer.
#include <jni.h>

#include <cstddef>
#include <vector>

#include "minidaw/engine/tone.h"

extern "C" JNIEXPORT jfloatArray JNICALL Java_app_minidaw_engine_TonePlayer_render(
    JNIEnv* env, jobject /* self */, jdoubleArray notes, jfloatArray sample, jdouble rootNote) {
  const jsize noteValues = env->GetArrayLength(notes);
  std::vector<double> values(static_cast<size_t>(noteValues));
  env->GetDoubleArrayRegion(notes, 0, noteValues, values.data());
  std::vector<minidaw::engine::ToneNote> tones;
  for (size_t i = 0; i + 2 < values.size(); i += 3) {
    tones.push_back({values[i], values[i + 1], values[i + 2]});
  }
  const jsize sampleSize = env->GetArrayLength(sample);
  std::vector<float> keys(static_cast<size_t>(sampleSize));
  env->GetFloatArrayRegion(sample, 0, sampleSize, keys.data());
  constexpr double kReleaseSeconds = 0.05;
  using minidaw::engine::renderTone;
  const std::vector<float> audio = renderTone(tones, keys, rootNote, kReleaseSeconds);
  const auto size = static_cast<jsize>(audio.size());
  jfloatArray result = env->NewFloatArray(size);
  env->SetFloatArrayRegion(result, 0, size, audio.data());
  return result;
}
