<script setup lang="ts">
import { ref } from 'vue'
import { MarkViewContent, markViewProps } from '@tiptap/vue-3'
import { useMarkViewCursor } from './composables/useMarkViewCursor'

const props = defineProps(markViewProps)
const rootEl = ref<HTMLElement | null>(null)
const { isCursorInside } = useMarkViewCursor(() => props, rootEl)

function handleLinkClick() {
  if (!props.editor.isEditable) {
    window.open(props.mark.attrs.href, '_blank', 'noopener,noreferrer')
  }
}
</script>

<template>
  <!-- Rendered mode: styled link via CSS. Source mode: [text](url) via ::before/::after. -->
  <span
    ref="rootEl"
    class="link-mark-view"
    :class="{ 'link-editing': isCursorInside }"
    :data-href="props.mark.attrs.href"
    @click="handleLinkClick"
  >
    <MarkViewContent as="span" class="link-content" />
  </span>
</template>
