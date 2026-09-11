package org.eloqium.tts.pipeline

/**
 * Processes text by replacing Unicode emoji sequences with their spoken names
 * in the specified language. Consecutive emojis are comma-separated for clean
 * Eloquence prosody and pause cadences. Spaces are added around words as needed,
 * while preserving natural attachment to sentence punctuation.
 */
object EmojiProcessor {

    /**
     * Replaces emoji sequences with spoken representations according to [language].
     */
    fun process(text: String, language: String): String {
        if (text.isEmpty() || !EmojiData.isLoaded) return text

        val langIdx = EmojiData.getLanguageIndex(language)
        val sb = StringBuilder(text.length + 64)
        var i = 0
        var prevWasEmoji = false

        while (i < text.length) {
            val match = EmojiData.findLongestMatch(text, i)
            if (match != null) {
                val (matchLen, ttsArray) = match
                val spoken = ttsArray[langIdx] ?: ttsArray[EmojiData.LANG_EN] ?: ""

                if (spoken.isNotEmpty()) {
                    if (prevWasEmoji) {
                        // Trim any whitespace between consecutive emojis
                        while (sb.isNotEmpty() && Character.isWhitespace(sb[sb.length - 1])) {
                            sb.deleteCharAt(sb.length - 1)
                        }
                        sb.append(", ")
                    } else if (sb.isNotEmpty()) {
                        val prevChar = sb[sb.length - 1]
                        if (Character.isLetterOrDigit(prevChar)) {
                            sb.append(' ')
                        }
                    }

                    sb.append(spoken)

                    val nextIdx = i + matchLen
                    if (nextIdx < text.length) {
                        val nextChar = text[nextIdx]
                        if (Character.isLetterOrDigit(nextChar)) {
                            sb.append(' ')
                        }
                    }

                    i += matchLen
                    prevWasEmoji = true
                    continue
                }
            }

            val ch = text[i]
            sb.append(ch)
            i++
            if (!Character.isWhitespace(ch)) {
                prevWasEmoji = false
            }
        }

        return sb.toString()
    }
}
