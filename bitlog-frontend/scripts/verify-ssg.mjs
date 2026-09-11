/**
 * 构建后校验：确认预渲染产物真的带上了内容和 SEO 元数据。
 *
 * 预渲染取数依赖构建时能访问后端 API。接口不可用时（VPS 重启、容器挂了），
 * vite-ssg 仍会输出结构完整但内容为空的 HTML —— 构建看起来是成功的，
 * 部署上去就是一批标题全叫 BitLog 的空壳页。
 *
 * 后端的发布钩子会在文章发布后立刻触发重建，正好可能撞上后端刚重启的时机，
 * 所以这里做硬校验：产物不达标就让构建失败。
 */
import { readdir, readFile } from 'node:fs/promises'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const DIST = join(dirname(fileURLToPath(import.meta.url)), '..', 'dist')
const FALLBACK_TITLES = new Set(['BitLog', '首页 | BitLog', '文章详情 | BitLog'])

const failures = []

function check(condition, message) {
  if (!condition) failures.push(message)
}

function titleOf(html) {
  return (html.match(/<title>(.*?)<\/title>/s)?.[1] ?? '').trim()
}

async function read(file) {
  return readFile(join(DIST, file), 'utf-8')
}

// ── 文章详情页 ────────────────────────────────────────────────────────────────
const articleFiles = (await readdir(join(DIST, 'article')).catch(() => [])).filter((f) =>
  f.endsWith('.html'),
)

check(articleFiles.length > 0, 'dist/article/ 下没有任何预渲染页面')

for (const file of articleFiles) {
  const html = await read(join('article', file))
  const title = titleOf(html)
  const where = `article/${file}`

  check(
    !FALLBACK_TITLES.has(title) && title.endsWith(' | BitLog'),
    `${where}: 文章标题没有正确渲染（拿到 "${title}"）`,
  )
  check(html.includes('application/ld+json'), `${where}: 缺少 JSON-LD 结构化数据`)
  check(html.includes('property="og:title"'), `${where}: 缺少 og:title`)
  check(html.includes('class="article-title"'), `${where}: 正文标题没有进入静态 HTML`)
}

// 标题必须两两不同，否则说明取数串了或全部退化成默认值
const titles = await Promise.all(
  articleFiles.map(async (f) => titleOf(await read(join('article', f)))),
)
const unique = new Set(titles)
check(
  unique.size === titles.length,
  `文章标题出现重复：${titles.length} 个页面只有 ${unique.size} 个不同标题`,
)

// ── 列表页与首页 ──────────────────────────────────────────────────────────────
const articlesHtml = await read('articles.html').catch(() => '')
check(articlesHtml !== '', 'dist/articles.html 不存在')
if (articlesHtml) {
  check(articlesHtml.includes('class="article-row'), 'articles.html: 文章列表没有进入静态 HTML')
}

const indexHtml = await read('index.html').catch(() => '')
check(indexHtml !== '', 'dist/index.html 不存在')
check(indexHtml.includes('class="article-row'), 'index.html: 首页文章列表没有进入静态 HTML')

// ── 公共页面名称：浏览器标题和分享标题不能退回统一的站点默认值 ────────────────
const publicPages = [
  { file: 'index.html', name: '首页', share: true },
  { file: 'articles.html', name: '文章', share: true },
  { file: 'friends.html', name: '友链', share: true },
  { file: 'notifications.html', name: '通知', share: false },
  { file: 'auth/callback.html', name: '登录', share: false },
]

for (const { file, name, share } of publicPages) {
  const html = await read(file).catch(() => '')
  const expectedTitle = `${name} | BitLog`
  const titleTags = html.match(/<title>.*?<\/title>/gs) ?? []
  check(
    titleTags.length === 1 && titleOf(html) === expectedTitle,
    `${file}: 应有且仅有一个标题「${expectedTitle}」（拿到 "${titleOf(html)}"）`,
  )
  if (share) {
    const ogTitles = (html.match(/<meta\b[^>]*>/g) ?? []).filter((tag) =>
      tag.includes('property="og:title"'),
    )
    check(
      ogTitles.length === 1 && ogTitles[0].includes(`content="${expectedTitle}"`),
      `${file}: 分享标题应为「${expectedTitle}」，且不能重复`,
    )
  }
  if (['文章', '友链', '通知'].includes(name)) {
    const heading = (html.match(/<h1\b[^>]*>(.*?)<\/h1>/s)?.[1] ?? '')
      .replace(/<[^>]*>/g, '')
      .trim()
    check(heading === `${name}.`, `${file}: 页面主标题应使用功能名称「${name}」`)
  }
}

// ── 结果 ──────────────────────────────────────────────────────────────────────
if (failures.length > 0) {
  console.error(`\n[verify-ssg] 预渲染产物校验失败（${failures.length} 项）：`)
  for (const f of failures) console.error(`  ✗ ${f}`)
  console.error('\n请按失败项检查页面标题配置及构建时 API 可达性，再重新构建。\n')
  process.exit(1)
}

console.log(`[verify-ssg] 通过：${articleFiles.length} 篇文章元数据完整，5 个公共页面标题一致`)
