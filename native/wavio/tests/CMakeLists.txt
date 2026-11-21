# Tests of wavio on the host (D-050).
add_executable(wavio-tests
  wav_test.cpp)
target_link_libraries(wavio-tests PRIVATE wavio GTest::gtest_main)
target_compile_options(wavio-tests PRIVATE -Wall -Wextra)
gtest_discover_tests(wavio-tests DISCOVERY_MODE PRE_TEST)
