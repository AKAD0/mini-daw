package app.minidaw.engine.api

import kotlin.test.Test
import kotlin.test.assertEquals

class SnapshotPrototypeTest {
    @Test
    fun bothWritersGiveTheSameBytes() {
        // The same numbers stand in engine/src/test/cpp/snapshot_test.cpp.
        assertEquals(
            0xD87D14E4F99FE61CuL,
            SnapshotPrototype.fingerprint(SnapshotPrototype.write(SnapshotPrototype.mvp))
        )
        assertEquals(
            0xC27A518E15BABED3uL,
            SnapshotPrototype.fingerprint(SnapshotPrototype.write(SnapshotPrototype.version10))
        )
    }

    @Test
    fun theSnapshotsHaveTheirSizes() {
        assertEquals(42676, SnapshotPrototype.write(SnapshotPrototype.mvp).size)
        assertEquals(85236, SnapshotPrototype.write(SnapshotPrototype.version10).size)
    }

    @Test
    fun theFingerprintIsFnv1a() {
        assertEquals(0xCBF29CE484222325uL, SnapshotPrototype.fingerprint(ByteArray(0)))
        assertEquals(0xAF63DC4C8601EC8CuL, SnapshotPrototype.fingerprint("a".toByteArray()))
    }
}
