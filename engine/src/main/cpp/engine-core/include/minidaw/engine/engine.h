#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

#include "minidaw/engine/format.h"
#include "minidaw/engine/spsc_ring.h"

namespace minidaw::engine {

// A Live command of architecture.md Section 5.3 as plain numbers, so that it crosses the border and the
// ring buffer without an allocation. The fields mean, by kind:
//   kSeek          a: position in Ticks
//   kSetMode       a: 0 Song mode, 1 Pattern mode; b: ID of the Pattern; c: ID of the Track
//   kSetLoop       a: start in Ticks; b: end in Ticks; c: 1 on, 0 off
//   kSetTempo      a: thousandths of a BPM
//   kSetValue      a: ID of the target; b: parameter; value: the value
//   kNoteOn        a: ID of the Sampler instrument; b: pitch; c: velocity; d: ID of the Track
//   kNoteOff       a: ID of the Sampler instrument; b: pitch; d: ID of the Track
//   kMetronome     a: 1 on, 0 off
//   kClearClipFlag a: ID of a channel, 0 for the Master channel, -2 for the output
struct LiveCommand {
  enum class Kind : uint8_t {
    kPlay,
    kStop,
    kSeek,
    kSetMode,
    kSetLoop,
    kSetTempo,
    kSetValue,
    kNoteOn,
    kNoteOff,
    kStopPreview,
    kMetronome,
    kClearClipFlag,
  };
  Kind kind = Kind::kPlay;
  int32_t a = 0;
  int32_t b = 0;
  int32_t c = 0;
  int32_t d = 0;
  float value = 0.0F;
};

// The parameters of kSetValue for a channel and a Sampler instrument (ValueParameter of :engine-api). For
// an Effect slot the parameter is the index of the value in the order of Section 3.10.
constexpr int32_t kParameterGain = 0;
constexpr int32_t kParameterPan = 1;

// The rate of the engine (D-035) and the Ticks of a quarter note (architecture.md Section 4.3).
constexpr int kRate = 48000;
constexpr double kFramesPerTickTimesTempo = 3000000.0;

// A Snapshot and the running values that Live commands change. The Control thread builds it; the audio
// thread takes it over at the start of a block and never allocates or frees.
struct Loaded {
  format::Snapshot snapshot;
  std::vector<float> channelGain;
  std::vector<float> channelPan;
  std::vector<float> instrumentGain;
  std::vector<float> parameters;
};

// The engine of architecture.md Section 5: Snapshots from the Control thread, Live commands from the
// main thread, and the render function of the audio thread. PH-02 adds the sources, the Sequencer and
// the Mixer to the render function; this first version keeps the Transport and renders silence.
class Engine {
 public:
  Engine() = default;
  ~Engine();
  Engine(const Engine&) = delete;
  Engine& operator=(const Engine&) = delete;
  Engine(Engine&&) = delete;
  Engine& operator=(Engine&&) = delete;

  // Control thread. Checks and loads a Snapshot (architecture.md Section 5.2). On an error the engine
  // keeps its Snapshot. A Snapshot that the audio thread has not taken yet is replaced by the newer one.
  format::Error loadSnapshot(std::span<const uint8_t> bytes, const format::Limits& limits = {});

  // Control thread. Frees the Snapshots that the audio thread gave back.
  void collect();

  // Main thread. Returns false when the queue is full; the command is then lost and counted.
  bool send(const LiveCommand& command);

  // Audio thread. Renders one block of stereo into `left` and `right`, which have the same length.
  void render(std::span<float> left, std::span<float> right);

  // Any thread: the telemetry of architecture.md Section 5.10 that this version keeps.
  [[nodiscard]] uint32_t droppedCommands() const { return dropped_.load(); }
  [[nodiscard]] uint32_t swaps() const { return swaps_.load(); }
  [[nodiscard]] double positionTicks() const { return positionOut_.load(); }
  [[nodiscard]] bool playing() const { return playingOut_.load(); }
  [[nodiscard]] uint32_t tempoMilliBpm() const { return tempoOut_.load(); }

  // Audio thread, also for tests: the running values of the current Snapshot. NaN when the ID is unknown.
  [[nodiscard]] float channelGain(uint32_t id) const;
  [[nodiscard]] float channelPan(uint32_t id) const;
  [[nodiscard]] float instrumentGain(uint32_t id) const;

 private:
  void swap();
  void apply(const LiveCommand& command);
  void setValue(const LiveCommand& command);
  void advance(size_t frames);

  std::atomic<Loaded*> latest_{nullptr};
  SpscRing<Loaded*, 64> retired_;
  SpscRing<LiveCommand, 256> commands_;
  Loaded* current_ = nullptr;

  // State of the audio thread.
  double position_ = 0.0;
  bool running_ = false;
  uint32_t tempo_ = 120000;
  bool loopOn_ = false;
  uint32_t loopStart_ = 0;
  uint32_t loopEnd_ = 0;
  uint8_t mode_ = 0;
  bool metronome_ = false;

  // Telemetry, written by the audio thread.
  std::atomic<uint32_t> dropped_{0};
  std::atomic<uint32_t> swaps_{0};
  std::atomic<double> positionOut_{0.0};
  std::atomic<bool> playingOut_{false};
  std::atomic<uint32_t> tempoOut_{120000};
};

}  // namespace minidaw::engine
