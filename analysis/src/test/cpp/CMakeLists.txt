# Tests of analysis-core on the host (feature_specifications.md Section 4.8, D-050).
add_executable(analysis-core-tests
  melodies_test.cpp
  melody_checks.cpp
  notes_test.cpp
  prepare_test.cpp
  pyin_test.cpp
  test_melodies.cpp)
target_link_libraries(analysis-core-tests PRIVATE analysis-core GTest::gtest_main)
target_compile_options(analysis-core-tests PRIVATE -Wall -Wextra)
gtest_discover_tests(analysis-core-tests DISCOVERY_MODE PRE_TEST)

# The candidate SwiftF0 runs where the build has ONNX Runtime (CMakeLists.txt at the top).
# ONNX Runtime is a library without the sanitizers. Its own allocations at the end of the
# process are not leaks of this project, so the suppressions name only this library.
if(TARGET analysis-swiftf0)
  add_executable(analysis-swiftf0-tests
    melody_checks.cpp
    swiftf0_test.cpp
    test_melodies.cpp)
  target_link_libraries(analysis-swiftf0-tests PRIVATE analysis-swiftf0 GTest::gtest_main)
  target_compile_definitions(analysis-swiftf0-tests PRIVATE
    MINIDAW_SWIFTF0_MODEL="${PROJECT_SOURCE_DIR}/analysis/src/main/assets/swiftf0/swiftf0-0.3.0.onnx")
  target_compile_options(analysis-swiftf0-tests PRIVATE -Wall -Wextra)
  gtest_discover_tests(analysis-swiftf0-tests DISCOVERY_MODE PRE_TEST
    PROPERTIES ENVIRONMENT "LSAN_OPTIONS=suppressions=${PROJECT_SOURCE_DIR}/checks/lsan_suppressions.txt")
endif()
