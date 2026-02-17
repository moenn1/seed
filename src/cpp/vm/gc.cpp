#include "seed/gc.h"
#include <atomic>
#include <cstdint>
#include <cstdlib>
#include <new>

namespace seed { namespace gc {

struct Header {
  Header* next{nullptr};
  std::size_t size{0};   // payload bytes
  std::uint8_t mark{0};  // mark bit
  std::uint8_t type{0};  // ObjType as u8
};

static Header* g_head = nullptr;
static std::atomic<std::size_t> g_bytes{0};
static std::atomic<std::size_t> g_objs{0};

static inline Header* header_from_payload(void* p) {
  if (!p) return nullptr;
  return reinterpret_cast<Header*>(reinterpret_cast<std::uint8_t*>(p) - sizeof(Header));
}

static inline void* payload_from_header(Header* h) {
  return reinterpret_cast<void*>(reinterpret_cast<std::uint8_t*>(h) + sizeof(Header));
}

void init() {
  // Free all existing blocks and reset accounting
  Header* h = g_head;
  while (h) {
    Header* nxt = h->next;
    std::free(h);
    h = nxt;
  }
  g_head = nullptr;
  g_bytes.store(0);
  g_objs.store(0);
}

void* alloc(std::size_t payloadBytes, ObjType type) {
  std::size_t total = sizeof(Header) + payloadBytes;
  Header* h = reinterpret_cast<Header*>(std::malloc(total));
  if (!h) throw std::bad_alloc();
  h->size = payloadBytes;
  h->mark = 0;
  h->type = static_cast<std::uint8_t>(type);
  h->next = g_head;
  g_head = h;
  g_bytes.fetch_add(payloadBytes);
  g_objs.fetch_add(1);
  return payload_from_header(h);
}

static inline void mark(Header* h) {
  if (!h || h->mark) return;
  h->mark = 1;
  // No child pointers to traverse in this initial slice.
}

void collect(const std::vector<void*>& roots) {
  // Mark
  for (void* p : roots) {
    if (!p) continue;
    Header* h = header_from_payload(p);
    mark(h);
  }
  // Sweep
  Header* prev = nullptr;
  Header* cur = g_head;
  while (cur) {
    if (!cur->mark) {
      // Unlink and free
      Header* dead = cur;
      cur = cur->next;
      if (prev) prev->next = cur; else g_head = cur;
      g_bytes.fetch_sub(dead->size);
      g_objs.fetch_sub(1);
      std::free(dead);
    } else {
      cur->mark = 0; // clear for next cycle
      prev = cur;
      cur = cur->next;
    }
  }
}

std::size_t heap_bytes()   { return g_bytes.load(); }
std::size_t heap_objects() { return g_objs.load(); }

}} // namespace seed::gc