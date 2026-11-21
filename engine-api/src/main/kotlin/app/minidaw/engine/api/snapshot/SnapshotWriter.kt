package app.minidaw.engine.api.snapshot

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The reserved bytes after a u8 field that start a group of four bytes. */
private const val PADDING = 3

/**
 * Writes the bytes of a Snapshot (feature_specifications.md Section 6.2, architecture.md Section 5.2): the header,
 * the directory of twelve pairs of an offset and a count, and the twelve tables. All numbers are little-endian.
 * The writer checks the range of each field. The engine checks references and order before a swap.
 */
object SnapshotWriter {
    fun write(content: SnapshotContent): ByteArray {
        require(content.tempoMilliBpm > 0) { "the tempo must be above 0, not ${content.tempoMilliBpm}" }
        val counts = content.counts()
        val offsets = IntArray(SnapshotFormat.TABLES)
        var end = SnapshotFormat.HEADER_SIZE + SnapshotFormat.DIRECTORY_SIZE
        for (table in 0 until SnapshotFormat.TABLES) {
            offsets[table] = end
            end += counts[table] * SnapshotFormat.RECORD_SIZES[table]
        }
        val out = ByteBuffer.allocate(end).order(ByteOrder.LITTLE_ENDIAN)
        out.putInt(SnapshotFormat.MARK)
        out.putU16(SnapshotFormat.VERSION)
        out.putReserved(2)
        out.putU32(end)
        out.putU32(content.tempoMilliBpm)
        out.putU8(content.numerator)
        out.putU8(content.denominator)
        out.putReserved(2)
        for (table in 0 until SnapshotFormat.TABLES) {
            out.putU32(offsets[table])
            out.putU32(counts[table])
        }
        out.putTables(content)
        check(out.position() == end) { "the tables end at ${out.position()}, not at $end" }
        return out.array()
    }
}

/** The twelve tables in the order of the directory. */
private fun ByteBuffer.putTables(content: SnapshotContent) {
    content.samples.forEach { put(it.toBytes()) }
    content.instruments.forEach { putInstrument(it) }
    content.envelopes.forEach { putEnvelope(it) }
    content.patterns.forEach { putPattern(it) }
    content.parts.forEach { putPart(it) }
    content.notes.forEach { putNote(it) }
    content.tracks.forEach { putTrack(it) }
    content.clips.forEach { putClip(it) }
    content.channels.forEach { putChannel(it) }
    content.slots.forEach { putSlot(it) }
    content.parameters.forEach { putFloat(it) }
    content.order.forEach { putU32(it) }
}

private fun ByteBuffer.putInstrument(r: InstrumentRecord) {
    putU32(r.id)
    putU32(r.sample)
    putU8(r.rootNote)
    putU8(r.playMode.code)
    putFlag(r.audible)
    putReserved(1)
    putFloat(r.gain)
    putFloat(r.pan)
    putFloat(r.fineTuningCents)
    putReference(r.ownChannel)
    putReference(r.envelope)
    putU32(r.startFrame)
    putU32(r.endFrame)
    putU32(r.loopStart)
    putU32(r.loopEnd)
}

private fun ByteBuffer.putEnvelope(r: EnvelopeRecord) {
    putFloat(r.attackMs)
    putFloat(r.decayMs)
    putFloat(r.sustain)
    putFloat(r.releaseMs)
}

private fun ByteBuffer.putPattern(r: PatternRecord) {
    putU32(r.id)
    putU32(r.lengthTicks)
    putU32(r.firstPart)
    putU32(r.partCount)
}

private fun ByteBuffer.putPart(r: PartRecord) {
    putU32(r.instrument)
    putU32(r.firstNote)
    putU32(r.noteCount)
}

private fun ByteBuffer.putNote(r: NoteRecord) {
    putU32(r.start)
    putU32(r.length)
    putU8(r.pitch)
    putU8(r.velocity)
    putReserved(2)
}

private fun ByteBuffer.putTrack(r: TrackRecord) {
    putU32(r.id)
    putU32(r.channel)
    putU32(r.firstClip)
    putU32(r.clipCount)
    putFlag(r.audible)
    putReserved(PADDING)
}

private fun ByteBuffer.putClip(r: ClipRecord) {
    putU32(r.id)
    putU8(r.kind.code)
    putReserved(PADDING)
    putU32(r.start)
    putU32(r.source)
    putU32(r.offset)
    putU32(r.length)
    putFloat(r.gain)
}

private fun ByteBuffer.putChannel(r: ChannelRecord) {
    putU32(r.id)
    putReference(r.target)
    putFloat(r.gain)
    putFloat(r.pan)
    putFlag(r.audible)
    putReserved(PADDING)
    putU32(r.firstSlot)
}

private fun ByteBuffer.putSlot(r: SlotRecord) {
    putU32(r.id)
    putU16(r.kind)
    putFlag(r.bypass)
    putReserved(1)
    putU32(r.firstParameter)
    putU32(r.parameterCount)
}
