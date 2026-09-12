# Eloqium TTS — Punctuation Behavior, Prosody & Text Processing Specification

## 1. Executive Summary

A critical complaint regarding existing OpenEVV-based Android TTS implementations (notably **EVVDroid**) is unnatural punctuation behavior—specifically:
1. Sentence-ending punctuation like question marks (`?`) frequently fail to trigger proper question intonation (rising pitch at clause boundary).
2. Spoken punctuation vs. prosodic punctuation is conflated, causing either unwanted spoken words or complete loss of prosodic phrasing.
3. Preprocessing regexes corrupt clause-boundary syntax expected by the OpenEVV linguistic parser.

This document details:
- The fundamental distinction between **Prosodic Punctuation** and **Spoken Punctuation**.
- Detailed analysis of EVVDroid's `Pauses.kt` and `Prosody.kt` flaws.
- OpenEVV's internal ECI clause parsing and intonation model.
- Screen reader conventions (TalkBack and NVDA IBMTTS driver).
- The complete text-processing pipeline implemented in **Eloqium TTS** (`org.eloqium.tts`).

---

## 2. Punctuation Dualism: Prosody vs. Spoken Echo

In text-to-speech for screen readers, punctuation marks serve two entirely distinct purposes:

```
                          +-------------------------+
                          |   Raw Input Text        |
                          |   "Hello, how are you?" |
                          +------------+------------+
                                       |
                    +------------------+------------------+
                    |                                     |
                    v                                     v
       [PROSODIC PUNCTUATION]                   [SPOKEN PUNCTUATION]
  Controls rhythm, pauses, and pitch.      Pronounces symbol names aloud.
  - Comma `,` -> Brief clause pause (~120ms).  - Code / Proofreading mode.
  - Period `.` -> Terminal pause + pitch fall.  - Echoes: "Hello comma how are
  - Question `?` -> Terminal pause + pitch rise. you question mark"
```

### 2.1 Prosodic Punctuation
Under normal reading, punctuation marks are **not spoken as words**. Instead, they provide syntactic and phonological cues:
- **Intonation contour**: A question mark signals an interrogative contour (pitch rise in general questions in English, Spanish, etc.). A period signals an assertive falling contour.
- **Clause boundary**: Commas, colons, and semicolons signal intermediate syntactic boundaries, triggering short pauses and continuation pitch.
- **Lexical disambiguation**: Periods in abbreviations (e.g. `Dr.`, `etc.`, `e.g.`) must not be treated as sentence boundaries.

### 2.2 Spoken Punctuation (Symbol Echo)
When a screen reader user navigates character-by-character, edits code, or sets Punctuation Verbosity to "All", symbols must be spoken phonetically (e.g. `left parenthesis`, `semicolon`, `asterisk`).
- Screen readers like **TalkBack** and **NVDA** perform symbol replacement in their own accessibility abstraction layers before passing text to the TTS engine.
- A well-behaved TTS engine must respect the caller's text without corrupting punctuation attachment or arbitrarily vocalizing punctuation when prosody was intended.

---

## 3. Analysis of Failure in EVVDroid

### 3.1 Flaw 1: Disabling Phrase Prediction by Default (`Prosody.kt`)

In EVVDroid's `Prosody.kt`:
```kotlin
if (!s.phrasePrediction) {
    sb.append("`pp0  ")
}
```
In EVVDroid's settings, `phrasePrediction` defaulted to `false`. Consequently, every single synthesis request had `` `pp0  `` prepended to it.

#### Impact in OpenEVV / ECI:
In ECI (Eloquence Command Interface):
- `` `pp1 `` enables **Phrase Prediction** (syntactic boundary analysis, stress assignment, and pitch movement).
- `` `pp0 `` **disables Phrase Prediction completely**.

