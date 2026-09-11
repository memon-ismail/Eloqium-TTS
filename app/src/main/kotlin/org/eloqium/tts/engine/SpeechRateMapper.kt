package org.eloqium.tts.engine

/**
 * Converts speech rate inputs into native OpenEVV speed parameters.
 * Replicates the authentic piecewise linear mapping from Eloquence TTS:
 * - Normal baseline speed in OpenEVV: 57
 * - Minimum speed in OpenEVV: 40
 * - Normal maximum speed: 150
 * - Unlocked maximum speed: 250
 */
object SpeechRateMapper {

    const val NORMAL_SPEED = 57
    const val MIN_SPEED = 40
    const val MAX_SPEED_NORMAL = 150
    const val MAX_SPEED_UNLOCKED = 250

    /**
     * Maps the -10..10 relative speech rate setting (0 = baseline speed 57)
     * to native OpenEVV speed.
     * - 0: 57 (normal Eloquence baseline)
     * - -10: 40 (minimum speed)
     * - +10: 150 (normal maximum) or 250 (unlocked maximum)
     */
    fun mapRelativeRate(relative: Int, unlocked: Boolean = false): Int {
        val r = relative.coerceIn(-10, 10)
        if (r == 0) return NORMAL_SPEED
        val maxOut = if (unlocked) MAX_SPEED_UNLOCKED else MAX_SPEED_NORMAL
        return if (r > 0) {
            NORMAL_SPEED + Math.round((maxOut - NORMAL_SPEED) * (r / 10.0)).toInt()
        } else {
            NORMAL_SPEED + Math.round((NORMAL_SPEED - MIN_SPEED) * (r / 10.0)).toInt()
        }.coerceIn(MIN_SPEED, maxOut)
    }

    /**
     * Converts legacy 0..100 stored speech rate into relative -10..10 scale.
     */
    fun mapLegacyToRelative(legacyValue: Int): Int =
        Math.round((legacyValue.coerceIn(0, 100) - 50) / 5.0).toInt().coerceIn(-10, 10)

    /**
     * Maps the 0..100 UI slider value (default 50 = normal rate)
     * to native OpenEVV speed.
     */
    fun mapSliderRate(sliderValue: Int, unlocked: Boolean = false): Int {
        val maxOut = if (unlocked) MAX_SPEED_UNLOCKED.toFloat() else MAX_SPEED_NORMAL.toFloat()
        return interpolate(
            minIn = 0f,
            maxIn = 100f,
            inVal = sliderValue.toFloat(),
            midIn = 50f,
            minOut = MIN_SPEED.toFloat(),
            maxOut = maxOut,
            midOut = NORMAL_SPEED.toFloat()
        )
    }

    /**
     * Maps Android TTS framework request speech rate (10..600, default 100 = 1.0x)
     * to native OpenEVV speed.
     */
    fun mapAndroidRate(requestRate: Int, unlocked: Boolean = false): Int {
        val maxOut = if (unlocked) MAX_SPEED_UNLOCKED.toFloat() else MAX_SPEED_NORMAL.toFloat()
        return interpolate(
            minIn = 10f,
            maxIn = 600f,
            inVal = requestRate.toFloat(),
            midIn = 100f,
            minOut = MIN_SPEED.toFloat(),
            maxOut = maxOut,
            midOut = NORMAL_SPEED.toFloat()
        )
    }

    private fun interpolate(
        minIn: Float,
        maxIn: Float,
        inVal: Float,
        midIn: Float,
        minOut: Float,
        maxOut: Float,
        midOut: Float
    ): Int {
        if (inVal == midIn) return midOut.toInt()
        val clamped = inVal.coerceIn(minIn, maxIn)
        val result = if (clamped >= midIn) {
            val ratio = (clamped - midIn) / (maxIn - midIn)
            midOut + (maxOut - midOut) * ratio
        } else {
            val ratio = (clamped - midIn) / (midIn - minIn)
            midOut + (midOut - minOut) * ratio
        }
        return Math.round(result).coerceIn(MIN_SPEED, maxOut.toInt())
    }
}
