#pragma once
#include <cstddef>
#include <cstdint>
#include <vector>

namespace seed::gc {

// Heap object type tags (extend as needed)
enum class ObjType : std::uint8_t { String = 1, Array = 2 };

// GC lifecycle
void init();

// Allocate a GC-managed block of payload bytes with a given type tag.
// Returns a pointer to the payload region (the GC header is stored internally).
void* alloc(std::size_t payloadBytes, ObjType type);

// Run a simple mark-sweep collection. The optional roots vector contains
// payload pointers that should be considered live. In this initial slice we
// do not traverse object graphs (no child pointers), so only roots survive.
void collect(const std::vector<void*>& roots = {});

// Introspection
std::size_t heap_bytes();
std::size_t heap_objects();

} // namespace seed::gc