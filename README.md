# Eloqium TTS

<p align="center">
  <img src="app/src/main/res/drawable/eloqium_logo.png" alt="Eloqium TTS Logo" width="160" height="160" />
</p>

<p align="center">
  <strong>Fast, intelligible, and offline text-to-speech engine for Android accessibility</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-0.1.1-blue.svg" alt="Version 0.1.1" />
  <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="Apache 2.0 License" />
  <img src="https://img.shields.io/badge/Platform-Android_8.0%2B-green.svg" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Architecture-ARM64%20%7C%20ARMv7%20%7C%20x86__64-orange.svg" alt="Architecture" />
  <img src="https://img.shields.io/badge/Synthesis-100%25_Offline-brightgreen.svg" alt="100% Offline" />
  <img src="https://img.shields.io/badge/Voices-64_Voices-blueviolet.svg" alt="64 Voices" />
</p>

---

## Contents

1. [About](#about)
2. [Features](#features)
3. [Supported Languages and Voices](#supported-languages-and-voices)
4. [Installation](#installation)
5. [Using Eloqium](#using-eloqium)
6. [Settings](#settings)
7. [Android TTS Integration](#android-tts-integration)
8. [Offline and Privacy](#offline-and-privacy)
9. [Building From Source](#building-from-source)
10. [Architecture](#architecture)
11. [Open-Source Components](#open-source-components)
12. [Third-Party Dependencies](#third-party-dependencies)
13. [Dictionary and Data Licensing](#dictionary-and-data-licensing)
14. [Contributing](#contributing)
15. [Known Limitations](#known-limitations)
16. [Links](#links)
17. [License](#license)

---

## About
 
Eloqium TTS (version 0.1.1) is an independent open-source text-to-speech engine designed for Android screen-reader users and everyday speech output. It provides fast, clear, and highly responsive speech synthesis that works completely offline on your device.
 
The project was created and is maintained by **Ismail Memon**.

### Special Thanks To

1. Chandu Rathod

2. Yashraj Shinde

Eloqium pairs an optimized native build of OpenEVV with modern Android accessibility services to deliver instantaneous response times and zero audio lag during rapid touch exploration and reading.

---

## Features

- **64 Distinct Voices**: 8 language dialects with 8 selectable voice presets each (Reed, Shelley, Bobby, Rocko, Glen, Sandy, Grandma, Grandpa).
- **Language and Regional Voice Support**: Native speech models for US English, UK English, Castilian Spanish, Latin American Spanish, French, Canadian French, German, and Italian.
- **Speech Rate Control**: Granular relative rate adjustment (-10 to +10) with an optional high-speed mode for advanced screen-reader users.
- **Pitch and Tone Shaping**: Adjust pitch, inflection, head size, roughness, and breathiness to customize voice personality.
- **Digital Volume Boost**: Clean PCM audio amplification with built-in clipping protection for noisy environments.
- **Punctuation Controls**: Five selectable punctuation verbosity levels (None, Some, Most, All, Custom) matching screen-reader preferences and user-defined symbol speech.
- **Intonation and Pause Controls**: Natural clause boundaries and customizable pause cadences.
- **Emoji and Emoticon Speech**: Full spoken descriptions for over 3,800 Unicode emojis and common text emoticons.
- **Number and Abbreviation Processing**: Context-aware expansion for common abbreviations and numbers without unexpected spelling pauses.
- **100% Offline Synthesis**: Zero network permissions required; all audio generation occurs locally on the processor.
- **Android TTS Integration**: Full compliance with the Android Text-to-Speech framework and screen readers like TalkBack.
- **Selectable Sampling Rates**: Supports both 11,025 Hz and 22,050 Hz output modes.
- **Accessibility-First Interface**: User interface designed with high-contrast elements, touch targets of 48dp or larger, and complete screen-reader labeling.

---

## Supported Languages and Voices

Eloqium provides 64 genuine voices across 8 language and regional variants:

| Language | Locale Code | Region | Default Voice |
| :--- | :--- | :--- | :--- |
| **English (US)** | `en-US` / `eng-USA` | United States | `eng-USA-Reed` |
| **English (UK)** | `en-GB` / `eng-GBR` | United Kingdom | `eng-GBR-Reed` |
| **Spanish (Spain)** | `es-ES` / `spa-ESP` | Spain | `spa-ESP-Reed` |
| **Spanish (Latin America)** | `es-MX` / `spa-MEX` | Latin America / Mexico | `spa-MEX-Reed` |
| **French (France)** | `fr-FR` / `fra-FRA` | France | `fra-FRA-Reed` |
| **French (Canada)** | `fr-CA` / `fra-CAN` | Canada | `fra-CAN-Reed` |
| **German** | `de-DE` / `deu-DEU` | Germany | `deu-DEU-Reed` |
| **Italian** | `it-IT` / `ita-ITA` | Italy | `ita-ITA-Reed` |

Each language includes 8 voice presets:
1. **Reed** (Default clear voice)
2. **Shelley**
3. **Bobby**
4. **Rocko**
5. **Glen**
6. **Sandy**
7. **Grandma**
8. **Grandpa**

---

## Installation

1. Download the latest release APK (`eloqium-tts-release.apk`) from the [Releases](https://github.com/memon-ismail/eloqium-tts/releases) page.
2. Open the downloaded file on your Android device and install it.
3. Launch the **Eloqium** app from your home screen or app drawer.

---

## Using Eloqium

1. Open the **Eloqium** application.
2. On the home screen, tap **System TTS Settings** to directly open Android's speech output settings.
3. Select **Eloqium TTS** as your Preferred Engine.
4. Return to the Eloqium home screen and tap **Eloqium settings** to customize your voice, speech rate, pitch, and punctuation preferences.

---

## Settings

The **Eloqium settings** screen allows you to fine-tune speech parameters:

- **Voice Profile**: Choose the default voice preset (Reed, Shelley, Bobby, etc.).
- **Speech Rate**: Adjust reading speed. Enable *Unlock High Speed* to access higher speaking rates.
- **Pitch**: Raise or lower the base voice pitch.
- **Volume**: Adjust digital output gain.
- **Voice Characteristics**: Fine-tune inflection, head size, roughness, and breathiness.
- **Punctuation Level**: Select None, Some, Most, All, or Custom:
  - **None**: Pure prosodic phrasing without spoken symbols.
  - **Some**: Verbalizes mathematical and syntax symbols (`*`, `/`, `\`, `#`, `%`, `&`, `+`, `=`, `@`, `^`, `~`, `<`, `>`, `|`, `$`).
  - **Most**: Some + structural delimiters (parentheses, brackets, braces, quotes, dashes, colons, semicolons).
  - **All**: All punctuation marks including sentence terminators (periods, commas, question marks, exclamation marks).
  - **Custom**: Verbalizes only user-specified characters entered in the custom punctuation field; all other marks remain prosodic.
- **Emoji and Emoticons**: Toggle spoken announcements for emojis and text smiles.
- **Number Processing**: Choose between natural numbers and individual digit reading.
- **Abbreviations**: Enable or disable context-sensitive abbreviation expansion.
- **Audio Sample Rate**: Select 11,025 Hz or 22,050 Hz.
- **Reset All Settings**: Restore all configuration values to their defaults.

---

## Android TTS Integration

Eloqium integrates with Android's system text-to-speech service architecture. It works with:
- **Android TalkBack** and other accessibility screen readers.
- In-app text reading, e-book readers, and GPS navigation apps.
- Dynamic language switching per sentence based on system locale requests.

When reading rapidly with TalkBack, Eloqium aborts previous speech instantly upon touch or swipe gestures, ensuring responsive exploration without lingering audio.

---

## Offline and Privacy

- **Zero Network Permissions**: The application does not request the `android.permission.INTERNET` permission in its manifest.
- **No Analytics or Telemetry**: No crash reporters, identifiers, or analytics libraries are present.
- **100% On-Device Processing**: Every piece of text is processed and synthesized entirely within the device's local memory.

---

## Building From Source

### Prerequisites
- Android SDK (API 35+) and Android NDK (version 26.1.10909125 or newer).
- JDK 17.
- Git with submodule support.

### Step 1: Clone the Repository
```bash
git clone --recurse-submodules https://github.com/memon-ismail/eloqium-tts.git
cd eloqium-tts
```

### Step 2: Build Native Libraries
```bash
./native/build-native.sh
```
This builds `libeloqiumjni.so` for `arm64-v8a`, `armeabi-v7a`, and `x86_64` with 16 KB ELF page-size alignment.

### Step 3: Run Standalone Test Suite
```bash
bash test/run-tests.sh
```

### Step 4: Assemble the Release APK
```bash
./gradlew assembleRelease
```
The compiled release APK will be located at `app/build/outputs/apk/release/app-release.apk`.

---

## Architecture

Eloqium processes speech requests through a thread-safe, unidirectional pipeline:

```
[Screen Reader / TTS Client]
             |
             v
[EloqiumTtsService (Android TextToSpeechService)]
             |
             +--> AbbreviationProcessor (Grammar & context expansion)
             +--> EmojiProcessor & EmoticonProcessor (Spoken descriptions)
             +--> NumberProcessor (Digits vs. numeric reading)
             +--> UnicodeNormalizer (NFKC canonical folding)
             +--> ScreenReaderPunctuationProcessor (Verbosity filtering)
             +--> OpenEVVCompatibilityFixes (Token & separator rules)
             +--> Chunker (Boundary-aware segmentation)
             +--> PauseProcessor (Clause markers)
             +--> OpenEVVEncoder (Latin-1 byte mapping)
             |
             v
[Native JNI Bridge (libeloqiumjni.so)]
             |
             v
[OpenEVV Synthesis Engine] ---> [Mutex-Synchronized Audio Ring Buffer] ---> [PCM Callback]
```

### Native Concurrency & Synchronization
The native bridge (`app/src/main/cpp/eloqium_jni.c`) coordinates background synthesis using:
- A circular ring buffer protected by `pthread_mutex_t` and `pthread_cond_t` condition variables (`room` and `filled`).
- Asynchronous synthesis on a dedicated native worker thread.
- Immediate synthesis abort upon `onStop()` calls, preventing buffer backlogs.
- Full 16 KB ELF segment page-size alignment compliant with modern Android requirements.

---

## Open-Source Components

- **OpenEVV (`libevv`)**: Open-source C speech synthesizer core (MIT License).
- **Android Platform (AOSP)**: Text-to-speech service contracts and system bindings (Apache License 2.0).
- **Unicode CLDR Data**: Multi-language emoji and symbol definitions (Unicode License).
- **EVVDroid**: Architecture reference for JNI audio streaming (Apache License 2.0).

---

## Third-Party Dependencies

Eloqium relies exclusively on open-source Android Jetpack and Kotlin libraries:
- `androidx.core:core-ktx`
- `androidx.activity:activity-compose`
- `androidx.compose:compose-bom` (Material 3, UI, Tooling Preview)
- `org.jetbrains.kotlinx:kotlin-stdlib`

---

## Dictionary and Data Licensing

- **Clean-Room Abbreviation Processor**: All abbreviations and disambiguation rules are implemented in Kotlin (`AbbreviationProcessor.kt`) under the Apache 2.0 license.
- **Proprietary Dictionaries Excluded**: Proprietary binary dictionary formats (`.cfd`, `.dct`, `.jdf`) are intentionally excluded to respect copyright boundaries.
- **Future Custom Pronunciation Support**: Any future user pronunciation features will utilize open-source, permissive dictionary pairs (such as CC0/MIT word lists) parsed entirely in memory.

---

## Contributing

Contributions are welcome! To contribute:
1. Fork the repository.
2. Create a focused feature branch (`git checkout -b feature/improvement`).
3. Ensure all automated tests pass (`bash test/run-tests.sh`).
4. Submit a pull request on GitHub with a clear summary of changes.

---

## Known Limitations

- **Language Scope**: Limited to the 8 supported language variants compiled into the core OpenEVV engine.
- **Sample Rate**: Native audio synthesis is 11,025 Hz or 22,050 Hz 16-bit mono PCM.

---

## Links

- **GitHub**: [https://github.com/memon-ismail/eloqium-tts](https://github.com/memon-ismail/eloqium-tts)
- **Connect on Telegram**: [https://t.me/blindroidofficial](https://t.me/blindroidofficial)

---

## License

```
Copyright (c) 2026 Ismail Memon and contributors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
