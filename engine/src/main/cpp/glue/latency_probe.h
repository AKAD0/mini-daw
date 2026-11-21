#pragma once

#include <string>
#include <vector>

namespace minidaw::engine::glue {

// The check V-07 (TK-006, D-040): the output stream plays the click 8 times, once per second, and
// the input stream records it. For each click, the result holds where the recording should hold it
// by the timestamps of both streams (feature_specifications.md Section 4.2.2) and where the
// recording holds it (calibration by loopback). Both are milliseconds after the input frame that
// was the newest when the click was written. Returns one JSON object. Needs the microphone.
std::string probeTakeLatency(const std::vector<float>& click);

}  // namespace minidaw::engine::glue
