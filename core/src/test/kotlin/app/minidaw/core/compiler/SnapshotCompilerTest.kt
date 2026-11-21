package app.minidaw.core.compiler

import app.minidaw.core.document.ProjectDocument
import app.minidaw.core.model.Fixtures
import app.minidaw.core.model.Limits
import app.minidaw.core.model.MixerChannel
import app.minidaw.core.model.Project
import app.minidaw.engine.api.EngineResult
import app.minidaw.engine.api.snapshot.ChannelRecord
import app.minidaw.engine.api.snapshot.ClipKind
import app.minidaw.engine.api.snapshot.ClipRecord
import app.minidaw.engine.api.snapshot.EffectKind
import app.minidaw.engine.api.snapshot.EnvelopeRecord
import app.minidaw.engine.api.snapshot.InstrumentRecord
import app.minidaw.engine.api.snapshot.NoteRecord
import app.minidaw.engine.api.snapshot.PartRecord
import app.minidaw.engine.api.snapshot.PatternRecord
import app.minidaw.engine.api.snapshot.PlayMode
import app.minidaw.engine.api.snapshot.SlotRecord
import app.minidaw.engine.api.snapshot.SnapshotCheck
import app.minidaw.engine.api.snapshot.SnapshotContent
import app.minidaw.engine.api.snapshot.TrackRecord
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SnapshotCompilerTest {
    private val fixtures = File(System.getProperty("minidaw.fixtures"))
    private val snapshots = File(System.getProperty("minidaw.snapshots"))
    private val empty = SlotRecord.EMPTY

    private fun fixture(name: String): Project {
        val result = ProjectDocument.read(File(fixtures, "$name.json").readText(), Limits.VERSION_1_0)
        assertIs<ProjectDocument.Result.Opened>(result, "$name: $result")
        return result.project
    }

    private fun gain(db: Float) = SnapshotCompiler.gain(db)

    @Test
    fun theNewProjectGivesItsExpectedSnapshot() {
        val expected = SnapshotContent(
            tempoMilliBpm = 120_000,
            numerator = 4,
            denominator = 4,
            samples = listOf(Fixtures.KICK.hash, Fixtures.LOOP.hash),
            instruments = listOf(
                InstrumentRecord(
                    id = 3,
                    sample = 0,
                    rootNote = 60,
                    playMode = PlayMode.GATE,
                    audible = true,
                    gain = 1f,
                    pan = 0f,
                    fineTuningCents = 0f,
                    startFrame = 0,
                    endFrame = 24_000
                )
            ),
            patterns = listOf(PatternRecord(id = 4, lengthTicks = 3840, firstPart = 0, partCount = 1)),
            parts = listOf(PartRecord(instrument = 0, firstNote = 0, noteCount = 0)),
            tracks = listOf(TrackRecord(id = 1, channel = 0, firstClip = 0, clipCount = 0, audible = true)),
            channels = listOf(
                ChannelRecord(id = 2, target = -1, gain = 1f, pan = 0f, audible = true, firstSlot = 0),
                ChannelRecord(id = 0, target = -1, gain = 1f, pan = 0f, audible = true, firstSlot = 4)
            ),
            slots = List(8) { empty },
            order = listOf(0)
        )
        assertEquals(expected, SnapshotCompiler.compile(fixture("new-project")))
        assertEquals(expected, SnapshotCompiler.compile(Fixtures.newProject()))
    }

    @Test
    fun theFixtureWithRoutingGivesItsExpectedSnapshot() {
        // The Bus (11) stands first in the Mixer; Drums (2) sends into it and is soloed; Loop (8) is not on the
        // solo path. The Track "Loop" is muted. The Sampler instrument "Loop" has the Bus as its own channel.
        val expected = SnapshotContent(
            tempoMilliBpm = 100_000,
            numerator = 4,
            denominator = 4,
            samples = listOf(Fixtures.KICK.hash, Fixtures.LOOP.hash),
            instruments = listOf(
                InstrumentRecord(
                    id = 3,
                    sample = 0,
                    rootNote = 60,
                    playMode = PlayMode.GATE,
                    audible = true,
                    gain = gain(-6f),
                    pan = 0f,
                    fineTuningCents = 0f,
                    envelope = 0,
                    startFrame = 0,
                    endFrame = 24_000
                ),
                InstrumentRecord(
                    id = 10,
                    sample = 1,
                    rootNote = 48,
                    playMode = PlayMode.ONE_SHOT,
                    audible = true,
                    gain = 1f,
                    pan = 0f,
                    fineTuningCents = 0f,
                    ownChannel = 0,
                    startFrame = 0,
                    endFrame = 480_000
                )
            ),
            envelopes = listOf(EnvelopeRecord(attackMs = 5f, decayMs = 100f, sustain = 0.5f, releaseMs = 200f)),
            patterns = listOf(PatternRecord(id = 4, lengthTicks = 3840, firstPart = 0, partCount = 2)),
            parts = listOf(PartRecord(instrument = 0, firstNote = 0, noteCount = 2), PartRecord(1, 2, 1)),
            notes = listOf(NoteRecord(0, 240, 60, 100), NoteRecord(960, 240, 64, 90), NoteRecord(0, 480, 48, 127)),
            tracks = listOf(
                TrackRecord(id = 1, channel = 1, firstClip = 0, clipCount = 2, audible = true),
                TrackRecord(id = 7, channel = 2, firstClip = 2, clipCount = 1, audible = false)
            ),
            clips = listOf(
                ClipRecord(5, ClipKind.PATTERN, start = 0, source = 0, offset = 0, length = 7680, gain = 1f),
                ClipRecord(6, ClipKind.AUDIO, start = 7680, source = 0, offset = 0, length = 24_000, gain = gain(-6f)),
                ClipRecord(9, ClipKind.AUDIO, start = 0, source = 1, offset = 4800, length = 96_000, gain = 1f)
            ),
            channels = listOf(
                ChannelRecord(id = 11, target = -1, gain = gain(-1f), pan = 0f, audible = true, firstSlot = 0),
                ChannelRecord(id = 2, target = 0, gain = 1f, pan = 0f, audible = true, firstSlot = 4),
                ChannelRecord(id = 8, target = -1, gain = gain(-3f), pan = -0.5f, audible = false, firstSlot = 8),
                ChannelRecord(id = 0, target = -1, gain = gain(-2f), pan = 0f, audible = true, firstSlot = 12)
            ),
            slots = List(4) { empty } + SlotRecord(12, EffectKind.EQ3, false, 0, 7) + List(3) { empty } +
                List(4) { empty } + SlotRecord(13, EffectKind.REVERB, false, 7, 4) + List(3) { empty },
            parameters = listOf(3f, 120f, 0f, 1000f, 0.9f, 0f, 8000f) + listOf(50f, 50f, 100f, 25f),
            order = listOf(1, 0, 2)
        )
        assertEquals(expected, SnapshotCompiler.compile(fixture("routing")))
    }

    @Test
    fun theBuildCompiledEveryFixture() {
        // The task compileFixtures of the build wrote one Snapshot per fixture, with the bytes of the compiler.
        val names = fixtures.listFiles { f -> f.extension == "json" }.orEmpty().map { it.nameWithoutExtension }.sorted()
        assertEquals(listOf("new-project", "routing"), names)
        for (name in names) {
            val bytes = File(snapshots, "$name.mdsn").readBytes()
            assertContentEquals(SnapshotCompiler.bytes(fixture(name)), bytes, name)
            assertEquals(EngineResult.OK, SnapshotCheck.check(bytes), name)
        }
    }

    @Test
    fun gainsFollowSection14() {
        assertEquals(1f, gain(0f))
        assertEquals(0f, gain(-90f))
        assertEquals(1.9952623f, gain(6f), 1e-6f)
        assertEquals(0.5011872f, gain(-6f), 1e-6f)
    }

    @Test
    fun everyChannelComesBeforeItsTarget() {
        // A chain C -> B -> A -> Master in the order A, B, C of the Mixer gives the order C, B, A.
        val channels = listOf(
            MixerChannel(id = 1, name = "A", color = 0),
            MixerChannel(id = 2, name = "B", color = 0, target = 1),
            MixerChannel(id = 3, name = "C", color = 0, target = 2),
            MixerChannel(id = 4, name = "D", color = 0)
        )
        assertEquals(listOf(2, 1, 0, 3), SnapshotCompiler.order(channels))
    }
}
