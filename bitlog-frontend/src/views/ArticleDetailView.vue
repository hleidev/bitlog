<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { getArticleDetail, type ArticleDetailVO } from '@/api/article'
import { formatDate } from '@/utils/format'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import mermaid from 'mermaid'

const ICON_COPY =
  '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">' +
  '<rect x="9" y="9" width="13" height="13" rx="2"/>' +
  '<path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>' +
  '</svg>'

const ICON_CHECK =
  '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">' +
  '<polyline points="20 6 9 17 4 12"/>' +
  '</svg>'

const MERMAID_CLASS = 'mermaid'

const md = new MarkdownIt({ html: false, linkify: true, typographer: true })

const getMermaidTheme = () => {
  return document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'default'
}

md.renderer.rules.fence = (tokens, idx) => {
  const token = tokens[idx]
  const info = token.info ? token.info.trim() : ''
  const lang = info ? info.split(/\s+/g)[0] : ''
  const str = token.content
  const escapedLang = md.utils.escapeHtml(lang || 'text')

  if (lang === MERMAID_CLASS) {
    return '<pre class="' + MERMAID_CLASS + '">' + md.utils.escapeHtml(str) + '</pre>\n'
  }

  const highlighted = lang && hljs.getLanguage(lang)
    ? hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
    : md.utils.escapeHtml(str)

  return (
    '<div class="prose-code-wrap">' +
    '<div class="prose-code-controls">' +
    '<span class="prose-code-dots"><span data-dot="red"></span><span data-dot="yellow"></span><span data-dot="green"></span></span>' +
    '<span class="prose-code-lang">' + escapedLang + '</span>' +
    '<button class="prose-code-copy" type="button" title="复制代码">' + ICON_COPY + '</button>' +
    '</div>' +
    '<div class="prose-code-block-outer">' +
    '<pre class="prose-code-block"><code class="hljs language-' + escapedLang + '">' + highlighted + '</code></pre>' +
    '</div>' +
    '</div>\n'
  )
}

const proseRef = ref<HTMLElement | null>(null)

const fullscreenVisible = ref(false)
const fullscreenLang = ref('')
const fullscreenHighlighted = ref('')
let fullscreenKeyHandler: ((e: KeyboardEvent) => void) | null = null

watch(fullscreenVisible, (val) => {
  if (val) {
    fullscreenKeyHandler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') { fullscreenVisible.value = false }
    }
    window.addEventListener('keydown', fullscreenKeyHandler)
  } else if (fullscreenKeyHandler) {
    window.removeEventListener('keydown', fullscreenKeyHandler)
    fullscreenKeyHandler = null
  }
})

const handleCopyClick = async (e: MouseEvent) => {
  const target = e.target as Element

  const btn = target.closest<HTMLButtonElement>('.prose-code-copy')
  if (btn) {
    const code = btn.closest('.prose-code-wrap')?.querySelector('.prose-code-block code')
    if (!code) return
    try {
      await navigator.clipboard.writeText(code.textContent ?? '')
      btn.innerHTML = ICON_CHECK
      btn.style.color = '#1a7f37'
      setTimeout(() => {
        if (btn) { btn.innerHTML = ICON_COPY; btn.style.color = '' }
      }, 2000)
    } catch {}
    return
  }

  const dot = target.closest<HTMLElement>('[data-dot]')
  if (dot) {
    const wrap = dot.closest<HTMLElement>('.prose-code-wrap')
    if (!wrap) return
    const type = dot.dataset.dot
    if (type === 'red' || type === 'yellow') {
      wrap.classList.toggle('is-collapsed')
    } else if (type === 'green') {
      const code = wrap.querySelector('.prose-code-block code')
      fullscreenLang.value = wrap.querySelector('.prose-code-lang')?.textContent?.trim() ?? ''
      fullscreenHighlighted.value = code?.innerHTML ?? ''
      fullscreenVisible.value = true
    }
  }
}

const router = useRouter()
const route = useRoute()

const article = ref<ArticleDetailVO | null>(null)
const loading = ref(true)
const error = ref(false)

const renderedContent = computed(() => (article.value ? md.render(article.value.content) : ''))

