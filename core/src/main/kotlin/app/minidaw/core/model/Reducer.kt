package app.minidaw.core.model

/**
 * The Reducer of architecture.md Section 4.2: a pure function from a state and an Edit to a new state. It
 * refuses an Edit that fails one of its checks or that would break an invariant; the state then stays unchanged.
 */
object Reducer {
    fun reduce(state: Project, edit: Edit, limits: Limits = Limits.MVP): EditResult = try {
        val next = edit.applyTo(state, limits)
        val broken = Invariants.check(next, limits)
        if (broken.isEmpty()) {
            EditResult.Applied(next)
        } else {
            EditResult.Refused(Refusal(Reason.INVARIANT, broken.first()))
        }
    } catch (refused: EditRefused) {
        EditResult.Refused(refused.refusal)
    }
}
