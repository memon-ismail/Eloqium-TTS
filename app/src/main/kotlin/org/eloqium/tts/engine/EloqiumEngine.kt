package org.eloqium.tts.engine

import android.util.Log
import org.eloqium.tts.pipeline.UnicodeNormalizer

/**
 * Thread-safe wrapper around a native OpenEVV synthesis instance.
 */
class EloqiumEngine private constructor(private val handle: Long, val language: Int) {
    private val lock = Any()
    @Volatile private var closed = false

    var sampleRateHz: Int = Eci.sampleRateHz(1)
        private set

    private var baseSpeed = Eci.DEFAULT_SPEED
    private var basePitch = 50

    companion object {
        private const val TAG = "EloqiumEngine"

        fun open(language: Int, initialSampleRateHz: Int = 11025): EloqiumEngine? {
            if (!NativeEngine.loaded) {
                Log.e(TAG, "Native library not loaded", NativeEngine.loadError)
                return null
            }
            val handle = NativeEngine.create(language)
            if (handle == 0L) {
                Log.e(TAG, "Failed to create native engine for language 0x%08x".format(language))
                return null
            }
            return EloqiumEngine(handle, language).apply { configure(initialSampleRateHz) }
        }
    }

    private fun configure(initialSampleRateHz: Int) {
        NativeEngine.setParam(handle, Eci.PARAM_LANGUAGE_DIALECT, language)
        NativeEngine.setParam(handle, Eci.PARAM_SYNTH_MODE, 0)
        NativeEngine.setParam(handle, Eci.PARAM_REAL_WORLD_UNITS, 0)
        NativeEngine.setParam(handle, Eci.PARAM_INPUT_TYPE, 1) // Enable annotations
        setSampleRate(initialSampleRateHz)
        applyVoice(0)
    }

    fun close() = synchronized(lock) {
        if (!closed) {
            closed = true
            NativeEngine.stop(handle)
            NativeEngine.destroy(handle)
        }
    }

    fun setSampleRate(hz: Int): Boolean = synchronized(lock) {
        if (closed) return false
        val index = Eci.sampleRateIndex(hz)
        val rc = NativeEngine.setParam(handle, Eci.PARAM_SAMPLE_RATE, index)
        val actual = NativeEngine.getParam(handle, Eci.PARAM_SAMPLE_RATE)
        sampleRateHz = Eci.sampleRateHz(actual)
        return rc >= 0 && sampleRateHz == Eci.sampleRateHz(index)
    }

    val currentBasePitch: Int get() = basePitch
    val currentBaseSpeed: Int get() = baseSpeed

    fun setAbbreviations(enabled: Boolean) {
        synchronized(lock) {
            if (closed) return
            NativeEngine.setParam(handle, Eci.PARAM_DICTIONARY, if (enabled) 0 else 1)
        }
    }

    fun applyVoice(presetIndex: Int, shape: Map<Int, Int> = emptyMap()) {
        synchronized(lock) {
            if (closed) return
            val preset = (presetIndex + Eci.FIRST_PRESET).coerceIn(Eci.FIRST_PRESET, Eci.LAST_PRESET)
            NativeEngine.copyVoice(handle, preset, Eci.SCRATCH_VOICE)
            NativeEngine.setVoiceParam(
                handle, Eci.SCRATCH_VOICE, Eci.VOICE_SPEED,
                shape[Eci.VOICE_SPEED] ?: Eci.DEFAULT_SPEED
            )
            for ((param, value) in shape) {
                NativeEngine.setVoiceParam(handle, Eci.SCRATCH_VOICE, param, Eci.clampVoice(param, value))
            }
            NativeEngine.copyVoice(handle, Eci.SCRATCH_VOICE, Eci.VOICE_CURRENT)
            baseSpeed = NativeEngine.getVoiceParam(handle, Eci.VOICE_CURRENT, Eci.VOICE_SPEED)
            basePitch = NativeEngine.getVoiceParam(handle, Eci.VOICE_CURRENT, Eci.VOICE_PITCH_BASELINE)
        }
    }

    fun setSpeed(speed: Int) {
        synchronized(lock) {
            if (closed) return
            NativeEngine.setVoiceParam(handle, Eci.VOICE_CURRENT, Eci.VOICE_SPEED, speed.coerceIn(0, Eci.SPEED_MAX))
        }
    }

    fun setPitch(pitch: Int) {
        synchronized(lock) {
            if (closed) return
            NativeEngine.setVoiceParam(handle, Eci.VOICE_CURRENT, Eci.VOICE_PITCH_BASELINE, pitch.coerceIn(0, Eci.PERCENT_MAX))
        }
    }

    fun setRatePercent(percent: Int) {
        synchronized(lock) {
            if (closed) return
            val speed = SpeechRateMapper.mapAndroidRate(percent)
            NativeEngine.setVoiceParam(handle, Eci.VOICE_CURRENT, Eci.VOICE_SPEED, speed)
        }
    }

    fun setPitchPercent(percent: Int) {
        synchronized(lock) {
            if (closed) return
            val pitch = (basePitch.toLong() * percent / 100).toInt().coerceIn(0, Eci.PERCENT_MAX)
            NativeEngine.setVoiceParam(handle, Eci.VOICE_CURRENT, Eci.VOICE_PITCH_BASELINE, pitch)
        }
    }

    fun speak(text: String): Boolean = synchronized(lock) {
        if (closed) return false
        val bytes = org.eloqium.tts.pipeline.OpenEVVEncoder.encode(UnicodeNormalizer.normalize(text))
        return NativeEngine.speak(handle, bytes)
    }

    fun speakBytes(bytes: ByteArray): Boolean = synchronized(lock) {
        if (closed) return false
        return NativeEngine.speak(handle, bytes)
    }

    fun read(dst: ByteArray): Int {
        if (closed) return -1
        return NativeEngine.read(handle, dst)
    }

    fun stop() {
        if (!closed) NativeEngine.stop(handle)
    }
}
