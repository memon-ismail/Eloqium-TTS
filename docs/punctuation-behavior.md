# Eloqium TTS — Punctuation & Prosodic Behavior

## 1. Overview

In Eloqium TTS, punctuation serves two distinct roles:
1. **Prosodic Punctuation**: Punctuation marks guide syntactic boundary detection, natural pauses, and pitch movement (such as declarative falling pitch on periods and interrogative rising pitch on question marks).
2. **Spoken Punctuation**: Punctuation symbols are pronounced aloud by name (e.g. "comma", "question mark") when reading code, navigating character-by-character, or under high verbosity.

Text preprocessing prepares punctuation tokens and ECI annotations before text reaches OpenEVV, ensuring that the native synthesis engine receives clean, correctly bound tokens.

---

## 2. Punctuation Levels & Custom Mode

Under **Settings → Text Processing**, users can toggle **Process punctuation** and select from 5 levels:

| Level | Mode | Behavior |
| :--- | :--- | :--- |
| **0** | **None** | Pure prosody. Punctuation shapes cadence, pauses, and pitch without spoken symbol names. |
| **1** | **Some** | Vocalizes mathematical and code symbols (`*`, `/`, `\`, `#`, `%`, `&`, `+`, `=`, `@`, `^`, `~`, `<`, `>`, `|`, `$`). Syntactic delimiters remain prosodic. |
| **2** | **Most** | Vocalizes `Some` plus brackets, quotes, dashes, colons, and semicolons. Sentence-ending periods and commas remain prosodic. |
| **3** | **All** | Vocalizes all punctuation marks, including sentence terminators (`.`, `,`, `?`, `!`). |
| **4** | **Custom** | Vocalizes **only** the exact symbols specified by the user in the *Custom punctuation* field. |

### Custom Punctuation Principles
- **Explicit Scope**: Speaks only the exact user-provided characters; it does not inherit characters from other levels.
- **Prosody for Unselected Marks**: Any punctuation mark omitted from the custom string remains active as prosodic punctuation, preserving natural clause boundaries and inflection.
- **Protection Rules**: Decimals (`3.14`), grouped numbers (`1,000`), times (`12:30`), abbreviations (`Dr.`), and phonetic SPR tags (`[...]`) remain protected against accidental splitting.
- **Empty Fallback**: If the custom string is left empty, the engine falls back safely to pure prosodic mode.

---

## 3. Screen Reader Interaction

Screen readers like Android TalkBack allow users to configure punctuation echo directly in accessibility settings:
- When TalkBack's punctuation echo is active, TalkBack replaces symbols with spoken words in its own layer before submitting text to Eloqium.
- Eloqium preserves incoming punctuation intact and directly attached to preceding words, preventing spurious spaces or artificial pause injection from disrupting the engine's clause boundaries.

---

## 4. Phrase Prediction & Pause Handling

- **Phrase Prediction**: Defaults to enabled in Eloqium (`phrasePrediction = true`). When enabled, text preprocessing prepends the ECI control tag (`` `pp1 ``) to synthesis chunks (or `` `pp0 `` when disabled). This control tag signals the downstream OpenEVV synthesizer to perform internal syntactic boundary analysis rather than using flat pitch.
- **Intonation Pauses**: Controlled via the *Intonation pauses* setting (`PAUSE_KEEP`), preserving standard clause pause intervals at commas, colons, semicolons, and sentence terminators.
- **Boundary Attachment**: During text preparation, punctuation marks are kept bound directly to preceding words (e.g. `"ready?"` rather than `"ready ?"`), allowing OpenEVV's parser to interpret terminal punctuation at clause boundaries.

---

## 5. Architectural Separation & Limitations

- **Text Preparation vs. Native Synthesis**: Eloqium's Kotlin pipeline is responsible for text preprocessing, symbol verbalization, and emitting ECI control tags. Downstream acoustic synthesis—including fundamental frequency movement, pitch contours, and phoneme timing—is computed by OpenEVV's native synthesis engine. The Kotlin pipeline provides the control tags, but does not itself generate acoustic waveforms or pitch tracks.
- **Playback Limitations**: Perceived acoustic output is governed by native engine tables and remains subject to Android system audio rendering and hardware output.