const toc = ref<{ id: string; level: number; text: string }[]>([])
const activeSection = ref('')
const scrollProgress = ref(0)

const activeParentId = computed(() => {
  const idx = toc.value.findIndex((t) => t.id === activeSection.value)
  if (idx === -1) return toc.value.find((t) => t.level === 2)?.id ?? ''
  for (let i = idx; i >= 0; i--) {
    if (toc.value[i].level === 2) return toc.value[i].id
  }
  return ''
})

const visibleTocItems = computed(() =>
  toc.value.filter((item, idx) => {
    if (item.level === 2) return true
    for (let i = idx - 1; i >= 0; i--) {
      if (toc.value[i].level === 2) return toc.value[i].id === activeParentId.value
    }
    return false
  }),
)

const renderMermaid = async () => {
  const mermaidEls = document.querySelectorAll<HTMLElement>('.prose .' + MERMAID_CLASS)
  if (!mermaidEls.length) return
  mermaid.initialize({ startOnLoad: false, securityLevel: 'loose', theme: getMermaidTheme() })
  mermaidEls.forEach((el) => el.removeAttribute('data-processed'))
  await mermaid.run({ nodes: Array.from(mermaidEls), suppressErrors: true })
}

const themeObserver = new MutationObserver(() => {
  if (article.value) renderMermaid()
})

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    article.value = await getArticleDetail(id)
  } catch {
    error.value = true
  }
  loading.value = false
  if (!article.value) return
  await nextTick()
  const headings = document.querySelectorAll<HTMLElement>('.prose h1, .prose h2, .prose h3, .prose h4')
  toc.value = Array.from(headings).map((el, i) => {
    if (!el.id) el.id = `heading-${i}`
    const tag = el.tagName
    const level = tag === 'H1' || tag === 'H2' ? 2 : 3
    return { id: el.id, level, text: el.textContent ?? '' }
  })
  if (toc.value.length) activeSection.value = toc.value[0].id
  window.addEventListener('scroll', onScroll, { passive: true })
  proseRef.value?.addEventListener('click', handleCopyClick)

  await renderMermaid()
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
})

const onScroll = () => {
  const el = document.documentElement
  const scrolled = el.scrollTop
  const total = el.scrollHeight - el.clientHeight
  scrollProgress.value = total > 0 ? (scrolled / total) * 100 : 0

  for (let i = toc.value.length - 1; i >= 0; i--) {
    const section = document.getElementById(toc.value[i].id)
    if (section && section.getBoundingClientRect().top <= 100) {
      activeSection.value = toc.value[i].id
      break
    }
  }
}

const scrollToSection = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  proseRef.value?.removeEventListener('click', handleCopyClick)
  themeObserver.disconnect()
})
</script>

