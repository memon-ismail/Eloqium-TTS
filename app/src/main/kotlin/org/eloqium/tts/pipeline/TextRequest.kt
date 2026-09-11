package org.eloqium.tts.pipeline

/**
 * Encapsulates a text synthesis request with caller voice, prosody, and pacing parameters.
 */
data class TextRequest(
    val text: String,
    val voiceName: String? = null,
    val speechRate: Int = 100,      // Rate percentage (100 = 1.0x normal)
    val pitch: Int = 100,           // Pitch percentage (100 = 1.0x normal)
    val phrasePrediction: Boolean = true,
    val pauseMode: Int = PauseProcessor.PAUSE_KEEP,
    val spokenPunctuation: Boolean = false
)
