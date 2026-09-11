package org.eloqium.tts.pipeline

import java.text.Normalizer

/**
 * Normalizes modern Unicode text into the Western character set supported by OpenEVV.
 * Handles typographic quotes, dashes, ellipses, non-breaking spaces, symbols, and diacritics.
 */
object UnicodeNormalizer {

    private const val HIGHEST_BYTE = 0xFF

    /**
     * Complete mapping of typographic, math, currency, and special characters
     * into plain Western ASCII equivalents supported by Eloquence / OpenEVV.
     */
    private val REPLACEMENTS = mapOf(
        // Single quotes & apostrophes
        '‘' to "'", '’' to "'", '‚' to "'", '‛' to "'", '′' to "'",
        // Double quotes
        '“' to "\"", '”' to "\"", '„' to "\"", '‟' to "\"", '«' to "\"", '»' to "\"", '″' to "\"",
        // Hyphens & dashes
        '‐' to "-", '‑' to "-", '‒' to "-", '–' to "-", '—' to "-", '―' to "-", '−' to "-",
        // Ellipsis & bullets
        '…' to "...", '•' to ".", '·' to ".",
        // Whitespace & zero-width
        '\u00A0' to " ", '\u2007' to " ", '\u202F' to " ",
        '\u200B' to "", '\u200C' to "", '\u200D' to "", '\uFEFF' to "",
        // Angles & slashes
        '‹' to "<", '›' to ">", '⁄' to "/", '∕' to "/",
        // Math & symbols
        '×' to "x", '™' to " trademark ", '№' to " number ",
        '€' to " euro ", '£' to " pounds ", '¥' to " yen ",
        '≤' to " less than or equal to ", '≥' to " greater than or equal to ",
        '≠' to " not equal to ", '≈' to " about ", '∞' to " infinity ",
        '→' to " to ", '←' to " from ", '°' to " degrees "
    )

    fun normalize(text: String): String {
        if (text.isEmpty()) return text
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val rep = REPLACEMENTS[c]
            when {
                rep != null -> sb.append(rep)
                c.code == 0 -> sb.append(' ')
                c.code <= HIGHEST_BYTE -> sb.append(c)
                else -> sb.append(fallback(text, i))
            }
            i += if (Character.isHighSurrogate(c) && i + 1 < text.length) 2 else 1
        }
        return sb.toString()
    }

    private fun fallback(text: String, at: Int): String {
        val c = text[at]
        val one = if (Character.isHighSurrogate(c) && at + 1 < text.length) {
            text.substring(at, at + 2)
        } else {
            c.toString()
        }
        val stripped = Normalizer.normalize(one, Normalizer.Form.NFD)
            .filter { it.code <= HIGHEST_BYTE && !isCombining(it) }
        return if (stripped.isEmpty()) " " else stripped
    }

    private fun isCombining(c: Char): Boolean = when (Character.getType(c).toByte()) {
        Character.NON_SPACING_MARK,
        Character.COMBINING_SPACING_MARK,
        Character.ENCLOSING_MARK -> true
        else -> false
    }
}
