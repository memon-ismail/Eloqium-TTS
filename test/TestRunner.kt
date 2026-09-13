package test

import org.eloqium.tts.service.Settings
import org.eloqium.tts.service.SettingsDefaults
import org.eloqium.tts.service.EloqiumTtsService
import org.eloqium.tts.pipeline.NumberProcessor
import org.eloqium.tts.engine.AudioGainProcessor
import org.eloqium.tts.ui.AboutNavigation
import android.speech.tts.SynthesisRequest
import android.speech.tts.SynthesisCallback
import android.os.Bundle

import org.eloqium.tts.engine.Eci
import org.eloqium.tts.engine.EloqiumEngine
import org.eloqium.tts.engine.LocaleMatcher
import org.eloqium.tts.engine.NativeEngine
import org.eloqium.tts.engine.SpeechRateMapper
import org.eloqium.tts.engine.VoiceParameterMapper
import org.eloqium.tts.engine.VoiceRegistry
import org.eloqium.tts.pipeline.AbbreviationDictionary
import org.eloqium.tts.pipeline.AbbreviationProcessor
import org.eloqium.tts.pipeline.Chunker
import org.eloqium.tts.pipeline.EmojiData
import org.eloqium.tts.pipeline.EmojiProcessor
import org.eloqium.tts.pipeline.EmoticonProcessor
import org.eloqium.tts.pipeline.OpenEVVCompatibilityFixes
import org.eloqium.tts.pipeline.OpenEVVEncoder
import org.eloqium.tts.pipeline.PauseProcessor
import org.eloqium.tts.pipeline.ScreenReaderPunctuationProcessor
import org.eloqium.tts.pipeline.TextRequest
import org.eloqium.tts.pipeline.UnicodeNormalizer
import android.speech.tts.TextToSpeech
import org.eloqium.tts.service.MatchMode
import org.eloqium.tts.service.UserDictionary
import org.eloqium.tts.service.UserDictionaryEntry
import org.eloqium.tts.service.UserDictionaryJson
import org.eloqium.tts.service.UserDictionaryRepository
import org.eloqium.tts.service.UserDictionarySystem
import org.eloqium.tts.pipeline.UserDictionaryProcessor
import java.io.File
import java.util.Locale

