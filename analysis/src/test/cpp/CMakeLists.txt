# Tests of analysis-core on the host (feature_specifications.md Section 4.8, D-050).
add_executable(analysis-core-tests
  melodies_test.cpp
  notes_test.cpp
  prepare_test.cpp
  pyin_test.cpp
  test_melodies.cpp)
target_link_libraries(analysis-core-tests PRIVATE analysis-core GTest::gtest_main)
target_compile_options(analysis-core-tests PRIVATE -Wall -Wextra)
gtest_discover_tests(analysis-core-tests DISCOVERY_MODE PRE_TEST)
