# Eloqium TTS — User Dictionary System (v0.1.3)

## 1. Overview

Eloqium TTS v0.1.3 features an expanded, accessibility-first **User Dictionary** subsystem. The User Dictionary is a user-controlled text replacement and pronunciation engine that transforms matching source words and phrases into user-defined replacement text or phonetic pronunciations prior to speech synthesis.

It empowers screen-reader users, students, and everyday listeners to customize how words, abbreviations, proper names, technical terminology, gaming jargon, and foreign phrases are spoken by Eloqium.

Unlike fixed linguistic engines, the User Dictionary operates dynamically across user-defined rules and supports multi-lingual, dialect-scoped dictionary management with zero audio path latency.

---

## 2. Conceptual Model: Text vs. Pronunciation Entries

The User Dictionary distinguishes clearly between two entry types:

### 2.1 Text Entries (`TEXT`)
- **Behavior**: Standard orthographic replacement. Matching source text is substituted with replacement text before linguistic analysis and abbreviation expansion.
- **Example**: `NASA` → `N A S A`, `approx.` → `approximately`, `BRB` → `be right back`.
- **Use Case**: Expanding acronyms, correcting colloquial contractions, replacing text abbreviations, and substituting words with phonetic respellings using standard alphabet characters.

### 2.2 Pronunciation Entries (`PRONUNCIATION`)
- **Behavior**: Direct phonemic substitution utilizing OpenEVV's native Standard Phonemic Representation (SPR).
- **Phoneme Enclosure**: Phonetic phonemes are entered and stored enclosed in square brackets (e.g., `[eh1l o0 k w iy2 ax m]`).
- **User Experience**: Users enter phonemes directly in the user-facing replacement field. There is no requirement for the user to type a leading backquote (`` ` ``) or trailing dot (``.``); Eloqium automatically normalizes and wraps entries cleanly.
- **Engine Injection & Pipeline Protection**: At synthesis time, Eloqium injects the phoneme stream directly into OpenEVV's phonetic parser in backquote phonetic mode (`` `[...] ``). The speech pipeline treats phonetic SPR tokens as atomic protected spans:
  - **Punctuation Protection**: Screen-reader punctuation verbalization leaves internal phonetic brackets and symbols untouched.
  - **Chunking Protection**: Text chunking keeps phonetic tokens intact within single sentence boundaries.
  - **Normalization Protection**: Number grouping and abbreviation expansion bypass phoneme contents.
- **Example**: `Eloqium` → `[eh1l o0 k w iy2 ax m]`.
- **Use Case**: Precise pronunciation override for irregular proper nouns, technical terms, and multilingual borrowed words where orthographic respelling does not achieve the exact acoustic result.

---

## 3. Hierarchy and Data Architecture

The User Dictionary data model follows a strict 4-level hierarchy:

```
User Dictionary System
  └── Languages (e.g., en-US, en-GB, es-ES, es-MX, fr-FR, fr-CA, de-DE, it-IT)
        └── Dictionaries (e.g., "User Dictionary", "Medical Terms", "Acronyms")
              └── Entries (Source, Replacement, Match Mode, Case Sensitivity, Type)
```

- **Strict Language & Locale Isolation**: Dictionaries belong strictly to a specific regional language variant (e.g., `en-US`, `en-GB`, `es-ES`, `es-MX`, `fr-FR`, `fr-CA`, `de-DE`, `it-IT`). Dialect scoping is isolated: rules defined for `en-US` only apply when American English is synthesizing and never leak into British English (`en-GB`), Spanish, or unconfigured locales. Locale tags with underscores (`en_US`) are automatically normalized to standard BCP-47 hyphenated format (`en-US`).
- **Language Lifecycle**: A language appears in the Dictionary Manager if and only if it contains at least one dictionary. When the user taps **Add Language**, selecting a language automatically creates a default enabled dictionary named **User Dictionary**. If all dictionaries under a language are deleted, the language is automatically removed from the active list and returns to the available languages list.
- **Multiple Dictionaries per Language**: Users can create, enable, disable, rename, export, and delete multiple independent dictionaries within each language.
- **Dictionary Provenance**: Dictionaries preserve origin metadata, including original imported filename, source language, dictionary layer (`user`, `root`, `main`, `abbr`), and import timestamp.

---

## 4. Match Modes & Case Sensitivity

When creating or editing an entry, users configure matching behavior:

