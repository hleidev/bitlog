<script setup lang="ts">
/**
 * ArticleContent — 文章阅读视图（只读）
 *
 * 取代 @bitlog/editor 的 ArticleEditor (read-only)：
 *   之前：Tiptap + ProseMirror 解析 Markdown → DOM
 *   现在：Lute WASM 解析 Markdown → HTML → 客户端 mermaid 渲染
 *
 * 写作侧（VditorWriter）也是 Lute 解析，输出结构与这里一致。
 * → 写出来什么样 = 读出来什么样，引擎统一。
 *
 * 代码块：Lute 输出 <pre><code class="language-xxx">CODE</code></pre>，
 * 在这里后处理为与原 Tiptap NodeView 视觉等价的结构（带 lang 标签
 * + 复制按钮的 .code-block-wrapper）。保留所有 prose.css 样式类。
 *
 * Mermaid：Lute 输出 <div class="language-mermaid">CODE</div>，
 * 客户端 mermaid.render() 替换为 SVG，失败显示错误提示。
 */
import { ref, onMounted, onBeforeUnmount, watch, useTemplateRef } from 'vue'
import mermaid from 'mermaid'
import { renderMarkdownToHtml } from '@/utils/lute-renderer'
import ImageLightbox from './ImageLightbox.vue'
import './prose.css'

const props = withDefaults(defineProps<{
  content: string
}>(), { content: '' })

const rootRef = useTemplateRef<HTMLElement>('rootRef')
const html = ref('')
const mermaidError = ref<string | null>(null)
let mermaidGlobalId = 0

// 图片 lightbox 状态
const lightboxOpen = ref(false)
const lightboxSrc = ref<string | null>(null)
const lightboxAlt = ref<string>('')
// Mermaid SVG 通过 slot 传入（不走 src/alt 路径）
const lightboxHtml = ref<string | null>(null)

mermaid.initialize({ startOnLoad: false, theme: 'neutral' })

async function render() {
  try {
    const rendered = await renderMarkdownToHtml(props.content || '')
    html.value = enhanceCodeBlocks(rendered)
  } catch (e) {
    console.error('ArticleContent render failed:', e)
    html.value = `<pre>${escapeHtml(props.content || '')}</pre>`
  }
}

watch(() => props.content, render, { immediate: false })
onMounted(render)

/**
 * Lute 输出 <pre><code class="language-xxx">CODE</code></pre>，
 * 改造成与原 Tiptap NodeView 视觉等价的结构，让 prose.css 现有
 * .code-block-wrapper / .code-header / .code-lang-label / .copy-btn
 * 样式直接命中。
 *
 * 改造规则：
 *   <pre><code class="language-typescript">const x = 1</code></pre>
 *   →  .code-block-wrapper
 *        .code-header [data-lang="typescript"]
 *        pre.code-body code.language-typescript
 */
function enhanceCodeBlocks(raw: string): string {
  // 避免 <pre><code> 之间的空白/换行被 regex 误伤——用 [\s\S] 匹配。
  let out = raw.replace(
    /<pre><code class="language-([^"]+)">([\s\S]*?)<\/code><\/pre>/g,
    (_m, lang, body) => wrapCodeBlock(lang, body),
  )
  out = out.replace(
    /<pre><code>([\s\S]*?)<\/code><\/pre>/g,
    (_m, body) => wrapCodeBlock('text', body),
  )
  out = out.replace(
    /<div class="language-mermaid">([\s\S]*?)<\/div>/g,
    (_m, body) => wrapCodeBlock('mermaid', body),
  )
  return out
}

function wrapCodeBlock(lang: string, body: string): string {
  if (lang === 'mermaid') {
    // Mermaid 块：不要 lang 标签 / 复制按钮 —— 它是图表不是代码，
    // 复制源码没有意义。客户端 mermaid.render() 替换占位 <pre>。
    return `<div class="mermaid-block" data-mermaid-block="1">` +
      `<pre class="mermaid-block__source" data-mermaid-source><code class="language-mermaid">${body}</code></pre>` +
      `</div>`
  }
  // 空代码块：保留原始 <pre>，不显示 lang 标签 / 复制按钮
  // （防止某些边界场景下出现「只有头没有身体」的孤悬 code header）
  if (!body.trim()) {
    return `<pre><code class="language-${lang}">${body}</code></pre>`
  }
  return `<div class="code-block-wrapper" data-lang="${lang}">` +
    `<div class="code-header">` +
    `<span class="code-lang-label">${lang}</span>` +
    `<button class="copy-btn" data-copy-btn type="button" title="Copy">` +
    `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">` +
    `<rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>` +
    `<path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>` +
    `</svg></button></div>` +
    `<pre class="code-body"><code class="language-${lang}">${body}</code></pre>` +
    `</div>`
}

