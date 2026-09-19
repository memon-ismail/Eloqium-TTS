# Eloqium TTS — Emoji & Emoticon Speech Pipeline

## 1. Overview

Eloqium TTS provides offline, multilingual speech vocalization for emojis and ASCII emoticons across all 5 supported language families:
- **English** (`en-US`, `en-GB`)
- **Spanish** (`es-ES`, `es-MX`)
- **French** (`fr-FR`, `fr-CA`)
- **German** (`de-DE`)
- **Italian** (`it-IT`)

The pipeline transforms Unicode sequences and text emoticons into localized spoken descriptions during text preprocessing, ensuring smooth pronunciation before text reaches the OpenEVV native synthesis core.

---

## 2. Emoji Processing

- **Dataset**: Built from Unicode 16.0 specifications and localized using Unicode CLDR 48 annotations across the 5 language families, covering 3,805 unique emoji sequences.
- **Trie Matching**: Matches sequences in-memory using greedy longest-match-first lookup. Multi-codepoint sequences (such as skin-tone modifiers, zero-width joiner sequences, and flags) resolve to their single composite description rather than fragmented parts.
- **Spacing & Punctuation Preservation**:
  - Consecutive emojis are separated by commas (e.g. `😀 👍` → `grinning face, thumbs up`) to provide natural pause cadences.
  - Word boundaries are preserved with surrounding spaces when emojis abut alphanumeric text (`Hello😀world` → `Hello grinning face world`).
  - Sentence-ending punctuation attached to emojis remains intact to maintain clause intonation.

---

## 3. ASCII Emoticons & Protected Spans

Eloqium recognizes a conservative set of common text emoticons:
- Smiles and grins: `:)`, `:-)`, `:D`, `:-D`
- Winks: `;)` , `;-)`
- Frowns and cries: `:(`, `:-(`, `:'(`, `:'-(`
- Playful / skeptical: `:P`, `:-P`, `:/`, `:-\`, `:O`, `:-O`
- Hearts and expressions: `<3`, `</3`, `B)`, `-_-`

### Protected Spans
To prevent corrupting non-conversational text, emoticon matching is bypassed within protected contexts:
- **URLs and web addresses**: `https://...`, `ftp://...`, `www....`
- **Time expressions**: `12:30`, `2:45:00`
- **Aspect ratios & drive paths**: `16:9`, `C:\Windows`
- **Programming & math syntax**: `std::vector`, `:=`, `->`, `=>`, `<3` when surrounded by math operators or comparison bounds.

---

## 4. Pipeline Position & Limitations

- **Execution Order**: Emoji and emoticon processing executes during early text preprocessing, immediately after User Dictionary and abbreviation expansion, and **before** character normalization. Running before normalization ensures that combining modifiers, zero-width joiners, and variation selectors are not stripped before they can be matched.
- **Acoustic Vocalization**: The processors perform text replacement; the resulting descriptive text is synthesized acoustically by the OpenEVV native formant engine.
- **Toggle Setting**: Emoji and emoticon vocalization can be toggled via the **Emoji / Emoticon** switch under **Settings → Text Processing**.
