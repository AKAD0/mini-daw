# engine-core: the portable part of the audio engine (architecture.md Section 2.1, rule 6).
# It includes no header of Android. The same target builds for Android and for a host.
add_library(engine-core STATIC
  src/engine.cpp
  src/format.cpp
  src/kernels.cpp
  src/load_bench.cpp
  src/platform.cpp
  src/snapshot.cpp
  src/take_place.cpp
  src/tone.cpp)
target_include_directories(engine-core PUBLIC include)
target_compile_features(engine-core PUBLIC cxx_std_20)
target_compile_options(engine-core PRIVATE -Wall -Wextra -Wpedantic -Werror)
