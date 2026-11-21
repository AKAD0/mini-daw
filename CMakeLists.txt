# Host build of the portable native code (NFR-MNT-01, technology_stack.md Section 3.3).
# The Android build of the module :engine adds the same directory engine-core.
#   cmake -S . -B build/host -DMINIDAW_SANITIZERS=address,undefined
#   cmake --build build/host && ctest --test-dir build/host
cmake_minimum_required(VERSION 3.22)
project(minidaw_native LANGUAGES CXX)

if(POLICY CMP0135)
  cmake_policy(SET CMP0135 NEW)
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(MINIDAW_SANITIZERS "" CACHE STRING "Sanitizers of the host build, for example address,undefined")
if(MINIDAW_SANITIZERS)
  add_compile_options(-fsanitize=${MINIDAW_SANITIZERS} -fno-sanitize-recover=all -fno-omit-frame-pointer)
  add_link_options(-fsanitize=${MINIDAW_SANITIZERS})
endif()

# The candidate SwiftF0 needs ONNX Runtime (technology_stack.md Sections 8.2 and 15). On a host with
# linux-x64 the build takes the official package of ONNX Runtime 1.31.1 from NuGet.
option(MINIDAW_SWIFTF0 "Build the candidate SwiftF0 on the host" ON)
option(MINIDAW_REQUIRE_SWIFTF0 "Stop the configuration when SwiftF0 cannot be built (CI service)" OFF)
if(MINIDAW_SWIFTF0 AND CMAKE_SYSTEM_NAME STREQUAL "Linux" AND CMAKE_SYSTEM_PROCESSOR MATCHES "^(x86_64|AMD64)$")
  include(FetchContent)
  FetchContent_Declare(
    onnxruntime
    URL https://api.nuget.org/v3-flatcontainer/microsoft.ml.onnxruntime/1.31.1/microsoft.ml.onnxruntime.1.31.1.nupkg
    URL_HASH SHA256=6871ccbc97f3d5bdb4095a1dcec33aa78d56f6ed8de5980ef5fa1d9f2e7c679a
    DOWNLOAD_NAME onnxruntime-1.31.1.zip)
  FetchContent_MakeAvailable(onnxruntime)
  set(MINIDAW_ORT_LIB_DIR ${onnxruntime_SOURCE_DIR}/runtimes/linux-x64/native)
  # The library names itself libonnxruntime.so.1, and the loader looks for that name.
  file(CREATE_LINK libonnxruntime.so ${MINIDAW_ORT_LIB_DIR}/libonnxruntime.so.1 SYMBOLIC)
  add_library(minidaw-onnxruntime SHARED IMPORTED)
  set_target_properties(minidaw-onnxruntime PROPERTIES
    IMPORTED_LOCATION ${MINIDAW_ORT_LIB_DIR}/libonnxruntime.so
    IMPORTED_SONAME libonnxruntime.so.1
    INTERFACE_INCLUDE_DIRECTORIES ${onnxruntime_SOURCE_DIR}/build/native/include)
elseif(MINIDAW_REQUIRE_SWIFTF0)
  message(FATAL_ERROR "SwiftF0 needs ONNX Runtime, which this build has only for Linux on x86_64")
endif()

add_subdirectory(engine/src/main/cpp/engine-core)
add_subdirectory(analysis/src/main/cpp/analysis-core)

option(MINIDAW_TESTS "Build the tests of the native code" ON)
if(MINIDAW_TESTS)
  enable_testing()
  include(FetchContent)
  FetchContent_Declare(
    googletest
    URL https://github.com/google/googletest/releases/download/v1.17.0/googletest-1.17.0.tar.gz
    URL_HASH SHA256=65fab701d9829d38cb77c14acdc431d2108bfdbf8979e40eb8ae567edf10b27c)
  set(INSTALL_GTEST OFF CACHE BOOL "" FORCE)
  set(BUILD_GMOCK OFF CACHE BOOL "" FORCE)
  FetchContent_MakeAvailable(googletest)
  include(GoogleTest)
  add_subdirectory(engine/src/test/cpp)
  add_subdirectory(analysis/src/test/cpp)
endif()
