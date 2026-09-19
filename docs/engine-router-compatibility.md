# Eloqium TTS — Engine & Router Compatibility Specification

## 1. Overview

Eloqium TTS implements the standard Android `TextToSpeechService` service contract. Screen readers, multilingual language switchers, and accessibility tools interact with Eloqium over Android's Binder IPC to query language availability, enumerate voices, and stream synthesized audio.

This document describes Eloqium's voice naming conventions, locale representations, and request resolution behavior.

---

## 2. Canonical Voice Identifiers & Aliases

Eloqium defines 64 genuine voices (8 regional language variants &times; 8 voice presets).

### Canonical Identifier Format
Voice identifiers follow the canonical pattern:
`{iso3Lang}-{iso3Country}-{presetName}` (e.g. `eng-USA-Reed`, `spa-ESP-Shelley`, `fra-FRA-Rocko`).

### Multi-Alias Resolution
To maintain compatibility with third-party routers and legacy configurations, incoming voice requests resolve through alias mapping:
- **Canonical names**: `eng-USA-Reed`, `deu-DEU-Grandpa`
- **Case & delimiter variations**: `eng_usa_reed`, `eng-usa-reed`
- **Legacy naming**: `eloqium_en_us_reed`, `eloqium-en-us-reed`
- **Short locale codes**: `en-us-reed`, `eng-us-reed`
- **Preset-only requests**: `Reed`, `Shelley`, `Bobby`, `Rocko`, `Glen`, `Sandy`, `Grandma`, `Grandpa`
- **Router wildcards & defaults**: `*Default`, `default`, `*`, `""`, or `null` resolve to the language's default voice.

---

## 3. Supported Locales & Preset Mapping

### Supported Regional Variants
| Regional Variant | BCP-47 Tag | ISO-639-1 / ISO-3166-1 | ISO-639-2 / ISO-3166-3 | Default Voice |
| :--- | :--- | :--- | :--- | :--- |
| American English | `en-US` | `en` / `US` | `eng` / `USA` | `eng-USA-Reed` |
| British English | `en-GB` | `en` / `GB` | `eng` / `GBR` | `eng-GBR-Reed` |
| Castilian Spanish | `es-ES` | `es` / `ES` | `spa` / `ESP` | `spa-ESP-Reed` |
| Mexican Spanish | `es-MX` | `es` / `MX` | `spa` / `MEX` | `spa-MEX-Reed` |
| French | `fr-FR` | `fr` / `FR` | `fra` / `FRA` | `fra-FRA-Reed` |
| Canadian French | `fr-CA` | `fr` / `CA` | `fra` / `CAN` | `fra-CAN-Reed` |
| German | `de-DE` | `de` / `DE` | `deu` / `DEU` | `deu-DEU-Reed` |
| Italian | `it-IT` | `it` / `IT` | `ita` / `ITA` | `ita-ITA-Reed` |

### Voice Presets
Each language includes 8 presets: **Reed** (0), **Shelley** (1), **Bobby** (2), **Rocko** (3), **Glen** (4), **Sandy** (5), **Grandma** (6), and **Grandpa** (7), corresponding to native ECI voice slots 1 through 8.

---

## 4. Android TTS Lifecycle & Routing Behavior

Eloqium implements core AOSP `TextToSpeechService` hooks:
- **`onIsLanguageAvailable(lang, country, variant)`**: Evaluates locale availability. Returns `LANG_COUNTRY_AVAILABLE` (1) when both language and country match, and `LANG_AVAILABLE` (0) when language matches without country. Returns `LANG_NOT_SUPPORTED` (-2) for unsupported languages.
- **`onGetVoices()`**: Emits the 64 voices, each instantiated with standard two-letter BCP-47 `Locale` instances (`Locale("en", "US")`, etc.). Standardizing on two-letter locales prevents `MissingResourceException` errors in AOSP clients querying `Locale.getISO3Country()`.
- **`onGetDefaultVoiceNameFor(lang, country, variant)`**: Resolves the default voice descriptor matching the requested locale.
- **`onSynthesizeText(request, callback)`**: Parses `request.voiceName` and `request.speechRate` / `pitch`. Applies the requested voice preset dynamically to the native engine before streaming 16-bit linear PCM audio to the client.
- **`onStop()`**: Responds to immediate speech interruption by signaling `eciDataAbort`, resetting queues to halt audio playback promptly.

---

## 5. Compatibility Limitations

- **Engine Scope**: Voice selection is restricted to the 8 compiled language variants and 8 presets.
- **AOSP Adherence**: Designed around standard AOSP Binder IPC contracts; compatibility is not guaranteed with non-standard or proprietary routers that violate Android TTS lifecycle sequencing.
