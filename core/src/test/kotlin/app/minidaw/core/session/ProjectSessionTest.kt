package app.minidaw.core.session

import app.minidaw.core.model.AddAudioClip
import app.minidaw.core.model.AddNote
import app.minidaw.core.model.AddPattern
import app.minidaw.core.model.AddPatternClip
import app.minidaw.core.model.AddSamplerInstrument
import app.minidaw.core.model.AddTrack
import app.minidaw.core.model.DeletePattern
import app.minidaw.core.model.DeleteTrack
import app.minidaw.core.model.Edit
import app.minidaw.core.model.EditResult
import app.minidaw.core.model.Fixtures
import app.minidaw.core.model.InstrumentValue
import app.minidaw.core.model.MakeUnique
import app.minidaw.core.model.MoveClip
import app.minidaw.core.model.Note
import app.minidaw.core.model.Project
import app.minidaw.core.model.RenameTrack
import app.minidaw.core.model.SetInstrumentValue
import app.minidaw.core.model.SetPatternLength
import app.minidaw.core.model.SetTempo
import app.minidaw.core.model.SwitchStep
import app.minidaw.engine.api.FakeAudioEngine
import app.minidaw.engine.api.LiveCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProjectSessionTest {
    private val start = Fixtures.newProject()
    private val track = start.tracks.single().id
    private val pattern = start.patterns.single().id
    private val kick = start.instruments.single().id

    /** One Edit of each kind of the model; the IDs follow from the order (nextId starts at 5). */
    private val edits: List<Edit> = listOf(
        AddTrack(),
        RenameTrack(track, "Drums"),
        AddPatternClip(track, pattern, start = 0),
        AddAudioClip(track, Fixtures.KICK, start = 3840),
        MoveClip(7, track, start = 7680),
        AddNote(pattern, kick, Note(0, 240, 60, 100)),
        SwitchStep(pattern, kick, step = 4),
        SetPatternLength(pattern, 2),
        MakeUnique(7),
        AddSamplerInstrument(Fixtures.LOOP),
        SetInstrumentValue(kick, InstrumentValue.VolumeDb(-3f)),
        SetTempo(96_000),
        AddPattern(),
        DeletePattern(pattern),
        DeleteTrack(track)
    )

    @Test
    fun undoRestoresTheExactEarlierStateAndRedoTheLaterOne() {
        // AC-PL-06, after every Edit of the list.
        val session = ProjectSession(start)
        for (edit in edits) {
            val before = session.state.value
            assertIs<EditResult.Applied>(session.apply(edit), "$edit")
            val after = session.state.value
            assertTrue(session.undo())
            assertEquals(before, session.state.value, "undo after $edit")
            assertTrue(session.redo())
            assertEquals(after, session.state.value, "redo after $edit")
        }
        // Back through every step to the start, and forward again.
        repeat(edits.size) { assertTrue(session.undo()) }
        assertEquals(start, session.state.value)
        assertFalse(session.undo())
        repeat(edits.size) { assertTrue(session.redo()) }
        assertFalse(session.redo())
    }

    @Test
    fun atLeastFiftyStepsAreKeptAndANewEditDropsRedo() {
        // AC-PRJ-07.
        val session = ProjectSession(start)
        repeat(60) { session.apply(SetTempo(60_000 + 1000 * it)) }
        var undone = 0
        while (session.undo()) undone++
        assertEquals(50, undone)
        assertEquals(60_000 + 1000 * 9, session.state.value.tempoMilliBpm)
        session.redo()
        session.apply(SetTempo(150_000))
        assertFalse(session.history.value.canRedo)
        assertFalse(session.redo())
    }

    @Test
    fun oneUndoRestoresADeletedTrackWithItsClips() {
        // AC-PL-11, the part of the undo.
        val session = ProjectSession(start)
        session.apply(AddPatternClip(track, pattern, start = 0))
        val withClip = session.state.value
        session.apply(DeleteTrack(track))
        assertTrue(session.state.value.tracks.isEmpty())
        session.undo()
        assertEquals(withClip.tracks, session.state.value.tracks)
    }

    @Test
    fun oneUndoRestoresAllPatternClipsOfADeletedPattern() {
        // AC-PT-05, the part of the undo.
        val session = ProjectSession(start)
        session.apply(AddTrack())
        val second = session.state.value.tracks.last().id
        session.apply(AddPatternClip(track, pattern, start = 0))
        session.apply(AddPatternClip(second, pattern, start = 3840))
        val before = session.state.value
        session.apply(DeletePattern(pattern))
        assertEquals(0, session.state.value.tracks.sumOf { it.clips.size })
        session.undo()
        assertEquals(before, session.state.value)
        assertEquals(2, session.state.value.tracks.sumOf { it.clips.size })
    }

    @Test
    fun aGestureOfSeveralEditsIsOneStepOfUndo() {
        val session = ProjectSession(start)
        session.beginGesture()
        (0..3).forEach { session.apply(SwitchStep(pattern, kick, it)) }
        session.endGesture()
        assertEquals(4, notes(session.state.value).size)
        session.undo()
        assertEquals(start, session.state.value)
        assertFalse(session.history.value.canUndo)
        // A gesture without a change leaves no step.
        session.beginGesture()
        session.apply(SwitchStep(pattern, kick, 99))
        session.endGesture()
        assertFalse(session.history.value.canUndo)
        assertTrue(session.history.value.canRedo)
    }

    @Test
    fun aRefusedEditChangesNothing() {
        val session = ProjectSession(start)
        assertIs<EditResult.Refused>(session.apply(SetTempo(1)))
        assertEquals(start, session.state.value)
        assertEquals(History(canUndo = false, canRedo = false), session.history.value)
    }

    @Test
    fun anotherProjectStartsWithEmptyLists() {
        val session = ProjectSession(start)
        session.apply(AddTrack())
        session.open(start.copy(name = "Other"))
        assertEquals("Other", session.state.value.name)
        assertFalse(session.undo())
    }

    @Test
    fun theTransportGoesToTheEngine() {
        val engine = FakeAudioEngine()
        val session = ProjectSession(start, engine)
        session.transport(LiveCommand.Play)
        session.transport(LiveCommand.Seek(3840))
        session.transport(LiveCommand.Stop)
        assertEquals(listOf(LiveCommand.Play, LiveCommand.Seek(3840), LiveCommand.Stop), engine.commands)
    }

    private fun notes(state: Project): List<Note> = state.patterns.single().parts.single().notes
}
