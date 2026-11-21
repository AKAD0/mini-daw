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

add_subdirectory(engine/src/main/cpp/engine-core)

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
endif()