| Match Mode | Description | Word Boundary Behavior | Example Source | Input Text | Result |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Exact match** | Source matches target word or phrase exactly according to case sensitivity. | Enforces Unicode word boundaries `(?<![\p{L}\p{N}_])` and `(?![\\p{L}\\p{N}_])` for alphanumeric tokens. | `NASA` → `N A S A` | `"NASA launch"` | `"N A S A launch"` (does not match `"NASAL"`) |
| **Starts with** | Target word begins with source. | Enforces left word boundary; matches prefix. | `micro` → `small ` | `"microscope"` | `"small scope"` |
| **Ends with** | Target word ends with source. | Enforces right word boundary; matches suffix. | `burgh` → `burg` | `"Pittsburgh"` | `"Pittsburg"` |
| **Contains** | Target contains source anywhere. | Substring match without boundary constraints. | `:=` → `assigned to` | `"x := 5"` | `"x assigned to 5"` |

### Case Sensitivity
- **Case-Sensitive Checked**: Matches source text with exact character casing.
- **Case-Sensitive Unchecked**: Case-insensitive matching powered by Unicode case-folding (`Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE`), ensuring accurate matching across international scripts and accents (e.g., `café` / `CAFÉ`).

---

## 5. Precedence and Pipeline Semantics

### Deterministic Multi-Rule Precedence
When multiple enabled dictionaries and entries participate in an utterance, rules are sorted deterministically:
1. **Source Length**: Longer source phrases match first (e.g., `"New York City"` matches before `"New York"` or `"New"`).
2. **Match Mode Specificity**: `Exact match` (Rank 1) > `Starts with` / `Ends with` (Rank 2) > `Contains` (Rank 3).
3. **Case Sensitivity**: Case-sensitive rules take priority over case-insensitive rules of the same length.
4. **Stable Dictionary Order**: Entries preserve the order of dictionary creation.

### Single-Pass Non-Cascading Replacement
Replacement execution utilizes single-pass interval mapping. Replacements are placed directly during string reconstruction, guaranteeing that replacement text or phoneme markers are never recursively re-scanned or corrupted by subsequent rules in the same utterance.

### Precedence Over Built-In Processors
User dictionary replacement executes **before** the built-in `AbbreviationProcessor`. If a user explicitly defines a replacement for `"Dr."` or `"St."`, the user's custom rule takes precedence.

### Zero Audio Path Latency
Active dictionary rules are compiled and cached in-memory. Synthesizing an utterance performs zero disk I/O and zero JSON parsing, preserving Eloqium's ultra-low latency.

---

## 6. Two-Level Search and Filtering

To manage large dictionaries and extensive word lists, Eloqium provides accessible, real-time search across two distinct levels:

### 6.1 Dictionary-Level Filter
- Located in the **Dictionaries** screen for any selected language.
- Real-time search across dictionary names, original imported filenames, source languages, and layers.
- Displays live result counts (e.g., `Search Results (2 of 5)`).
- In-memory filtering: Evaluates dynamically against the active dictionary collection without disk I/O or reserialization on keystrokes.
- Provides an accessible clear button and an empty-state action to reset the filter.

### 6.2 Entry-Level Filter
- Located inside any open dictionary in the **Words and Replacements** screen.
- Real-time filtering across source words, replacements, entry types (`text` or `pronunciation`), and match modes.
- Instant, zero-latency in-memory filtering that updates dynamically as the user types without disk I/O.
- Fully accessible to TalkBack with clear button and accessible empty state.

### 6.3 Accessibility Design
- Search input fields rely exclusively on visible text labels (`label = { Text("Search dictionaries") }` and `label = { Text("Search words") }`). Redundant `contentDescription` attributes are removed from the input fields, ensuring screen readers like TalkBack announce the search label exactly once.
- Trailing clear-search buttons retain explicit content descriptions (`contentDescription = "Clear search"`).

---

## 7. JSON Export and Import (Schema Version 2)

### Schema Specification
Exported and imported dictionaries adhere to a human-readable, deterministic JSON schema:

```json
{
  "schemaVersion": 2,
  "language": "en-US",
  "name": "Acronyms",
  "enabled": true,
  "provenance": {
    "originalFilename": "ENU_USER.DIC",
    "sourceLanguage": "ENU",
    "dictionaryLayer": "user",
    "importedAt": "2026-09-14T12:00:00Z"
  },
  "entries": [
    {
      "source": "Eloqium",
      "replacement": "[eh1l o0 k w iy2 ax m]",
      "matchMode": "Exact match",
      "caseSensitive": false,
      "type": "pronunciation"
    },
    {
      "source": "NASA",
      "replacement": "N A S A",
      "matchMode": "Exact match",
      "caseSensitive": true,
      "type": "text"
    }
  ]
}
```

### Schema Migration & Backward Compatibility
- **Schema v1 Compatibility**: Older Schema v1 JSON files (which lacked `type` and `provenance`) are automatically migrated upon import. Missing `type` fields default safely to `text`.
- **Automatic Re-Export Upgrade**: Re-exporting any dictionary automatically outputs standard Schema v2.

---

