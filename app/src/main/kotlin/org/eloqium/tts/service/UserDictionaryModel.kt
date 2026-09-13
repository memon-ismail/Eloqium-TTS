package org.eloqium.tts.service

import java.util.UUID

/**
 * Match modes for user dictionary entries.
 */
enum class MatchMode(val displayName: String) {
    EXACT("Exact match"),
    STARTS_WITH("Starts with"),
    ENDS_WITH("Ends with"),
    CONTAINS("Contains");

    companion object {
        val ALL_MODES = listOf(EXACT, STARTS_WITH, ENDS_WITH, CONTAINS)

        fun fromString(value: String?): MatchMode {
            if (value == null) return EXACT
            val trimmed = value.trim()
            return entries.firstOrNull {
                it.displayName.equals(trimmed, ignoreCase = true) ||
                it.name.equals(trimmed, ignoreCase = true)
            } ?: EXACT
        }
    }
}

/**
 * Individual word or phrase replacement entry.
 */
data class UserDictionaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val source: String,
    val replacement: String,
    val matchMode: MatchMode = MatchMode.EXACT,
    val caseSensitive: Boolean = false
)

/**
 * A named dictionary containing entries for a language.
 */
data class UserDictionary(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "User Dictionary",
    val enabled: Boolean = true,
    val entries: List<UserDictionaryEntry> = emptyList()
)

/**
 * Container of dictionaries belonging to a specific language dialect (e.g. en-US).
 */
data class LanguageDictionaries(
    val languageTag: String,
    val dictionaries: List<UserDictionary> = emptyList()
)

/**
 * Complete root data model for the User Dictionary subsystem.
 */
data class UserDictionarySystem(
    val schemaVersion: Int = 1,
    val languages: List<LanguageDictionaries> = emptyList()
)

/**
 * Parsed representation of a standalone dictionary file for import.
 */
data class ParsedDictionary(
    val schemaVersion: Int,
    val languageTag: String,
    val dictionary: UserDictionary
)

/**
 * Self-contained, pure-Kotlin JSON parser and serializer with schema validation.
 * Zero external dependencies, fully compatible with JVM tests and Android runtime.
 */
object UserDictionaryJson {

    const val CURRENT_SCHEMA_VERSION = 1

