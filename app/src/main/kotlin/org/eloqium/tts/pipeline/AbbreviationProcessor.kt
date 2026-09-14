package org.eloqium.tts.pipeline

import java.util.regex.Pattern

/**
 * Expands common abbreviations using [AbbreviationDictionary] when enabled.
 * Accurately handles context: word boundaries, surrounding punctuation,
 * numbers, and disambiguation (e.g. "St." as Saint vs Street).
 */
object AbbreviationProcessor {

    private class CompiledEntry(
        val pattern: Pattern,
        val expansion: String
    )

    private val COMPILED_ENTRIES: List<CompiledEntry> = AbbreviationDictionary.ENTRIES.map { entry ->
        val escaped = Pattern.quote(entry.abbreviation)
        val flags = if (entry.isCaseSensitive) 0 else Pattern.CASE_INSENSITIVE
        // Capture boundary opener/whitespace before abbreviation, and lookahead boundary delimiter after
        val regex = "(^|[\\s(\\[{\"\'‘“])($escaped)(?=[\\s,\\]!?:;\"\'’”]|$|\\b)"
        CompiledEntry(Pattern.compile(regex, flags), entry.expansion)
    }

    // Disambiguation for "St."
    // 1. Saint: St. followed by a capitalized name (e.g., "St. Jude", "St. Patrick")
    private val SAINT_PATTERN = Pattern.compile(
        "(^|[\\s(\\[{\"\'‘“])(St\\.)(?=\\s+[A-Z])"
    )
    // 2. Street: St. preceded by a name or number (e.g., "Main St.", "5th St.")
    private val STREET_PATTERN = Pattern.compile(
        "([A-Za-z0-9][\\s]+)(St\\.)(?=[\\s,\\]!?:;\"\'’”]|$|\\b)"
    )

    private val PHONETIC_TAG_REGEX = Regex("""`\[[^\]\r\n\t]+\]""")

    fun process(text: String, enabled: Boolean): String {
        if (!enabled || text.isEmpty()) return text

        if (!text.contains("`[")) {
            return expand(text)
        }

        val sb = StringBuilder(text.length + 16)
        var cursor = 0
        for (match in PHONETIC_TAG_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > cursor) {
                sb.append(expand(text.substring(cursor, start)))
            }
            sb.append(match.value)
            cursor = end
        }
        if (cursor < text.length) {
            sb.append(expand(text.substring(cursor)))
        }
        return sb.toString()
    }

    private fun expand(text: String): String {
        var result = text

        // 1. Handle St. disambiguation
        result = SAINT_PATTERN.matcher(result).replaceAll("$1Saint")
        result = STREET_PATTERN.matcher(result).replaceAll("$1Street")

        // 2. Handle standard dictionary entries
        for (compiled in COMPILED_ENTRIES) {
            val matcher = compiled.pattern.matcher(result)
            if (matcher.find()) {
                result = matcher.replaceAll("$1${compiled.expansion}")
            }
        }

        return result
    }
}
