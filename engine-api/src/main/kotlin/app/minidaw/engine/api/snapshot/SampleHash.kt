package app.minidaw.engine.api.snapshot

/**
 * The hash of a Sample: SHA-256, written as 64 lowercase hexadecimal characters. The document of a Project
 * names a Sample by this text (feature_specifications.md Section 6.1); a Snapshot holds its 32 bytes.
 */
@JvmInline
value class SampleHash(val hex: String) {
    init {
        require(hex.length == HEX_LENGTH && hex.all { it in '0'..'9' || it in 'a'..'f' }) {
            "a hash has $HEX_LENGTH lowercase hexadecimal characters: $hex"
        }
    }

    /** The 32 bytes of the hash. */
    fun toBytes(): ByteArray = ByteArray(BYTES) { i ->
        (DIGITS.indexOf(hex[2 * i]) shl NIBBLE_BITS or DIGITS.indexOf(hex[2 * i + 1])).toByte()
    }

    companion object {
        const val BYTES = 32
        private const val HEX_LENGTH = 2 * BYTES
        private const val DIGITS = "0123456789abcdef"
        private const val NIBBLE_BITS = 4
        private const val NIBBLE_MASK = 0x0F
        private const val BYTE_MASK = 0xFF

        /** The hash of 32 bytes. */
        fun of(bytes: ByteArray): SampleHash {
            require(bytes.size == BYTES) { "a hash has $BYTES bytes, not ${bytes.size}" }
            return SampleHash(
                buildString {
                    for (byte in bytes) {
                        val value = byte.toInt() and BYTE_MASK
                        append(DIGITS[value ushr NIBBLE_BITS])
                        append(DIGITS[value and NIBBLE_MASK])
                    }
                }
            )
        }
    }
}
