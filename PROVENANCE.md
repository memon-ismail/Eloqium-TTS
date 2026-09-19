# Eloqium TTS — Architectural Provenance & Lineage

## 1. Project Background

**Eloqium TTS** (`org.eloqium.tts`) is an independent open-source Android Text-to-Speech engine created and maintained by **Ismail Memon**. It is designed to deliver fast, highly intelligible speech synthesis for Android screen-reader users and everyday listeners.

---

## 2. Component Lineage

### 2.1 Native Speech Engine: OpenEVV (`libevv`)
- **Upstream Repository**: https://github.com/Mudb0y/openevv
- **License**: MIT License + IBM Speech Synthesis Language Rules Notice.
- **Architecture**: OpenEVV provides the native C formant synthesis engine and linguistic rules tables. Eloqium applies architecture patches for ARM64 Delta-Low vectorizer state preservation and ARM32 frame alignment.

### 2.2 Native Audio Streaming Architecture (EVVDroid Reference)
- **Source Repository**: https://github.com/trypsynth/evvdroid (Apache License 2.0).
- **Architecture**: Eloqium builds upon the native JNI worker thread and circular PCM ring buffer synchronization concepts originally demonstrated in EVVDroid, including pure C callbacks and immediate speech cancellation via `eciDataAbort`.

---

## 3. Independent Implementations

All application-level subsystems and text processing modules in Eloqium TTS were written independently:
- **Canonical Locale & Router Handling**: Full AOSP `TextToSpeechService` implementation using standard BCP-47 two-letter `Locale` instances (`Locale("en", "US")`) to ensure compatibility with Android TTS clients and multi-engine routers.
- **Text Preprocessing Pipeline**: Independent implementations of Unicode 16.0 trie emoji matching, ASCII emoticon protection, context-aware abbreviation expansion, number formatting, and screen-reader punctuation verbosity.
- **Punctuation & Intonation**: Default-enabled phrase prediction (`pp1`) and clause-boundary attachment preserving natural interrogative and declarative pitch contours.
- **User Dictionary Subsystem**: Independent multi-lingual dictionary engine supporting text and OpenEVV SPR phonetic entries, JSON Schema v2 persistence, and legacy IBM `.dic` importing.
- **Modern User Interface**: Native Jetpack Compose interface built for accessibility, TalkBack navigation, and Material 3 design.

---

## 4. Licensing Boundaries

- **Eloqium TTS Core**: Licensed under the Apache License, Version 2.0.
- **No GPL Dependencies**: No code was ported or copied from GPL-licensed projects (such as the desktop NVDA IBMTTS driver). All Kotlin, Java, and glue code is Apache-2.0.
- **Upstream Notices**: Copyright attributions and third-party notices for OpenEVV, EVVDroid, and Unicode CLDR data are maintained in [NOTICE](NOTICE) and [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