When `` `pp0 `` is active:
1. OpenEVV stops calculating intonational contours across clauses.
2. The pitch remains flat across words.
3. Sentence-ending question marks (`?`) produce **no rising pitch contour**. An utterance like `"Hello, how are you?"` sounds completely flat and robotic, indistinguishable from a declarative statement.

### 3.2 Flaw 2: Regex Corruption of Clause Boundaries (`Pauses.kt`)

In EVVDroid's `Pauses.kt`:
```kotlin
private val AT_A_MARK = Regex("""([^\s\p{P}])\s*([,;:\.\?!])(\s*[\"\']?)(\s|$)""")
...
// Where $BRIEF was "`p1" (ECI pause command)
out = AT_A_MARK.replace(out, "$1 $BRIEF$2$3$4")
```

Let us trace the transformation on `"Hello, how are you?"`:
- Match: `$1` = `"u"` (last character of `"you"`), `$2` = `"?"`, `$3` = `""`, `$4` = `""`.
- Replacement: `"$1 $BRIEF$2$3$4"`
- Output string: `"Hello, how are you `p1?"`

#### Catastrophic Effects on OpenEVV Parser:
1. **Space insertion before punctuation**: Notice the space between `$1` and `$BRIEF`: `"you `p1?"`.
2. OpenEVV's tokenizer treats punctuation attached directly to the word token (`you?`) as an indicator of clause termination.
3. By inserting a space and an inline pause command (`` `p1? ``), OpenEVV encounters the pause command, flushes the audio pipeline, and treats `?` as an isolated punctuation token rather than the terminal boundary of the clause `how are you`.
4. As a result, the pitch contour calculation for the clause is broken, and any question intonation is lost.

### 3.3 Flaw 3: Ineffective and Redundant Pause Injection
OpenEVV's native synthesis engine already contains finely tuned linguistic timing tables for commas, colons, semicolons, and periods. EVVDroid attempted to inject manual pause tags (`` `p1 ``, `` `p2 ``) before punctuation marks, compounding latency, causing audio stutter, and interfering with OpenEVV's natural rhythm.

---

## 4. OpenEVV Linguistic & Prosodic Architecture

