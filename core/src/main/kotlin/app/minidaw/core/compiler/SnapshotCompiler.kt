package app.minidaw.core.compiler

import app.minidaw.core.model.Audible
import app.minidaw.core.model.AudioClip
import app.minidaw.core.model.Effect
import app.minidaw.core.model.EffectCatalog
import app.minidaw.core.model.Limits
import app.minidaw.core.model.MixerChannel
import app.minidaw.core.model.PatternClip
import app.minidaw.core.model.Project
import app.minidaw.engine.api.snapshot.ChannelRecord
import app.minidaw.engine.api.snapshot.ClipKind
import app.minidaw.engine.api.snapshot.ClipRecord
import app.minidaw.engine.api.snapshot.EnvelopeRecord
import app.minidaw.engine.api.snapshot.InstrumentRecord
import app.minidaw.engine.api.snapshot.NoteRecord
import app.minidaw.engine.api.snapshot.PartRecord
import app.minidaw.engine.api.snapshot.PatternRecord
import app.minidaw.engine.api.snapshot.SampleHash
import app.minidaw.engine.api.snapshot.SlotRecord
import app.minidaw.engine.api.snapshot.SnapshotContent
import app.minidaw.engine.api.snapshot.SnapshotFormat
import app.minidaw.engine.api.snapshot.SnapshotWriter
import app.minidaw.engine.api.snapshot.TrackRecord
import kotlin.math.pow

/**
 * The compiler of feature_specifications.md Section 2.2.3: a state of a Project becomes the twelve tables of a
 * Snapshot (Section 6.2). References become numbers of records, dB become plain gains, Notes and Clips are
 * sorted, the flags "audible" follow Section 3.2.3, and the channels get their order of processing (3.2.5).
 * The Master channel is the last channel, with the ID 0.
 */
object SnapshotCompiler {
    private const val DB_PER_DECADE = 20.0

    /** `gain = 10 ^ (dB / 20)`, and exactly 0 at −90 dB (feature_specifications.md Section 1.4). */
    fun gain(db: Float): Float = if (db <= Limits.MIN_DB) 0f else 10.0.pow(db / DB_PER_DECADE).toFloat()

    fun bytes(project: Project): ByteArray = SnapshotWriter.write(compile(project))

    fun compile(project: Project): SnapshotContent {
        val samples = project.samples.map { it.hash }
        val sampleIndex = samples.withIndex().associate { (i, h) -> h to i }
        val channelIndex = project.mixer.channels.withIndex().associate { (i, c) -> c.id to i }
        val instrumentIndex = project.instruments.withIndex().associate { (i, s) -> s.id to i }
        val patternIndex = project.patterns.withIndex().associate { (i, p) -> p.id to i }
        val envelopes = project.instruments.mapNotNull { it.envelope }
            .map { EnvelopeRecord(it.attackMs, it.decayMs, it.sustain, it.releaseMs) }
        val audibleInstruments = Audible.instruments(project.instruments)
        var envelope = 0
        val instruments = project.instruments.map { i ->
            val frames = project.samples.first { it.hash == i.sample }.frames
            InstrumentRecord(
                id = i.id,
                sample = sampleIndex.getValue(i.sample),
                rootNote = i.rootNote,
                playMode = i.playMode,
                audible = audibleInstruments.getValue(i.id),
                gain = gain(i.volumeDb),
                pan = i.pan,
                fineTuningCents = i.fineCents,
                ownChannel = i.channel?.let { channelIndex.getValue(it) } ?: SnapshotFormat.NONE,
                envelope = if (i.envelope == null) SnapshotFormat.NONE else envelope++,
                startFrame = i.startFrame ?: 0,
                endFrame = i.endFrame ?: frames,
                loopStart = i.loop?.startFrame ?: 0,
                loopEnd = i.loop?.endFrame ?: 0
            )
        }
        val sequences = sequences(project, instrumentIndex)
        val (trackRecords, clipRecords) = tracks(project, channelIndex, sampleIndex, patternIndex)
        val routing = mixer(project, channelIndex)
        return SnapshotContent(
            tempoMilliBpm = project.tempoMilliBpm,
            numerator = project.timeSignature.numerator,
            denominator = project.timeSignature.denominator,
            samples = samples,
            instruments = instruments,
            envelopes = envelopes,
            patterns = sequences.patterns,
            parts = sequences.parts,
            notes = sequences.notes,
            tracks = trackRecords,
            clips = clipRecords,
            channels = routing.channels,
            slots = routing.slots,
            parameters = routing.parameters,
            order = order(project.mixer.channels)
        )
    }

