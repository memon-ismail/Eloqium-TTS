package org.eloqium.tts.service

import android.content.SharedPreferences
import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import org.eloqium.tts.engine.AudioGainProcessor
import org.eloqium.tts.engine.Eci
import org.eloqium.tts.engine.EloqiumEngine
import org.eloqium.tts.engine.LocaleMatcher
import org.eloqium.tts.engine.SpeechRateMapper
import org.eloqium.tts.engine.VoiceParameterMapper
import org.eloqium.tts.engine.VoiceRegistry
import org.eloqium.tts.pipeline.AbbreviationProcessor
import org.eloqium.tts.pipeline.Chunker
import org.eloqium.tts.pipeline.EmojiData
import org.eloqium.tts.pipeline.EmojiProcessor
import org.eloqium.tts.pipeline.EmoticonProcessor
import org.eloqium.tts.pipeline.NumberProcessor
import org.eloqium.tts.pipeline.OpenEVVCompatibilityFixes
import org.eloqium.tts.pipeline.OpenEVVEncoder
import org.eloqium.tts.pipeline.PauseProcessor
import org.eloqium.tts.pipeline.ScreenReaderPunctuationProcessor
import org.eloqium.tts.pipeline.TextRequest
import org.eloqium.tts.pipeline.UnicodeNormalizer

class EloqiumTtsService : TextToSpeechService() {

    @Volatile private var _settings: Settings? = null
    private val settings: Settings
        get() {
            var s = _settings
            if (s == null) {
                synchronized(this) {
                    s = _settings
                    if (s == null) {
                        s = Settings(this)
                        _settings = s
                    }
                }
            }
            return s!!
        }

    private var engine: EloqiumEngine? = null
    private val engineLock = Any()
    @Volatile private var stopped = false

    @Volatile private var activeVoiceDesc: VoiceRegistry.VoiceDescriptor? = null
    @Volatile private var activeLanguage: LocaleMatcher.LanguageEntry = LocaleMatcher.ENTRIES.first()

    override fun onCreate() {
        Log.i(TAG, "onCreate: Initializing Eloqium TTS Service")
        try {
            _settings = Settings(this)
            Log.i(TAG, "onCreate: Settings initialized early (voiceProfile=${_settings?.voiceProfile})")
        } catch (t: Throwable) {
            Log.e(TAG, "onCreate: Failed early settings initialization, will use defaults", t)
        }
        super.onCreate()
        Log.i(TAG, "onCreate: Eloqium TTS Service created successfully")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: Destroying Eloqium TTS Service")
        synchronized(engineLock) {
            try {
                engine?.close()
            } catch (t: Throwable) {
                Log.e(TAG, "onDestroy: Error closing engine", t)
            } finally {
                engine = null
            }
        }
        super.onDestroy()
        Log.i(TAG, "onDestroy: Cleaned up Eloqium TTS Service")
    }

    private val currentVoiceProfile: Int
        get() = try {
            settings.voiceProfile
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read voice profile, using default", t)
            SettingsDefaults.DEFAULT_VOICE_PROFILE
        }

    private fun getShapeFromSettings(): Map<Int, Int> = try {
        mapOf(
            Eci.VOICE_PITCH_FLUCTUATION to VoiceParameterMapper.mapInflection(settings.inflection),
            Eci.VOICE_HEAD_SIZE to VoiceParameterMapper.mapHeadSize(settings.headSize),
            Eci.VOICE_ROUGHNESS to VoiceParameterMapper.mapRoughness(settings.roughness),
            Eci.VOICE_BREATHINESS to VoiceParameterMapper.mapBreathiness(settings.breathiness)
        )
    } catch (t: Throwable) {
        Log.w(TAG, "Failed to read voice shape from settings, using baseline defaults", t)
        mapOf(
            Eci.VOICE_PITCH_FLUCTUATION to VoiceParameterMapper.mapInflection(0),
            Eci.VOICE_HEAD_SIZE to VoiceParameterMapper.mapHeadSize(0),
            Eci.VOICE_ROUGHNESS to VoiceParameterMapper.mapRoughness(0),
            Eci.VOICE_BREATHINESS to VoiceParameterMapper.mapBreathiness(0)
        )
    }

