# Eloqium TTS — User Dictionary Specification

## 1. Overview

The User Dictionary subsystem in Eloqium TTS allows users to customize speech output by defining custom text replacements and phonetic pronunciation overrides. Dictionaries are language-scoped and evaluated during early text preprocessing before abbreviations and general linguistic analysis.

---

## 2. Entry Types

Eloqium supports two distinct dictionary entry types:

1. **Text Entries (`TEXT`)**:
   - Standard orthographic substitution (e.g. `NASA` → `N A S A`, `BRB` → `be right back`).
   - Replaces matching source words or phrases with alternative text prior to abbreviation expansion.
2. **Pronunciation Entries (`PRONUNCIATION`)**:
   - Direct phonemic overrides using OpenEVV's Standard Phonemic Representation (SPR).
   - Phonemes are entered and stored enclosed in square brackets (e.g. `Eloqium` → `[eh1l o0 k w iy2 ax m]`).
   - Treated as protected spans throughout the synthesis pipeline: phonetic brackets and stress markers are guarded against normalization, number processing, and punctuation verbalization.

---

## 3. Matching Modes & Case Sensitivity

Each entry specifies how source text is detected:

| Match Mode | Description | Example | Result |
| :--- | :--- | :--- | :--- |
| **Exact match** | Matches the complete word or token (applying word boundaries to alphanumeric words; symbol sequences match directly). | `NASA` → `N A S A` | Matches `"NASA launch"`, ignores `"NASAL"`. |
| **Starts with** | Matches words beginning with the source prefix. | `micro` → `small ` | Matches `"microscope"` → `"small scope"`. |
| **Ends with** | Matches words ending with the source suffix. | `burgh` → `burg` | Matches `"Pittsburgh"` → `"Pittsburg"`. |
| **Contains** | Matches source text anywhere as a substring. | `:=` → `assigned to` | Matches `"x := 5"` → `"x assigned to 5"`. |

- **Case Sensitivity**: Entries can be toggled as case-sensitive or case-insensitive. Case-sensitive rules take priority over case-insensitive rules when source lengths are equal.

---

## 4. Precedence & Pipeline Semantics

When multiple rules match within an utterance, substitutions follow a deterministic 4-tier precedence:
1. **Source Length**: Longer source phrases match first (`"New York City"` before `"New York"`).
2. **Match Mode Specificity**: `Exact match` > `Starts with` / `Ends with` > `Contains`.
3. **Case Sensitivity**: Case-sensitive entries take priority over case-insensitive entries.
4. **Stable Dictionary Order**: Preserves the order in which entries were created.

- **Non-Cascading Replacement**: Executed via single-pass interval replacement. Replacement text or phoneme tokens are never re-scanned or modified by subsequent rules in the same utterance.
- **Precedence over Abbreviations**: User dictionary rules execute before the built-in abbreviation expander, allowing users to override default expansions (such as `"Dr."` or `"St."`).

---

## 5. Language Scoping & Isolation

- **Locale Isolation**: Dictionaries belong strictly to specific regional dialects (`en-US`, `en-GB`, `es-ES`, `es-MX`, `fr-FR`, `fr-CA`, `de-DE`, `it-IT`). Rules defined for `en-US` only execute during US English synthesis and never leak into other dialects.
- **Multiple Dictionaries**: Users can create, enable, disable, rename, and organize multiple dictionaries per language (e.g. "Medical Terms", "Acronyms").

---

## 6. Import and Export

### JSON Schema Version 2
Dictionaries export and import as structured JSON containing schema version (`2`), language tag, dictionary name, enabled status, provenance, and entry arrays. Schema v1 files are automatically migrated upon import.

### IBM Eloquence `.dic` Import
Eloqium imports legacy IBM `.dic` dictionary files as an external compatibility and migration format:
- **Encoding Fallback**: Automatically tries UTF-8 first; falls back to Windows-1252 if replacement characters occur, and ISO-8859-1 if decoding fails.
- **Layer & Language Proposal**: Filename prefixes (e.g. `ENU*`, `DEU*`) and keywords (`root`, `main`, `abbr`) propose destination languages and layers, with user confirmation.
- **Entry Classification**: Lines with phonemic brackets (`[...]`) import as `PRONUNCIATION` entries; plain text pairs import as `TEXT` entries with `Exact match` and case-sensitive defaults.
- **Dialect Warnings**: Warns if importing a dictionary into a phonetically incompatible dialect (e.g. English into Spanish).

---

## 7. ECI Interaction & Limitations

- **Phonetic SPR Protection**: Pronunciation tags (`[...]`) are protected at all times and remain intact regardless of whether the **ECI Voice Tags** setting is enabled or disabled.
- **Formant Voice Switching**: Inline voice preset tags do not alter vocal tract models during ongoing formant synthesis; voice preset selection applies per utterance.
- **Storage Format**: IBM `.dic` files are an import format; internal dictionary persistence uses native JSON.
