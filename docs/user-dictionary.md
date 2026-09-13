# Eloqium TTS — User Dictionary System (v0.1.2)

## 1. Overview

Eloqium TTS v0.1.2 introduces the **User Dictionary** subsystem. The User Dictionary is a user-controlled, pronunciation and text replacement engine that transforms matching source text into user-defined replacement text prior to speech synthesis.

It allows screen-reader users and everyday listeners to customize how words, phrases, abbreviations, specialized acronyms, technical jargon, proper names, and gamer terminology are pronounced by Eloqium.

Unlike a fixed linguistic dictionary, the User Dictionary operates dynamically across user-defined rules and supports multi-lingual, dialect-scoped dictionary management.

---

## 2. Hierarchy and Data Architecture

The User Dictionary data model follows a strict 4-level hierarchy:

```
User Dictionary System
  └── Languages (e.g., en-US, es-ES, fr-FR)
        └── Dictionaries (e.g., "User Dictionary", "Medical Terms", "Acronyms")
              └── Entries (Source → Replacement, Match Mode, Case Sensitivity)
```

- **Language Scoping & Strict Locale Isolation**: Dictionaries strictly belong to a specific language/dialect (e.g. `en-US`, `en-GB`, `es-ES`, `es-MX`, `fr-FR`, `fr-CA`, `de-DE`, `it-IT`). Dialect scoping is strictly isolated: rules defined for `en-US` only apply when American English is synthesizing, and never leak into `en-GB`, Spanish, or base unconfigured languages. Underscore-separated locale tags (`en_US`) are normalized to standard BCP-47 hyphenated format (`en-US`).
- **Language Lifecycle**: A language is considered added if and only if it contains at least one dictionary. When the user taps **Add Language**, selecting a language automatically creates a default enabled dictionary named **User Dictionary** with zero entries. If all dictionaries under a language are deleted, the language is automatically removed from the active list and becomes available in **Add Language** again.
- **Multiple Dictionaries per Language**: Users can create, enable, disable, rename, import, and export multiple dictionaries per language.

---

## 3. Global Master Switch & Enable/Disable State

1. **Global Master Switch (`User Dictionary`)**:
   - Located under the **User Dictionary** section in Settings.
   - When **Off**: No user dictionary entries modify speech across any language. All dictionary configurations and entries remain completely intact.
   - When **On**: Enabled dictionaries for the active synthesis language participate in text replacement.
2. **Individual Dictionary State**:
   - Every individual dictionary can be toggled **Enabled** or **Disabled** via its long-press context menu.
   - Disabled dictionaries retain all entries and metadata but are skipped during synthesis.

---

## 4. Match Modes & Semantics

When creating or editing a word via **Add Word**, users can select from exactly four match modes:

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
When multiple enabled dictionaries and entries participate in an utterance, rules are ordered deterministically:
1. **Source Length**: Longer source phrases match first (e.g., `"New York City"` matches before `"New York"` or `"New"`).
2. **Match Mode Specificity**: `Exact match` (Rank 1) > `Starts with` / `Ends with` (Rank 2) > `Contains` (Rank 3).
3. **Case Sensitivity**: Case-sensitive rules take priority over case-insensitive rules of the same length.
4. **Stable Dictionary Order**: Entries preserve the order of dictionary creation.

### Single-Pass Non-Cascading Replacement
Replacement execution utilizes single-pass interval mapping. Replacements are placed directly during string reconstruction, guaranteeing that replacement text is never recursively re-scanned or corrupted by subsequent rules in the same utterance.

### Precedence Over Built-In Abbreviations
User dictionary replacement executes **before** the built-in `AbbreviationProcessor`. If a user explicitly defines a replacement for `"Dr."` or `"St."`, the user's custom replacement takes precedence. If no user entry matches, built-in abbreviations continue to expand normally.

### Zero Audio Path Latency
Active dictionary rules are compiled and cached in-memory. Synthesizing an utterance performs zero disk I/O and zero JSON parsing, preserving Eloqium's ultra-low latency.

---

## 6. JSON Export and Import

### Schema Specification (Version 1)
Exported and imported dictionaries adhere to a human-readable, deterministic JSON schema:

```json
{
  "schemaVersion": 1,
  "language": "en-US",
  "name": "Acronyms",
  "enabled": true,
  "entries": [
    {
      "source": "Eloqium",
      "replacement": "Ee-loh-kee-um",
      "matchMode": "Exact match",
      "caseSensitive": false
    },
    {
      "source": "NASA",
      "replacement": "N A S A",
      "matchMode": "Exact match",
      "caseSensitive": true
    }
  ]
}
```

### Import Handling & Duplicate Resolution
- **In-App Import**: Selecting **Import Dictionary** inside a language imports the JSON file directly under that language using Android's Storage Access Framework (`OpenDocument`).
- **Duplicate Name Safety**: If a dictionary with the same name already exists in the language, the imported dictionary is automatically renamed with an incrementing suffix (e.g., `"Acronyms (1)"`), avoiding overwrites.
- **Validation**: All JSON inputs are validated. Corrupt JSON, unsupported `schemaVersion` values, or missing required fields yield accessible user-facing dialogs without crashes.

---

## 7. System "Open With" Integration (`Eloqium Dictionary Importer`)

Eloqium registers specific Android Intent Filters for `ACTION_VIEW` and `ACTION_SEND` targeting `.json` files across both `content://` and `file://` schemes:
- **System Chooser**: When a user selects or shares a `.json` dictionary from any Android file manager (Files by Google, Samsung My Files, Total Commander, etc.), **Eloqium Dictionary Importer** (`DictionaryImportActivity`) appears in the system intent chooser.
- **Safe Pre-Validation**: The importer inspects the URI (resolving `intent.data`, `EXTRA_STREAM`, or `ClipData`), reads the JSON stream via `ContentResolver`, and validates the schema without making any premature changes to persistent storage.
- **Explicit Confirmation Dialog**: Before importing, the user is presented with an accessible confirmation dialog titled **"Import dictionary?"** displaying:
  - Dictionary name
  - Language display name
  - Total entry count
  - **Import** (confirm) and **Cancel** (dismiss) actions.
- **Transactional Import**: Only when the user explicitly taps **Import** does the repository write the dictionary to storage. If a dictionary with that name already exists in the language, the importer automatically resolves conflicts by appending an incrementing suffix (e.g., `"User Dictionary (1)"`).
- **Completion Dialog**: Upon successful import, a completion dialog appears titled **"Dictionary imported"** with actions to **Open Settings** (navigating directly to Eloqium Settings) or finish (**Done**).
- **Error Handling**: If the JSON is malformed or cannot be opened, an accessible **"Import error"** dialog details the issue without application crashes.
- **Security & Storage**: Zero raw filesystem path assumptions or dangerous storage permissions required. Uses Android content URI resolution throughout.

---

## 8. Accessibility & TalkBack Design

The User Dictionary UI is built accessibility-first:
- **Heading Semantics**: Screen and section headers are marked with `semantics { heading() }`.
- **Descriptive Announcements**: List items announce the name, status, entry count, and available gestures (e.g. `"User Dictionary, Enabled, 5 entries. Long press for options."`).
- **Accessible Grouping**: Radio button choices for match modes and checkboxes for case sensitivity use `mergeDescendants = true` with standard Android `Role.RadioButton` and `Role.Checkbox`.
- **Contrast & Focus**: Text fields provide explicit labels and focus listeners. All destructive actions (deleting dictionaries, clearing languages) require explicit confirmation dialogs.
