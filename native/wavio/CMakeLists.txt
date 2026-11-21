# wavio: the small static library that reads and writes WAV files. engine-core and the native code
# of :analysis share it (architecture.md Section 2.1, rule 7). It includes no header of Android.
add_library(wavio STATIC
  src/wav.cpp)
target_include_directories(wavio PUBLIC include)
target_compile_features(wavio PUBLIC cxx_std_20)
target_compile_options(wavio PRIVATE -Wall -Wextra -Wpedantic -Werror)

