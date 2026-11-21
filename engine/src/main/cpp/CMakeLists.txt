# Native library of the module :engine for Android (architecture.md Section 2.1): engine-core, the glue
# for Oboe and the JNI bridge. The host build (CMakeLists.txt at the top) adds only engine-core.
cmake_minimum_required(VERSION 3.22.1)
project(minidaw_engine LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_subdirectory(engine-core)
find_package(oboe REQUIRED CONFIG)

add_library(minidaw-engine SHARED
  glue/audio_path_probe.cpp
  glue/latency_probe.cpp
  jni/audio_path_probe_jni.cpp)
target_link_libraries(minidaw-engine PRIVATE engine-core oboe::oboe log)
target_compile_options(minidaw-engine PRIVATE -Wall -Wextra -Werror)
# Pages of 16 KB (CON-11): the NDK r29 links so by default; checks/check_native_pages.py checks it.
