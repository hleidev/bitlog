<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { useSeoMeta, useHead } from '@unhead/vue'
import { getArticleDetail, type ArticleDetailVO } from '@/api/article'
import { formatDate } from '@/utils/format'
import { ArticleEditor } from '@bitlog/editor'

const router = useRouter()
const route = useRoute()

const article = ref<ArticleDetailVO | null>(null)
const loading = ref(true)
const error = ref(false)
const scrollProgress = ref(0)

const SITE_URL = 'https://bitlog.harrylei.top'

const articleUrl = computed(() => article.value ? `${SITE_URL}/article/${article.value.id}` : SITE_URL)
const articleDescription = computed(() => article.value?.summary ?? 'BitLog — 个人技术博客')

useHead({
  title: () => article.value ? `${article.value.title} | BitLog` : 'BitLog',
  link: [{ rel: 'canonical', href: articleUrl }],
})
useSeoMeta({
  description: articleDescription,
  ogType: 'article',
  ogTitle: () => article.value?.title ?? 'BitLog',
  ogDescription: articleDescription,
  ogUrl: articleUrl,
  ogImage: `${SITE_URL}/og-image.png`,
  articlePublishedTime: () => article.value ? new Date(article.value.publishTime).toISOString() : undefined,
  articleAuthor: () => article.value ? ['https://bitlog.harrylei.top'] : undefined,
  articleSection: () => article.value?.category?.name,
  articleTag: () => article.value?.tags.map((t) => t.name),
  twitterCard: 'summary_large_image',
  twitterTitle: () => article.value?.title ?? 'BitLog',
  twitterDescription: articleDescription,
  twitterImage: `${SITE_URL}/og-image.png`,
})

const onScroll = () => {
  const el = document.documentElement
  const total = el.scrollHeight - el.clientHeight
  scrollProgress.value = total > 0 ? (el.scrollTop / total) * 100 : 0
}

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    article.value = await getArticleDetail(id)
  } catch {
    error.value = true
  }
  loading.value = false
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <div class="article-page">
    <!-- Reading progress bar -->
    <div class="progress-bar" :style="{ width: scrollProgress + '%' }" />

    <!-- Loading -->
    <div v-if="loading" class="page-state">
      <div class="skeleton-body container">
        <div class="skeleton-line w-20" />
        <div class="skeleton-line w-70" />
        <div class="skeleton-line w-50" />
        <div class="skeleton-line w-100" />
        <div class="skeleton-line w-80" />
        <div class="skeleton-line w-100" />
        <div class="skeleton-line w-70" />
      </div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="page-state page-state--error">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
      </svg>
      <p>文章加载失败</p>
      <button @click="router.back()">返回上一页</button>
    </div>

    <div v-else-if="article" class="article-layout container view-enter">
      <article class="article-body">
        <!-- Category -->
        <RouterLink
          v-if="article.category"
          :to="{ path: '/articles', query: { categoryId: article.category.id } }"
          class="article-category"
        >{{ article.category.name }}</RouterLink>

        <!-- Title -->
        <h1 class="article-title">{{ article.title }}</h1>

        <div v-if="article.publishTime" class="article-meta-date">{{ formatDate(article.publishTime) }}</div>

        <div class="article-divider" />

        <ArticleEditor :content="article.content" />

        <!-- Footer tags -->
        <div class="article-footer">
          <div class="article-footer__tags">
            <RouterLink
              v-for="tag in article.tags"
              :key="tag.id"
              :to="{ path: '/articles', query: { tagId: tag.id } }"
              class="footer-tag"
            >{{ tag.name }}</RouterLink>
          </div>
        </div>

        <!-- Comment section -->
        <div class="comment-section">
          <div class="section-header">
            <span class="section-label">评论</span>
            <div class="section-rule"></div>
          </div>
          <p class="comment-placeholder">评论功能开发中。</p>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.progress-bar {
  position: fixed;
  top: 0; left: 0;
  height: 2px;
  background: var(--color-accent);
  z-index: 1000;
  transition: width 0.1s linear;
}

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

.page-state--error button {
  font-size: 13px;
  color: var(--color-accent);
  background: none;
  border: 1px solid var(--color-accent);
  border-radius: 4px;
  padding: 6px 18px;
  cursor: pointer;
  transition: all var(--transition-base);
  font-family: var(--font-sans);
}

.page-state--error button:hover { background: var(--color-bg-hover); }

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: calc(var(--spacing-header-height) + 64px);
  max-width: var(--spacing-prose);
  margin: 0 auto;
  width: 100%;
  padding-left: var(--spacing-page-padding);
  padding-right: var(--spacing-page-padding);
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
.skeleton-line.w-60 { width: 60%; }
.skeleton-line.w-70 { width: 70%; }
.skeleton-line.w-80 { width: 80%; }
.skeleton-line.w-100 { width: 100%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.article-layout {
  padding-top: calc(var(--spacing-header-height) + 64px);
  padding-bottom: 100px;
  padding-left: var(--spacing-page-padding);
  padding-right: var(--spacing-page-padding);
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
  transition: color var(--transition-base);
}

.article-category:hover { color: var(--color-accent-dark); }

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

.article-meta-date {
  font-size: 13px;
  color: var(--color-text-faint);
  letter-spacing: 0.04em;
  margin-top: -20px;
  margin-bottom: 32px;
  font-family: var(--font-sans);
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
  transition: all var(--transition-base);
}

.footer-tag:hover {
  color: var(--color-accent);
  border-color: var(--color-accent);
}

.comment-section { margin-top: 56px; }

.section-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
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

.comment-placeholder {
  font-size: 13px;
  color: var(--color-text-faint);
  letter-spacing: 0.02em;
}

@media (max-width: 900px) {
  .article-title { font-size: 28px; }
}

@media (max-width: 768px) {
  .article-layout {
    padding-top: calc(var(--spacing-header-height) + 40px);
    padding-left: 20px;
    padding-right: 20px;
  }
}
</style>
