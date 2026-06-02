<script setup lang="ts">
import { ref } from 'vue'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import ImageLightbox from './ImageLightbox.vue'

const props = defineProps(nodeViewProps)
const lightboxOpen = ref(false)
</script>

<template>
  <node-view-wrapper as="figure" class="image-node-view" :class="{ 'image-node-view--editable': editor.isEditable, 'image-node-view--selected': selected }">
    <img
      :src="node.attrs.src"
      :alt="node.attrs.alt || ''"
      :title="node.attrs.title || undefined"
      class="image-node-view__img"
      draggable="false"
      @click="lightboxOpen = true"
    />
    <ImageLightbox
      :open="lightboxOpen"
      :src="node.attrs.src"
      :alt="node.attrs.alt"
      @close="lightboxOpen = false"
    />
  </node-view-wrapper>
</template>

<style>
.image-node-view {
  display: block;
  margin: 28px 0;
  position: relative;
}

.image-node-view__img {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 0 auto;
  border-radius: 4px;
  cursor: zoom-in;
  transition: opacity 0.15s;
}

.image-node-view__img:hover {
  opacity: 0.92;
}

.image-node-view--selected .image-node-view__img {
  outline: 2px solid var(--color-accent, #b85c38);
  outline-offset: 4px;
}
</style>
