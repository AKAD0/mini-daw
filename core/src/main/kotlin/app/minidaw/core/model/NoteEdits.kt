package app.minidaw.core.model

// The Edits of Parts and Notes (feature_specifications.md Section 2.2.1). A Note is identified inside its Part by
// its start and its pitch. Edits of several Notes are P1 and come with version 1.0.

private fun Pattern.part(instrument: Int): Part =
    parts.firstOrNull { it.instrument == instrument }
        ?: refuse(Reason.NOT_FOUND, "Pattern $id has no Part of Sampler instrument $instrument")

private fun Pattern.withPart(part: Part): Pattern =
    copy(parts = parts.map { if (it.instrument == part.instrument) part else it })

private fun Part.note(start: Int, pitch: Int): Note = notes.firstOrNull { it.start == start && it.pitch == pitch }
    ?: refuse(Reason.NOT_FOUND, "no Note of pitch $pitch starts at $start")

/** Checks a Note against its ranges (FR-PT-03) and the end of the Pattern, and puts it in the order of the Part. */
private fun Part.withNote(note: Note, pattern: Pattern): Part {
    if (!NoteRules.inRange(note)) {
        refuse(Reason.RANGE, "a Note has a pitch of 0 to 127, a velocity of 1 to 127 and at least 30 Ticks")
    }
    if (note.start + note.length > pattern.length) refuse(Reason.RANGE, "a Note ends inside its Pattern")
    if (notes.any { it.start == note.start && it.pitch == note.pitch }) {
        refuse(Reason.DUPLICATE, "a Note of pitch ${note.pitch} starts at ${note.start} already")
    }
    if (notes.size >= Limits.NOTES_PER_PART) {
        refuse(Reason.LIMIT, "a Part holds at most ${Limits.NOTES_PER_PART} Notes")
    }
    return copy(notes = (notes + note).sortedWith(compareBy({ it.start }, { it.pitch })))
}

private fun Part.without(note: Note): Part = copy(notes = notes - note)

/** Applies a change to one Part of a Pattern. */
private fun Project.changePart(pattern: Int, instrument: Int, change: (Part, Pattern) -> Part): Project {
    val target = pattern(pattern)
    return withPattern(target.withPart(change(target.part(instrument), target)))
}

/** At most one Part per Sampler instrument, and at most the limit of Sampler instruments per Pattern. */
data class AddPart(val pattern: Int, val instrument: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val target = state.pattern(pattern)
        state.instrument(instrument)
        if (target.parts.any { it.instrument == instrument }) {
            refuse(Reason.DUPLICATE, "the Pattern has this Part already")
        }
        if (target.parts.size >= limits.instrumentsPerPattern) {
            refuse(Reason.LIMIT, "a Pattern holds at most ${limits.instrumentsPerPattern} Sampler instruments")
        }
        return state.withPattern(target.copy(parts = target.parts + Part(instrument)))
    }
}

/** Removes a Part with its Notes. */
data class RemovePart(val pattern: Int, val instrument: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val target = state.pattern(pattern)
        target.part(instrument)
        return state.withPattern(target.copy(parts = target.parts.filter { it.instrument != instrument }))
    }
}

/** The Notes of a Part go to another Sampler instrument, which has no Part in this Pattern yet. */
data class ReplacePartInstrument(val pattern: Int, val from: Int, val to: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val target = state.pattern(pattern)
        state.instrument(to)
        if (target.parts.any { it.instrument == to }) refuse(Reason.DUPLICATE, "the new Sampler instrument has a Part")
        val part = target.part(from)
        val parts = target.parts.map { if (it == part) part.copy(instrument = to) else it }
        return state.withPattern(target.copy(parts = parts))
    }
}

data class AddNote(val pattern: Int, val instrument: Int, val note: Note) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project =
        state.changePart(pattern, instrument) { part, owner -> part.withNote(note, owner) }
}

/** Moves the Note that starts at `start` with `pitch` to a new start and pitch. */
data class MoveNote(
    val pattern: Int,
    val instrument: Int,
    val start: Int,
    val pitch: Int,
    val newStart: Int,
    val newPitch: Int
) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project =
        state.changePart(pattern, instrument) { part, owner ->
            val old = part.note(start, pitch)
            part.without(old).withNote(old.copy(start = newStart, pitch = newPitch), owner)
        }
}

data class ResizeNote(val pattern: Int, val instrument: Int, val start: Int, val pitch: Int, val length: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project =
        state.changePart(pattern, instrument) { part, owner ->
            val old = part.note(start, pitch)
            part.without(old).withNote(old.copy(length = length), owner)
        }
}

data class DeleteNote(val pattern: Int, val instrument: Int, val start: Int, val pitch: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project =
        state.changePart(pattern, instrument) { part, _ -> part.without(part.note(start, pitch)) }
}

/**
 * A tap on a step button (FR-CR-05): a Note at the Root note that starts on the Step goes away; otherwise one with
 * the length of one Step and the velocity 100 appears.
 */
data class SwitchStep(val pattern: Int, val instrument: Int, val step: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val root = state.instrument(instrument).rootNote
        val at = step * TimeBase.STEP
        return state.changePart(pattern, instrument) { part, owner ->
            val there = part.notes.firstOrNull { it.start == at && it.pitch == root }
            if (there != null) {
                part.without(there)
            } else {
                if (step < 0) refuse(Reason.RANGE, "a Step is not negative")
                part.withNote(Note(at, TimeBase.STEP, root, Limits.DEFAULT_VELOCITY), owner)
            }
        }
    }
}
