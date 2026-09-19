# Eloqium TTS website

This directory contains the public website for Eloqium TTS. It is a static Astro site intended to deploy from `website/` on Vercel.

## Commands

```bash
npm install
npm run dev
npm run build
npm run preview
npm run check
```

Set `PUBLIC_SITE_URL` in the deployment environment to the final HTTPS origin when it is known. Vercel's deployment URL is used automatically when the variable is not set; the value is used for canonical metadata and the generated sitemap.

Product and release data that changes over time lives in `src/data/site.ts`. The current Download page links directly to the latest APK; the Releases page keeps the version history and older downloads.

The website uses semantic HTML, a keyboard-accessible navigation menu, page-specific metadata, a generated sitemap, and `robots.txt`. The site is intended to remain lightweight and easy to deploy from Vercel without a custom server.
