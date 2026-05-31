import { writeFileSync } from 'fs'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const PUBLIC_DIR = resolve(__dirname, '../public')
const SITE_URL = 'https://bitlog.harrylei.top'
const API_BASE = 'https://api.harrylei.top/api'
const PAGE_SIZE = 100

async function fetchAllArticles() {
  const articles = []
  let pageNum = 1

  while (true) {
    const url = `${API_BASE}/v1/article/page?pageNum=${pageNum}&pageSize=${PAGE_SIZE}`
    const res = await fetch(url)
    if (!res.ok) throw new Error(`API returned ${res.status} for ${url}`)
    const { data } = await res.json()
    articles.push(...data.content)
    if (!data.hasNext) break
    pageNum++
  }

  return articles
}

function buildSitemap(articles) {
  const pages = [
    { loc: `${SITE_URL}/`, changefreq: 'weekly', priority: '1.0' },
    { loc: `${SITE_URL}/articles`, changefreq: 'daily', priority: '0.9' },
    ...articles.map((a) => ({
      loc: `${SITE_URL}/article/${a.id}`,
      lastmod: a.publishTime ? a.publishTime.slice(0, 10) : undefined,
      changefreq: 'monthly',
      priority: '0.8',
    })),
  ]

  const urlNodes = pages
    .map((p) => {
      const lastmod = p.lastmod ? `\n    <lastmod>${p.lastmod}</lastmod>` : ''
      return `  <url>\n    <loc>${p.loc}</loc>${lastmod}\n    <changefreq>${p.changefreq}</changefreq>\n    <priority>${p.priority}</priority>\n  </url>`
    })
    .join('\n')

  return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urlNodes}\n</urlset>`
}

async function main() {
  let articles = []
  try {
    articles = await fetchAllArticles()
    console.log(`[sitemap] fetched ${articles.length} articles`)
  } catch (err) {
    console.warn(`[sitemap] failed to fetch articles: ${err.message}`)
    console.warn('[sitemap] generating sitemap with static pages only')
  }

  writeFileSync(`${PUBLIC_DIR}/sitemap.xml`, buildSitemap(articles), 'utf-8')
  console.log('[sitemap] wrote public/sitemap.xml')
}

main()
