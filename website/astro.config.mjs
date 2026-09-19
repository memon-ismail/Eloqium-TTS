import { defineConfig } from 'astro/config';

const configuredSite = process.env.PUBLIC_SITE_URL || (process.env.VERCEL_URL ? `https://${process.env.VERCEL_URL}` : undefined);

export default defineConfig({
  output: 'static',
  site: configuredSite,
  vite: {
    server: { allowedHosts: true },
    preview: { allowedHosts: true }
  }
});
