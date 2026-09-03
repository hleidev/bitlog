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
import hljs from 'highlight.js/lib/common'
import { renderMarkdownToHtml } from '@/utils/lute-renderer'
import { useTheme } from '@/composables/useTheme'
import ImageLightbox from './ImageLightbox.vue'
import '@bitlog/editor/prose.css'

const props = withDefaults(
  defineProps<{
    content: string
  }>(),
  { content: '' },
)

// 正文由 Lute WASM 异步渲染，挂载时 DOM 还是空的。目录等下游消费者要靠
// 这个事件才知道标题什么时候真正进了 DOM。
const emit = defineEmits<{ rendered: [root: HTMLElement | null] }>()

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

/**
 * Mermaid 主题跟随站点明暗。
 *
 * 早先固定用 neutral(深字浅底)，暗色模式下只能反过来强行给画布刷浅底，
 * 结果是纯黑页面中间烧出一大块白板。这里改成用 base 主题 + 站点中性色阶，
 * 明暗两套各自成立，画布也就能跟着 --color-bg-card 走。
 *
 * ⚠️ base 主题只认显式给出的键，没覆盖到的会回落成 Mermaid 的浅色默认值。
 * 只配流程图那几个键是不够的：sequence 的 Note 会是亮黄、ER 的属性行会是
 * 近白、pie 的扇区会全糊成同一档灰。下面按图种分组补全。
 *
 * 已实测覆盖：flowchart / sequence / class / state / gantt / er / pie。
 * 新增图种（journey、quadrant、mindmap、timeline、sankey…）需要另行确认，
 * 别默认继承——它们各有一套自己的键。
 *
 * 只用中性色 + terracotta：图表是内容，配色跟随站点而非 Mermaid 默认。
 */
function mermaidPalette(dark: boolean) {
  const surface = dark ? '#141414' : '#fafafa'
  const node = dark ? '#1f1f1f' : '#ffffff'
  const nodeAlt = dark ? '#262626' : '#f5f5f5'
  // 必须是不透明色：Mermaid 会把同一个 token 同时当描边和填充用（ER 的属性行
  // 就是拿 border 当 fill 的）。半透明白当填充会在深底上烧出一条亮带。
  const border = dark ? '#4a4a4a' : '#cccccc'
  const text = dark ? '#fafafa' : '#111111'
  const line = dark ? '#8a8a8a' : '#767676'
  // accent 从 CSS 变量取，不在这里抄色值：Mermaid 要的是 JS 值（themeVariables
  // 吃不了 var()），但唯一真源仍应是 styles/variables.css 的 accent primitives。
  // 自定义属性不参与 transition，且主题切换是 setAttribute 同步完成、watcher
  // 异步才触发，所以此处读到的必定是切换后的终值，不会是过渡中的混合色。
  const css = getComputedStyle(document.documentElement)
  const accent = css.getPropertyValue('--color-accent').trim()
  const accentAlt = css.getPropertyValue('--color-accent-light').trim()
  // 分类色：中性设计下不引入彩虹色，改用「accent + 明度阶梯」保证可分辨。
  const ramp = dark
    ? [accent, '#d4d4d4', '#8a8a8a', '#4d4d4d', accentAlt, '#eaeaea']
    : [accent, '#3d3d3d', '#8a8a8a', '#d0d0d0', accentAlt, '#111111']
  const pie = Object.fromEntries(
    Array.from({ length: 12 }, (_, i) => [`pie${i + 1}`, ramp[i % ramp.length]]),
  )

  return {
    // ── 通用 ──
    background: surface,
    primaryColor: node,
    primaryBorderColor: border,
    primaryTextColor: text,
    secondaryColor: nodeAlt,
    tertiaryColor: node,
    lineColor: line,
    textColor: text,
    mainBkg: node,
    nodeBorder: border,
    edgeLabelBackground: surface,
    titleColor: text,
    // ── sequence ──
    actorBkg: node,
    actorBorder: border,
    actorTextColor: text,
    actorLineColor: line,
    signalColor: text,
    signalTextColor: text,
    labelBoxBkgColor: node,
    labelBoxBorderColor: border,
    labelTextColor: text,
    loopTextColor: text,
    noteBkgColor: nodeAlt,
    noteTextColor: text,
    noteBorderColor: border,
    activationBkgColor: nodeAlt,
    activationBorderColor: border,
    sequenceNumberColor: surface,
    // ── gantt ──
    sectionBkgColor: surface,
    altSectionBkgColor: nodeAlt,
    sectionBkgColor2: surface,
    taskBkgColor: node,
    taskBorderColor: border,
    taskTextColor: text,
    taskTextLightColor: text,
    taskTextDarkColor: text,
    taskTextOutsideColor: text,
    activeTaskBkgColor: accent,
    activeTaskBorderColor: accent,
    doneTaskBkgColor: nodeAlt,
    doneTaskBorderColor: border,
    critBkgColor: accent,
    critBorderColor: accent,
    gridColor: line,
    todayLineColor: accent,
    // ── er ──
    attributeBackgroundColorOdd: node,
    attributeBackgroundColorEven: nodeAlt,
    // ── pie ──
    ...pie,
    pieTitleTextColor: text,
    pieSectionTextColor: text,
    pieLegendTextColor: text,
    pieStrokeColor: border,
    pieOuterStrokeColor: border,
  }
}

