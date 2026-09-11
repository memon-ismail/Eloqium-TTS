# Third-Party Licenses and Legal Notices

Eloqium TTS incorporates or links against several open-source libraries and projects.
Below are the complete licenses and copyright notices for each component.

---

## 1. EVVDroid

- **Origin**: https://github.com/trypsynth/evvdroid
- **License**: Apache License, Version 2.0
- **Copyright**: Copyright (c) 2024-2025 trypsynth

Portions of the native JNI worker thread architecture, ring-buffer synchronization,
and audio pacing pipeline are derived from EVVDroid under the Apache-2.0 License.

```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 2. OpenEVV (`libevv`)

- **Origin**: https://github.com/Mudb0y/openevv
- **License**: MIT License + IBM Language Rules Notice
- **Copyright**: Copyright (c) 2023-2025 Mudb0y and OpenEVV contributors

```
MIT License

Copyright (c) 2023 Mudb0y

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### IBM Speech Synthesis Language Rules Notice
Portions of the linguistic rule tables and phoneme data within `lang/` and `rom/`
originate from IBM Speech Synthesis technologies and are distributed under the terms
provided with the OpenEVV project repository.

---

## 3. NVDA IBMTTS Driver (Reference Only — No GPL Code Included)

- **Origin**: https://github.com/davidacm/NVDA-IBMTTS-Driver
- **License**: GNU General Public License v2 (GPL-2.0)
- **Notice**: The NVDA IBMTTS Driver was consulted strictly as a functional, behavioral,
  and documentation reference for ECI command sequences, punctuation verbosity levels,
  and parameter scaling. **No source code from NVDA IBMTTS Driver is incorporated or
  copied into Eloqium TTS**, preserving strict Apache-2.0 clean-room compliance.

---

## 4. Android Open Source Project (AOSP)

- **Origin**: https://android.googlesource.com/platform/frameworks/base/
- **License**: Apache License, Version 2.0
- **Copyright**: Copyright (C) 2011-2024 The Android Open Source Project

---

## 5. Unicode Consortium Data Files (Unicode 16.0 & CLDR 48)

- **Origin**: https://unicode.org/Public/emoji/16.0/ & https://github.com/unicode-org/cldr-json
- **License**: Unicode License Agreement (Unicode-3.0 / Unicode Terms of Use)
- **Copyright**: Copyright (c) 1991-2024 Unicode, Inc. All rights reserved.

Emoji definitions, standardized sequences, and multilingual spoken annotations for
English, Spanish, French, German, and Italian incorporated in `app/src/main/assets/emoji_data.bin`
are generated from Unicode 16.0 (`emoji-test.txt`) and Unicode CLDR release 48 (`cldr-annotations-full`).

```
UNICODE, INC. LICENSE AGREEMENT - DATA FILES AND SOFTWARE

See Terms of Use for definitions of Unicode Standard, Data Files, and Software.
NOTICE TO USER: Carefully read the following legal agreement. BY DOWNLOADING,
INSTALLING, COPYING OR OTHERWISE USING UNICODE INC.'S DATA FILES ("DATA FILES"),
AND/OR SOFTWARE ("SOFTWARE"), YOU UNEQUIVOCALLY ACCEPT, AND AGREE TO BE BOUND BY,
ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. IF YOU DO NOT AGREE, DO NOT
DOWNLOAD, INSTALL, COPY, DISTRIBUTE OR USE THE DATA FILES OR SOFTWARE.

COPYRIGHT AND PERMISSION NOTICE
Copyright (c) 1991-2024 Unicode, Inc. All rights reserved.
Distributed under the Terms of Use in https://www.unicode.org/copyright.html.

Permission is hereby granted, free of charge, to any person obtaining a copy
of the Unicode data files and any associated documentation (the "Data Files")
or Unicode software and any associated documentation (the "Software") to deal
in the Data Files or Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, and/or sell
copies of the Data Files or Software, and to permit persons to whom the Data
Files or Software are furnished to do so, provided that either
(a) this copyright and permission notice appear with all copies of the Data
Files or Software, or
(b) this copyright and permission notice appear in associated Documentation.
```
