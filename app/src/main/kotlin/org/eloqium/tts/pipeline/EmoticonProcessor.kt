package org.eloqium.tts.pipeline

import java.util.regex.Pattern

/**
 * Handles conservative ASCII emoticon substitution across all 5 Eloqium language families:
 * en (0), es (1), fr (2), de (3), it (4).
 *
 * Implements strict span protection to prevent false positive replacements inside:
 * - URLs (http://, https://, ftp://, www.)
 * - Times (2:30, 12:45:00)
 * - Ratios (16:9, 4:3)
 * - Windows paths (C:\, D:\)
 * - Scope operators (::)
 * - Code & math comparisons (<, >, :=, ->, =>)
 * - Token boundary context (surrounded by alphanumerics)
 */
object EmoticonProcessor {

    private class EmoticonEntry(
        val pattern: String,
        val en: String,
        val es: String,
        val fr: String,
        val de: String,
        val it: String
    ) {
        fun getTranslation(langIdx: Int): String {
            return when (langIdx) {
                EmojiData.LANG_EN -> en
                EmojiData.LANG_ES -> es
                EmojiData.LANG_FR -> fr
                EmojiData.LANG_DE -> de
                EmojiData.LANG_IT -> it
                else -> en
            }
        }
    }

    // Ordered longest first to guarantee greedy matching
    private val ENTRIES = arrayOf(
        EmoticonEntry("</3", "broken heart", "corazón roto", "cœur brisé", "gebrochenes Herz", "cuore spezzato"),
        EmoticonEntry(":-D", "grinning face", "cara con amplia sonrisa", "visage avec grand sourire", "breit grinsendes Gesicht", "faccina che ride"),
        EmoticonEntry(":D", "grinning face", "cara con amplia sonrisa", "visage avec grand sourire", "breit grinsendes Gesicht", "faccina che ride"),
        EmoticonEntry(":-)", "smiling face", "cara sonriente", "visage souriant", "lächelndes Gesicht", "faccina sorridente"),
        EmoticonEntry(":)", "smiling face", "cara sonriente", "visage souriant", "lächelndes Gesicht", "faccina sorridente"),
        EmoticonEntry(":-(", "frowning face", "cara triste", "visage triste", "trauriges Gesicht", "faccina triste"),
        EmoticonEntry(":(", "frowning face", "cara triste", "visage triste", "trauriges Gesicht", "faccina triste"),
        EmoticonEntry(";--)", "winking face", "cara guiñando el ojo", "visage faisant un clin d'œil", "zwinkerndes Gesicht", "faccina che fa l'occhiolino"),
        EmoticonEntry(";-)", "winking face", "cara guiñando el ojo", "visage faisant un clin d'œil", "zwinkerndes Gesicht", "faccina che fa l'occhiolino"),
        EmoticonEntry(";)", "winking face", "cara guiñando el ojo", "visage faisant un clin d'œil", "zwinkerndes Gesicht", "faccina che fa l'occhiolino"),
        EmoticonEntry(":-P", "face with tongue", "cara sacando la lengua", "visage qui tire la langue", "Gesicht mit herausgestreckter Zunge", "faccina con lingua"),
        EmoticonEntry(":P", "face with tongue", "cara sacando la lengua", "visage qui tire la langue", "Gesicht mit herausgestreckter Zunge", "faccina con lingua"),
        EmoticonEntry(":-p", "face with tongue", "cara sacando la lengua", "visage qui tire la langue", "Gesicht mit herausgestreckter Zunge", "faccina con lingua"),
        EmoticonEntry(":p", "face with tongue", "cara sacando la lengua", "visage qui tire la langue", "Gesicht mit herausgestreckter Zunge", "faccina con lingua"),
        EmoticonEntry(":-/", "skeptical face", "cara escéptica", "visage sceptique", "skeptisches Gesicht", "faccina scettica"),
        EmoticonEntry(":/", "skeptical face", "cara escéptica", "visage sceptique", "skeptisches Gesicht", "faccina scettica"),
        EmoticonEntry(":-\\", "skeptical face", "cara escéptica", "visage sceptique", "skeptisches Gesicht", "faccina scettica"),
        EmoticonEntry(":\\", "skeptical face", "cara escéptica", "visage sceptique", "skeptisches Gesicht", "faccina scettica"),
        EmoticonEntry(":-O", "surprised face", "cara sorprendida", "visage surpris", "überraschtes Gesicht", "faccina sorpresa"),
        EmoticonEntry(":O", "surprised face", "cara sorprendida", "visage surpris", "überraschtes Gesicht", "faccina sorpresa"),
        EmoticonEntry(":-o", "surprised face", "cara sorprendida", "visage surpris", "überraschtes Gesicht", "faccina sorpresa"),
        EmoticonEntry(":o", "surprised face", "cara sorprendida", "visage surpris", "überraschtes Gesicht", "faccina sorpresa"),
        EmoticonEntry(":'-(", "crying face", "cara llorando", "visage qui pleure", "weinendes Gesicht", "faccina che piange"),
        EmoticonEntry(":'(", "crying face", "cara llorando", "visage qui pleure", "weinendes Gesicht", "faccina che piange"),
        EmoticonEntry("B-)", "cool face", "cara genial", "visage cool", "cooles Gesicht", "faccina con occhiali"),
        EmoticonEntry("B)", "cool face", "cara genial", "visage cool", "cooles Gesicht", "faccina con occhiali"),
        EmoticonEntry("-_-", "expressionless face", "cara inexpresiva", "visage inexpressif", "ausdrucksloses Gesicht", "faccina inespressiva"),
        EmoticonEntry("<3", "heart", "corazón", "cœur", "Herz", "cuore")
    )

