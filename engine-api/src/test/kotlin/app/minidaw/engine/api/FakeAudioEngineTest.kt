package app.minidaw.engine.api

import app.minidaw.engine.api.snapshot.ChannelRecord
import app.minidaw.engine.api.snapshot.SampleHash
import app.minidaw.engine.api.snapshot.SlotRecord
import app.minidaw.engine.api.snapshot.SnapshotContent
import app.minidaw.engine.api.snapshot.SnapshotWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeAudioEngineTest {
    private val snapshot = SnapshotWriter.write(
        SnapshotContent(
            tempoMilliBpm = 120_000,
            numerator = 4,
            denominator = 4,
            channels = listOf(ChannelRecord(id = 0, target = -1, gain = 1f, pan = 0f, audible = true, firstSlot = 0)),
            slots = List(4) { SlotRecord.EMPTY }
        )
    )
    private val hash = SampleHash("ab".repeat(SampleHash.BYTES))

    @Test
    fun theFakeRecordsLiveCommandsInOrder() {
        val engine = FakeAudioEngine()
        engine.send(LiveCommand.Play)
        engine.send(LiveCommand.Seek(960))
        engine.send(LiveCommand.Stop)
        assertEquals(listOf(LiveCommand.Play, LiveCommand.Seek(960), LiveCommand.Stop), engine.commands)
    }

    @Test
    fun theFakeKeepsGoodSnapshotsAndRefusesOthers() {
        val engine = FakeAudioEngine()
        assertEquals(EngineResult.OK, engine.loadSnapshot(snapshot))
        assertEquals(EngineResult.BAD_MARK, engine.loadSnapshot(snapshot.copyOf().also { it[0] = 0 }))
        engine.refuseNextSnapshot = EngineResult.BAD_ORDER
        assertEquals(EngineResult.BAD_ORDER, engine.loadSnapshot(snapshot))
        assertNull(engine.refuseNextSnapshot)
        assertEquals(EngineResult.OK, engine.loadSnapshot(snapshot))
        assertEquals(2, engine.snapshots.size)
    }

    @Test
    fun aPreviewNeedsARegisteredSample() {
        val engine = FakeAudioEngine()
        assertEquals(EngineResult.UNKNOWN_SAMPLE, engine.startPreview(hash))
        engine.registerSample(hash, "/library/ab.wav", SampleUse.MEMORY)
        assertEquals(EngineResult.OK, engine.startPreview(hash))
        engine.releaseSample(hash)
        assertTrue(engine.samples.isEmpty())
        assertEquals(listOf(hash), engine.previews)
    }

    @Test
    fun oneTakeRunsAtATime() {
        val engine = FakeAudioEngine()
        assertEquals(EngineResult.OK, engine.startTake("/takes/1.wav"))
        assertEquals(EngineResult.BUSY, engine.startTake("/takes/2.wav"))
        assertEquals(EngineResult.OK, engine.stopTake())
        assertNull(engine.takePath)
    }

    @Test
    fun eventsAreReadOnce() {
        val engine = FakeAudioEngine()
        engine.pendingEvents += EngineEvent.StreamerLate(clipId = 12)
        engine.pendingEvents += EngineEvent.TakeStopped(TakeStop.INPUT_LOST)
        assertEquals(2, engine.readEvents().size)
        assertTrue(engine.readEvents().isEmpty())
    }

    @Test
    fun resultsCrossAsCodes() {
        assertEquals(EngineResult.BAD_ORDER, EngineResult.of(7))
        assertEquals(EngineResult.UNKNOWN, EngineResult.of(99))
        assertEquals(EngineResult.entries.size, EngineResult.entries.map { it.code }.toSet().size)
    }

    @Test
    fun liveCommandsCheckTheirValues() {
        assertFailsWith<IllegalArgumentException> { LiveCommand.NoteOn(1, pitch = 128, velocity = 100, trackId = 2) }
        assertFailsWith<IllegalArgumentException> { LiveCommand.NoteOn(1, pitch = 60, velocity = 0, trackId = 2) }
        assertFailsWith<IllegalArgumentException> { LiveCommand.SetLoop(startTicks = 960, endTicks = 960, true) }
        assertFailsWith<IllegalArgumentException> { LiveCommand.SetMode(TransportMode.PATTERN) }
        assertFailsWith<IllegalArgumentException> { LiveCommand.SetTempo(0) }
        assertEquals(TransportMode.PATTERN, LiveCommand.SetMode(TransportMode.PATTERN, patternId = 9).mode)
    }
}
