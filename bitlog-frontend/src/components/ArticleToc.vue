<script setup lang="ts">
/**
 * ArticleToc — 正文右侧的刻度式目录（minimap TOC）
 *
 * 只收 h2：一级章节就是文章骨架，更深的层级留给正文自己表达。折叠态画一组
 * 等长刻度线作为结构缩略图；hover / 键盘聚焦时容器横向展开，标题文字淡入。
 *
 * 锚在窗口右缘（Notion / ChatGPT 一类浮动目录的通行做法，fixed + right），
 * 刻度始终贴边不动，展开时面板向左长进正文与窗口之间的留白。
 * 留白足够时不会压到正文，所以不需要背景板和边框，与全站无阴影的设计一致；
 * 留白不足以容纳展开态时整体隐藏，见文末 media query。
 *
 * 注意与文档站（Vercel / Stripe / MDN）那种 TOC 不是一个模式：那种是网格里
 * 常驻展开的 sticky 列，占布局空间；这里是浮动折叠的缩略图，不占位。
 *
 * 锚点不自建一套：Lute 已开 SetHeadingID（见 markdown-render-config.ts），
 * 渲染出来的标题自带 id，这里直接复用，只对缺失 / 重名的情况兜底补写。
 *
 * 内容是 Lute WASM 异步渲染的，挂载时 DOM 还是空的，所以不在 onMounted
 * 里扫描，改由父组件在 ArticleContent 的 rendered 事件里调 build()。
 */
import { ref, onMounted, onBeforeUnmount, useTemplateRef } from 'vue'

interface TocItem {
  id: string
  text: string
}

const navRef = useTemplateRef<HTMLElement>('navRef')
const items = ref<TocItem[]>([])
const activeId = ref('')

/** 与 items 一一对应的标题元素，只用于读取位置，不需要响应式 */
let headingEls: HTMLElement[] = []

/** 判定线：滚过固定顶栏再多留一点余量，才算进入该章节 */
let headerOffset = 0

function readHeaderOffset(): number {
  const raw = getComputedStyle(document.documentElement).getPropertyValue('--spacing-header-height')
  return (parseFloat(raw) || 0) + 24
}

/** Lute 的 SetHeadingAnchor 会往标题里塞一个 <a class="vditor-anchor">，取文本时要跳过 */
function headingText(el: HTMLElement): string {
  let text = ''
  el.childNodes.forEach((node) => {
    const isAnchor =
      node.nodeType === Node.ELEMENT_NODE &&
      (node as HTMLElement).classList.contains('vditor-anchor')
    if (!isAnchor) text += node.textContent ?? ''
  })
  return text.trim()
}

function slugify(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-+|-+$/g, '')
}

function uniqueId(base: string, used: Set<string>): string {
  const seed = base || 'section'
  if (!used.has(seed)) return seed
  let n = 2
  while (used.has(`${seed}-${n}`)) n++
  return `${seed}-${n}`
}

/** 扫描正文，重建目录。父组件在每次正文渲染完成后调用。 */
function build(root: HTMLElement | null) {
  headingEls = []
  items.value = []
  activeId.value = ''
  if (!root) return

  headerOffset = readHeaderOffset()

  const used = new Set<string>()
  const list: TocItem[] = []
  for (const el of Array.from(root.querySelectorAll<HTMLElement>('h2'))) {
    const text = headingText(el)
    if (!text) continue
    const id = uniqueId(el.id || slugify(text), used)
    used.add(id)
    if (el.id !== id) el.id = id
    list.push({ id, text })
    headingEls.push(el)
  }
  items.value = list
  updateActive()
}

function updateActive() {
  if (!headingEls.length) return
  let index = 0
  for (let i = 0; i < headingEls.length; i++) {
    if (headingEls[i].getBoundingClientRect().top > headerOffset) break
    index = i
  }
  // 末章太短时永远越不过判定线，滚到底就强制点亮最后一条
  const doc = document.documentElement
  if (doc.scrollHeight - doc.scrollTop - doc.clientHeight < 4) {
    index = headingEls.length - 1
  }
  if (items.value[index].id === activeId.value) return
  activeId.value = items.value[index].id
  syncRailScroll(index)
}

/**
 * 标题多到装不下时轨道自身会滚动，高亮跑出可视区就跟着挪一下。
 * 用户正把鼠标停在展开的列表上时不打扰他。
 */
function syncRailScroll(index: number) {
  const nav = navRef.value
  if (!nav || nav.scrollHeight <= nav.clientHeight || nav.matches(':hover')) return
  const link = nav.querySelectorAll<HTMLElement>('.toc__link')[index]
  if (!link) return
  const target = link.offsetTop - nav.clientHeight / 2 + link.offsetHeight / 2
  nav.scrollTop = Math.max(0, Math.min(target, nav.scrollHeight - nav.clientHeight))
}

let ticking = false

function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    ticking = false
    updateActive()
  })
}

