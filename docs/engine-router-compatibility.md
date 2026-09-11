# Eloqium TTS — Android TextToSpeechService Lifecycle and Multilingual Client Routing Specification

## 1. Executive Summary

Android screen-reader users and accessibility tools frequently rely on multilingual engine-switchers and TTS routers to dynamically route speech requests to different TTS engines depending on the language or script of the text.

While previous implementations like **EVVDroid** functioned when selected directly as the primary system TTS engine, they failed completely (producing zero audio or throwing silent exceptions) when invoked through intermediary TTS routers and external client wrappers.

This specification documents:
1. The formal Android `TextToSpeechService` contract.
2. The exact mechanism of client-side locale serialization and deserialization.
3. The root-cause mechanism that produces total silence in EVVDroid under external client routing.
4. The secondary voice-preset selection bug in EVVDroid.
5. The language availability matching bug affecting strict-equality router checks.
6. The definitive architectural solution implemented in **Eloqium TTS** (`org.eloqium.tts`).

---

## 2. Android TextToSpeech IPC Architecture & Contract

### 2.1 The Two Interfaces: Client vs. Service

Android's TTS subsystem operates over Binder IPC:
- **Client layer**: `android.speech.tts.TextToSpeech` (used by applications, screen readers, and engine switchers).
- **Service layer**: `android.speech.tts.TextToSpeechService` (implemented by the engine service in its own process).

```
+--------------------------------------------------------------------+
| Application / Screen Reader / TTS Client (e.g. TalkBack, AOSP Client)  |
|   -> android.speech.tts.TextToSpeech (AOSP Client Library)         |
+---------------------------------+----------------------------------+
                                  | Binder IPC (ITextToSpeechService)
+---------------------------------v----------------------------------+
| Eloqium TTS Service (org.eloqium.tts)                              |
|   -> android.speech.tts.TextToSpeechService (AOSP Service Base)    |
|   -> EloqiumTtsService (Service Implementation)                    |
+--------------------------------------------------------------------+
```

### 2.2 Core Service Lifecycle Methods

A conformant `TextToSpeechService` must implement the following lifecycle hooks:

| Method | Purpose | Return Values / Expectations |
| :--- | :--- | :--- |
| `onIsLanguageAvailable(lang, country, variant)` | Tests if language/country/variant is supported. | `LANG_COUNTRY_VAR_AVAILABLE` (2)<br>`LANG_COUNTRY_AVAILABLE` (1)<br>`LANG_AVAILABLE` (0)<br>`LANG_NOT_SUPPORTED` (-2) |
| `onLoadLanguage(lang, country, variant)` | Pre-initializes native acoustic models / dictionaries. | Same integer status codes as `onIsLanguageAvailable`. |
| `onGetLanguage()` | Returns current language as 3-element `String[]` `[ISO3Lang, ISO3Country, Variant]`. | Array of 3 strings. |
| `onGetVoices()` | Returns all available voices as `List<Voice>`. | Each `Voice` contains `(name, locale, quality, latency, requiresNetwork, features)`. |
| `onGetDefaultVoiceNameFor(lang, country, variant)` | Returns default voice name for locale. | Unique voice identifier string. |
| `onLoadVoice(voiceName)` | Pre-loads a specific voice. | `SUCCESS` (0) or `ERROR` (-1). |
| `onSynthesizeText(request, callback)` | Synthesizes text; streams PCM to `SynthesisCallback`. | Must call `callback.start()`, `callback.audioAvailable()`, and `callback.done()`. |

---

## 3. The Root Cause of Engine-Switcher Incompatibility

### 3.1 The Bug in EVVDroid's `onGetVoices()`

