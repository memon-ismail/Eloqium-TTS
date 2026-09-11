package org.eloqium.tts.engine

/**
 * OpenEVV / Eloquence Command Interface (ECI) constants and definitions.
 */
object Eci {
    const val PARAM_SYNTH_MODE = 0
    const val PARAM_INPUT_TYPE = 1
    const val PARAM_TEXT_MODE = 2
    const val PARAM_DICTIONARY = 3
    const val PARAM_SAMPLE_RATE = 5
    const val PARAM_REAL_WORLD_UNITS = 8
    const val PARAM_LANGUAGE_DIALECT = 9
    const val PARAM_NUMBER_MODE = 10
    const val PARAM_PHRASE_PREDICTION = 12

    const val VOICE_GENDER = 0
    const val VOICE_HEAD_SIZE = 1
    const val VOICE_PITCH_BASELINE = 2
    const val VOICE_PITCH_FLUCTUATION = 3
    const val VOICE_ROUGHNESS = 4
    const val VOICE_BREATHINESS = 5
    const val VOICE_SPEED = 6
    const val VOICE_VOLUME = 7

    const val VOICE_CURRENT = 0
    const val FIRST_PRESET = 1
    const val LAST_PRESET = 8
    const val SCRATCH_VOICE = 9

    const val NUM_VOICE_PARAMS = 8

    const val DICT_MAIN = 0
    const val DICT_ROOT = 1
    const val DICT_ABBREVIATION = 2

    const val DEFAULT_SPEED = 50
    const val SPEED_MAX = 250
    const val PERCENT_MAX = 100

    val SAMPLE_RATES = intArrayOf(8000, 11025, 22050, 16000, 32000, 44100, 48000)

    fun sampleRateHz(index: Int): Int =
        SAMPLE_RATES.getOrElse(index) { SAMPLE_RATES[1] }

    fun sampleRateIndex(hz: Int): Int {
        val at = SAMPLE_RATES.indexOf(hz)
        return if (at >= 0) at else 1
    }

    val PRESET_NAMES = arrayOf(
        "Reed", "Shelley", "Bobby", "Rocko", "Glen", "Sandy", "Grandma", "Grandpa"
    )

    fun voiceRange(param: Int): IntRange = when (param) {
        VOICE_GENDER -> 0..1
        VOICE_SPEED -> 0..SPEED_MAX
        else -> 0..PERCENT_MAX
    }

    fun clampVoice(param: Int, value: Int): Int {
        val range = voiceRange(param)
        return value.coerceIn(range.first, range.last)
    }
}
