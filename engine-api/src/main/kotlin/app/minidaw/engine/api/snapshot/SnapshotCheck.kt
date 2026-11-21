package app.minidaw.engine.api.snapshot

import app.minidaw.engine.api.EngineResult
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Checks 1 and 2 of the engine before a swap (feature_specifications.md Section 6.2): mark, version and size are
 * right, and every table lies inside the block. The engine also checks references, order and limits; the fake
 * engine uses this part only.
 */
object SnapshotCheck {
    private const val U16_MASK = 0xFFFF
    private const val U32_MASK = 0xFFFFFFFFL
    private const val VERSION_AT = 4
    private const val SIZE_AT = 8
    private const val PAIR = 8

    fun check(bytes: ByteArray): EngineResult {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return when {
            bytes.size < SnapshotFormat.HEADER_SIZE + SnapshotFormat.DIRECTORY_SIZE -> EngineResult.TOO_SHORT
            b.getInt(0) != SnapshotFormat.MARK -> EngineResult.BAD_MARK
            b.getShort(VERSION_AT).toInt() and U16_MASK != SnapshotFormat.VERSION -> EngineResult.BAD_VERSION
            b.getInt(SIZE_AT).toLong() and U32_MASK != bytes.size.toLong() -> EngineResult.BAD_SIZE
            !tablesInside(b, bytes.size.toLong()) -> EngineResult.TABLE_OUTSIDE
            else -> EngineResult.OK
        }
    }

    private fun tablesInside(b: ByteBuffer, size: Long): Boolean = (0 until SnapshotFormat.TABLES).all { table ->
        val at = SnapshotFormat.HEADER_SIZE + PAIR * table
        val offset = b.getInt(at).toLong() and U32_MASK
        val count = b.getInt(at + Int.SIZE_BYTES).toLong() and U32_MASK
        offset >= SnapshotFormat.HEADER_SIZE + SnapshotFormat.DIRECTORY_SIZE &&
            offset + count * SnapshotFormat.RECORD_SIZES[table] <= size
    }
}
