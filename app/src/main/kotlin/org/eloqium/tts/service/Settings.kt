package org.eloqium.tts.service

import android.content.Context
import android.content.SharedPreferences
import org.eloqium.tts.engine.SpeechRateMapper
import org.eloqium.tts.engine.AudioGainProcessor

/**
 * Canonical defaults and keys for Eloqium TTS settings.
 */
object SettingsDefaults {
    const val PREFS_NAME = "eloqium_settings"

    // Keys
    const val KEY_VOICE_PROFILE = "voice_profile"
    const val KEY_DEFAULT_PRESET = "default_preset"
    const val KEY_FORCE_SPEECH_RATE = "force_speech_rate"
    const val KEY_SPEECH_RATE = "speech_rate"
    const val KEY_UNLOCK_MAX_RATE = "unlock_max_rate"
    const val KEY_FORCE_PITCH = "force_pitch"
    const val KEY_PITCH = "pitch"
    const val KEY_FORCE_VOLUME = "force_volume"
    const val KEY_VOLUME = "volume"
    const val KEY_INFLECTION = "inflection"
    const val KEY_HEAD_SIZE = "head_size"
    const val KEY_ROUGHNESS = "roughness"
    const val KEY_BREATHINESS = "breathiness"
    const val KEY_EMOJI_EMOTICON = "emoji_emoticon"
    const val KEY_PROCESS_PUNCTUATION = "process_punctuation"
    const val KEY_PUNCTUATION_LEVEL = "punctuation_level"
    const val KEY_USE_NUMBER_PROCESSING = "use_number_processing"
    const val KEY_NUMBER_PROCESSING_MODE = "number_processing_mode"
    const val KEY_USE_ABBREVIATIONS = "use_abbreviations"
    const val KEY_INTONATION_PAUSES = "intonation_pauses"
    const val KEY_FORCE_LANGUAGE = "force_language"
    const val KEY_LANGUAGE = "language"
    const val KEY_SAMPLING_RATE = "sampling_rate"
    const val KEY_PHRASE_PREDICTION = "phrase_prediction"
    const val KEY_RESPECT_APP_VOICE = "respect_app_voice"

    // Defaults
    const val DEFAULT_VOICE_PROFILE = 0 // Reed
    const val DEFAULT_FORCE_SPEECH_RATE = false
    const val DEFAULT_SPEECH_RATE = 0 // Relative scale: -10..10 (0 = normal Eloquence speed 57)
    const val DEFAULT_UNLOCK_MAX_RATE = false
    const val DEFAULT_FORCE_PITCH = false
    const val DEFAULT_PITCH = 0 // Relative scale: -10..10 (0 = normal baseline pitch)
    const val DEFAULT_FORCE_VOLUME = false
    const val DEFAULT_VOLUME = 0 // Relative scale: -10..10 (0 = normal 100% volume)
    const val DEFAULT_INFLECTION = 0 // Relative: -10..10 (0 maps to native baseline 30)
    const val DEFAULT_HEAD_SIZE = 0 // Relative: -10..10 (0 maps to native baseline 50)
    const val DEFAULT_ROUGHNESS = 0 // Relative: -10..10 (0 maps to native baseline 0)
    const val DEFAULT_BREATHINESS = 0 // Relative: -10..10 (0 maps to native baseline 0)
    const val DEFAULT_EMOJI_EMOTICON = true
    const val DEFAULT_PROCESS_PUNCTUATION = false
    const val DEFAULT_PUNCTUATION_LEVEL = 0 // 0=None, 1=Some, 2=Most, 3=All
    const val DEFAULT_USE_NUMBER_PROCESSING = false
    const val DEFAULT_NUMBER_PROCESSING_MODE = 0 // 0=Digits, 1=Pairs, 2=Triplets
    const val DEFAULT_USE_ABBREVIATIONS = true
    const val DEFAULT_INTONATION_PAUSES = true
    const val DEFAULT_FORCE_LANGUAGE = false
    const val DEFAULT_LANGUAGE = "en-US"
    const val DEFAULT_SAMPLING_RATE = 11025
    const val DEFAULT_PHRASE_PREDICTION = true
    const val DEFAULT_RESPECT_APP_VOICE = true

