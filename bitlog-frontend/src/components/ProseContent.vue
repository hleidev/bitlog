<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import mermaid from 'mermaid'
import '@/assets/styles/prose.css'

const props = defineProps<{ content: string }>()

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

md.renderer.rules.fence = (tokens, idx) => {
  const token = tokens[idx]
  const info = token.info ? token.info.trim() : ''
  const lang = info ? info.split(/\s+/g)[0] : ''
  const str = token.content
  const escapedLang = md.utils.escapeHtml(lang || 'text')

  if (lang === MERMAID_CLASS) {
    return '<pre class="' + MERMAID_CLASS + '">' + md.utils.escapeHtml(str) + '</pre>\n'
  }

  const highlighted =
    lang && hljs.getLanguage(lang)
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

const renderedContent = computed(() => DOMPurify.sanitize(md.render(props.content)))

const getMermaidTheme = () =>
  document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'default'

const renderMermaid = async () => {
  const els = document.querySelectorAll<HTMLElement>('.prose .' + MERMAID_CLASS)
  if (!els.length) return
  mermaid.initialize({ startOnLoad: false, securityLevel: 'loose', theme: getMermaidTheme() })
  els.forEach((el) => {
    if (!el.dataset.mermaidSrc) {
      el.dataset.mermaidSrc = el.textContent ?? ''
    } else {
      el.textContent = el.dataset.mermaidSrc
    }
    el.removeAttribute('data-processed')
  })
  await mermaid.run({ nodes: Array.from(els), suppressErrors: true })
}

const handleClick = async (e: MouseEvent) => {
  const target = e.target as Element

  const btn = target.closest<HTMLButtonElement>('.prose-code-copy')
  if (btn) {
    const code = btn.closest('.prose-code-wrap')?.querySelector('.prose-code-block code')
    if (!code) return
    try {
      await navigator.clipboard.writeText(code.textContent ?? '')
      btn.innerHTML = ICON_CHECK
      btn.style.color = '#1a7f37'
      setTimeout(() => { btn.innerHTML = ICON_COPY; btn.style.color = '' }, 2000)
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

const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') fullscreenVisible.value = false
}

watch(fullscreenVisible, (val) => {
  if (val) window.addEventListener('keydown', onKeydown)
  else window.removeEventListener('keydown', onKeydown)
})

const themeObserver = new MutationObserver(() => renderMermaid())

onMounted(async () => {
  await nextTick()
  await renderMermaid()
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
})

onUnmounted(() => themeObserver.disconnect())

defineExpose({ proseRef })
</script>

<template>
  <div class="prose" ref="proseRef" v-html="renderedContent" @click="handleClick" />

  <Teleport to="body">
    <Transition name="code-fs">
      <div
        v-if="fullscreenVisible"
        class="code-fs-overlay"
        @click.self="fullscreenVisible = false"
      >
        <div class="code-fs-panel">
          <div class="code-fs-header">
            <span class="code-fs-dots">
              <span class="code-fs-dot code-fs-dot--red" title="关闭" @click="fullscreenVisible = false" />
              <span class="code-fs-dot code-fs-dot--yellow" />
              <span class="code-fs-dot code-fs-dot--green" />
            </span>
            <span class="code-fs-lang">{{ fullscreenLang }}</span>
            <button class="code-fs-close" title="关闭" @click="fullscreenVisible = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
          <div class="code-fs-body">
            <pre class="code-fs-pre"><code class="hljs" v-html="fullscreenHighlighted" /></pre>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
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
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.code-fs-dot:hover { filter: brightness(0.85); }

.code-fs-dot--red    { background: radial-gradient(circle at 38% 35%, #ff8a80, #ff5f57); }
.code-fs-dot--yellow { background: radial-gradient(circle at 38% 35%, #ffe57f, #febc2e); cursor: default; }
.code-fs-dot--green  { background: radial-gradient(circle at 38% 35%, #69f0ae, #28c840); cursor: default; }

.code-fs-dot--red::after {
  content: '';
  position: absolute;
  width: 7px; height: 7px;
  background-color: rgba(0, 0, 0, 0.5);
  mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M2.5 2.5l5 5M7.5 2.5l-5 5' stroke='black' stroke-width='1.6' stroke-linecap='round'/%3E%3C/svg%3E");
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M2.5 2.5l5 5M7.5 2.5l-5 5' stroke='black' stroke-width='1.6' stroke-linecap='round'/%3E%3C/svg%3E");
  mask-size: contain; mask-repeat: no-repeat; mask-position: center;
  -webkit-mask-size: contain; -webkit-mask-repeat: no-repeat; -webkit-mask-position: center;
  opacity: 0; transition: opacity 0.12s;
}
.code-fs-dot--red:hover::after { opacity: 1; }

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
  width: 26px; height: 26px;
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
