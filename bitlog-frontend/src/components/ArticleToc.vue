<script setup lang="ts">
/** 正文 h2 目录：宽屏常驻侧栏，窄屏使用可展开的阅读目录。 */
import { ref, onMounted, onBeforeUnmount, useTemplateRef } from 'vue'

interface TocItem {
  id: string
  text: string
}

const navRef = useTemplateRef<HTMLElement>('navRef')
const toggleRef = useTemplateRef<HTMLButtonElement>('toggleRef')
const items = ref<TocItem[]>([])
const activeId = ref('')
const mobileOpen = ref(false)

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

let scrollFrame: number | null = null

function onScroll() {
  headerOffset = readHeaderOffset()
  if (scrollFrame !== null) return
  scrollFrame = requestAnimationFrame(() => {
    scrollFrame = null
    updateActive()
  })
}

function closeMobileToc() {
  if (!mobileOpen.value) return
  mobileOpen.value = false
  toggleRef.value?.focus({ preventScroll: true })
}

function onItemClick(e: MouseEvent, item: TocItem) {
  e.preventDefault()
  mobileOpen.value = false
  const el = document.getElementById(item.id)
  if (!el) return
  headerOffset = readHeaderOffset()
  // 不写 location.hash：replaceState 会和 vue-router 的历史状态打架，
  // 而复制锚点链接的需求由标题自带的 .vditor-anchor 承担。
  const top = el.getBoundingClientRect().top + window.scrollY - headerOffset
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (!el.hasAttribute('tabindex')) el.setAttribute('tabindex', '-1')
  el.focus({ preventScroll: true })
  window.scrollTo({ top, behavior: reduced ? 'auto' : 'smooth' })
  activeId.value = item.id
}

onMounted(() => {
  headerOffset = readHeaderOffset()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  if (scrollFrame !== null) cancelAnimationFrame(scrollFrame)
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onScroll)
})

defineExpose({ build })
</script>

<template>
  <aside
    v-if="items.length > 1"
    class="toc"
    :class="{ 'toc--open': mobileOpen }"
    @keydown.esc="closeMobileToc"
  >
    <button
      ref="toggleRef"
      class="toc__toggle"
      :aria-expanded="mobileOpen"
      aria-controls="article-toc-panel"
      @click="mobileOpen = !mobileOpen"
    >
      <span>文章目录</span><span aria-hidden="true">{{ mobileOpen ? '−' : '+' }}</span>
    </button>
    <nav id="article-toc-panel" ref="navRef" class="toc__panel" aria-label="文章目录">
      <p class="toc__heading">本篇目录 <span>CONTENTS</span></p>
      <ul class="toc__list">
        <li v-for="(item, index) in items" :key="item.id">
          <a
            class="toc__link"
            :class="{ 'toc__link--active': item.id === activeId }"
            :href="`#${item.id}`"
            :aria-current="item.id === activeId ? 'location' : undefined"
            @click="onItemClick($event, item)"
          >
            <span class="toc__index" aria-hidden="true">{{
              String(index + 1).padStart(2, '0')
            }}</span>
            <span class="toc__text">{{ item.text }}</span>
          </a>
        </li>
      </ul>
    </nav>
  </aside>
</template>

<style scoped>
.toc {
  position: fixed;
  right: 24px;
  top: 176px;
  width: min(240px, calc(50vw - var(--spacing-prose) / 2 - 64px));
  z-index: 30;
}
.toc__toggle {
  display: none;
}
.toc__panel {
  max-height: calc(100dvh - 240px);
  overflow-y: auto;
  scrollbar-width: thin;
  padding-right: 8px;
}
.toc__heading {
  font-size: 13px;
  color: var(--color-text-primary);
  padding-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.toc__heading > span {
  font: 12px var(--font-mono);
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
}
.toc__list {
  display: flex;
  flex-direction: column;
  border-left: 1px solid var(--color-border);
}
.toc__link {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 9px 0 9px 14px;
  margin-left: -1px;
  border-left: 2px solid transparent;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.6;
  transition:
    color 0.2s,
    border-color 0.2s;
}
.toc__index {
  font: 11px var(--font-mono);
  opacity: 0.7;
  flex: none;
}
.toc__text {
  overflow-wrap: anywhere;
}
.toc__link:hover {
  color: var(--color-text-primary);
}
.toc__link--active {
  color: var(--color-accent);
  border-left-color: var(--color-accent);
}
@media (max-width: 1259px) {
  .toc {
    top: auto;
    bottom: 24px;
    left: 24px;
    right: auto;
    width: auto;
    max-width: calc(100vw - 88px);
    display: flex;
    flex-direction: column-reverse;
    border: 1px solid var(--color-border-strong);
    background: var(--color-bg-card);
  }
  .toc__toggle {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
    color: var(--color-text-primary);
    min-height: 44px;
    padding: 10px 16px;
    font-size: 13px;
  }
  .toc__panel {
    display: none;
  }
  .toc--open {
    width: 340px;
  }
  .toc--open .toc__panel {
    display: block;
    max-height: 55dvh;
    padding: 20px 16px 8px;
    border-bottom: 1px solid var(--color-border);
  }
  .toc__heading {
    display: none;
  }
  .toc__link {
    font-size: 14px;
    padding-top: 10px;
    padding-bottom: 10px;
  }
}
</style>
