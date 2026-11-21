#pragma once

#include <cstdint>
#include <span>
#include <string>
#include <vector>

namespace minidaw::engine::glue {

// The check V-04 (TK-007). Each function returns one JSON object.

// Renders a standard test Project (0: MVP, 1: version 1.0) in the callback of an output stream for
// `seconds` and records the load of each callback: the time of the render divided by the time of
// the frames. The stream plays silence.
std::string probeLoad(int32_t project, int32_t seconds, const std::vector<float>& sample);

// The cost of each kernel on the calling thread, in microseconds for 128 frames.
std::string probeKernels(const std::vector<float>& sample);

// Loads the bytes of a Snapshot `runs` times and gives the median time. The bytes come from the
// writer of :engine-api; the result also says whether they equal those of the writer of
// engine-core.
std::string probeSnapshotLoad(std::span<const uint8_t> bytes, int32_t runs);

}  // namespace minidaw::engine::glue
