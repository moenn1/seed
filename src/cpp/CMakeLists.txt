add_library(seed_runtime STATIC
    runtime/runtime.cpp
)
target_include_directories(seed_runtime PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/../include)

add_library(seed_vm STATIC
    vm/vm_exec.cpp
    vm/loader.cpp
    vm/gc.cpp
)
target_include_directories(seed_vm PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/../include)
target_link_libraries(seed_vm PRIVATE seed_runtime)

add_library(seed_codegen STATIC
    codegen/a64_emit.cpp
)
target_include_directories(seed_codegen PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/../include)
target_link_libraries(seed_codegen PUBLIC seed_vm seed_runtime)

add_executable(seedvm apps/seedvm.cpp)
target_link_libraries(seedvm PRIVATE seed_vm seed_runtime)

add_executable(seeddasm apps/seeddasm.cpp)
target_link_libraries(seeddasm PRIVATE seed_vm)

add_executable(seedc apps/seedc.cpp)
target_link_libraries(seedc PRIVATE seed_codegen seed_vm seed_runtime)

if(ENABLE_LLVM)
  add_library(seed_llvm_backend STATIC
      codegen/llvm_backend.cpp
  )
  target_link_libraries(seed_llvm_backend PRIVATE seed_vm seed_runtime LLVM)
endif()

install(TARGETS seedvm RUNTIME DESTINATION bin)

if(ENABLE_LLVM)
  add_executable(seedjit apps/seedjit.cpp)
  target_link_libraries(seedjit PRIVATE seed_codegen seed_vm seed_runtime LLVM)
endif()