<template>
  <!-- Reading progress bar -->
  <div class="progress-bar" :style="{ width: scrollProgress + '%' }" />

  <!-- Loading -->
  <div v-if="loading" class="page-state">
    <div class="skeleton-header" />
    <div class="skeleton-body container">
      <div class="skeleton-line w-60" />
      <div class="skeleton-line w-100" />
      <div class="skeleton-line w-80" />
      <div class="skeleton-line w-100" />
      <div class="skeleton-line w-70" />
    </div>
  </div>

  <!-- Error -->
  <div v-else-if="error" class="page-state page-state--error">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
      <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
    </svg>
    <p>文章加载失败</p>
    <button @click="router.back()">返回上一页</button>
  </div>

  <div v-else-if="article" class="article-detail">
    <!-- Article header (dark, no cover image) -->
    <div class="article-header">
      <div class="article-header__inner">
        <div class="article-header__label">
          <RouterLink
            v-if="article.category"
            :to="{ path: '/articles', query: { categoryId: article.category.id } }"
            class="meta-category"
          >{{ article.category.name }}</RouterLink>
          <div class="header-rule"></div>
        </div>
        <h1 class="article-title">{{ article.title }}</h1>
        <div class="article-header__foot">
          <span class="meta-date">{{ formatDate(article.publishTime) }}</span>
        </div>
      </div>
    </div>

    <!-- Article layout -->
    <div class="article-layout container view-enter">
      <!-- Main content -->
      <article class="article-body">
        <!-- Tags -->
        <div v-if="article.tags.length" class="article-tags">
          <RouterLink
            v-for="tag in article.tags"
            :key="tag.id"
            :to="{ path: '/articles', query: { tagId: tag.id } }"
            class="article-tag"
          >{{ tag.name }}</RouterLink>
        </div>

        <div class="prose" ref="proseRef" v-html="renderedContent" />

        <!-- Footer tags -->
        <div class="article-footer">
          <div class="article-footer__tags">
            <RouterLink
              v-for="tag in article.tags"
              :key="tag.id"
              :to="{ path: '/articles', query: { tagId: tag.id } }"
              class="footer-tag"
            >{{ tag.name }}</RouterLink>
          </div>
        </div>

        <!-- Comment section -->
        <div class="comment-section">
          <div class="section-header">
            <span class="section-label">评论</span>
            <div class="section-rule"></div>
          </div>
          <p class="comment-placeholder">评论功能开发中。</p>
        </div>
      </article>

      <!-- TOC sidebar -->
      <aside class="toc-sidebar">
        <div class="toc-card">
          <h4 class="toc-title">目录</h4>
          <nav class="toc-nav">
            <a
              v-for="item in visibleTocItems"
              :key="item.id"
              class="toc-item"
              :class="[`toc-item--h${item.level}`, { 'toc-item--active': activeSection === item.id }]"
              @click.prevent="scrollToSection(item.id)"
              href="#"
            >
              {{ item.text }}
            </a>
          </nav>
        </div>
      </aside>
    </div>
  </div>

  <!-- Code fullscreen modal -->
  <Teleport to="body">
    <Transition name="code-fs">
    <div v-if="fullscreenVisible" class="code-fs-overlay" @click.self="fullscreenVisible = false">
      <div class="code-fs-panel">
        <div class="code-fs-header">
          <span class="code-fs-dots">
            <span class="code-fs-dot code-fs-dot--red" title="关闭" @click="fullscreenVisible = false"></span>
            <span class="code-fs-dot code-fs-dot--yellow"></span>
            <span class="code-fs-dot code-fs-dot--green"></span>
          </span>
          <span class="code-fs-lang">{{ fullscreenLang }}</span>
          <button class="code-fs-close" title="关闭" @click="fullscreenVisible = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="code-fs-body">
          <pre class="code-fs-pre"><code class="hljs" v-html="fullscreenHighlighted"></code></pre>
        </div>
      </div>
    </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ── Progress bar ── */
.progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 2px;
  background: var(--color-accent);
  z-index: 1000;
  transition: width 0.1s linear;
}

/* ── Loading / Error ── */
.page-state {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--color-text-muted);
}

.page-state--error svg {
  width: 40px;
  height: 40px;
  color: var(--color-text-faint);
}

.page-state--error p {
  font-size: 15px;
}

.page-state--error button {
  font-size: 13px;
  color: var(--color-accent);
  background: none;
  border: 1px solid var(--color-accent);
  border-radius: 4px;
  padding: 6px 18px;
  cursor: pointer;
  transition: all var(--transition-base);
  font-family: var(--font-sans);
}

.page-state--error button:hover {
  background: var(--color-bg-hover);
}

.skeleton-header {
  width: 100%;
  height: 320px;
  background: var(--color-hero-bg);
}

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 40px;
}

