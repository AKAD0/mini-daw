#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

namespace minidaw::engine::format {

// The bytes of a Snapshot (feature_specifications.md Section 6.2, D-089): a header, a directory of
// twelve pairs of an offset and a count, and twelve tables of records. All numbers are little-endian.
// SnapshotWriter of :engine-api writes them; the compiler of :core fills the tables.
constexpr uint32_t kMark = 0x4E53444DU;  // The letters "MDSN".
constexpr uint16_t kVersion = 1;
constexpr size_t kHeaderSize = 20;
constexpr size_t kTables = 12;
constexpr size_t kDirectorySize = kTables * 8;
constexpr int32_t kNone = -1;
constexpr size_t kSlotsPerChannel = 4;
constexpr size_t kHashSize = 32;
constexpr uint8_t kMaxPitch = 127;
constexpr uint16_t kMaxEffectKind = 8;
constexpr float kMaxGain = 16.0F;

enum Table : uint8_t {
  kSampleTable,
  kInstrumentTable,
  kEnvelopeTable,
  kPatternTable,
  kPartTable,
  kNoteTable,
  kTrackTable,
  kClipTable,
  kChannelTable,
  kSlotTable,
  kParameterTable,
  kOrderTable,
};

constexpr std::array<uint32_t, kTables> kRecordSizes{32, 48, 16, 16, 12, 12, 20, 28, 24, 16, 4, 4};

// The reasons for a refusal. The numbers equal the codes of EngineResult in :engine-api.
enum class Error : uint8_t {
  kNone = 0,
  kTooShort = 1,
  kMark = 2,
  kVersion = 3,
  kSize = 4,
  kTable = 5,
  kReference = 6,
  kOrder = 7,
  kUnsorted = 8,
  kLimit = 9,
};

const char* errorText(Error error);

struct Instrument {
  uint32_t id = 0;
  uint32_t sample = 0;
  uint8_t rootNote = 60;
  uint8_t playMode = 0;  // 0: gate, 1: one-shot
  bool audible = true;
  float gain = 1.0F;
  float pan = 0.0F;
  float fineCents = 0.0F;
  int32_t ownChannel = kNone;
  int32_t envelope = kNone;
  uint32_t startFrame = 0;
  uint32_t endFrame = 0;
  uint32_t loopStart = 0;
  uint32_t loopEnd = 0;
};

struct Envelope {
  float attackMs = 0.0F;
  float decayMs = 0.0F;
  float sustain = 1.0F;
  float releaseMs = 0.0F;
};

struct Pattern {
  uint32_t id = 0;
  uint32_t lengthTicks = 0;
  uint32_t firstPart = 0;
  uint32_t partCount = 0;
};

struct Part {
  uint32_t instrument = 0;
  uint32_t firstNote = 0;
  uint32_t noteCount = 0;
};

struct Note {
  uint32_t start = 0;
  uint32_t length = 0;
  uint8_t pitch = 0;
  uint8_t velocity = 0;
};

struct Track {
  uint32_t id = 0;
  uint32_t channel = 0;
  uint32_t firstClip = 0;
  uint32_t clipCount = 0;
  bool audible = true;
};

struct Clip {
  uint32_t id = 0;
  uint8_t kind = 0;  // 0: Audio clip, 1: Pattern clip
  uint32_t start = 0;
  uint32_t source = 0;
  uint32_t offset = 0;
  uint32_t length = 0;
  float gain = 1.0F;
};

struct Channel {
  uint32_t id = 0;
  int32_t target = kNone;  // kNone: the Master channel
  float gain = 1.0F;
  float pan = 0.0F;
  bool audible = true;
  uint32_t firstSlot = 0;
};

struct Slot {
  uint32_t id = 0;
  uint16_t kind = 0;  // 0: empty, 1 eq3, 2 delay, 3 reverb, 4 compressor, ... 8 chorus
  bool bypass = false;
  uint32_t firstParameter = 0;
  uint32_t parameterCount = 0;
};

// The limits of requirements.md Section 8 that check 6 applies. The defaults are those of version 1.0.
struct Limits {
  uint32_t tracks = 16;
  uint32_t channels = 16;  // Mixer channels without the Master channel
  uint32_t instrumentsPerPattern = 16;
  uint32_t activeEffects = 16;
  uint32_t notesPerPart = 4096;
};

// A Snapshot in the memory of the engine. The last channel is the Master channel.
struct Snapshot {
  uint32_t tempoMilliBpm = 120000;
  uint8_t numerator = 4;
  uint8_t denominator = 4;
  std::vector<std::array<uint8_t, kHashSize>> samples;
  std::vector<Instrument> instruments;
  std::vector<Envelope> envelopes;
  std::vector<Pattern> patterns;
  std::vector<Part> parts;
  std::vector<Note> notes;
  std::vector<Track> tracks;
  std::vector<Clip> clips;
  std::vector<Channel> channels;
  std::vector<Slot> slots;
  std::vector<float> parameters;
  std::vector<uint32_t> order;
};

// Checks the bytes (the six checks of Section 6.2, and that every number is finite and in its range)
// and reads them into `out`. On an error, `out` stays unchanged.
Error load(std::span<const uint8_t> bytes, Snapshot& out, const Limits& limits = Limits{});

}  // namespace minidaw::engine::format
