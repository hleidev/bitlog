<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getArticleDraft } from '@/api/admin/article'
import type { ArticleDetailVO } from '@/api/admin/article'
import MarkdownIt from 'markdown-it'
import '@/assets/styles/prose.css'

const ICON_COPY =
  '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">' +
  '<rect x="9" y="9" width="13" height="13" rx="2"/>' +
  '<path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>' +
  '</svg>'

const ICON_CHECK =
  '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">' +
  '<polyline points="20 6 9 17 4 12"/>' +
  '</svg>'

const md = new MarkdownIt({ html: false, linkify: true, typographer: true })

md.renderer.rules.fence = (tokens, idx) => {
  const token = tokens[idx]
  const info = token.info ? token.info.trim() : ''
  const lang = info ? info.split(/\s+/g)[0] : ''
  const escapedLang = md.utils.escapeHtml(lang || 'text')
  const highlighted = md.utils.escapeHtml(token.content)
  return (
    '<div class="prose-code-wrap">' +
    '<div class="prose-code-controls">' +
    '<span class="prose-code-dots"><span data-dot="red"></span><span data-dot="yellow"></span><span data-dot="green"></span></span>' +
    '<span class="prose-code-lang">' + escapedLang + '</span>' +
    '<button class="prose-code-copy" type="button" title="复制代码">' + ICON_COPY + '</button>' +
    '</div>' +
    '<div class="prose-code-block-outer">' +
    '<pre class="prose-code-block"><code class="hljs">' + highlighted + '</code></pre>' +
    '</div>' +
    '</div>\n'
  )
}

const route = useRoute()
const article = ref<ArticleDetailVO | null>(null)
const loading = ref(true)
const error = ref(false)
const proseRef = ref<HTMLElement | null>(null)

// ── TOC ───────────────────────────────────────────────────────────────────────
const toc = ref<{ id: string; level: number; text: string }[]>([])
const activeSection = ref('')

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

const scrollToSection = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const onScroll = () => {
  for (let i = toc.value.length - 1; i >= 0; i--) {
    const el = document.getElementById(toc.value[i].id)
    if (el && el.getBoundingClientRect().top <= 100) {
      activeSection.value = toc.value[i].id
      break
    }
  }
}

// ── Copy handler ──────────────────────────────────────────────────────────────
const renderedContent = computed(() => article.value ? md.render(article.value.content) : '')

const handleCopyClick = async (e: MouseEvent) => {
  const target = e.target as Element
  const btn = target.closest<HTMLButtonElement>('.prose-code-copy')
  if (!btn) return
  const code = btn.closest('.prose-code-wrap')?.querySelector('.prose-code-block code')
  if (!code) return
  try {
    await navigator.clipboard.writeText(code.textContent ?? '')
    btn.innerHTML = ICON_CHECK
    btn.style.color = '#1a7f37'
    setTimeout(() => { btn.innerHTML = ICON_COPY; btn.style.color = '' }, 2000)
  } catch {}
}

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    article.value = await getArticleDraft(id)
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
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
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  proseRef.value?.removeEventListener('click', handleCopyClick)
})
</script>

<template>
  <div class="preview-page">
    <div class="preview-bar">
      <span class="preview-badge">草稿预览</span>
      <span class="preview-bar-title">{{ article?.title ?? '' }}</span>
      <a href="/admin/articles" class="preview-back">返回后台</a>
    </div>

    <div v-if="loading" class="preview-state">加载中…</div>
    <div v-else-if="error" class="preview-state preview-state--error">文章加载失败</div>
    <div v-else class="preview-layout">
      <div class="preview-content">
        <h1 class="preview-title">{{ article?.title }}</h1>
        <div v-if="article?.tags?.length" class="preview-tags">
          <span v-for="tag in article.tags" :key="tag" class="preview-tag">{{ tag }}</span>
        </div>
        <div class="prose" ref="proseRef" v-html="renderedContent" />
      </div>

      <aside v-if="toc.length" class="toc-sidebar">
        <div class="toc-card">
          <h4 class="toc-title">目录</h4>
          <nav class="toc-nav">
            <a
              v-for="item in visibleTocItems"
              :key="item.id"
              class="toc-item"
              :class="[`toc-item--h${item.level}`, { 'toc-item--active': activeSection === item.id }]"
              href="#"
              @click.prevent="scrollToSection(item.id)"
            >{{ item.text }}</a>
          </nav>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.preview-page {
  min-height: 100vh;
  background: var(--color-bg);
  font-family: var(--font-sans);
}

.preview-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 24px;
  height: 48px;
  background: #1f1f1f;
  border-bottom: 1px solid #333;
}

.preview-badge {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #febc2e;
  background: rgba(254, 188, 46, 0.12);
  border: 1px solid rgba(254, 188, 46, 0.3);
  padding: 2px 8px;
  border-radius: 3px;
}

.preview-bar-title {
  flex: 1;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-back {
  flex-shrink: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  text-decoration: none;
  transition: color 0.15s;
}

.preview-back:hover { color: rgba(255, 255, 255, 0.85); }

.preview-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 48px);
  font-size: 15px;
  color: var(--color-text-muted);
}

.preview-state--error { color: #e53e3e; }

.preview-layout {
  display: grid;
  grid-template-columns: 1fr 200px;
  gap: 48px;
  max-width: 1100px;
  margin: 0 auto;
  padding: 56px 24px 80px;
}

.preview-content { min-width: 0; }

.preview-title {
  font-family: var(--font-serif);
  font-size: 36px;
  font-weight: 500;
  color: var(--color-text-primary);
  line-height: 1.3;
  margin: 0 0 20px;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 36px;
}

.preview-tag {
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  border: 1px solid var(--color-border);
  padding: 2px 10px;
  border-radius: 2px;
}

/* ── TOC ── */
.toc-sidebar {
  position: sticky;
  top: calc(48px + 24px);
  max-height: calc(100vh - 48px - 48px);
  align-self: start;
}

.toc-card {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 48px - 48px);
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

.toc-item:hover { color: var(--color-text-secondary); }

.toc-item--active {
  color: var(--color-accent);
  border-left-color: var(--color-accent);
  font-weight: 500;
}

@media (max-width: 900px) {
  .preview-layout {
    grid-template-columns: 1fr;
    padding: 32px 16px 60px;
  }
  .toc-sidebar { display: none; }
  .preview-title { font-size: 26px; }
}
</style>
