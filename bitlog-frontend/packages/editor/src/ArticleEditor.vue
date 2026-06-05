<script setup lang="ts">
import { ref, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import mermaid from 'mermaid'
import { createExtensions, normalizeMarkdown } from './core'
import EditorBubbleMenu from './components/EditorBubbleMenu.vue'
import EditorFloatingMenu from './components/EditorFloatingMenu.vue'
import LinkEditorModal from './components/LinkEditorModal.vue'
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
      if (!props.editable) return false
      const files = event.clipboardData?.files
      if (files?.length) {
        const images = Array.from(files).filter(f => f.type.startsWith('image/'))
        if (images.length) {
          if (!props.uploadImage) return false
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
        }
      }
      const text = event.clipboardData?.getData('text/plain')
      if (text && /\|/.test(text)) {
        event.preventDefault()
        editor.value?.commands.insertContent(normalizeMarkdown(text), { contentType: 'markdown' })
        return true
      }
      return false
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

// ── Link modal state ──────────────────────────────────────────────────────────
const linkModalOpen = ref(false)
const linkModalInitialHref = ref<string | null>(null)

function openLinkModal() {
  if (!editor.value) return
  linkModalInitialHref.value = editor.value.isActive('link')
    ? (editor.value.getAttributes('link').href as string | null) ?? null
    : null
  linkModalOpen.value = true
}

function applyLink(url: string | null) {
  if (!editor.value) return
  if (url === null) {
    editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
  } else {
    editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
  }
  linkModalOpen.value = false
}

function closeLinkModal() {
  linkModalOpen.value = false
}

function getMarkdown(): string {
  return editor.value?.getMarkdown() ?? ''
}

defineExpose({ getMarkdown })
</script>

<style scoped>
/* Reset the floating-ui wrapper that surrounds the menu. Tiptap v3 uses
   @floating-ui/dom instead of tippy, but the wrapper element still needs its
   default background cleared so the dark toolbar reads correctly. */
:deep(.tippy-box),
:deep([data-floating-ui-portal]) {
  background: transparent !important;
  box-shadow: none !important;
}
</style>

<template>
  <div class="article-editor" :class="{ 'is-editable': editable }">
    <EditorBubbleMenu v-if="editable && editor" :editor="editor" @open-link="openLinkModal" />
    <EditorFloatingMenu v-if="editable && editor" :editor="editor" @open-link="openLinkModal" />
    <EditorContent :editor="editor" />
    <LinkEditorModal
      :open="linkModalOpen"
      :initial-href="linkModalInitialHref"
      @apply="applyLink"
      @close="closeLinkModal"
    />
  </div>
</template>
