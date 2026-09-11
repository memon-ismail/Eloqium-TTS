package org.eloqium.tts.engine

import android.speech.tts.Voice
import java.util.Locale

/**
 * Registry of all 64 genuine voices (8 language dialects x 8 presets).
 * Voice names use the canonical format: "{iso3Lang}-{iso3Country}-{presetName}" (e.g. "eng-USA-Reed").
 *
 * Robust multi-alias resolution supports:
 * - Canonical names: "eng-USA-Reed", "eng-GBR-Shelley", etc.
 * - Lowercase / underscore variants: "eng_usa_reed"
 * - Eloqium legacy names: "eloqium_en_us_reed"
 * - Presets only: "Reed", "Shelley", "Bobby", "Rocko", "Glen", "Sandy", "Grandma", "Grandpa"
 * - Router defaults: "*Default", "default", "", null
 * - Locale strings: "en", "eng", "en-US", "eng-USA", etc.
 */
object VoiceRegistry {

    data class VoiceDescriptor(
        val name: String,
        val entry: LocaleMatcher.LanguageEntry,
        val presetId: Int,
        val presetName: String,
        val voice: Voice
    )

    val ALL_VOICES: List<VoiceDescriptor> = buildList {
        for (entry in LocaleMatcher.ENTRIES) {
            for ((presetId, presetName) in Eci.PRESET_NAMES.withIndex()) {
                val voiceName = "${entry.iso3Lang}-${entry.iso3Country}-$presetName"
                val voice = Voice(
                    voiceName,
                    entry.canonicalLocale,
                    Voice.QUALITY_NORMAL,
                    Voice.LATENCY_VERY_LOW,
                    false, // requiresNetwork = false
                    emptySet()
                )
                add(VoiceDescriptor(voiceName, entry, presetId, presetName, voice))
            }
        }
    }

    val DEFAULT_VOICE: VoiceDescriptor = ALL_VOICES.first()

    // Map of normalized name keys to VoiceDescriptor for fast lookup
    private val LOOKUP_MAP: Map<String, VoiceDescriptor> = buildMap {
        for (desc in ALL_VOICES) {
            // Canonical: "eng-usa-reed"
            put(desc.name.lowercase(), desc)
            put(desc.name.lowercase().replace('-', '_'), desc)

            // Eloqium format: "eloqium_en_us_reed"
            val eloqiumName = "eloqium_${desc.entry.iso2Lang}_${desc.entry.iso2Country.lowercase()}_${desc.presetName.lowercase()}"
            put(eloqiumName, desc)
            put(eloqiumName.replace('_', '-'), desc)

            // Short codes: "eng-us-reed", "en-us-reed"
            put("${desc.entry.iso3Lang}-${desc.entry.iso2Country}-${desc.presetName}".lowercase(), desc)
            put("${desc.entry.iso2Lang}-${desc.entry.iso2Country}-${desc.presetName}".lowercase(), desc)
        }
    }

    fun findDefaultVoiceFor(entry: LocaleMatcher.LanguageEntry, presetId: Int = 0): VoiceDescriptor =
        ALL_VOICES.firstOrNull { it.entry.eciId == entry.eciId && it.presetId == presetId }
            ?: ALL_VOICES.firstOrNull { it.entry.eciId == entry.eciId }
            ?: DEFAULT_VOICE

    fun findDefaultVoiceFor(localeStr: String, presetId: Int = 0): VoiceDescriptor? {
        val match = LocaleMatcher.matchLanguage(localeStr) ?: return null
        return findDefaultVoiceFor(match.first, presetId)
    }

    fun findVoice(name: String?): VoiceDescriptor? {
        if (name.isNullOrBlank()) return DEFAULT_VOICE
        val clean = name.trim().lowercase()

        // Router defaults / wildcards
        if (clean == "*default" || clean == "default" || clean == "*") {
            return DEFAULT_VOICE
        }

        // Direct / canonical / alias lookup
        LOOKUP_MAP[clean]?.let { return it }

        // Preset name only: "Reed", "Shelley", etc. (case-insensitive)
        for (desc in ALL_VOICES) {
            if (desc.presetName.equals(clean, ignoreCase = true)) {
                return desc
            }
        }

        // Locale match: if name is a language/locale query (e.g. "en", "eng", "en_US", "fra")
        val matchedEntry = LocaleMatcher.matchLanguage(clean)
        if (matchedEntry != null) {
            return findDefaultVoiceFor(matchedEntry.first)
        }

        return null
    }

    /**
     * Returns default voice name for the requested language and preset.
     * Returns null if language is not supported, preventing
     * routers from misrouting unsupported languages to Eloqium.
     */
    fun getDefaultVoiceNameFor(
        lang: String?,
        country: String? = null,
        variant: String? = null,
        presetId: Int = 0
    ): String? {
        val match = LocaleMatcher.matchLanguage(lang, country, variant) ?: return null
        val presetName = Eci.PRESET_NAMES.getOrElse(presetId.coerceIn(0, 7)) { "Reed" }
        return "${match.first.iso3Lang}-${match.first.iso3Country}-$presetName"
    }
}
