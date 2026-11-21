package app.minidaw.engine.api.snapshot

import java.nio.ByteBuffer

// Little-endian fields of feature_specifications.md Section 6.2, with their ranges.

private const val U8_MAX = 0xFF
private const val U16_MAX = 0xFFFF

internal fun ByteBuffer.putU8(value: Int) {
    require(value in 0..U8_MAX) { "a u8 field holds 0 to $U8_MAX, not $value" }
    put(value.toByte())
}

internal fun ByteBuffer.putU16(value: Int) {
    require(value in 0..U16_MAX) { "a u16 field holds 0 to $U16_MAX, not $value" }
    putShort(value.toShort())
}

internal fun ByteBuffer.putU32(value: Int) {
    require(value >= 0) { "a u32 field holds no negative number, not $value" }
    putInt(value)
}

/** An i32 that is a reference or −1 for "none". */
internal fun ByteBuffer.putReference(value: Int) {
    require(value >= SnapshotFormat.NONE) { "a reference is −1 or a number of a record, not $value" }
    putInt(value)
}

internal fun ByteBuffer.putFlag(value: Boolean) {
    put(if (value) 1.toByte() else 0.toByte())
}

internal fun ByteBuffer.putReserved(bytes: Int) {
    repeat(bytes) { put(0.toByte()) }
}
