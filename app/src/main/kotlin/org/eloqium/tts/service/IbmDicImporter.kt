package org.eloqium.tts.service

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Detailed report generated during IBM .dic dictionary parsing and import.
 */
data class IbmDicImportReport(
    val filename: String,
    val totalLines: Int,
    val validEntries: Int,
    val emptyLines: Int,
    val malformedLines: Int,
    val pronunciationCount: Int,
    val textCount: Int,
    val prosodyCount: Int,
    val duplicateCount: Int,
    val detectedLanguage: String,
    val recommendedLocale: String,
    val targetLocale: String,
    val dictionaryLayer: String,
    val warnings: List<String>
)

/**
 * Result of parsing an IBM .dic stream or string.
 */
data class IbmDicParseResult(
    val dictionary: UserDictionary,
    val targetLocale: String,
    val report: IbmDicImportReport
)

/**
 * Importer and parser for IBM Eloquence .dic dictionaries (Root, Main, Abbreviation, User).
 *
 * Capabilities:
 * - Robust character encoding detection: UTF-8 with automatic fallback to Windows-1252 / ISO-8859-1.
 * - Automatic source language detection from standard IBM prefix (ENU, ENG, DEU, GER, ESP, SPA, FRA, FRE, ITA).
 * - Automatic layer classification (Root, Main, Abbr, User).
 * - Entry classification: PRONUNCIATION (OpenEVV SPR phonemes) vs. TEXT vs. Prosody/ECI.
 * - Strict phonetic SPR normalization preserving syllable markers, stresses, and tone indices.
 * - Locale compatibility validation and warning generation.
 * - Detailed diagnostic import report.
 */
object IbmDicImporter {

    private val PHONETIC_SPR_REGEX = Regex("""^`?\[[^\]\r\n\t]+\]\.?$""")
    private val ECI_OR_PROSODY_REGEX = Regex("""`[a-z0-9]""")

    /**
     * Detects source language from filename prefix or content.
     * Returns Triple(detectedCode, recommendedLocale, compatibleLocales).
     */
    fun detectLanguage(filename: String): Triple<String, String, Set<String>> {
        val upper = filename.uppercase()
        return when {
            upper.startsWith("ENU") -> Triple("ENU", "en-US", setOf("en-US", "en-GB"))
            upper.startsWith("ENG") -> Triple("ENG", "en-GB", setOf("en-GB", "en-US"))
            upper.startsWith("DEU") || upper.startsWith("GER") -> Triple("DEU", "de-DE", setOf("de-DE"))
            upper.startsWith("ESP") || upper.startsWith("SPA") -> Triple("ESP", "es-ES", setOf("es-ES", "es-US", "es-MX"))
            upper.startsWith("FRA") || upper.startsWith("FRE") -> Triple("FRA", "fr-FR", setOf("fr-FR", "fr-CA"))
            upper.startsWith("ITA") -> Triple("ITA", "it-IT", setOf("it-IT"))
            else -> Triple("UNKNOWN", "en-US", setOf("en-US"))
        }
    }

    /**
     * Detects dictionary layer type from filename.
     */
    fun detectLayer(filename: String): String {
        val lower = filename.lowercase()
        return when {
            lower.contains("root") -> "root"
            lower.contains("main") -> "main"
            lower.contains("abbr") -> "abbr"
            else -> "user"
        }
    }

    /**
     * Validates whether target locale is phonetically compatible with detected language.
     */
    fun isLocaleCompatible(sourceLanguage: String, targetLocale: String): Boolean {
        val cleanSource = sourceLanguage.uppercase()
        val cleanTarget = targetLocale.trim().replace('_', '-')
        val compatible = when {
            cleanSource == "ENU" -> setOf("en-US", "en-GB")
            cleanSource == "ENG" -> setOf("en-GB", "en-US")
            cleanSource == "DEU" || cleanSource == "GER" -> setOf("de-DE")
            cleanSource == "ESP" || cleanSource == "SPA" -> setOf("es-ES", "es-US", "es-MX")
            cleanSource == "FRA" || cleanSource == "FRE" -> setOf("fr-FR", "fr-CA")
            cleanSource == "ITA" -> setOf("it-IT")
            else -> emptySet()
        }
        if (compatible.isEmpty()) return true // Unknown source language, allow user discretion
        return compatible.any { it.equals(cleanTarget, ignoreCase = true) }
    }

    /**
     * Parses an IBM .dic formatted byte array with automatic encoding detection.
     */
    fun parse(
        bytes: ByteArray,
        filename: String = "Imported.dic",
        overrideTargetLocale: String? = null
    ): Result<IbmDicParseResult> {
        // Try UTF-8 first; if malformed replacement characters are encountered, fallback to Windows-1252
        val text = try {
            val utf8 = String(bytes, StandardCharsets.UTF_8)
            if (utf8.contains('\uFFFD')) {
                String(bytes, Charset.forName("windows-1252"))
            } else {
                utf8
            }
        } catch (t: Throwable) {
            String(bytes, StandardCharsets.ISO_8859_1)
        }
        return parseString(text, filename, overrideTargetLocale)
    }