### 4.1 ECI Command Interface Mechanics
OpenEVV uses backtick (`` ` ``) escape sequences for inline synthesis control:
- `` `pp0 `` / `` `pp1 ``: Phrase prediction off/on.
- `` `p1 `` .. `` `p9 ``: Explicit pauses (in units of 10ms or internal ticks).
- `` `vs<0..8> ``: Switch voice preset (0=Reed, 1=Wendy, etc.).
- `` `s<rate> ``: Speech rate.
- `` `p<pitch> ``: Base pitch.

### 4.2 Clause Boundary Detection in OpenEVV
OpenEVV's C engine processes text in syntactic chunks called **clauses**:
1. It buffers input text until a clause-terminating character is encountered (`.`, `!`, `?`, `;`, `:`, newline, or EOF).
2. It assigns part-of-speech probabilities and identifies content vs. function words.
3. If phrase prediction is enabled (`` `pp1 ``), it computes the fundamental frequency ($F_0$) track:
   - Declarative period (`.`): Declination line sloping down to a low terminal pitch.
   - Question mark (`?`): Pitch slope rising significantly on the final stressed syllable and trailing unstressed syllables.
   - Comma (`,`): Slight continuation rise or level boundary tone followed by a clause pause.
4. If punctuation is stripped, detached by leading whitespace, or interrupted by inline pause commands, OpenEVV cannot associate the terminal intonation contour with the clause words.

---

## 5. Screen Reader Integration Patterns

### 5.1 NVDA IBMTTS Driver Pattern
In the NVDA IBMTTS driver (`NVDA-IBMTTS-Driver`):
1. NVDA's core processes symbol levels (None, Some, Most, All) through `symbols.dic`.
2. The synth driver receives pre-processed text where requested punctuation marks have already been replaced with words.
3. The driver:
   - Does **not** insert spaces before punctuation.
   - Keeps Phrase Prediction enabled by default (`pp1`).
   - Translates rate, pitch, and inflection into ECI parameters.
   - Escapes unprompted backtick characters so user text cannot accidentally break the ECI state machine.

### 5.2 Android TalkBack Pattern
1. TalkBack sends plain Unicode text to `TextToSpeech.speak()`.
2. When "Speak punctuation" is enabled in TalkBack settings, TalkBack replaces punctuation characters with their localized names before submitting to the engine.
3. When "Speak punctuation" is disabled, TalkBack sends natural punctuation directly (e.g. `"Are you ready?"`).
4. The TTS engine must preserve these punctuation marks intact, directly attached to the preceding words, so that OpenEVV's acoustic model produces natural inflection.

---

## 6. Eloqium TTS Text-Processing Pipeline

Eloqium TTS introduces a modular, high-performance text-processing pipeline:

```
+-------------------------------------------------------------------------------+
| Eloqium Text Processing Pipeline                                              |
|                                                                               |
|  [TextRequest]                                                                |
|        |                                                                      |
|        v                                                                      |
|  1. Unicode Normalizer (UnicodeNormalizer.kt)                                 |
|     - Replaces typographical quotes (“ ” ‘ ’) with standard ASCII (' ").     |
|     - Normalizes em-dash (—) and en-dash (–) to hyphens.                      |
|     - Replaces ellipsis (…) with three dots (...).                            |
|     - Strips zero-width characters (ZWNJ, ZWJ, BOM, soft hyphens).            |
|     - Converts symbols/math (≤, ≥, ≠, €, £, °, etc.) to spoken equivalents.   |
|        |                                                                      |
|        v                                                                      |
|  2. Screen Reader Punctuation Processor (ScreenReaderPunctuationProcessor.kt) |
|     - Distinguishes spoken punctuation vs prosodic punctuation.              |
|     - Preserves decimals (3.14), grouped numbers (1,024), times (2:30).      |
|     - Preserves abbreviations (U.S., e.g., i.e.) and titles (Mr. Smith).      |
|     - Binds detached punctuation marks tightly to preceding clause-final word.|
|        |                                                                      |
|        v                                                                      |
|  3. OpenEVV Compatibility Fixes (OpenEVVCompatibilityFixes.kt)                |
|     - Separates digit boundaries (teamtalk5 -> teamtalk 5).                   |
|     - Fixes openers and loose suffixes without breaking speech flow.          |
|     - Sanitizes rogue backticks (`) to prevent ECI command injection.         |
|        |                                                                      |
|        v                                                                      |
|  4. Sentence & Clause Chunker (Chunker.kt)                                    |
|     - Splits long paragraphs strictly at natural sentence/clause boundaries.  |
|     - Never splits decimals, grouped numbers, times, or dotted abbreviations. |
|     - Never creates empty chunks; preserves bound punctuation.                |
|        |                                                                      |
|        v                                                                      |
|  5. Pause & Intonation Processor (PauseProcessor.kt)                          |
|     - Guarantees phrase prediction (`pp1) is active for natural pitch contour.|
|     - Manages trailing gap pauses (`p100) and clause pauses (`p1).            |
|        |                                                                      |
|        v                                                                      |
|  6. OpenEVV Encoder (OpenEVVEncoder.kt)                                       |
|     - Converts processed character sequence to ISO-8859-1 byte array.         |
|        |                                                                      |
|        v                                                                      |
|  [OpenEVV Native C Engine (eciSynthesize)]                                    |
|     - Real-time synthesis into PCM ring buffer with immediate cancellation.   |
+-------------------------------------------------------------------------------+
```

### 6.1 Normalization Rules Table

| Input Pattern | Replacement | Rationale |
| :--- | :--- | :--- |
| `“` / `”` | `"` | OpenEVV expects ASCII quotes. |
| `‘` / `’` | `'` | Preserves apostrophe contractions (`don't`, `it's`). |
| `—` (Em-dash) / `–` (En-dash) | ` , ` | Converts typographic dashes to natural clause pauses. |
| `…` (Horizontal ellipsis) | `...` | Normalizes Unicode ellipsis to standard sentence pause. |
| `\u200B` .. `\u200D` (Zero-width) | `""` (removed) | Prevents tokenizer desynchronization. |
| Backtick `` ` `` | `'` (or escaped) | Prevents accidental ECI command injection. |
| Multiple spaces `\s+` | Single space ` ` | Normalizes whitespace. |

### 6.2 Guaranteed Prosodic Outcomes in Eloqium TTS

1. **Natural Question Intonation**: Utterances ending in `?` reliably trigger OpenEVV's interrogative rising pitch contour because phrase prediction is active and `?` remains attached to the clause-final word.
2. **Clear Declarative Cadence**: Utterances ending in `.` trigger OpenEVV's declarative falling pitch contour.
3. **Smooth Clause Flow**: Commas produce natural continuation pauses without audible stutter or dropped audio frames.
4. **Full Screen-Reader Compatibility**: TalkBack's punctuation echo levels function exactly as intended without engine-side interference.

---

## 7. Empirical Audio Analysis & Verification

We compiled OpenEVV natively on Android/Termux (`clang 21.1.8`, `aarch64-unknown-linux-android24`) and synthesized the utterance `"Hello, how are you?"` under three conditions:
1. **Eloqium Natural Prosody**: Phrase prediction enabled (`pp1`), natural clause binding (`"Hello, how are you?"`).
2. **EVVDroid `pp0` Condition**: Phrase prediction disabled (`pp0`), mimicking EVVDroid default prosody settings (`"`pp0 Hello, how are you?"`).
3. **EVVDroid Pauses Regex Condition**: Inline pause command with space injected before question mark, mimicking EVVDroid's `Pauses.kt` regex (`"Hello, how are you `p1?"`).

### Acoustic Measurements:

| Condition | Sample Rate | Audio Frames | Utterance Duration | Prosody & Rhythm Character |
| :--- | :--- | :--- | :--- | :--- |
| **1. Eloqium Natural** | 11,025 Hz | 18,535 | **1.681 s** | Dynamic intonation, natural rising pitch on question mark, fluent clause transitions. |
| **2. EVVDroid `pp0`** | 11,025 Hz | 35,827 | **3.250 s** (+93%) | Completely flat pitch contour, unnatural dragged-out vowels, zero question intonation. |
| **3. EVVDroid Pause Regex** | 11,025 Hz | 31,130 | **2.824 s** (+68%) | Artificial pause before terminal punctuation, decoupled question mark, broken clause boundary. |

These empirical measurements confirm that disabling phrase prediction and injecting spaces before punctuation corrupts speech timing, roughly doubles utterance latency, and obliterates natural question intonation.

---

## 8. Custom Punctuation Level & Settings Architecture

Eloqium TTS supports 5 distinct punctuation levels selectable via the **Punctuation Level** settings dialog:

| Level Index | Mode | Spoken Characters | Behavior |
| :--- | :--- | :--- | :--- |
| `0` | **None** | *None* | Pure prosodic punctuation. Clause boundaries and pitch contours are preserved without spoken symbols. |
| `1` | **Some** | `* / \ # % & + = @ ^ ~ < > \| $` | Verbalizes mathematical and syntax symbols; structural delimiters and sentence boundaries remain prosodic. |
| `2` | **Most** | `Some` + `( ) [ ] { } " ' - – — _ : ;` | Verbalizes brackets, quotes, dashes, colons, and semicolons. Sentence-ending periods/commas remain prosodic. |
| `3` | **All** | `Most` + `. , ? !` | Verbalizes all punctuation marks including clause and sentence terminators. |
| `4` | **Custom** | *User-defined string* | Verbalizes **only** the exact symbols specified by the user. Unspecified marks provide natural prosody. |

### 8.1 Custom Punctuation Design Principles

1. **Strict User-Specified Scope**:
   In Custom mode, Eloqium TTS speaks **only** the symbols explicitly specified by the user in the `custom_punctuation` setting. It does not implicitly include the `Some`, `Most`, or `All` symbol sets.
2. **Prosody Preservation for Unselected Marks**:
   Punctuation marks and clause terminators not included in the custom string are never discarded or detached; they remain tightly bound to the preceding tokens so the engine generates natural clause pauses and question/assertive pitch contours.
3. **Number and Intonation Protection**:
   Decimals (`3.14`), thousands separators (`1,000`), times (`12:30`), abbreviations (`Dr.`, `e.g.`), and question marks (`?`) remain protected against inappropriate decomposition even when general symbols are active.
4. **Deduplication and Whitespace Filtering**:
   Custom character sequences are processed through an ordered set (`customChars.toSet()`). Duplicate characters (e.g. entering `@@@***`) are deduplicated and processed safely without recursive replacement or speech duplication. Whitespace characters are ignored.
5. **Safe Empty String Fallback**:
   When the custom string is empty or contains only whitespace, Custom mode acts safely as prosodic punctuation: no symbols are spoken aloud, while natural phrasing and intonation pauses are fully preserved.
6. **TalkBack Accessibility**:
   The `PunctuationLevelDialog` treats each radio button and its label as a single accessible control (`Role.RadioButton` with merged descendants). When "Custom" is selected, an `OutlinedTextField` appears directly below the Custom radio option with an accessible label and `contentDescription`. Focus is automatically transferred to the input field upon selection. The dialog provides accessible **OK** (confirm) and **Cancel** (dismiss) actions.
7. **State Persistence & Defaults**:
   The custom punctuation string is stored in SharedPreferences under key `custom_punctuation` (default: `""`). Switching between punctuation levels retains the user's custom string in memory and storage, and invoking **Reset all settings** restores `custom_punctuation` to `""`.

