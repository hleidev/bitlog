<script setup lang="ts">
import { FloatingMenu } from '@tiptap/vue-3/menus'
import type { Editor } from '@tiptap/core'

const props = defineProps<{ editor: Editor }>()
const emit = defineEmits<{ openLink: [] }>()

function openLink() {
  emit('openLink')
}

function insertTable() {
  props.editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}

// P → H1 → H2 → H3 → P cycle.  Same behavior as the bubble-menu button so the
// two menus never disagree about which heading a click produces.
function cycleHeading() {
  const { editor } = props
  if (editor.isActive('heading', { level: 1 })) {
    editor.chain().focus().toggleHeading({ level: 2 }).run()
  } else if (editor.isActive('heading', { level: 2 })) {
    editor.chain().focus().toggleHeading({ level: 3 }).run()
  } else if (editor.isActive('heading', { level: 3 })) {
    editor.chain().focus().setParagraph().run()
  } else {
    editor.chain().focus().toggleHeading({ level: 1 }).run()
  }
}
</script>

<template>
  <FloatingMenu
    :editor="editor"
    :options="{ placement: 'left', offset: 8, flip: true, shift: true }"
  >
    <div class="floating-toolbar">
      <button
        :class="{ 'is-active': editor.isActive('heading') }"
        title="标题（P → H1 → H2 → H3 → P）"
        @mousedown.prevent="cycleHeading"
      >H</button>

      <span class="toolbar-sep" />

      <button title="无序列表" @mousedown.prevent="editor.chain().focus().toggleBulletList().run()">•</button>
      <button title="有序列表" @mousedown.prevent="editor.chain().focus().toggleOrderedList().run()">1.</button>

      <span class="toolbar-sep" />

      <button title="引用" @mousedown.prevent="editor.chain().focus().toggleBlockquote().run()">❝</button>
      <button title="代码块" @mousedown.prevent="editor.chain().focus().toggleCodeBlock().run()">{}</button>
      <button title="水平线" @mousedown.prevent="editor.chain().focus().setHorizontalRule().run()">—</button>

      <span class="toolbar-sep" />

      <button title="链接" @mousedown.prevent="openLink">🔗</button>
      <button title="插入表格" @mousedown.prevent="insertTable">⊞</button>
    </div>
  </FloatingMenu>
</template>

<style scoped>
.floating-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  background: #1c1917;
  border: 1px solid #3a3632;
  border-radius: 6px;
  padding: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
}

.floating-toolbar button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 28px;
  padding: 0 6px;
  border: none;
  background: transparent;
  color: #c0b8b0;
  border-radius: 4px;
  cursor: pointer;
  font-family: inherit;
  font-size: 13px;
  transition: background 0.1s, color 0.1s;
}

.floating-toolbar button:hover { background: #3a3632; color: #f0ede8; }
.floating-toolbar button.is-active { background: var(--admin-accent, #b85c38); color: #fff; }

.toolbar-sep {
  width: 1px;
  height: 18px;
  background: #3a3632;
  margin: 0 4px;
  flex-shrink: 0;
}
</style>
