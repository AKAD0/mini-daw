package app.minidaw.core.model

import app.minidaw.core.model.Fixtures.edit
import app.minidaw.core.model.Fixtures.refuses
import app.minidaw.engine.api.snapshot.SampleHash
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClipEditsTest {
    private val start = Fixtures.newProject().edit(AddTrack())
    private val first = start.tracks[0].id
    private val second = start.tracks[1].id
    private val pattern = start.patterns.single().id

    private fun Project.clips(track: Int): List<Clip> = tracks.single { it.id == track }.clips

    @Test
    fun anAudioClipHasItsLengthInTicksByTheTempo() {
        // 24,000 frames at 120 BPM: 25 frames per Tick, 960 Ticks.
        val next = start.edit(AddAudioClip(first, Fixtures.KICK, start = 0))
        val clip = next.clips(first).single() as AudioClip
        assertEquals(960, ClipRules.lengthTicks(clip, next.tempoMilliBpm))
        assertEquals(Triple(0, 24_000, 0), Triple(clip.offset, clip.length, clip.start))
    }

    @Test
    fun aNewSampleEntersTheTableOfUsedSamples() {
        val snare = UsedSample(SampleHash("33".repeat(SampleHash.BYTES)), name = "Snare", frames = 12_000, channels = 1)
        val next = start.edit(AddAudioClip(first, snare, start = 0))
        assertEquals(snare, next.samples.last())
        assertEquals(start.samples.size + 1, next.samples.size)
        assertEquals(next.samples.size, next.edit(AddAudioClip(second, snare, start = 0)).samples.size)
    }

    @Test
    fun theRegionOfAnAudioClipLiesInsideItsSample() {
        start.refuses(AddAudioClip(first, Fixtures.KICK, start = 0, offset = 1, length = 24_000), Reason.RANGE)
        start.refuses(AddAudioClip(first, Fixtures.KICK, start = 0, offset = -1, length = 1000), Reason.RANGE)
        start.refuses(AddAudioClip(first, Fixtures.KICK, start = 0, offset = 0, length = 479), Reason.RANGE)
        start.refuses(AddAudioClip(first, Fixtures.KICK, start = -1), Reason.RANGE)
    }

    @Test
    fun clipsOfOneTrackDoNotOverlap() {
        // Spans [0, 3840) and [3839, ...) share one Tick; [3840, ...) shares none.
        val one = start.edit(AddPatternClip(first, pattern, start = 0))
        one.refuses(AddPatternClip(first, pattern, start = 3839), Reason.OVERLAP)
        one.refuses(AddAudioClip(first, Fixtures.KICK, start = 3000), Reason.OVERLAP)
        val two = one.edit(AddPatternClip(first, pattern, start = 3840))
        assertEquals(listOf(0, 3840), two.clips(first).map { it.start })
        assertEquals(1, two.edit(AddPatternClip(second, pattern, start = 0)).clips(second).size)
    }

    @Test
    fun aPatternClipHasALengthAndAnOffsetInsideThePattern() {
        start.refuses(AddPatternClip(first, pattern, start = 0, length = 0), Reason.RANGE)
        start.refuses(AddPatternClip(first, pattern, start = 0, offset = 3840), Reason.RANGE)
        start.refuses(AddPatternClip(first, 999, start = 0), Reason.NOT_FOUND)
        val long = start.edit(AddPatternClip(first, pattern, start = 0, length = 4 * 3840, offset = 960))
        val clip = long.clips(first).single() as PatternClip
        // The place inside the Pattern: (960 + 6840 − 0) mod 3840 = 120.
        assertEquals(120, ClipRules.placeInPattern(clip, position = 6840, patternLength = 3840))
    }

    @Test
    fun aMovedClipKeepsItsOrderAndMayChangeItsTrack() {
        val two = start.edit(AddPatternClip(first, pattern, start = 0))
            .edit(AddPatternClip(first, pattern, start = 7680))
        val late = two.clips(first)[1].id
        val early = two.clips(first)[0].id
        val swapped = two.edit(MoveClip(early, first, 11_520)).edit(MoveClip(late, first, 0))
        assertEquals(listOf(late, early), swapped.clips(first).map { it.id })
        val moved = two.edit(MoveClip(late, second, start = 960))
        assertEquals(listOf(early), moved.clips(first).map { it.id })
        assertEquals(listOf(960), moved.clips(second).map { it.start })
        two.refuses(MoveClip(late, first, start = 1920), Reason.OVERLAP)
        two.refuses(MoveClip(late, first, start = -240), Reason.RANGE)
        two.refuses(MoveClip(999, first, start = 0), Reason.NOT_FOUND)
    }

    @Test
    fun aTrimOfTheStartMovesOffsetAndLengthByTheSameTime() {
        val placed = start.edit(AddAudioClip(first, Fixtures.LOOP, start = 3840))
        val clip = placed.clips(first).single() as AudioClip
        val trimmed = ClipRules.trimStart(clip, ticks = 480, tempoMilliBpm = placed.tempoMilliBpm)
        assertEquals(Triple(4320, 12_000, 468_000), Triple(trimmed.start, trimmed.offset, trimmed.length))
        val next = placed.edit(TrimClip(clip.id, trimmed.start, trimmed.offset, trimmed.length))
        assertEquals(trimmed, next.clips(first).single())
        val back = ClipRules.trimStart(trimmed, ticks = -480, tempoMilliBpm = placed.tempoMilliBpm)
        assertEquals(clip, back)
        placed.refuses(TrimClip(clip.id, start = 3840, offset = 0, length = 480_001), Reason.RANGE)
    }

    @Test
    fun aDuplicateStandsRightAfterItsClip() {
        val one = start.edit(AddAudioClip(first, Fixtures.KICK, start = 960))
        val clip = one.clips(first).single()
        val two = one.edit(DuplicateClip(clip.id))
        assertEquals(listOf(960, 1920), two.clips(first).map { it.start })
        val blocked = one.edit(AddPatternClip(first, pattern, start = 1920))
        blocked.refuses(DuplicateClip(clip.id), Reason.OVERLAP)
    }

    @Test
    fun aDeletedClipLeavesItsTrack() {
        val one = start.edit(AddPatternClip(first, pattern, start = 0))
        val next = one.edit(DeleteClip(one.clips(first).single().id))
        assertTrue(next.clips(first).isEmpty())
        assertEquals(one.patterns, next.patterns)
    }

    @Test
    fun makeUniqueGivesOneClipItsOwnPattern() {
        val two = start.edit(AddPatternClip(first, pattern, start = 0))
            .edit(AddPatternClip(second, pattern, start = 0))
        val clip = two.clips(first).single().id
        val next = two.edit(MakeUnique(clip))
        val copy = next.patterns[1]
        assertEquals(listOf("Pattern 1", "Pattern 2"), next.patterns.map { it.name })
        assertEquals((next.clips(first).single() as PatternClip).pattern, copy.id)
        assertEquals(pattern, (next.clips(second).single() as PatternClip).pattern)
        assertEquals(start.patterns.single().parts, copy.parts)
        next.edit(AddAudioClip(first, Fixtures.KICK, start = 3840)).let { state ->
            state.refuses(MakeUnique(state.clips(first).last().id), Reason.RANGE)
        }
    }

    @Test
    fun aClipEndsInsideTheLengthLimitOfTheProject() {
        // 10 min at 120 BPM are 1,152,000 Ticks.
        assertEquals(1_152_000L, ClipRules.maxEnd(start.tempoMilliBpm, Limits.MVP))
        start.edit(AddPatternClip(first, pattern, start = 1_152_000 - 3840))
        start.refuses(AddPatternClip(first, pattern, start = 1_152_000 - 3839), Reason.LIMIT)
    }

    @Test
    fun noSequenceOfEditsMakesTwoClipsOfOneTrackOverlap() {
        // AC-PL-01, with a fixed seed: 3,000 random Edits of Clips on three Tracks.
        val random = Random(20_261_121)
        var state = start.edit(AddTrack())
        var applied = 0
        repeat(3000) {
            val result = Reducer.reduce(state, randomEdit(random, state))
            if (result is EditResult.Applied) {
                state = result.state
                applied++
            }
            for (track in state.tracks) {
                track.clips.forEachIndexed { i, a ->
                    track.clips.drop(i + 1).forEach { b -> assertFalse(ClipRules.overlap(a, b, state.tempoMilliBpm)) }
                }
            }
            assertEquals(emptyList(), Invariants.check(state, Limits.MVP))
        }
        // The sequence must reach crowded Tracks, not only refusals.
        assertTrue(applied > 300, "only $applied Edits were applied")
    }

    private fun randomEdit(random: Random, state: Project): Edit {
        val track = state.tracks[random.nextInt(state.tracks.size)].id
        val at = random.nextInt(0, 64) * 240
        val clip = state.tracks.flatMap { it.clips }.randomOrNull(random)?.id
        return if (clip == null || random.nextInt(3) == 0) {
            addEdit(random, track, at)
        } else {
            changeEdit(random, clip, track, at)
        }
    }

    private fun addEdit(random: Random, track: Int, at: Int): Edit = if (random.nextBoolean()) {
        AddPatternClip(track, pattern, at, length = random.nextInt(1, 33) * 240, offset = random.nextInt(16) * 240)
    } else {
        AddAudioClip(track, Fixtures.KICK, at, offset = random.nextInt(0, 12_000), length = random.nextInt(480, 12_000))
    }

    private fun changeEdit(random: Random, clip: Int, track: Int, at: Int): Edit = when (random.nextInt(4)) {
        0 -> MoveClip(clip, track, at)
        1 -> TrimClip(clip, at, offset = random.nextInt(0, 3840), length = random.nextInt(1, 8000))
        2 -> DuplicateClip(clip)
        else -> DeleteClip(clip)
    }
}
