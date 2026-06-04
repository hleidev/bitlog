<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getArticleDraft } from '@/api/admin/article'
import type { ArticleDetailVO } from '@/api/admin/article'
import { ArticleEditor } from '@bitlog/editor'

const route = useRoute()
const article = ref<ArticleDetailVO | null>(null)
const loading = ref(true)
const error = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    article.value = await getArticleDraft(id)
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="preview-page">
    <div v-if="loading" class="page-state">
      <div class="skeleton-body">
        <div class="skeleton-line w-20" />
        <div class="skeleton-line w-70" />
        <div class="skeleton-line w-50" />
        <div class="skeleton-line w-100" />
        <div class="skeleton-line w-80" />
      </div>
    </div>

    <div v-else-if="error" class="page-state page-state--error">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12.01" y1="16" x2="12.01" y2="16" />
      </svg>
      <p>文章加载失败</p>
    </div>

    <div v-else-if="article" class="article-layout">
      <article class="article-body">
        <!-- Category -->
        <span v-if="article.category" class="article-category">{{ article.category.name }}</span>

        <!-- Title -->
        <h1 class="article-title">{{ article.title }}</h1>

        <div class="article-divider" />

        <ArticleEditor :content="article.content" />

        <!-- Footer tags -->
        <div v-if="article.tags.length" class="article-footer">
          <div class="article-footer__tags">
            <span
              v-for="tag in article.tags"
              :key="tag.id"
              class="footer-tag"
            >{{ tag.name }}</span>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.page-state {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--color-text-muted);
}

.page-state--error svg { width: 40px; height: 40px; color: var(--color-text-faint); }
.page-state--error p { font-size: 15px; }

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: var(--spacing-prose);
  width: 100%;
  padding: 64px 48px 0;
  margin: 0 auto;
}

.skeleton-line {
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--color-bg-hover) 25%, var(--color-border) 50%, var(--color-bg-hover) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-line.w-20 { width: 20%; height: 10px; }
.skeleton-line.w-50 { width: 50%; }
.skeleton-line.w-70 { width: 70%; }
.skeleton-line.w-80 { width: 80%; }
.skeleton-line.w-100 { width: 100%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.article-layout {
  padding: 64px 48px 100px;
}

.article-body {
  max-width: var(--spacing-prose);
  margin: 0 auto;
  width: 100%;
}

.article-category {
  display: inline-block;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--color-accent);
  margin-bottom: 20px;
}

.article-title {
  font-family: var(--font-serif);
  font-size: clamp(28px, 4vw, 48px);
  font-weight: 400;
  line-height: 1.28;
  color: var(--color-text-primary);
  letter-spacing: 0.01em;
  margin-bottom: 32px;
}

.article-divider {
  height: 1px;
  background: var(--color-border);
  margin-bottom: 36px;
}

.article-footer {
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
}

.article-footer__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.footer-tag {
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  padding: 4px 12px;
  border-radius: var(--radius-tag);
  border: 1px solid var(--color-border);
}

@media (max-width: 768px) {
  .article-layout { padding: 40px 20px 80px; }
}
</style>
