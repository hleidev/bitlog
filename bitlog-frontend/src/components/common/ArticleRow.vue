<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { type ArticleItemVO } from '@/api/article'
import { prefetchArticleDetail } from '@/api/articleCache'
import { formatYearMonth } from '@/utils/format'

defineProps<{ article: ArticleItemVO }>()
</script>

<template>
  <RouterLink
    :to="`/article/${article.id}`"
    class="article-row"
    @mouseenter="prefetchArticleDetail(article.id)"
    @focus="prefetchArticleDetail(article.id)"
  >
    <div class="article-meta">
      <time :datetime="article.publishTime">{{ formatYearMonth(article.publishTime) }}</time>
      <span v-if="article.category || article.tags.length" class="article-tag">{{
        article.category?.name || article.tags[0]?.name
      }}</span>
    </div>
    <div class="article-body">
      <h3 class="article-title">{{ article.title }}</h3>
      <p v-if="article.summary" class="article-excerpt">{{ article.summary }}</p>
    </div>
    <span class="article-arrow" aria-hidden="true">↗</span>
  </RouterLink>
</template>

<style scoped>
.article-row {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr) 28px;
  gap: 24px;
  padding: 28px 0;
  border-bottom: 1px solid var(--color-border);
  align-items: start;
}
.article-row:first-child {
  padding-top: 0;
}
.article-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 9px;
  padding-top: 5px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.article-meta time {
  font-family: var(--font-mono);
  white-space: nowrap;
}
.article-tag {
  border: 1px solid var(--color-border);
  padding: 1px 7px;
  font-size: 12px;
}
.article-body {
  min-width: 0;
}
.article-title {
  font-family: var(--font-display);
  font-size: 23px;
  font-weight: 600;
  line-height: 1.6;
  letter-spacing: -0.015em;
  text-wrap: pretty;
  transition: color 0.2s;
}
.article-excerpt {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.9;
  margin-top: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.article-arrow {
  margin-top: 3px;
  font-size: 22px;
  color: var(--color-text-muted);
  transition:
    transform 0.2s,
    color 0.2s;
}
.article-row:hover .article-title {
  color: var(--color-accent);
}
.article-row:hover .article-arrow {
  color: var(--color-accent);
  transform: translate(3px, -3px);
}
@media (max-width: 960px) {
  .article-row {
    grid-template-columns: minmax(0, 1fr) 24px;
    gap: 12px;
  }
  .article-meta {
    grid-column: 1 / -1;
    flex-direction: row;
    align-items: center;
    gap: 16px;
    padding: 0;
  }
}
@media (max-width: 640px) {
  .article-row {
    padding: 24px 0;
  }
  .article-title {
    font-size: 21px;
  }
  .article-excerpt {
    font-size: 14px;
  }
}
</style>
