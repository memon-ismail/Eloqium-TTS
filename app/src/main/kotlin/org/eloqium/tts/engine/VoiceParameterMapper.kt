package org.eloqium.tts.engine

/**
 * Maps relative user adjustments (-10..10) to native OpenEVV voice parameters
 * around authentic Eloquence / OpenEVV baselines.
 *
 * Relative scale conventions:
 * - 0: Genuine engine baseline (neutral / factory default)
 * - Negative (-10..-1): Progressively lower / softer than baseline
 * - Positive (1..10): Progressively higher / stronger than baseline
 */
object VoiceParameterMapper {

    const val RELATIVE_MIN = -10
    const val RELATIVE_MAX = 10
    const val RELATIVE_DEFAULT = 0

    // Authentic Eloquence / OpenEVV baselines for Preset 0 (Reed / default voice)
    const val DEFAULT_PITCH_BASELINE = 65
    const val DEFAULT_INFLECTION_BASELINE = 30
    const val DEFAULT_HEAD_SIZE_BASELINE = 50
    const val DEFAULT_ROUGHNESS_BASELINE = 0
    const val DEFAULT_BREATHINESS_BASELINE = 0

    /**
     * Maps relative pitch adjustment (-10..10) around the voice's [basePitch].
     * 0 returns [basePitch] (e.g. 65 for Reed).
     * -10 returns the lower pitch bound (25).
     * 10 returns the upper pitch bound (100).
     */
    fun mapPitch(relative: Int, basePitch: Int = DEFAULT_PITCH_BASELINE): Int {
        val r = relative.coerceIn(RELATIVE_MIN, RELATIVE_MAX)
        if (r == 0) return basePitch
        return if (r > 0) {
            val max = 100
            basePitch + Math.round((max - basePitch) * (r / 10.0)).toInt()
        } else {
            val min = 25
            basePitch + Math.round((basePitch - min) * (r / 10.0)).toInt()
        }.coerceIn(0, 100)
    }

    /**
     * Maps relative inflection adjustment (-10..10) around [baseInflection] (default 30).
     * 0 returns [baseInflection] (30).
     * -10 returns 0.
     * 10 returns 100.
     */
    fun mapInflection(relative: Int, baseInflection: Int = DEFAULT_INFLECTION_BASELINE): Int {
        val r = relative.coerceIn(RELATIVE_MIN, RELATIVE_MAX)
        if (r == 0) return baseInflection
        return if (r > 0) {
            val max = 100
            baseInflection + Math.round((max - baseInflection) * (r / 10.0)).toInt()
        } else {
            val min = 0
            baseInflection + Math.round((baseInflection - min) * (r / 10.0)).toInt()
        }.coerceIn(0, 100)
    }

    /**
     * Maps relative head size adjustment (-10..10) around [baseHeadSize] (default 50).
     * 0 returns [baseHeadSize] (50).
     * -10 returns 0.
     * 10 returns 100.
     */
    fun mapHeadSize(relative: Int, baseHeadSize: Int = DEFAULT_HEAD_SIZE_BASELINE): Int {
        val r = relative.coerceIn(RELATIVE_MIN, RELATIVE_MAX)
        if (r == 0) return baseHeadSize
        return if (r > 0) {
            val max = 100
            baseHeadSize + Math.round((max - baseHeadSize) * (r / 10.0)).toInt()
        } else {
            val min = 0
            baseHeadSize + Math.round((baseHeadSize - min) * (r / 10.0)).toInt()
        }.coerceIn(0, 100)
    }

    /**
     * Maps relative roughness adjustment (-10..10) around [baseRoughness] (default 0).
     * 0 returns [baseRoughness] (0).
     * Negative values clamp gracefully at 0.
     * 10 returns 100.
     */
    fun mapRoughness(relative: Int, baseRoughness: Int = DEFAULT_ROUGHNESS_BASELINE): Int {
        val r = relative.coerceIn(RELATIVE_MIN, RELATIVE_MAX)
        if (r <= 0) return baseRoughness.coerceAtLeast(0)
        val max = 100
        return (baseRoughness + Math.round((max - baseRoughness) * (r / 10.0)).toInt()).coerceIn(0, 100)
    }

    /**
     * Maps relative breathiness adjustment (-10..10) around [baseBreathiness] (default 0).
     * 0 returns [baseBreathiness] (0).
     * Negative values clamp gracefully at 0.
     * 10 returns 100.
     */
    fun mapBreathiness(relative: Int, baseBreathiness: Int = DEFAULT_BREATHINESS_BASELINE): Int {
        val r = relative.coerceIn(RELATIVE_MIN, RELATIVE_MAX)
        if (r <= 0) return baseBreathiness.coerceAtLeast(0)
        val max = 100
        return (baseBreathiness + Math.round((max - baseBreathiness) * (r / 10.0)).toInt()).coerceIn(0, 100)
    }
}
