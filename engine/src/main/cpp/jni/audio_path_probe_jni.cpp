// The JNI bridge of the check V-03 (architecture.md Section 12.2). Kotlin class:
// app.minidaw.engine.AudioPathProbe.
#include <jni.h>

#include <cstdint>
#include <string>

#include "../glue/audio_path_probe.h"

using minidaw::engine::glue::probeInput;
using minidaw::engine::glue::probeOutput;

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
