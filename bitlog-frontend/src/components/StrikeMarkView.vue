<script setup lang="ts">
import { ref } from 'vue'
import { MarkViewContent, markViewProps } from '@tiptap/vue-3'
import { useMarkViewCursor } from '@/composables/useMarkViewCursor'

const props = defineProps(markViewProps)
const rootEl = ref<HTMLElement | null>(null)
const { isCursorInside } = useMarkViewCursor(() => props, rootEl)
</script>

<template>
  <span ref="rootEl" class="strike-mark-view" :class="{ 'mark-source': isCursorInside }">
    <MarkViewContent as="s" />
  </span>
</template>
