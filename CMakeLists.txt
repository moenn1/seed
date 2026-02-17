cmake_minimum_required(VERSION 3.20)
project(seed LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

option(ENABLE_LLVM "Enable LLVM-based backend and JIT" OFF)

if(ENABLE_LLVM)
  if(NOT DEFINED LLVM_DIR)
    message(STATUS "Tip: set -DLLVM_DIR=/opt/homebrew/opt/llvm/lib/cmake/llvm when using Homebrew on Apple Silicon")
  endif()
  find_package(LLVM REQUIRED CONFIG)
  message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
  include_directories(${LLVM_INCLUDE_DIRS})
  add_definitions(${LLVM_DEFINITIONS})
  add_definitions(-DENABLE_LLVM_JIT)
endif()

enable_testing()
add_subdirectory(src/cpp)
add_subdirectory(tests/cpp)

message(STATUS "Note: Java frontend is built separately with Maven in seed/java")