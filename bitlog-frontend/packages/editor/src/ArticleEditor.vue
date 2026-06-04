<script setup lang="ts">
import { watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { BubbleMenu } from '@tiptap/vue-3/menus'
import mermaid from 'mermaid'
import { createExtensions, normalizeMarkdown } from './core'
import './styles/prose.css'

const props = withDefaults(defineProps<{
  content: string
  editable?: boolean
  uploadImage?: (file: File) => Promise<string>
}>(), { editable: false })

const emit = defineEmits<{ change: []; error: [message: string] }>()

mermaid.initialize({ startOnLoad: false, theme: 'neutral' })

const editor = useEditor({
  editable: props.editable,
  content: normalizeMarkdown(props.content),
  contentType: 'markdown',
  extensions: createExtensions(),
  editorProps: {
    handlePaste(view, event) {
      if (!props.editable || !props.uploadImage) return false
      const files = event.clipboardData?.files
      if (!files?.length) return false
      const images = Array.from(files).filter(f => f.type.startsWith('image/'))
      if (!images.length) return false
      event.preventDefault()
      const { schema } = view.state
      images.forEach(async (file) => {
        try {
          const fileUrl = await props.uploadImage!(file)
          view.dispatch(view.state.tr.replaceSelectionWith(
            schema.nodes.image.create({ src: fileUrl, alt: file.name })
          ))
        } catch (e) {
          console.error('Image upload failed:', e)
          emit('error', `图片上传失败：${file.name}`)
        }
      })
      return true
    },
    handleDrop(view, event) {
      if (!props.editable || !props.uploadImage) return false
      const files = event.dataTransfer?.files
      if (!files?.length) return false
      const images = Array.from(files).filter(f => f.type.startsWith('image/'))
      if (!images.length) return false
      event.preventDefault()
      const { schema } = view.state
      const dropPos = view.posAtCoords({ left: event.clientX, top: event.clientY })?.pos
      images.forEach(async (file) => {
        try {
          const fileUrl = await props.uploadImage!(file)
          const insertAt = dropPos ?? view.state.doc.content.size
          view.dispatch(view.state.tr.insert(insertAt,
            schema.nodes.image.create({ src: fileUrl, alt: file.name })
          ))
        } catch (e) {
          console.error('Image upload failed:', e)
          emit('error', `图片上传失败：${file.name}`)
        }
      })
      return true
    },
  },
  onUpdate: () => emit('change'),
})

watch(() => props.content, (newContent) => {
  if (!editor.value) return
  if (editor.value.getMarkdown() === normalizeMarkdown(newContent)) return
  editor.value.commands.setContent(normalizeMarkdown(newContent), { contentType: 'markdown' })
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