    private val URL_PATTERN = Pattern.compile("https?://\\S+|ftp://\\S+|www\\.\\S+")
    private val TIME_RATIO_PATTERN = Pattern.compile("\\b\\d+:\\d+(?::\\d+)?\\b")
    private val WIN_PATH_PATTERN = Pattern.compile("\\b[A-Za-z]:[\\\\/]")
    private val SCOPE_PATTERN = Pattern.compile("::")
    private val COMPARISON_PATTERN = Pattern.compile("\\d+\\s*[<>]=?\\s*\\d+|[A-Za-z0-9_]+[<>=]=?\\d+|[A-Za-z0-9_]+\\s+[<>=]=?\\s+\\d+|:=|->|=>")

    fun process(text: String, language: String): String {
        if (text.isEmpty()) return text

        val langIdx = EmojiData.getLanguageIndex(language)

        // Find protected spans
        val protectedSpans = ArrayList<IntArray>()
        fun addMatches(pattern: Pattern) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                protectedSpans.add(intArrayOf(matcher.start(), matcher.end()))
            }
        }

        addMatches(URL_PATTERN)
        addMatches(TIME_RATIO_PATTERN)
        addMatches(WIN_PATH_PATTERN)
        addMatches(SCOPE_PATTERN)
        addMatches(COMPARISON_PATTERN)

        fun isProtected(start: Int, end: Int): Boolean {
            for (span in protectedSpans) {
                if (!(end <= span[0] || start >= span[1])) {
                    return true
                }
            }
            return false
        }

        val sb = StringBuilder(text.length + 32)
        var i = 0

        while (i < text.length) {
            var matched = false

            for (entry in ENTRIES) {
                if (text.startsWith(entry.pattern, i)) {
                    val endI = i + entry.pattern.length

                    if (isProtected(i, endI)) {
                        continue
                    }

                    // Context boundaries:
                    // Preceding character check
                    if (i > 0) {
                        val prevChar = text[i - 1]
                        if (Character.isLetterOrDigit(prevChar) || prevChar == ':' || prevChar == '/' || prevChar == '\\' || prevChar == '<' || prevChar == '>' || prevChar == '@') {
                            continue
                        }
                    }

                    // Following character check
                    if (endI < text.length) {
                        val nextChar = text[endI]
                        if (Character.isLetterOrDigit(nextChar) || nextChar == ':' || nextChar == '/' || nextChar == '\\' || nextChar == '<' || nextChar == '>' || nextChar == '@') {
                            continue
                        }
                    }

                    // Spacing
                    if (sb.isNotEmpty() && Character.isLetterOrDigit(sb[sb.length - 1])) {
                        sb.append(' ')
                    }

                    sb.append(entry.getTranslation(langIdx))

                    if (endI < text.length && Character.isLetterOrDigit(text[endI])) {
                        sb.append(' ')
                    }

                    i = endI
                    matched = true
                    break
                }
            }

            if (!matched) {
                sb.append(text[i])
                i++
            }
        }

        return sb.toString()
    }
}
