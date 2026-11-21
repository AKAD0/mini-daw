package app.minidaw.core.model

/** Why the Reducer refused an Edit. The UI names the reason in a Hint (ui_ux_specification.md Section 16.3). */
enum class Reason {
    /** A referenced item does not exist. */
    NOT_FOUND,

    /** A limit of requirements.md Section 8 or of feature_specifications.md Section 1.5. */
    LIMIT,

    /** Two Clips of one Track would overlap (D-006). */
    OVERLAP,

    /** A value lies outside its range. */
    RANGE,

    /** The item exists already, for example a second Part of one Sampler instrument in a Pattern. */
    DUPLICATE,

    /** Other items stand in the way, for example Notes behind a new end of a Pattern. */
    CONFLICT,

    /** The new state would break an invariant of architecture.md Section 4.2. */
    INVARIANT
}

data class Refusal(val reason: Reason, val detail: String)

/** The result of an Edit: a new state, or the reason why the state stays unchanged. */
sealed interface EditResult {
    data class Applied(val state: Project) : EditResult

    data class Refused(val refusal: Refusal) : EditResult
}

/**
 * An Edit of the Project (architecture.md Section 4.2). Each Edit knows how it changes a state and throws
 * [EditRefused] when one of its checks fails; [Reducer] turns this into [EditResult.Refused].
 */
interface Edit {
    fun applyTo(state: Project, limits: Limits): Project
}

/** Thrown by an Edit that fails a check. It never leaves the Reducer. */
class EditRefused(val refusal: Refusal) : RuntimeException(refusal.detail)

/** Refuses the Edit that is being applied. */
fun refuse(reason: Reason, detail: String): Nothing = throw EditRefused(Refusal(reason, detail))
