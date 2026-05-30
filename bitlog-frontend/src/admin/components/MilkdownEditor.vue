<script setup lang="ts">
import { onMounted } from 'vue'
import { Milkdown, useEditor } from '@milkdown/vue'
import { Crepe, CrepeFeature } from '@milkdown/crepe'
import { $view } from '@milkdown/utils'
import { codeBlockSchema } from '@milkdown/preset-commonmark'
import { uploadConfig } from '@milkdown/kit/plugin/upload'
import type { Uploader } from '@milkdown/kit/plugin/upload'
import type { Node } from '@milkdown/kit/prose/model'
import hljs from 'highlight.js'
import mermaid from 'mermaid'
import { uploadFile } from '@/api/file'

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

function escapeHtml(s: string) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function attachCopy(btn: HTMLButtonElement, getText: () => string) {
  btn.addEventListener('click', async (e) => {
    e.stopPropagation()
    try {
      await navigator.clipboard.writeText(getText())
      btn.innerHTML = ICON_CHECK
      btn.style.color = '#1a7f37'
      setTimeout(() => { btn.innerHTML = ICON_COPY; btn.style.color = '' }, 2000)
    } catch {}
  })
}

function buildProseCodeWrap(lang: string, code: string, getText: () => string): HTMLElement {
  const escapedLang = lang || 'text'
  const highlighted =
    lang && hljs.getLanguage(lang)
      ? hljs.highlight(code, { language: lang, ignoreIllegals: true }).value
      : escapeHtml(code)

  const wrap = document.createElement('div')
  wrap.className = 'prose-code-wrap'

  const controls = document.createElement('div')
  controls.className = 'prose-code-controls'

  const dots = document.createElement('span')
  dots.className = 'prose-code-dots'
  dots.innerHTML = '<span></span><span></span><span></span>'

  const langEl = document.createElement('span')
  langEl.className = 'prose-code-lang'
  langEl.textContent = escapedLang

  const copyBtn = document.createElement('button')
  copyBtn.className = 'prose-code-copy'
  copyBtn.type = 'button'
  copyBtn.title = '复制代码'
  copyBtn.innerHTML = ICON_COPY
  attachCopy(copyBtn, getText)

  controls.append(dots, langEl, copyBtn)

  const outer = document.createElement('div')
  outer.className = 'prose-code-block-outer'

  const pre = document.createElement('pre')
  pre.className = 'prose-code-block'

  const codeEl = document.createElement('code')
  codeEl.className = `hljs language-${escapedLang}`
  codeEl.innerHTML = highlighted

  pre.appendChild(codeEl)
  outer.appendChild(pre)
  wrap.append(controls, outer)
  return wrap
}

let mermaidSeq = 0

mermaid.initialize({ startOnLoad: false, securityLevel: 'loose' })

async function renderMermaid(container: HTMLElement, code: string) {
  if (!code.trim()) {
    container.innerHTML = '<span class="mermaid-hint">开始输入 Mermaid 代码…</span>'
    return
  }
  try {
    const { svg } = await mermaid.render(`mme-${++mermaidSeq}`, code.trim())
    container.innerHTML = svg
  } catch (err) {
    console.error('[mermaid]', err)
    container.innerHTML = '<span class="mermaid-error">图表语法错误</span>'
  }
}

