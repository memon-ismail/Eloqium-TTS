import type { APIRoute } from 'astro';

export const GET: APIRoute = ({ site }) => {
  const sitemapUrl = site ? new URL('/sitemap.xml', site).toString() : '/sitemap.xml';
  return new Response(`User-agent: *\nAllow: /\nSitemap: ${sitemapUrl}\n`, {
    headers: { 'Content-Type': 'text/plain; charset=utf-8' }
  });
};
