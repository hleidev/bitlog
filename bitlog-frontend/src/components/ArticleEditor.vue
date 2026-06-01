<script setup lang="ts">
import { useEditor, EditorContent, VueNodeViewRenderer } from '@tiptap/vue-3'
import { BubbleMenu } from '@tiptap/vue-3/menus'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import { Markdown } from '@tiptap/markdown'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import { createLowlight, common } from 'lowlight'
import { uploadFile } from '@/api/file'
import mermaid from 'mermaid'
import CodeBlockView from './CodeBlockView.vue'
import '@/assets/styles/prose.css'

const props = withDefaults(defineProps<{
  content: string
  editable?: boolean
}>(), { editable: false })

const emit = defineEmits<{ change: [] }>()

mermaid.initialize({ startOnLoad: false, theme: 'neutral' })

const lowlight = createLowlight(common)

const editor = useEditor({
  editable: props.editable,
  content: props.content,
  contentType: 'markdown',
  extensions: [
    StarterKit.configure({ codeBlock: false }),
    Image.configure({ allowBase64: false }),
    Markdown,
    CodeBlockLowlight
      .extend({ addNodeView() { return VueNodeViewRenderer(CodeBlockView) } })
      .configure({ lowlight }),
  ],
  editorProps: {
    handlePaste(view, event) {
      if (!props.editable) return false
      const files = event.clipboardData?.files
      if (!files?.length) return false
      const images = Array.from(files).filter(f => f.type.startsWith('image/'))
      if (!images.length) return false
      event.preventDefault()
      images.forEach(async (file) => {
        try {
          const { fileUrl } = await uploadFile(file, 'article')
          view.dispatch(view.state.tr.replaceSelectionWith(
            view.state.schema.nodes.image.create({ src: fileUrl, alt: file.name })
          ))
        } catch {}
      })
      return true
    },
    handleDrop(view, event) {
      if (!props.editable) return false
      const files = event.dataTransfer?.files
      if (!files?.length) return false
      const images = Array.from(files).filter(f => f.type.startsWith('image/'))
      if (!images.length) return false
      event.preventDefault()
      images.forEach(async (file) => {
        try {
          const { fileUrl } = await uploadFile(file, 'article')
          const pos = view.posAtCoords({ left: event.clientX, top: event.clientY })?.pos ?? view.state.doc.content.size
          view.dispatch(view.state.tr.insert(pos,
            view.state.schema.nodes.image.create({ src: fileUrl, alt: file.name })
          ))
        } catch {}
      })
      return true
    },
  },
  onUpdate: () => emit('change'),
})

function getMarkdown(): string {
  return editor.value?.getMarkdown() ?? ''
}

defineExpose({ getMarkdown })
</script>

<style scoped>
/* ── BubbleMenu ───────────────────────────────────────────────────────────── */
:deep(.tippy-box) { background: transparent !important; box-shadow: none !important; }

.bubble-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  background: #1c1917;
  border: 1px solid #3a3632;
  border-radius: 6px;
  padding: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
}

.bubble-toolbar button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 28px;
  border: none;
  background: transparent;
  color: #c0b8b0;
  border-radius: 4px;
  cursor: pointer;
  font-family: inherit;
  font-size: 13px;
  transition: background 0.1s, color 0.1s;
}

.bubble-toolbar button:hover { background: #3a3632; color: #f0ede8; }
.bubble-toolbar button.is-active { background: var(--admin-accent, #b85c38); color: #fff; }
.bubble-toolbar button code { font-family: var(--font-mono); font-size: 13px; }
</style>

<template>
  <div class="article-editor" :class="{ 'is-editable': editable }">
    <BubbleMenu
      v-if="editable && editor"
      :editor="editor"
      :tippy-options="{ duration: 100, placement: 'top' }"
    >
      <div class="bubble-toolbar">
        <button
          :class="{ 'is-active': editor.isActive('bold') }"
          title="粗体 ⌘B"
          @mousedown.prevent="editor.chain().focus().toggleBold().run()"
        ><strong>B</strong></button>
        <button
          :class="{ 'is-active': editor.isActive('italic') }"
          title="斜体 ⌘I"
          @mousedown.prevent="editor.chain().focus().toggleItalic().run()"
        ><em>I</em></button>
        <button
          :class="{ 'is-active': editor.isActive('strike') }"
          title="删除线"
          @mousedown.prevent="editor.chain().focus().toggleStrike().run()"
        ><s>S</s></button>
        <button
          :class="{ 'is-active': editor.isActive('code') }"
          title="行内代码 ⌘⇧C"
          @mousedown.prevent="editor.chain().focus().toggleCode().run()"
        ><code>`</code></button>
      </div>
    </BubbleMenu>
    <EditorContent :editor="editor" />
  </div>
</template>
