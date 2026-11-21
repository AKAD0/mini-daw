package app.minidaw.core.session

import app.minidaw.core.model.Edit
import app.minidaw.core.model.EditResult
import app.minidaw.core.model.Limits
import app.minidaw.core.model.Project
import app.minidaw.core.model.Reducer
import app.minidaw.engine.api.AudioEngine
import app.minidaw.engine.api.LiveCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether undo and redo have a step (for the buttons of the global bar). */
data class History(val canUndo: Boolean, val canRedo: Boolean)

/**
 * The open Project (architecture.md Sections 4.2 and 12.1): its current state as a stream of values, the Edits
 * through the Reducer, gestures, undo and redo, and the control of the Transport. It runs on the main thread only.
 *
 * Undo and redo hold whole states. States share their unchanged parts, so a step costs only the copied lists. The
 * lists live in memory only and start empty for each Project. A gesture of several Edits is one step of undo.
 */
class ProjectSession(
    initial: Project,
    private val engine: AudioEngine? = null,
    private val limits: Limits = Limits.MVP,
    private val capacity: Int = UNDO_STEPS
) {
    private val current = MutableStateFlow(initial)
    private val steps = MutableStateFlow(History(canUndo = false, canRedo = false))
    private val undoSteps = ArrayDeque<Project>()
    private val redoSteps = ArrayDeque<Project>()
    private var gestureStart: Project? = null

    /** The current state. */
    val state: StateFlow<Project> = current.asStateFlow()

    /** Whether undo and redo can act. */
    val history: StateFlow<History> = steps.asStateFlow()

    init {
        require(capacity >= UNDO_STEPS) { "undo keeps at least $UNDO_STEPS steps (FR-PRJ-04)" }
    }

    /** Applies an Edit. A refused Edit changes nothing. A new Edit empties the list for redo. */
    fun apply(edit: Edit): EditResult {
        val result = Reducer.reduce(current.value, edit, limits)
        if (result is EditResult.Applied) {
            if (gestureStart == null) push(current.value) else redoSteps.clear()
            current.value = result.state
            publish()
        }
        return result
    }

    /** Starts a gesture: the Edits until [endGesture] become one step of undo. */
    fun beginGesture() {
        if (gestureStart == null) gestureStart = current.value
    }

    fun endGesture() {
        val start = gestureStart ?: return
        gestureStart = null
        if (start != current.value) push(start)
        publish()
    }

    /** The state before the last step becomes current. Returns false when there is no step. */
    fun undo(): Boolean {
        endGesture()
        val previous = undoSteps.removeLastOrNull() ?: return false
        redoSteps.addLast(current.value)
        current.value = previous
        publish()
        return true
    }

    /** The state after the last undo becomes current again. Returns false when there is no step. */
    fun redo(): Boolean {
        endGesture()
        val next = redoSteps.removeLastOrNull() ?: return false
        undoSteps.addLast(current.value)
        current.value = next
        publish()
        return true
    }

    /** Opens another Project. Undo and redo start empty (architecture.md Section 4.2, rule 5). */
    fun open(project: Project) {
        gestureStart = null
        undoSteps.clear()
        redoSteps.clear()
        current.value = project
        publish()
    }

    /** Play, Stop, Seek and the other commands of the Transport go straight to the engine. */
    fun transport(command: LiveCommand) {
        engine?.send(command)
    }

    private fun push(state: Project) {
        undoSteps.addLast(state)
        if (undoSteps.size > capacity) undoSteps.removeFirst()
        redoSteps.clear()
    }

    private fun publish() {
        steps.value = History(canUndo = undoSteps.isNotEmpty(), canRedo = redoSteps.isNotEmpty())
    }

    companion object {
        /** At least 50 steps of undo (FR-PRJ-04, prov.). */
        const val UNDO_STEPS = 50
    }
}
