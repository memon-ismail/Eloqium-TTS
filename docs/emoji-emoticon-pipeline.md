# Eloqium TTS - Emoji & Emoticon Speech Pipeline Specification

## 1. Executive Summary

Eloqium TTS provides fully offline, multilingual speech vocalization for Unicode 16.0 emojis and conservative ASCII emoticons across all 5 supported language families:
- English (`en` / `en-US`, `en-GB`)
- Spanish (`es` / `es-ES`, `es-US`)
- French (`fr` / `fr-FR`, `fr-CA`)
- German (`de` / `de-DE`)
- Italian (`it` / `it-IT`)

The emoji and emoticon processing pipeline converts Unicode sequences and emoticons into natural spoken phrases before text reaches the OpenEVV legacy synthesis layer, ensuring that Eloquence produces rich, fluid, and contextually accurate speech without native engine modification or illegal code-point errors.

---

## 2. Pipeline Architecture & Execution Ordering

The execution order within `org.eloqium.tts.service.EloqiumTtsService` is strictly ordered:

```
[SynthesisRequest.charSequenceText]
             │
             ▼
    [EmojiProcessor]         <-- UTF-16 Trie greedy longest-match lookup from assets/emoji_data.bin
             │
             ▼
   [EmoticonProcessor]       <-- Conservative ASCII emoticons with protected spans (URLs, code, math, times)
             │
             ▼
   [UnicodeNormalizer]       <-- Normalizes typographic quotes, dashes, symbols to Latin-1
             │
             ▼
 [ScreenReaderPunctuation]   <-- Handles verbalization / punctuation re-binding
             │
             ▼
[OpenEVVCompatibilityFixes]  <-- Separates digit/letter boundaries, sanitizes rogue backticks
             │
             ▼
     [PauseProcessor]        <-- Phrase prediction (`pp1), question pitch inflection, pause rules
             │
             ▼
        [Chunker]            <-- Splits large paragraphs at natural clause boundaries
             │
             ▼
     [OpenEVVEncoder]        <-- Encodes clean Latin-1 bytes for libevv
             │
             ▼
    [NativeEngine.speak]     <-- Native OpenEVV synthesis to 16-bit PCM streaming audio
```

### Why Emoji & Emoticon Processors Run Before `UnicodeNormalizer`:
`UnicodeNormalizer` strips non-Latin-1 code points (replacing them with whitespace) and removes zero-width joiners (`\u200D`) and variation selectors (`\uFE0F`). If normalization ran prior to emoji handling, compound emojis (`👩‍💻`, `❤️‍🔥`, `👨‍👩‍👧‍👦`) and modified emojis (`👍🏽`, `❤️`) would have their modifiers stripped or be completely deleted. By processing emoji and emoticons first, modern Unicode symbols are converted into spoken natural language words that pass smoothly through normalization, punctuation handling, and encoding.

---

## 3. Data Structure & Traversal Algorithm

### Compact Binary Asset (`app/src/main/assets/emoji_data.bin`)
- Generated at build time by `tools/generate_emoji_data.py`.
- Size: ~719 KB (702.5 KB).
- Contains 3,805 unique Unicode 16.0 emoji sequences strictly filtered against official Unicode UTS #51 `emoji-test.txt` and annotated from Unicode CLDR 48.
- Filter strictly excludes raw ASCII punctuation (`.`, `,`, `:`, `;`, `?`, `!`, `/`, `\`), preventing false-positive corruption of numbers (e.g. `3.14`), times (`2:30`), or abbreviations (`U.S.`).

### In-Memory UTF-16 Trie (`EmojiData.kt`)
- At runtime, `EmojiData.load()` builds an in-memory `CharTrie` where each node represents a UTF-16 `Char` code unit.
- Terminal nodes hold an array of localized spoken strings indexed by language ID:
  - 0: `en`
  - 1: `es`
  - 2: `fr`
  - 3: `de`
  - 4: `it`
- Lookup algorithm: Greedy longest-match-first starting at current index. Traversal takes $O(L \cdot D)$ time where $D \le 15$ (maximum emoji sequence length in UTF-16), executing in under 0.1 ms per sentence with zero GC overhead.

---

## 4. Spacing, Boundary & Punctuation Rules

1. **Consecutive Emoji Comma-Separation**:
   - Multiple consecutive emojis (e.g. `😂😂😂` or `😀 👍 ❤️`) are separated by `", "`.
   - In Eloquence/OpenEVV, comma-separation generates clean clause boundaries with natural micro-pauses rather than rapid, breathless run-on speech.
2. **Word Boundary Spacing**:
   - If an emoji immediately abuts alphanumeric characters (e.g. `Hello😀world`), spaces are automatically inserted (`Hello grinning face world`).
3. **Sentence Punctuation Preservation**:
   - When an emoji is followed by sentence punctuation (e.g. `Hello 😀!`), the punctuation remains attached (`Hello grinning face!`) so that `ScreenReaderPunctuationProcessor` and `PauseProcessor` apply proper falling/rising question intonation.

---

## 5. ASCII Emoticons & Protected Span Rules (`EmoticonProcessor.kt`)

Supported emoticons:
- Smiling: `:)`, `:-)`
- Frowning: `:(`, `:-(`
- Winking: `;)` , `;-)`, `;--)`
- Grinning: `:D`, `:-D`
- Tongue: `:P`, `:-P`, `:p`, `:-p`
- Skeptical: `:/`, `:-/`, `:\`, `:-\`
- Surprised: `:O`, `:-O`, `:o`, `:-o`
- Crying: `:'(`, `:'-(`
- Cool: `B)`, `B-)`
- Hearts: `<3`, `</3`
- Expressionless: `-_-`

### Strict Protected Spans (Zero False Positives):
To prevent corrupting URLs, times, ratios, drive letters, and code:
- **URLs**: `https?://\S+`, `ftp://\S+`, `www\.\S+`
- **Times**: `\b\d+:\d+(?::\d+)?\b` (e.g. `2:30`, `12:45:00`)
- **Ratios**: `\b\d+:\d+\b` (e.g. `16:9`, `4:3`)
- **Windows Paths**: `\b[A-Za-z]:[\\/]` (e.g. `C:\Windows`)
- **Scope Operators**: `::` (e.g. `std::vector`)
- **Comparisons & Syntax**: `\d+\s*[<>]=?\s*\d+`, `[A-Za-z0-9_]+<\d+`, `[A-Za-z0-9_]+\s+[<>=]=?\s+\d+`, `:=`, `->`, `=>`
- **Token Boundaries**: Requires that the preceding character and following character are not alphanumeric, `@`, or syntax characters.

---

## 6. Verification and Regression Suite

The 14-suite regression test suite in `test/TestRunner.kt` verifies:
- Unicode 16.0 base emojis, modifiers, ZWJ sequences, flags, and keycaps (Suite 12).
- Conservative ASCII emoticons and strict span protections (Suite 13).
- End-to-end PCM audio synthesis across all 5 language families (Suite 14).