## 8. IBM Eloquence `.dic` Importer

Eloqium provides native, high-performance importing for legacy IBM Eloquence `.dic` dictionaries as an external compatibility and migration format:

### 8.1 External Compatibility Format
- IBM `.dic` files are treated as external compatibility files, not internal persistence files. When imported, entries are parsed, classified, and saved into native Schema Version 2 JSON format.

### 8.2 Supported Layers
- Automatically detects dictionary layer type from filename:
  - `Root`: Base lexicon definitions.
  - `Main`: Primary application words.
  - `Abbreviation` (`abbr`): Abbreviation expansions.
  - `User`: Custom user dictionaries.

### 8.3 Character Encoding Detection
- Automatically attempts UTF-8 parsing with automatic fallback to Windows-1252 (ANSI) / ISO-8859-1 when replacement characters or high-byte characters are encountered.

### 8.4 Intelligent Proposal & Target Locale Selection
- **Filename as Proposal**: The filename prefix (e.g., `ENU*`, `ENG*`, `ESP*`, `FRA*`, `DEU*`, `ITA*`) is treated strictly as an initial detection hint/proposal, not as authoritative proof of the dictionary's actual language. Renaming an English file to `ESP` does not make its contents Spanish.
- **Destination Language Selector**: When importing via Android's file manager or the in-app file picker, the user is presented with an accessible destination language selector listing all 8 supported Eloqium languages (`en-US`, `en-GB`, `es-ES`, `es-MX`, `fr-FR`, `fr-CA`, `de-DE`, `it-IT`).
- **Incompatibility Warning**: If the selected destination language is phonetically incompatible with the detected source language (for example, importing an English `ENU` dictionary into `Spanish (Spain)`), an explicit warning banner alerts the user prior to confirmation:
  > ⚠️ Target language (Spanish (Spain)) does not match detected dictionary language (ENU). Phonetic pronunciation tags may sound incorrect in this language.
- **Storage**: The chosen destination locale is the actual locale used for dictionary persistence.

### 8.5 Phonemic SPR Extraction & Normalization
- Automatically classifies lines containing phoneme brackets (e.g., `word \t `[`f ah n eh t ih k`].`) as `PRONUNCIATION` entries.
- Strips redundant trailing dots and leading/trailing backticks while preserving internal syllable and stress markers.
- Classifies non-phonetic entries as standard `TEXT` replacements.

---

## 9. ECI Voice Tags & Pipeline Security

### 9.1 Architectural Separation
- **User Dictionary** and **ECI Voice Tags** are strictly separated:
  - The User Dictionary controls text substitutions and phonemic pronunciation overrides (`[...]`).
  - The ECI Voice Tags setting controls interpretation of inline ECI parameter annotations in synthesized text.
  - The User Dictionary's pronunciation tags (`[...]`) are protected at all times and never depend on whether ECI Voice Tags is enabled or disabled.

### 9.2 The "ECI Voice Tags" Setting
Located under **Settings → Voice**:
- **Enabled (Default)**: Preserves supported ECI parameter tags (`vv`, `vb`, `vs`, `vf`, `vh`, `vr`, `vy`, `p`, `0`-`4`, `pp0`, `pp1`) and prevents premature splitting by text-normalization rules. Sanitizes unapproved rogue backticks to `'`.
- **Disabled**: Automatically sanitizes user backticks outside phonetic tags to apostrophes (`'`), ensuring that arbitrary user text cannot accidentally trigger unexpected synthesizer state changes.

### 9.3 Engine Voice Preset In-Text Behavior
- In OpenEVV's formant synthesis mode, inline voice preset tags (`v1`-`v8`) do not swap acoustic voice profiles because formant mode lacks concatenative voice tables (`et_present == 0`). Voice preset switching occurs at the profile configuration layer (`copyVoice()`), whereas individual parameter tags (`vv`, `vb`, `vs`, etc.) directly invoke `et_setVoiceParam()` and work inline as expected.

---

## 10. Accessibility & TalkBack Design

The User Dictionary subsystem adheres strictly to Android accessibility best practices:
- **Heading Semantics**: Every screen and section top bar is marked with `Modifier.semantics { heading() }`.
- **Single-Announcement Search**: Search text fields use visible labels without redundant accessibility labels, eliminating duplicated TalkBack announcements.
- **Descriptive Announcements**: List items announce status, entry counts, provenance, and available gestures (e.g., `"User Dictionary, Enabled, 42 entries, ENU_USER.DIC. Double tap to open, long press for options."`).
- **Accessible Touch Targets**: All interactive controls provide minimum touch bounds of 48dp.
- **Accessible Dialogs**: Confirmation dialogs for adding, editing, renaming, importing, and deleting dictionaries provide clear labeling, focus retention, and explicit dismiss buttons.
