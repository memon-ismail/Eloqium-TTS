import type { APIRoute } from 'astro';
import { sitePaths } from '../data/site';

export const GET: APIRoute = ({ site }) => {
  const origin = site?.origin || '';
  const entries = sitePaths.map((path) => {
    const loc = origin ? new URL(path, `${origin}/`).toString() : path;
    return `  <url><loc>${loc}</loc></url>`;
  }).join('\n');
  const body = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${entries}\n</urlset>`;
  return new Response(body, {
    headers: { 'Content-Type': 'application/xml; charset=utf-8' }
  });
};
