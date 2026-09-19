export const voiceVariants = [
  { language: 'English', region: 'United States', tag: 'en-US' },
  { language: 'English', region: 'United Kingdom', tag: 'en-GB' },
  { language: 'Spanish', region: 'Spain', tag: 'es-ES' },
  { language: 'Spanish', region: 'Mexico / Latin America', tag: 'es-MX' },
  { language: 'French', region: 'France', tag: 'fr-FR' },
  { language: 'French', region: 'Canada', tag: 'fr-CA' },
  { language: 'German', region: 'Germany', tag: 'de-DE' },
  { language: 'Italian', region: 'Italy', tag: 'it-IT' }
] as const;

export const voicePresets = [
  'Reed',
  'Shelley',
  'Bobby',
  'Rocko',
  'Glen',
  'Sandy',
  'Grandma',
  'Grandpa'
] as const;

export const punctuationLevels = [
  {
    name: 'None',
    description: 'Punctuation remains part of normal speech phrasing, but Eloqium does not replace punctuation marks with their spoken names.'
  },
  {
    name: 'Some',
    description: 'Eloqium speaks a core group of symbols, including mathematical and structural characters such as #, %, &, +, =, @, and < and >.'
  },
  {
    name: 'Most',
    description: 'The Some set is extended to include brackets, braces, quotation marks, dashes, underscores, colons, and semicolons. Colons in recognized time expressions can also be spoken at this level.'
  },
  {
    name: 'All',
    description: 'The Most set is extended to sentence and clause punctuation such as periods, commas, question marks, and exclamation marks. Decimal and thousands separators can also be verbalized in numeric expressions.'
  },
  {
    name: 'Custom',
    description: 'You enter the characters you want Eloqium to speak. Characters outside that selection continue through the ordinary text-processing path rather than being automatically added to the spoken-punctuation set.'
  }
] as const;

export const dictionaryModes = [
  {
    name: 'Exact match',
    detail: 'Applies when the source text matches the entry according to Eloqium’s exact-match rules. It is the narrowest matching mode and is normally the best starting point when one particular word or phrase needs a different reading.'
  },
  {
    name: 'Starts with',
    detail: 'Applies when the relevant text begins with the entry. This is useful when the same opening text should receive the same replacement across longer forms.'
  },
  {
    name: 'Ends with',
    detail: 'Applies when the relevant text ends with the entry. It can be useful for recurring endings or forms where the part that needs to change appears at the end of the text being matched.'
  },
  {
    name: 'Contains',
    detail: 'Applies when the entry occurs within the relevant text. Because this is broader than an exact match, it should be used when you intentionally want the rule to match text containing the source phrase.'
  }
] as const;

export const numberProcessingModes = [
  {
    name: 'Smart',
    description: 'Selects formatting based on context, preserving dates, times, decimals, currency, and phone numbers while grouping long digit sequences.'
  },
  {
    name: 'Digits',
    description: 'Separates a multi-digit number into individual digits before synthesis. For example, 1984 becomes 1 9 8 4.'
  },
  {
    name: 'Pairs',
    description: 'Groups digits from the left in pairs. For example, 1984 becomes 19 84, while 123 becomes 12 3.'
  },
  {
    name: 'Triplets',
    description: 'Groups digits from the right in groups of three. For example, 1234567 becomes 1 234 567.'
  }
] as const;

