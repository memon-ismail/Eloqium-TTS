#!/usr/bin/env python3
"""
Eloqium TTS - Unicode & CLDR Emoji Data Generator
=================================================
Source data:
- Unicode Emoji Specification: UTS #51 (Version 16.0)
  https://unicode.org/Public/emoji/16.0/emoji-test.txt
- Unicode CLDR (Common Locale Data Repository) Annotations (Version 48)
  https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json/

Licensing & Provenance:
- Unicode Data Files and Software are distributed under the Unicode License Agreement
  (Unicode-3.0 / Unicode Terms of Use).
  Copyright © 1991-2024 Unicode, Inc. All rights reserved.
- This generator produces a compact, offline binary asset `emoji_data.bin` containing
  only the official TTS spoken names for the 5 languages supported by Eloqium TTS:
  English (en), Spanish (es), French (fr), German (de), Italian (it).
"""

import urllib.request
import json
import struct
import os
import sys

UNICODE_VERSION = (16, 0)
CLDR_VERSION = (48, 0)
FORMAT_VERSION = 1
LANGUAGES = ["en", "es", "fr", "de", "it"]

EMOJI_TEST_URL = f"https://unicode.org/Public/emoji/{UNICODE_VERSION[0]}.{UNICODE_VERSION[1]}/emoji-test.txt"
CLDR_ANN_URL = "https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json/cldr-annotations-full/annotations/{}/annotations.json"
CLDR_DER_URL = "https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json/cldr-annotations-derived-full/annotationsDerived/{}/annotations.json"

def fetch_url(url):
    print(f"  Fetching: {url}")
    req = urllib.request.Request(url, headers={"User-Agent": "Eloqium-Build-Tool/1.0"})
    with urllib.request.urlopen(req) as resp:
        return resp.read().decode("utf-8")

def parse_valid_emojis():
    print(f"Downloading Unicode {UNICODE_VERSION[0]}.{UNICODE_VERSION[1]} emoji-test.txt...")
    content = fetch_url(EMOJI_TEST_URL)
    valid_set = set()
    for line in content.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split(";")
        if len(parts) >= 2:
            hex_codes = parts[0].strip().split()
            seq = "".join(chr(int(h, 16)) for h in hex_codes)
            valid_set.add(seq)
            # Also add form without U+FE0F
            if "\ufe0f" in seq:
                valid_set.add(seq.replace("\ufe0f", ""))
    print(f"  Found {len(valid_set)} valid Unicode emoji sequences (including un/minimally-qualified).")
    return valid_set

def fetch_cldr_annotations(valid_emojis):
    raw_data = {}
    for lang in LANGUAGES:
        print(f"Fetching CLDR {CLDR_VERSION[0]} annotations for '{lang}'...")
        r1 = json.loads(fetch_url(CLDR_ANN_URL.format(lang)))
        r2 = json.loads(fetch_url(CLDR_DER_URL.format(lang)))
        ann1 = r1.get("annotations", {}).get("annotations", {})
        ann2 = r2.get("annotationsDerived", {}).get("annotations", {})
        combined = {**ann1, **ann2}
        
        tts_map = {}
        for k, v in combined.items():
            if "tts" in v and v["tts"]:
                tts = v["tts"][0].strip()
                if tts:
                    # STRICT FILTER: only accept if key is recognized in valid_emojis
                    # or variant of valid_emojis (e.g. without FE0F)
                    if k in valid_emojis or k.replace("\ufe0f", "") in valid_emojis:
                        tts_map[k] = tts
        raw_data[lang] = tts_map
        print(f"  '{lang}': {len(tts_map)} valid emoji TTS entries.")
    return raw_data

def build_binary(valid_emojis, raw_data, output_path):
    print("Consolidating emoji sequences across all languages...")
    unified_keys = set()
    for lang in LANGUAGES:
        for k in raw_data[lang].keys():
            unified_keys.add(k)
            # Add variant with/without U+FE0F
            if "\ufe0f" in k:
                unified_keys.add(k.replace("\ufe0f", ""))
            # Keycap with U+FE0F
            if "\u20e3" in k and "\ufe0f\u20e3" not in k:
                unified_keys.add(k.replace("\u20e3", "\ufe0f\u20e3"))
            if k == "\u2764":
                unified_keys.add("\u2764\ufe0f")
            if k == "\u2764\u200d\U0001f525":
                unified_keys.add("\u2764\ufe0f\u200d\U0001f525")
            if k == "\u2764\u200d\U0001fa79":
                unified_keys.add("\u2764\ufe0f\u200d\U0001fa79")

    # Order longest first (for greedy Trie matching), then lexicographical
    sorted_keys = sorted(list(unified_keys), key=lambda x: (-len(x), x))
    print(f"Total compiled unique emoji sequences: {len(sorted_keys)}")

    def resolve_tts(lang, key):
        val = raw_data[lang].get(key)
        if val: return val
        no_fe0f = key.replace("\ufe0f", "")
        val = raw_data[lang].get(no_fe0f)
        if val: return val
        if "\ufe0f\u20e3" in key:
            val = raw_data[lang].get(key.replace("\ufe0f\u20e3", "\u20e3"))
            if val: return val
        if lang != "en":
            return resolve_tts("en", key)
        return ""

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "wb") as f:
        # Magic: 'ELQE'
        f.write(b"ELQE")
        # Format version (uint16)
        f.write(struct.pack(">H", FORMAT_VERSION))
        # Unicode version (uint8 major, uint8 minor)
        f.write(struct.pack(">BB", UNICODE_VERSION[0], UNICODE_VERSION[1]))
        # CLDR version (uint8 major, uint8 minor)
        f.write(struct.pack(">BB", CLDR_VERSION[0], CLDR_VERSION[1]))
        # Language count (uint16)
        f.write(struct.pack(">H", len(LANGUAGES)))
        for lang in LANGUAGES:
            f.write(lang.encode("ascii")[:2])
        # Emoji count (uint32)
        f.write(struct.pack(">I", len(sorted_keys)))

        # Keys
        for k in sorted_keys:
            kb = k.encode("utf-8")
            f.write(struct.pack(">H", len(kb)))
            f.write(kb)

        # Per-language TTS strings
        for lang in LANGUAGES:
            pool = bytearray()
            offsets = []
            string_to_offset = {}
            for k in sorted_keys:
                tts = resolve_tts(lang, k)
                if not tts:
                    offsets.append(0xFFFFFFFF)
                else:
                    if tts in string_to_offset:
                        offsets.append(string_to_offset[tts])
                    else:
                        off = len(pool)
                        string_to_offset[tts] = off
                        offsets.append(off)
                        tb = tts.encode("utf-8")
                        pool.extend(struct.pack(">H", len(tb)))
                        pool.extend(tb)
            f.write(struct.pack(">I", len(pool)))
            for off in offsets:
                f.write(struct.pack(">I", off))
            f.write(pool)

    size = os.path.getsize(output_path)
    print(f"\nSUCCESS! Generated {output_path} ({size} bytes, {size/1024:.1f} KB)")
    print(f"Unicode: {UNICODE_VERSION[0]}.{UNICODE_VERSION[1]} | CLDR: {CLDR_VERSION[0]}.{CLDR_VERSION[1]} | Languages: {', '.join(LANGUAGES)}")

def main():
    dest = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/emoji_data.bin"
    valid_emojis = parse_valid_emojis()
    raw_data = fetch_cldr_annotations(valid_emojis)
    build_binary(valid_emojis, raw_data, dest)

if __name__ == "__main__":
    main()