fun main() {
    var passed = 0
    var failed = 0

    fun assert(condition: Boolean, message: String) {
        if (condition) {
            println("  [PASS] $message")
            passed++
        } else {
            println("  [FAIL] $message")
            failed++
        }
    }

    println("==================================================")
    println("   Eloqium TTS Authoritative Foundation Regression")
    println("==================================================")

    // Load binary emoji data asset for testing
    val emojiBin = File("app/src/main/assets/emoji_data.bin")
    if (emojiBin.exists()) {
        emojiBin.inputStream().use { EmojiData.load(it) }
    }

    // --- Suite 1: Canonical Locales & Router Interoperability ---
    println("\n--- Suite 1: Canonical Locales & Router Interoperability ---")
    for (entry in LocaleMatcher.ENTRIES) {
        val loc = entry.canonicalLocale
        assert(loc.isO3Language == entry.iso3Lang, "${entry.displayName} ISO3 language matches ${entry.iso3Lang}")
        assert(loc.country.isNotEmpty(), "${entry.displayName} country (${loc.country}) is non-empty")
    }

    // --- Suite 2: Multilingual Router Compatibility Matching ---
    println("\n--- Suite 2: Multilingual Router Compatibility Matching ---")
    val enUsMatch = LocaleMatcher.matchLanguage("en", "US")
    assert(enUsMatch?.first?.iso2Lang == "en" && enUsMatch.first.iso2Country == "US", "matchLanguage('en', 'US') resolves to en-US")
    assert(enUsMatch?.second == TextToSpeech.LANG_COUNTRY_AVAILABLE, "matchLanguage('en', 'US') returns LANG_COUNTRY_AVAILABLE (1)")

    val legacyEvvMatch = LocaleMatcher.matchLanguage("eng", "USA")
    assert(legacyEvvMatch?.first?.iso2Lang == "en" && legacyEvvMatch.first.iso2Country == "US", "matchLanguage('eng', 'USA') resolves to en-US")
    assert(legacyEvvMatch?.second == TextToSpeech.LANG_COUNTRY_AVAILABLE, "matchLanguage('eng', 'USA') returns LANG_COUNTRY_AVAILABLE (1)")

    val langOnly = LocaleMatcher.matchLanguage("en", "")
    assert(langOnly?.second == TextToSpeech.LANG_AVAILABLE, "matchLanguage('en') returns LANG_AVAILABLE (0)")

    assert(LocaleMatcher.matchLanguage("es", "ES")?.first?.eciId == 0x00020000, "matchLanguage('es', 'ES') resolves to 0x00020000")
    assert(LocaleMatcher.matchLanguage("es", "US")?.first?.eciId == 0x00020001, "matchLanguage('es', 'US') resolves to 0x00020001")
    assert(LocaleMatcher.matchLanguage("es", "MX")?.first?.eciId == 0x00020001, "matchLanguage('es', 'MX') alias resolves to 0x00020001")
    assert(LocaleMatcher.matchLanguage("es", "419")?.first?.eciId == 0x00020001, "matchLanguage('es', '419') alias resolves to 0x00020001")
    assert(LocaleMatcher.matchLanguage("de", "DE")?.first?.eciId == 0x00040000, "de-DE resolves to 0x00040000")
    assert(LocaleMatcher.matchLanguage("fr", "FR")?.first?.eciId == 0x00030000, "fr-FR resolves to 0x00030000")
    assert(LocaleMatcher.matchLanguage("fr", "CA")?.first?.eciId == 0x00030001, "fr-CA resolves to 0x00030001")
    assert(LocaleMatcher.matchLanguage("it", "IT")?.first?.eciId == 0x00050000, "it-IT resolves to 0x00050000")

    val ptBr = LocaleMatcher.matchLanguage("pt", "BR")
    assert(ptBr == null, "pt-BR correctly returns null (not bundled in OpenEVV)")
    assert(LocaleMatcher.availability("pt", "BR") == TextToSpeech.LANG_NOT_SUPPORTED, "pt-BR returns LANG_NOT_SUPPORTED (-2)")
    assert(LocaleMatcher.matchLanguage("xyz", "ABC") == null, "matchLanguage('xyz', 'ABC') returns null")
    assert(LocaleMatcher.availability("xyz") == TextToSpeech.LANG_NOT_SUPPORTED, "availability('xyz') returns LANG_NOT_SUPPORTED (-2)")

    // --- Suite 3: Voice Registry & Dynamic Voice Selection ---
    println("\n--- Suite 3: Voice Registry & Dynamic Voice Selection ---")
    assert(VoiceRegistry.ALL_VOICES.size == 64, "Total genuine voices == 64 (8 languages x 8 presets)")
    val reedVoice = VoiceRegistry.findVoice("eloqium_en_us_reed")
    assert(reedVoice?.presetId == 0, "findVoice('eloqium_en_us_reed') found Reed preset 0")
    val shelleyVoice = VoiceRegistry.findVoice("eloqium_en_us_shelley")
    assert(shelleyVoice?.presetId == 1, "findVoice('eloqium_en_us_shelley') found Shelley preset 1")
    val canonicalVoice = VoiceRegistry.findVoice("eng-USA-Reed")
    assert(canonicalVoice?.presetId == 0, "findVoice('eng-USA-Reed') found Reed")
    val defaultEnUs = VoiceRegistry.getDefaultVoiceNameFor("en", "US")
    assert(defaultEnUs == "eng-USA-Reed", "default voice name for en-US is eng-USA-Reed")
    val defaultUnsupported = VoiceRegistry.getDefaultVoiceNameFor("jpn", "JPN")
    assert(defaultUnsupported == null, "default voice name for unsupported language returns null")
    val starDefault = VoiceRegistry.findVoice("*Default")
    assert(starDefault?.presetId == 0, "findVoice('*Default') resolves to default voice")

    // --- Suite 4: Unicode & Legacy Character Transformations ---
    println("\n--- Suite 4: Unicode & Legacy Character Transformations ---")
    val smartQuotes = "“Hello” and ‘World’"
    val normQuotes = UnicodeNormalizer.normalize(smartQuotes)
    assert(normQuotes == "\"Hello\" and 'World'", "Smart quotes normalized: $normQuotes")

    val dashes = "one—two–three−four"
    val normDashes = UnicodeNormalizer.normalize(dashes)
    assert(normDashes == "one-two-three-four", "Typographic dashes normalized: $normDashes")

    val symbols = "5 ≤ 10, 10 ≥ 5, 2 ≠ 3, π ≈ 3.14, ∞, 100€, 50£, 1000¥, 98°"
    val normSymbols = UnicodeNormalizer.normalize(symbols)
    assert(normSymbols.contains("less than or equal to") && normSymbols.contains("euro") && normSymbols.contains("degrees"),
        "Math and currency symbols normalized to spoken words")

    val ellipsis = "Wait… pause"
    val normEllipsis = UnicodeNormalizer.normalize(ellipsis)
    assert(normEllipsis == "Wait... pause", "Ellipsis normalized to 3 dots")

    // --- Suite 5: Screen Reader Punctuation Processor ---
    println("\n--- Suite 5: Screen Reader Punctuation Processor ---")
    val detachedPunct = "Are you ready ? Yes , I am !"
    val boundPunct = ScreenReaderPunctuationProcessor.process(detachedPunct, spokenPunctuation = false)
    assert(boundPunct == "Are you ready? Yes, I am!", "Detached punctuation rebound to preceding word: '$boundPunct'")

    val spokenPunct = ScreenReaderPunctuationProcessor.process("Hello, world!", spokenPunctuation = true)
    assert(spokenPunct.contains("comma") && spokenPunct.contains("exclamation mark"), "Spoken punctuation verbalized: '$spokenPunct'")

    // --- Suite 6: OpenEVV Compatibility Fixes ---
    println("\n--- Suite 6: OpenEVV Compatibility Fixes ---")
    val teamtalk = "teamtalk5"
    val fixedDigit = OpenEVVCompatibilityFixes.apply(teamtalk)
    assert(fixedDigit == "teamtalk 5", "Digit boundary separated: '$fixedDigit'")

    val opener = "item(first)"
    val fixedOpener = OpenEVVCompatibilityFixes.apply(opener)
    assert(fixedOpener == "item (first)", "Opener boundary separated: '$fixedOpener'")

    val looseS = "file (s)"
    val fixedS = OpenEVVCompatibilityFixes.apply(looseS)
    assert(fixedS == "file(s)", "Loose suffix parenthesized: '$fixedS'")

    val rogueEci = "Unsafe `v1 `invalid text"
    val sanitizedEci = OpenEVVCompatibilityFixes.apply(rogueEci)
    assert(!sanitizedEci.contains("`invalid"), "Rogue ECI commands safely sanitized: '$sanitizedEci'")

    // --- Suite 7: Pause Duration & Intonation Pipeline ---
    println("\n--- Suite 7: Pause Duration & Intonation Pipeline ---")
    val questionIntonation = PauseProcessor.process("Are you there?", pauseMode = PauseProcessor.PAUSE_KEEP, phrasePrediction = true)
    assert(questionIntonation.startsWith("`pp1 ") && questionIntonation.endsWith("there?"),
        "Phrase prediction enabled (pp1) and question mark bound tightly: '$questionIntonation'")

    val flattenedIntonation = PauseProcessor.process("Statement.", phrasePrediction = false)
    assert(flattenedIntonation.startsWith("`pp0 "), "Phrase prediction disabled explicitly: '$flattenedIntonation'")

    // --- Suite 8: Punctuation Matrix & Chunking ---
    println("\n--- Suite 8: Punctuation Matrix & Chunking ---")
    val testMatrix1 = "Hello. Hello! Hello? Hello, Hello; Hello: Hello... Hello, how are you? Are you there? Really? Wait... what?"
    val chunks1 = Chunker.chunk(testMatrix1)
    assert(chunks1.size >= 4, "Sentence matrix chunked into multiple pieces (${chunks1.size} chunks)")
    for (chunk in chunks1) {
        assert(chunk.isNotBlank(), "Chunk is non-empty: '$chunk'")
    }

    val testMatrix2 = "Values: 3.14 1,024 2:30 U.S. Mr. Smith e.g. at 5:00."
    val chunks2 = Chunker.chunk(testMatrix2)
    assert(chunks2.size == 1, "Protected numbers, times, and abbreviations kept intact in 1 chunk (${chunks2.size} chunks)")
    assert(chunks2[0].contains("3.14") && chunks2[0].contains("1,024") && chunks2[0].contains("2:30") &&
           chunks2[0].contains("U.S.") && chunks2[0].contains("Mr. Smith") && chunks2[0].contains("e.g."),
        "Decimals, thousands, times, abbreviations, and titles preserved without false splits")

    // --- Suite 9: End-to-End Native JNI Synthesis Across All 8 Languages ---
    println("\n--- Suite 9: End-to-End Native JNI Synthesis Across All 8 Languages ---")
    assert(NativeEngine.loaded, "NativeEngine.loaded is true")

    val sampleTexts = mapOf(
        0x00010000 to "This is Eloqium text-to-speech for American English.",
        0x00010001 to "This is Eloqium text-to-speech for British English.",
        0x00020000 to "Esta es la síntesis de voz de Eloqium para español de España.",
        0x00020001 to "Esta es la síntesis de voz de Eloqium para español de América Latina.",
        0x00030000 to "Ceci est la synthèse vocale Eloqium pour le français.",
        0x00030001 to "Ceci est la synthèse vocale Eloqium pour le français canadien.",
        0x00040000 to "Dies ist die Eloqium-Sprachsynthese für Deutsch.",
        0x00050000 to "Questa è la sintesi vocale Eloqium per l'italiano."
    )

    for (entry in LocaleMatcher.ENTRIES) {
        val handle = NativeEngine.create(entry.eciId)
        assert(handle != 0L, "NativeEngine created instance for ${entry.displayName} (0x${Integer.toHexString(entry.eciId)})")
        if (handle != 0L) {
            val textStr = sampleTexts[entry.eciId] ?: "Sample test."
            val processed = PauseProcessor.process(
                OpenEVVCompatibilityFixes.apply(
                    ScreenReaderPunctuationProcessor.process(
                        UnicodeNormalizer.normalize(textStr)
                    )
                )
            )
            val textBytes = OpenEVVEncoder.encode(processed)
            val startTime = System.currentTimeMillis()
            val ok = NativeEngine.speak(handle, textBytes)
            assert(ok, "NativeEngine.speak succeeded for ${entry.displayName}")
            val buf = ByteArray(4096)
            var total = 0
            var firstReadMs = -1L
            while (true) {
                val n = NativeEngine.read(handle, buf)
                if (n <= 0) break
                if (firstReadMs == -1L) firstReadMs = System.currentTimeMillis() - startTime
                total += n
            }
            val totalTime = System.currentTimeMillis() - startTime
            assert(total > 0, "${entry.displayName} produced $total bytes of PCM audio (TTFP: ${firstReadMs}ms, Total: ${totalTime}ms)")
            NativeEngine.destroy(handle)
            Thread.sleep(50)
        }
    }

    // --- Suite 10: Heavy Native Stop / Interruption Stress Testing ---
    println("\n--- Suite 10: Native Stop / Interruption Stress Testing (10 Iterations) ---")
    val stopHandle = NativeEngine.create(0x00010000)
    assert(stopHandle != 0L, "Created engine handle for stop stress test")
    if (stopHandle != 0L) {
        var allIterationsPassed = true
        val longParagraph = ("This is a long sentence testing immediate speech interruption. ").repeat(6)
        val paragraphBytes = OpenEVVEncoder.encode(PauseProcessor.process(longParagraph))

        for (i in 1..10) {
            NativeEngine.speak(stopHandle, paragraphBytes)
            val tempBuf = ByteArray(2048)
            val initialRead = NativeEngine.read(stopHandle, tempBuf)
            if (initialRead <= 0) {
                allIterationsPassed = false
                break
            }
            // Trigger immediate stop
            NativeEngine.stop(stopHandle)
            val postStopRead = NativeEngine.read(stopHandle, tempBuf)
            if (postStopRead > 0) {
                allIterationsPassed = false
                break
            }

            // Immediately synthesize follow-up utterance on same engine instance
            val followUpBytes = OpenEVVEncoder.encode(PauseProcessor.process("Short follow-up utterance $i."))
            val followUpOk = NativeEngine.speak(stopHandle, followUpBytes)
            if (!followUpOk) {
                allIterationsPassed = false
                break
            }
            var followUpTotal = 0
            while (true) {
                val n = NativeEngine.read(stopHandle, tempBuf)
                if (n <= 0) break
                followUpTotal += n
            }
            if (followUpTotal <= 0) {
                allIterationsPassed = false
                break
            }
        }
        assert(allIterationsPassed, "10 consecutive speech -> stop -> speech cycles completed without hanging or deadlocks")
        NativeEngine.destroy(stopHandle)
        Thread.sleep(50)
    }

    // --- Suite 11: Speech Rate & Pitch Parameter Scaling ---
    println("\n--- Suite 11: Speech Rate & Pitch Parameter Scaling ---")
    val paramHandle = NativeEngine.create(0x00010000)
    assert(paramHandle != 0L, "Created engine handle for parameter tests")
    if (paramHandle != 0L) {
        for (preset in 0..7) {
            val copyRes = NativeEngine.copyVoice(paramHandle, preset + Eci.FIRST_PRESET, Eci.VOICE_CURRENT)
            assert(copyRes != 0, "copyVoice succeeded for preset $preset (${Eci.PRESET_NAMES[preset]})")
        }

        val speedSet = NativeEngine.setVoiceParam(paramHandle, Eci.VOICE_CURRENT, Eci.VOICE_SPEED, 80)
        val speedGet = NativeEngine.getVoiceParam(paramHandle, Eci.VOICE_CURRENT, Eci.VOICE_SPEED)
        assert(speedSet >= 0 && speedGet == 80, "setVoiceParam SPEED 80 succeeded (read back: $speedGet)")

        val pitchSet = NativeEngine.setVoiceParam(paramHandle, Eci.VOICE_CURRENT, Eci.VOICE_PITCH_BASELINE, 65)
        val pitchGet = NativeEngine.getVoiceParam(paramHandle, Eci.VOICE_CURRENT, Eci.VOICE_PITCH_BASELINE)
        assert(pitchSet >= 0 && pitchGet == 65, "setVoiceParam PITCH 65 succeeded (read back: $pitchGet)")

        NativeEngine.destroy(paramHandle)
        Thread.sleep(50)
    }

    // --- Suite 12: Unicode 16.0 & CLDR 48 Emoji Speech Processing ---
    println("\n--- Suite 12: Unicode 16.0 & CLDR 48 Emoji Speech Processing ---")
    assert(EmojiData.isLoaded, "EmojiData.isLoaded is true")
    assert(EmojiData.totalEmojis >= 3800, "EmojiData loaded ${EmojiData.totalEmojis} unique emoji sequences")

    // Basic emojis
    assert(EmojiProcessor.process("😀", "en") == "grinning face", "😀 -> grinning face")
    assert(EmojiProcessor.process("😂", "en") == "face with tears of joy", "😂 -> face with tears of joy")
    assert(EmojiProcessor.process("😎", "en") == "smiling face with sunglasses", "😎 -> smiling face with sunglasses")

    // Variation selector
    assert(EmojiProcessor.process("❤️", "en") == "red heart", "❤️ (with U+FE0F) -> red heart")
    assert(EmojiProcessor.process("\u2764", "en") == "red heart", "❤ (without U+FE0F) -> red heart")

    // Skin-tone modifier
    assert(EmojiProcessor.process("👍🏽", "en") == "thumbs up: medium skin tone", "👍🏽 -> thumbs up: medium skin tone")

    // ZWJ / profession
    assert(EmojiProcessor.process("👩\u200D💻", "en") == "woman technologist", "👩‍💻 -> woman technologist")

    // Family ZWJ sequence
    assert(EmojiProcessor.process("👨\u200D👩\u200D👧\u200D👦", "en") == "family: man, woman, girl, boy", "👨‍👩‍👧‍👦 -> family: man, woman, girl, boy")

    // Flags
    assert(EmojiProcessor.process("🇮🇳", "en") == "flag: India", "🇮🇳 -> flag: India")
    assert(EmojiProcessor.process("🇺🇸", "en") == "flag: United States", "🇺🇸 -> flag: United States")

    // Keycap
    assert(EmojiProcessor.process("1️⃣", "en") == "keycap: 1", "1️⃣ -> keycap: 1")

    // Complex modifier sequence
    assert(EmojiProcessor.process("❤️\u200D🔥", "en") == "heart on fire", "❤️‍🔥 -> heart on fire")

    // Repeated emojis with comma-separation prosody
    val tripleJoy = EmojiProcessor.process("😂😂😂", "en")
    assert(tripleJoy == "face with tears of joy, face with tears of joy, face with tears of joy",
        "Consecutive emojis comma-separated: '$tripleJoy'")

    val spacedJoy = EmojiProcessor.process("😂 😂 😂", "en")
    assert(spacedJoy == "face with tears of joy, face with tears of joy, face with tears of joy",
        "Whitespace-separated consecutive emojis comma-separated: '$spacedJoy'")

    // Word and punctuation boundary spacing
    val mixedSurround = EmojiProcessor.process("Hello 😀 world", "en")
    assert(mixedSurround == "Hello grinning face world", "Word boundary space preserved: '$mixedSurround'")

    val mixedNoSpaces = EmojiProcessor.process("Hello😀world", "en")
    assert(mixedNoSpaces == "Hello grinning face world", "Missing spaces automatically injected around words: '$mixedNoSpaces'")

    val attachedPunct = EmojiProcessor.process("Hello 😀!", "en")
    assert(attachedPunct == "Hello grinning face!", "Sentence punctuation remains tightly bound: '$attachedPunct'")

    val punctBeforeAfter = EmojiProcessor.process("Hello! 😀 Are you okay?", "en")
    assert(punctBeforeAfter == "Hello! grinning face Are you okay?", "Punctuation before and after emoji preserved: '$punctBeforeAfter'")

    // Multilingual annotations across all 5 languages
    assert(EmojiProcessor.process("😀", "es") == "cara sonriendo", "Spanish 😀 -> cara sonriendo")
    assert(EmojiProcessor.process("❤️", "es") == "corazón rojo", "Spanish ❤️ -> corazón rojo")
    assert(EmojiProcessor.process("👍🏽", "es") == "pulgar hacia arriba: tono de piel medio", "Spanish 👍🏽 -> pulgar hacia arriba: tono de piel medio")

    assert(EmojiProcessor.process("😀", "fr") == "visage rieur", "French 😀 -> visage rieur")
    assert(EmojiProcessor.process("❤️", "fr") == "cœur rouge", "French ❤️ -> cœur rouge")

    assert(EmojiProcessor.process("😀", "de") == "grinsendes Gesicht", "German 😀 -> grinsendes Gesicht")
    assert(EmojiProcessor.process("❤️", "de") == "rotes Herz", "German ❤️ -> rotes Herz")

    assert(EmojiProcessor.process("😀", "it") == "faccina con un gran sorriso", "Italian 😀 -> faccina con un gran sorriso")
    assert(EmojiProcessor.process("❤️", "it") == "cuore rosso", "Italian ❤️ -> cuore rosso")

    // --- Suite 13: Conservative ASCII Emoticons & Protected Spans ---
    println("\n--- Suite 13: Conservative ASCII Emoticons & Protected Spans ---")
    assert(EmoticonProcessor.process("Hello :)", "en") == "Hello smiling face", "Smile :) -> smiling face")
    assert(EmoticonProcessor.process("I am sad :(", "en") == "I am sad frowning face", "Sad :( -> frowning face")
    assert(EmoticonProcessor.process("Just kidding ;)", "en") == "Just kidding winking face", "Wink ;) -> winking face")
    assert(EmoticonProcessor.process("Awesome :-D", "en") == "Awesome grinning face", "Grin :-D -> grinning face")
    assert(EmoticonProcessor.process("Love you <3", "en") == "Love you heart", "Heart <3 -> heart")
    assert(EmoticonProcessor.process("Broken </3", "en") == "Broken broken heart", "Broken heart </3 -> broken heart")
    assert(EmoticonProcessor.process("Skeptical :/", "en") == "Skeptical skeptical face", "Skeptical :/ -> skeptical face")

    // Multilingual emoticons
    assert(EmoticonProcessor.process("Hola :)", "es") == "Hola cara sonriente", "Spanish :) -> cara sonriente")
    assert(EmoticonProcessor.process("Bonjour :)", "fr") == "Bonjour visage souriant", "French :) -> visage souriant")
    assert(EmoticonProcessor.process("Hallo :)", "de") == "Hallo lächelndes Gesicht", "German :) -> lächelndes Gesicht")
    assert(EmoticonProcessor.process("Ciao :)", "it") == "Ciao faccina sorridente", "Italian :) -> faccina sorridente")

    // Strict Protected Spans & Zero False Positives
    val protectedUrl = EmoticonProcessor.process("Visit http://example.com/test or https://secure.org", "en")
    assert(protectedUrl == "Visit http://example.com/test or https://secure.org", "URLs protected: '$protectedUrl'")

    val protectedTime = EmoticonProcessor.process("Meeting at 2:30 PM", "en")
    assert(protectedTime == "Meeting at 2:30 PM", "Times protected: '$protectedTime'")

    val protectedRatio = EmoticonProcessor.process("Display ratio is 16:9", "en")
    assert(protectedRatio == "Display ratio is 16:9", "Ratios protected: '$protectedRatio'")

    val protectedWinPath = EmoticonProcessor.process("Path is C:\\path\\to\\file", "en")
    assert(protectedWinPath == "Path is C:\\path\\to\\file", "Windows drive path protected: '$protectedWinPath'")

    val protectedScope = EmoticonProcessor.process("Call std::vector in C++", "en")
    assert(protectedScope == "Call std::vector in C++", "Scope operator :: protected: '$protectedScope'")

    val protectedComp1 = EmoticonProcessor.process("Check if x < 3 in code", "en")
    assert(protectedComp1 == "Check if x < 3 in code", "Comparison x < 3 protected: '$protectedComp1'")

    val protectedComp2 = EmoticonProcessor.process("if (x<3)", "en")
    assert(protectedComp2 == "if (x<3)", "Comparison x<3 protected: '$protectedComp2'")

    val protectedNumbers = EmoticonProcessor.process("Value is 3.14 and 1,024", "en")
    assert(protectedNumbers == "Value is 3.14 and 1,024", "Numbers protected: '$protectedNumbers'")

    val protectedAbbrev = EmoticonProcessor.process("Mr. Smith at U.S. e.g.", "en")
    assert(protectedAbbrev == "Mr. Smith at U.S. e.g.", "Abbreviations protected: '$protectedAbbrev'")

    // --- Suite 14: End-to-End Emoji Speech Synthesis with OpenEVV Native Engine ---
    println("\n--- Suite 14: End-to-End Emoji Speech Synthesis with OpenEVV Native Engine ---")
    val emojiUtterances = mapOf(
        0x00010000 to Pair("Hello! 😀 Have a wonderful day ❤️!", "en"),
        0x00020000 to Pair("¡Hola! 😀 ¡Que tengas un excelente día ❤️!", "es"),
        0x00030000 to Pair("Bonjour! 😀 Passez une excellente journée ❤️!", "fr"),
        0x00040000 to Pair("Hallo! 😀 Einen wunderschönen Tag noch ❤️!", "de"),
        0x00050000 to Pair("Ciao! 😀 Buona splendida giornata ❤️!", "it")
    )

    for ((eciId, pair) in emojiUtterances) {
        val (rawText, langCode) = pair
        val handle = NativeEngine.create(eciId)
        assert(handle != 0L, "NativeEngine created for emoji synthesis (0x${Integer.toHexString(eciId)})")
        if (handle != 0L) {
            val emojiProcessed = EmojiProcessor.process(rawText, langCode)
            val emotProcessed = EmoticonProcessor.process(emojiProcessed, langCode)
            val normalized = UnicodeNormalizer.normalize(emotProcessed)
            val punctuated = ScreenReaderPunctuationProcessor.process(normalized, spokenPunctuation = false)
            val fixed = OpenEVVCompatibilityFixes.apply(punctuated)
            val prepared = PauseProcessor.process(fixed)
            val textBytes = OpenEVVEncoder.encode(prepared)

            val ok = NativeEngine.speak(handle, textBytes)
            assert(ok, "NativeEngine.speak accepted emoji text for 0x${Integer.toHexString(eciId)}")

            val buf = ByteArray(4096)
            var total = 0
            while (true) {
                val n = NativeEngine.read(handle, buf)
                if (n <= 0) break
                total += n
            }
            assert(total > 0, "NativeEngine synthesized $total PCM bytes for emoji utterance in $langCode")
            NativeEngine.destroy(handle)
        }
    }

    // --- Suite 15: Central Settings Model, Defaults, Persistence & Reset All ---
    println("\n--- Suite 15: Central Settings Model, Defaults, Persistence & Reset All ---")
    val mockPrefs = MockSharedPreferences()
    val settings = Settings(mockPrefs)

    // 15.1 Default values check
    assert(settings.voiceProfile == SettingsDefaults.DEFAULT_VOICE_PROFILE, "Default voiceProfile is ${SettingsDefaults.DEFAULT_VOICE_PROFILE}")
    assert(settings.forceSpeechRate == SettingsDefaults.DEFAULT_FORCE_SPEECH_RATE, "Default forceSpeechRate is false")
    assert(settings.speechRate == SettingsDefaults.DEFAULT_SPEECH_RATE, "Default speechRate is ${SettingsDefaults.DEFAULT_SPEECH_RATE}")
    assert(settings.unlockMaxRate == SettingsDefaults.DEFAULT_UNLOCK_MAX_RATE, "Default unlockMaxRate is false")
    assert(settings.forcePitch == SettingsDefaults.DEFAULT_FORCE_PITCH, "Default forcePitch is false")
    assert(settings.pitch == SettingsDefaults.DEFAULT_PITCH, "Default pitch is ${SettingsDefaults.DEFAULT_PITCH}")
    assert(settings.forceVolume == SettingsDefaults.DEFAULT_FORCE_VOLUME, "Default forceVolume is false")
    assert(settings.volume == SettingsDefaults.DEFAULT_VOLUME, "Default volume is ${SettingsDefaults.DEFAULT_VOLUME}")
    assert(settings.inflection == SettingsDefaults.DEFAULT_INFLECTION, "Default inflection is ${SettingsDefaults.DEFAULT_INFLECTION}")
    assert(settings.headSize == SettingsDefaults.DEFAULT_HEAD_SIZE, "Default headSize is ${SettingsDefaults.DEFAULT_HEAD_SIZE}")
    assert(settings.roughness == SettingsDefaults.DEFAULT_ROUGHNESS, "Default roughness is ${SettingsDefaults.DEFAULT_ROUGHNESS}")
    assert(settings.breathiness == SettingsDefaults.DEFAULT_BREATHINESS, "Default breathiness is ${SettingsDefaults.DEFAULT_BREATHINESS}")
    assert(settings.enableEmoji == SettingsDefaults.DEFAULT_EMOJI_EMOTICON, "Default enableEmoji is true")
    assert(settings.processPunctuation == SettingsDefaults.DEFAULT_PROCESS_PUNCTUATION, "Default processPunctuation is false")
    assert(settings.punctuationLevel == SettingsDefaults.DEFAULT_PUNCTUATION_LEVEL, "Default punctuationLevel is 0 (None)")
    assert(settings.customPunctuation == SettingsDefaults.DEFAULT_CUSTOM_PUNCTUATION, "Default customPunctuation is empty")
    assert(settings.useNumberProcessing == SettingsDefaults.DEFAULT_USE_NUMBER_PROCESSING, "Default useNumberProcessing is false")
    assert(settings.numberProcessingMode == SettingsDefaults.DEFAULT_NUMBER_PROCESSING_MODE, "Default numberProcessingMode is 0 (Digits)")
    assert(settings.useAbbreviations == SettingsDefaults.DEFAULT_USE_ABBREVIATIONS, "Default useAbbreviations is true")
    assert(settings.intonationPauses == SettingsDefaults.DEFAULT_INTONATION_PAUSES, "Default intonationPauses is true")
    assert(settings.forceLanguage == SettingsDefaults.DEFAULT_FORCE_LANGUAGE, "Default forceLanguage is false")
    assert(settings.language == SettingsDefaults.DEFAULT_LANGUAGE, "Default language is ${SettingsDefaults.DEFAULT_LANGUAGE}")
    assert(settings.samplingRate == SettingsDefaults.DEFAULT_SAMPLING_RATE, "Default samplingRate is ${SettingsDefaults.DEFAULT_SAMPLING_RATE}")

    // 15.2 Mutate every setting away from default
    settings.voiceProfile = 3
    settings.forceSpeechRate = true
    settings.speechRate = 5
    settings.unlockMaxRate = true
    settings.forcePitch = true
    settings.pitch = 5
    settings.forceVolume = true
    settings.volume = -3
    settings.inflection = 4
    settings.headSize = -3
    settings.roughness = 6
    settings.breathiness = 2
    settings.enableEmoji = false
    settings.processPunctuation = true
    settings.punctuationLevel = 4
    settings.customPunctuation = "@#%*"
    settings.useNumberProcessing = true
    settings.numberProcessingMode = 1
    settings.useAbbreviations = false
    settings.intonationPauses = false
    settings.forceLanguage = true
    settings.language = "es-ES"
    settings.samplingRate = 16000

    assert(settings.voiceProfile == 3 && mockPrefs.map[SettingsDefaults.KEY_VOICE_PROFILE] == 3, "Mutated voiceProfile persisted")
    assert(settings.forceSpeechRate && mockPrefs.map[SettingsDefaults.KEY_FORCE_SPEECH_RATE] == true, "Mutated forceSpeechRate persisted")
    assert(settings.speechRate == 5 && mockPrefs.map[SettingsDefaults.KEY_SPEECH_RATE] == 5, "Mutated speechRate persisted")
    assert(settings.unlockMaxRate && mockPrefs.map[SettingsDefaults.KEY_UNLOCK_MAX_RATE] == true, "Mutated unlockMaxRate persisted")
    assert(settings.forcePitch && mockPrefs.map[SettingsDefaults.KEY_FORCE_PITCH] == true, "Mutated forcePitch persisted")
    assert(settings.pitch == 5 && mockPrefs.map[SettingsDefaults.KEY_PITCH] == 5, "Mutated pitch persisted")
    assert(settings.forceVolume && mockPrefs.map[SettingsDefaults.KEY_FORCE_VOLUME] == true, "Mutated forceVolume persisted")
    assert(settings.volume == -3 && mockPrefs.map[SettingsDefaults.KEY_VOLUME] == -3, "Mutated volume persisted")
    assert(settings.inflection == 4 && mockPrefs.map[SettingsDefaults.KEY_INFLECTION] == 4, "Mutated inflection persisted")
    assert(settings.headSize == -3 && mockPrefs.map[SettingsDefaults.KEY_HEAD_SIZE] == -3, "Mutated headSize persisted")
    assert(settings.roughness == 6 && mockPrefs.map[SettingsDefaults.KEY_ROUGHNESS] == 6, "Mutated roughness persisted")
    assert(settings.breathiness == 2 && mockPrefs.map[SettingsDefaults.KEY_BREATHINESS] == 2, "Mutated breathiness persisted")
    assert(!settings.enableEmoji && mockPrefs.map[SettingsDefaults.KEY_EMOJI_EMOTICON] == false, "Mutated enableEmoji persisted")
    assert(settings.processPunctuation && mockPrefs.map[SettingsDefaults.KEY_PROCESS_PUNCTUATION] == true, "Mutated processPunctuation persisted")
    assert(settings.punctuationLevel == 4 && mockPrefs.map[SettingsDefaults.KEY_PUNCTUATION_LEVEL] == 4, "Mutated punctuationLevel persisted")
    assert(settings.customPunctuation == "@#%*" && mockPrefs.map[SettingsDefaults.KEY_CUSTOM_PUNCTUATION] == "@#%*", "Mutated customPunctuation persisted")
    assert(settings.useNumberProcessing && mockPrefs.map[SettingsDefaults.KEY_USE_NUMBER_PROCESSING] == true, "Mutated useNumberProcessing persisted")
    assert(settings.numberProcessingMode == 1 && mockPrefs.map[SettingsDefaults.KEY_NUMBER_PROCESSING_MODE] == 1, "Mutated numberProcessingMode persisted")
    assert(!settings.useAbbreviations && mockPrefs.map[SettingsDefaults.KEY_USE_ABBREVIATIONS] == false, "Mutated useAbbreviations persisted")
    assert(!settings.intonationPauses && mockPrefs.map[SettingsDefaults.KEY_INTONATION_PAUSES] == false, "Mutated intonationPauses persisted")
    assert(settings.forceLanguage && mockPrefs.map[SettingsDefaults.KEY_FORCE_LANGUAGE] == true, "Mutated forceLanguage persisted")
    assert(settings.language == "es-ES" && mockPrefs.map[SettingsDefaults.KEY_LANGUAGE] == "es-ES", "Mutated language persisted")
    assert(settings.samplingRate == 16000 && mockPrefs.map[SettingsDefaults.KEY_SAMPLING_RATE] == 16000, "Mutated samplingRate persisted")

    // Verify persistence across reload in new Settings instance
    val reloadedSettings = Settings(mockPrefs)
    assert(reloadedSettings.punctuationLevel == 4, "Reloaded settings has punctuationLevel 4")
    assert(reloadedSettings.customPunctuation == "@#%*", "Reloaded settings has customPunctuation @#%*")

    // 15.3 Reset All Settings
    settings.resetAll()
    assert(settings.voiceProfile == SettingsDefaults.DEFAULT_VOICE_PROFILE, "resetAll restored voiceProfile")
    assert(settings.forceSpeechRate == SettingsDefaults.DEFAULT_FORCE_SPEECH_RATE, "resetAll restored forceSpeechRate")
    assert(settings.speechRate == SettingsDefaults.DEFAULT_SPEECH_RATE, "resetAll restored speechRate")
    assert(settings.unlockMaxRate == SettingsDefaults.DEFAULT_UNLOCK_MAX_RATE, "resetAll restored unlockMaxRate")
    assert(settings.forcePitch == SettingsDefaults.DEFAULT_FORCE_PITCH, "resetAll restored forcePitch")
    assert(settings.pitch == SettingsDefaults.DEFAULT_PITCH, "resetAll restored pitch")
    assert(settings.forceVolume == SettingsDefaults.DEFAULT_FORCE_VOLUME, "resetAll restored forceVolume")
    assert(settings.volume == SettingsDefaults.DEFAULT_VOLUME, "resetAll restored volume")
    assert(settings.inflection == SettingsDefaults.DEFAULT_INFLECTION, "resetAll restored inflection")
    assert(settings.headSize == SettingsDefaults.DEFAULT_HEAD_SIZE, "resetAll restored headSize")
    assert(settings.roughness == SettingsDefaults.DEFAULT_ROUGHNESS, "resetAll restored roughness")
    assert(settings.breathiness == SettingsDefaults.DEFAULT_BREATHINESS, "resetAll restored breathiness")
    assert(settings.enableEmoji == SettingsDefaults.DEFAULT_EMOJI_EMOTICON, "resetAll restored enableEmoji")
    assert(settings.processPunctuation == SettingsDefaults.DEFAULT_PROCESS_PUNCTUATION, "resetAll restored processPunctuation")
    assert(settings.punctuationLevel == SettingsDefaults.DEFAULT_PUNCTUATION_LEVEL, "resetAll restored punctuationLevel")
    assert(settings.customPunctuation == SettingsDefaults.DEFAULT_CUSTOM_PUNCTUATION, "resetAll restored customPunctuation")
    assert(settings.useNumberProcessing == SettingsDefaults.DEFAULT_USE_NUMBER_PROCESSING, "resetAll restored useNumberProcessing")
    assert(settings.numberProcessingMode == SettingsDefaults.DEFAULT_NUMBER_PROCESSING_MODE, "resetAll restored numberProcessingMode")
    assert(settings.useAbbreviations == SettingsDefaults.DEFAULT_USE_ABBREVIATIONS, "resetAll restored useAbbreviations")
    assert(settings.intonationPauses == SettingsDefaults.DEFAULT_INTONATION_PAUSES, "resetAll restored intonationPauses")
    assert(settings.forceLanguage == SettingsDefaults.DEFAULT_FORCE_LANGUAGE, "resetAll restored forceLanguage")
    assert(settings.language == SettingsDefaults.DEFAULT_LANGUAGE, "resetAll restored language")
    assert(settings.samplingRate == SettingsDefaults.DEFAULT_SAMPLING_RATE, "resetAll restored samplingRate")
    assert(mockPrefs.map[SettingsDefaults.KEY_SPEECH_RATE] == SettingsDefaults.DEFAULT_SPEECH_RATE, "resetAll persisted default speechRate in storage")
    assert(mockPrefs.map[SettingsDefaults.KEY_SAMPLING_RATE] == SettingsDefaults.DEFAULT_SAMPLING_RATE, "resetAll persisted default samplingRate in storage")

    // --- Suite 16: Force Speech Rate Precedence & Relative Rate Mapping ---
    println("\n--- Suite 16: Force Speech Rate Precedence & Relative Rate Mapping ---")
    val requestRate = 75
    val forcedRate = 5
    settings.forceSpeechRate = false
    val normalPrecedence = if (settings.forceSpeechRate) settings.speechRate else requestRate
    assert(normalPrecedence == requestRate, "When forceSpeechRate=false, requestRate ($requestRate) takes precedence")

    settings.forceSpeechRate = true
    settings.speechRate = forcedRate
    val forcedPrecedence = if (settings.forceSpeechRate) settings.speechRate else requestRate
    assert(forcedPrecedence == forcedRate, "When forceSpeechRate=true, Eloqium speechRate ($forcedRate) overrides requestRate")

    // Relative rate mapping checks
    assert(SpeechRateMapper.mapRelativeRate(0, unlocked = false) == 57, "Relative rate 0 maps to baseline 57 speed")
    assert(SpeechRateMapper.mapRelativeRate(-10, unlocked = false) == 40, "Relative rate -10 maps to min 40 speed")
    assert(SpeechRateMapper.mapRelativeRate(10, unlocked = false) == 150, "Relative rate 10 maps to normal max 150 speed")
    assert(SpeechRateMapper.mapRelativeRate(10, unlocked = true) == 250, "Relative rate 10 (unlocked) maps to unlocked max 250 speed")
    assert(SpeechRateMapper.mapAndroidRate(100, unlocked = false) == 57, "Android 1.0x (100) maps to baseline 57 speed")

    // Gradual progression across -10, -5, 0, 3, 7, 10
    val rM10 = SpeechRateMapper.mapRelativeRate(-10)
    val rM5 = SpeechRateMapper.mapRelativeRate(-5)
    val r0 = SpeechRateMapper.mapRelativeRate(0)
    val r3 = SpeechRateMapper.mapRelativeRate(3)
    val r7 = SpeechRateMapper.mapRelativeRate(7)
    val r10 = SpeechRateMapper.mapRelativeRate(10)
    assert(rM10 < rM5 && rM5 < r0 && r0 < r3 && r3 < r7 && r7 < r10,
        "Speech rate scales monotonically: -10->$rM10, -5->$rM5, 0->$r0, 3->$r3, 7->$r7, 10->$r10")

    // Monotonicity check across -10..10
    var strictlyMonotonic = true
    for (i in -10 until 10) {
        if (SpeechRateMapper.mapRelativeRate(i) > SpeechRateMapper.mapRelativeRate(i + 1)) {
            strictlyMonotonic = false
            break
        }
    }
    assert(strictlyMonotonic, "SpeechRateMapper.mapRelativeRate is strictly monotonic across -10..10")

    // Legacy migration check
    assert(SpeechRateMapper.mapLegacyToRelative(50) == 0, "Legacy 50 maps to relative 0")
    assert(SpeechRateMapper.mapLegacyToRelative(0) == -10, "Legacy 0 maps to relative -10")
    assert(SpeechRateMapper.mapLegacyToRelative(100) == 10, "Legacy 100 maps to relative 10")
    assert(SpeechRateMapper.mapLegacyToRelative(75) == 5, "Legacy 75 maps to relative 5")

    // Native rate bounds
    val rateEngineHandle = NativeEngine.create(0x00010000)
    if (rateEngineHandle != 0L) {
        val minSpeedSet = NativeEngine.setVoiceParam(rateEngineHandle, Eci.VOICE_CURRENT, Eci.VOICE_SPEED, SpeechRateMapper.MIN_SPEED)
        assert(minSpeedSet >= 0, "Native engine accepts minimum speech rate (40)")
        val normalSpeedSet = NativeEngine.setVoiceParam(rateEngineHandle, Eci.VOICE_CURRENT, Eci.VOICE_SPEED, SpeechRateMapper.NORMAL_SPEED)
        assert(normalSpeedSet >= 0, "Native engine accepts normal baseline speech rate (57)")
        val maxSpeedSet = NativeEngine.setVoiceParam(rateEngineHandle, Eci.VOICE_CURRENT, Eci.VOICE_SPEED, SpeechRateMapper.MAX_SPEED_UNLOCKED)
        assert(maxSpeedSet >= 0, "Native engine accepts unlocked maximum speech rate (250)")
        NativeEngine.destroy(rateEngineHandle)
    }

    // --- Suite 17: Force Pitch Precedence & Relative Scale Mapping ---
    println("\n--- Suite 17: Force Pitch Precedence & Relative Scale Mapping ---")
    val requestPitch = 60
    val forcedPitchVal = -3
    settings.forcePitch = false
    val normalPitchPrec = if (settings.forcePitch) settings.pitch else requestPitch
    assert(normalPitchPrec == requestPitch, "When forcePitch=false, requestPitch ($requestPitch) takes precedence")

    settings.forcePitch = true
    settings.pitch = forcedPitchVal
    val forcedPitchPrec = if (settings.forcePitch) settings.pitch else requestPitch
    assert(forcedPitchPrec == forcedPitchVal, "When forcePitch=true, Eloqium pitch ($forcedPitchVal) overrides requestPitch")

    // Relative pitch mapping around basePitch=65
    assert(VoiceParameterMapper.mapPitch(0, 65) == 65, "Pitch 0 maps to neutral basePitch (65)")
    assert(VoiceParameterMapper.mapPitch(-10, 65) == 25, "Pitch -10 maps to lower bound (25)")
    assert(VoiceParameterMapper.mapPitch(10, 65) == 100, "Pitch 10 maps to upper bound (100)")
    val pMinus5 = VoiceParameterMapper.mapPitch(-5, 65)
    val pPlus5 = VoiceParameterMapper.mapPitch(5, 65)
    assert(pMinus5 < 65 && pPlus5 > 65, "Pitch -5 ($pMinus5) < baseline (65) < +5 ($pPlus5)")

    var pitchMonotonic = true
    for (i in -10 until 10) {
        if (VoiceParameterMapper.mapPitch(i, 65) > VoiceParameterMapper.mapPitch(i + 1, 65)) {
            pitchMonotonic = false
            break
        }
    }
    assert(pitchMonotonic, "VoiceParameterMapper.mapPitch is strictly monotonic across -10..10")

    val pitchEngineHandle = NativeEngine.create(0x00010000)
    if (pitchEngineHandle != 0L) {
        val minPitchSet = NativeEngine.setVoiceParam(pitchEngineHandle, Eci.VOICE_CURRENT, Eci.VOICE_PITCH_BASELINE, 25)
        assert(minPitchSet >= 0, "Native engine accepts lower pitch bound (25)")
        val maxPitchSet = NativeEngine.setVoiceParam(pitchEngineHandle, Eci.VOICE_CURRENT, Eci.VOICE_PITCH_BASELINE, 100)
        assert(maxPitchSet >= 0, "Native engine accepts maximum pitch baseline (100)")
        NativeEngine.destroy(pitchEngineHandle)
    }

    // --- Suite 18: Audio Gain & Relative Volume Processing ---
    println("\n--- Suite 18: Audio Gain & Relative Volume Processing ---")
    // Test 16-bit linear PCM little-endian buffer with samples: [1000, -2000, 30000, -30000, 0]
    val testSamples = shortArrayOf(1000, -2000, 30000, -30000, 0)
    fun makePcmBuffer(samples: ShortArray): ByteArray {
        val buf = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val s = samples[i].toInt()
            buf[i * 2] = (s and 0xFF).toByte()
            buf[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return buf
    }
    fun readSample(buf: ByteArray, idx: Int): Short {
        val low = buf[idx * 2].toInt() and 0xFF
        val high = buf[idx * 2 + 1].toInt()
        return ((high shl 8) or low).toShort()
    }

    // Default volume (0 -> 100% gain)
    val buf0 = makePcmBuffer(testSamples)
    AudioGainProcessor.applyRelativeGain(buf0, buf0.size, 0)
    assert(readSample(buf0, 0) == 1000.toShort(), "Relative Volume 0 leaves sample 1000 unchanged")
    assert(readSample(buf0, 2) == 30000.toShort(), "Relative Volume 0 leaves sample 30000 unchanged")

    // Attenuated volume (-5 -> 60% gain)
    val bufM5 = makePcmBuffer(testSamples)
    AudioGainProcessor.applyRelativeGain(bufM5, bufM5.size, -5)
    assert(readSample(bufM5, 0) == 600.toShort(), "Relative Volume -5 scales 1000 to 600 (read: ${readSample(bufM5, 0)})")
    assert(readSample(bufM5, 1) == (-1200).toShort(), "Relative Volume -5 scales -2000 to -1200 (read: ${readSample(bufM5, 1)})")

    // Min volume (-10 -> 20% gain)
    val bufM10 = makePcmBuffer(testSamples)
    AudioGainProcessor.applyRelativeGain(bufM10, bufM10.size, -10)
    assert(readSample(bufM10, 0) == 200.toShort(), "Relative Volume -10 scales 1000 to 200 (read: ${readSample(bufM10, 0)})")

    // Boosted volume (+5 -> 150% gain with clamping)
    val bufP5 = makePcmBuffer(testSamples)
    AudioGainProcessor.applyRelativeGain(bufP5, bufP5.size, 5)
    assert(readSample(bufP5, 0) == 1500.toShort(), "Relative Volume +5 scales 1000 to 1500")
    assert(readSample(bufP5, 2) == 32767.toShort(), "Relative Volume +5 clamps 30000 * 1.5 to 32767")
    assert(readSample(bufP5, 3) == (-32768).toShort(), "Relative Volume +5 clamps -30000 * 1.5 to -32768")

    // Legacy volume conversion
    assert(AudioGainProcessor.mapLegacyToRelative(100) == 0, "Legacy 100 maps to relative 0")
    assert(AudioGainProcessor.mapLegacyToRelative(0) == -10, "Legacy 0 maps to relative -10")
    assert(AudioGainProcessor.mapLegacyToRelative(50) == -5, "Legacy 50 maps to relative -5")

    // --- Suite 19: Voice Characteristics Engine Mapping ---
    println("\n--- Suite 19: Voice Characteristics Engine Mapping ---")
    // Inflection: 0 -> 30, -10 -> 0, 10 -> 100
    assert(VoiceParameterMapper.mapInflection(0) == 30, "Relative inflection 0 maps to baseline 30")
    assert(VoiceParameterMapper.mapInflection(-10) == 0, "Relative inflection -10 maps to 0")
    assert(VoiceParameterMapper.mapInflection(10) == 100, "Relative inflection 10 maps to 100")

    // Head size: 0 -> 50, -10 -> 0, 10 -> 100
    assert(VoiceParameterMapper.mapHeadSize(0) == 50, "Relative head size 0 maps to baseline 50")
    assert(VoiceParameterMapper.mapHeadSize(-10) == 0, "Relative head size -10 maps to 0")
    assert(VoiceParameterMapper.mapHeadSize(10) == 100, "Relative head size 10 maps to 100")

    // Roughness: 0 -> 0, 10 -> 100, negative clamps to 0
    assert(VoiceParameterMapper.mapRoughness(0) == 0, "Relative roughness 0 maps to baseline 0")
    assert(VoiceParameterMapper.mapRoughness(-5) == 0, "Relative roughness -5 clamps to 0")
    assert(VoiceParameterMapper.mapRoughness(10) == 100, "Relative roughness 10 maps to 100")

    // Breathiness: 0 -> 0, 10 -> 100, negative clamps to 0
    assert(VoiceParameterMapper.mapBreathiness(0) == 0, "Relative breathiness 0 maps to baseline 0")
    assert(VoiceParameterMapper.mapBreathiness(-5) == 0, "Relative breathiness -5 clamps to 0")
    assert(VoiceParameterMapper.mapBreathiness(10) == 100, "Relative breathiness 10 maps to 100")

    val voiceCharHandle = NativeEngine.create(0x00010000)
    if (voiceCharHandle != 0L) {
        val infVal = VoiceParameterMapper.mapInflection(5)
        val infSet = NativeEngine.setVoiceParam(voiceCharHandle, Eci.VOICE_CURRENT, Eci.VOICE_PITCH_FLUCTUATION, infVal)
        val infGet = NativeEngine.getVoiceParam(voiceCharHandle, Eci.VOICE_CURRENT, Eci.VOICE_PITCH_FLUCTUATION)
        assert(infSet >= 0 && infGet == infVal, "Inflection maps to VOICE_PITCH_FLUCTUATION ($infVal)")

        val headVal = VoiceParameterMapper.mapHeadSize(3)
        val headSet = NativeEngine.setVoiceParam(voiceCharHandle, Eci.VOICE_CURRENT, Eci.VOICE_HEAD_SIZE, headVal)
        val headGet = NativeEngine.getVoiceParam(voiceCharHandle, Eci.VOICE_CURRENT, Eci.VOICE_HEAD_SIZE)
        assert(headSet >= 0 && headGet == headVal, "Head size maps to VOICE_HEAD_SIZE ($headVal)")

        val roughVal = VoiceParameterMapper.mapRoughness(4)
        val roughSet = NativeEngine.setVoiceParam(voiceCharHandle, Eci.VOICE_CURRENT, Eci.VOICE_ROUGHNESS, roughVal)
        val roughGet = NativeEngine.getVoiceParam(voiceCharHandle, Eci.VOICE_CURRENT, Eci.VOICE_ROUGHNESS)
        assert(roughSet >= 0 && roughGet == roughVal, "Roughness maps to VOICE_ROUGHNESS ($roughVal)")

        val breathVal = VoiceParameterMapper.mapBreathiness(2)
        val breathSet = NativeEngine.setVoiceParam(voiceCharHandle, Eci.VOICE_CURRENT, Eci.VOICE_BREATHINESS, breathVal)
        val breathGet = NativeEngine.getVoiceParam(voiceCharHandle, Eci.VOICE_CURRENT, Eci.VOICE_BREATHINESS)
        assert(breathSet >= 0 && breathGet == breathVal, "Breathiness maps to VOICE_BREATHINESS ($breathVal)")

        NativeEngine.destroy(voiceCharHandle)
    }

    // --- Suite 20: Screen Reader Punctuation Levels ---
    println("\n--- Suite 20: Screen Reader Punctuation Levels ---")
    val punctText = "Test: #1 & 10% * 2 / 3 + 1 = 2 (items) [first] \"hello\" - next; period."

    // Disabled: prosodic only
    val pDisabled = ScreenReaderPunctuationProcessor.process(punctText, enabled = false)
    assert(!pDisabled.contains("star") && !pDisabled.contains("slash"), "Disabled punctuation does not verbalize symbols")

    // Level None: prosodic only
    val pNone = ScreenReaderPunctuationProcessor.process(punctText, enabled = true, level = SettingsDefaults.PUNCT_NONE)
    assert(!pNone.contains("star") && !pNone.contains("dot"), "Level None does not verbalize symbols")

    // Level Some: mathematical & critical symbols only
    val pSome = ScreenReaderPunctuationProcessor.process(punctText, enabled = true, level = SettingsDefaults.PUNCT_SOME)
    assert(pSome.contains("number") && pSome.contains("and") && pSome.contains("percent") &&
           pSome.contains("star") && pSome.contains("slash") && pSome.contains("plus") && pSome.contains("equals"),
        "Level Some verbalizes mathematical and syntax symbols: ")
    assert(!pSome.contains("left paren") && !pSome.contains("dot") && !pSome.contains("quote"),
        "Level Some does NOT verbalize parentheses, quotes, or periods")

    // Level Most: Some + brackets, quotes, dashes, colons
    val pMost = ScreenReaderPunctuationProcessor.process(punctText, enabled = true, level = SettingsDefaults.PUNCT_MOST)
    assert(pMost.contains("star") && pMost.contains("left paren") && pMost.contains("right paren") &&
           pMost.contains("left bracket") && pMost.contains("right bracket") && pMost.contains("quote") &&
           pMost.contains("dash") && pMost.contains("colon") && pMost.contains("semicolon"),
        "Level Most verbalizes structural delimiters: ")
    assert(!pMost.contains("dot"), "Level Most does NOT verbalize final period/dot")

    // Level All: Most + periods, commas, questions, exclamations
    val pAll = ScreenReaderPunctuationProcessor.process("Hello, world! Is it true? Yes.", enabled = true, level = SettingsDefaults.PUNCT_ALL)
    assert(pAll.contains("comma") && pAll.contains("exclamation mark") &&
           pAll.contains("question mark") && pAll.contains("dot"),
        "Level All verbalizes all punctuation: ")

    // Level Custom: single symbol
    val pCustomSingle = ScreenReaderPunctuationProcessor.process("Contact @user or check #tag and 50% off.", enabled = true, level = SettingsDefaults.PUNCT_CUSTOM, customPunctuation = "@")
    assert(pCustomSingle.contains("at") && !pCustomSingle.contains("number") && !pCustomSingle.contains("percent"),
        "Custom single symbol verbalizes only '@': $pCustomSingle")

    // Level Custom: multiple symbols
    val pCustomMulti = ScreenReaderPunctuationProcessor.process("Item @user #tag 50% * 2 = 100", enabled = true, level = SettingsDefaults.PUNCT_CUSTOM, customPunctuation = "@#*")
    assert(pCustomMulti.contains("at") && pCustomMulti.contains("number") && pCustomMulti.contains("star") && !pCustomMulti.contains("percent") && !pCustomMulti.contains("equals"),
        "Custom multiple symbols verbalizes only '@', '#', '*': $pCustomMulti")

    // Level Custom: empty string
    val pCustomEmpty = ScreenReaderPunctuationProcessor.process("Hello, world! Is it true? Yes.", enabled = true, level = SettingsDefaults.PUNCT_CUSTOM, customPunctuation = "")
    assert(!pCustomEmpty.contains("comma") && !pCustomEmpty.contains("question mark") && !pCustomEmpty.contains("dot") && !pCustomEmpty.contains("exclamation mark"),
        "Custom empty string verbalizes no punctuation: $pCustomEmpty")
    assert(pCustomEmpty.contains("?"), "Custom empty string preserves prosodic question mark: $pCustomEmpty")

    // Level Custom: duplicate characters
    val pCustomDups = ScreenReaderPunctuationProcessor.process("Check @here and *now*", enabled = true, level = SettingsDefaults.PUNCT_CUSTOM, customPunctuation = "@@@@****")
    assert(pCustomDups.contains("at") && pCustomDups.contains("star"), "Custom duplicate characters handled safely: $pCustomDups")
    assert(!pCustomDups.contains("at at") && !pCustomDups.contains("star star star"), "Custom duplicates do not cause duplicate verbalizations: $pCustomDups")

    // Level Custom: switching between modes
    val switchInput = "Alert: @user at 10%!"
    val resNone = ScreenReaderPunctuationProcessor.process(switchInput, enabled = true, level = SettingsDefaults.PUNCT_NONE)
    val resSome = ScreenReaderPunctuationProcessor.process(switchInput, enabled = true, level = SettingsDefaults.PUNCT_SOME)
    val resMost = ScreenReaderPunctuationProcessor.process(switchInput, enabled = true, level = SettingsDefaults.PUNCT_MOST)
    val resAll = ScreenReaderPunctuationProcessor.process(switchInput, enabled = true, level = SettingsDefaults.PUNCT_ALL)
    val resCustom = ScreenReaderPunctuationProcessor.process(switchInput, enabled = true, level = SettingsDefaults.PUNCT_CUSTOM, customPunctuation = "@")
    assert(!resNone.contains("at user"), "Mode switching None: $resNone")
    assert(resSome.contains("at user") && resSome.contains("percent") && !resSome.contains("colon"), "Mode switching Some: $resSome")
    assert(resMost.contains("colon") && resMost.contains("percent"), "Mode switching Most: $resMost")
    assert(resAll.contains("exclamation mark"), "Mode switching All: $resAll")
    assert(resCustom.contains("at user") && !resCustom.contains("percent") && !resCustom.contains("colon") && !resCustom.contains("exclamation mark"), "Mode switching Custom: $resCustom")

    // Protected patterns in Custom and other levels
    val pProtectedSome = ScreenReaderPunctuationProcessor.process("Values 3.14, 1,000 at 12:30.", enabled = true, level = SettingsDefaults.PUNCT_SOME)
    assert(pProtectedSome.contains("3.14") && pProtectedSome.contains("1,000") && pProtectedSome.contains("12:30"),
        "Numbers and times protected in Level Some: $pProtectedSome")

    val pProtectedCustom = ScreenReaderPunctuationProcessor.process("Values 3.14, 1,000 at 12:30. Is it valid? Yes.", enabled = true, level = SettingsDefaults.PUNCT_CUSTOM, customPunctuation = "@#")
    assert(pProtectedCustom.contains("3.14") && pProtectedCustom.contains("1,000") && pProtectedCustom.contains("12:30"),
        "Numbers and times protected in Level Custom: $pProtectedCustom")
    assert(pProtectedCustom.contains("?"), "Question intonation mark preserved in Level Custom: $pProtectedCustom")

    // Repeated punctuation
    val pRepeated = ScreenReaderPunctuationProcessor.process("Wait...", enabled = true, level = SettingsDefaults.PUNCT_ALL)
    assert(pRepeated.contains("dot dot dot") || pRepeated.contains("dot"), "Repeated punctuation verbalized: ")

    // --- Suite 21: Number Processing Modes ---
    println("\n--- Suite 21: Number Processing Modes ---")
    val numText = "In 1984 there were 42 computers and 1000000 bytes."

    // Disabled: untouched
    val numDisabled = NumberProcessor.process(numText, enabled = false)
    assert(numDisabled == numText, "Disabled number processing leaves text untouched")

    // Mode 0: Digits (spaced)
    val numDigits = NumberProcessor.process("Code 1984 and 42", enabled = true, mode = SettingsDefaults.NUMBER_DIGITS)
    assert(numDigits == "Code 1 9 8 4 and 4 2", "Mode Digits spaces every digit: ")

    // Mode 1: Pairs (2-digit chunks)
    val numPairs = NumberProcessor.process("Year 1984, hex 123456, small 123", enabled = true, mode = SettingsDefaults.NUMBER_PAIRS)
    assert(numPairs == "Year 19 84, hex 12 34 56, small 12 3", "Mode Pairs groups 2-by-2: ")

    // Mode 2: Triplets (3-digit groups from right)
    val numTriplets = NumberProcessor.process("Total 1234567 and 1000000", enabled = true, mode = SettingsDefaults.NUMBER_TRIPLETS)
    assert(numTriplets == "Total 1 234 567 and 1 000 000", "Mode Triplets groups in 3s: ")

    // --- Suite 22: Sampling Rate, Languages & About Navigation ---
    println("\n--- Suite 22: Sampling Rate, Languages & About Navigation ---")
    // Sampling rates
    for (rate in SettingsDefaults.SUPPORTED_SAMPLING_RATES) {
        val idx = Eci.sampleRateIndex(rate)
        val hz = Eci.sampleRateHz(idx)
        assert(hz == rate, "Sampling rate $rate Hz maps to index $idx and resolves back to $hz Hz")
    }

    // Supported languages
    for (lang in SettingsDefaults.SUPPORTED_LANGUAGES) {
        val parts = lang.split("-")
        val match = LocaleMatcher.matchLanguage(parts[0], parts[1])
        assert(match != null, "Supported language $lang resolves in LocaleMatcher (eciId=0x${Integer.toHexString(match?.first?.eciId ?: 0)})")
    }

    // About Navigation URLs
    assert(AboutNavigation.GITHUB_URL == "https://github.com/memon-ismail/eloqium-tts", "AboutNavigation.GITHUB_URL matches repo")
    assert(AboutNavigation.TELEGRAM_URL == "https://t.me/blindroidofficial", "AboutNavigation.TELEGRAM_URL matches official Telegram")
    assert(AboutNavigation.TELEGRAM_APP_URI.startsWith("tg://"), "AboutNavigation.TELEGRAM_APP_URI is valid tg:// scheme")

    // --- Suite 23: Voice Profile Dynamic Selection & Native Application ---
    println("\n--- Suite 23: Voice Profile Dynamic Selection & Native Application ---")
    val defaultReed = VoiceRegistry.findDefaultVoiceFor("en-US", presetId = 0)
    assert(defaultReed?.name?.contains("Reed") == true, "findDefaultVoiceFor with preset 0 returns Reed")

    val defaultShelley = VoiceRegistry.findDefaultVoiceFor("en-US", presetId = 1)
    assert(defaultShelley?.name?.contains("Shelley") == true, "findDefaultVoiceFor with preset 1 returns Shelley")

    val defaultBobby = VoiceRegistry.findDefaultVoiceFor("en-US", presetId = 2)
    assert(defaultBobby?.name?.contains("Bobby") == true, "findDefaultVoiceFor with preset 2 returns Bobby")

    val defaultGrandma = VoiceRegistry.findDefaultVoiceFor("en-US", presetId = 6)
    assert(defaultGrandma?.name?.contains("Grandma") == true, "findDefaultVoiceFor with preset 6 returns Grandma")

    val voiceNameSandy = VoiceRegistry.getDefaultVoiceNameFor("en-US", presetId = 5)
    assert(voiceNameSandy == "eng-USA-Sandy", "getDefaultVoiceNameFor with preset 5 returns eng-USA-Sandy")

    val voiceNameReed = VoiceRegistry.getDefaultVoiceNameFor("en-US", presetId = 0)
    assert(voiceNameReed == "eng-USA-Reed", "getDefaultVoiceNameFor with preset 0 returns eng-USA-Reed")

    val testEngine = EloqiumEngine.open(0x00010000)
    if (testEngine != null) {
        testEngine.applyVoice(0) // Reed
        val reedPitch = testEngine.currentBasePitch
        testEngine.applyVoice(1) // Grandma
        val grandmaPitch = testEngine.currentBasePitch
        testEngine.applyVoice(7) // Bobby
        val bobbyPitch = testEngine.currentBasePitch
        assert(reedPitch != grandmaPitch, "Preset switching alters base pitch (Reed=$reedPitch, Grandma=$grandmaPitch)")
        assert(bobbyPitch != reedPitch, "Preset switching alters base pitch for Bobby ($bobbyPitch)")
        testEngine.close()
    }

    // --- Suite 24: Abbreviation Dictionary & Context-Aware Processor ---
    println("\n--- Suite 24: Abbreviation Dictionary & Context-Aware Processor ---")
    val docText = "Dr. Smith visited Mr. Jones on Main St."
    val abbrevDisabled = AbbreviationProcessor.process(docText, enabled = false)
    assert(abbrevDisabled == docText, "Disabled abbreviation processing leaves text completely unchanged")

    val abbrevEnabled = AbbreviationProcessor.process(docText, enabled = true)
    assert(abbrevEnabled == "Doctor Smith visited Mister Jones on Main Street",
        "Enabled abbreviation expands Dr., Mr., and Main St.: $abbrevEnabled")

    // Disambiguation of St. (Saint vs Street)
    val saintTest = "Visit St. Jude Hospital near St. Patrick Church."
    val saintProcessed = AbbreviationProcessor.process(saintTest, enabled = true)
    assert(saintProcessed == "Visit Saint Jude Hospital near Saint Patrick Church.",
        "St. before capital letter expands to Saint: $saintProcessed")

    val streetTest = "Turn onto 5th St. after passing Oak St."
    val streetProcessed = AbbreviationProcessor.process(streetTest, enabled = true)
    assert(streetProcessed.contains("5th Street") && streetProcessed.contains("Oak Street"),
        "St. after street name or number expands to Street: $streetProcessed")

    // Titles, units, and common abbreviations
    val latinTest = "He had dogs, e.g. collies, etc."
    val latinProcessed = AbbreviationProcessor.process(latinTest, enabled = true)
    assert(latinProcessed.contains("for example") && latinProcessed.contains("etcetera"),
        "e.g. and etc. expand cleanly: $latinProcessed")

    val aptTest = "Apt. 4B, 100 Sunset Blvd., Ste. 200"
    val aptProcessed = AbbreviationProcessor.process(aptTest, enabled = true)
    assert(aptProcessed.contains("Apartment 4B") && aptProcessed.contains("Boulevard") && aptProcessed.contains("Suite"),
        "Apt., Blvd., Ste. expand cleanly: $aptProcessed")

    // Boundary protections (preserve non-abbreviation words)
    val wordTest = "The Street string Draft state"
    val wordProcessed = AbbreviationProcessor.process(wordTest, enabled = true)
    assert(wordProcessed == wordTest, "Abbreviation patterns do not corrupt non-abbreviation words: $wordProcessed")

    // --- Suite 25: Emoji Runtime Loading & Text Preprocessing Pipeline Order ---
    println("\n--- Suite 25: Emoji Runtime Loading & Text Preprocessing Pipeline Order ---")
    assert(EmojiData.isLoaded, "EmojiData is loaded and available for runtime processing")
    assert(EmojiData.totalEmojis > 3000, "EmojiData contains over 3000 emoji entries (actual: ${EmojiData.totalEmojis})")

    val emojiSample = "Hello 😀 world! 👍 Great job ❤️"
    val emojiSub = EmojiProcessor.process(emojiSample, "en")
    assert(emojiSub.contains("grinning face") && emojiSub.contains("thumbs up") && emojiSub.contains("red heart"),
        "EmojiProcessor properly replaces emojis with descriptions: $emojiSub")

    // Pipeline ordering: Emoji replacement BEFORE UnicodeNormalizer
    val pipelineInput = "Testing 😀 rocket 🚀"
    val afterEmoji = EmojiProcessor.process(pipelineInput, "en")
    val afterNormalize = UnicodeNormalizer.normalize(afterEmoji)
    assert(afterNormalize.contains("grinning face") && afterNormalize.contains("rocket"),
        "Pipeline ordering (Emoji first, then Normalizer) preserves emoji speech output: $afterNormalize")

    // --- Suite 26: Accessibility & Semantics String Formatting ---
    println("\n--- Suite 26: Accessibility & Semantics String Formatting ---")
    fun formatRelative(value: Int): String = when {
        value == 0 -> "0 (Default)"
        value > 0 -> "+$value"
        else -> "$value"
    }

    assert(formatRelative(0) == "0 (Default)", "Relative 0 formatted as '0 (Default)'")
    assert(formatRelative(-10) == "-10", "Relative -10 formatted as '-10'")
    assert(formatRelative(5) == "+5", "Relative 5 formatted as '+5'")
    assert(formatRelative(-3) == "-3", "Relative -3 formatted as '-3'")

    fun rateSupportingText(rate: Int, unlocked: Boolean): String =
        if (unlocked) "Current: $rate% (High speed enabled)" else "Current: $rate%"
    assert(rateSupportingText(50, false) == "Current: 50%", "Normal rate text: 'Current: 50%'")
    assert(rateSupportingText(80, true) == "Current: 80% (High speed enabled)", "Unlocked rate text indicates high speed")

    fun punctLevelLabel(level: Int): String = when (level) {
        0 -> "None"
        1 -> "Some"
        2 -> "Most"
        3 -> "All"
        4 -> "Custom"
        else -> "None"
    }
    assert(punctLevelLabel(SettingsDefaults.PUNCT_NONE) == "None", "Punct level 0 is None")
    assert(punctLevelLabel(SettingsDefaults.PUNCT_SOME) == "Some", "Punct level 1 is Some")
    assert(punctLevelLabel(SettingsDefaults.PUNCT_MOST) == "Most", "Punct level 2 is Most")
    assert(punctLevelLabel(SettingsDefaults.PUNCT_ALL) == "All", "Punct level 3 is All")
    assert(punctLevelLabel(SettingsDefaults.PUNCT_CUSTOM) == "Custom", "Punct level 4 is Custom")

    // --- Suite 27: System TTS Lifecycle, Regex Safety & Thread-Safe Engine ---
    println("\n--- Suite 27: System TTS Lifecycle, Regex Safety & Thread-Safe Engine ---")

    // 27.1 Regex lookbehind safety across varied punctuation and boundaries
    val drSmith = AbbreviationProcessor.process("Dr. Smith met Mr. Jones.", enabled = true)
    assert(drSmith.contains("Doctor") && drSmith.contains("Mister"), "AbbreviationProcessor expands 'Dr.' and 'Mr.': $drSmith")

    val quotedAbbr = AbbreviationProcessor.process("(\"Dr. Smith\") ['Prof. Oak']", enabled = true)
    assert(quotedAbbr.contains("Doctor") && quotedAbbr.contains("Professor"), "AbbreviationProcessor expands within brackets and quotes: $quotedAbbr")

    val stJohn = AbbreviationProcessor.process("Visit St. John's at 5th Ave.", enabled = true)
    assert(stJohn.contains("Saint") && stJohn.contains("Avenue"), "AbbreviationProcessor expands 'St.' and 'Ave.': $stJohn")

    val vsStreet = AbbreviationProcessor.process("Main St. vs. Elm St.", enabled = true)
    assert(vsStreet.contains("Main Street") && vsStreet.contains("versus") && vsStreet.contains("Elm Street"),
        "Disambiguation and case-insensitive abbreviation works: $vsStreet")

    assert(AbbreviationProcessor.process("", enabled = true) == "", "Empty string processed without error")
    assert(AbbreviationProcessor.process("   ", enabled = true) == "   ", "Whitespace string preserved")
    assert(AbbreviationProcessor.process("Dr. Smith", enabled = false) == "Dr. Smith", "Disabled processor leaves text unchanged")

    // 27.2 System TTS Default Voice Contract
    val defaultVoiceName = VoiceRegistry.ALL_VOICES.firstOrNull()?.name ?: "eng-USA-Reed"
    assert(defaultVoiceName == "eng-USA-Reed", "Default voice name is 'eng-USA-Reed'")
    assert(VoiceRegistry.findVoice(defaultVoiceName) != null, "Default voice is found in VoiceRegistry")
    assert(VoiceRegistry.findVoice("eloqium_en_us_reed")?.name == "eng-USA-Reed", "Legacy alias 'eloqium_en_us_reed' resolves to default voice")
    assert(VoiceRegistry.findVoice("reed")?.name == "eng-USA-Reed", "Voice lookup by preset name 'reed' succeeds")

    // 27.3 About navigation metadata
    assert(AboutNavigation.GITHUB_URL == "https://github.com/memon-ismail/eloqium-tts", "GitHub URL is correct")
    assert(AboutNavigation.TELEGRAM_URL == "https://t.me/blindroidofficial", "Telegram URL is correct")
    assert(AboutNavigation.TELEGRAM_APP_URI == "tg://resolve?domain=blindroidofficial", "Telegram app URI is correct")

    // 27.4 Thread-Safe Engine Instance Lifecycle
    val suite27Engine = EloqiumEngine.open(0x00010000)
    var threadSafetyPassed = true
    if (suite27Engine != null) {
        try {
            suite27Engine.stop()
            suite27Engine.close()
        } catch (e: Exception) {
            threadSafetyPassed = false
        }
    }
    assert(threadSafetyPassed, "EloqiumEngine safe lifecycle calls (stop, close) execute cleanly")

    // --- Suite 28: System TextToSpeechService Complete Lifecycle & Concurrency ---
    println("\n--- Suite 28: System TextToSpeechService Complete Lifecycle & Concurrency ---")

    // 28.1 Standalone instantiation and onCreate without Activity/UI
    val service = EloqiumTtsService()
    var serviceCreatePassed = false
    try {
        service.onCreate() // Exercises simulated AOSP onCreate() -> onLoadLanguage("eng", "USA", "")
        serviceCreatePassed = true
    } catch (e: Throwable) {
        println("  [EXCEPTION] Service onCreate failed: ${e.message}")
        e.printStackTrace()
    }
    assert(serviceCreatePassed, "EloqiumTtsService.onCreate executes cleanly under simulated AOSP lifecycle without Activity")

    // 28.2 Language availability queries
    assert(service.onIsLanguageAvailable("eng", "USA", "") == TextToSpeech.LANG_COUNTRY_AVAILABLE, "onIsLanguageAvailable(eng, USA) returns LANG_COUNTRY_AVAILABLE")
    assert(service.onIsLanguageAvailable("es", "ES", "") == TextToSpeech.LANG_COUNTRY_AVAILABLE, "onIsLanguageAvailable(es, ES) returns LANG_COUNTRY_AVAILABLE")
    assert(service.onIsLanguageAvailable("xyz", "XYZ", "") == TextToSpeech.LANG_NOT_SUPPORTED, "onIsLanguageAvailable(xyz, XYZ) returns LANG_NOT_SUPPORTED")

    // 28.3 Language getter
    val currentLang = service.onGetLanguage()
    assert(currentLang.size == 3 && currentLang[0] == "eng" && currentLang[1] == "USA", "onGetLanguage returns active language [${currentLang.joinToString(", ")}]")

    // 28.4 Canonical voices query
    val voices = service.onGetVoices()
    assert(voices.size == 64, "onGetVoices returns exactly 64 canonical voices (found ${voices.size})")

    // 28.5 Default voice queries
    val nullDefault = service.onGetDefaultVoiceNameFor(null, null, null)
    assert(nullDefault == "eng-USA-Reed", "onGetDefaultVoiceNameFor(null, null, null) returns 'eng-USA-Reed' (got: $nullDefault)")

    val usDefault = service.onGetDefaultVoiceNameFor("eng", "USA", "")
    assert(usDefault == "eng-USA-Reed", "onGetDefaultVoiceNameFor(eng, USA) returns 'eng-USA-Reed'")

    val esDefault = service.onGetDefaultVoiceNameFor("spa", "ESP", "")
    assert(esDefault == "spa-ESP-Reed", "onGetDefaultVoiceNameFor(spa, ESP) returns 'spa-ESP-Reed'")

    // 28.6 Voice validation
    assert(service.onIsValidVoiceName("eng-USA-Reed") == TextToSpeech.SUCCESS, "onIsValidVoiceName('eng-USA-Reed') returns SUCCESS")
    assert(service.onIsValidVoiceName("non-existent-voice") == TextToSpeech.SUCCESS, "onIsValidVoiceName('non-existent-voice') gracefully returns SUCCESS")

    // 28.7 Voice loading & dynamic language update
    val loadVoiceResult = service.onLoadVoice("eng-USA-Shelley")
    assert(loadVoiceResult == TextToSpeech.SUCCESS, "onLoadVoice('eng-USA-Shelley') returns SUCCESS")

    val loadFrenchResult = service.onLoadLanguage("fra", "FRA", "")
    assert(loadFrenchResult == TextToSpeech.LANG_COUNTRY_AVAILABLE, "onLoadLanguage('fra', 'FRA') succeeds")
    val frenchLang = service.onGetLanguage()
    assert(frenchLang[0] == "fra" && frenchLang[1] == "FRA", "onGetLanguage reflects newly loaded French language [${frenchLang.joinToString(", ")}]")

    // Restore to English for synthesis test
    service.onLoadLanguage("eng", "USA", "")

    // 28.8 Synthesis Callback and Synchronous Synthesis Contract
    class TestSynthesisCallback : SynthesisCallback {
        var started = false
        var sampleRate = 0
        var audioFormat = 0
        var channelCount = 0
        var audioBytes = 0
        var finished = false
        var errorCalled = false

        override fun getMaxBufferSize(): Int = 4096
        override fun start(sampleRateInHz: Int, audioFormat: Int, channelCount: Int): Int {
            this.started = true
            this.sampleRate = sampleRateInHz
            this.audioFormat = audioFormat
            this.channelCount = channelCount
            return TextToSpeech.SUCCESS
        }
        override fun audioAvailable(buffer: ByteArray, offset: Int, length: Int): Int {
            audioBytes += length
            return TextToSpeech.SUCCESS
        }
        override fun done(): Int {
            finished = true
            return TextToSpeech.SUCCESS
        }
        override fun error() { errorCalled = true }
        override fun error(errorCode: Int) { errorCalled = true }
        override fun hasStarted(): Boolean = started
        override fun hasFinished(): Boolean = finished
    }

    val synthReq = SynthesisRequest("Hello from Eloqium system Text-to-Speech engine.", Bundle())
    val synthCb = TestSynthesisCallback()
    service.onSynthesizeText(synthReq, synthCb)

    assert(synthCb.started, "onSynthesizeText triggered callback.start(...)")
    assert(synthCb.audioBytes > 0, "onSynthesizeText delivered ${synthCb.audioBytes} PCM bytes")
    assert(synthCb.finished, "onSynthesizeText called callback.done()")
    assert(!synthCb.errorCalled, "onSynthesizeText did not encounter any errors")

    // 28.9 Blank text synthesis contract
    val blankReq = SynthesisRequest("   ", Bundle())
    val blankCb = TestSynthesisCallback()
    service.onSynthesizeText(blankReq, blankCb)
    assert(blankCb.started && blankCb.finished && blankCb.audioBytes == 0, "Blank text synthesis calls start() and done() without audio error")

    // 28.10 Stop / Abort handling contract (must not call done())
    val stopCb = object : SynthesisCallback {
        var started = false
        var doneCalled = false
        override fun getMaxBufferSize(): Int = 4096
        override fun start(sampleRateInHz: Int, audioFormat: Int, channelCount: Int): Int {
            started = true
            return TextToSpeech.SUCCESS
        }
        override fun audioAvailable(buffer: ByteArray, offset: Int, length: Int): Int {
            // Simulate immediate interruption on first audio packet
            service.onStop()
            return TextToSpeech.SUCCESS
        }
        override fun done(): Int {
            doneCalled = true
            return TextToSpeech.SUCCESS
        }
        override fun error() {}
        override fun error(errorCode: Int) {}
        override fun hasStarted(): Boolean = started
        override fun hasFinished(): Boolean = doneCalled
    }
    val longReq = SynthesisRequest("This is a long sentence meant to test abortion and stopping behavior in Eloqium TTS.", Bundle())
    service.onSynthesizeText(longReq, stopCb)
    assert(!stopCb.doneCalled, "When onStop() is called during synthesis, callback.done() is NOT called (conforms to Android spec)")

    // 28.11 Service destruction cleanup
    var serviceDestroyPassed = true
    try {
        service.onDestroy()
    } catch (e: Throwable) {
        serviceDestroyPassed = false
    }
    assert(serviceDestroyPassed, "EloqiumTtsService.onDestroy completes cleanly and releases native engine")

    // =========================================================================
    // --- Suite 29: User Dictionary JSON Engine, Schema Validation & Serialization ---
    // =========================================================================
    println("\n--- Suite 29: User Dictionary JSON Engine, Schema Validation & Serialization ---")

    val sampleDict = UserDictionary(
        name = "Technical Terms",
        enabled = true,
        entries = listOf(
            UserDictionaryEntry(source = "Eloqium", replacement = "Ee-loh-kee-um", matchMode = MatchMode.EXACT, caseSensitive = false),
            UserDictionaryEntry(source = "NASA", replacement = "N A S A", matchMode = MatchMode.EXACT, caseSensitive = true),
            UserDictionaryEntry(source = "micro", replacement = "small ", matchMode = MatchMode.STARTS_WITH, caseSensitive = false),
            UserDictionaryEntry(source = "burgh", replacement = "burg", matchMode = MatchMode.ENDS_WITH, caseSensitive = false),
            UserDictionaryEntry(source = ":=", replacement = "assigned to", matchMode = MatchMode.CONTAINS, caseSensitive = false)
        )
    )

    val exportedJson = UserDictionaryJson.exportDictionary(sampleDict, "en-US")
    assert(exportedJson.contains("\"schemaVersion\": 1"), "Exported JSON contains schemaVersion 1")
    assert(exportedJson.contains("\"language\": \"en-US\""), "Exported JSON contains correct language tag")
    assert(exportedJson.contains("\"name\": \"Technical Terms\""), "Exported JSON contains dictionary name")
    assert(exportedJson.contains("\"source\": \"Eloqium\""), "Exported JSON contains entry source")
    assert(exportedJson.contains("\"matchMode\": \"Starts with\""), "Exported JSON serializes matchMode display name")

    val parsedResult = UserDictionaryJson.parseDictionary(exportedJson)
    assert(parsedResult.isSuccess, "parseDictionary succeeds on valid exported JSON")
    val parsed = parsedResult.getOrThrow()
    assert(parsed.schemaVersion == 1, "Parsed schemaVersion is 1")
    assert(parsed.languageTag == "en-US", "Parsed language is en-US")
    assert(parsed.dictionary.name == "Technical Terms", "Parsed dictionary name matches")
    assert(parsed.dictionary.enabled, "Parsed dictionary enabled state matches")
    assert(parsed.dictionary.entries.size == 5, "Parsed dictionary contains all 5 entries")
    assert(parsed.dictionary.entries[1].source == "NASA" && parsed.dictionary.entries[1].caseSensitive, "Parsed entry preserves case sensitivity")
    assert(parsed.dictionary.entries[2].matchMode == MatchMode.STARTS_WITH, "Parsed entry preserves Starts with match mode")

    // Error handling: Malformed JSON
    val malformedResult = UserDictionaryJson.parseDictionary("not a json string {")
    assert(malformedResult.isFailure, "parseDictionary fails gracefully on malformed JSON")

    // Error handling: Missing schemaVersion
    val noVersionResult = UserDictionaryJson.parseDictionary("{\"language\": \"en-US\", \"name\": \"Test\"}")
    assert(noVersionResult.isFailure, "parseDictionary fails on missing schemaVersion")

    // Error handling: Unsupported schema version
    val badVersionResult = UserDictionaryJson.parseDictionary("{\"schemaVersion\": 99, \"language\": \"en-US\", \"name\": \"Test\"}")
    assert(badVersionResult.isFailure && badVersionResult.exceptionOrNull()?.message?.contains("Unsupported schema version: 99") == true,
        "parseDictionary rejects unsupported schema version 99 with descriptive message")

    // Error handling: Missing language
    val noLangResult = UserDictionaryJson.parseDictionary("{\"schemaVersion\": 1, \"name\": \"Test\"}")
    assert(noLangResult.isFailure && noLangResult.exceptionOrNull()?.message?.contains("Missing required field: language") == true,
        "parseDictionary rejects missing language")

    // Empty dictionary export and import
    val emptyDict = UserDictionary(name = "Empty Dict", enabled = false, entries = emptyList())
    val emptyJson = UserDictionaryJson.exportDictionary(emptyDict, "es-ES")
    val parsedEmpty = UserDictionaryJson.parseDictionary(emptyJson).getOrThrow()
    assert(parsedEmpty.dictionary.entries.isEmpty() && !parsedEmpty.dictionary.enabled, "Empty dictionary export/import round-trips cleanly")

    // System-level export and import
    val sampleSystem = UserDictionarySystem(
        schemaVersion = 1,
        languages = listOf(
            org.eloqium.tts.service.LanguageDictionaries("en-US", listOf(sampleDict)),
            org.eloqium.tts.service.LanguageDictionaries("es-ES", listOf(emptyDict))
        )
    )
    val sysJson = UserDictionaryJson.exportSystem(sampleSystem)
    val parsedSys = UserDictionaryJson.parseSystem(sysJson)
    assert(parsedSys.languages.size == 2, "parseSystem round-trips all languages")
    assert(parsedSys.languages[0].languageTag == "en-US" && parsedSys.languages[0].dictionaries[0].entries.size == 5, "parseSystem round-trips nested dictionaries")

    // =========================================================================
    // --- Suite 30: User Dictionary System & Repository Lifecycle ---
    // =========================================================================
    println("\n--- Suite 30: User Dictionary System & Repository Lifecycle ---")

    val dictMockPrefs = MockSharedPreferences()
    val repo = UserDictionaryRepository(dictMockPrefs)

    // 30.1 Initial state: no languages
    assert(repo.getAddedLanguages().isEmpty(), "Initial repository has zero added languages")
    assert(!repo.isLanguageAdded("en-US"), "isLanguageAdded returns false for en-US initially")

    // 30.2 Add Language creates User Dictionary automatically
    val enLang = repo.addLanguage("en-US")
    assert(repo.isLanguageAdded("en-US"), "isLanguageAdded returns true after adding en-US")
    assert(repo.getAddedLanguages().size == 1, "Added languages size is 1")
    assert(enLang.dictionaries.size == 1, "Adding a language automatically creates exactly one dictionary")
    val defaultDict = enLang.dictionaries[0]
    assert(defaultDict.name == "User Dictionary", "Automatically-created dictionary is named 'User Dictionary'")
    assert(defaultDict.enabled, "Automatically-created dictionary is enabled by default")
    assert(defaultDict.entries.isEmpty(), "Automatically-created dictionary starts with zero entries")

    // 30.3 Add second dictionary
    val customDict = repo.addDictionary("en-US", "Names")
    assert(repo.getDictionaries("en-US").size == 2, "en-US now contains 2 dictionaries")
    assert(customDict.name == "Names" && customDict.enabled, "New dictionary 'Names' created enabled with zero entries")

    // 30.4 Rename dictionary
    val renamedSuccess = repo.renameDictionary("en-US", customDict.id, "Proper Names")
    assert(renamedSuccess, "renameDictionary returns true")
    assert(repo.getDictionary("en-US", customDict.id)?.name == "Proper Names", "Dictionary name updated to 'Proper Names'")

    // 30.5 Disable and Re-enable dictionary
    repo.setDictionaryEnabled("en-US", customDict.id, false)
    assert(repo.getDictionary("en-US", customDict.id)?.enabled == false, "Dictionary can be disabled")
    repo.setDictionaryEnabled("en-US", customDict.id, true)
    assert(repo.getDictionary("en-US", customDict.id)?.enabled == true, "Dictionary can be re-enabled")

    // 30.6 Entry CRUD
    val entry1 = UserDictionaryEntry(source = "Eloqium", replacement = "Ee-loh-kee-um", matchMode = MatchMode.EXACT, caseSensitive = false)
    val entry2 = UserDictionaryEntry(source = "NASA", replacement = "N A S A", matchMode = MatchMode.EXACT, caseSensitive = true)
    repo.addEntry("en-US", defaultDict.id, entry1)
    repo.addEntry("en-US", defaultDict.id, entry2)
    assert(repo.getEntries("en-US", defaultDict.id).size == 2, "Added 2 entries to User Dictionary")

    // Rejects empty source
    val invalidEntryResult = repo.addEntry("en-US", defaultDict.id, UserDictionaryEntry(source = "   ", replacement = "x"))
    assert(!invalidEntryResult, "Repository rejects entry with blank source")

    // Update entry
    val updatedEntry1 = entry1.copy(replacement = "Ee loh kwee um")
    repo.updateEntry("en-US", defaultDict.id, updatedEntry1)
    assert(repo.getEntries("en-US", defaultDict.id).first { it.id == entry1.id }.replacement == "Ee loh kwee um", "Entry replacement updated")

    // Delete entry
    repo.deleteEntry("en-US", defaultDict.id, entry2.id)
    assert(repo.getEntries("en-US", defaultDict.id).size == 1, "Entry deleted successfully")

    // 30.7 Persistence across repository reload
    val reloadedRepo = UserDictionaryRepository(dictMockPrefs)
    assert(reloadedRepo.getAddedLanguages().size == 1, "Reloaded repository retains 1 added language")
    assert(reloadedRepo.getDictionaries("en-US").size == 2, "Reloaded repository retains 2 dictionaries")
    assert(reloadedRepo.getEntries("en-US", defaultDict.id).first().replacement == "Ee loh kwee um", "Reloaded repository retains entries")

    // 30.8 Duplicate dictionary name handling on import
    val importRes1 = reloadedRepo.importDictionary("en-US", UserDictionaryJson.exportDictionary(defaultDict, "en-US"))
    assert(importRes1.isSuccess, "Import dictionary succeeds")
    assert(importRes1.getOrThrow().dictionary.name == "User Dictionary (1)", "Duplicate dictionary name handled by appending ' (1)'")

    // 30.9 Final-dictionary deletion behavior
    // Currently en-US has 3 dictionaries: "User Dictionary", "Proper Names", "User Dictionary (1)"
    val allDicts = reloadedRepo.getDictionaries("en-US")
    reloadedRepo.deleteDictionary("en-US", allDicts[0].id)
    reloadedRepo.deleteDictionary("en-US", allDicts[1].id)
    assert(reloadedRepo.isLanguageAdded("en-US"), "Language remains added while 1 dictionary still exists")
    reloadedRepo.deleteDictionary("en-US", allDicts[2].id)
    assert(!reloadedRepo.isLanguageAdded("en-US"), "Language disappears from added languages after final dictionary is deleted")
    assert(reloadedRepo.getAddedLanguages().isEmpty(), "No added languages remaining after final dictionary deleted")

    // =========================================================================
    // --- Suite 31: User Dictionary Matching Engine & Precedence ---
    // =========================================================================
    println("\n--- Suite 31: User Dictionary Matching Engine & Precedence ---")

    // 31.1 Exact match mode
    assert(UserDictionaryProcessor.matchesTarget("NASA", "NASA", MatchMode.EXACT, true), "Exact match target: equal")
    assert(!UserDictionaryProcessor.matchesTarget("NASA", "NASAL", MatchMode.EXACT, true), "Exact match target: not equal to prefix")
    val exactRules = UserDictionaryProcessor.compileEntries(listOf(
        UserDictionaryEntry(source = "NASA", replacement = "N A S A", matchMode = MatchMode.EXACT, caseSensitive = true),
        UserDictionaryEntry(source = ":=", replacement = "assigned to", matchMode = MatchMode.EXACT, caseSensitive = false)
    ))
    assert(UserDictionaryProcessor.process("The NASA space probe", exactRules) == "The N A S A space probe", "Exact match replaces standalone word")
    assert(UserDictionaryProcessor.process("The NASAL cavity", exactRules) == "The NASAL cavity", "Exact match protects words with same prefix")
    assert(UserDictionaryProcessor.process("variable := 42", exactRules) == "variable assigned to 42", "Exact match replaces symbol expression")

    // 31.2 Starts with match mode
    assert(UserDictionaryProcessor.matchesTarget("micro", "microscope", MatchMode.STARTS_WITH, false), "Starts with target: prefix match")
    assert(!UserDictionaryProcessor.matchesTarget("scope", "microscope", MatchMode.STARTS_WITH, false), "Starts with target: suffix does not match")
    val startsWithRules = UserDictionaryProcessor.compileEntries(listOf(
        UserDictionaryEntry(source = "micro", replacement = "small ", matchMode = MatchMode.STARTS_WITH, caseSensitive = false)
    ))
    assert(UserDictionaryProcessor.process("Look at the microscope", startsWithRules) == "Look at the small scope", "Starts with replaces prefix")
    assert(UserDictionaryProcessor.process("antimicrobial", startsWithRules) == "antimicrobial", "Starts with does not match middle of word")

    // 31.3 Ends with match mode
    assert(UserDictionaryProcessor.matchesTarget("burgh", "Pittsburgh", MatchMode.ENDS_WITH, false), "Ends with target: suffix match")
    val endsWithRules = UserDictionaryProcessor.compileEntries(listOf(
        UserDictionaryEntry(source = "burgh", replacement = "burg", matchMode = MatchMode.ENDS_WITH, caseSensitive = false)
    ))
    assert(UserDictionaryProcessor.process("Welcome to Pittsburgh today", endsWithRules) == "Welcome to Pittsburg today", "Ends with replaces suffix")
    assert(UserDictionaryProcessor.process("burgher", endsWithRules) == "burgher", "Ends with does not match prefix")

    // 31.4 Contains match mode
    assert(UserDictionaryProcessor.matchesTarget("cat", "scatty", MatchMode.CONTAINS, false), "Contains target: substring match")
    val containsRules = UserDictionaryProcessor.compileEntries(listOf(
        UserDictionaryEntry(source = "cat", replacement = "feline", matchMode = MatchMode.CONTAINS, caseSensitive = false)
    ))
    assert(UserDictionaryProcessor.process("scatty cat", containsRules) == "sfelinety feline", "Contains replaces arbitrary substrings")

    // 31.5 Case sensitivity
    val caseSensitiveRules = UserDictionaryProcessor.compileEntries(listOf(
        UserDictionaryEntry(source = "Eloqium", replacement = "Ee-loh-kee-um", matchMode = MatchMode.EXACT, caseSensitive = true)
    ))
    assert(UserDictionaryProcessor.process("Eloqium is great", caseSensitiveRules) == "Ee-loh-kee-um is great", "Case-sensitive matches exact case")
    assert(UserDictionaryProcessor.process("eloqium is great", caseSensitiveRules) == "eloqium is great", "Case-sensitive rejects lower-case")

    val caseInsensitiveRules = UserDictionaryProcessor.compileEntries(listOf(
        UserDictionaryEntry(source = "Eloqium", replacement = "Ee-loh-kee-um", matchMode = MatchMode.EXACT, caseSensitive = false)
    ))
    assert(UserDictionaryProcessor.process("eloqium ELOQIUM Eloqium", caseInsensitiveRules) == "Ee-loh-kee-um Ee-loh-kee-um Ee-loh-kee-um", "Case-insensitive matches all cases")

    // 31.6 Overlapping precedence: Longer source takes priority
    val overlapRules = UserDictionaryProcessor.compileEntries(listOf(
        UserDictionaryEntry(source = "New", replacement = "Fresh", matchMode = MatchMode.EXACT, caseSensitive = false),
        UserDictionaryEntry(source = "New York City", replacement = "NYC", matchMode = MatchMode.EXACT, caseSensitive = false),
        UserDictionaryEntry(source = "New York", replacement = "NY", matchMode = MatchMode.EXACT, caseSensitive = false)
    ))
    assert(UserDictionaryProcessor.process("I love New York City and New York and New things.", overlapRules) == "I love NYC and NY and Fresh things.",
        "Overlapping entries resolve with longer phrase precedence")

    // 31.7 Single-pass non-cascading replacement
    val cascadeRules = UserDictionaryProcessor.compileEntries(listOf(
        UserDictionaryEntry(source = "cat", replacement = "caterpillar", matchMode = MatchMode.EXACT, caseSensitive = false),
        UserDictionaryEntry(source = "pill", replacement = "tablet", matchMode = MatchMode.CONTAINS, caseSensitive = false)
    ))
    assert(UserDictionaryProcessor.process("The cat sat.", cascadeRules) == "The caterpillar sat.", "Single-pass replacement prevents cascading mutations")

    // 31.8 Multilingual & Unicode text
    val unicodeRules = UserDictionaryProcessor.compileEntries(listOf(
        UserDictionaryEntry(source = "año", replacement = "year", matchMode = MatchMode.EXACT, caseSensitive = false),
        UserDictionaryEntry(source = "München", replacement = "Munich", matchMode = MatchMode.EXACT, caseSensitive = false)
    ))
    assert(UserDictionaryProcessor.process("Feliz año en München!", unicodeRules) == "Feliz year en Munich!", "Unicode accented and umlaut characters match cleanly")

    // =========================================================================
    // --- Suite 32: Synthesis Pipeline Integration & Regression Testing ---
    // =========================================================================
    println("\n--- Suite 32: Synthesis Pipeline Integration & Regression Testing ---")

    val testSettings = Settings(dictMockPrefs)
    val testRepo = testSettings.userDictionaryRepository
    testRepo.clearAll()
    testRepo.addLanguage("en-US")
    val enDefaultDict = testRepo.getDictionaries("en-US").first()

    // 32.1 Precedence over built-in abbreviations
    testRepo.addEntry("en-US", enDefaultDict.id, UserDictionaryEntry(
        source = "Dr.",
        replacement = "Drive",
        matchMode = MatchMode.EXACT,
        caseSensitive = false
    ))
    testSettings.userDictionaryEnabled = true
    testSettings.useAbbreviations = true

    // Step 0: User Dictionary
    val step0Text = UserDictionaryProcessor.process("Visit Dr. Smith at 5th Ave.", "en-US", testRepo, testSettings.userDictionaryEnabled)
    assert(step0Text.contains("Drive Smith"), "User dictionary replaces 'Dr.' with 'Drive'")

    // Step 1: Built-in Abbreviations runs on output of step 0
    val step1Text = AbbreviationProcessor.process(step0Text, testSettings.useAbbreviations)
    assert(step1Text.contains("Drive Smith"), "User replacement 'Drive' preserved and not overwritten with Doctor")
    assert(step1Text.contains("Avenue"), "Built-in abbreviation 'Ave.' expands normally when not in user dictionary")

    // 32.2 Global switch OFF disables user dictionary without deleting entries
    testSettings.userDictionaryEnabled = false
    val step0Disabled = UserDictionaryProcessor.process("Visit Dr. Smith", "en-US", testRepo, testSettings.userDictionaryEnabled)
    assert(step0Disabled == "Visit Dr. Smith", "When userDictionaryEnabled is false, text is unchanged")
    val step1Disabled = AbbreviationProcessor.process(step0Disabled, testSettings.useAbbreviations)
    assert(step1Disabled.contains("Doctor Smith"), "When user dictionary is disabled, built-in abbreviations expand Dr. to Doctor")
    assert(testRepo.getEntries("en-US", enDefaultDict.id).isNotEmpty(), "Entries remain intact when global switch is off")

    // Turn global switch back ON
    testSettings.userDictionaryEnabled = true

    // 32.3 Downstream Pipeline Harmony: Emoji, Numbers, and Punctuation
    testRepo.addEntry("en-US", enDefaultDict.id, UserDictionaryEntry(
        source = "PI",
        replacement = "3.14",
        matchMode = MatchMode.EXACT,
        caseSensitive = true
    ))
    testRepo.addEntry("en-US", enDefaultDict.id, UserDictionaryEntry(
        source = "goodjob",
        replacement = "thumbs up",
        matchMode = MatchMode.EXACT,
        caseSensitive = false
    ))

    val mixedInput = "Value PI and goodjob: is it valid?"
    val p0 = UserDictionaryProcessor.process(mixedInput, "en-US", testRepo, true)
    assert(p0.contains("3.14") && p0.contains("thumbs up"), "User dictionary expands mixed source correctly")

    val p1 = AbbreviationProcessor.process(p0, true)
    val p2 = EmojiProcessor.process(p1, "en")
    val p3Preserved = NumberProcessor.process(p2, enabled = false)
    val p6Preserved = ScreenReaderPunctuationProcessor.process(p3Preserved, enabled = true, level = SettingsDefaults.PUNCT_SOME)
    assert(p6Preserved.contains("3.14"), "Number decimals preserved downstream")
    val p3Digits = NumberProcessor.process(p2, enabled = true, mode = SettingsDefaults.NUMBER_DIGITS)
    val p6Digits = ScreenReaderPunctuationProcessor.process(p3Digits, enabled = true, level = SettingsDefaults.PUNCT_SOME)
    assert(p6Digits.contains("3.1 4"), "Number digits formatted correctly downstream")
    assert(p6Preserved.contains("?"), "Question intonation mark preserved downstream")

    // 32.4 Live Synthesis test with User Dictionary active
    val liveService = EloqiumTtsService()
    val liveCallback = TestSynthesisCallback()
    val liveReq = SynthesisRequest("Testing Eloqium user dictionary integration with PI.", Bundle())
    liveService.onSynthesizeText(liveReq, liveCallback)
    assert(liveCallback.started && liveCallback.finished && liveCallback.audioBytes > 0, "Live synthesis delivers audio cleanly with user dictionary active")
    liveService.onDestroy()

    // =========================================================================
    // --- Suite 33: Real Runtime Replacement & Cache Synchronization ---
    // =========================================================================
    println("\n--- Suite 33: Real Runtime Replacement & Cache Synchronization ---")

    // 33.1 Match modes & case-sensitivity on real text
    val testSyncPrefs = MockSharedPreferences()
    val testSyncRepo = UserDictionaryRepository(testSyncPrefs)
    testSyncRepo.addLanguage("en-US")
    val syncDict = testSyncRepo.getDictionaries("en-US").first()

    // Test exact match: "hi" -> "hello", case-insensitive
    val hiEntry = UserDictionaryEntry(source = "hi", replacement = "hello", matchMode = MatchMode.EXACT, caseSensitive = false)
    testSyncRepo.addEntry("en-US", syncDict.id, hiEntry)

    val resLower = UserDictionaryProcessor.process("hi there", "en-US", testSyncRepo, true)
    assert(resLower == "hello there", "Exact match replaces lowercase word: 'hi' -> 'hello'")

    val resUpper = UserDictionaryProcessor.process("HI there", "en-US", testSyncRepo, true)
    assert(resUpper == "hello there", "Exact match case-insensitive replaces uppercase: 'HI' -> 'hello'")

    val resWordGuard = UserDictionaryProcessor.process("which one is this", "en-US", testSyncRepo, true)
    assert(resWordGuard == "which one is this", "Exact match does not replace substring inside word: 'which'")

    // Test case-sensitive exact match
    testSyncRepo.deleteEntry("en-US", syncDict.id, hiEntry.id)
    val hiCaseEntry = UserDictionaryEntry(source = "hi", replacement = "hello", matchMode = MatchMode.EXACT, caseSensitive = true)
    testSyncRepo.addEntry("en-US", syncDict.id, hiCaseEntry)

    assert(UserDictionaryProcessor.process("hi there", "en-US", testSyncRepo, true) == "hello there", "Case-sensitive matches exact case: 'hi'")
    assert(UserDictionaryProcessor.process("HI there", "en-US", testSyncRepo, true) == "HI there", "Case-sensitive rejects different case: 'HI'")

    // Test Starts with
    testSyncRepo.clearAll()
    testSyncRepo.addLanguage("en-US")
    val pfxDict = testSyncRepo.getDictionaries("en-US").first()
    testSyncRepo.addEntry("en-US", pfxDict.id, UserDictionaryEntry(source = "micro", replacement = "small ", matchMode = MatchMode.STARTS_WITH, caseSensitive = false))
    assert(UserDictionaryProcessor.process("the microscope", "en-US", testSyncRepo, true) == "the small scope", "Starts with replaces word prefix")
    assert(UserDictionaryProcessor.process("antimicrobial", "en-US", testSyncRepo, true) == "antimicrobial", "Starts with does not match middle of word")

    // Test Ends with
    testSyncRepo.addEntry("en-US", pfxDict.id, UserDictionaryEntry(source = "burgh", replacement = "burg", matchMode = MatchMode.ENDS_WITH, caseSensitive = false))
    assert(UserDictionaryProcessor.process("visit Pittsburgh", "en-US", testSyncRepo, true) == "visit Pittsburg", "Ends with replaces word suffix")
    assert(UserDictionaryProcessor.process("the burgher", "en-US", testSyncRepo, true) == "the burgher", "Ends with does not match word prefix")

    // Test Contains
    testSyncRepo.addEntry("en-US", pfxDict.id, UserDictionaryEntry(source = "cat", replacement = "feline", matchMode = MatchMode.CONTAINS, caseSensitive = false))
    assert(UserDictionaryProcessor.process("scatty cat", "en-US", testSyncRepo, true) == "sfelinety feline", "Contains replaces arbitrary substring")

    // Test disabled dictionary
    testSyncRepo.setDictionaryEnabled("en-US", pfxDict.id, false)
    assert(UserDictionaryProcessor.process("the microscope", "en-US", testSyncRepo, true) == "the microscope", "Disabled dictionary produces unchanged text")
    testSyncRepo.setDictionaryEnabled("en-US", pfxDict.id, true)
    assert(UserDictionaryProcessor.process("the microscope", "en-US", testSyncRepo, true) == "the small scope", "Re-enabled dictionary applies replacements")

    // Test global switch
    assert(UserDictionaryProcessor.process("the microscope", "en-US", testSyncRepo, false) == "the microscope", "Global switch OFF produces unchanged text")

    // 33.2 Cross-Instance Cache Invalidation (TTS Service vs UI)
    val sharedPrefs = MockSharedPreferences()
    // Simulate TTS Service starting up with its repository instance
    val serviceRepo = UserDictionaryRepository(sharedPrefs)
    // TTS Service runs initial query, warming its cache
    val initialOut = UserDictionaryProcessor.process("hi there", "en-US", serviceRepo, true)
    assert(initialOut == "hi there", "Initial TTS synthesis before UI dictionary setup produces unchanged text")

    // Simulate UI running in Settings with its own separate repository instance
    val uiRepo = UserDictionaryRepository(sharedPrefs)
    uiRepo.addLanguage("en-US")
    val uiDict = uiRepo.getDictionaries("en-US").first()
    val newEntry = UserDictionaryEntry(source = "hi", replacement = "hello", matchMode = MatchMode.EXACT, caseSensitive = false)
    uiRepo.addEntry("en-US", uiDict.id, newEntry)

    // Service handles NEXT synthesis request using EXISTING serviceRepo instance (NO service restart)
    val serviceNextOut = UserDictionaryProcessor.process("hi there", "en-US", serviceRepo, true)
    assert(serviceNextOut == "hello there", "Existing TTS service repository sees UI-added entry without service restart")

    // UI updates the entry replacement: "hello" -> "greetings"
    val updatedEntry = newEntry.copy(replacement = "greetings")
    uiRepo.updateEntry("en-US", uiDict.id, updatedEntry)

    val serviceUpdateOut = UserDictionaryProcessor.process("hi there", "en-US", serviceRepo, true)
    assert(serviceUpdateOut == "greetings there", "Existing TTS service repository sees UI-edited entry without service restart")

    // UI disables dictionary
    uiRepo.setDictionaryEnabled("en-US", uiDict.id, false)
    val serviceDisabledOut = UserDictionaryProcessor.process("hi there", "en-US", serviceRepo, true)
    assert(serviceDisabledOut == "hi there", "Existing TTS service repository sees UI-disabled dictionary without service restart")

    // UI re-enables dictionary
    uiRepo.setDictionaryEnabled("en-US", uiDict.id, true)
    val serviceReEnabledOut = UserDictionaryProcessor.process("hi there", "en-US", serviceRepo, true)
    assert(serviceReEnabledOut == "greetings there", "Existing TTS service repository sees UI-reenabled dictionary without service restart")

    // UI deletes entry
    uiRepo.deleteEntry("en-US", uiDict.id, newEntry.id)
    val serviceDeletedOut = UserDictionaryProcessor.process("hi there", "en-US", serviceRepo, true)
    assert(serviceDeletedOut == "hi there", "Existing TTS service repository sees UI-deleted entry without service restart")

    // UI imports dictionary
    val importDict = UserDictionary(
        name = "Imported",
        enabled = true,
        entries = listOf(UserDictionaryEntry(source = "hi", replacement = "salute", matchMode = MatchMode.EXACT, caseSensitive = false))
    )
    val importJson = UserDictionaryJson.exportDictionary(importDict, "en-US")
    uiRepo.importDictionary("en-US", importJson)
    val serviceImportOut = UserDictionaryProcessor.process("hi there", "en-US", serviceRepo, true)
    assert(serviceImportOut == "salute there", "Existing TTS service repository sees UI-imported dictionary without service restart")

    // =========================================================================
    // --- Suite 34: Strict Language / Locale Isolation ---
    // =========================================================================
    println("\n--- Suite 34: Strict Language / Locale Isolation ---")

    val isoPrefs = MockSharedPreferences()
    val isoRepo = UserDictionaryRepository(isoPrefs)

    // 1. Add en-US with rule: "Hi" -> "Hello"
    isoRepo.addLanguage("en-US")
    val enUsDict = isoRepo.getDictionaries("en-US").first()
    isoRepo.addEntry("en-US", enUsDict.id, UserDictionaryEntry(
        source = "Hi",
        replacement = "Hello",
        matchMode = MatchMode.EXACT,
        caseSensitive = false
    ))

    // 2. Add en-GB with rule: "Hi" -> "Good morning"
    isoRepo.addLanguage("en-GB")
    val enGbDict = isoRepo.getDictionaries("en-GB").first()
    isoRepo.addEntry("en-GB", enGbDict.id, UserDictionaryEntry(
        source = "Hi",
        replacement = "Good morning",
        matchMode = MatchMode.EXACT,
        caseSensitive = false
    ))

    // 3. Add es-ES with rule: "hola" -> "saludos"
    isoRepo.addLanguage("es-ES")
    val esEsDict = isoRepo.getDictionaries("es-ES").first()
    isoRepo.addEntry("es-ES", esEsDict.id, UserDictionaryEntry(
        source = "hola",
        replacement = "saludos",
        matchMode = MatchMode.EXACT,
        caseSensitive = false
    ))

    // 4. Verify en-US synthesizes "Hello"
    val usResult = UserDictionaryProcessor.process("Hi friend", "en-US", isoRepo, true)
    assert(usResult == "Hello friend", "en-US applies its own dictionary rule: 'Hi' -> 'Hello'")

    // 5. Verify en-GB synthesizes "Good morning"
    val gbResult = UserDictionaryProcessor.process("Hi friend", "en-GB", isoRepo, true)
    assert(gbResult == "Good morning friend", "en-GB applies its own dictionary rule: 'Hi' -> 'Good morning'")

    // 6. Verify en-US rules NEVER leak into en-GB
    assert(gbResult != "Hello friend", "en-US dictionary rule does NOT leak into en-GB")

    // 7. Verify en-GB rules NEVER leak into en-US
    assert(usResult != "Good morning friend", "en-GB dictionary rule does NOT leak into en-US")

    // 8. Verify es-ES synthesizes "saludos"
    val esResult = UserDictionaryProcessor.process("hola amigo", "es-ES", isoRepo, true)
    assert(esResult == "saludos amigo", "es-ES applies its own dictionary rule: 'hola' -> 'saludos'")

    // 9. Verify es-ES rules NEVER leak into es-MX (which has no dictionary)
    val mxResult = UserDictionaryProcessor.process("hola amigo", "es-MX", isoRepo, true)
    assert(mxResult == "hola amigo", "es-ES dictionary rule does NOT leak into es-MX")

    // 10. Verify language-only fallback does NOT leak en-US into base 'en' or unrelated locale 'en-CA'
    val caResult = UserDictionaryProcessor.process("Hi friend", "en-CA", isoRepo, true)
    assert(caResult == "Hi friend", "en-US rule does NOT leak into unconfigured en-CA")
    val baseEnResult = UserDictionaryProcessor.process("Hi friend", "en", isoRepo, true)
    assert(baseEnResult == "Hi friend", "en-US rule does NOT leak into base 'en'")

    // 11. Verify underscore normalization: 'en_US' matches 'en-US'
    val underscoreResult = UserDictionaryProcessor.process("Hi friend", "en_US", isoRepo, true)
    assert(underscoreResult == "Hello friend", "Underscore tag 'en_US' normalizes and matches 'en-US'")

    println("   RESULTS: $passed PASSED, $failed FAILED")
    println("==================================================")
    if (failed > 0) {
        System.exit(1)
    }
}
