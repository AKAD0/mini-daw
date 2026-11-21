#pragma once

#include <cstddef>
#include <string>
#include <vector>

#include "minidaw/engine/kernels.h"

namespace minidaw::engine {

// The counts of a standard test Project (requirements.md Section 8).
struct TestCounts {
  int audioTracks = 4;
  int patternTracks = 4;
  int voicesPerPatternTrack = 4;
  int channels = 8;
  int equalizers = 4;
  int delays = 2;
  int reverbs = 2;
};

// The standard test Project of the MVP, and that of version 1.0 with the doubled counts.
TestCounts mvpCounts();
TestCounts version10Counts();

// The render of the check V-04 (TK-007): the Voices and the Streamers of a standard test Project
// into their Mixer channels, one effect per channel, and the sum into the Master channel. It
// renders internal blocks of at most 128 frames (architecture.md Section 5.4) and allocates
// nothing after its constructor. Its output is never played.
class LoadBench {
 public:
  static constexpr int kBlock = 128;

  LoadBench(const TestCounts& counts, const std::vector<float>& sample);

  // Renders `frames` frames into both outputs.
  void render(float* left, float* right, int frames);

  [[nodiscard]] size_t voices() const { return voices_.size(); }
  [[nodiscard]] size_t streamers() const { return streamers_.size(); }
  [[nodiscard]] size_t effects() const;

 private:
  struct Channel {
    std::vector<float> left;
    std::vector<float> right;
    float gainLeft = 1.0F;
    float gainRight = 1.0F;
    int kind = 0;
    size_t effect = 0;
  };

  struct Streamer {
    size_t channel = 0;
    size_t position = 0;
    float gain = 1.0F;
  };

  struct Voice {
    size_t channel = 0;
    VoiceState state;
  };

  void renderBlock(float* left, float* right, int frames);

  SincTable table_;
  GuardedSample sample_;
  std::vector<float> audioLeft_;
  std::vector<float> audioRight_;
  std::vector<Channel> channels_;
  std::vector<Voice> voices_;
  std::vector<Streamer> streamers_;
  std::vector<Equalizer> equalizers_;
  std::vector<Delay> delays_;
  std::vector<Reverb> reverbs_;
};

// The cost of one kernel: microseconds for one block of 128 frames, on the calling thread.
struct KernelCost {
  std::string name;
  double microsPerBlock = 0.0;
};

// Measures each kernel alone over `blocks` blocks: one Voice, one Streamer, eq3, delay and
// reverb.
std::vector<KernelCost> measureKernels(const std::vector<float>& sample, int blocks);

}  // namespace minidaw::engine
