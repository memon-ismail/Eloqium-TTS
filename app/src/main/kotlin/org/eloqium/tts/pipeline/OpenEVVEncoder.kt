package org.eloqium.tts.pipeline

/**
 * Encodes processed strings into ISO-8859-1 / Windows-1252 byte arrays for OpenEVV native synthesis.
 */
object OpenEVVEncoder {

    private const val MAX_BYTE = 0xFF

    fun encode(text: String): ByteArray {
        val out = ByteArray(text.length)
        for (i in text.indices) {
            val code = text[i].code
            out[i] = if (code in 1..MAX_BYTE) code.toByte() else ' '.code.toByte()
        }
        return out
    }
}
