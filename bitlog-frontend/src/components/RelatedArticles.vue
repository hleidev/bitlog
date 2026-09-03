<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { type ArticleLink } from '@/api/article'
import { prefetchArticleDetail } from '@/api/articleCache'
import { formatYearMonth } from '@/utils/format'

defineProps<{ articles: ArticleLink[] }>()
</script>

<template>
  <nav v-if="articles.length" class="related" aria-label="继续阅读">
    <div class="section-header">
      <span class="section-label">继续阅读</span>
      <div class="section-rule"></div>
    </div>

    <RouterLink
      v-for="item in articles"
      :key="item.id"
      :to="`/article/${item.id}`"
      class="related-row"
      @mouseenter="prefetchArticleDetail(item.id)"
      @focus="prefetchArticleDetail(item.id)"
    >
      <span class="related-title">{{ item.title }}</span>
      <span class="related-date">{{ formatYearMonth(item.publishTime) }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.related {
  margin-top: 56px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 8px;
}

.section-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.section-rule {
  flex: 1;
  height: 1px;
  background: var(--color-border);
}

.related-row {
  display: flex;
  align-items: baseline;
  gap: 24px;
  padding: 16px 10px;
  margin: 0 -10px;
  border-bottom: 1px solid var(--color-border-light);
  transition: background var(--transition-base);
}

.related-row:hover {
  background: var(--color-bg-hover);
}

.related-title {
  font-family: var(--font-serif);
  font-size: 15.5px;
  line-height: 1.5;
  color: var(--color-text-primary);
  width: fit-content;
  max-width: 100%;
  background-image: linear-gradient(var(--color-accent), var(--color-accent));
  background-repeat: no-repeat;
  background-size: 0% 1px;
  background-position: left bottom;
  padding-bottom: 1px;
  transition: background-size var(--transition-sweep);
}

.related-row:hover .related-title {
  background-size: 100% 1px;
}

.related-date {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 11px;
  color: var(--color-text-muted);
  letter-spacing: 0.03em;
  font-variant-numeric: tabular-nums;
  transition: color var(--transition-base);
}

.related-row:hover .related-date {
  color: var(--color-text-secondary);
}

@media (max-width: 640px) {
  .related-date {
    display: none;
  }
}
</style>
