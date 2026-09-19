# Eloqium TTS

<p align="center">
  <img src="app/src/main/res/drawable/eloqium_logo.png" alt="Eloqium TTS Logo" width="160" height="160" />
</p>

<p align="center">
  <strong>Fast, intelligible, and offline text-to-speech engine for Android accessibility</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-0.1.4-blue.svg" alt="Version 0.1.4" />
  <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="Apache 2.0 License" />
  <img src="https://img.shields.io/badge/Platform-Android_6.0%2B-green.svg" alt="Android 6.0+" />
  <img src="https://img.shields.io/badge/Architecture-ARM64%20%7C%20ARMv7%20%7C%20x86__64-orange.svg" alt="Architecture" />
  <img src="https://img.shields.io/badge/Synthesis-On--Device_Offline-brightgreen.svg" alt="On-Device Offline" />
  <img src="https://img.shields.io/badge/Voices-64_Voices-blueviolet.svg" alt="64 Voices" />
</p>

---

## About

Eloqium TTS is an independent open-source text-to-speech engine designed for Android screen-reader users and everyday speech output. It pairs an optimized native build of the OpenEVV formant synthesis engine with modern Android accessibility services to deliver responsive, clear, and fully offline speech synthesis.

The project was created and is maintained by **Ismail Memon**, with special thanks to Chandu Rathod and Yashraj Shinde.

---

## Features

- **64 Distinct Voices**: 8 regional language variants with 8 selectable voice presets each (Reed, Shelley, Bobby, Rocko, Glen, Sandy, Grandma, Grandpa).
- **Multilingual Support**: Speech models across 5 language families: US English, UK English, Castilian Spanish, Latin American Spanish, French, Canadian French, German, and Italian.
- **Granular Speech Controls**: Configurable speech rate (with an optional maximum rate unlock mode), pitch, native sampling rate (8,000 Hz to 48,000 Hz), and digital volume scaling.
- **Voice Characteristics**: Fine-tune inflection, vocal tract head size, roughness, and breathiness.
- **Punctuation Modes**: Five selectable punctuation levels (None, Some, Most, All, Custom) respecting screen-reader preferences and user-defined symbol speech.
- **Capitals Indication**: Announce uppercase letters via pitch raise (cleanly raising pitch for standalone single-letter words) or explicit verbalization (None, Pitch raise, Say capital).
- **Background Speech Reliability**: Dedicated *Eloqium foreground service* with persistent notification and battery optimization controls to improve speech reliability when running in the background, particularly on devices with restrictive background management.
- **User Dictionary**: Multi-lingual text replacements and OpenEVV SPR phonetic pronunciation overrides, IBM `.dic` import, Schema v2 JSON import/export, and real-time search.
- **Emoji and Emoticons**: Spoken descriptions for 3,805 Unicode 16.0 emojis and conservative ASCII emoticons across 5 language families.
- **Number & Abbreviation Processing**: Context-aware abbreviation expansion and selectable number formatting (Digits, Pairs, Triplets, and Smart grouping for phone numbers and OTP codes).
- **On-Device Offline Architecture**: Operates entirely locally with zero telemetry and does not request Android network permissions.
- **Accessible Interface**: Built with high-contrast UI, minimum 48dp touch targets, standard graphical vector navigation icons, and full TalkBack semantics.

---

## Supported Languages and Voices

Eloqium provides 64 genuine voices (8 regional variants &times; 8 presets):

| Regional Variant | Locale Code | Region | Default Canonical Voice |
| :--- | :--- | :--- | :--- |
| **English (US)** | `en-US` / `eng-USA` | United States | `eng-USA-Reed` |
| **English (UK)** | `en-GB` / `eng-GBR` | United Kingdom | `eng-GBR-Reed` |
| **Spanish (Spain)** | `es-ES` / `spa-ESP` | Spain | `spa-ESP-Reed` |
| **Spanish (Latin America)** | `es-MX` / `spa-MEX` | Latin America / Mexico | `spa-MEX-Reed` |
| **French (France)** | `fr-FR` / `fra-FRA` | France | `fra-FRA-Reed` |
| **French (Canada)** | `fr-CA` / `fra-CAN` | Canada | `fra-CAN-Reed` |
| **German (Germany)** | `de-DE` / `deu-DEU` | Germany | `deu-DEU-Reed` |
| **Italian (Italy)** | `it-IT` / `ita-ITA` | Italy | `ita-ITA-Reed` |

Voice Presets: **Reed** (default), **Shelley**, **Bobby**, **Rocko**, **Glen**, **Sandy**, **Grandma**, and **Grandpa**.

---

## Installation & Setup