    /**
     * Parses an IBM .dic formatted input stream.
     */
    fun parse(
        stream: InputStream,
        filename: String = "Imported.dic",
        overrideTargetLocale: String? = null
    ): Result<IbmDicParseResult> {
        val bytes = stream.readBytes()
        return parse(bytes, filename, overrideTargetLocale)
    }

    /**
     * Parses an IBM .dic formatted string.
     */
    fun parseString(
        content: String,
        filename: String = "Imported.dic",
        overrideTargetLocale: String? = null
    ): Result<IbmDicParseResult> {
        return try {
            val (detectedLang, defaultLocale, compatibleLocales) = detectLanguage(filename)
            val effectiveTargetLocale = overrideTargetLocale?.trim().takeUnless { it.isNullOrEmpty() } ?: defaultLocale
            val layer = detectLayer(filename)

            val warnings = mutableListOf<String>()
            if (!isLocaleCompatible(detectedLang, effectiveTargetLocale)) {
                warnings.add(
                    "Dictionary source language ($detectedLang) may not be phonetically compatible with target locale ($effectiveTargetLocale)."
                )
            }

            var totalLines = 0
            var emptyLines = 0
            var malformedLines = 0
            var pronunciationCount = 0
            var textCount = 0
            var prosodyCount = 0
            var duplicateCount = 0

            // LinkedHashMap maintains insertion order while tracking duplicates
            val entriesMap = LinkedHashMap<String, UserDictionaryEntry>()

            content.lineSequence().forEach { rawLine ->
                totalLines++
                val line = rawLine.trimEnd('\r', '\n')
                if (line.isBlank()) {
                    emptyLines++
                    return@forEach
                }

                val parts = line.split('\t')
                if (parts.size < 2) {
                    malformedLines++
                    return@forEach
                }

                val source = parts[0].trim()
                val rawVal = parts.drop(1).joinToString("\t").trim()
                if (source.isEmpty()) {
                    malformedLines++
                    return@forEach
                }

                // Check if phonetic SPR
                val isPhonetic = PHONETIC_SPR_REGEX.matches(rawVal)

                val (entryType, cleanReplacement) = if (isPhonetic) {
                    pronunciationCount++
                    val spr = rawVal
                        .removePrefix("`")
                        .removePrefix("[")
                        .removeSuffix(".")
                        .removeSuffix("]")
                        .trim()
                    DictionaryEntryType.PRONUNCIATION to spr
                } else {
                    if (ECI_OR_PROSODY_REGEX.containsMatchIn(rawVal)) {
                        prosodyCount++
                    } else {
                        textCount++
                    }
                    DictionaryEntryType.TEXT to rawVal
                }

                if (entriesMap.containsKey(source)) {
                    duplicateCount++
                }

                entriesMap[source] = UserDictionaryEntry(
                    id = UUID.randomUUID().toString(),
                    source = source,
                    replacement = cleanReplacement,
                    matchMode = MatchMode.EXACT,
                    caseSensitive = true,
                    type = entryType
                )
            }

            val validEntries = entriesMap.size
            val dictionaryName = filename
                .substringBeforeLast('.')
                .replace('_', ' ')
                .ifBlank { "Imported Dictionary" }

            val provenance = DictionaryProvenance(
                format = "ibm_dic",
                originalFilename = filename,
                sourceLanguage = detectedLang,
                targetLocale = effectiveTargetLocale,
                dictionaryLayer = layer,
                importDate = System.currentTimeMillis()
            )

            val dictionary = UserDictionary(
                id = UUID.randomUUID().toString(),
                name = dictionaryName,
                enabled = true,
                entries = entriesMap.values.toList(),
                provenance = provenance
            )

            val report = IbmDicImportReport(
                filename = filename,
                totalLines = totalLines,
                validEntries = validEntries,
                emptyLines = emptyLines,
                malformedLines = malformedLines,
                pronunciationCount = pronunciationCount,
                textCount = textCount,
                prosodyCount = prosodyCount,
                duplicateCount = duplicateCount,
                detectedLanguage = detectedLang,
                recommendedLocale = defaultLocale,
                targetLocale = effectiveTargetLocale,
                dictionaryLayer = layer,
                warnings = warnings
            )

            Result.success(IbmDicParseResult(dictionary, effectiveTargetLocale, report))
        } catch (t: Throwable) {
            Result.failure(IllegalArgumentException("Failed to parse IBM .dic file: ${t.message}", t))
        }
    }
}
