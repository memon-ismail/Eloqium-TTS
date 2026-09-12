package org.eloqium.tts.pipeline

import org.eloqium.tts.service.SettingsDefaults

/**
 * Screen reader punctuation processor supporting None, Some, Most, All levels.
 *
 * Deterministic rules:
 * - NONE: Prosodic punctuation only; punctuation marks guide phrasing/pausing.
 * - SOME: Critical syntactic and mathematical symbols (*, /, \, #, %, &, +, =, @, ^, ~, <, >, |, $).
 * - MOST: SOME + structural delimiters (parentheses, brackets, braces, quotes, dashes, colons, semicolons).
 * - ALL:  MOST + sentence and clause terminators (period/dot, comma, question mark, exclamation mark).
 *
 * Protects numbers (3.14, 1,024), times (12:30), and handles lone punctuation and repeated punctuation safely.
 */
object ScreenReaderPunctuationProcessor {

    private val SPACE_BEFORE_PUNCT = Regex("""([A-Za-z0-9\W&&[^\s]])\s+([,.:;?!])(?=\s|$)""")
    private val MULTI_SPACE = Regex("""[ \t]+""")

    // Protected patterns: decimals, thousands separators, times
    private val DECIMAL_REGEX = Regex("""\b\d+\.\d+\b""")
    private val THOUSANDS_REGEX = Regex("""\b\d{1,3}(,\d{3})+\b""")
    private val TIME_REGEX = Regex("""\b\d{1,2}:\d{2}\b""")

    // Mappings for SOME
    private val SOME_MAP = mapOf(
        '#' to " number ",
        '$' to " dollar ",
        '%' to " percent ",
        '&' to " and ",
        '*' to " star ",
        '+' to " plus ",
        '=' to " equals ",
        '/' to " slash ",
        '\\' to " backslash ",
        '@' to " at ",
        '^' to " caret ",
        '~' to " tilde ",
        '<' to " less than ",
        '>' to " greater than ",
        '|' to " bar "
    )

    // Mappings added in MOST
    private val MOST_ADDITIONS = mapOf(
        '(' to " left paren ",
        ')' to " right paren ",
        '[' to " left bracket ",
        ']' to " right bracket ",
        '{' to " left brace ",
        '}' to " right brace ",
        '"' to " quote ",
        '\'' to " apostrophe ",
        '-' to " dash ",
        '–' to " dash ",
        '—' to " dash ",
        '_' to " underscore ",
        ':' to " colon ",
        ';' to " semicolon "
    )

    // Mappings added in ALL
    private val ALL_ADDITIONS = mapOf(
        '.' to " dot ",
        ',' to " comma ",
        '?' to " question mark ",
        '!' to " exclamation mark "
    )

    // Extended symbols for custom mode recognition
    private val EXTENDED_SYMBOLS = mapOf(
        '€' to " euro ",
        '£' to " pound ",
        '¥' to " yen ",
        '¢' to " cent ",
        '₹' to " rupee ",
        '§' to " section ",
        '°' to " degree ",
        '±' to " plus minus ",
        '÷' to " divided by ",
        '×' to " times ",
        '¿' to " inverted question mark ",
        '¡' to " inverted exclamation mark ",
        '©' to " copyright ",
        '®' to " registered ",
        '™' to " trademark ",
        '•' to " bullet ",
        '`' to " grave accent "
    )

    private val MASTER_PUNCT_MAP = SOME_MAP + MOST_ADDITIONS + ALL_ADDITIONS + EXTENDED_SYMBOLS

    fun buildCustomMap(customChars: String): Map<Char, String> {
        if (customChars.isEmpty()) return emptyMap()
        val result = mutableMapOf<Char, String>()
        for (c in customChars.toSet()) {
            if (c.isWhitespace()) continue
            val spoken = MASTER_PUNCT_MAP[c] ?: " $c "
            result[c] = spoken
        }
        return result
    }

    fun process(
        text: String,
        enabled: Boolean = false,
        level: Int = SettingsDefaults.PUNCT_NONE,
        customPunctuation: String = ""
    ): String {
        if (text.isEmpty()) return text
        if (!enabled || level == SettingsDefaults.PUNCT_NONE) {
            // Prosodic punctuation mode
            val out = SPACE_BEFORE_PUNCT.replace(text, "$1$2")
            return MULTI_SPACE.replace(out, " ").trim()
        }

        // Active spoken punctuation map based on level
        val activeMap = when (level) {
            SettingsDefaults.PUNCT_SOME -> SOME_MAP
            SettingsDefaults.PUNCT_MOST -> SOME_MAP + MOST_ADDITIONS
            SettingsDefaults.PUNCT_ALL -> SOME_MAP + MOST_ADDITIONS + ALL_ADDITIONS
            SettingsDefaults.PUNCT_CUSTOM -> buildCustomMap(customPunctuation)
            else -> emptyMap()
        }

        if (activeMap.isEmpty()) {
            val out = SPACE_BEFORE_PUNCT.replace(text, "$1$2")
            return MULTI_SPACE.replace(out, " ").trim()
        }

        // Tokenize while protecting numeric patterns
        val boundText = SPACE_BEFORE_PUNCT.replace(text, "$1$2")
        val sb = StringBuilder(boundText.length * 2)
        var i = 0
        val len = boundText.length

        while (i < len) {
            // Check if current position matches a decimal or thousands or time
            val sub = boundText.substring(i)
            val decMatch = DECIMAL_REGEX.find(sub)
            if (decMatch != null && decMatch.range.first == 0) {
                // If level is ALL, even dot/comma in numbers might need reading or keep natural
                // Standard screen reader: at ALL level, 3.14 -> 3 dot 14
                if (level == SettingsDefaults.PUNCT_ALL) {
                    val numStr = decMatch.value
                    val transformed = numStr.replace(".", " dot ")
                    sb.append(transformed)
                } else {
                    sb.append(decMatch.value)
                }
                i += decMatch.value.length
                continue
            }

            val thouMatch = THOUSANDS_REGEX.find(sub)
            if (thouMatch != null && thouMatch.range.first == 0) {
                if (level == SettingsDefaults.PUNCT_ALL) {
                    val numStr = thouMatch.value
                    val transformed = numStr.replace(",", " comma ")
                    sb.append(transformed)
                } else {
                    sb.append(thouMatch.value)
                }
                i += thouMatch.value.length
                continue
            }

            val timeMatch = TIME_REGEX.find(sub)
            if (timeMatch != null && timeMatch.range.first == 0) {
                if (level == SettingsDefaults.PUNCT_MOST || level == SettingsDefaults.PUNCT_ALL) {
                    val timeStr = timeMatch.value
                    val transformed = timeStr.replace(":", " colon ")
                    sb.append(transformed)
                } else {
                    sb.append(timeMatch.value)
                }
                i += timeMatch.value.length
                continue
            }

            val c = boundText[i]
            val spoken = activeMap[c]
            if (spoken != null) {
                sb.append(spoken)
            } else {
                sb.append(c)
            }
            i++
        }

        return MULTI_SPACE.replace(sb.toString().trim(), " ")
    }

    // Backward-compat overload
    fun process(text: String, spokenPunctuation: Boolean): String {
        return process(
            text,
            enabled = spokenPunctuation,
            level = if (spokenPunctuation) SettingsDefaults.PUNCT_ALL else SettingsDefaults.PUNCT_NONE
        )
    }
}
