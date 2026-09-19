package org.eloqium.tts.engine

import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Robust locale mapping and normalization conforming to standard Android
 * TextToSpeechService conventions and external multilingual engine routers.
 *
 * Mapped to the 8 genuine language modules bundled and compiled in OpenEVV:
 * - en-US (0x00010000)
 * - en-GB (0x00010001)
 * - es-ES (0x00020000)
 * - es-US / es-419 / es-MX (0x00020001)
 * - fr-FR (0x00030000)
 * - fr-CA (0x00030001)
 * - de-DE (0x00040000)
 * - it-IT (0x00050000)
 */
object LocaleMatcher {

    data class LanguageEntry(
        val eciId: Int,
        val iso2Lang: String,
        val iso2Country: String,
        val iso3Lang: String,
        val iso3Country: String,
        val canonicalLocale: Locale,
        val displayName: String,
        val aliasCountries: Set<String> = emptySet()
    ) {
        val bcp47Tag: String get() = "$iso2Lang-$iso2Country"
    }

    val ENTRIES = listOf(
        LanguageEntry(0x00010000, "en", "US", "eng", "USA", Locale("en", "US"), "English (United States)"),
        LanguageEntry(0x00010001, "en", "GB", "eng", "GBR", Locale("en", "GB"), "English (United Kingdom)"),
        LanguageEntry(0x00020000, "es", "ES", "spa", "ESP", Locale("es", "ES"), "Spanish (Spain)"),
        LanguageEntry(0x00020001, "es", "MX", "spa", "MEX", Locale("es", "MX"), "Spanish (Latin America)", 
            setOf("US", "USA", "MX", "MEX", "419", "AR", "ARG", "CO", "COL", "CL", "CHL")),
        LanguageEntry(0x00030000, "fr", "FR", "fra", "FRA", Locale("fr", "FR"), "French (France)"),
        LanguageEntry(0x00030001, "fr", "CA", "fra", "CAN", Locale("fr", "CA"), "French (Canada)"),
        LanguageEntry(0x00040000, "de", "DE", "deu", "DEU", Locale("de", "DE"), "German (Germany)"),
        LanguageEntry(0x00050000, "it", "IT", "ita", "ITA", Locale("it", "IT"), "Italian (Italy)")
    )

    private val BY_ECI = ENTRIES.associateBy { it.eciId }

    fun entryFor(eciId: Int): LanguageEntry? = BY_ECI[eciId]

    fun canonicalLocaleFor(eciId: Int): Locale =
        entryFor(eciId)?.canonicalLocale ?: ENTRIES.first().canonicalLocale

    /**
     * Matches an incoming (language, country, variant) query from TextToSpeechService clients.
     * Supports both 2-letter (ISO 639-1) and 3-letter (ISO 639-2) language/country codes,
     * compound locale strings ("en-US", "en_US", "eng_USA"), and aliases.
     */
    fun matchLanguage(lang: String?, country: String? = null, variant: String? = null): Pair<LanguageEntry, Int>? {
        if (lang.isNullOrBlank()) return null
        var cleanLang = lang.trim().lowercase()
        var cleanCountry = country?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }

        // Handle composite language string (e.g. "en-US", "eng_USA") when country is not passed separately
        if (cleanCountry == null && (cleanLang.contains('-') || cleanLang.contains('_'))) {
            val parts = cleanLang.split('-', '_')
            cleanLang = parts[0]
            cleanCountry = parts.getOrNull(1)?.uppercase()?.takeIf { it.isNotEmpty() }
        }

        // Find candidate languages matching either 2-letter or 3-letter code
        val candidates = ENTRIES.filter {
            it.iso2Lang.equals(cleanLang, ignoreCase = true) ||
            it.iso3Lang.equals(cleanLang, ignoreCase = true)
        }
        if (candidates.isEmpty()) return null

        // If country specified, attempt exact country match or alias match
        if (cleanCountry != null) {
            val countryMatch = candidates.firstOrNull {
                it.iso2Country.equals(cleanCountry, ignoreCase = true) ||
                it.iso3Country.equals(cleanCountry, ignoreCase = true) ||
                it.aliasCountries.contains(cleanCountry)
            }
            if (countryMatch != null) {
                return countryMatch to TextToSpeech.LANG_COUNTRY_AVAILABLE
            }
        }

        // Return primary dialect for language with LANG_AVAILABLE
        return candidates.first() to TextToSpeech.LANG_AVAILABLE
    }

    /**
     * Availability code for onIsLanguageAvailable.
     */
    fun availability(lang: String?, country: String? = null, variant: String? = null): Int {
        val found = matchLanguage(lang, country, variant) ?: return TextToSpeech.LANG_NOT_SUPPORTED
        return found.second
    }
}
