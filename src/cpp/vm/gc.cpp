#include "seed/gc.h"
#include <atomic>
namespace seed { namespace gc {
static std::atomic<std::size_t> heap{0};
void init() { heap = 0; }
void collect() {}
std::size_t heap_bytes() { return heap.load(); }
}} // namespace seed::gc
