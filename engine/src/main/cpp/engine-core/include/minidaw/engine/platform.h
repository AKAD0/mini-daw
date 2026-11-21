#pragma once

#include <atomic>
#include <cstdint>
#include <limits>

namespace minidaw::engine {

// The audio thread shares values with other threads only through atomic values,
// and it never takes a lock (architecture.md Sections 1.2 and 3.2, NFR-RES-03).
// These checks stop the build on a platform where such a value would need a lock.
static_assert(std::atomic<std::uint32_t>::is_always_lock_free);
static_assert(std::atomic<std::uint64_t>::is_always_lock_free);
static_assert(std::atomic<std::int64_t>::is_always_lock_free);
static_assert(std::atomic<float>::is_always_lock_free);
static_assert(std::atomic<double>::is_always_lock_free);

// The engine computes audio as 32-bit floating point numbers of IEEE 754
// (technology_stack.md Section 1: 48 kHz, float).
static_assert(std::numeric_limits<float>::is_iec559);
static_assert(sizeof(float) == 4);

// Returns true when the atomic values of the audio thread are lock-free at run time too.
bool audioAtomicsAreLockFree() noexcept;

}  // namespace minidaw::engine