    /** Every channel comes before its target; channels of the same level keep the order of the Mixer. */
    fun order(channels: List<MixerChannel>): List<Int> {
        val targets = channels.associate { it.id to it.target }
        fun depth(id: Int): Int = generateSequence(targets[id]) { targets[it] }.take(channels.size).count()
        return channels.indices.sortedWith(compareByDescending<Int> { depth(channels[it].id) }.thenBy { it })
    }
}

private class Sequences(val patterns: List<PatternRecord>, val parts: List<PartRecord>, val notes: List<NoteRecord>)

private fun sequences(project: Project, instrumentIndex: Map<Int, Int>): Sequences {
    val patterns = mutableListOf<PatternRecord>()
    val parts = mutableListOf<PartRecord>()
    val notes = mutableListOf<NoteRecord>()
    for (pattern in project.patterns) {
        patterns += PatternRecord(pattern.id, pattern.length, firstPart = parts.size, partCount = pattern.parts.size)
        for (part in pattern.parts) {
            val sorted = part.notes.sortedWith(compareBy({ it.start }, { it.pitch }))
            parts += PartRecord(instrumentIndex.getValue(part.instrument), notes.size, sorted.size)
            sorted.mapTo(notes) { NoteRecord(it.start, it.length, it.pitch, it.velocity) }
        }
    }
    return Sequences(patterns, parts, notes)
}

private fun tracks(
    project: Project,
    channelIndex: Map<Int, Int>,
    sampleIndex: Map<SampleHash, Int>,
    patternIndex: Map<Int, Int>
): Pair<List<TrackRecord>, List<ClipRecord>> {
    val audible = Audible.tracks(project.tracks)
    val tracks = mutableListOf<TrackRecord>()
    val clips = mutableListOf<ClipRecord>()
    for (track in project.tracks) {
        val sorted = track.clips.sortedBy { it.start }
        val channel = channelIndex.getValue(track.channel)
        tracks += TrackRecord(track.id, channel, clips.size, sorted.size, audible.getValue(track.id))
        sorted.mapTo(clips) { c ->
            when (c) {
                is AudioClip -> ClipRecord(
                    c.id, ClipKind.AUDIO, c.start, sampleIndex.getValue(c.sample), c.offset, c.length,
                    SnapshotCompiler.gain(c.gainDb)
                )

                is PatternClip -> ClipRecord(
                    c.id, ClipKind.PATTERN, c.start, patternIndex.getValue(c.pattern), c.offset, c.length,
                    SnapshotCompiler.gain(c.gainDb)
                )
            }
        }
    }
    return tracks to clips
}

private class Routing(val channels: List<ChannelRecord>, val slots: List<SlotRecord>, val parameters: List<Float>)

private fun mixer(project: Project, channelIndex: Map<Int, Int>): Routing {
    val audible = Audible.channels(project.mixer)
    val slots = mutableListOf<SlotRecord>()
    val parameters = mutableListOf<Float>()
    fun slotsOf(effects: List<Effect?>): Int {
        val first = slots.size
        for (effect in effects) {
            val spec = effect?.let { EffectCatalog.spec(it.kind) }
            if (effect == null || spec == null) {
                slots += SlotRecord.EMPTY
            } else {
                slots += SlotRecord(effect.id, spec.code, effect.bypass, parameters.size, spec.parameters.size)
                spec.parameters.mapTo(parameters) { effect.params[it.name] ?: it.default }
            }
        }
        return first
    }
    val channels = project.mixer.channels.map { c ->
        ChannelRecord(
            id = c.id,
            target = c.target?.let { channelIndex.getValue(it) } ?: SnapshotFormat.NONE,
            gain = SnapshotCompiler.gain(c.volumeDb),
            pan = c.pan,
            audible = audible.getValue(c.id),
            firstSlot = slotsOf(c.slots)
        )
    }
    val master = ChannelRecord(
        id = SnapshotFormat.EMPTY_ID,
        target = SnapshotFormat.NONE,
        gain = SnapshotCompiler.gain(project.mixer.master.volumeDb),
        pan = 0f,
        audible = true,
        firstSlot = slotsOf(project.mixer.master.slots)
    )
    return Routing(channels + master, slots, parameters)
}
