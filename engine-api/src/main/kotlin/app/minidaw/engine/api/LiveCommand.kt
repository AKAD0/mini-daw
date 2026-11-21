package app.minidaw.engine.api

private const val MIDI_MAX = 127
private val MIDI_RANGE = 0..MIDI_MAX

/** What the Transport plays (FR-TR-02). */
enum class TransportMode {
    SONG,
    PATTERN
}

/** The parameters of [LiveCommand.SetValue] for a channel and for a Sampler instrument. */
object ValueParameter {
    /** The level of a channel or of a Sampler instrument, as a plain factor. */
    const val GAIN = 0

    /** The pan of a channel, from −1 to 1. */
    const val PAN = 1
}

/**
 * The Live commands of architecture.md Section 5.3. They go from the main thread to the audio thread and
 * change running values without a new Snapshot. Each names its target by the ID of the Project; the engine
 * drops a command for an ID that its Snapshot does not hold, and counts the drop.
 */
sealed interface LiveCommand {
    /** Starts the Transport with a short fade. */
    data object Play : LiveCommand

    /** Stops the Transport with a short fade. */
    data object Stop : LiveCommand

    /** Moves the playhead. Sounding Voices fade out. */
    data class Seek(val positionTicks: Int) : LiveCommand {
        init {
            require(positionTicks >= 0) { "a position is not negative: $positionTicks" }
        }
    }

    /** Song mode, or Pattern mode with the Pattern and the Track whose channel plays it (FR-TR-02, FR-MX-15). */
    data class SetMode(
        val mode: TransportMode,
        val patternId: Int = LiveCommand.NO_ID,
        val trackId: Int = LiveCommand.NO_ID
    ) : LiveCommand {
        init {
            require(mode == TransportMode.SONG || patternId != LiveCommand.NO_ID) { "Pattern mode names a Pattern" }
        }
    }

    /** The loop region of Song mode (FR-TR-11). */
    data class SetLoop(val startTicks: Int, val endTicks: Int, val enabled: Boolean) : LiveCommand {
        init {
            require(startTicks in 0 until endTicks) { "a loop starts before it ends: $startTicks to $endTicks" }
        }
    }

    /** A new anchor of the tempo while the user drags it, in thousandths of a BPM. */
    data class SetTempo(val milliBpm: Int) : LiveCommand {
        init {
            require(milliBpm > 0) { "the tempo must be above 0, not $milliBpm" }
        }
    }

    /**
     * A running value: a fader or a pan ([ValueParameter]), the volume of a Sampler instrument, or a parameter
     * of an effect, whose index follows the tables of feature_specifications.md Section 3.10. The engine ramps to
     * the value (FR-MX-13).
     */
    data class SetValue(val targetId: Int, val parameter: Int, val value: Float) : LiveCommand

    /** A key of the Piano roll, a step button or a new Note (FR-PT-05). */
    data class NoteOn(val instrumentId: Int, val pitch: Int, val velocity: Int, val trackId: Int) : LiveCommand {
        init {
            require(pitch in MIDI_RANGE) { "a pitch lies in $MIDI_RANGE, not $pitch" }
            require(velocity in 1..MIDI_MAX) { "a velocity lies in 1..$MIDI_MAX, not $velocity" }
        }
    }

    /** Ends a Note of [NoteOn]. */
    data class NoteOff(val instrumentId: Int, val pitch: Int, val trackId: Int) : LiveCommand {
        init {
            require(pitch in MIDI_RANGE) { "a pitch lies in $MIDI_RANGE, not $pitch" }
        }
    }

    /** Ends the preview of a Sample. */
    data object StopPreview : LiveCommand

    /** The metronome (FR-TR-09). */
    data class Metronome(val on: Boolean) : LiveCommand

    companion object {
        /** No Pattern or no Track. */
        const val NO_ID = -1
    }
}
