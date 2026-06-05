<script setup lang="ts">
import { BubbleMenu } from '@tiptap/vue-3/menus'
import type { Editor } from '@tiptap/core'

const props = defineProps<{ editor: Editor }>()
const emit = defineEmits<{ openLink: [] }>()

function openLink() {
  emit('openLink')
}

// P → H1 → H2 → H3 → P cycle.  Label reflects the current heading level so
// the user can see what the next click will produce by inverting the
// "active = next" rule of thumb.
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

const headingLabel = () => {
  if (props.editor.isActive('heading', { level: 1 })) return 'H1'
  if (props.editor.isActive('heading', { level: 2 })) return 'H2'
  if (props.editor.isActive('heading', { level: 3 })) return 'H3'
  return 'H'
}
</script>

<template>
  <BubbleMenu
    :editor="editor"
    :options="{ placement: 'top', offset: 8, flip: true, shift: true }"
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

      <span class="toolbar-sep" />

      <button
        :class="{ 'is-active': editor.isActive('heading') }"
        title="标题（P → H1 → H2 → H3 → P）"
        @mousedown.prevent="cycleHeading"
      >{{ headingLabel() }}</button>
      <button
        :class="{ 'is-active': editor.isActive('link') }"
        title="链接"
        @mousedown.prevent="openLink"
      >🔗</button>
    </div>
  </BubbleMenu>
</template>

<style scoped>
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

.bubble-toolbar button:hover { background: #3a3632; color: #f0ede8; }
.bubble-toolbar button.is-active { background: var(--admin-accent, #b85c38); color: #fff; }
.bubble-toolbar button code { font-family: var(--font-mono); font-size: 13px; }

.toolbar-sep {
  width: 1px;
  height: 18px;
  background: #3a3632;
  margin: 0 4px;
  flex-shrink: 0;
}
</style>
