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

# The candidate SwiftF0 needs ONNX Runtime. The top CMakeLists.txt provides it on a host with
# linux-x64. The Android build adds it with the app for the checks (TK-010).
if(TARGET minidaw-onnxruntime)
  add_library(analysis-swiftf0 STATIC
    src/swiftf0.cpp)
  target_link_libraries(analysis-swiftf0 PUBLIC analysis-core PRIVATE minidaw-onnxruntime)
  target_compile_options(analysis-swiftf0 PRIVATE -Wall -Wextra -Wpedantic -Werror)
endif()
