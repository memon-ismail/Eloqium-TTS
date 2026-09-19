package org.eloqium.tts.pipeline

import org.eloqium.tts.engine.VoiceParameterMapper
import org.eloqium.tts.service.SettingsDefaults

/**
 * Pipeline processor for Capitals indication.
 * Supports None (default), Pitch raise (raises pitch only for capital letters),
 * and Say capital (verbally indicates uppercase letters).
 * Strictly preserves OpenEVV phonetic SPR tags (`[...]`) and ECI tags.
 */
object CapitalsProcessor {

    private val PHONETIC_TAG_REGEX = Regex("""`\[[^\]\r\n\t]+\]""")
    private val ECI_TAG_REGEX = Regex("""`([a-z]{1,2}\d{0,4}|[0-4])(?=[^A-Za-z0-9]|$)""")

    fun process(
        text: String,
        mode: Int = SettingsDefaults.CAPITALS_NONE,
        basePitch: Int = VoiceParameterMapper.DEFAULT_PITCH_BASELINE
    ): String {
        if (text.isEmpty() || mode == SettingsDefaults.CAPITALS_NONE) {
            return text
        }

        return when (mode) {
            SettingsDefaults.CAPITALS_PITCH_RAISE -> applyPitchRaise(text, basePitch)
            SettingsDefaults.CAPITALS_SAY_CAPITAL -> applySayCapital(text)
            else -> text
        }
    }

    /**
     * Checks whether the character at [index] is an uppercase letter that constitutes
     * a complete standalone one-letter word.
     * Standalone means not part of a larger word, acronym, compound, or alphanumeric token.
     */
    fun isStandaloneCapitalWord(text: String, index: Int): Boolean {
        val c = text[index]
        if (!c.isLetter() || !c.isUpperCase()) return false

        // Check preceding character boundary
        if (index > 0) {
            val prev = text[index - 1]
            if (prev.isLetterOrDigit() || prev == '_') return false
            // Check attached apostrophe or hyphen (e.g. "didn't", "A-team", "pre-A")
            if ((prev == '\'' || prev == '’' || prev == '-') && index >= 2) {
                val beforePunct = text[index - 2]
                if (beforePunct.isLetterOrDigit() || beforePunct == '_') return false
            }
        }

        // Check following character boundary
        if (index + 1 < text.length) {
            val next = text[index + 1]
            if (next.isLetterOrDigit() || next == '_') return false
            // Check attached apostrophe or hyphen (e.g. "A's", "I'm", "A-team", "A-1")
            if ((next == '\'' || next == '’' || next == '-') && index + 2 < text.length) {
                val afterPunct = text[index + 2]
                if (afterPunct.isLetterOrDigit() || afterPunct == '_') return false
            }
        }

        return true
    }

    /**
     * Pitch raise: raises pitch only when an uppercase letter is a complete standalone one-letter word,
     * restoring baseline pitch immediately after.
     */
    private fun applyPitchRaise(text: String, basePitch: Int): String {
        val raisedPitch = (basePitch + 20).coerceIn(0, 100)
        val sb = StringBuilder(text.length + 32)
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]

            // Protect OpenEVV phonetic SPR tags (`[...]`)
            if (c == '`' && i + 1 < len && text[i + 1] == '[') {
                val end = text.indexOf(']', i + 2)
                if (end != -1) {
                    sb.append(text, i, end + 1)
                    i = end + 1
                    continue
                }
            }

            // Protect existing ECI tags (`vb65, `p100, etc.)
            if (c == '`' && i + 1 < len && text[i + 1].isLetterOrDigit()) {
                var j = i + 1
                while (j < len && text[j].isLetterOrDigit()) j++
                sb.append(text, i, j)
                i = j
                continue
            }

            if (isStandaloneCapitalWord(text, i)) {
                sb.append("`vb").append(raisedPitch)
                sb.append(c)
                sb.append("`vb").append(basePitch)
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    /**
     * Say capital: verbally announces "capital" immediately before each uppercase letter without classifying lowercase letters as capital.
     */
    private fun applySayCapital(text: String): String {
        val sb = StringBuilder(text.length + 32)
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]

            // Protect OpenEVV phonetic SPR tags (`[...]`)
            if (c == '`' && i + 1 < len && text[i + 1] == '[') {
                val end = text.indexOf(']', i + 2)
                if (end != -1) {
                    sb.append(text, i, end + 1)
                    i = end + 1
                    continue
                }
            }

            // Protect existing ECI tags
            if (c == '`' && i + 1 < len && text[i + 1].isLetterOrDigit()) {
                var j = i + 1
                while (j < len && text[j].isLetterOrDigit()) j++
                sb.append(text, i, j)
                i = j
                continue
            }

            if (c.isLetter()) {
                val start = i
                while (i < len && text[i].isLetter()) i++
                val word = text.substring(start, i)
                sb.append(processWord(word))
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun processWord(word: String): String {
        var uppers = 0
        for (ch in word) {
            if (ch.isUpperCase()) uppers++
        }
        if (uppers == 0) return word

        // All uppercase word (e.g. "APPLE" -> "capital A capital P capital P capital L capital E")
        if (uppers == word.length) {
            val sb = StringBuilder()
            for ((idx, ch) in word.withIndex()) {
                if (idx > 0) sb.append(" ")
                sb.append("capital ").append(ch)
            }
            return sb.toString()
        }

        // Single letter (e.g. "A" -> "capital A")
        if (word.length == 1) {
            return "capital $word"
        }

        // Standard capitalized word (e.g. "Apple" -> "capital Apple")
        if (word[0].isUpperCase() && uppers == 1) {
            return "capital $word"
        }

        // Mixed-case / camelCase / PascalCase (e.g. "McDonald" -> "capital Mc capital Donald", "iPhone" -> "i capital Phone")
        val sb = StringBuilder()
        var partStart = 0
        for (idx in 1 until word.length) {
            if (word[idx].isUpperCase() && word[idx - 1].isLowerCase()) {
                val part = word.substring(partStart, idx)
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(processWord(part))
                partStart = idx
            }
        }
        val lastPart = word.substring(partStart)
        if (sb.isNotEmpty()) sb.append(" ")
        sb.append(processWord(lastPart))
        return sb.toString()
    }
}