    // Punctuation levels
    const val PUNCT_NONE = 0
    const val PUNCT_SOME = 1
    const val PUNCT_MOST = 2
    const val PUNCT_ALL = 3

    // Number modes
    const val NUMBER_DIGITS = 0
    const val NUMBER_PAIRS = 1
    const val NUMBER_TRIPLETS = 2

    // Genuine sampling rates supported by OpenEVV
    val SUPPORTED_SAMPLING_RATES = intArrayOf(8000, 11025, 16000, 22050, 32000, 44100, 48000)

    // Supported languages
    val SUPPORTED_LANGUAGES = arrayOf(
        "en-US", "en-GB", "es-ES", "es-US", "fr-FR", "fr-CA", "de-DE", "it-IT"
    )
}

/**
 * Centralized settings model for Eloqium TTS.
 * Single source of truth backed by persistent SharedPreferences.
 */
class Settings(val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences(SettingsDefaults.PREFS_NAME, Context.MODE_PRIVATE)
    )

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    // Voice & Voice Profile
    var voiceProfile: Int
        get() = prefs.getInt(SettingsDefaults.KEY_VOICE_PROFILE, SettingsDefaults.DEFAULT_VOICE_PROFILE).coerceIn(0, 7)
        set(value) = prefs.edit().putInt(SettingsDefaults.KEY_VOICE_PROFILE, value.coerceIn(0, 7)).apply()

    // Backward-compat alias for defaultPreset
    var defaultPreset: Int
        get() = voiceProfile
        set(value) { voiceProfile = value }

    // Force speech rate, relative rate (-10..10) & unlock max rate
    var forceSpeechRate: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_FORCE_SPEECH_RATE, SettingsDefaults.DEFAULT_FORCE_SPEECH_RATE)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_FORCE_SPEECH_RATE, value).apply()

    var speechRate: Int
        get() {
            val raw = prefs.getInt(SettingsDefaults.KEY_SPEECH_RATE, SettingsDefaults.DEFAULT_SPEECH_RATE)
            return if (raw in -10..10) raw else SpeechRateMapper.mapLegacyToRelative(raw)
        }
        set(value) = prefs.edit().putInt(SettingsDefaults.KEY_SPEECH_RATE, value.coerceIn(-10, 10)).apply()

    var unlockMaxRate: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_UNLOCK_MAX_RATE, SettingsDefaults.DEFAULT_UNLOCK_MAX_RATE)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_UNLOCK_MAX_RATE, value).apply()

    // Force pitch & relative pitch (-10..10)
    var forcePitch: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_FORCE_PITCH, SettingsDefaults.DEFAULT_FORCE_PITCH)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_FORCE_PITCH, value).apply()

    var pitch: Int
        get() = prefs.getInt(SettingsDefaults.KEY_PITCH, SettingsDefaults.DEFAULT_PITCH).coerceIn(-10, 10)
        set(value) = prefs.edit().putInt(SettingsDefaults.KEY_PITCH, value.coerceIn(-10, 10)).apply()

    // Force volume & relative volume (-10..10)
    var forceVolume: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_FORCE_VOLUME, SettingsDefaults.DEFAULT_FORCE_VOLUME)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_FORCE_VOLUME, value).apply()

    var volume: Int
        get() {
            val raw = prefs.getInt(SettingsDefaults.KEY_VOLUME, SettingsDefaults.DEFAULT_VOLUME)
            return if (raw in -10..10) raw else AudioGainProcessor.mapLegacyToRelative(raw)
        }
        set(value) = prefs.edit().putInt(SettingsDefaults.KEY_VOLUME, value.coerceIn(-10, 10)).apply()

    // Voice characteristics (relative -10..10)
    var inflection: Int
        get() = prefs.getInt(SettingsDefaults.KEY_INFLECTION, SettingsDefaults.DEFAULT_INFLECTION).coerceIn(-10, 10)
        set(value) = prefs.edit().putInt(SettingsDefaults.KEY_INFLECTION, value.coerceIn(-10, 10)).apply()

    var headSize: Int
        get() = prefs.getInt(SettingsDefaults.KEY_HEAD_SIZE, SettingsDefaults.DEFAULT_HEAD_SIZE).coerceIn(-10, 10)
        set(value) = prefs.edit().putInt(SettingsDefaults.KEY_HEAD_SIZE, value.coerceIn(-10, 10)).apply()

    var roughness: Int
        get() = prefs.getInt(SettingsDefaults.KEY_ROUGHNESS, SettingsDefaults.DEFAULT_ROUGHNESS).coerceIn(-10, 10)
        set(value) = prefs.edit().putInt(SettingsDefaults.KEY_ROUGHNESS, value.coerceIn(-10, 10)).apply()

    var breathiness: Int
        get() = prefs.getInt(SettingsDefaults.KEY_BREATHINESS, SettingsDefaults.DEFAULT_BREATHINESS).coerceIn(-10, 10)
        set(value) = prefs.edit().putInt(SettingsDefaults.KEY_BREATHINESS, value.coerceIn(-10, 10)).apply()

    // Emoji / Emoticon
    var enableEmoji: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_EMOJI_EMOTICON, SettingsDefaults.DEFAULT_EMOJI_EMOTICON)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_EMOJI_EMOTICON, value).apply()

    // Process punctuation & level
    var processPunctuation: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_PROCESS_PUNCTUATION, SettingsDefaults.DEFAULT_PROCESS_PUNCTUATION)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_PROCESS_PUNCTUATION, value).apply()

    var punctuationLevel: Int
        get() = prefs.getInt(SettingsDefaults.KEY_PUNCTUATION_LEVEL, SettingsDefaults.DEFAULT_PUNCTUATION_LEVEL).coerceIn(0, 3)
        set(value) = prefs.edit().putInt(SettingsDefaults.KEY_PUNCTUATION_LEVEL, value.coerceIn(0, 3)).apply()

    // Number processing & mode
    var useNumberProcessing: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_USE_NUMBER_PROCESSING, SettingsDefaults.DEFAULT_USE_NUMBER_PROCESSING)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_USE_NUMBER_PROCESSING, value).apply()

    var numberProcessingMode: Int
        get() = prefs.getInt(SettingsDefaults.KEY_NUMBER_PROCESSING_MODE, SettingsDefaults.DEFAULT_NUMBER_PROCESSING_MODE).coerceIn(0, 2)
        set(value) = prefs.edit().putInt(SettingsDefaults.KEY_NUMBER_PROCESSING_MODE, value.coerceIn(0, 2)).apply()

    // Abbreviations & Intonation pauses
    var useAbbreviations: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_USE_ABBREVIATIONS, SettingsDefaults.DEFAULT_USE_ABBREVIATIONS)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_USE_ABBREVIATIONS, value).apply()

    var intonationPauses: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_INTONATION_PAUSES, SettingsDefaults.DEFAULT_INTONATION_PAUSES)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_INTONATION_PAUSES, value).apply()

    // Force language & Language
    var forceLanguage: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_FORCE_LANGUAGE, SettingsDefaults.DEFAULT_FORCE_LANGUAGE)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_FORCE_LANGUAGE, value).apply()

    var language: String
        get() = prefs.getString(SettingsDefaults.KEY_LANGUAGE, SettingsDefaults.DEFAULT_LANGUAGE) ?: SettingsDefaults.DEFAULT_LANGUAGE
        set(value) = prefs.edit().putString(SettingsDefaults.KEY_LANGUAGE, value).apply()

    // Sampling rate
    var samplingRate: Int
        get() {
            val rate = prefs.getInt(SettingsDefaults.KEY_SAMPLING_RATE, SettingsDefaults.DEFAULT_SAMPLING_RATE)
            return if (rate in SettingsDefaults.SUPPORTED_SAMPLING_RATES) rate else SettingsDefaults.DEFAULT_SAMPLING_RATE
        }
        set(value) {
            val valid = if (value in SettingsDefaults.SUPPORTED_SAMPLING_RATES) value else SettingsDefaults.DEFAULT_SAMPLING_RATE
            prefs.edit().putInt(SettingsDefaults.KEY_SAMPLING_RATE, valid).apply()
        }

    // Engine options
    var phrasePrediction: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_PHRASE_PREDICTION, SettingsDefaults.DEFAULT_PHRASE_PREDICTION)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_PHRASE_PREDICTION, value).apply()

    var respectAppVoiceRequests: Boolean
        get() = prefs.getBoolean(SettingsDefaults.KEY_RESPECT_APP_VOICE, SettingsDefaults.DEFAULT_RESPECT_APP_VOICE)
        set(value) = prefs.edit().putBoolean(SettingsDefaults.KEY_RESPECT_APP_VOICE, value).apply()

    // Reset all settings to default
    fun resetAll() {
        prefs.edit()
            .putInt(SettingsDefaults.KEY_VOICE_PROFILE, SettingsDefaults.DEFAULT_VOICE_PROFILE)
            .putBoolean(SettingsDefaults.KEY_FORCE_SPEECH_RATE, SettingsDefaults.DEFAULT_FORCE_SPEECH_RATE)
            .putInt(SettingsDefaults.KEY_SPEECH_RATE, SettingsDefaults.DEFAULT_SPEECH_RATE)
            .putBoolean(SettingsDefaults.KEY_UNLOCK_MAX_RATE, SettingsDefaults.DEFAULT_UNLOCK_MAX_RATE)
            .putBoolean(SettingsDefaults.KEY_FORCE_PITCH, SettingsDefaults.DEFAULT_FORCE_PITCH)
            .putInt(SettingsDefaults.KEY_PITCH, SettingsDefaults.DEFAULT_PITCH)
            .putBoolean(SettingsDefaults.KEY_FORCE_VOLUME, SettingsDefaults.DEFAULT_FORCE_VOLUME)
            .putInt(SettingsDefaults.KEY_VOLUME, SettingsDefaults.DEFAULT_VOLUME)
            .putInt(SettingsDefaults.KEY_INFLECTION, SettingsDefaults.DEFAULT_INFLECTION)
            .putInt(SettingsDefaults.KEY_HEAD_SIZE, SettingsDefaults.DEFAULT_HEAD_SIZE)
            .putInt(SettingsDefaults.KEY_ROUGHNESS, SettingsDefaults.DEFAULT_ROUGHNESS)
            .putInt(SettingsDefaults.KEY_BREATHINESS, SettingsDefaults.DEFAULT_BREATHINESS)
            .putBoolean(SettingsDefaults.KEY_EMOJI_EMOTICON, SettingsDefaults.DEFAULT_EMOJI_EMOTICON)
            .putBoolean(SettingsDefaults.KEY_PROCESS_PUNCTUATION, SettingsDefaults.DEFAULT_PROCESS_PUNCTUATION)
            .putInt(SettingsDefaults.KEY_PUNCTUATION_LEVEL, SettingsDefaults.DEFAULT_PUNCTUATION_LEVEL)
            .putBoolean(SettingsDefaults.KEY_USE_NUMBER_PROCESSING, SettingsDefaults.DEFAULT_USE_NUMBER_PROCESSING)
            .putInt(SettingsDefaults.KEY_NUMBER_PROCESSING_MODE, SettingsDefaults.DEFAULT_NUMBER_PROCESSING_MODE)
            .putBoolean(SettingsDefaults.KEY_USE_ABBREVIATIONS, SettingsDefaults.DEFAULT_USE_ABBREVIATIONS)
            .putBoolean(SettingsDefaults.KEY_INTONATION_PAUSES, SettingsDefaults.DEFAULT_INTONATION_PAUSES)
            .putBoolean(SettingsDefaults.KEY_FORCE_LANGUAGE, SettingsDefaults.DEFAULT_FORCE_LANGUAGE)
            .putString(SettingsDefaults.KEY_LANGUAGE, SettingsDefaults.DEFAULT_LANGUAGE)
            .putInt(SettingsDefaults.KEY_SAMPLING_RATE, SettingsDefaults.DEFAULT_SAMPLING_RATE)
            .putBoolean(SettingsDefaults.KEY_PHRASE_PREDICTION, SettingsDefaults.DEFAULT_PHRASE_PREDICTION)
            .putBoolean(SettingsDefaults.KEY_RESPECT_APP_VOICE, SettingsDefaults.DEFAULT_RESPECT_APP_VOICE)
            .apply()
    }
}