.skeleton-line {
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--color-bg-hover) 25%, var(--color-border) 50%, var(--color-bg-hover) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-line.w-60 { width: 60%; }
.skeleton-line.w-70 { width: 70%; }
.skeleton-line.w-80 { width: 80%; }
.skeleton-line.w-100 { width: 100%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── Article header ── */
.article-header {
  background: var(--color-hero-bg);
  padding-top: var(--spacing-header-height);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.article-header__inner {
  max-width: var(--spacing-container);
  margin: 0 auto;
  padding: 72px var(--spacing-page-padding) 80px;
}

.article-header__label {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 32px;
}

.header-rule {
  flex: 1;
  height: 1px;
  background: rgba(245, 243, 239, 0.08);
}

.meta-category {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--color-accent);
  text-decoration: none;
  flex-shrink: 0;
  background-image: linear-gradient(var(--color-accent), var(--color-accent));
  background-repeat: no-repeat;
  background-size: 0% 1px;
  background-position: left bottom;
  padding-bottom: 1px;
  transition: background-size var(--transition-sweep);
}

.meta-category:hover {
  background-size: 100% 1px;
}

.article-title {
  font-family: var(--font-serif);
  font-size: clamp(28px, 4.5vw, 54px);
  font-weight: 400;
  line-height: 1.28;
  color: var(--color-text-on-dark);
  max-width: 860px;
  letter-spacing: 0.01em;
}

.article-header__foot {
  margin-top: 28px;
}

.meta-date {
  font-size: 12px;
  color: rgba(245, 243, 239, 0.3);
  letter-spacing: 0.08em;
  font-family: var(--font-sans);
}

/* ── Layout ── */
.article-layout {
  display: flex;
  align-items: flex-start;
  gap: 48px;
  padding-top: 56px;
  padding-bottom: 100px;
}

/* ── Article body ── */
.article-body {
  flex: 1;
  min-width: 0;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 36px;
}

.article-tag {
  font-size: 11.5px;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  padding: 3px 10px;
  border-radius: var(--radius-tag);
  transition: all var(--transition-base);
}

.article-tag:hover {
  color: var(--color-accent);
  border-color: var(--color-accent);
}

/* ── Article footer ── */
.article-footer {
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
}

.article-footer__tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.footer-tag {
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  padding: 4px 12px;
  border-radius: var(--radius-tag);
  border: 1px solid var(--color-border);
  transition: all var(--transition-base);
}

.footer-tag:hover {
  color: var(--color-accent);
  border-color: var(--color-accent);
}

/* ── Comment section ── */
.comment-section {
  margin-top: 56px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.section-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.section-rule {
  flex: 1;
  height: 1px;
  background: var(--color-border);
}

.comment-placeholder {
  font-size: 13px;
  color: var(--color-text-faint);
  letter-spacing: 0.02em;
}

/* ── TOC sidebar ── */
.toc-sidebar {
  width: 220px;
  flex-shrink: 0;
  position: sticky;
  top: calc(var(--spacing-header-height) + 24px);
  max-height: calc(100vh - var(--spacing-header-height) - 48px);
}

.toc-card {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - var(--spacing-header-height) - 48px);
  overflow: hidden;
}

.toc-title {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--color-text-faint);
  padding: 0 10px 12px;
  flex-shrink: 0;
}

.toc-nav {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 0 0 12px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--color-border) transparent;
}

.toc-nav::-webkit-scrollbar { width: 2px; }
.toc-nav::-webkit-scrollbar-track { background: transparent; }
.toc-nav::-webkit-scrollbar-thumb { background: var(--color-border); }

.toc-item {
  display: block;
  font-size: 12.5px;
  line-height: 1.45;
  color: var(--color-text-muted);
  padding: 5px 10px;
  transition: color var(--transition-base);
  border-left: 2px solid transparent;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toc-item--h3 {
  padding-left: 20px;
  font-size: 12px;
  color: var(--color-text-faint);
}

.toc-item:hover {
  color: var(--color-text-secondary);
}

.toc-item--active {
  color: var(--color-accent);
  border-left-color: var(--color-accent);
  font-weight: 500;
}

/* ── Responsive ── */
@media (max-width: 900px) {
  .toc-sidebar { display: none; }
  .article-title { font-size: 26px; }
  .article-layout { padding-top: 40px; }
}

@media (max-width: 768px) {
  .article-header__inner {
    padding: 48px 20px 56px;
  }
}

/* ── Code fullscreen modal ── */
.code-fs-enter-active { transition: opacity 0.22s ease; }
.code-fs-leave-active { transition: opacity 0.18s ease; }
.code-fs-enter-from,
.code-fs-leave-to { opacity: 0; }
.code-fs-enter-active .code-fs-panel {
  transition: transform 0.22s cubic-bezier(0.34, 1.4, 0.64, 1), opacity 0.22s ease;
}
.code-fs-leave-active .code-fs-panel {
  transition: transform 0.18s ease, opacity 0.18s ease;
}
.code-fs-enter-from .code-fs-panel { transform: scale(0.96); opacity: 0; }
.code-fs-leave-to .code-fs-panel { transform: scale(0.98); opacity: 0; }

.code-fs-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.62);
  backdrop-filter: blur(6px);
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.code-fs-panel {
  width: calc(100vw - 40px);
  height: calc(100vh - 40px);
  background: #282828;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.code-fs-header {
  display: flex;
  align-items: center;
  padding: 0 14px;
  height: 40px;
  flex-shrink: 0;
  background: #282828;
}