1. Download `eloqium-tts-release.apk` from the [Releases](https://github.com/memon-ismail/eloqium-tts/releases) page.
2. Install the APK on your Android device (Android 6.0+).
3. Open the **Eloqium** app and tap **System TTS Settings** on the home screen.
4. Select **Eloqium TTS** as your Preferred Engine.
5. Tap **Eloqium settings** on the home screen to customize voice, rate, pitch, and punctuation preferences.

---

## Eloqium Settings

The settings screen provides seven organized categories:

1. **Voice**: Select voice preset, configure speech rate (with *Force rate* and *Unlock maximum rate*), base pitch (with *Force pitch*), output volume (with *Force volume* digital gain scaling), and toggle inline *ECI Voice Tags*.
2. **Voice Characteristics**: Adjust *Inflection*, *Head size*, *Roughness*, and *Breathiness* on a relative -10 to +10 scale.
3. **Text Processing**: Configure *Emoji / Emoticon* vocalization, *Process punctuation* toggle, *Punctuation level* (None, Some, Most, All, Custom), *Custom punctuation* symbol list, *Number processing* mode (Digits, Pairs, Triplets, Smart), *Use abbreviations*, and *Intonation pauses*.
4. **User Dictionary**: Global dictionary toggle and navigation to the multi-lingual Dictionary Manager.
5. **Language & Audio**: Configure *Force language*, default *Language*, native synthesis *Sampling rate* (8,000 Hz to 48,000 Hz; default 11,025 Hz), and *Capitals indication* (None, Pitch raise, Say capital).
6. **Advanced**: Configure *Eloqium foreground service* (with *App notifications*, *Show persistent notification*, and *Battery optimization* controls to improve speech reliability when running in the background).
7. **Reset**: Reset all configuration preferences to default values.

---

## Architecture

Eloqium processes speech requests through a thread-safe, sequential linear pipeline:

```
[Incoming Synthesis Request]
             │
             ▼
 1. Text Preprocessing          (User dictionary, abbreviations, emojis, emoticons, numbers)
             │
             ▼
 2. Normalization & Formatting  (Unicode normalization, punctuation verbosity, capitals indication)
             │
             ▼
 3. Synthesis Preparation       (Opener fixes, backtick sanitization, sentence chunking, pause tags)
             │
             ▼
 4. OpenEVV Native Core      (Asynchronous native worker thread, circular PCM ring buffer)
             │
             ▼
 5. Android Audio Stream     (SynthesisCallback streaming, digital gain if Force volume enabled)
```

- **Immediate Interruption**: Rapid speech cancellations (such as TalkBack touch exploration) invoke `NativeEngine.stop()` via `eciDataAbort`, promptly flushing internal buffers.
- **Router Compatibility**: Adheres to standard Android `TextToSpeechService` contracts with canonical BCP-47 locale handling to support dynamic language routers and screen readers.

---

## Offline and Privacy

- **Zero Network Permissions**: The application does not request `android.permission.INTERNET` and performs no network communication.
- **No Telemetry**: No crash analytics, tracking identifiers, or remote telemetry libraries are included. All speech processing executes entirely on-device.

---

## Building From Source

### Prerequisites
- Android SDK (compileSdk 36, minSdk 23) and NDK (version 26.1+).
- JDK 17.
- Git with submodule support.

```bash
# Clone with submodules
git clone --recurse-submodules https://github.com/memon-ismail/eloqium-tts.git
cd eloqium-tts

# Build native libraries (ARM64, ARMv7, x86_64)
./native/build-native.sh

# Run standalone regression suite
bash test/run-tests.sh

# Assemble release APK
./gradlew assembleRelease
```

---

## Known Limitations

- **Language Scope**: Limited to the 8 compiled regional variants in the OpenEVV core.
- **Formant Synthesis**: Voice preset changes apply per utterance; inline preset tags do not alter vocal tract models during ongoing formant synthesis.
- **Dictionary Storage**: IBM `.dic` files serve as an external import format; internal storage uses native JSON Schema Version 2.
- **Audio Output**: Streams 16-bit mono linear PCM audio at selectable sampling rates (8 kHz to 48 kHz).

---

## Technical Documentation

Detailed subsystem specifications are available in the `docs/` directory:
- [User Dictionary Specification](docs/user-dictionary.md): Dictionary model, matching modes, precedence, and IBM `.dic` import.
- [Punctuation & Intonation Specification](docs/punctuation-behavior.md): Punctuation levels, custom punctuation, and phrase prediction.
- [Emoji & Emoticon Specification](docs/emoji-emoticon-pipeline.md): Unicode 16.0 trie matching, emoticon protection, and spoken descriptions.
- [Engine & Router Compatibility](docs/engine-router-compatibility.md): Canonical voice names, aliases, and Android TTS routing contracts.
- [Architectural Provenance](PROVENANCE.md): Upstream OpenEVV lineage, EVVDroid lineage, and licensing boundaries.

---

## License

Copyright (c) 2026 Ismail Memon and contributors. Licensed under the [Apache License, Version 2.0](LICENSE).  
Native OpenEVV core is licensed under the MIT License with the IBM Speech Synthesis Language Rules Notice. See [NOTICE](NOTICE) and [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) for details.
