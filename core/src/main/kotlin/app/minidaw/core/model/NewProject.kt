package app.minidaw.core.model

import app.minidaw.engine.api.snapshot.PlayMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The content of a new Project (feature_specifications.md Section 1.7, D-080). */
object NewProject {
    /** The default Sampler instrument of the factory set (feature_specifications.md Section 7). */
    data class DefaultInstrument(val sample: UsedSample, val name: String, val rootNote: Int)

    const val TEMPO_MILLI_BPM = 120_000
    private const val TRACK_ID = 1
    private const val CHANNEL_ID = 2
    private const val INSTRUMENT_ID = 3
    private const val PATTERN_ID = 4
    private val NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val ID = Regex("[0-9a-f]{32}")

    /**
     * A Project named by its date and time (FR-PRJ-05): 120 BPM in 4/4 (AC-TR-09), the Track "Track 1" with its
     * Mixer channel, the default Sampler instrument, and "Pattern 1" of one bar with one Part for it.
     */
    fun create(id: String, now: Instant, zone: ZoneId, instrument: DefaultInstrument): Project {
        require(ID.matches(id)) { "the ID of a Project has 32 lowercase hexadecimal characters" }
        val trackId = TRACK_ID
        val channelId = CHANNEL_ID
        val instrumentId = INSTRUMENT_ID
        val patternId = PATTERN_ID
        return Project(
            id = id,
            name = NAME.format(now.atZone(zone)),
            created = now,
            modified = now,
            tempoMilliBpm = TEMPO_MILLI_BPM,
            timeSignature = TimeSignature.FOUR_FOUR,
            nextId = patternId + 1,
            tracks = listOf(Track(id = trackId, name = "Track 1", color = 0, channel = channelId)),
            patterns = listOf(
                Pattern(
                    id = patternId,
                    name = "Pattern 1",
                    color = 0,
                    length = TimeBase.barTicks(TimeSignature.FOUR_FOUR),
                    parts = listOf(Part(instrumentId))
                )
            ),
            instruments = listOf(
                SamplerInstrument(
                    id = instrumentId,
                    name = instrument.name,
                    color = 0,
                    sample = instrument.sample.hash,
                    rootNote = instrument.rootNote,
                    playMode = PlayMode.GATE
                )
            ),
            samples = listOf(instrument.sample),
            mixer = Mixer(channels = listOf(MixerChannel(id = channelId, name = "Track 1", color = 0)))
        )
    }
}
