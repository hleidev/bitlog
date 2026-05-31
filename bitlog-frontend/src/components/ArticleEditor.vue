<script setup lang="ts">
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import { Markdown } from '@tiptap/markdown'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import { createLowlight, common } from 'lowlight'
import { uploadFile } from '@/api/file'
import '@/assets/styles/prose.css'

const props = withDefaults(defineProps<{
  content: string
  editable?: boolean
}>(), { editable: false })

const emit = defineEmits<{ change: [] }>()

const lowlight = createLowlight(common)

const editor = useEditor({
  editable: props.editable,
  content: props.content,
  contentType: 'markdown',
  extensions: [
    StarterKit.configure({ codeBlock: false }),
    Image.configure({ allowBase64: false }),
    Markdown,
    CodeBlockLowlight.configure({ lowlight }),
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

<template>
  <EditorContent
    :editor="editor"
    class="article-editor"
    :class="{ 'is-editable': editable }"
  />
</template>
