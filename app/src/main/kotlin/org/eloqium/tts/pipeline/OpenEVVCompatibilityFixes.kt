package org.eloqium.tts.pipeline

/**
 * Text transformations specifically designed to avoid OpenEVV parsing and spelling quirks.
 * Isolated from screen reader punctuation logic to maintain clear architectural boundaries.
 */
object OpenEVVCompatibilityFixes {

    private val PHONETIC_TAG_REGEX = Regex("""`\[[^\]\r\n\t]+\]""")

    /**
     * Supported user ECI voice and control annotation tags.
     * Matches volume, speed, pitch baseline, fluctuation, head size, roughness, breathiness,
     * numeric pauses (`0-`4, `p<ms>), and phrase prediction tags (`pp0, `pp1).
     */
    private val SUPPORTED_ECI_TAG_REGEX = Regex("""`([a-z]{1,2}\d{0,4}|[0-4])(?=[^A-Za-z0-9]|$)""")

    /** Letter followed immediately by a digit causes Eloquence to spell out the entire token. */
    private val BEFORE_A_DIGIT = Regex("""([A-Za-z])(\d)""")

    /** Openers that cause Eloquence to start spelling instead of pronouncing words. */
    private const val OPENERS = """~#${'$'}%^*({|\[<•"""
    private val BEFORE_AN_OPENER = Regex("([A-Za-z]+)([" + Regex.escape(OPENERS) + "])")

    /** "books (s)" back to "books(s)", avoiding long awkward pauses. */
    private val LOOSE_S_SUFFIX = Regex("""([A-Za-z]+)\s+(\(s\))""")

    /** Commas around a round thousand ("1,000,000") which Eloquence misreads as "one comma hundred". */
    private val GROUPED_THOUSANDS = Regex("""\b\d{1,3},000(?:,\d{3})+\b""")

    fun apply(text: String, eciVoiceTagsEnabled: Boolean = true): String {
        if (text.isEmpty()) return text
        if (!text.contains("`[")) {
            return applyFixes(text, eciVoiceTagsEnabled)
        }

        val sb = StringBuilder(text.length + 16)
        var cursor = 0
        for (match in PHONETIC_TAG_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > cursor) {
                sb.append(applyFixes(text.substring(cursor, start), eciVoiceTagsEnabled))
            }
            // Emit phonetic SPR tag verbatim without digit, opener, or backtick corruption
            sb.append(match.value)
            cursor = end
        }
        if (cursor < text.length) {
            sb.append(applyFixes(text.substring(cursor), eciVoiceTagsEnabled))
        }
        return sb.toString()
    }

    private fun applyFixes(text: String, eciVoiceTagsEnabled: Boolean): String {
        if (text.isEmpty()) return text
        if (!text.contains('`')) {
            return applyNonBacktickFixes(text)
        }

        if (!eciVoiceTagsEnabled) {
            // ECI Voice Tags OFF: convert all user backticks to apostrophe
            val sanitized = text.replace('`', '\'')
            return applyNonBacktickFixes(sanitized)
        }

        // ECI Voice Tags ON: preserve supported ECI tags verbatim; sanitize rogue backticks
        val sb = StringBuilder(text.length + 8)
        var cursor = 0
        for (match in SUPPORTED_ECI_TAG_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > cursor) {
                val nonEciSpan = text.substring(cursor, start).replace('`', '\'')
                sb.append(applyNonBacktickFixes(nonEciSpan))
            }
            sb.append(match.value)
            cursor = end
        }
        if (cursor < text.length) {
            val nonEciSpan = text.substring(cursor).replace('`', '\'')
            sb.append(applyNonBacktickFixes(nonEciSpan))
        }
        return sb.toString()
    }

    private fun applyNonBacktickFixes(text: String): String {
        if (text.isEmpty()) return text
        var out = text
        out = BEFORE_A_DIGIT.replace(out, "$1 $2")
        out = BEFORE_AN_OPENER.replace(out, "$1 $2")
        out = LOOSE_S_SUFFIX.replace(out, "$1$2")
        out = GROUPED_THOUSANDS.replace(out) { it.value.replace(",", "") }
        return out
    }

    /**
     * Sanitizes backtick characters to prevent rogue ECI escape injection.
     * Preserves valid ECI command tags and OpenEVV phonetic SPR tags (`[...]`).
     */
    fun sanitizeBackticks(text: String, eciVoiceTagsEnabled: Boolean = true): String {
        if (!text.contains('`')) return text
        return apply(text, eciVoiceTagsEnabled)
    }
}
