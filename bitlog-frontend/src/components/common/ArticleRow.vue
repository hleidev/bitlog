<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { type ArticleItemVO } from '@/api/article'
import { prefetchArticleDetail } from '@/api/articleCache'

defineProps<{ article: ArticleItemVO }>()

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}`
}
</script>

<template>
  <RouterLink
    :to="`/article/${article.id}`"
    class="article-row"
    @mouseenter="prefetchArticleDetail(article.id)"
    @focus="prefetchArticleDetail(article.id)"
  >
    <div class="article-date">{{ formatDate(article.publishTime) }}</div>
    <div class="article-body">
      <span class="article-title">{{ article.title }}</span>
      <span v-if="article.summary" class="article-excerpt">{{ article.summary }}</span>
    </div>
    <div class="article-right">
      <span class="article-tag">{{ article.category?.name || article.tags[0]?.name || '' }}</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.article-row {
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 28px 10px;
  margin: 0 -10px;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  transition: background var(--transition-base);
}

.article-row::before {
  counter-increment: article-counter;
  content: counter(article-counter, decimal-leading-zero);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-faint);
  letter-spacing: 0.06em;
  flex-shrink: 0;
  width: 24px;
}

.article-row.is-visible {
  animation: rowFadeUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.article-row:hover {
  background: var(--color-bg-hover);
}

@keyframes rowFadeUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.article-date {
  width: 80px;
  flex-shrink: 0;
  font-size: 11px;
  color: var(--color-text-muted);
  letter-spacing: 0.03em;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  transition: color var(--transition-base);
}

.article-body {
  flex: 1;
  min-width: 0;
  max-width: 680px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.article-title {
  font-family: var(--font-serif);
  font-size: 17.5px;
  font-weight: 400;
  color: var(--color-text-primary);
  line-height: 1.55;
  letter-spacing: 0.01em;
  width: fit-content;
  max-width: 100%;
  background-image: linear-gradient(var(--color-accent), var(--color-accent));
  background-repeat: no-repeat;
  background-size: 0% 1px;
  background-position: left bottom;
  padding-bottom: 1px;
  transition: background-size var(--transition-sweep);
}

.article-row:hover .article-title {
  background-size: 100% 1px;
}

.article-excerpt {
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.8;
  letter-spacing: 0.01em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-right {
  margin-left: auto;
  flex-shrink: 0;
  padding-top: 3px;
}

.article-tag {
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  transition: color var(--transition-base);
}

.article-row:hover .article-tag {
  color: var(--color-accent);
}

.article-row:hover .article-date {
  color: var(--color-text-secondary);
}

@media (max-width: 768px) {
  .article-row {
    gap: 20px;
  }

  .article-right {
    display: none;
  }
}

@media (max-width: 640px) {
  .article-date {
    display: none;
  }
}
</style>