In EVVDroid's `native/Eci.kt`:
```kotlin
fun localeOf(language: Int): Triple<String, String, String> = when (language) {
    LANG_AMERICAN_ENGLISH -> Triple("eng", "USA", "")
    LANG_BRITISH_ENGLISH -> Triple("eng", "GBR", "")
    LANG_CASTILIAN_SPANISH -> Triple("spa", "ESP", "")
    LANG_MEXICAN_SPANISH -> Triple("spa", "MEX", "")
    LANG_FRENCH -> Triple("fra", "FRA", "")
    LANG_CANADIAN_FRENCH -> Triple("fra", "CAN", "")
    LANG_GERMAN -> Triple("deu", "DEU", "")
    LANG_ITALIAN -> Triple("ita", "ITA", "")
    LANG_BRAZILIAN_PORTUGUESE -> Triple("por", "BRA", "")
    else -> Triple("und", "", "")
}
```

In EVVDroid's `EvvTtsService.kt`:
```kotlin
override fun onGetVoices(): MutableList<Voice> {
    val out = ArrayList<Voice>()
    for (language in Eci.languages) {
        val loc = Eci.localeOf(language)
        val locale = Locale(loc.first, loc.second) // BUG: Locale("eng", "USA")
        for (preset in 0 until Eci.VOICE_COUNT) {
            out.add(Voice(voiceName(language, preset), locale, ...))
        }
    }
    return out
}
```

EVVDroid constructed Java `Locale` objects using three-letter strings for both language and country: `Locale("eng", "USA")`.

### 3.2 Java Locale Specification & `getISO3Country()`

In Java (`java.util.Locale`), the constructor `Locale(String language, String country)` expects:
- `language`: An ISO 639-1 two-letter code (e.g., `"en"`).
- `country`: An ISO 3166-1 alpha-2 two-letter code (e.g., `"US"`).

When `Locale("eng", "USA")` is instantiated:
1. `locale.getLanguage()` returns `"eng"`.
2. `locale.getCountry()` returns `"USA"`.
3. `locale.toString()` produces `"eng_USA"`.

When `locale.getISO3Country()` is subsequently called on this instance:
- The Java runtime looks up the country string in its internal table of ISO 3166-1 alpha-2 country codes to convert it to a 3-letter code.
- Because `"USA"` is already 3 letters and is NOT in the 2-letter table, Java throws:
```
java.util.MissingResourceException: Couldn't find 3-letter country code for USA
    at java.util.Locale.getISO3Country(Locale.java:1255)
```

### 3.3 The External Client Routing Lifecycle Failure

When an external multi-engine routing service or accessibility switcher manages TTS dispatch:

#### Step 1: Voice Discovery & Storage
1. The routing client queries the target TTS engine via `tts.getVoices()`.
2. In implementations with the flaw, it receives voices whose `voice.getLocale()` is `Locale("eng", "USA")`.
3. The router builds its internal voice cache key from `voice.getLocale().toString()`, storing `"eng_USA"`.

#### Step 2: Language Switching During Synthesis
When the router dispatches an utterance to the engine:
1. It reconstructs the locale from the stored string (`parts = str.split("_")` $\to$ `new Locale("eng", "USA")`).
2. It calls `childTts.setLanguage(targetLocale)`.

#### Step 3: Failure Inside AOSP `TextToSpeech.java`
Inside the Android Framework's `android.speech.tts.TextToSpeech.java`:
```java
public int setLanguage(final Locale loc) {
    return runAction(new Action<Integer>() {
        @Override
        public Integer run(ITextToSpeechService service) throws RemoteException {
            if (loc == null) {
                return LANG_NOT_SUPPORTED;
            }
            String language = null;
            String country = null;
            String variant = null;
            try {
                language = loc.getISO3Language();
            } catch (MissingResourceException e) {
                Log.w(TAG, "Couldn't retrieve ISO 639-2/T language code for locale: " + loc, e);
                return LANG_NOT_SUPPORTED;
            }
            try {
                country = loc.getISO3Country(); // CRITICAL FAILURE: Throws MissingResourceException!
            } catch (MissingResourceException e) {
                Log.w(TAG, "Couldn't retrieve ISO 3166 country code for locale: " + loc, e);
                return LANG_NOT_SUPPORTED; // RETURNS -2 IMMEDIATELY!
            }
            variant = loc.getVariant();
            return service.loadLanguage(getCallerIdentity(), language, country, variant);
        }
    }, LANG_NOT_SUPPORTED, "setLanguage");
}
```

