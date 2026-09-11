package org.eloqium.tts.engine

import android.util.Log

/**
 * Low-level JNI interface to libeloqiumjni.so.
 */
object NativeEngine {
    private const val TAG = "NativeEngine"

    val loaded: Boolean
    val loadError: Throwable?

    init {
        var success = false
        var error: Throwable? = null
        try {
            System.loadLibrary("eloqiumjni")
            success = true
        } catch (t: Throwable) {
            error = t
            Log.e(TAG, "Failed to load libeloqiumjni.so", t)
        }
        loaded = success
        loadError = error
    }

    @JvmStatic external fun create(language: Int): Long
    @JvmStatic external fun destroy(handle: Long)
    @JvmStatic external fun speak(handle: Long, text: ByteArray): Boolean
    @JvmStatic external fun read(handle: Long, dst: ByteArray): Int
    @JvmStatic external fun stop(handle: Long)
    @JvmStatic external fun loadDictionary(handle: Long, volume: Int, path: String): Int
    @JvmStatic external fun teachWord(handle: Long, volume: Int, key: ByteArray, say: ByteArray): Int
    @JvmStatic external fun lookUpWord(handle: Long, volume: Int, key: ByteArray): String?
    @JvmStatic external fun forgetDictionaries(handle: Long)
    @JvmStatic external fun setParam(handle: Long, param: Int, value: Int): Int
    @JvmStatic external fun getParam(handle: Long, param: Int): Int
    @JvmStatic external fun setVoiceParam(handle: Long, voice: Int, param: Int, value: Int): Int
    @JvmStatic external fun getVoiceParam(handle: Long, voice: Int, param: Int): Int
    @JvmStatic external fun copyVoice(handle: Long, from: Int, to: Int): Int
    @JvmStatic external fun languages(): IntArray
    @JvmStatic external fun version(): String
}