function applyMermaidTheme(dark: boolean) {
  mermaid.initialize({
    startOnLoad: false,
    theme: 'base',
    fontFamily: "'Inter', -apple-system, 'PingFang SC', sans-serif",
    themeVariables: mermaidPalette(dark),
  })
}

const { isDark } = useTheme()
// mermaidPalette 读 getComputedStyle 取 CSS 变量，预渲染阶段没有 document。
// mermaid 本来也只在客户端渲染，这里跳过初始化即可。
if (!import.meta.env.SSR) applyMermaidTheme(isDark.value)

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
  out = out.replace(/<pre><code>([\s\S]*?)<\/code><\/pre>/g, (_m, body) =>
    wrapCodeBlock('text', body),
  )
  out = out.replace(/<div class="language-mermaid">([\s\S]*?)<\/div>/g, (_m, body) =>
    wrapCodeBlock('mermaid', body),
  )
  return out
}

function wrapCodeBlock(lang: string, body: string): string {
  if (lang === 'mermaid') {
    // Mermaid 块：不要 lang 标签 / 复制按钮 —— 它是图表不是代码，
    // 复制源码没有意义。客户端 mermaid.render() 替换占位 <pre>。
    return (
      `<div class="mermaid-block" data-mermaid-block="1">` +
      `<pre class="mermaid-block__source" data-mermaid-source><code class="language-mermaid">${body}</code></pre>` +
      `</div>`
    )
  }
  // 空代码块：保留原始 <pre>，不显示 lang 标签 / 复制按钮
  // （防止某些边界场景下出现「只有头没有身体」的孤悬 code header）
  if (!body.trim()) {
    return `<pre><code class="language-${lang}">${body}</code></pre>`
  }
  return (
    `<div class="code-block-wrapper" data-lang="${lang}">` +
    `<div class="code-header">` +
    `<span class="code-lang-label">${lang}</span>` +
    `<button class="copy-btn" data-copy-btn type="button" title="Copy">` +
    `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">` +
    `<rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>` +
    `<path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>` +
    `</svg></button></div>` +
    `<pre class="code-body"><code class="language-${lang}">${body}</code></pre>` +
    `</div>`
  )
}

/**
 * 按 viewBox 的固有尺寸给 Mermaid SVG 定尺。
 *
 * Mermaid 输出的是 width="100%" + viewBox，没有固有宽度可依，CSS 的 width:auto
 * 只会解析成容器宽度 —— 结果是窄图被放大（class 图放大 1.6 倍）、宽图被压扁
 * （甘特图固有 1699px 压进 758px 正文列是 0.45 倍，字小到读不了）。
 * 显式按 viewBox 定尺后，图表以设计尺寸呈现，文字大小才正常。
 *
 * 过高的图按比例缩到 70vh 以内；过宽的图不缩，交给 .mermaid-svg-wrap 横向滚动
 * （与本站表格、代码块一致），点击仍可进 lightbox 放大。
 */
function sizeMermaidSvg(svgEl: SVGElement | null) {
  if (!svgEl) return
  const vb = (svgEl.getAttribute('viewBox') || '').split(/[\s,]+/).map(Number)
  const [, , vw, vh] = vb
  if (!vw || !vh) return
  const maxH = window.innerHeight * 0.7
  const scale = vh > maxH ? maxH / vh : 1
  svgEl.style.width = `${Math.round(vw * scale)}px`
  svgEl.style.height = `${Math.round(vh * scale)}px`
}

// Mermaid 客户端渲染。
// 保留隐藏的 <pre data-mermaid-source>（不再 replaceWith），这样切换主题时
// 还能拿到原始语句重渲染。
//
// 并发保护：本函数是 fire-and-forget 调用的（主题切换 / 内容变化），而
// mermaid.render() 是异步的。若两轮交错，会各自 append 一个 SVG 造成重复。
// 用自增的 runId 作代际标记：await 回来发现已有更新的一轮开始，就丢弃本轮结果。
let mermaidRunId = 0

