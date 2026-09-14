package org.eloqium.tts.pipeline

import org.eloqium.tts.service.SettingsDefaults

/**
 * Preprocessor that transforms numbers into Digits, Pairs, or Triplets before synthesis.
 */
object NumberProcessor {

    private val NUMBER_REGEX = Regex("""\d+""")
    private val PHONETIC_TAG_REGEX = Regex("""`\[[^\]\r\n\t]+\]""")

    fun process(
        text: String,
        enabled: Boolean = false,
        mode: Int = SettingsDefaults.NUMBER_DIGITS
    ): String {
        if (!enabled || text.isEmpty()) return text

        if (!text.contains("`[")) {
            return NUMBER_REGEX.replace(text) { match ->
                val num = match.value
                formatNumber(num, mode)
            }
        }

        val sb = StringBuilder(text.length + 16)
        var cursor = 0
        for (match in PHONETIC_TAG_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > cursor) {
                sb.append(NUMBER_REGEX.replace(text.substring(cursor, start)) { m ->
                    formatNumber(m.value, mode)
                })
            }
            sb.append(match.value)
            cursor = end
        }
        if (cursor < text.length) {
            sb.append(NUMBER_REGEX.replace(text.substring(cursor)) { m ->
                formatNumber(m.value, mode)
            })
        }
        return sb.toString()
    }

    fun formatNumber(digits: String, mode: Int): String {
        if (digits.length <= 1) return digits

        return when (mode) {
            SettingsDefaults.NUMBER_DIGITS -> {
                digits.toCharArray().joinToString(" ")
            }
            SettingsDefaults.NUMBER_PAIRS -> {
                // Group in pairs: e.g. "1984" -> "19 84", "123" -> "12 3", "123456" -> "12 34 56"
                digits.chunked(2).joinToString(" ")
            }
            SettingsDefaults.NUMBER_TRIPLETS -> {
                // Group in triplets from right (standard number grouping): e.g. "1234567" -> "1 234 567", "1000" -> "1 000"
                val len = digits.length
                val firstGroupLen = if (len % 3 == 0) 3 else len % 3
                val sb = StringBuilder()
                sb.append(digits.substring(0, firstGroupLen))
                var idx = firstGroupLen
                while (idx < len) {
                    sb.append(" ")
                    sb.append(digits.substring(idx, idx + 3))
                    idx += 3
                }
                sb.toString()
            }
            else -> digits
        }
    }
}
