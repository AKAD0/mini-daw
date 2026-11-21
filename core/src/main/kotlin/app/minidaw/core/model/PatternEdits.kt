package app.minidaw.core.model

// The Edits of Patterns (feature_specifications.md Section 2.2.1). Lengths are whole bars of the time signature.

private fun Project.patternTicks(bars: Int): Int {
    if (bars !in Limits.MIN_PATTERN_BARS..Limits.MAX_PATTERN_BARS) {
        refuse(Reason.RANGE, "a Pattern has ${Limits.MIN_PATTERN_BARS} to ${Limits.MAX_PATTERN_BARS} bars")
    }
    return bars * TimeBase.barTicks(timeSignature)
}

/** A new Pattern at the end of the list, by default of 1 bar. `instrument` gives it a first Part. */
data class AddPattern(
    val name: String? = null,
    val color: Int? = null,
    val bars: Int = 1,
    val instrument: Int? = null
) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val name = name ?: defaultName("Pattern", state.patterns.map { it.name })
        val color = color ?: nextColor(state.patterns.map { it.color })
        checkName(name)
        checkColor(color)
        val parts = instrument?.let { listOf(Part(state.instrument(it).id)) } ?: emptyList()
        val length = state.patternTicks(bars)
        val pattern = Pattern(id = state.nextId, name = name, color = color, length = length, parts = parts)
        return state.copy(nextId = state.nextId + 1, patterns = state.patterns + pattern)
    }
}

data class RenamePattern(val pattern: Int, val name: String) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        checkName(name)
        return state.withPattern(state.pattern(pattern).copy(name = name))
    }
}

data class RecolorPattern(val pattern: Int, val color: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        checkColor(color)
        return state.withPattern(state.pattern(pattern).copy(color = color))
    }
}

/** A copy with the next default name, right after the Pattern in the list. */
data class DuplicatePattern(val pattern: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val original = state.pattern(pattern)
        val copy = original.copy(id = state.nextId, name = defaultName("Pattern", state.patterns.map { it.name }))
        val at = state.patterns.indexOf(original) + 1
        return state.copy(
            nextId = state.nextId + 1,
            patterns = state.patterns.take(at) + copy + state.patterns.drop(at)
        )
    }
}

/** No Note may lie behind the new end, and no Pattern clip may start its window behind it. */
data class SetPatternLength(val pattern: Int, val bars: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val old = state.pattern(pattern)
        val length = state.patternTicks(bars)
        if (old.parts.any { part -> part.notes.any { it.start + it.length > length } }) {
            refuse(Reason.CONFLICT, "Notes lie behind the new end of the Pattern")
        }
        val windows = state.tracks.flatMap { it.clips }.filterIsInstance<PatternClip>()
            .filter { it.pattern == pattern }
        if (windows.any { it.offset >= length }) refuse(Reason.CONFLICT, "a Pattern clip starts behind the new end")
        return state.withPattern(old.copy(length = length))
    }
}

/** Deletes a Pattern and all its Pattern clips (FR-PT-08). */
data class DeletePattern(val pattern: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        state.pattern(pattern)
        return state.copy(
            patterns = state.patterns.filter { it.id != pattern },
            tracks = state.tracks.map { track ->
                if (track.clips.none { it is PatternClip && it.pattern == pattern }) {
                    track
                } else {
                    track.copy(clips = track.clips.filterNot { it is PatternClip && it.pattern == pattern })
                }
            }
        )
    }
}