// Parse "```lang\ncode\n```" back into { lang, code }
function parseRaw(raw: string): { lang: string; code: string } {
  const lines = raw.split('\n')
  const langMatch = (lines[0] || '').match(/^```(.*)$/)
  const lang = langMatch ? langMatch[1].trim() : ''
  let closeIdx = lines.length - 1
  while (closeIdx > 0 && lines[closeIdx].trim() !== '```') closeIdx--
  const code = lines.slice(1, closeIdx > 0 ? closeIdx : undefined).join('\n')
  return { lang, code }
}

function autoResizeTextarea(ta: HTMLTextAreaElement) {
  ta.style.height = 'auto'
  ta.style.height = ta.scrollHeight + 'px'
}

const codeBlockPlugin = $view(codeBlockSchema.node, () => {
  return (node: any, view: any, getPos: any) => {
    let currentNode = node

    // Write lang + code back to ProseMirror document as a single transaction
    function dispatchUpdate(newLang: string, newCode: string) {
      const pos = typeof getPos === 'function' ? getPos() : undefined
      if (pos === undefined) return
      const { state } = view
      const nodeAtPos = state.doc.nodeAt(pos)
      if (!nodeAtPos) return
      if (newLang === nodeAtPos.attrs.language && newCode === nodeAtPos.textContent) return
      let tr = state.tr
      if (newLang !== nodeAtPos.attrs.language) {
        tr = tr.setNodeMarkup(pos, undefined, { ...nodeAtPos.attrs, language: newLang })
      }
      const mappedPos = tr.mapping.map(pos)
      const mappedNode = tr.doc.nodeAt(mappedPos)
      if (mappedNode && newCode !== mappedNode.textContent) {
        const start = mappedPos + 1
        const end = mappedPos + mappedNode.nodeSize - 1
        if (newCode) {
          tr = tr.replaceWith(start, end, state.schema.text(newCode))
        } else {
          tr = tr.delete(start, end)
        }
      }
      if (tr.docChanged) view.dispatch(tr)
    }

    function makeTextarea(): HTMLTextAreaElement {
      const ta = document.createElement('textarea')
      ta.className = 'me-raw-editor'
      ta.style.display = 'none'
      ta.spellcheck = false
      ta.setAttribute('wrap', 'off')
      return ta
    }

    // ── Mermaid block ─────────────────────────────────────────────────────
    if ((node.attrs.language ?? '') === 'mermaid') {
      const dom = document.createElement('div')
      dom.className = 'me-mermaid'

      const preview = document.createElement('div')
      preview.className = 'me-mermaid__preview'

      const textarea = makeTextarea()
      dom.append(preview, textarea)
      renderMermaid(preview, node.textContent)

      let isOpen = false

      const openSource = () => {
        isOpen = true
        preview.style.display = 'none'
        textarea.value = '```' + currentNode.attrs.language + '\n' + currentNode.textContent + '\n```'
        textarea.style.display = 'block'
        autoResizeTextarea(textarea)
        dom.classList.add('is-editing')
        requestAnimationFrame(() => textarea.focus())
      }

      const closeSource = () => {
        const { lang: newLang, code: newCode } = parseRaw(textarea.value)
        textarea.style.display = 'none'
        dom.classList.remove('is-editing')
        renderMermaid(preview, newCode)
        preview.style.display = ''
        dispatchUpdate(newLang, newCode)
        isOpen = false
      }

      const onDocClick = (e: MouseEvent) => {
        if (isOpen && !dom.contains(e.target as Node)) closeSource()
      }

      preview.addEventListener('click', () => { if (!isOpen) openSource() })
      textarea.addEventListener('input', () => autoResizeTextarea(textarea))
      textarea.addEventListener('keydown', (e) => {
        if (e.key === 'Tab') {
          e.preventDefault()
          const s = textarea.selectionStart
          textarea.value = textarea.value.slice(0, s) + '  ' + textarea.value.slice(textarea.selectionEnd)
          textarea.selectionStart = textarea.selectionEnd = s + 2
          autoResizeTextarea(textarea)
        } else if (e.key === 'Escape') {
          closeSource()
        }
      })
      document.addEventListener('click', onDocClick, true)

      return {
        dom,
        ignoreMutation: () => true,
        stopEvent(event: Event) {
          if (textarea.contains(event.target as Node)) return true
          return preview.contains(event.target as Node) &&
            (event.type === 'mousedown' || event.type === 'click')
        },
        update(updated: any) {
          if (updated.attrs.language !== 'mermaid') return false
          currentNode = updated
          if (!isOpen) renderMermaid(preview, updated.textContent)
          return true
        },
        destroy() {
          document.removeEventListener('click', onDocClick, true)
        },
      }
    }

    // ── Regular code block ────────────────────────────────────────────────
    const lang: string = node.attrs.language ?? ''
    const dom = document.createElement('div')
    dom.className = 'me-code'

    let previewWrap = buildProseCodeWrap(lang, node.textContent, () => currentNode.textContent)
    dom.appendChild(previewWrap)

    const editLayer = document.createElement('div')
    editLayer.className = 'me-code__edit'
    editLayer.style.display = 'none'

    const textarea = makeTextarea()
    editLayer.appendChild(textarea)
    dom.appendChild(editLayer)

    let isEditing = false

    const openEdit = () => {
      isEditing = true
      previewWrap.style.display = 'none'
      textarea.value = '```' + currentNode.attrs.language + '\n' + currentNode.textContent + '\n```'
      textarea.style.display = 'block'
      editLayer.style.display = 'block'
      autoResizeTextarea(textarea)
      dom.classList.add('is-editing')
      requestAnimationFrame(() => textarea.focus())
    }

    const closeEdit = () => {
      const { lang: newLang, code: newCode } = parseRaw(textarea.value)
      textarea.style.display = 'none'
      editLayer.style.display = 'none'
      dom.classList.remove('is-editing')
      refreshPreview(newLang, newCode)
      dispatchUpdate(newLang, newCode)
      isEditing = false
    }

    // Re-attached every time refreshPreview replaces the element
    const attachPreviewClick = () => {
      previewWrap.addEventListener('click', () => { if (!isEditing) openEdit() })
    }

    const refreshPreview = (newLang: string, newCode: string) => {
      const next = buildProseCodeWrap(newLang, newCode, () => newCode)
      dom.replaceChild(next, previewWrap)
      previewWrap = next
      attachPreviewClick()
    }

    attachPreviewClick()

    const onDocClick = (e: MouseEvent) => {
      if (isEditing && !dom.contains(e.target as Node)) closeEdit()
    }

    textarea.addEventListener('input', () => autoResizeTextarea(textarea))
    textarea.addEventListener('keydown', (e) => {
      if (e.key === 'Tab') {
        e.preventDefault()
        const s = textarea.selectionStart
        textarea.value = textarea.value.slice(0, s) + '  ' + textarea.value.slice(textarea.selectionEnd)
        textarea.selectionStart = textarea.selectionEnd = s + 2
        autoResizeTextarea(textarea)
      } else if (e.key === 'Escape') {
        closeEdit()
      }
    })
    document.addEventListener('click', onDocClick, true)

    return {
      dom,
      ignoreMutation: () => true,
      stopEvent(event: Event) {
        if (textarea.contains(event.target as Node)) return true
        return event.type === 'mousedown' || event.type === 'click'
      },
      update(updated: any) {
        currentNode = updated
        if (!isEditing) refreshPreview(updated.attrs.language ?? '', updated.textContent)
        return true
      },
      destroy() {
        document.removeEventListener('click', onDocClick, true)
      },
    }
  }
})

const emit = defineEmits<{ change: [] }>()

let crepeInstance: Crepe | null = null

const uploader = async (file: File) => {
  const res = await uploadFile(file, 'article')
  return res.fileUrl
}

const { get } = useEditor((root) => {
  const crepe = new Crepe({
    root,
    defaultValue: props.content,
    features: {
      [CrepeFeature.CodeMirror]: false,
      [CrepeFeature.ListItem]: false,
      [CrepeFeature.LinkTooltip]: false,
      [CrepeFeature.Latex]: false,
    },
    featureConfigs: {
      [CrepeFeature.ImageBlock]: { onUpload: uploader },
    },
  })

  crepe.editor.use(codeBlockPlugin)

  crepe.on(listener => listener.markdownUpdated(() => emit('change')))

  crepeInstance = crepe
  return crepe
})

function getMarkdown(): string {
  return crepeInstance?.getMarkdown() ?? ''
}

onMounted(() => {
  const editor = get()
  if (!editor) return

  const pasteUploader: Uploader = async (files, schema) => {
    const images: File[] = []
    for (let i = 0; i < files.length; i++) {
      const file = files.item(i)
      if (file && file.type.includes('image')) images.push(file)
    }
    // Prefer the block-level image node registered by CrepeFeature.ImageBlock;
    // fall back to the inline image node if it is not in the schema.
    const imageNodeType = schema.nodes.image_block ?? schema.nodes.imageBlock ?? schema.nodes.image
    const nodes: Node[] = await Promise.all(
      images.map(async (image) => {
        const src = await uploader(image)
        return imageNodeType.createAndFill({ src, alt: image.name }) as Node
      }),
    )
    return nodes
  }

  editor.config((ctx) => {
    ctx.set(uploadConfig, {
      uploader: pasteUploader,
      enableHtmlFileUploader: true,
    })
  })
})

defineExpose({ get, getMarkdown })
</script>

<template>
  <Milkdown />
</template>
