// The JNI bridge of the checks V-03 and V-07 (architecture.md Section 12.2). Kotlin class:
// app.minidaw.engine.AudioPathProbe.
#include <jni.h>

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "../glue/audio_path_probe.h"
#include "../glue/latency_probe.h"

using minidaw::engine::glue::probeInput;
using minidaw::engine::glue::probeOutput;
using minidaw::engine::glue::probeTakeLatency;

extern "C" JNIEXPORT jstring JNICALL Java_app_minidaw_engine_AudioPathProbe_probeOutput(
    JNIEnv* env, jobject /* self */, jboolean performanceHint, jint seconds) {
  const std::string json = probeOutput(performanceHint == JNI_TRUE, static_cast<int32_t>(seconds));
  return env->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_app_minidaw_engine_AudioPathProbe_probeInput(JNIEnv* env, jobject /* self */) {
  const std::string json = probeInput();
  return env->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jstring JNICALL Java_app_minidaw_engine_AudioPathProbe_probeTakeLatency(
    JNIEnv* env, jobject /* self */, jfloatArray click) {
  const jsize size = env->GetArrayLength(click);
  std::vector<float> values(static_cast<size_t>(size));
  env->GetFloatArrayRegion(click, 0, size, values.data());
  const std::string json = probeTakeLatency(values);
  return env->NewStringUTF(json.c_str());
}