Because `loc.getISO3Country()` throws `MissingResourceException`, `TextToSpeech.setLanguage()` **immediately aborts without sending any IPC call to the engine service** and returns `LANG_NOT_SUPPORTED` (`-2`).

#### Step 4: Router Abort & Silence
The routing client checks the return code of `setLanguage`:
If it receives `-2` or `-1`, it immediately closes the utterance without emitting audio buffers. The result is instantaneous silence.

---

## 4. Direct System Selection Behavior

When EVVDroid was selected directly in Android's "Text-to-speech output" settings as the primary system engine:
1. System screen readers do not serialize and deserialize `Voice.getLocale().toString()`.
2. The system initializes the engine with `Locale.getDefault()`.
3. On a US English Android device, `Locale.getDefault()` is `Locale("en", "US")`.
4. In AOSP `TextToSpeech.java`:
   - `Locale("en", "US").getISO3Language()` -> `"eng"` (success).
   - `Locale("en", "US").getISO3Country()` -> `"USA"` (success).
   - AOSP IPC sends `loadLanguage("eng", "USA", "")` to the engine.
5. In EVVDroid's `EvvTtsService.kt`:
   ```kotlin
   matchLanguage("eng", "USA") -> LANG_AMERICAN_ENGLISH (0x00010000)
   ```
   The match succeeded, so speech played normally when called directly by the system default engine configuration.

The bug only materialized when intermediary routers queried `getVoices()`, stored serialized locales, and attempted to switch engines using those locales.

---

## 5. Secondary Architectural Flaws in EVVDroid

### 5.1 The Voice Selection Preset Ignored Bug

In `EvvTtsService.kt`:
```kotlin
override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
    ...
    val wanted = findVoice(request.voiceName)
    val language = wanted?.first ?: found.first
    val target = ensureEngine(language)
    // BUG: ensureEngine initializes the engine with s.voice (user's setting in EVVDroid UI):
    // Eci.setParam(hEngine, Eci.PARAM_VOICE, s.voice)
    // It COMPLETELY IGNORES wanted?.second (the voice preset requested by the caller)!
}
```
When a client application selected a specific voice preset (e.g. `Shelley`, `Grandpa`, `Rocko`), EVVDroid ignored the request and spoke with whatever preset was selected inside its own UI settings.

### 5.2 Strict Equality Language Availability Checks

Certain third-party TTS client applications check language availability using strict equality:
```java
int available = tts.isLanguageAvailable(locale);
if (available == TextToSpeech.LANG_AVAILABLE) {
    // client activates engine
} else {
    // client marks engine unavailable
}
```
Android's `TextToSpeech` specifies that `isLanguageAvailable` returns:
- `LANG_AVAILABLE` = 0 (language supported, country unknown)
- `LANG_COUNTRY_AVAILABLE` = 1 (language and country supported)
- `LANG_COUNTRY_VAR_AVAILABLE` = 2 (language, country, and variant supported)

Because EVVDroid returned `LANG_COUNTRY_AVAILABLE` (`1`) when country was specified, a client application's strict equality check `available == 0` evaluated to false, causing it to mark the engine as unsupported.

---

## 6. Eloqium TTS Architecture & Solution

Eloqium TTS implements a clean, standard-compliant architecture designed for 100% interoperability with all engine routers, screen readers, and direct Android TTS clients.

```
+-------------------------------------------------------------------------------+
| EloqiumTtsService (org.eloqium.tts)                                           |
|                                                                               |
|  1. Canonical Locale Management                                               |
|     - Uses standard BCP-47 / ISO 639-1 (2-letter) + ISO 3166-1 (2-letter):    |
|       Locale("en", "US"), Locale("en", "GB"), Locale("es", "ES"), etc.        |
|     - Full ISO-639-1 <-> ISO-639-2/T <-> ECI Language ID mapping table.       |
|                                                                               |
|  2. Robust Locale Normalizer (LocaleMatcher)                                  |
|     - Normalizes incoming (lang, country, variant) whether passed as:         |
|       ("en", "US"), ("eng", "USA"), ("eng", "US"), ("en", "USA"), or "en-US"|
|     - Safely resolves language without throwing MissingResourceException.     |
|                                                                               |
|  3. Compliant Voice Metadata                                                  |
|     - Voice objects contain canonical BCP-47 locales.                         |
|     - Voice names uniquely encode (language, preset): "eloqium_en_us_reed"    |
|                                                                               |
|  4. Dynamic Voice Selection                                                   |
|     - onSynthesizeText parses request.voiceName and applies the exact preset   |
|       to OpenEVV before speech generation.                                    |
|                                                                               |
|  5. Permissive Language Availability                                          |
|     - onIsLanguageAvailable returns LANG_COUNTRY_AVAILABLE (1) when country   |
|       matches, LANG_AVAILABLE (0) when language matches without country.       |
+-------------------------------------------------------------------------------+
```

