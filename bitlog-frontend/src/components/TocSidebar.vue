<script setup lang="ts">
import type { TocItem } from '@/composables/useToc'

defineProps<{
  items: TocItem[]
  activeSection: string
}>()

defineEmits<{ scrollTo: [id: string] }>()
</script>

<template>
  <div class="toc-card">
    <h4 class="toc-title">目录</h4>
    <nav class="toc-nav">
      <a
        v-for="item in items"
        :key="item.id"
        class="toc-item"
        :class="[`toc-item--h${item.level}`, { 'toc-item--active': activeSection === item.id }]"
        href="#"
        @click.prevent="$emit('scrollTo', item.id)"
      >{{ item.text }}</a>
    </nav>
  </div>
</template>

<style scoped>
.toc-card {
  display: flex;
  flex-direction: column;
  max-height: inherit;
  overflow: hidden;
}

.toc-title {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--color-text-faint);
  padding: 0 10px 12px;
  flex-shrink: 0;
}

.toc-nav {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 0 0 12px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--color-border) transparent;
}

.toc-nav::-webkit-scrollbar { width: 2px; }
.toc-nav::-webkit-scrollbar-track { background: transparent; }
.toc-nav::-webkit-scrollbar-thumb { background: var(--color-border); }

.toc-item {
  display: block;
  font-size: 12.5px;
  line-height: 1.45;
  color: var(--color-text-muted);
  padding: 5px 10px;
  transition: color var(--transition-base);
  border-left: 2px solid transparent;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toc-item--h3 {
  padding-left: 20px;
  font-size: 12px;
  color: var(--color-text-faint);
}

.toc-item:hover { color: var(--color-text-secondary); }

.toc-item--active {
  color: var(--color-accent);
  border-left-color: var(--color-accent);
  font-weight: 500;
}
</style>
