#pragma once

#include <array>
#include <atomic>
#include <cstddef>

namespace minidaw::engine {

// A ring buffer with one writer and one reader and no lock (architecture.md Section 3). The writer
// calls push, the reader calls pop. Neither call waits or allocates, so the audio thread may use it.
template <typename T, size_t N>
class SpscRing {
  static_assert(N >= 2 && (N & (N - 1)) == 0, "the size of the ring is a power of two");

 public:
  // Returns false when the ring is full; the value is then not stored.
  bool push(const T& value) {
    const size_t head = head_.load(std::memory_order_relaxed);
    const size_t next = (head + 1) & (N - 1);
    if (next == tail_.load(std::memory_order_acquire)) {
      return false;
    }
    items_[head] = value;
    head_.store(next, std::memory_order_release);
    return true;
  }

  // The writer asks whether its next push would fail.
  [[nodiscard]] bool full() const {
    const size_t next = (head_.load(std::memory_order_relaxed) + 1) & (N - 1);
    return next == tail_.load(std::memory_order_acquire);
  }

  // Returns false when the ring is empty.
  bool pop(T& value) {
    const size_t tail = tail_.load(std::memory_order_relaxed);
    if (tail == head_.load(std::memory_order_acquire)) {
      return false;
    }
    value = items_[tail];
    tail_.store((tail + 1) & (N - 1), std::memory_order_release);
    return true;
  }

 private:
  std::array<T, N> items_{};
  std::atomic<size_t> head_{0};
  std::atomic<size_t> tail_{0};
};

}  // namespace minidaw::engine
