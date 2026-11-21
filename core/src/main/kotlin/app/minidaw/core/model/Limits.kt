package app.minidaw.core.model

/**
 * The limits of requirements.md Section 8 and of feature_specifications.md Section 1.5, in one place (all
 * provisional). The Reducer refuses an Edit that would pass one of them.
 */
data class Limits(
    val tracks: Int,
    val channels: Int,
    val instrumentsPerPattern: Int,
    val activeEffects: Int,
    val projectMinutes: Int = 10
) {
    companion object {
        val MVP = Limits(tracks = 8, channels = 8, instrumentsPerPattern = 8, activeEffects = 8)
        val VERSION_1_0 = Limits(tracks = 16, channels = 16, instrumentsPerPattern = 16, activeEffects = 16)

        const val MIN_PATTERN_BARS = 1
        const val MAX_PATTERN_BARS = 256
        const val NOTES_PER_PART = 4096
        const val MIN_NOTE_TICKS = 30
        const val MIN_AUDIO_FRAMES = 480
        const val NAME_LENGTH = 40
        const val COLORS = 12
        const val MIDI_MAX = 127
        const val MIN_DB = -90f
        const val MAX_DB = 6f
        const val MIN_CLIP_DB = -24f
        const val MAX_CLIP_DB = 12f
        const val DEFAULT_VELOCITY = 100
        const val DEFAULT_ROOT_NOTE = 60
    }
}
