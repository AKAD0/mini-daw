# Tests of engine-core on the host (technology_stack.md Section 12, D-050).
add_executable(engine-core-tests
  kernels_test.cpp
  load_bench_test.cpp
  platform_test.cpp
  snapshot_test.cpp
  take_place_test.cpp)
target_link_libraries(engine-core-tests PRIVATE engine-core GTest::gtest_main)
target_compile_options(engine-core-tests PRIVATE -Wall -Wextra)
gtest_discover_tests(engine-core-tests DISCOVERY_MODE PRE_TEST)
