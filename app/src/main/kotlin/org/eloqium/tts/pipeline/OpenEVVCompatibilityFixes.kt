package org.eloqium.tts.pipeline

/**
 * Text transformations specifically designed to avoid OpenEVV parsing and spelling quirks.
 * Isolated from screen reader punctuation logic to maintain clear architectural boundaries.
 */
object OpenEVVCompatibilityFixes {

    /** Letter followed immediately by a digit causes Eloquence to spell out the entire token. */
    private val BEFORE_A_DIGIT = Regex("""([A-Za-z])(\d)""")

    /** Openers that cause Eloquence to start spelling instead of pronouncing words. */
    private const val OPENERS = """~#${'$'}%^*({|\[<•"""
    private val BEFORE_AN_OPENER = Regex("([A-Za-z]+)([" + Regex.escape(OPENERS) + "])")

    /** "books (s)" back to "books(s)", avoiding long awkward pauses. */
    private val LOOSE_S_SUFFIX = Regex("""([A-Za-z]+)\s+(\(s\))""")

    /** Commas around a round thousand ("1,000,000") which Eloquence misreads as "one comma hundred". */
    private val GROUPED_THOUSANDS = Regex("""\b\d{1,3},000(?:,\d{3})+\b""")

    /** Valid ECI command tags starting with backtick. Any unapproved backtick is sanitized. */
    private val VALID_ECI_TAG = Regex("""^`[a-z]{1,2}\d{0,4}$""")

    fun apply(text: String): String {
        if (text.isEmpty()) return text
        var out = text
        out = BEFORE_A_DIGIT.replace(out, "$1 $2")
        out = BEFORE_AN_OPENER.replace(out, "$1 $2")
        out = LOOSE_S_SUFFIX.replace(out, "$1$2")
        out = GROUPED_THOUSANDS.replace(out) { it.value.replace(",", "") }
        out = sanitizeBackticks(out)
        return out
    }

    /**
     * Sanitizes backtick characters to prevent rogue ECI escape injection.
     */
    fun sanitizeBackticks(text: String): String {
        if (!text.contains('`')) return text
        val parts = text.split("`")
        val sb = StringBuilder(parts[0])
        for (i in 1 until parts.size) {
            val part = parts[i]
            val token = "`" + part.takeWhile { it.isLetterOrDigit() }
            if (VALID_ECI_TAG.matches(token)) {
                sb.append('`').append(part)
            } else {
                sb.append('\'').append(part)
            }
        }
        return sb.toString()
    }
}
