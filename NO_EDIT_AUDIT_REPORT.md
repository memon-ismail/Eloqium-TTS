# Eloqium — No-Edit Audit Report

**Date of Initial Audit:** September 10, 2026  
**Audit Target:** Eloqium TTS (`org.eloqium.tts`) repository at commit `53c4949`  
**Auditor:** Antigravity Autonomous Agent  
**Purpose:** Pre-implementation read-only baseline audit, external research, and architecture evaluation covering licensing, dependencies, lifecycle contracts, native JNI stability, and accessibility.

> [!NOTE]
> **Historical Baseline Audit Snapshot**: This document is an archived, read-only pre-change evaluation performed on September 10, 2026 against baseline commit `53c4949` prior to the subsequent implementation cycles (v0.1.0 cleanup, v0.1.1 Custom Punctuation, and v0.1.2 User Dictionary). Historical findings and labels such as `REQUIRES MODIFICATION` document the initial baseline state that existed prior to those changes and must not be interpreted as describing the current production state.

---

## 1. Audit Scope

This audit evaluates the complete repository state of **Eloqium TTS** prior to re-architecture and cleanup. The scope comprises:
1. **Source Code & Stubs**: Every Kotlin, Java, and C/JNI source file in `app/src/main` and `test/`.
2. **Build Configurations**: Gradle build scripts (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`), Gradle version catalogs (`gradle/libs.versions.toml`), Android Manifest, and native Makefiles/scripts.
3. **Native Speech Engine Integration**: OpenEVV submodule (`native/openevv`), architecture patches (`native/patches/`), and low-level JNI bindings (`eloqium_jni.c`).
4. **Third-Party Dependencies & Data**: Bundled binary assets (`emoji_data.bin`), external reference repos (`references/`), and external community dictionaries.
5. **Licensing & Legal Notices**: `LICENSE`, `NOTICE`, `PROVENANCE.md`, `THIRD_PARTY_LICENSES.md`, and upstream submodule notices.
6. **UI & Accessibility**: Jetpack Compose screens (`HomeScreen`, `SettingsScreen`, `AboutScreen`), accessibility semantics, and Android TTS service lifecycle compliance.

---

## 2. Repository Inventory

### 2.1 Tracked Source Code Files
- **Kotlin Service & Pipeline (19 files)**:
  - `app/src/main/kotlin/org/eloqium/tts/service/EloqiumTtsService.kt`
  - `app/src/main/kotlin/org/eloqium/tts/service/Settings.kt`
  - `app/src/main/kotlin/org/eloqium/tts/engine/AudioGainProcessor.kt`
  - `app/src/main/kotlin/org/eloqium/tts/engine/Eci.kt`
  - `app/src/main/kotlin/org/eloqium/tts/engine/EloqiumEngine.kt`
  - `app/src/main/kotlin/org/eloqium/tts/engine/LocaleMatcher.kt`
  - `app/src/main/kotlin/org/eloqium/tts/engine/NativeEngine.kt`
  - `app/src/main/kotlin/org/eloqium/tts/engine/SpeechRate.kt`
  - `app/src/main/kotlin/org/eloqium/tts/engine/SpeechRateMapper.kt`
  - `app/src/main/kotlin/org/eloqium/tts/engine/VoiceParameterMapper.kt`
  - `app/src/main/kotlin/org/eloqium/tts/engine/VoiceRegistry.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/AbbreviationDictionary.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/AbbreviationProcessor.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/Chunker.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/EmojiData.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/EmojiProcessor.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/EmoticonProcessor.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/NumberProcessor.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/OpenEVVCompatibilityFixes.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/OpenEVVEncoder.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/PauseProcessor.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/ScreenReaderPunctuationProcessor.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/TextRequest.kt`
  - `app/src/main/kotlin/org/eloqium/tts/pipeline/UnicodeNormalizer.kt`
- **Kotlin UI Layer (7 files)**:
  - `app/src/main/kotlin/org/eloqium/tts/ui/MainActivity.kt`
  - `app/src/main/kotlin/org/eloqium/tts/ui/HomeScreen.kt`
  - `app/src/main/kotlin/org/eloqium/tts/ui/SettingsActivity.kt`
  - `app/src/main/kotlin/org/eloqium/tts/ui/SettingsScreen.kt`
  - `app/src/main/kotlin/org/eloqium/tts/ui/AboutScreen.kt`
  - `app/src/main/kotlin/org/eloqium/tts/ui/AboutNavigation.kt`
  - `app/src/main/kotlin/org/eloqium/tts/ui/theme/Theme.kt`
- **Android Framework Activities (2 files)**:
  - `app/src/main/kotlin/org/eloqium/tts/ui/CheckVoiceDataActivity.kt`
  - `app/src/main/kotlin/org/eloqium/tts/ui/GetSampleTextActivity.kt`
- **Native JNI & C Layer (1 file)**:
  - `app/src/main/cpp/eloqium_jni.c`
- **Native Build & Patches (5 files)**:
  - `native/android.mk`
  - `native/build-native.sh`
  - `native/patches/0001-delta_low-adjacent-stores.patch`
  - `native/patches/0002-arm32-frame-alignment.patch`
  - `native/patches/0003-delta_low-regions-limit.patch`
- **Testing & Mock Suite (10 files)**:
  - `test/TestRunner.kt`
  - `test/MockSharedPreferences.kt`
  - `test/stubs/android/content/Context.java`
  - `test/stubs/android/content/Intent.java`
  - `test/stubs/android/content/SharedPreferences.java`
  - `test/stubs/android/content/TestPrefs.java`
  - `test/stubs/android/media/AudioFormat.java`
  - `test/stubs/android/os/Bundle.java`
  - `test/stubs/android/speech/tts/SynthesisCallback.java`
  - `test/stubs/android/speech/tts/SynthesisRequest.java`
  - `test/stubs/android/speech/tts/TextToSpeech.java`
  - `test/stubs/android/speech/tts/TextToSpeechService.java`
  - `test/stubs/android/speech/tts/Voice.java`
  - `test/stubs/android/util/Log.java`
- **Assets & Resources**:
  - `app/src/main/assets/emoji_data.bin` (719 KB precompiled UTF-16 trie)
  - `app/src/main/res/drawable/eloqium_logo.png`
  - `app/src/main/res/values/strings.xml`, `colors.xml`
  - `app/src/main/res/xml/tts_engine.xml`

### 2.2 Untracked / Ignored Artifacts
- Root test audio outputs: `test_evvdroid_pause.wav`, `test_evvdroid_pp0.wav`, `test_hello.wav`, `test_natural.wav`, `test_uk.wav`, `test_us.wav`.
- Build directory leftovers: `build_artifacts/app-debug.apk`.
- Upstream reference folders in `references/` (`abstts`, `evvdroid`, `NVDA-IBMTTS-Driver`).

---

## 3. Architecture Findings

### 3.1 Architectural Strengths [VERIFIED]
1. **Unidirectional Synthesis Pipeline**: Text flows cleanly through:
   `AbbreviationProcessor` → `EmojiProcessor` / `EmoticonProcessor` → `NumberProcessor` → `UnicodeNormalizer` → `ScreenReaderPunctuationProcessor` → `OpenEVVCompatibilityFixes` → `Chunker` → `PauseProcessor` → `OpenEVVEncoder` → `EloqiumEngine.speakBytes()`.
2. **Decoupled Settings Single Source of Truth**: All preference reading and writing goes through `Settings.kt` backed by SharedPreferences, independent of Activity lifecycle.
3. **No Hidden Activity Dependency**: The service instantiates its own dependencies without requiring `MainActivity` or Compose to have ever run.

### 3.2 Architectural Defects & Redundancies [VERIFIED]
1. **Redundant Speed Classes**: Both `SpeechRate.kt` and `SpeechRateMapper.kt` exist in `engine/`. `SpeechRate.kt` is a 19-line legacy helper only referenced in one unused method in `EloqiumEngine.kt`.
2. **Monolithic About UI**: `AboutScreen.kt` previously contained all text, cards, buttons, and hardcoded layout elements in a single 292-line file.
3. **Unused Version Catalog Entries**: `gradle/libs.versions.toml` declares `material` (Google Material Components) and AndroidX testing libraries that are not consumed by the Compose Material 3 build.

---

## 4. Code Findings

### 4.1 Code Quality & Formatting [VERIFIED]
- Kotlin code adheres to standard conventions.
- Explicit type signatures are present on all public API and engine methods.
- Nullability handling in `LocaleMatcher` and `VoiceRegistry` is defensive and verified against non-standard client/router inputs.

### 4.2 Dead & Obsolete References [VERIFIED]
- `SpeechRate.kt` duplicates functionality provided more authentically by `SpeechRateMapper.kt`.
- Root directory contains stale test `.wav` audio output files from previous development passes.

---

## 5. Android TTS Lifecycle Findings

### 5.1 TextToSpeechService Contract Compliance [VERIFIED]
- **Service Creation (`onCreate`)**: Fixed in commit `53c4949`. `_settings = Settings(this)` is initialized *before* `super.onCreate()` is called. A lazy-safe backing property `settings` ensures zero possibility of `UninitializedPropertyAccessException` when AOSP invokes `onLoadLanguage()` from within `super.onCreate()`.
- **Language & Voice Loading**:
  - `onLoadLanguage()`: Dynamically matches input locale, updates `activeLanguage` and `activeVoiceDesc`, and returns `LANG_COUNTRY_AVAILABLE` or `LANG_AVAILABLE`.
  - `onLoadVoice()`: Maps canonical voice name to dialect and preset, updating active descriptors without reallocating native engine instances unnecessarily.
  - `onGetVoices()`: Returns a fixed canonical set of 64 immutable `Voice` instances (8 languages × 8 presets).
- **Synchronous Synthesis Contract (`onSynthesizeText`)**:
  - Audio delivery is completely synchronous on the service's `SynthThread`.
  - Audio buffers (4096 bytes PCM 16-bit) are pushed sequentially via `callback.audioAvailable()`.
  - Callbacks are never leaked to native threads or retained past function return.
- **Stop & Interruption Handling (`onStop`)**:
  - Marked volatile `@Volatile private var stopped = false`.
  - `onStop()` immediately halts synthesis in the native worker thread via `pthread_cond_broadcast` and returns.
  - **Critical Contract Conformance**: When stopped mid-utterance, `callback.done()` is explicitly *not* called, complying with the official Android SDK documentation.

---

## 6. Native/JNI Findings

### 6.1 Memory Management & Thread Safety [VERIFIED]
- Inspecting `app/src/main/cpp/eloqium_jni.c`:
  - **Thread Synchronization**: Pthread mutex `in->lock` protects the ring buffer (`in->ring`, 128 KB) and state flags (`aborted`, `started`, `done`, `busy`, `quitting`).
  - **Condition Variables**: `in->room` (signaled when consumer takes audio), `in->filled` (signaled when producer puts audio), and `in->work` (signals worker thread to start/stop).
  - **Lifecycle Teardown**: `Java_org_eloqium_tts_engine_NativeEngine_destroy` sets `aborted = 1; quitting = 1;`, broadcasts all condition variables, calls `pthread_join(in->worker, NULL)` before freeing memory, preventing use-after-free.
  - **Buffer Ownership**: Memory allocated in `speak()` (`in->text`) is freed upon subsequent utterance or final destruction.
- **OpenEVV Native Patches**:
  - `0001-delta_low-adjacent-stores.patch`: Fixes 64-bit ARM adjacent store lookup bugs.
  - `0002-arm32-frame-alignment.patch`: Fixes 32-bit ARM unaligned multi-word load/store SIGBUS crashes.
  - `0003-delta_low-regions-limit.patch`: Expands `REGIONS` from 512 to 2048 to prevent `abort()` during simultaneous 8-language initialization.

---

## 7. Dependency Inventory

| Dependency Name | Group & Artifact | Version | License | Bundled in APK | Purpose & Usage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **AndroidX Core KTX** | `androidx.core:core-ktx` | `1.17.0` | Apache-2.0 | Yes (DEX) | Kotlin extensions for Android framework classes |
| **Compose BOM** | `androidx.compose:compose-bom` | `2026.03.01` | Apache-2.0 | Yes (DEX) | Jetpack Compose Bill of Materials |
| **Compose Material 3** | `androidx.compose.material3:material3` | BOM-aligned | Apache-2.0 | Yes (DEX) | Material Design 3 UI controls |
| **Compose UI** | `androidx.compose.ui:ui` | BOM-aligned | Apache-2.0 | Yes (DEX) | Declarative UI framework |
| **Compose UI Tooling Preview** | `androidx.compose.ui:ui-tooling-preview` | BOM-aligned | Apache-2.0 | Yes (DEX) | Layout inspection preview support |
| **Activity Compose** | `androidx.activity:activity-compose` | `1.13.0` | Apache-2.0 | Yes (DEX) | `ComponentActivity.setContent` integration |
| **JUnit 4** | `junit:junit` | `4.13.2` | EPL-1.0 | No (Test only)| Local JVM testing framework |
| **OpenEVV (`libevv`)** | `native/openevv` | `d4ddb8e` | MIT + IBM Notice | Yes (JNI `.so`) | Native speech synthesis engine |
| **CLDR 48 / Unicode 16.0** | Unicode Consortium | 16.0 / 48 | Unicode-3.0 | Yes (Asset) | Offline emoji & emoticon multilingual spoken data |

---

## 8. Open-Source TTS Engine Audit

### 8.1 OpenEVV Origin & Licensing [VERIFIED]
- **Repository**: `https://github.com/Mudb0y/openevv`
- **Primary Author**: Stanislaw Przedzinkowski (Mudb0y).
- **Core Engine Code (`src/`, `rom/`, `cli/`, `tools/`)**: Licensed under the **MIT License**. Written in clean C against IBM object interfaces.
- **Language Data & Tables (`lang/`, `src/klatt/klatt_tables.c`)**: Transcribed from IBM Embedded ViaVoice (EVV) 4.3 SDK (`evvWXP.exe`). Governed by IBM Speech Synthesis Language Rules Notice.
- **Corporate History of Eloquence**:
  Eloquent Technology, Inc. (ETI) → SpeechWorks → ScanSoft → Nuance Communications → Cerence Inc. (2019 spinoff). Text-to-speech assets are currently subject to ongoing federal litigation (*Cerence Inc. v. Microsoft Corp. & Nuance Communications*, D. Del. 1:25-cv-00553).
- **Redistribution Rights Assessment**:
  The C code of OpenEVV is permissively licensed (MIT). The language tables (`lang/`) are reverse-engineered/transcribed from IBM ViaVoice 4.3 SDK for accessibility interoperability. OpenEVV upstream explicitly documents this dual nature in `NOTICE`.

---

## 9. Dictionary Audit

### 9.1 Evaluated Dictionary Candidates
1. **`eigencrow/IBMTTSDictionaries`**:
   - **Provenance**: Community-maintained repository of English pronunciation entries for Eloquence/IBMTTS screen reader users.
   - **License**: **CC0-1.0 (Public Domain Dedication)**.
   - **Format**: Windows-1252 text files (`.dic`).
   - **Status**: **VERIFIED DISTRIBUTABLE**.
   - **Technical Reality in OpenEVV**: The OpenEVV function `ed_loadDict()` is a stub returning `DICT_NOT_SUPPORTED`. External file-based `.dic` loading is not supported in the underlying engine; in-memory dynamic pairs via `eciUpdateDict` are supported.
2. **Proprietary Eloquence Binary Dictionaries (`*.cfd`, `*.dct`, `*.jdf`)**:
   - **Provenance**: Proprietary dictionary blobs shipped with Code Factory Eloquence or commercial JAWS.
   - **License**: Proprietary / All rights reserved.
   - **Status**: **DO NOT DISTRIBUTE**. Cannot be redistributed in open source.
3. **Eloqium Clean-Room Abbreviation Dictionary (`AbbreviationDictionary.kt`)**:
   - **Provenance**: Original clean-room curated mapping of 35+ common English abbreviations with context-sensitive title/street disambiguation.
   - **License**: **Apache License 2.0**.
   - **Status**: **VERIFIED DISTRIBUTABLE**.

---

## 10. Licensing and Attribution Audit

### 10.1 Licensing Architecture Status [VERIFIED]
- **Project License**: Apache License 2.0 (`LICENSE`).
- **`NOTICE`**: Acknowledges Eloqium contributors, EVVDroid (Apache-2.0), OpenEVV (MIT), IBM Language Rules Notice, and AOSP (Apache-2.0).
- **`THIRD_PARTY_LICENSES.md`**: Provides full verbatim license texts for EVVDroid, OpenEVV, AOSP, and Unicode Inc. (Unicode 16.0 / CLDR 48).
- **`PROVENANCE.md`**: Documents technical lineage, clean-room boundary against NVDA GPL code, and departures from EVVDroid.
- **Finding**: Attribution architecture is legally sound, thorough, and accurately reflects component origins.

---

## 11. Branding Audit

### 11.1 Project Naming Consistency [VERIFIED]
- **App Name**: `Eloqium` (`R.string.app_name`).
- **Engine Name**: `Eloqium TTS` (`R.string.engine_name`).
- **Package ID**: `org.eloqium.tts`.
- **Logo**: Dedicated original vector/PNG icon at `app/src/main/res/drawable/eloqium_logo.png`.
- **Finding**: Naming is completely consistent. No proprietary trademarks (IBM, Eloquence, ViaVoice, Nuance) are used as app or engine identifiers.

---

## 12. UI/UX Audit

### 12.1 Current Home Screen Structure [REQUIRES MODIFICATION]
- Currently has:
  1. Logo image
  2. Welcome heading
  3. "Settings" button
  4. "About" button
- **Issue**: Lacks direct access to Android's System Text-to-Speech Settings screen. Users must manually navigate Android's deep Settings hierarchy to enable the engine.
- **Required Modification**:
  - Rename settings button to: `Eloqium settings`.
  - Add directly below it: `System TTS Settings` launching `Intent("com.android.settings.TTS_SETTINGS")` with safe fallbacks.

### 12.2 Current About Screen Structure [REQUIRES MODIFICATION]
- Currently has:
  - Header & Identity
  - "About Eloqium" card
  - "Why Eloqium" card
  - "Project Information" card
  - "Credit / Support" card (with Ismail Memon, GitHub, Telegram links)
  - Non-affiliation disclaimer
- **Issue**: Contains an ad-hoc "Credit / Support" section rather than a formal, polished open-source product information architecture.
- **Required Modification**:
  - Remove the old "Credit / Support" presentation.
  - Re-architect into modular components: Identity, Project Details, Engine & Technical Lineage, Licensing & Legal Notices, Offline Architecture & Privacy, and Project Links.

---

## 13. Accessibility Audit

### 13.1 Screen Reader (TalkBack) Review [VERIFIED]
- **Touch Targets**: All interactive buttons meet or exceed the 48dp Android Accessibility Guidelines.
- **Semantics**: Headings are annotated with `Modifier.semantics { heading() }`.
- **Navigation Controls**: Top bar back buttons include descriptive `contentDescription = "Navigate back"`.
- **Screen Reader Clarity**: Label/value pairs on settings sliders and about info rows use `semantics(mergeDescendants = true)` to ensure talkback reads label and value in a single coherent utterance.

---

## 14. Documentation Audit

### 14.1 Status of Existing Documentation [REQUIRES EXPANSION]
- `README.md` is currently only 25 lines and lacks complete architectural diagrams, installation guides, compilation instructions, and comprehensive feature breakdowns.
- `docs/` contains 3 high-quality specifications: `emoji-emoticon-pipeline.md`, `engine-router-compatibility.md`, and `punctuation-behavior.md`.
- **Finding**: `README.md` must be completely re-architected into an authoritative, professional open-source documentation portal.

---

## 15. Repository Cleanliness Audit

### 15.1 Extraneous Files Discovered [VERIFIED]
- Root directory contains loose test audio outputs: `test_evvdroid_pause.wav`, `test_evvdroid_pp0.wav`, `test_hello.wav`, `test_natural.wav`, `test_uk.wav`, `test_us.wav`.
- Build directory contains old APK build artifact copies in `build_artifacts/`.
- `SpeechRate.kt` is redundant.

---

## 16. Security and Privacy Audit

### 16.1 Permissions & Data Protection [VERIFIED]
- **Zero Network Permissions**: `AndroidManifest.xml` requests *zero* network permissions (`android.permission.INTERNET` is neither requested nor used).
- **100% Offline Processing**: All text processing, emoji dictionaries, and speech synthesis run entirely on-device in native code.
- **No Telemetry or Analytics**: Zero tracking SDKs or analytics services are linked or initialized.
- **Service Security**: The TTS service is exported solely with `android.intent.action.TTS_SERVICE` for legitimate screen reader binding.

---

## 17. Proposed Changes

1. **Clean Repository**: Remove loose root `.wav` files and obsolete build artifacts. Remove redundant `SpeechRate.kt` and consolidate rate conversion into `SpeechRateMapper.kt`.
2. **Re-Architect Home UI**:
   - Change "Settings" button label to: `Eloqium settings`.
   - Add immediately below it: `System TTS Settings` (launches Android system TTS settings with graceful fallbacks).
3. **Re-Architect About UI**:
   - Completely remove the old "Credit / Support" card presentation.
   - Decompose into clean, modular composable components: Identity, Project Details, Engine & Technical Lineage, Licensing & Legal Notices, Offline Architecture & Privacy, and Project Links.
4. **Clean Code Comments**:
   - Remove redundant AI-generated filler comments across Kotlin and C files while preserving concise non-obvious explanations.
5. **Redesign README.md**:
   - Full professional open-source documentation structure with project logo, table of contents, architecture breakdown, building instructions, and legal attributions.
6. **Maintain 100% Test Coverage**:
   - Verify all 28 test suites in `test/TestRunner.kt` pass with 0 failures after all modifications.

---

## 18. Items Requiring Verification

- **REQUIRES SOURCE VERIFICATION**: Physical phone launch of `System TTS Settings` across diverse OEM distributions (Samsung OneUI, Xiaomi HyperOS, Google Pixel) to confirm system intent resolution.
- **REQUIRES LICENSE VERIFICATION**: Future distribution of third-party custom user dictionary files (.dic) if OpenEVV ever adds dynamic file-based dictionary loading.

---

## 19. Licensing Risks

1. **OpenEVV Language Tables**: The C engine is MIT, but the transcribed language rules originated from IBM EVV 4.3 SDK. Risk is mitigated by upstream OpenEVV's explicit notice and clean-room re-implementation of the synthesis engine.
2. **NVDA IBMTTS Driver GPL Contamination Risk**: Eliminated. Verified zero NVDA driver code is present in Eloqium.

---

## 20. Final Recommendation

Proceed directly with the planned re-architecture and implementation:
- Clean extraneous files.
- Refactor and modularize UI (`HomeScreen` and `AboutScreen`).
- Connect `System TTS Settings` button.
- Overhaul `README.md`.
- Verify with automated test suite and clean build.
