package app.minidaw.core.model

import app.minidaw.engine.api.snapshot.PlayMode

// The Edits of Sampler instruments and of the whole Project (feature_specifications.md Section 2.2.1).

/**
 * A new Sampler instrument on a Sample, which enters the table of used Samples. The caller passes the Root note
 * that the Library stores for the Sample, otherwise 60. The play mode is gate; from the Channel Rack it is one-shot
 * (FR-CR-11).
 */
data class AddSamplerInstrument(
    val sample: UsedSample,
    val name: String? = null,
    val playMode: PlayMode = PlayMode.GATE,
    val rootNote: Int = Limits.DEFAULT_ROOT_NOTE
) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val name = name ?: sample.name.take(Limits.NAME_LENGTH)
        checkName(name)
        if (rootNote !in 0..Limits.MIDI_MAX) refuse(Reason.RANGE, "a Root note lies in 0 to 127")
        val instrument = SamplerInstrument(
            id = state.nextId,
            name = name,
            color = nextColor(state.instruments.map { it.color }),
            sample = sample.hash,
            rootNote = rootNote,
            playMode = playMode
        )
        val samples = if (state.samples.any { it.hash == sample.hash }) state.samples else state.samples + sample
        return state.copy(nextId = state.nextId + 1, instruments = state.instruments + instrument, samples = samples)
    }
}

/** One field of a Sampler instrument. Pan, solo and fine tuning are used from version 1.0 on. */
sealed interface InstrumentValue {
    data class Name(val name: String) : InstrumentValue

    data class Color(val color: Int) : InstrumentValue

    data class RootNote(val note: Int) : InstrumentValue

    data class Mode(val mode: PlayMode) : InstrumentValue

    data class VolumeDb(val db: Float) : InstrumentValue

    data class Mute(val mute: Boolean) : InstrumentValue

    data class Pan(val pan: Float) : InstrumentValue

    data class Solo(val solo: Boolean) : InstrumentValue

    data class FineCents(val cents: Float) : InstrumentValue
}

/** The range of the fine tuning (prov., feature_specifications.md Section 1.5). */
private const val MAX_CENTS = 100f

/** Sets one field within its range. */
data class SetInstrumentValue(val instrument: Int, val value: InstrumentValue) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val old = state.instrument(instrument)
        val changed = when (value) {
            is InstrumentValue.Name -> old.copy(name = value.name.also(::checkName))
            is InstrumentValue.Color -> old.copy(color = value.color.also(::checkColor))
            is InstrumentValue.RootNote -> old.copy(rootNote = inRange(value.note, 0..Limits.MIDI_MAX))
            is InstrumentValue.Mode -> old.copy(playMode = value.mode)
            is InstrumentValue.VolumeDb -> old.copy(volumeDb = inRange(value.db, Limits.MIN_DB..Limits.MAX_DB))
            is InstrumentValue.Mute -> old.copy(mute = value.mute)
            is InstrumentValue.Pan -> old.copy(pan = inRange(value.pan, -1f..1f))
            is InstrumentValue.Solo -> old.copy(solo = value.solo)
            is InstrumentValue.FineCents -> old.copy(fineCents = inRange(value.cents, -MAX_CENTS..MAX_CENTS))
        }
        return state.copy(instruments = state.instruments.map { if (it.id == instrument) changed else it })
    }

    private fun <T : Comparable<T>> inRange(value: T, range: ClosedRange<T>): T =
        if (value in range) value else refuse(Reason.RANGE, "the value lies outside $range")
}

/** Deletes a Sampler instrument and its Parts in all Patterns. */
data class DeleteSamplerInstrument(val instrument: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        state.instrument(instrument)
        return state.copy(
            instruments = state.instruments.filter { it.id != instrument },
            patterns = state.patterns.map { p ->
                if (p.parts.none { it.instrument == instrument }) {
                    p
                } else {
                    p.copy(parts = p.parts.filter { it.instrument != instrument })
                }
            }
        )
    }
}

/** The tempo in thousandths of a BPM, from 30 to 300 BPM (FR-TR-03). Audio clips keep their frames (D-006). */
data class SetTempo(val milliBpm: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        if (milliBpm !in TimeBase.MIN_TEMPO_MILLI_BPM..TimeBase.MAX_TEMPO_MILLI_BPM) {
            refuse(Reason.RANGE, "the tempo lies in 30 to 300 BPM")
        }
        return state.copy(tempoMilliBpm = milliBpm)
    }
}

/** P1 (FR-TR-05): one of the time signatures of feature_specifications.md Section 1.1. */
data class SetTimeSignature(val signature: TimeSignature) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        if (signature !in ALLOWED) refuse(Reason.RANGE, "the time signature is one of ${ALLOWED.joinToString()}")
        return state.copy(timeSignature = signature)
    }

    companion object {
        val ALLOWED = listOf(
            TimeSignature(2, 4),
            TimeSignature(3, 4),
            TimeSignature(4, 4),
            TimeSignature(5, 4),
            TimeSignature(6, 8),
            TimeSignature(7, 8)
        )
    }
}