export const releases = [
  {
    version: 'v0.1.4',
    title: 'Foreground Service & Text Processing',
    date: '18 September 2026',
    summary: 'Adds Smart number processing, standalone capital indication, and optional foreground-service controls.',
    downloadUrl: '',
    githubUrl: 'https://github.com/memon-ismail/Eloqium-TTS/releases',
    changes: [
      'Add Smart number processing for context-aware number formatting.',
      'Add capital indication pitch raise for standalone uppercase single-letter words.',
      'Add optional Eloqium foreground service with persistent notification and battery optimization controls.'
    ]
  },
  {
    version: 'v0.1.3',
    title: 'Pronunciation Dictionary & ECI Controls',
    date: '14 September 2026',
    summary: 'Adds pronunciation entries, compatible IBM .dic import, and a setting for inline ECI voice and control tags.',
    downloadUrl: 'https://github.com/memon-ismail/Eloqium-TTS/releases/download/v0.1.3/eloqium-tts-release.apk',
    githubUrl: 'https://github.com/memon-ismail/Eloqium-TTS/releases/tag/v0.1.3',
    changes: [
      'Add pronunciation entries using OpenEVV Standard Phonemic Representation (SPR).',
      'Import compatible legacy IBM .dic dictionary files.',
      'Add the ECI Voice Tags setting under Settings → Voice.',
      'Improve dictionary search announcements for TalkBack users.',
      'Verify 16 KB ELF alignment for the native libraries included in the release.'
    ]
  },
  {
    version: 'v0.1.2',
    title: 'User Dictionary',
    date: '13 September 2026',
    summary: 'Introduces language-scoped dictionaries with text replacements, pronunciation rules, matching controls, and JSON import/export.',
    downloadUrl: 'https://github.com/memon-ismail/Eloqium-TTS/releases/download/v0.1.2/eloqium-tts-release.apk',
    githubUrl: 'https://github.com/memon-ismail/Eloqium-TTS/releases/tag/v0.1.2',
    changes: [
      'Create multiple named dictionaries for supported regional languages.',
      'Match entries exactly, by starting text, by ending text, or by contained text.',
      'Choose case-sensitive or case-insensitive matching and enable or disable entries.',
      'Import and export the version 2 JSON dictionary format.',
      'Open compatible JSON dictionary files from Android file managers and sharing apps.'
    ]
  },
  {
    version: 'v0.1.1',
    title: 'Custom Punctuation',
    date: '12 September 2026',
    summary: 'Adds a Custom punctuation level so users can choose which punctuation characters should be spoken.',
    downloadUrl: 'https://github.com/memon-ismail/Eloqium-TTS/releases/download/v0.1.1/eloqium-tts-release.apk',
    githubUrl: 'https://github.com/memon-ismail/Eloqium-TTS/releases/tag/v0.1.1',
    changes: [
      'Add None, Some, Most, All, and Custom punctuation levels.',
      'Allow users to enter their own set of punctuation characters.',
      'Preserve special handling for numbers, times, abbreviations, and punctuation attachment.',
      'Save punctuation settings across application restarts.'
    ]
  },
  {
    version: 'v0.1.0',
    title: 'Initial Production Release',
    date: '11 September 2026',
    summary: 'First production release with regional language variants, voice presets, speech controls, text processing, and Android TTS integration.',
    downloadUrl: 'https://github.com/memon-ismail/Eloqium-TTS/releases/download/v0.1.0/eloqium-tts-release.apk',
    githubUrl: 'https://github.com/memon-ismail/Eloqium-TTS/releases/tag/v0.1.0',
    changes: [
      'Add eight regional language variants with eight voice presets for each variant.',
      'Add speech rate, pitch, volume, and voice-character controls.',
      'Add punctuation, number, abbreviation, emoji, and emoticon processing.',
      'Integrate Eloqium with Android’s standard TextToSpeechService interface.'
    ]
  }
] as const;

export const currentRelease = releases[0];

export const site = {
  name: 'Eloqium TTS',
  currentVersion: currentRelease.version.replace(/^v/, ''),
  minAndroid: 'Android 6.0 (API 23) or later',
  github: 'https://github.com/memon-ismail/Eloqium-TTS',
  releasesUrl: 'https://github.com/memon-ismail/Eloqium-TTS/releases',
  telegram: 'https://t.me/blindroidofficial'
} as const;

export const navItems = [
  { label: 'Features', href: '/features/' },
  { label: 'Voices', href: '/voices/' },
  { label: 'Download', href: '/download/' },
  { label: 'Guide', href: '/guide/' },
  { label: 'Dictionary', href: '/dictionary/' },
  { label: 'Releases', href: '/releases/' },
  { label: 'About', href: '/about/' }
] as const;

export const sitePaths = [
  '/',
  '/features/',
  '/voices/',
  '/download/',
  '/guide/',
  '/dictionary/',
  '/releases/',
  '/about/',
  '/privacy/',
  '/licenses/'
] as const;
