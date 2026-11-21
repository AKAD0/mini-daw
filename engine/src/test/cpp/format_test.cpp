#include "minidaw/engine/format.h"

#include <gtest/gtest.h>

#include <cmath>
#include <cstdint>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <iterator>
#include <limits>
#include <random>
#include <vector>

#include "snapshot_bytes.h"

namespace minidaw::engine::format {
namespace {

using test::smallSnapshot;
using test::SnapshotBytes;

Error loadOf(const Snapshot& s) {
  Snapshot out;
  return load(SnapshotBytes::write(s), out);
}

// The folder of the Snapshots that :core:compileFixtures wrote (architecture.md Section 15). The one
// command sets it, and in its strict mode also MINIDAW_REQUIRE_FIXTURES.
std::filesystem::path fixtures() {
  const char* folder = std::getenv("MINIDAW_FIXTURES");
  return folder == nullptr ? std::filesystem::path{} : std::filesystem::path{folder};
}

std::vector<uint8_t> readFile(const std::filesystem::path& path) {
  std::ifstream in(path, std::ios::binary);
  return {std::istreambuf_iterator<char>(in), std::istreambuf_iterator<char>()};
}

TEST(Format, AWrittenSnapshotLoadsWithEveryValue) {
  const Snapshot s = smallSnapshot();
  Snapshot out;
  ASSERT_EQ(load(SnapshotBytes::write(s), out), Error::kNone);
  EXPECT_EQ(out.tempoMilliBpm, 120000U);
  EXPECT_EQ(out.instruments.size(), 2U);
  EXPECT_EQ(out.instruments[1].playMode, 1);
  EXPECT_FLOAT_EQ(out.instruments[1].gain, 0.5F);
  EXPECT_EQ(out.instruments[0].envelope, 0);
  EXPECT_FLOAT_EQ(out.envelopes[0].releaseMs, 200.0F);
  EXPECT_EQ(out.notes[1].pitch, 64);
  EXPECT_EQ(out.clips[2].offset, 100U);
  EXPECT_EQ(out.channels[0].target, 1);
  EXPECT_FLOAT_EQ(out.channels[1].pan, -0.5F);
  EXPECT_EQ(out.slots[0].kind, 1);
  EXPECT_FLOAT_EQ(out.parameters[3], 1000.0F);
  EXPECT_EQ(out.order, (std::vector<uint32_t>{0, 1}));
  EXPECT_EQ(out.samples[0][31], 0x11);
}

TEST(Format, TheFixturesOfTheCompilerLoad) {
  const std::filesystem::path folder = fixtures();
  if (folder.empty() || !std::filesystem::exists(folder / "routing.mdsn")) {
    if (std::getenv("MINIDAW_REQUIRE_FIXTURES") != nullptr) {
      FAIL() << "the Snapshots of the fixtures are missing in " << folder;
    }
    GTEST_SKIP() << "MINIDAW_FIXTURES does not name the folder of the compiled fixtures";
  }
  Snapshot fresh;
  ASSERT_EQ(load(readFile(folder / "new-project.mdsn"), fresh), Error::kNone);
  EXPECT_EQ(fresh.tracks.size(), 1U);
  EXPECT_EQ(fresh.channels.size(), 2U);
  Snapshot routing;
  ASSERT_EQ(load(readFile(folder / "routing.mdsn"), routing), Error::kNone);
  EXPECT_EQ(routing.tempoMilliBpm, 100000U);
  EXPECT_EQ(routing.channels.size(), 4U);
  EXPECT_EQ(routing.channels.back().id, 0U);
  EXPECT_EQ(routing.channels[1].target, 0);
  EXPECT_EQ(routing.order, (std::vector<uint32_t>{1, 0, 2}));
  EXPECT_EQ(routing.instruments[1].ownChannel, 0);
  EXPECT_FALSE(routing.tracks[1].audible);
  EXPECT_FALSE(routing.channels[2].audible);
  EXPECT_EQ(routing.slots[4].kind, 1);
  EXPECT_EQ(routing.slots[12].kind, 3);
}

TEST(Format, TheHeaderIsChecked) {
  const std::vector<uint8_t> good = SnapshotBytes::write(smallSnapshot());
  Snapshot out;
  EXPECT_EQ(load(std::span(good).first(100), out), Error::kTooShort);
  std::vector<uint8_t> bytes = good;
  bytes[0] = 'X';
  EXPECT_EQ(load(bytes, out), Error::kMark);
  bytes = good;
  bytes[4] = 2;
  EXPECT_EQ(load(bytes, out), Error::kVersion);
  bytes = good;
  bytes.pop_back();
  EXPECT_EQ(load(bytes, out), Error::kSize);
  // The count of the Notes grows past the end of the block.
  bytes = good;
  bytes[kHeaderSize + kNoteTable * 8 + 4] = 200;
  EXPECT_EQ(load(bytes, out), Error::kTable);
  // An offset points into the directory.
  bytes = good;
  bytes[kHeaderSize + kClipTable * 8] = 4;
  bytes[kHeaderSize + kClipTable * 8 + 1] = 0;
  EXPECT_EQ(load(bytes, out), Error::kTable);
}

TEST(Format, EveryReferenceIsChecked) {
  const auto refused = [](auto change) {
    Snapshot s = smallSnapshot();
    change(s);
    return loadOf(s) == Error::kReference;
  };
  EXPECT_TRUE(refused([](Snapshot& s) { s.instruments[0].sample = 1; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.instruments[0].envelope = 1; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.instruments[0].ownChannel = 2; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.instruments[0].playMode = 2; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.patterns[0].partCount = 3; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.parts[1].instrument = 2; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.parts[1].noteCount = 2; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.notes[0].pitch = 128; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.notes[0].velocity = 0; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.tracks[1].channel = 3; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.tracks[1].clipCount = 2; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.clips[0].source = 1; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.clips[0].kind = 2; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.channels[0].target = 0; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.channels[0].target = 2; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.channels[2].firstSlot = 9; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.slots[0].parameterCount = 8; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.slots[0].kind = 9; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.channels.back().id = 5; }));
  EXPECT_TRUE(refused([](Snapshot& s) { s.channels.clear(); }));
}

TEST(Format, NotesAndClipsAreSortedAndTheOrderIsComplete) {
  Snapshot s = smallSnapshot();
  std::swap(s.notes[0], s.notes[1]);
  EXPECT_EQ(loadOf(s), Error::kUnsorted);
  s = smallSnapshot();
  std::swap(s.clips[0], s.clips[1]);
  EXPECT_EQ(loadOf(s), Error::kUnsorted);
  s = smallSnapshot();
  s.order = {1, 0};
  EXPECT_EQ(loadOf(s), Error::kOrder);
  s.order = {0, 0};
  EXPECT_EQ(loadOf(s), Error::kOrder);
  s.order = {0};
  EXPECT_EQ(loadOf(s), Error::kOrder);
  s.order = {0, 2};
  EXPECT_EQ(loadOf(s), Error::kOrder);
}

TEST(Format, NumbersAndCountsKeepTheirLimits) {
  const auto limited = [](auto change) {
    Snapshot s = smallSnapshot();
    change(s);
    return loadOf(s) == Error::kLimit;
  };
  const float nan = std::numeric_limits<float>::quiet_NaN();
  const float inf = std::numeric_limits<float>::infinity();
  EXPECT_TRUE(limited([&](Snapshot& s) { s.channels[0].gain = nan; }));
  EXPECT_TRUE(limited([&](Snapshot& s) { s.instruments[0].pan = 2.0F; }));
  EXPECT_TRUE(limited([&](Snapshot& s) { s.clips[0].gain = -1.0F; }));
  EXPECT_TRUE(limited([&](Snapshot& s) { s.parameters[0] = inf; }));
  EXPECT_TRUE(limited([&](Snapshot& s) { s.envelopes[0].attackMs = nan; }));
  EXPECT_TRUE(limited([](Snapshot& s) { s.tempoMilliBpm = 29999; }));
  EXPECT_TRUE(limited([](Snapshot& s) { s.tracks.resize(17, s.tracks[0]); }));
  Snapshot mvp = smallSnapshot();
  mvp.slots[4] = mvp.slots[0];
  mvp.slots[4].id = 13;
  Snapshot out;
  EXPECT_EQ(load(SnapshotBytes::write(mvp), out, Limits{.activeEffects = 1}), Error::kLimit);
  EXPECT_EQ(load(SnapshotBytes::write(mvp), out, Limits{.activeEffects = 2}), Error::kNone);
}

TEST(Format, ARefusedSnapshotLeavesTheOldOneUnchanged) {
  Snapshot out;
  ASSERT_EQ(load(SnapshotBytes::write(smallSnapshot()), out), Error::kNone);
  Snapshot broken = smallSnapshot();
  broken.notes.clear();
  EXPECT_NE(load(SnapshotBytes::write(broken), out), Error::kNone);
  EXPECT_EQ(out.notes.size(), 3U);
}

TEST(Format, RandomBytesNeverBreakTheLoader) {
  // Changed and cut bytes either load into a Snapshot whose references hold, or are refused.
  const std::vector<uint8_t> good = SnapshotBytes::write(smallSnapshot());
  std::mt19937 random(20261122);
  size_t loaded = 0;
  for (int round = 0; round < 5000; ++round) {
    std::vector<uint8_t> bytes = good;
    const int changes = 1 + static_cast<int>(random() % 4);
    for (int c = 0; c < changes; ++c) {
      bytes[random() % bytes.size()] = static_cast<uint8_t>(random());
    }
    if (round % 7 == 0) {
      bytes.resize(random() % bytes.size());
    }
    Snapshot out;
    if (load(bytes, out) == Error::kNone) {
      ++loaded;
      for (const Track& t : out.tracks) {
        ASSERT_LT(t.channel, out.channels.size());
        ASSERT_LE(t.firstClip + t.clipCount, out.clips.size());
      }
      for (const Channel& ch : out.channels) {
        ASSERT_TRUE(std::isfinite(ch.gain));
      }
    }
  }
  EXPECT_GT(loaded, 0U);
}

}  // namespace
}  // namespace minidaw::engine::format
