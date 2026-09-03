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
      <span class="article-date">{{ formatYearMonth(article.publishTime) }}</span>
    </div>
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
  align-items: baseline;
  gap: 40px;
  padding: 28px 10px;
  margin: 0 -10px;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  position: relative;
  transition: background var(--transition-base);
}

.article-meta {
  display: flex;
  align-items: baseline;
  gap: 12px;
  width: 96px;
  flex-shrink: 0;
}

.article-meta::before {
  counter-increment: article-counter;
  content: counter(article-counter, decimal-leading-zero);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-faint);
  letter-spacing: 0.06em;
  flex-shrink: 0;
}

.article-row:hover {
  background: var(--color-bg-hover);
}

/* hover 的落点是最左侧这根短竖线，与 hero 的 accent-bar 同一形状语言。
   正文列被它推开 5px，右侧分类不动，列对齐不塌。 */
.article-row::after {
  content: '';
  position: absolute;
  left: 0;
  /* 与上下 padding 对齐，让竖线随行高伸缩：行有无摘要相差 20 多像素，
     写死高度会让同一条杠在不同行里比例完全不同。 */
  top: 28px;
  bottom: 28px;
  width: 2.5px;
  background: var(--color-accent);
  border-radius: var(--radius-tag);
  transform: scaleY(0);
  transition: transform var(--transition-sweep);
}

.article-row:hover::after {
  transform: scaleY(1);
}

.article-meta,
.article-body {
  transition: transform var(--transition-sweep);
}

.article-row:hover .article-meta,
.article-row:hover .article-body {
  transform: translateX(5px);
}

.article-date {
  font-size: 11px;
  color: var(--color-text-muted);
  letter-spacing: 0.03em;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  transition: color var(--transition-base);
}

/* 分类保持右对齐成列（编辑式列表的常规做法），但正文列上限从 680px 提到
   840px：680px 在 1280px 容器下会在正文与分类之间留出 ~250px 的空洞，
   让分类看起来像漂在页面边缘的孤儿。840px 把空隙收到 ~75px，
   窄屏下正文自然收缩、分类紧贴其后。 */
.article-body {
  flex: 1;
  min-width: 0;
  max-width: 840px;
  display: flex;
  flex-direction: column;
  gap: 7px;
  /* 标题 + 一行摘要的高度。没有摘要的文章不至于把整行压矮半截，列表节奏才稳。 */
  min-height: 58px;
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
  .article-meta {
    display: none;
  }
}
</style>
