#pragma once

#include <cstdint>
#include <string>

namespace minidaw::engine::glue {

// The check V-03 (TK-005, technology_stack.md Section 14): opens a stream of Oboe as the engine
// will (D-035) and reports what the phone grants. Each function returns one JSON object.

// Output: low-latency mode, exclusive, float, stereo, 48 kHz, a buffer of 2 Bursts, silence. It
// runs for `seconds` and then reads the count of Underruns and the estimate of the latency of
// Oboe. With 0 seconds it opens and starts.
std::string probeOutput(bool performanceHint, int32_t seconds);

// Input: low-latency mode, the preset UNPROCESSED, float, mono, 48 kHz. Needs the permission of
// the microphone.
std::string probeInput();

}  // namespace minidaw::engine::glue
