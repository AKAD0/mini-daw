# analysis-core: the portable part of the conversion of humming (architecture.md Section 9).
# It includes no header of Android. The same target builds for Android and for a host.
add_library(analysis-core STATIC
  src/frame.cpp
  src/notes.cpp
  src/prepare.cpp
  src/pyin.cpp)
target_include_directories(analysis-core PUBLIC include)
target_compile_features(analysis-core PUBLIC cxx_std_20)
target_compile_options(analysis-core PRIVATE -Wall -Wextra -Wpedantic -Werror)
