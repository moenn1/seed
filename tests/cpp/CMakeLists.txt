cmake_minimum_required(VERSION 3.20)
include(FetchContent)

FetchContent_Declare(
  googletest
  URL https://github.com/google/googletest/archive/refs/tags/v1.14.0.zip
)
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

add_executable(test_sanity test_sanity.cpp)
target_link_libraries(test_sanity GTest::gtest_main seed_vm seed_runtime)

include(GoogleTest)
gtest_discover_tests(test_sanity)
