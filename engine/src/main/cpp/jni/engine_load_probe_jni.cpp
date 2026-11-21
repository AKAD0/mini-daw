// The JNI bridge of the check V-04 (architecture.md Section 12.2). Kotlin class:
// app.minidaw.engine.EngineLoadProbe.
#include <jni.h>

#include <cstddef>
#include <cstdint>
#include <span>
#include <string>
#include <vector>

#include "../glue/load_probe.h"

namespace {

std::vector<float> floats(JNIEnv* env, jfloatArray array) {
  const jsize size = env->GetArrayLength(array);
  std::vector<float> values(static_cast<size_t>(size));
  env->GetFloatArrayRegion(array, 0, size, values.data());
  return values;
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL Java_app_minidaw_engine_EngineLoadProbe_probeLoad(
    JNIEnv* env, jobject /* self */, jint project, jint seconds, jfloatArray sample) {
  const std::string json = minidaw::engine::glue::probeLoad(project, seconds, floats(env, sample));
  return env->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jstring JNICALL Java_app_minidaw_engine_EngineLoadProbe_probeKernels(
    JNIEnv* env, jobject /* self */, jfloatArray sample) {
  const std::string json = minidaw::engine::glue::probeKernels(floats(env, sample));
  return env->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jstring JNICALL Java_app_minidaw_engine_EngineLoadProbe_probeSnapshotLoad(
    JNIEnv* env, jobject /* self */, jbyteArray bytes, jint runs) {
  const jsize size = env->GetArrayLength(bytes);
  std::vector<uint8_t> values(static_cast<size_t>(size));
  env->GetByteArrayRegion(bytes, 0, size, reinterpret_cast<jbyte*>(values.data()));
  const std::string json = minidaw::engine::glue::probeSnapshotLoad(values, runs);
  return env->NewStringUTF(json.c_str());
}