// Mermaid 客户端渲染
async function hydrateMermaid() {
  if (!rootRef.value) return
  const blocks = rootRef.value.querySelectorAll<HTMLElement>('[data-mermaid-block="1"]')
  for (const block of Array.from(blocks)) {
    const sourceEl = block.querySelector<HTMLElement>('[data-mermaid-source]')
    if (!sourceEl) continue
    const source = sourceEl.textContent?.trim() ?? ''
    if (!source) continue
    try {
      const id = `mermaid-render-${++mermaidGlobalId}`
      const { svg } = await mermaid.render(id, source)
      // 用 .mermaid-svg-wrap 包裹 SVG（轻量 wrapper，无 lang 标签 / 无 header）
      const wrap = document.createElement('div')
      wrap.className = 'mermaid-svg-wrap'
      wrap.innerHTML = svg.replace(/max-width:\s*[\d.]+px;?\s*/g, '')
      sourceEl.replaceWith(wrap)
    } catch (e) {
      mermaidError.value = (e as Error).message || 'Mermaid 语法错误'
      const err = document.createElement('div')
      err.className = 'mermaid-error'
      err.textContent = 'Mermaid 语法错误'
      sourceEl.replaceWith(err)
    }
  }
}

// 复制按钮 + 图片 lightbox + Mermaid lightbox（共用 ImageLightbox）
function onRootClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  // 复制按钮
  const btn = target.closest<HTMLElement>('[data-copy-btn]')
  if (btn) {
    const wrapper = btn.closest<HTMLElement>('.code-block-wrapper')
    const code = wrapper?.querySelector<HTMLElement>('pre.code-body code')
    if (code?.textContent) {
      navigator.clipboard.writeText(code.textContent).then(() => {
        btn.classList.add('copy-btn--copied')
        setTimeout(() => btn.classList.remove('copy-btn--copied'), 2000)
      }).catch(() => {/* 静默：clipboard 不可用 */})
    }
    return
  }
  // 图片点击 → 打开 lightbox（走 ImageLightbox）
  const img = target.closest<HTMLImageElement>('img')
  if (img) {
    e.preventDefault()
    lightboxSrc.value = img.src
    lightboxAlt.value = img.alt
    lightboxOpen.value = true
    return
  }
  // Mermaid SVG 点击 → 走 ImageLightbox 的 slot 路径，让 SVG 以自然尺寸显示
  const mermaidWrap = target.closest<HTMLElement>('.mermaid-block .mermaid-svg-wrap')
  if (mermaidWrap) {
    e.preventDefault()
    // 提取 SVG outerHTML，作为 lightbox 的 slot 内容
    const svg = mermaidWrap.querySelector('svg')
    if (svg) {
      lightboxHtml.value = svg.outerHTML
      lightboxOpen.value = true
    }
  }
}

function onRootKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    closeLightbox()
  }
}

function closeLightbox() {
  lightboxOpen.value = false
  lightboxSrc.value = null
  lightboxHtml.value = null
  lightboxAlt.value = ''
}

onMounted(() => {
  hydrateMermaid()
  document.addEventListener('keydown', onRootKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onRootKeydown)
  closeLightbox()
})

// 内容变化时重新 hydrate mermaid
watch(html, () => nextTickHydrate())

async function nextTickHydrate() {
  await new Promise(r => setTimeout(r, 0))
  hydrateMermaid()
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}
</script>

<template>
  <div ref="rootRef" class="ProseMirror article-content" @click="onRootClick" v-html="html" />
  <ImageLightbox
    :open="lightboxOpen"
    :src="lightboxSrc ?? undefined"
    :alt="lightboxAlt"
    @close="closeLightbox"
  >
    <template v-if="lightboxHtml" #interactive>
      <div class="lightbox-svg-host" v-html="lightboxHtml" />
    </template>
  </ImageLightbox>
</template>

<style scoped>
/* 不需要 scoped 样式：所有视觉规则都通过 .ProseMirror 类命中 prose.css。
   这里只补 lightbox 的最小样式（Mermaid 放大弹层），因为原 Tiptap
   ImageLightbox 是 Vue 组件，这里直接走原生 DOM。 */
</style>

<style>
/* ── Mermaid 块（详情页）───────────────────────────────────────── */
/* 没有 lang 标签 / 复制按钮 —— 是图表不是代码。 */
.mermaid-block {
  margin: 28px 0;
  background: #fff;
  border: 1px solid var(--color-border, #e8e4de);
  border-radius: 4px;
  overflow: hidden;
}
.mermaid-block__source {
  display: none; /* 渲染后被 SVG 替换，但保留 fallback 语义 */
}
.mermaid-svg-wrap {
  display: block;
  padding: 24px 20px;
  background: #fff;
  cursor: zoom-in;
}
.mermaid-svg-wrap svg {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 0 auto;
}

/* ── Mermaid 错误的最小兜底样式（如果 Mermaid 渲染失败）────────── */
.mermaid-error {
  padding: 16px 20px;
  color: #e06c75;
  font-family: var(--font-mono, monospace);
  font-size: 12px;
}
</style>
