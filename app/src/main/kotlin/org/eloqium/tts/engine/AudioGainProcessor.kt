package org.eloqium.tts.engine

/**
 * Deterministic PCM audio gain scaling for 16-bit linear PCM little-endian audio.
 */
object AudioGainProcessor {

    /**
     * Applies relative volume scaling (-10..10) to 16-bit linear PCM little-endian audio.
     * - 0: Baseline volume (100%, unity gain, no alteration)
     * - Negative (-10..-1): Smooth attenuation down to 0.20x (-10)
     * - Positive (1..10): Smooth gain boost up to 2.0x (+10) with 16-bit clamping
     */
    fun applyRelativeGain(buffer: ByteArray, length: Int, relativeVolume: Int) {
        val rel = relativeVolume.coerceIn(-10, 10)
        if (rel == 0) return // Base volume, no processing needed

        val factor = if (rel > 0) {
            1.0f + (rel * 0.10f) // 1.1x .. 2.0x
        } else {
            1.0f + (rel * 0.08f) // 0.92x .. 0.20x
        }

        val numSamples = (length.coerceAtMost(buffer.size)) / 2
        for (i in 0 until numSamples) {
            val byteIdx = i * 2
            val low = buffer[byteIdx].toInt() and 0xFF
            val high = buffer[byteIdx + 1].toInt() // sign-extended
            val sample = (high shl 8) or low

            val scaled = (sample * factor).toInt().coerceIn(-32768, 32767)

            buffer[byteIdx] = (scaled and 0xFF).toByte()
            buffer[byteIdx + 1] = ((scaled shr 8) and 0xFF).toByte()
        }
    }

    /**
     * Converts legacy 0..100 stored volume into relative -10..10 scale.
     */
    fun mapLegacyToRelative(legacyVolume: Int): Int =
        Math.round((legacyVolume.coerceIn(0, 100) - 100) / 10.0).toInt().coerceIn(-10, 0)

    /**
     * Applies volume scaling in place to a 16-bit PCM byte array.
     * @param buffer 16-bit linear PCM little-endian byte buffer.
     * @param length Number of valid bytes in buffer.
     * @param volumePercent Volume level from 0 to 100.
     */
    fun applyGain(buffer: ByteArray, length: Int, volumePercent: Int) {
        val clampedVolume = volumePercent.coerceIn(0, 100)
        if (clampedVolume == 100) return // Full volume, no attenuation

        if (clampedVolume == 0) {
            // Mute
            buffer.fill(0, 0, length.coerceAtMost(buffer.size))
            return
        }

        val factor = clampedVolume / 100.0f
        val numSamples = (length.coerceAtMost(buffer.size)) / 2

        for (i in 0 until numSamples) {
            val byteIdx = i * 2
            val low = buffer[byteIdx].toInt() and 0xFF
            val high = buffer[byteIdx + 1].toInt() // sign-extended
            val sample = (high shl 8) or low

            val scaled = (sample * factor).toInt().coerceIn(-32768, 32767)

            buffer[byteIdx] = (scaled and 0xFF).toByte()
            buffer[byteIdx + 1] = ((scaled shr 8) and 0xFF).toByte()
        }
    }
}
