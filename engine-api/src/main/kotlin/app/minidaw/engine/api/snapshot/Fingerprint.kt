package app.minidaw.engine.api.snapshot

/** FNV-1a with 64 bits: a short fingerprint of bytes for tests and fixtures. */
object Fingerprint {
    private const val OFFSET_BASIS = 14695981039346656037uL
    private const val PRIME = 1099511628211uL

    fun of(bytes: ByteArray): ULong {
        var hash = OFFSET_BASIS
        for (byte in bytes) {
            hash = (hash xor byte.toUByte().toULong()) * PRIME
        }
        return hash
    }
}
