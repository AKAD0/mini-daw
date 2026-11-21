# Native library of :checkapps:humming (TK-010): analysis-core, SwiftF0 and the JNI bridge.
cmake_minimum_required(VERSION 3.22.1)
project(minidaw_humming LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

get_filename_component(MINIDAW_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../../../../.. ABSOLUTE)

# ONNX Runtime from the AAR (build.gradle.kts takes it out).
add_library(minidaw-onnxruntime SHARED IMPORTED)
set_target_properties(minidaw-onnxruntime PROPERTIES
  IMPORTED_LOCATION ${MINIDAW_ORT_DIR}/jni/${ANDROID_ABI}/libonnxruntime.so
  INTERFACE_INCLUDE_DIRECTORIES ${MINIDAW_ORT_DIR}/headers)

add_subdirectory(${MINIDAW_ROOT}/analysis/src/main/cpp/analysis-core analysis-core)

add_library(minidaw-humming SHARED
  hum_probe_jni.cpp)
target_link_libraries(minidaw-humming PRIVATE analysis-swiftf0 analysis-core log)
target_compile_options(minidaw-humming PRIVATE -Wall -Wextra -Werror)