async function hydrateMermaid() {
  if (!rootRef.value) return
  const runId = ++mermaidRunId
  const blocks = rootRef.value.querySelectorAll<HTMLElement>('[data-mermaid-block="1"]')
  for (const block of Array.from(blocks)) {
    const sourceEl = block.querySelector<HTMLElement>('[data-mermaid-source]')
    if (!sourceEl) continue
    const source = sourceEl.textContent?.trim() ?? ''
    if (!source) continue
    try {
      const id = `mermaid-render-${++mermaidGlobalId}`
      const { svg } = await mermaid.render(id, source)
      if (runId !== mermaidRunId) return
      // 渲染成功后再换掉旧产物，避免切主题时闪烁空白
      block.querySelectorAll('.mermaid-svg-wrap, .mermaid-error').forEach((el) => el.remove())
      // 用 .mermaid-svg-wrap 包裹 SVG（轻量 wrapper，无 lang 标签 / 无 header）
      const wrap = document.createElement('div')
      wrap.className = 'mermaid-svg-wrap'
      wrap.innerHTML = svg.replace(/max-width:\s*[\d.]+px;?\s*/g, '')
      sizeMermaidSvg(wrap.querySelector('svg'))
      block.appendChild(wrap)
    } catch (e) {
      if (runId !== mermaidRunId) return
      mermaidError.value = (e as Error).message || 'Mermaid 语法错误'
      block.querySelectorAll('.mermaid-svg-wrap, .mermaid-error').forEach((el) => el.remove())
      const err = document.createElement('div')
      err.className = 'mermaid-error'
      err.textContent = 'Mermaid 语法错误'
      block.appendChild(err)
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
      navigator.clipboard
        .writeText(code.textContent)
        .then(() => {
          btn.classList.add('copy-btn--copied')
          setTimeout(() => btn.classList.remove('copy-btn--copied'), 2000)
        })
        .catch(() => {
          /* 静默：clipboard 不可用 */
        })
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

function hydrateHighlight() {
  if (!rootRef.value) return
  const codeEls = rootRef.value.querySelectorAll<HTMLElement>('pre.code-body code')
  for (const el of Array.from(codeEls)) {
    hljs.highlightElement(el)
  }
}

// 内容变化时重新 hydrate mermaid + highlight.js
watch(html, () => nextTickHydrate())

// 主题切换：Mermaid 的颜色是渲染期烧进 SVG 的，改 CSS 没用，必须重渲染。
watch(isDark, (dark) => {
  applyMermaidTheme(dark)
  hydrateMermaid()
})

async function nextTickHydrate() {
  await new Promise((r) => setTimeout(r, 0))
  hydrateMermaid()
  hydrateHighlight()
  emit('rendered', rootRef.value)
}

function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
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
    <div v-if="lightboxHtml" class="lightbox-svg-host" v-html="lightboxHtml" />
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
/* 画布跟随主题，与代码块同一层 surface。
   Mermaid 现在明暗各有一套配色，不再需要在暗色下强行刷浅底。 */
.mermaid-block {
  margin: 28px 0;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}
.mermaid-block__source {
  display: none; /* 保留在 DOM 里：主题切换时要靠它拿原始语句重渲染 */
}
.mermaid-svg-wrap {
  display: block;
  padding: 24px 20px;
  background: var(--color-bg-card);
  cursor: zoom-in;
  /* 超宽图（甘特图等）横向滚动，而不是被压缩到看不清 */
  overflow-x: auto;
}
/* Mermaid 输出的 <svg> 带 width="100%" 属性 + viewBox。若不显式约束 width，
   属性生效会把图表拉伸到整个正文宽度 —— 竖向流程图会被放大到几屏高
   (实测 439×860 的图被拉成 758×1486)。这里改用 viewBox 的固有尺寸，
   宽图由 max-width 收进正文，长图由 max-height 收进一屏，点击可放大看细节。 */
/* 尺寸由 sizeMermaidSvg() 按 viewBox 显式写在 style 上，这里只管居中 */
.mermaid-svg-wrap svg {
  display: block;
  margin: 0 auto;
}

/* ER 图的属性行底色被 Mermaid 写死在渲染器里（见其
   themes/erDiagram-oldHardcodedValues：#ffffff / #f2f2f2），themeVariables
   覆盖不掉，暗色下会在图中间烧出两条亮带。只能在 CSS 层按主题纠正。
   每个 row-rect 组里第一个 path 是底色，第二个是描边，故只改 first-child。 */
[data-theme='dark'] .mermaid-svg-wrap .row-rect-odd > path:first-child {
  fill: #1f1f1f;
}
[data-theme='dark'] .mermaid-svg-wrap .row-rect-even > path:first-child {
  fill: #262626;
}

/* ── Mermaid 错误的最小兜底样式（如果 Mermaid 渲染失败）────────── */
.mermaid-error {
  padding: 16px 20px;
  color: var(--color-danger);
  font-family: var(--font-mono, monospace);
  font-size: 12px;
}
</style>
