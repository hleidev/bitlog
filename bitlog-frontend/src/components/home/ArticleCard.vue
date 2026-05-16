<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()

defineProps<{
  article: {
    id: number
    title: string
    summary: string | null
    cover: string | null
    categoryName: string | null
    tags: string[]
    publishTime: string
    readCount: number
    commentCount: number
  }
}>()

const formatDate = (iso: string) => iso ? iso.slice(0, 10) : ''
</script>

<template>
  <article class="article-card" @click="router.push(`/article/${article.id}`)">
    <div class="card-cover">
      <img :src="article.cover ?? ''" :alt="article.title" loading="lazy" />
    </div>
    <div class="card-body">
      <span class="card-category">{{ article.categoryName }}</span>
      <h2 class="card-title">{{ article.title }}</h2>
      <p class="card-summary">{{ article.summary }}</p>
      <div class="card-meta">
        <span class="meta-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
          {{ formatDate(article.publishTime) }}
        </span>
        <span class="meta-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
          </svg>
          {{ article.readCount.toLocaleString() }}
        </span>
        <span class="meta-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/>
          </svg>
          {{ article.commentCount }}
        </span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.article-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 28px;
  display: flex;
  align-items: flex-start;
  gap: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
}

.card-cover {
  width: 260px;
  height: 180px;
  flex-shrink: 0;
  border-radius: 12px;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.article-card:hover .card-cover img {
  transform: scale(1.06);
}

.card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 180px;
  min-width: 0;
}

.card-category {
  display: inline-block;
  align-self: flex-start;
  background: var(--color-bg-hover);
  color: var(--color-text-muted);
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 20px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color var(--transition-base);
}

.article-card:hover .card-title {
  color: var(--color-accent);
}

.card-summary {
  font-size: 14px;
  color: var(--color-text-muted);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  margin-top: auto;
  display: flex;
  align-items: center;
  gap: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-faint);
}

.meta-item svg {
  width: 13px;
  height: 13px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .article-card {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }

  .card-cover {
    width: 100%;
    height: 190px;
  }

  .card-body {
    min-height: unset;
  }

  .card-title {
    font-size: 15px;
  }
}
</style>