    fun exportDictionary(dict: UserDictionary, languageTag: String): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"schemaVersion\": ").append(CURRENT_SCHEMA_VERSION).append(",\n")
        sb.append("  \"language\": \"").append(escape(languageTag)).append("\",\n")
        sb.append("  \"name\": \"").append(escape(dict.name)).append("\",\n")
        sb.append("  \"enabled\": ").append(dict.enabled).append(",\n")
        sb.append("  \"entries\": [\n")
        dict.entries.forEachIndexed { index, entry ->
            sb.append("    {\n")
            sb.append("      \"source\": \"").append(escape(entry.source)).append("\",\n")
            sb.append("      \"replacement\": \"").append(escape(entry.replacement)).append("\",\n")
            sb.append("      \"matchMode\": \"").append(escape(entry.matchMode.displayName)).append("\",\n")
            sb.append("      \"caseSensitive\": ").append(entry.caseSensitive).append("\n")
            sb.append("    }")
            if (index < dict.entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    fun exportSystem(system: UserDictionarySystem): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"schemaVersion\": ").append(system.schemaVersion).append(",\n")
        sb.append("  \"languages\": [\n")
        system.languages.forEachIndexed { lIdx, lang ->
            sb.append("    {\n")
            sb.append("      \"languageTag\": \"").append(escape(lang.languageTag)).append("\",\n")
            sb.append("      \"dictionaries\": [\n")
            lang.dictionaries.forEachIndexed { dIdx, dict ->
                sb.append("        {\n")
                sb.append("          \"id\": \"").append(escape(dict.id)).append("\",\n")
                sb.append("          \"name\": \"").append(escape(dict.name)).append("\",\n")
                sb.append("          \"enabled\": ").append(dict.enabled).append(",\n")
                sb.append("          \"entries\": [\n")
                dict.entries.forEachIndexed { eIdx, entry ->
                    sb.append("            {\n")
                    sb.append("              \"id\": \"").append(escape(entry.id)).append("\",\n")
                    sb.append("              \"source\": \"").append(escape(entry.source)).append("\",\n")
                    sb.append("              \"replacement\": \"").append(escape(entry.replacement)).append("\",\n")
                    sb.append("              \"matchMode\": \"").append(escape(entry.matchMode.displayName)).append("\",\n")
                    sb.append("              \"caseSensitive\": ").append(entry.caseSensitive).append("\n")
                    sb.append("            }")
                    if (eIdx < dict.entries.size - 1) sb.append(",")
                    sb.append("\n")
                }
                sb.append("          ]\n")
                sb.append("        }")
                if (dIdx < lang.dictionaries.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("      ]\n")
            sb.append("    }")
            if (lIdx < system.languages.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    fun parseDictionary(jsonString: String): Result<ParsedDictionary> {
        return try {
            val root = parseElement(jsonString) as? Map<*, *>
                ?: return Result.failure(IllegalArgumentException("Invalid JSON: root must be an object"))

            val schemaVerAny = root["schemaVersion"]
                ?: return Result.failure(IllegalArgumentException("Missing required field: schemaVersion"))
            val schemaVersion = when (schemaVerAny) {
                is Number -> schemaVerAny.toInt()
                is String -> schemaVerAny.toIntOrNull()
                else -> null
            } ?: return Result.failure(IllegalArgumentException("Invalid schemaVersion: expected integer"))

            if (schemaVersion != CURRENT_SCHEMA_VERSION) {
                return Result.failure(IllegalArgumentException(
                    "Unsupported schema version: $schemaVersion (supported: $CURRENT_SCHEMA_VERSION)"
                ))
            }

            val language = root["language"]?.toString()?.trim()
            if (language.isNullOrEmpty()) {
                return Result.failure(IllegalArgumentException("Missing required field: language"))
            }

            val name = root["name"]?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: "Imported Dictionary"
            val enabled = root["enabled"] as? Boolean ?: true

            val entriesList = mutableListOf<UserDictionaryEntry>()
            val rawEntries = root["entries"] as? List<*>
            if (rawEntries != null) {
                for (item in rawEntries) {
                    val entryMap = item as? Map<*, *> ?: continue
                    val source = entryMap["source"]?.toString() ?: ""
                    if (source.isEmpty()) continue
                    val replacement = entryMap["replacement"]?.toString() ?: ""
                    val matchMode = MatchMode.fromString(entryMap["matchMode"]?.toString())
                    val caseSensitive = entryMap["caseSensitive"] as? Boolean ?: false

                    entriesList.add(
                        UserDictionaryEntry(
                            id = entryMap["id"]?.toString() ?: UUID.randomUUID().toString(),
                            source = source,
                            replacement = replacement,
                            matchMode = matchMode,
                            caseSensitive = caseSensitive
                        )
                    )
                }
            }

            val dict = UserDictionary(
                id = root["id"]?.toString() ?: UUID.randomUUID().toString(),
                name = name,
                enabled = enabled,
                entries = entriesList
            )

            Result.success(ParsedDictionary(schemaVersion, language, dict))
        } catch (t: Throwable) {
            Result.failure(IllegalArgumentException("Failed to parse dictionary JSON: ${t.message}", t))
        }
    }

    fun parseSystem(jsonString: String): UserDictionarySystem {
        if (jsonString.isBlank()) return UserDictionarySystem()
        return try {
            val root = parseElement(jsonString) as? Map<*, *> ?: return UserDictionarySystem()
            val schemaVersion = (root["schemaVersion"] as? Number)?.toInt() ?: CURRENT_SCHEMA_VERSION
            val rawLanguages = root["languages"] as? List<*> ?: return UserDictionarySystem(schemaVersion)

            val languages = mutableListOf<LanguageDictionaries>()
            for (langItem in rawLanguages) {
                val langMap = langItem as? Map<*, *> ?: continue
                val langTag = langMap["languageTag"]?.toString()?.trim() ?: continue
                if (langTag.isEmpty()) continue

                val dicts = mutableListOf<UserDictionary>()
                val rawDicts = langMap["dictionaries"] as? List<*>
                if (rawDicts != null) {
                    for (dictItem in rawDicts) {
                        val dictMap = dictItem as? Map<*, *> ?: continue
                        val name = dictMap["name"]?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: "User Dictionary"
                        val enabled = dictMap["enabled"] as? Boolean ?: true
                        val id = dictMap["id"]?.toString() ?: UUID.randomUUID().toString()

                        val entries = mutableListOf<UserDictionaryEntry>()
                        val rawEntries = dictMap["entries"] as? List<*>
                        if (rawEntries != null) {
                            for (eItem in rawEntries) {
                                val eMap = eItem as? Map<*, *> ?: continue
                                val source = eMap["source"]?.toString() ?: ""
                                if (source.isEmpty()) continue
                                val replacement = eMap["replacement"]?.toString() ?: ""
                                val matchMode = MatchMode.fromString(eMap["matchMode"]?.toString())
                                val caseSensitive = eMap["caseSensitive"] as? Boolean ?: false
                                val entryId = eMap["id"]?.toString() ?: UUID.randomUUID().toString()

                                entries.add(
                                    UserDictionaryEntry(
                                        id = entryId,
                                        source = source,
                                        replacement = replacement,
                                        matchMode = matchMode,
                                        caseSensitive = caseSensitive
                                    )
                                )
                            }
                        }

                        dicts.add(UserDictionary(id = id, name = name, enabled = enabled, entries = entries))
                    }
                }

                if (dicts.isNotEmpty()) {
                    languages.add(LanguageDictionaries(languageTag = langTag, dictionaries = dicts))
                }
            }

            UserDictionarySystem(schemaVersion = schemaVersion, languages = languages)
        } catch (t: Throwable) {
            UserDictionarySystem()
        }
    }

    // --- Lightweight Pure-Kotlin JSON Parser Implementation ---

    private fun escape(s: String): String {
        val out = StringBuilder()
        for (c in s) {
            when (c) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> {
                    if (c.code < 0x20) {
                        out.append(String.format("\\u%04x", c.code))
                    } else {
                        out.append(c)
                    }
                }
            }
        }
        return out.toString()
    }

    private fun parseElement(json: String): Any? {
        val parser = SimpleJsonParser(json.trim())
        return parser.parseValue()
    }

    private class SimpleJsonParser(private val src: String) {
        private var pos = 0

        fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= src.length) return null
            return when (src[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else -> parseNumber()
            }
        }

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) {
                pos++
            }
        }

        private fun parseObject(): Map<String, Any?> {
            val map = LinkedHashMap<String, Any?>()
            pos++ // skip '{'
            skipWhitespace()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWhitespace()
                if (src[pos] != '"') throw IllegalArgumentException("Expected string key in object at $pos")
                val key = parseString()
                skipWhitespace()
                if (pos >= src.length || src[pos] != ':') throw IllegalArgumentException("Expected ':' at $pos")
                pos++ // skip ':'
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                    continue
                } else if (pos < src.length && src[pos] == '}') {
                    pos++
                    break
                } else {
                    throw IllegalArgumentException("Expected ',' or '}' in object at $pos")
                }
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            val list = ArrayList<Any?>()
            pos++ // skip '['
            skipWhitespace()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                val value = parseValue()
                list.add(value)
                skipWhitespace()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                    continue
                } else if (pos < src.length && src[pos] == ']') {
                    pos++
                    break
                } else {
                    throw IllegalArgumentException("Expected ',' or ']' in array at $pos")
                }
            }
            return list
        }

        private fun parseString(): String {
            pos++ // skip leading '"'
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                if (c == '"') return sb.toString()
                if (c == '\\') {
                    if (pos >= src.length) break
                    when (val esc = src[pos++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (pos + 4 <= src.length) {
                                val hex = src.substring(pos, pos + 4)
                                pos += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            throw IllegalArgumentException("Unterminated string literal")
        }

        private fun parseBoolean(): Boolean {
            if (src.startsWith("true", pos)) {
                pos += 4
                return true
            }
            if (src.startsWith("false", pos)) {
                pos += 5
                return false
            }
            throw IllegalArgumentException("Invalid boolean literal at $pos")
        }

        private fun parseNull(): Any? {
            if (src.startsWith("null", pos)) {
                pos += 4
                return null
            }
            throw IllegalArgumentException("Invalid null literal at $pos")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (pos < src.length && (src[pos] == '-' || src[pos] == '+')) pos++
            while (pos < src.length && (src[pos].isDigit() || src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E' || src[pos] == '-' || src[pos] == '+')) {
                pos++
            }
            val numStr = src.substring(start, pos)
            return if (numStr.contains('.') || numStr.contains('e') || numStr.contains('E')) {
                numStr.toDouble()
            } else {
                numStr.toLongOrNull()?.let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it } ?: numStr.toDouble()
            }
        }
    }
}
