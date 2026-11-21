package app.minidaw.core.model

/** The rules for Clips of feature_specifications.md Section 2.2.2. Positions and lengths are Ticks. */
object ClipRules {
    private const val SECONDS_PER_MINUTE = 60
    private const val MILLI = 1000

    /** The length on the timeline. An Audio clip keeps its frames, so its length in Ticks follows the tempo. */
    fun lengthTicks(clip: Clip, tempoMilliBpm: Int): Int = when (clip) {
        is AudioClip -> TimeBase.audioLengthTicks(clip.length, tempoMilliBpm)
        is PatternClip -> clip.length
    }

    /** The first Tick after the Clip. */
    fun end(clip: Clip, tempoMilliBpm: Int): Int = clip.start + lengthTicks(clip, tempoMilliBpm)

    /** Two Clips of one Track overlap when their spans in Ticks share at least one Tick. */
    fun overlap(a: Clip, b: Clip, tempoMilliBpm: Int): Boolean =
        a.start < end(b, tempoMilliBpm) && b.start < end(a, tempoMilliBpm)

    /** The place inside the Pattern that a Pattern clip plays at `position`: (offset + position − start) mod length. */
    fun placeInPattern(clip: PatternClip, position: Int, patternLength: Int): Int =
        Math.floorMod(clip.offset + position - clip.start, patternLength)

    /**
     * Moves the start of an Audio clip by `ticks` (negative: earlier). The offset moves by the same time, rounded to
     * whole frames, and the length changes by the same number of frames.
     */
    fun trimStart(clip: AudioClip, ticks: Int, tempoMilliBpm: Int): AudioClip {
        val magnitude = TimeBase.framesOfTicks(Math.abs(ticks), tempoMilliBpm)
        val frames = if (ticks < 0) -magnitude else magnitude
        return clip.copy(start = clip.start + ticks, offset = clip.offset + frames, length = clip.length - frames)
    }

    /** The last Tick that a Clip may reach: the limit of the length of a Project at this tempo. */
    fun maxEnd(tempoMilliBpm: Int, limits: Limits): Long = limits.projectMinutes.toLong() * SECONDS_PER_MINUTE *
        TimeBase.TICKS_PER_QUARTER * tempoMilliBpm / (SECONDS_PER_MINUTE * MILLI)
}
