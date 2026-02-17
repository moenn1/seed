#pragma once
#include <cstddef>
namespace seed { namespace gc {
void init();
void collect();
std::size_t heap_bytes();
}} // namespace seed::gc
