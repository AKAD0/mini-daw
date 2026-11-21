#include "minidaw/engine/engine.h"

#include <gtest/gtest.h>

#include <atomic>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <limits>
#include <random>
#include <thread>
#include <vector>

#include "snapshot_bytes.h"

namespace minidaw::engine {
namespace {

using Kind = LiveCommand::Kind;
using test::smallSnapshot;
using test::SnapshotBytes;

std::vector<uint8_t> smallBytes() { return SnapshotBytes::write(smallSnapshot()); }

void renderFrames(Engine& engine, size_t frames) {
  std::vector<float> left(frames);
  std::vector<float> right(frames);
  engine.render(left, right);
}

TEST(Engine, TheTransportAdvancesByTheTempo) {
  Engine engine;
  ASSERT_EQ(engine.loadSnapshot(smallBytes()), format::Error::kNone);
  engine.send({.kind = Kind::kPlay});
  // One second at 120 BPM is two quarter notes: 1,920 Ticks.
  for (int block = 0; block < 375; ++block) {
    renderFrames(engine, 128);
  }
  EXPECT_TRUE(engine.playing());
  EXPECT_NEAR(engine.positionTicks(), 1920.0, 1e-6);
  engine.send({.kind = Kind::kSetTempo, .a = 60000});
  engine.send({.kind = Kind::kSeek, .a = 3840});
  renderFrames(engine, 48000);
  EXPECT_NEAR(engine.positionTicks(), 3840.0 + 960.0, 1e-6);
  engine.send({.kind = Kind::kStop});
  renderFrames(engine, 48000);
  EXPECT_FALSE(engine.playing());
  EXPECT_NEAR(engine.positionTicks(), 4800.0, 1e-6);
}

TEST(Engine, TheLoopOfSongModeWraps) {
  Engine engine;
  ASSERT_EQ(engine.loadSnapshot(smallBytes()), format::Error::kNone);
  engine.send({.kind = Kind::kSetLoop, .a = 960, .b = 1920, .c = 1});
  engine.send({.kind = Kind::kSeek, .a = 960});
  engine.send({.kind = Kind::kPlay});
  renderFrames(engine, 36000);  // 1,440 Ticks at 120 BPM
  EXPECT_DOUBLE_EQ(engine.positionTicks(), 960.0 + 480.0);
}

TEST(Engine, ABrokenSnapshotIsRefusedAndTheOldOneStays) {
  Engine engine;
  ASSERT_EQ(engine.loadSnapshot(smallBytes()), format::Error::kNone);
  renderFrames(engine, 128);
  std::vector<uint8_t> broken = smallBytes();
  broken[0] = 0;
  EXPECT_EQ(engine.loadSnapshot(broken), format::Error::kMark);
  renderFrames(engine, 128);
  EXPECT_EQ(engine.swaps(), 1U);
  EXPECT_FLOAT_EQ(engine.channelGain(8), 0.7F);
}

TEST(Engine, ALiveValueStaysUntilAnEditChangesIt) {
  // The rule for values at a swap (architecture.md Section 5.2).
  Engine engine;
  ASSERT_EQ(engine.loadSnapshot(smallBytes()), format::Error::kNone);
  engine.send({.kind = Kind::kSetValue, .a = 8, .b = kParameterGain, .value = 0.25F});
  engine.send({.kind = Kind::kSetValue, .a = 10, .b = kParameterGain, .value = 0.125F});
  renderFrames(engine, 128);
  EXPECT_FLOAT_EQ(engine.channelGain(8), 0.25F);
  // A new Snapshot with the same values keeps the live ones.
  ASSERT_EQ(engine.loadSnapshot(smallBytes()), format::Error::kNone);
  renderFrames(engine, 128);
  EXPECT_FLOAT_EQ(engine.channelGain(8), 0.25F);
  EXPECT_FLOAT_EQ(engine.instrumentGain(10), 0.125F);
  // An Edit that changes the gain wins.
  format::Snapshot edited = smallSnapshot();
  edited.channels[1].gain = 0.9F;
  ASSERT_EQ(engine.loadSnapshot(SnapshotBytes::write(edited)), format::Error::kNone);
  renderFrames(engine, 128);
  EXPECT_FLOAT_EQ(engine.channelGain(8), 0.9F);
  EXPECT_FLOAT_EQ(engine.channelPan(8), -0.5F);
  EXPECT_FLOAT_EQ(engine.instrumentGain(10), 0.125F);
}

TEST(Engine, CommandsForUnknownItemsAreDroppedAndCounted) {
  Engine engine;
  ASSERT_EQ(engine.loadSnapshot(smallBytes()), format::Error::kNone);
  engine.send({.kind = Kind::kSetValue, .a = 999, .b = kParameterGain, .value = 0.5F});
  engine.send({.kind = Kind::kNoteOn, .a = 999, .b = 60, .c = 100, .d = 1});
  engine.send({.kind = Kind::kSetMode, .a = 1, .b = 999});
  engine.send({.kind = Kind::kSetValue, .a = 8, .b = kParameterGain,
               .value = std::numeric_limits<float>::quiet_NaN()});
  engine.send({.kind = Kind::kSetTempo, .a = 1});
  renderFrames(engine, 64);
  EXPECT_EQ(engine.droppedCommands(), 5U);
  EXPECT_FLOAT_EQ(engine.channelGain(8), 0.7F);
  EXPECT_EQ(engine.tempoMilliBpm(), 120000U);
}

TEST(Engine, RandomLiveCommandsAndSwapsCauseNoCrashAndNoInvalidNumber) {
  // The criterion of TK-021, with a fixed seed: commands with random kinds, targets and values (also NaN
  // and infinity), good and broken Snapshots, and blocks of random sizes.
  Engine engine;
  std::mt19937 random(21);
  const std::vector<uint8_t> good = smallBytes();
  const std::vector<float> values{0.0F, 0.5F, 1.0F, -1.0F, 1e9F, -1e9F,
                                  std::numeric_limits<float>::quiet_NaN(),
                                  std::numeric_limits<float>::infinity()};
  for (int round = 0; round < 20000; ++round) {
    const uint32_t what = random() % 10;
    if (what < 6) {
      LiveCommand c;
      c.kind = static_cast<Kind>(random() % 12);
      c.a = static_cast<int32_t>(random() % 20) - 2;
      c.b = static_cast<int32_t>(random() % 140) - 5;
      c.c = static_cast<int32_t>(random() % 130);
      c.d = static_cast<int32_t>(random() % 10);
      c.value = values[random() % values.size()];
      if (c.kind == Kind::kSetTempo || c.kind == Kind::kSeek) {
        c.a = static_cast<int32_t>(random() % 400000) - 1000;
      }
      engine.send(c);
    } else if (what < 8) {
      std::vector<uint8_t> bytes = good;
      if (random() % 2 == 0) {
        bytes[random() % bytes.size()] = static_cast<uint8_t>(random());
      }
      engine.loadSnapshot(bytes);
    } else {
      std::vector<float> left(random() % 1025);
      std::vector<float> right(left.size());
      engine.render(left, right);
      for (size_t i = 0; i < left.size(); ++i) {
        ASSERT_TRUE(std::isfinite(left[i]) && std::isfinite(right[i]));
      }
      ASSERT_TRUE(std::isfinite(engine.positionTicks()));
      ASSERT_GE(engine.positionTicks(), 0.0);
      ASSERT_GE(engine.tempoMilliBpm(), 30000U);
      ASSERT_LE(engine.tempoMilliBpm(), 300000U);
      engine.collect();
    }
  }
  EXPECT_GT(engine.swaps(), 0U);
}

TEST(Engine, TheThreadsOfTheEngineWorkTogether) {
  // The audio thread renders while the main thread sends commands and the Control thread loads.
  Engine engine;
  std::atomic<bool> stop{false};
  std::thread audio([&] {
    std::vector<float> left(256);
    std::vector<float> right(256);
    while (!stop.load()) {
      engine.render(left, right);
    }
  });
  std::thread sender([&] {
    for (int i = 0; i < 20000; ++i) {
      engine.send({.kind = (i % 2 == 0) ? Kind::kPlay : Kind::kStop});
    }
  });
  const std::vector<uint8_t> bytes = smallBytes();
  for (int i = 0; i < 300; ++i) {
    EXPECT_EQ(engine.loadSnapshot(bytes), format::Error::kNone);
    std::this_thread::sleep_for(std::chrono::microseconds(100));
  }
  sender.join();
  stop.store(true);
  audio.join();
  engine.collect();
  EXPECT_GT(engine.swaps(), 0U);
}

}  // namespace
}  // namespace minidaw::engine
