# Eloqium TTS — Architectural Provenance & Lineage

## 1. Project Background

**Eloqium TTS** (`org.eloqium.tts`) is an independent open-source Android Text-to-Speech engine created and maintained by **Ismail Memon**. It is engineered to deliver fast, highly intelligible speech synthesis for Android screen-reader users.

The project was created to address long-standing limitations, compatibility failures, and prosodic flaws in prior Android OpenEVV wrappers, while providing a modern, standard-compliant, maintainable implementation.

---

## 2. Lineage and Component Provenance

### 2.1 Native Speech Engine: OpenEVV (`libevv`)
- **Upstream Repository**: https://github.com/Mudb0y/openevv
- **License**: MIT + IBM Language Rules Notice.
- **Architectural Enhancements Applied in Eloqium TTS**:
  - **Delta-Low Adjacent Stores Patch (`0001-delta_low-adjacent-stores.patch`)**: Prevents compiler vectorizer register corruption in the Delta VM state on 64-bit ARM architectures.
  - **ARM32 Frame Alignment Patch (`0002-arm32-frame-alignment.patch`)**: Ensures 8-byte stack frame alignment across JNI call boundaries on 32-bit ARM (armeabi-v7a).
  - **Native Compiler Verification**: Successfully compiled natively on ARM64 Android with Termux `clang` 21.1.8.

### 2.2 Native JNI & Audio Pipeline Lineage (from EVVDroid)
- **Source Repository**: https://github.com/trypsynth/evvdroid (Apache 2.0).
- **Preserved Core Strengths**:
  - Persistent native worker thread avoiding thread creation per utterance.
  - Ring buffer synchronization (`take`, `give`, `filled`, `room` condition variables).
  - Pure C JNI callbacks avoiding cross-thread JNI reflection during audio synthesis.
  - Cancellation via `eciDataAbort` returned from the native callback, avoiding the instability of `eciStop`.
  - Sentence-aware chunking and 300ms audio pacing lead.

### 2.3 Critical Deviations & Incompatible Code Removed from EVVDroid
Eloqium TTS completely departs from EVVDroid in the following areas:

1. **Locale Representation**:
   - *EVVDroid flaw*: Initialized `Locale` objects with 3-letter country codes (`Locale("eng", "USA")`), causing `MissingResourceException` inside AOSP `TextToSpeech.java` and failures with external engine routers.
   - *Eloqium TTS fix*: Uses canonical BCP-47 / ISO 639-1 + ISO 3166-1 alpha-2 `Locale` objects (`Locale("en", "US")`, `Locale("en", "GB")`, etc.), eliminating all crashes.

2. **Caller Voice Selection**:
   - *EVVDroid flaw*: Synthesizer ignored `request.voiceName` requested by the caller and forced the engine's internal UI preset.
   - *Eloqium TTS fix*: Full dynamic voice resolution parsing `request.voiceName` into `(language, preset)`.

3. **Language Availability Matching**:
   - *EVVDroid flaw*: Incompatible with routers checking strict equality against `LANG_AVAILABLE`.
   - *Eloqium TTS fix*: Implemented bidirectional ISO-2/ISO-3 normalization with permissive matching.

4. **Punctuation & Prosody Pipeline**:
   - *EVVDroid flaw*: Disabled Phrase Prediction by default (`pp0`), flattening intonation; injected spaces and pauses before punctuation (`Pauses.kt`), breaking clause boundaries.
   - *Eloqium TTS fix*: Enforces `pp1` by default, strictly binds terminal punctuation to words, and handles unicode quotes/dashes cleanly.

---

## 3. Clean-Room Licensing Compliance

- **No GPL code**: Although the NVDA IBMTTS Driver is a popular desktop implementation, it is licensed under GPLv2. To protect the Apache-2.0 status of Eloqium TTS, no code was copied or ported from the NVDA driver. All text processing and Android service code was written clean-room.
- **Attribution retained**: Full copyright notices and licenses from OpenEVV and EVVDroid are retained in `NOTICE` and `THIRD_PARTY_LICENSES.md`.
