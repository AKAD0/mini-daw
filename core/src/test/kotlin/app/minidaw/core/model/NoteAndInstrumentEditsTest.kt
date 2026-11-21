package app.minidaw.core.model

import app.minidaw.core.model.Fixtures.edit
import app.minidaw.core.model.Fixtures.refuses
import app.minidaw.engine.api.snapshot.PlayMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoteAndInstrumentEditsTest {
    private val start = Fixtures.newProject()
    private val pattern = start.patterns.single().id
    private val kick = start.instruments.single().id

    private fun Project.notes(instrument: Int = kick): List<Note> =
        patterns.single { it.id == pattern }.parts.single { it.instrument == instrument }.notes

    @Test
    fun notesStaySortedByStartAndPitch() {
        val next = start.edit(AddNote(pattern, kick, Note(960, 240, 64, 100)))
            .edit(AddNote(pattern, kick, Note(0, 240, 67, 100)))
            .edit(AddNote(pattern, kick, Note(960, 240, 60, 90)))
        assertEquals(listOf(0 to 67, 960 to 60, 960 to 64), next.notes().map { it.start to it.pitch })
    }

    @Test
    fun aNoteOutsideItsRangesOrBehindTheEndIsRefused() {
        // AC-PT-02.
        start.refuses(AddNote(pattern, kick, Note(0, 240, 128, 100)), Reason.RANGE)
        start.refuses(AddNote(pattern, kick, Note(0, 240, -1, 100)), Reason.RANGE)
        start.refuses(AddNote(pattern, kick, Note(0, 240, 60, 0)), Reason.RANGE)
        start.refuses(AddNote(pattern, kick, Note(0, 240, 60, 128)), Reason.RANGE)
        start.refuses(AddNote(pattern, kick, Note(0, 29, 60, 100)), Reason.RANGE)
        start.refuses(AddNote(pattern, kick, Note(-240, 240, 60, 100)), Reason.RANGE)
        start.refuses(AddNote(pattern, kick, Note(3600, 241, 60, 100)), Reason.RANGE)
        start.edit(AddNote(pattern, kick, Note(3600, 240, 60, 100)))
    }

    @Test
    fun aPartHoldsOneNotePerStartAndPitch() {
        val one = start.edit(AddNote(pattern, kick, Note(0, 240, 60, 100)))
        one.refuses(AddNote(pattern, kick, Note(0, 480, 60, 50)), Reason.DUPLICATE)
        val two = one.edit(AddNote(pattern, kick, Note(480, 240, 60, 100)))
        two.refuses(MoveNote(pattern, kick, 480, 60, newStart = 0, newPitch = 60), Reason.DUPLICATE)
    }

    @Test
    fun moveResizeAndDeleteNameTheNoteByStartAndPitch() {
        val one = start.edit(AddNote(pattern, kick, Note(0, 240, 60, 100)))
        val moved = one.edit(MoveNote(pattern, kick, 0, 60, newStart = 480, newPitch = 62))
        assertEquals(listOf(Note(480, 240, 62, 100)), moved.notes())
        val resized = moved.edit(ResizeNote(pattern, kick, 480, 62, length = 960))
        assertEquals(listOf(Note(480, 960, 62, 100)), resized.notes())
        moved.refuses(ResizeNote(pattern, kick, 480, 62, length = 3841 - 480), Reason.RANGE)
        moved.refuses(DeleteNote(pattern, kick, 0, 60), Reason.NOT_FOUND)
        assertTrue(resized.edit(DeleteNote(pattern, kick, 480, 62)).notes().isEmpty())
    }

    @Test
    fun aStepButtonAddsAndRemovesANoteAtTheRootNote() {
        // AC-CR-01: a tap adds a Note at the Root note with a length of one Step; a second tap removes it.
        val on = start.edit(SwitchStep(pattern, kick, step = 4))
        assertEquals(listOf(Note(start = 960, length = 240, pitch = 60, velocity = 100)), on.notes())
        assertTrue(on.edit(SwitchStep(pattern, kick, step = 4)).notes().isEmpty())
        // A Note at another pitch on the same Step stays, and the tap adds the Root note beside it.
        val other = start.edit(AddNote(pattern, kick, Note(960, 480, 64, 80))).edit(SwitchStep(pattern, kick, 4))
        assertEquals(listOf(60, 64), other.notes().map { it.pitch })
        start.refuses(SwitchStep(pattern, kick, step = 16), Reason.RANGE)
        start.refuses(SwitchStep(pattern, kick, step = -1), Reason.RANGE)
    }

    @Test
    fun aPatternHoldsOnePartPerSamplerInstrument() {
        val snare = start.edit(AddSamplerInstrument(Fixtures.LOOP, name = "Loop")).instruments.last().id
        val two = start.edit(AddSamplerInstrument(Fixtures.LOOP, name = "Loop")).edit(AddPart(pattern, snare))
        assertEquals(listOf(kick, snare), two.patterns.single().parts.map { it.instrument })
        two.refuses(AddPart(pattern, snare), Reason.DUPLICATE)
        two.refuses(ReplacePartInstrument(pattern, kick, snare), Reason.DUPLICATE)
        val removed = two.edit(RemovePart(pattern, snare))
        assertEquals(listOf(kick), removed.patterns.single().parts.map { it.instrument })
    }

    @Test
    fun aReplacedInstrumentKeepsTheNotes() {
        val withNote = start.edit(AddNote(pattern, kick, Note(0, 240, 60, 100)))
            .edit(AddSamplerInstrument(Fixtures.LOOP))
        val loop = withNote.instruments.last()
        assertEquals("Loop", loop.name)
        val next = withNote.edit(ReplacePartInstrument(pattern, kick, loop.id))
        assertEquals(listOf(Note(0, 240, 60, 100)), next.notes(loop.id))
    }

    @Test
    fun theMvpHoldsAtMostEightSamplerInstrumentsInAPattern() {
        var state = start
        repeat(7) { state = state.edit(AddSamplerInstrument(Fixtures.LOOP)) }
        val ids = state.instruments.drop(1).map { it.id }
        ids.forEach { state = state.edit(AddPart(pattern, it)) }
        assertEquals(8, state.patterns.single().parts.size)
        val ninth = state.edit(AddSamplerInstrument(Fixtures.LOOP))
        ninth.refuses(AddPart(pattern, ninth.instruments.last().id), Reason.LIMIT)
    }

    @Test
    fun aDeletedSamplerInstrumentTakesItsPartsInAllPatterns() {
        val two = start.edit(AddPattern(instrument = kick))
        val next = two.edit(DeleteSamplerInstrument(kick))
        assertTrue(next.instruments.isEmpty())
        assertTrue(next.patterns.all { it.parts.isEmpty() })
    }

    @Test
    fun theValuesOfASamplerInstrumentKeepTheirRanges() {
        val next = start.edit(SetInstrumentValue(kick, InstrumentValue.RootNote(48)))
            .edit(SetInstrumentValue(kick, InstrumentValue.Mode(PlayMode.ONE_SHOT)))
            .edit(SetInstrumentValue(kick, InstrumentValue.VolumeDb(-6f)))
            .edit(SetInstrumentValue(kick, InstrumentValue.Name("Bass drum")))
        val changed = next.instruments.single()
        assertEquals("Bass drum", changed.name)
        assertEquals(48, changed.rootNote)
        assertEquals(PlayMode.ONE_SHOT, changed.playMode)
        assertEquals(-6f, changed.volumeDb)
        start.refuses(SetInstrumentValue(kick, InstrumentValue.RootNote(128)), Reason.RANGE)
        start.refuses(SetInstrumentValue(kick, InstrumentValue.VolumeDb(6.5f)), Reason.RANGE)
        start.refuses(SetInstrumentValue(kick, InstrumentValue.Pan(1.5f)), Reason.RANGE)
        start.refuses(SetInstrumentValue(kick, InstrumentValue.Color(12)), Reason.RANGE)
        start.refuses(SetInstrumentValue(999, InstrumentValue.Mute(true)), Reason.NOT_FOUND)
        // A step button uses the new Root note.
        assertEquals(48, next.edit(SwitchStep(pattern, kick, 0)).notes().single().pitch)
    }

    @Test
    fun theTempoLiesBetween30And300Bpm() {
        assertEquals(300_000, start.edit(SetTempo(300_000)).tempoMilliBpm)
        start.refuses(SetTempo(29_999), Reason.RANGE)
        start.refuses(SetTempo(300_001), Reason.RANGE)
        // A faster tempo may make two Audio clips overlap; the state stays valid (D-006).
        val track = start.tracks.single().id
        val clips = start.edit(AddAudioClip(track, Fixtures.KICK, 0)).edit(AddAudioClip(track, Fixtures.KICK, 960))
        val fast = clips.edit(SetTempo(240_000))
        val (a, b) = fast.tracks.single().clips
        assertTrue(ClipRules.overlap(a, b, fast.tempoMilliBpm))
    }

    @Test
    fun theTimeSignatureIsOneOfTheList() {
        assertEquals(TimeSignature(6, 8), start.edit(SetTimeSignature(TimeSignature(6, 8))).timeSignature)
        start.refuses(SetTimeSignature(TimeSignature(9, 8)), Reason.RANGE)
    }
}