function onItemClick(e: MouseEvent, item: TocItem) {
  e.preventDefault()
  const el = document.getElementById(item.id)
  if (!el) return
  // 不写 location.hash：replaceState 会和 vue-router 的历史状态打架，
  // 而复制锚点链接的需求由标题自带的 .vditor-anchor 承担。
  const top = el.getBoundingClientRect().top + window.scrollY - headerOffset
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  window.scrollTo({ top, behavior: reduced ? 'auto' : 'smooth' })
  activeId.value = item.id
}

onMounted(() => {
  headerOffset = readHeaderOffset()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onScroll)
})

defineExpose({ build })
</script>

<template>
  <!-- 只有一个标题的文章不值得占一条轨道 -->
  <nav v-if="items.length > 1" ref="navRef" class="toc" aria-label="文章目录">
    <ul class="toc__list">
      <li v-for="item in items" :key="item.id">
        <a
          class="toc__link"
          :class="{ 'toc__link--active': item.id === activeId }"
          :href="`#${item.id}`"
          @click="onItemClick($event, item)"
        >
          <span class="toc__text">{{ item.text }}</span>
          <span class="toc__tick" aria-hidden="true" />
        </a>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
.toc {
  /* 轨道尺寸集中在这几个值上，文末断点由它们推导 */
  --toc-rail: 18px;
  /* 贴窗口右缘的距离，与 Notion 的 right: 1rem 同量级 */
  --toc-edge: 16px;
  /* 展开态左缘与正文右缘之间至少留这么多，免得两栏糊在一起 */
  --toc-gap: 32px;
  /* 热区要比刻度宽得多：刻度只有十几像素，不放大很难 hover 中 */
  --toc-hit: 14px;
  /* 展开态的宽度上限 = 正文右缘到窗口右缘之间的全部留白。
     不写死一个数：宽屏上中文标题基本不会被截断，窄屏自动收窄。 */
  --toc-room: calc(50vw - var(--spacing-prose) / 2 - var(--toc-gap) - var(--toc-edge));

  position: fixed;
  top: 50%;
  /* 锚窗口右缘：刻度贴边不动，展开时面板向左长 */
  right: var(--toc-edge);
  transform: translateY(-50%);
  z-index: 20;

  /* 宽度由内容撑、由留白封顶：短标题的文章不会展开出一块空面板 */
  width: max-content;
  max-width: calc(var(--toc-rail) + var(--toc-hit) * 2);
  max-height: 72vh;
  padding: var(--toc-hit);
  /* 折叠态靠横向裁切藏住文字；纵向留滚动，长文目录不至于被闷掉 */
  overflow: hidden auto;
  scrollbar-width: none;
  transition: max-width var(--transition-base);
}

.toc::-webkit-scrollbar {
  display: none;
}

.toc:hover,
.toc:focus-within {
  max-width: var(--toc-room);
}

.toc__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.toc__link {
  display: flex;
  align-items: center;
  /* 刻度是最后一个 flex item + 靠右对齐 → 它永远贴着轨道右缘，
     展开时不会跟着面板一起位移。文字则向左溢出，被容器裁掉。 */
  justify-content: flex-end;
  gap: 10px;
  text-decoration: none;
  color: inherit;
}

.toc__link:focus-visible {
  outline: 1px solid var(--color-accent);
  outline-offset: 3px;
}

.toc__tick {
  flex: none;
  width: var(--toc-rail);
  height: 2px;
  background: var(--color-text-faint);
  transition: background-color var(--transition-base);
}

.toc__link:hover .toc__tick {
  background: var(--color-text-secondary);
}

.toc__link--active .toc__tick {
  background: var(--color-accent);
}

.toc__text {
  /* 不许压缩：压缩会让折叠态挤出一个只剩省略号的小尾巴，
     必须让它整条溢出到左边由容器裁掉。 */
  flex: none;
  font-family: var(--font-sans);
  font-size: 12px;
  line-height: 1.5;
  letter-spacing: 0.01em;
  color: var(--color-text-muted);
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  /* 省略号要在文字自身上生效，容器裁切只会硬切在字中间 */
  max-width: calc(var(--toc-room) - var(--toc-hit) * 2 - var(--toc-rail) - 10px);
  opacity: 0;
  transform: translateX(4px);
  transition:
    opacity var(--transition-base),
    transform var(--transition-base),
    color var(--transition-base);
}

.toc:hover .toc__text,
.toc:focus-within .toc__text {
  opacity: 1;
  transform: none;
}

.toc__link:hover .toc__text {
  color: var(--color-text-primary);
}

.toc__link--active .toc__text {
  color: var(--color-accent);
}

@media (prefers-reduced-motion: reduce) {
  .toc,
  .toc__tick,
  .toc__text {
    transition: none;
  }
}

/* 留白不够就整体隐藏：--toc-room 在视口 1260px 时约剩 182px，
   再窄下去展开态就只能压到正文上了。 */
@media (max-width: 1259px) {
  .toc {
    display: none;
  }
}
</style>