### 6.1 Eloqium Language Mapping Matrix

| Language | Canonical BCP-47 | ISO 639-1 | ISO 639-2/T | ISO 3166-1 alpha-2 | ISO 3166-1 alpha-3 | OpenEVV ID |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| American English | `en-US` | `en` | `eng` | `US` | `USA` | `0x00010000` |
| British English | `en-GB` | `en` | `eng` | `GB` | `GBR` | `0x00020000` |
| Castilian Spanish| `es-ES` | `es` | `spa` | `ES` | `ESP` | `0x00030000` |
| Mexican Spanish | `es-MX` | `es` | `spa` | `MX` | `MEX` | `0x00040000` |
| French | `fr-FR` | `fr` | `fra` | `FR` | `FRA` | `0x00050000` |
| Canadian French | `fr-CA` | `fr` | `fra` | `CA` | `CAN` | `0x00060000` |
| German | `de-DE` | `de` | `deu` | `DE` | `DEU` | `0x00070000` |
| Italian | `it-IT` | `it` | `ita` | `IT` | `ITA` | `0x00080000` |
| Brazilian Portuguese | `pt-BR` | `pt` | `por` | `BR` | `BRA` | `0x00090000` |

### 6.2 Eloqium Voice Preset Matrix

Each language supports OpenEVV's 8 standard voice presets:

| Preset ID | Preset Constant | Preset Name | Voice Identifier Format |
| :--- | :--- | :--- | :--- |
| 0 | `ECI_VOICE_ADULT_MALE` | Reed | `eloqium_<lang>_<country>_reed` |
| 1 | `ECI_VOICE_ADULT_FEMALE` | Wendy | `eloqium_<lang>_<country>_wendy` |
| 2 | `ECI_VOICE_CHILD` | Bobby | `eloqium_<lang>_<country>_bobby` |
| 3 | `ECI_VOICE_ELDERLY_FEMALE`| Grandma | `eloqium_<lang>_<country>_grandma` |
| 4 | `ECI_VOICE_ELDERLY_MALE` | Grandpa | `eloqium_<lang>_<country>_grandpa` |
| 5 | `ECI_VOICE_YOUNG_FEMALE` | Shelley | `eloqium_<lang>_<country>_shelley` |
| 6 | `ECI_VOICE_YOUNG_MALE` | Rocko | `eloqium_<lang>_<country>_rocko` |
| 7 | `ECI_VOICE_FEMALE_WHISPER`| Glen | `eloqium_<lang>_<country>_glen` |

### 6.3 Implementation Guarantee

By standardizing on canonical `Locale(iso2Lang, iso2Country)`:
1. `voice.getLocale().toString()` returns `"en_US"`.
2. Client routers deserialize cleanly to `new Locale("en", "US")`.
3. In `TextToSpeech.setLanguage(new Locale("en", "US"))`:
   - `loc.getISO3Language()` -> `"eng"` (succeeds without exception).
   - `loc.getISO3Country()` -> `"USA"` (succeeds without exception).
4. The client receives `LANG_COUNTRY_AVAILABLE` (1) or `LANG_AVAILABLE` (0).
5. IPC call to Eloqium TTS executes successfully, streaming synthesis audio to the client.
6. Total compatibility with multi-engine routers, accessibility tools, and direct Android TTS clients is guaranteed.