.code-fs-dots {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.code-fs-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  cursor: pointer;
  transition: filter 0.12s;
}

.code-fs-dot:hover { filter: brightness(0.85); }

.code-fs-dot--red    { background: radial-gradient(circle at 38% 35%, #ff8a80, #ff5f57); position: relative; }
.code-fs-dot--yellow { background: radial-gradient(circle at 38% 35%, #ffe57f, #febc2e); cursor: default; }
.code-fs-dot--green  { background: radial-gradient(circle at 38% 35%, #69f0ae, #28c840); cursor: default; }

.code-fs-dot--red::after {
  content: '';
  position: absolute;
  inset: 0;
  margin: auto;
  width: 7px;
  height: 7px;
  background-color: rgba(0, 0, 0, 0.5);
  mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M2.5 2.5l5 5M7.5 2.5l-5 5' stroke='black' stroke-width='1.6' stroke-linecap='round'/%3E%3C/svg%3E");
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M2.5 2.5l5 5M7.5 2.5l-5 5' stroke='black' stroke-width='1.6' stroke-linecap='round'/%3E%3C/svg%3E");
  mask-size: contain; mask-repeat: no-repeat; mask-position: center;
  -webkit-mask-size: contain; -webkit-mask-repeat: no-repeat; -webkit-mask-position: center;
  opacity: 0;
  transition: opacity 0.12s;
}
.code-fs-dot--red:hover::after { opacity: 1; }

.code-fs-dot--yellow { position: relative; }
.code-fs-dot--yellow::after {
  content: '';
  position: absolute;
  inset: 0;
  margin: auto;
  width: 7px;
  height: 7px;
  background-color: rgba(0, 0, 0, 0.5);
  mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M2 5h6' stroke='black' stroke-width='1.6' stroke-linecap='round'/%3E%3C/svg%3E");
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2020/svg' viewBox='0 0 10 10'%3E%3Cpath d='M2 5h6' stroke='black' stroke-width='1.6' stroke-linecap='round'/%3E%3C/svg%3E");
  mask-size: contain; mask-repeat: no-repeat; mask-position: center;
  -webkit-mask-size: contain; -webkit-mask-repeat: no-repeat; -webkit-mask-position: center;
  opacity: 0;
  transition: opacity 0.12s;
}
.code-fs-dot--yellow:hover::after { opacity: 1; }

.code-fs-dot--green { position: relative; }
.code-fs-dot--green::after {
  content: '';
  position: absolute;
  inset: 0;
  margin: auto;
  width: 7px;
  height: 7px;
  background-color: rgba(0, 0, 0, 0.5);
  mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M4 1H1v3M6 9h3V6' stroke='black' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'/%3E%3Cpath d='M1.2 1.2l3.3 3.3M8.8 8.8L5.5 5.5' stroke='black' stroke-width='1.4' stroke-linecap='round'/%3E%3C/svg%3E");
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M4 1H1v3M6 9h3V6' stroke='black' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'/%3E%3Cpath d='M1.2 1.2l3.3 3.3M8.8 8.8L5.5 5.5' stroke='black' stroke-width='1.4' stroke-linecap='round'/%3E%3C/svg%3E");
  mask-size: contain; mask-repeat: no-repeat; mask-position: center;
  -webkit-mask-size: contain; -webkit-mask-repeat: no-repeat; -webkit-mask-position: center;
  opacity: 0;
  transition: opacity 0.12s;
}
.code-fs-dot--green:hover::after { opacity: 1; }

.code-fs-lang {
  flex: 1;
  text-align: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  font-family: var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  user-select: none;
}

.code-fs-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  color: rgba(255, 255, 255, 0.45);
  background: none;
  border: none;
  border-radius: 2px;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s, background 0.15s;
  flex-shrink: 0;
}

.code-fs-close:hover {
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.1);
}

.code-fs-close svg { width: 13px; height: 13px; display: block; }

.code-fs-body {
  flex: 1;
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.15) transparent;
}

.code-fs-pre { margin: 0; background: #282828; }

.code-fs-pre .hljs {
  display: block;
  padding: 20px 24px;
  font-family: var(--font-mono);
  font-size: 14px;
  line-height: 1.7;
  background: #282828 !important;
  color: #ffffff;
  white-space: pre;
}
</style>

<!-- Non-scoped: prose styles must apply to v-html injected content -->
<style>
.prose {
  font-size: 16px;
  line-height: 1.85;
  color: var(--color-text-secondary);
}

.prose h1 {
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin: 48px 0 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.prose h2 {
  font-family: var(--font-serif);
  font-size: 21px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin: 40px 0 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border);
}


.prose h1:first-child,
.prose h2:first-child { margin-top: 0; }

.prose h3 {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 28px 0 12px;
}

.prose h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 22px 0 10px;
}

.prose > p:first-of-type {
  font-size: 18px;
  color: var(--color-text-primary);
  line-height: 1.8;
}

.prose p { margin-bottom: 18px; }

.prose strong {
  font-weight: 600;
  color: var(--color-text-primary);
}

.prose em {
  font-style: italic;
  color: var(--color-text-muted);
}

.prose a {
  color: var(--color-accent);
  text-decoration: underline;
  text-decoration-color: rgba(184, 92, 56, 0.35);
  text-underline-offset: 3px;
  transition: color var(--transition-base), text-decoration-color var(--transition-base);
}

.prose a:hover {
  color: var(--color-accent-dark);
  text-decoration-color: var(--color-accent-dark);
}

.prose code {
  font-family: var(--font-mono);
  font-size: 0.875em;
  background: var(--color-bg-hover);
  color: var(--color-accent-light);
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid var(--color-border);
}

.prose .prose-code-wrap {
  position: relative;
  margin: 26px 0;
  border-radius: 4px;
  overflow: hidden;
  isolation: isolate;
  contain: paint;
  background: #282828;
}

.prose .prose-code-controls {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 38px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  pointer-events: none;
  z-index: 1;
}

.prose .prose-code-controls .prose-code-copy { pointer-events: all; }

.prose .prose-code-dots {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.prose .prose-code-dots span {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  cursor: pointer;
  pointer-events: all;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.prose .prose-code-dots span::after {
  content: '';
  position: absolute;
  width: 7px;
  height: 7px;
  background-color: rgba(0, 0, 0, 0.5);
  mask-size: contain; mask-repeat: no-repeat; mask-position: center;
  -webkit-mask-size: contain; -webkit-mask-repeat: no-repeat; -webkit-mask-position: center;
  opacity: 0;
  transition: opacity 0.12s;
}

.prose .prose-code-dots:hover span::after { opacity: 1; }

.prose .prose-code-dots span:nth-child(1) { background: radial-gradient(circle at 38% 35%, #ff8a80, #ff5f57); }
.prose .prose-code-dots span:nth-child(2) { background: radial-gradient(circle at 38% 35%, #ffe57f, #febc2e); }
.prose .prose-code-dots span:nth-child(3) { background: radial-gradient(circle at 38% 35%, #69f0ae, #28c840); }

.prose .prose-code-dots span:nth-child(1)::after {
  mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M2.5 2.5l5 5M7.5 2.5l-5 5' stroke='black' stroke-width='1.6' stroke-linecap='round'/%3E%3C/svg%3E");
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M2.5 2.5l5 5M7.5 2.5l-5 5' stroke='black' stroke-width='1.6' stroke-linecap='round'/%3E%3C/svg%3E");
}
.prose .prose-code-dots span:nth-child(2)::after {
  mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M2 5h6' stroke='black' stroke-width='1.6' stroke-linecap='round'/%3E%3C/svg%3E");
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M2 5h6' stroke='black' stroke-width='1.6' stroke-linecap='round'/%3E%3C/svg%3E");
}
.prose .prose-code-dots span:nth-child(3)::after {
  mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M1 4V1h3M9 6v3H6' stroke='black' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'/%3E%3Cpath d='M1.2 1.2l3.3 3.3M8.8 8.8L5.5 5.5' stroke='black' stroke-width='1.4' stroke-linecap='round'/%3E%3C/svg%3E");
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M1 4V1h3M9 6v3H6' stroke='black' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'/%3E%3Cpath d='M1.2 1.2l3.3 3.3M8.8 8.8L5.5 5.5' stroke='black' stroke-width='1.4' stroke-linecap='round'/%3E%3C/svg%3E");
}

.prose .prose-code-lang {
  flex: 1;
  text-align: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  font-family: var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  user-select: none;
}

.prose .prose-code-copy {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  color: rgba(255, 255, 255, 0.45);
  background: none;
  border: none;
  border-radius: 2px;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s, background 0.15s;
}

.prose .prose-code-copy:hover {
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.1);
}

.prose .prose-code-copy svg { width: 13px; height: 13px; display: block; pointer-events: none; }

.prose .prose-code-block-outer {
  display: grid;
  grid-template-rows: 1fr;
  transition: grid-template-rows 0.28s ease;
}

.prose .prose-code-wrap.is-collapsed { min-height: 38px; }
.prose .prose-code-wrap.is-collapsed .prose-code-block-outer { grid-template-rows: 0fr; }

.prose .prose-code-block {
  margin: 0;
  background: #282828;
  min-height: 0;
}

.prose .prose-code-block .hljs {
  display: block;
  overflow-x: auto;
  padding: 46px 22px 20px;
  font-family: var(--font-mono);
  font-size: 13.5px;
  line-height: 1.65;
  background: #282828 !important;
  color: #ffffff;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.15) transparent;
}

.prose .prose-code-block .hljs::-webkit-scrollbar { height: 3px; }
.prose .prose-code-block .hljs::-webkit-scrollbar-track { background: transparent; }
.prose .prose-code-block .hljs::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.15); border-radius: 2px; }

.prose .mermaid {
  background: transparent !important;
  border: none !important;
  border-radius: 0 !important;
  padding: 0 !important;
  margin: 24px 0 !important;
  overflow: visible;
  display: block;
}

.prose :deep(.mermaid svg) {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
  max-width: 100%;
}

.prose pre:not(.prose-code-block) {
  background: #0d1117;
  border-radius: 4px;
  padding: 20px 24px;
  overflow-x: auto;
  margin: 24px 0;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.prose pre:not(.prose-code-block) code {
  font-family: var(--font-mono);
  font-size: 13.5px;
  background: none;
  color: #a8c4d8;
  padding: 0;
  border: none;
  border-radius: 0;
  line-height: 1.75;
  white-space: pre;
}

.prose blockquote {
  border-left: 2px solid var(--color-accent);
  margin: 28px 0;
  padding: 14px 20px;
  background: rgba(184, 92, 56, 0.04);
}

.prose blockquote p {
  margin: 0;
  color: var(--color-text-muted);
  font-style: italic;
}

.prose ul,
.prose ol {
  padding-left: 24px;
  margin-bottom: 18px;
}

.prose li {
  margin-bottom: 8px;
  line-height: 1.75;
}

.prose ul li { list-style: disc; }
.prose ol li { list-style: decimal; }

.prose hr {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 40px 0;
}

.prose img {
  max-width: 100%;
  border-radius: 4px;
  margin: 20px 0;
}

.prose table {
  width: 100%;
  border-collapse: collapse;
  margin: 24px 0;
  font-size: 14px;
  border: 1px solid var(--color-border);
}

.prose th {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
  font-weight: 600;
  text-align: left;
  padding: 10px 16px;
  border-bottom: 1px solid var(--color-border);
}

.prose td {
  padding: 10px 16px;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-secondary);
}

.prose tr:last-child td { border-bottom: none; }

@media (max-width: 900px) {
  .prose { font-size: 15px; }
  .prose h2 { font-size: 19px; }
}
</style>