    // ---- Language & Voice Query ---------------------------------------------

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        return try {
            val result = LocaleMatcher.availability(lang, country, variant)
            Log.i(TAG, "onIsLanguageAvailable(lang=$lang, country=$country, variant=$variant) -> $result")
            result
        } catch (t: Throwable) {
            Log.e(TAG, "onIsLanguageAvailable failed for lang=$lang, country=$country, variant=$variant", t)
            TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onGetLanguage(): Array<String> {
        return try {
            val entry = activeVoiceDesc?.entry ?: activeLanguage
            val result = arrayOf(entry.iso3Lang, entry.iso3Country, "")
            Log.i(TAG, "onGetLanguage -> [${result.joinToString(", ")}]")
            result
        } catch (t: Throwable) {
            Log.e(TAG, "onGetLanguage failed, using fallback [eng, USA, ]", t)
            arrayOf("eng", "USA", "")
        }
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        Log.i(TAG, "onLoadLanguage: entry lang=$lang, country=$country, variant=$variant")
        return try {
            val match = LocaleMatcher.matchLanguage(lang, country, variant)
            if (match != null) {
                val entry = match.first
                activeLanguage = entry
                val defaultVoice = VoiceRegistry.findDefaultVoiceFor(entry, currentVoiceProfile)
                activeVoiceDesc = defaultVoice
                Log.i(TAG, "onLoadLanguage: exit -> SUCCESS (eciId=0x${Integer.toHexString(entry.eciId)}, voice=${defaultVoice.name})")
                match.second
            } else {
                Log.w(TAG, "onLoadLanguage: exit -> LANG_NOT_SUPPORTED (lang=$lang, country=$country, variant=$variant)")
                TextToSpeech.LANG_NOT_SUPPORTED
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onLoadLanguage: uncaught exception (lang=$lang, country=$country, variant=$variant)", t)
            TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onGetVoices(): MutableList<Voice> {
        return try {
            val voices = VoiceRegistry.ALL_VOICES.map { it.voice }.toMutableList()
            Log.i(TAG, "onGetVoices -> Returning ${voices.size} canonical voices")
            voices
        } catch (t: Throwable) {
            Log.e(TAG, "onGetVoices: failed to return voice list", t)
            mutableListOf()
        }
    }

    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String? {
        return try {
            if (lang.isNullOrBlank() && country.isNullOrBlank() && variant.isNullOrBlank()) {
                val defaultDesc = activeVoiceDesc ?: VoiceRegistry.findDefaultVoiceFor(activeLanguage, currentVoiceProfile)
                Log.i(TAG, "onGetDefaultVoiceNameFor(null/empty) -> ${defaultDesc.name}")
                return defaultDesc.name
            }
            val voiceName = VoiceRegistry.getDefaultVoiceNameFor(lang, country, variant, currentVoiceProfile)
            Log.i(TAG, "onGetDefaultVoiceNameFor(lang=$lang, country=$country, variant=$variant) -> $voiceName")
            voiceName
        } catch (t: Throwable) {
            Log.e(TAG, "onGetDefaultVoiceNameFor failed (lang=$lang, country=$country, variant=$variant)", t)
            VoiceRegistry.ALL_VOICES.firstOrNull()?.name ?: "eng-USA-Reed"
        }
    }

    override fun onLoadVoice(voiceName: String?): Int {
        Log.i(TAG, "onLoadVoice: entry voiceName=$voiceName")
        return try {
            val desc = VoiceRegistry.findVoice(voiceName) ?: VoiceRegistry.findDefaultVoiceFor(activeLanguage, currentVoiceProfile)
            activeVoiceDesc = desc
            activeLanguage = desc.entry
            Log.i(TAG, "onLoadVoice: exit -> loaded voice=${desc.name}, eciId=0x${Integer.toHexString(desc.entry.eciId)}")
            TextToSpeech.SUCCESS
        } catch (t: Throwable) {
            Log.e(TAG, "onLoadVoice: uncaught exception (voiceName=$voiceName)", t)
            TextToSpeech.ERROR
        }
    }

    override fun onIsValidVoiceName(voiceName: String?): Int {
        return try {
            val isValid = VoiceRegistry.findVoice(voiceName) != null
            Log.i(TAG, "onIsValidVoiceName(voiceName=$voiceName) -> ${if (isValid) "VALID" else "FALLBACK_VALID"}")
            TextToSpeech.SUCCESS
        } catch (t: Throwable) {
            Log.e(TAG, "onIsValidVoiceName failed (voiceName=$voiceName)", t)
            TextToSpeech.SUCCESS
        }
    }

    override fun onGetFeaturesForLanguage(lang: String?, country: String?, variant: String?): MutableSet<String> {
        Log.i(TAG, "onGetFeaturesForLanguage(lang=$lang, country=$country, variant=$variant)")
        return mutableSetOf()
    }

    private fun ensureEngine(languageId: Int): EloqiumEngine? = synchronized(engineLock) {
        val existing = engine
        if (existing != null && existing.language == languageId) {
            return existing
        }
        Log.i(TAG, "ensureEngine: Opening engine for languageId=0x${Integer.toHexString(languageId)}")
        existing?.close()
        val created = EloqiumEngine.open(languageId)
        engine = created
        if (created == null) {
            Log.e(TAG, "ensureEngine: Failed to open native engine for languageId=0x${Integer.toHexString(languageId)}")
        }
        return created
    }

    // ---- Synthesis ----------------------------------------------------------

    override fun onStop() {
        Log.i(TAG, "onStop: synthesis abort requested")
        stopped = true
        synchronized(engineLock) {
            try {
                engine?.stop()
            } catch (t: Throwable) {
                Log.e(TAG, "onStop: error stopping engine", t)
            }
        }
    }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return
        stopped = false

        try {
            // 1. Language Precedence (Force language)
            val entry = if (settings.forceLanguage) {
                val parts = settings.language.split("-")
                val l = parts.getOrNull(0)
                val c = parts.getOrNull(1)
                LocaleMatcher.matchLanguage(l, c, null)?.first ?: LocaleMatcher.ENTRIES.first()
            } else {
                var vDesc = request.voiceName?.takeIf { it.isNotBlank() }?.let { VoiceRegistry.findVoice(it) }
                val requestMatch = if (vDesc == null && !request.language.isNullOrBlank()) {
                    LocaleMatcher.matchLanguage(request.language, request.country, request.variant)
                } else null

                vDesc?.entry
                    ?: requestMatch?.first
                    ?: activeVoiceDesc?.entry
                    ?: activeLanguage
            }

            // 2. Voice Descriptor & Preset (Voice Profile)
            val userPreset = settings.voiceProfile
            val reqVoiceName = request.voiceName?.takeIf { it.isNotBlank() }
            val voiceDesc = reqVoiceName?.let { VoiceRegistry.findVoice(it) }

            val preset = if (reqVoiceName == null || !settings.respectAppVoiceRequests || settings.forceLanguage) {
                userPreset
            } else {
                val isGenericDefault = (voiceDesc?.presetId == 0 && userPreset != 0)
                if (isGenericDefault) {
                    userPreset
                } else {
                    voiceDesc?.presetId ?: userPreset
                }
            }

            val resolvedVoiceDesc = VoiceRegistry.findDefaultVoiceFor(entry, preset)
            activeVoiceDesc = resolvedVoiceDesc

            val targetEngine = ensureEngine(entry.eciId)
            if (targetEngine == null) {
                Log.e(TAG, "Failed to initialize engine for ${entry.displayName} (eciId=0x${Integer.toHexString(entry.eciId)})")
                callback.error(TextToSpeech.ERROR_SERVICE)
                return
            }

            // Apply Sampling Rate
            targetEngine.setSampleRate(settings.samplingRate)

            // Apply Abbreviations (Native ECI parameter)
            targetEngine.setAbbreviations(settings.useAbbreviations)

            // Apply Voice Preset & Characteristics
            val shape = getShapeFromSettings()
            targetEngine.applyVoice(preset, shape)

            // Speech Rate Precedence & Mapping
            val speed = if (settings.forceSpeechRate) {
                SpeechRateMapper.mapRelativeRate(settings.speechRate, settings.unlockMaxRate)
            } else {
                SpeechRateMapper.mapAndroidRate(request.speechRate, settings.unlockMaxRate)
            }
            targetEngine.setSpeed(speed)

            // Pitch Precedence & Mapping
            val pitch = if (settings.forcePitch) {
                VoiceParameterMapper.mapPitch(settings.pitch, targetEngine.currentBasePitch)
            } else {
                val reqPitch = request.pitch
                if (reqPitch == 100) {
                    targetEngine.currentBasePitch
                } else {
                    val rel = Math.round((reqPitch - 100) / 10.0).toInt().coerceIn(-10, 10)
                    VoiceParameterMapper.mapPitch(rel, targetEngine.currentBasePitch)
                }
            }
            targetEngine.setPitch(pitch)

            val rawText = request.charSequenceText?.toString().orEmpty()
            Log.i(TAG, "onSynthesizeText: start reqVoice=${request.voiceName}, resolvedVoice=${resolvedVoiceDesc.name}, lang=${entry.displayName}, preset=$preset, speed=$speed, pitch=$pitch, textLen=${rawText.length}")

            if (rawText.isBlank()) {
                callback.start(targetEngine.sampleRateHz, AudioFormat.ENCODING_PCM_16BIT, 1)
                callback.done()
                return
            }

            val langCode = entry.iso2Lang

            // Pipeline step 1: Abbreviations expansion
            val abbrProcessed = AbbreviationProcessor.process(rawText, settings.useAbbreviations)

            // Pipeline step 2: Emoji & Emoticons
            try {
                EmojiData.ensureLoaded(this)
            } catch (t: Throwable) {
                Log.w(TAG, "onSynthesizeText: EmojiData ensureLoaded warning", t)
            }
            val emojiProcessed = if (settings.enableEmoji) {
                val ep = EmojiProcessor.process(abbrProcessed, langCode)
                EmoticonProcessor.process(ep, langCode)
            } else {
                abbrProcessed
            }

            // Pipeline step 3: Number Processing
            val numberProcessed = NumberProcessor.process(
                emojiProcessed,
                enabled = settings.useNumberProcessing,
                mode = settings.numberProcessingMode
            )

            // Pipeline step 4: Intonation Pauses
            val pauseMode = if (settings.intonationPauses) PauseProcessor.PAUSE_KEEP else PauseProcessor.PAUSE_ALL

            val textReq = TextRequest(
                text = numberProcessed,
                voiceName = resolvedVoiceDesc.name,
                speechRate = speed,
                pitch = pitch,
                phrasePrediction = settings.phrasePrediction,
                pauseMode = pauseMode,
                spokenPunctuation = settings.processPunctuation
            )

            // Pipeline step 5: Unicode Normalization (after emojis & emoticons are replaced with words)
            val normalized = UnicodeNormalizer.normalize(textReq.text)

            // Pipeline step 6: Punctuation Processing
            val punctuated = ScreenReaderPunctuationProcessor.process(
                normalized,
                enabled = settings.processPunctuation,
                level = settings.punctuationLevel,
                customPunctuation = settings.customPunctuation
            )

            // Pipeline step 7: OpenEVV fixes & Chunking
            val fixed = OpenEVVCompatibilityFixes.apply(punctuated)
            val chunks = Chunker.chunk(fixed)

            val pace = Pace(targetEngine.sampleRateHz * BYTES_PER_SAMPLE)
            var started = false

            for ((index, chunk) in chunks.withIndex()) {
                if (stopped) break
                val isLast = (index == chunks.lastIndex)
                val preparedChunk = PauseProcessor.process(
                    text = chunk,
                    pauseMode = textReq.pauseMode,
                    phrasePrediction = textReq.phrasePrediction,
                    isLastChunk = isLast
                )
                val bytes = OpenEVVEncoder.encode(preparedChunk)
                if (!targetEngine.speakBytes(bytes)) {
                    Log.e(TAG, "onSynthesizeText: Engine refused text chunk (index $index of ${chunks.size})")
                    callback.error(TextToSpeech.ERROR_SYNTHESIS)
                    return
                }

                val buffer = ByteArray(STREAM_BUFFER_SIZE)
                while (!stopped) {
                    val bytesRead = targetEngine.read(buffer)
                    if (bytesRead <= 0) break

                    // Force Volume Scaling (relative -10..10)
                    if (settings.forceVolume) {
                        AudioGainProcessor.applyRelativeGain(buffer, bytesRead, settings.volume)
                    }

                    if (!started) {
                        if (callback.start(targetEngine.sampleRateHz, AudioFormat.ENCODING_PCM_16BIT, 1) != TextToSpeech.SUCCESS) {
                            targetEngine.stop()
                            return
                        }
                        started = true
                    }

                    if (callback.audioAvailable(buffer, 0, bytesRead) != TextToSpeech.SUCCESS) {
                        targetEngine.stop()
                        return
                    }

                    pace.consume(bytesRead)
                }
            }

            if (stopped) {
                Log.i(TAG, "onSynthesizeText: synthesis aborted because onStop was requested")
                return
            }

            if (started) {
                callback.done()
            } else {
                callback.start(targetEngine.sampleRateHz, AudioFormat.ENCODING_PCM_16BIT, 1)
                callback.done()
            }
            Log.i(TAG, "onSynthesizeText: completed successfully (${chunks.size} chunks)")
        } catch (t: Throwable) {
            Log.e(TAG, "onSynthesizeText: Exception during speech synthesis", t)
            try {
                callback.error(TextToSpeech.ERROR_SYNTHESIS)
            } catch (ignored: Throwable) {
                // Ignore secondary callback exceptions
            }
        }
    }

    private class Pace(private val bytesPerSecond: Int) {
        private val startTimeNs = System.nanoTime()
        private var totalBytes = 0L

        fun consume(bytes: Int) {
            totalBytes += bytes
        }

        fun delivered(bytes: Int) {
            totalBytes += bytes
        }

        fun deliveredMs(): Long =
            if (bytesPerSecond <= 0) 0 else totalBytes * 1000L / bytesPerSecond

        fun aheadMs(): Long =
            deliveredMs() - (System.nanoTime() - startTimeNs) / 1_000_000L
    }

    companion object {
        private const val TAG = "EloqiumTtsService"
        private const val STREAM_BUFFER_SIZE = 4096
        private const val BYTES_PER_SAMPLE = 2
    }
}
