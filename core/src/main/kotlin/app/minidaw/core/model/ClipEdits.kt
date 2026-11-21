package app.minidaw.core.model

// The Edits of Clips (feature_specifications.md Sections 2.2.1 and 2.2.2). Split, gain and Edits of several
// Clips are P1 and come with version 1.0.

/** The Track that holds the Clip, and the Clip. */
internal fun Project.clip(id: Int): Pair<Track, Clip> {
    val track = tracks.firstOrNull { t -> t.clips.any { it.id == id } }
        ?: refuse(Reason.NOT_FOUND, "Clip $id does not exist")
    return track to track.clips.first { it.id == id }
}

/** Puts the Clip onto the Track in the order of starts. Refuses an overlap and a place behind the limit. */
internal fun Project.place(trackId: Int, clip: Clip, limits: Limits): Project {
    val track = track(trackId)
    if (clip.start < 0) refuse(Reason.RANGE, "a Clip starts at 0 or later")
    val others = track.clips.filter { it.id != clip.id }
    if (others.any { ClipRules.overlap(it, clip, tempoMilliBpm) }) {
        refuse(Reason.OVERLAP, "the place on Track $trackId is not free")
    }
    if (ClipRules.end(clip, tempoMilliBpm) > ClipRules.maxEnd(tempoMilliBpm, limits)) {
        refuse(Reason.LIMIT, "a Project lasts at most ${limits.projectMinutes} min")
    }
    return withTrack(track.copy(clips = (others + clip).sortedBy { it.start }))
}

private fun Project.withoutClip(id: Int): Project {
    val (track, _) = clip(id)
    return withTrack(track.copy(clips = track.clips.filter { it.id != id }))
}

private fun checkAudioRegion(offset: Int, length: Int, sampleFrames: Int) {
    if (offset < 0 || length < Limits.MIN_AUDIO_FRAMES || offset.toLong() + length > sampleFrames) {
        refuse(Reason.RANGE, "the region lies outside the Sample or is shorter than 10 ms")
    }
}

private fun checkPatternWindow(offset: Int, length: Int, pattern: Pattern) {
    if (length <= 0 || offset !in 0 until pattern.length) {
        refuse(Reason.RANGE, "a Pattern clip has a length, and its offset lies inside the Pattern")
    }
}

/** An Audio clip of the region `offset`, `length` (frames) of the Sample, which enters the table of used Samples. */
data class AddAudioClip(
    val track: Int,
    val sample: UsedSample,
    val start: Int,
    val offset: Int = 0,
    val length: Int = sample.frames
) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val known = state.samples.firstOrNull { it.hash == sample.hash }
        checkAudioRegion(offset, length, (known ?: sample).frames)
        val clip = AudioClip(id = state.nextId, sample = sample.hash, start = start, offset = offset, length = length)
        val listed = if (known == null) state.copy(samples = state.samples + sample) else state
        return listed.copy(nextId = state.nextId + 1).place(track, clip, limits)
    }
}

/** A Pattern clip; its default length is the length of the Pattern. */
data class AddPatternClip(
    val track: Int,
    val pattern: Int,
    val start: Int,
    val length: Int? = null,
    val offset: Int = 0
) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val played = state.pattern(pattern)
        val length = length ?: played.length
        checkPatternWindow(offset, length, played)
        val clip = PatternClip(id = state.nextId, pattern = pattern, start = start, length = length, offset = offset)
        return state.copy(nextId = state.nextId + 1).place(track, clip, limits)
    }
}

/** Moves a Clip in time and, if `track` differs, to another Track. */
data class MoveClip(val clip: Int, val track: Int, val start: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val (_, moved) = state.clip(clip)
        val placed = when (moved) {
            is AudioClip -> moved.copy(start = start)
            is PatternClip -> moved.copy(start = start)
        }
        return state.withoutClip(clip).place(track, placed, limits)
    }
}

/** New start, offset and length of a Clip (Section 2.2.2; [ClipRules.trimStart] computes them for an Audio clip). */
data class TrimClip(val clip: Int, val start: Int, val offset: Int, val length: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val (track, old) = state.clip(clip)
        val trimmed = when (old) {
            is AudioClip -> {
                checkAudioRegion(offset, length, state.samples.first { it.hash == old.sample }.frames)
                old.copy(start = start, offset = offset, length = length)
            }

            is PatternClip -> {
                checkPatternWindow(offset, length, state.pattern(old.pattern))
                old.copy(start = start, offset = offset, length = length)
            }
        }
        return state.place(track.id, trimmed, limits)
    }
}

/** A copy right after the Clip on its Track. Refused when this place is not free. */
data class DuplicateClip(val clip: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val (track, original) = state.clip(clip)
        val after = ClipRules.end(original, state.tempoMilliBpm)
        val copy = when (original) {
            is AudioClip -> original.copy(id = state.nextId, start = after)
            is PatternClip -> original.copy(id = state.nextId, start = after)
        }
        return state.copy(nextId = state.nextId + 1).place(track.id, copy, limits)
    }
}

data class DeleteClip(val clip: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project = state.withoutClip(clip)
}

/** Gives a Pattern clip its own copy of the Pattern, with the next default name (FR-PT-02). */
data class MakeUnique(val clip: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val (track, old) = state.clip(clip)
        if (old !is PatternClip) refuse(Reason.RANGE, "only a Pattern clip can be made unique")
        val original = state.pattern(old.pattern)
        val copy = original.copy(id = state.nextId, name = defaultName("Pattern", state.patterns.map { it.name }))
        val at = state.patterns.indexOf(original) + 1
        val patterns = state.patterns.take(at) + copy + state.patterns.drop(at)
        val clips = track.clips.map { if (it.id == clip) old.copy(pattern = copy.id) else it }
        return state.copy(nextId = state.nextId + 1, patterns = patterns).withTrack(track.copy(clips = clips))
    }
}
