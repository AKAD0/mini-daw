package app.minidaw.core.model

import app.minidaw.core.model.Fixtures.edit
import app.minidaw.core.model.Fixtures.refuses
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackAndPatternEditsTest {
    private val start = Fixtures.newProject()
    private val track = start.tracks.single()
    private val pattern = start.patterns.single()

    @Test
    fun aNewProjectHasTheContentOfSection17() {
        // AC-TR-09: 4/4. Also 120 BPM, one Track with its Mixer channel, one Pattern of one bar with one Part.
        assertEquals(TimeSignature(4, 4), start.timeSignature)
        assertEquals(120_000, start.tempoMilliBpm)
        assertEquals("2026-11-20 07:55", start.name)
        assertEquals("Track 1", track.name)
        val channel = start.mixer.channels.single()
        assertEquals(track.channel, channel.id)
        assertEquals(track.name to track.color, channel.name to channel.color)
        assertEquals(0f, channel.volumeDb)
        assertEquals(3840, pattern.length)
        assertEquals(listOf(start.instruments.single().id), pattern.parts.map { it.instrument })
        assertEquals(emptyList(), Invariants.check(start, Limits.MVP))
    }

    @Test
    fun aNewTrackGetsAMixerChannelWithItsNameAndColor() {
        val next = start.edit(AddTrack())
        val added = next.tracks.last()
        assertEquals("Track 2", added.name)
        assertEquals(1, added.color)
        val channel = next.mixer.channels.single { it.id == added.channel }
        assertEquals("Track 2" to 1, channel.name to channel.color)
        assertEquals(added.channel + 1, next.nextId)
    }

    @Test
    fun renameRecolorAndMuteChangeOnlyTheirTrack() {
        // AC-PL-11, part 1: add, rename, recolor and delete give the states of Section 2.2.1.
        val next = start.edit(AddTrack()).edit(RenameTrack(track.id, "Drums"))
            .edit(RecolorTrack(track.id, 5)).edit(MuteTrack(track.id, true))
        val changed = next.tracks.first()
        assertEquals(Triple("Drums", 5, true), Triple(changed.name, changed.color, changed.mute))
        assertEquals("Track 2", next.tracks.last().name)
        assertEquals("Track 1", next.mixer.channels.first().name, "the Mixer channel keeps its own name")
    }

    @Test
    fun aDeletedTrackTakesItsClipsAndLeavesItsChannel() {
        val clip = PatternClip(id = start.nextId, pattern = pattern.id, start = 0, length = 3840)
        val withClip = start.copy(
            nextId = start.nextId + 1,
            tracks = listOf(track.copy(clips = listOf(clip)))
        )
        val next = withClip.edit(DeleteTrack(track.id))
        assertTrue(next.tracks.isEmpty())
        assertEquals(withClip.mixer, next.mixer)
        // The state before stays whole, so that one undo restores the Track with its Clips (TK-017).
        assertEquals(listOf(clip), withClip.tracks.single().clips)
    }

    @Test
    fun trackEditsCheckTheirValues() {
        start.refuses(RenameTrack(track.id, "x".repeat(41)), Reason.RANGE)
        start.refuses(RenameTrack(track.id, " "), Reason.RANGE)
        start.refuses(RecolorTrack(track.id, 12), Reason.RANGE)
        start.refuses(MuteTrack(999, true), Reason.NOT_FOUND)
        start.refuses(DeleteTrack(999), Reason.NOT_FOUND)
    }

    @Test
    fun theMvpHasAtMostEightTracks() {
        val full = (2..8).fold(start) { state, _ -> state.edit(AddTrack()) }
        assertEquals(8, full.tracks.size)
        full.refuses(AddTrack(), Reason.LIMIT)
        assertEquals(9, full.edit(AddTrack(), Limits.VERSION_1_0).tracks.size)
    }

    @Test
    fun moveTrackChangesTheOrder() {
        val three = start.edit(AddTrack()).edit(AddTrack())
        val moved = three.edit(MoveTrack(track.id, 2))
        assertEquals(listOf("Track 2", "Track 3", "Track 1"), moved.tracks.map { it.name })
        three.refuses(MoveTrack(track.id, 3), Reason.RANGE)
    }

    @Test
    fun aNewPatternHasOneBarAndTheNextName() {
        val next = start.edit(AddPattern(instrument = start.instruments.single().id))
        val added = next.patterns.last()
        assertEquals("Pattern 2", added.name)
        assertEquals(3840, added.length)
        assertEquals(1, added.parts.size)
        start.refuses(AddPattern(bars = 0), Reason.RANGE)
        start.refuses(AddPattern(bars = 257), Reason.RANGE)
        start.refuses(AddPattern(instrument = 999), Reason.NOT_FOUND)
    }

    @Test
    fun aShorterPatternMayNotCutNotes() {
        val note = Note(start = 3840 + 960, length = 240, pitch = 60, velocity = 100)
        val part = Part(pattern.parts.single().instrument, listOf(note))
        val long = start.edit(SetPatternLength(pattern.id, 2)).let { state ->
            state.withPattern(state.patterns.single().copy(parts = listOf(part)))
        }
        long.refuses(SetPatternLength(pattern.id, 1), Reason.CONFLICT)
        assertEquals(4 * 3840, long.edit(SetPatternLength(pattern.id, 4)).patterns.single().length)
    }

    @Test
    fun aDeletedPatternTakesAllItsPatternClips() {
        // AC-PT-05: the deletion of a Pattern removes its Pattern clips. The state before keeps them (TK-017).
        val second = start.edit(AddPattern()).patterns.last()
        val two = start.edit(AddPattern()).edit(AddTrack())
        val tracks = two.tracks.mapIndexed { i, t ->
            t.copy(
                clips = listOf(
                    PatternClip(id = 100 + 2 * i, pattern = pattern.id, start = 0, length = 3840),
                    PatternClip(id = 101 + 2 * i, pattern = second.id, start = 3840, length = 3840)
                )
            )
        }
        val withClips = two.copy(tracks = tracks, nextId = 200)
        val next = withClips.edit(DeletePattern(pattern.id))
        assertEquals(listOf(second.id), next.patterns.map { it.id })
        assertTrue(next.tracks.all { t -> t.clips.all { (it as PatternClip).pattern == second.id } })
        assertEquals(2, next.tracks.sumOf { it.clips.size })
        assertEquals(4, withClips.tracks.sumOf { it.clips.size })
    }

    @Test
    fun aDuplicateStandsRightAfterItsPattern() {
        val two = start.edit(AddPattern())
        val next = two.edit(DuplicatePattern(pattern.id))
        assertEquals(listOf("Pattern 1", "Pattern 3", "Pattern 2"), next.patterns.map { it.name })
        assertEquals(pattern.parts, next.patterns[1].parts)
        assertFalse(next.patterns[1].id == pattern.id)
    }

    @Test
    fun theInvariantsFindBrokenStates() {
        val loop = start.copy(
            mixer = start.mixer.copy(channels = start.mixer.channels.map { it.copy(target = it.id) })
        )
        assertTrue(Invariants.check(loop, Limits.MVP).any { "loop" in it })
        val missing = start.copy(tracks = listOf(track.copy(channel = 999)))
        assertTrue(Invariants.check(missing, Limits.MVP).any { "missing Mixer channel" in it })
        val twice = start.copy(patterns = start.patterns + pattern)
        assertTrue(Invariants.check(twice, Limits.MVP).any { "share an ID" in it })
    }
}
