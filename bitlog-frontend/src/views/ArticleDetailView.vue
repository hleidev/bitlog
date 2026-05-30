<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { getArticleDetail, type ArticleDetailVO } from '@/api/article'
import { formatDate } from '@/utils/format'
import ProseContent from '@/components/ProseContent.vue'
import TocSidebar from '@/components/TocSidebar.vue'
import { useToc } from '@/composables/useToc'

const router = useRouter()
const route = useRoute()

const article = ref<ArticleDetailVO | null>(null)
const loading = ref(true)
const error = ref(false)
const scrollProgress = ref(0)

const { toc, activeSection, visibleTocItems, buildToc, scrollToSection } = useToc()

const HEADER_OFFSET = 60

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
  if (!article.value) return
  await nextTick()
  buildToc()
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
      <div class="skeleton-header" />
      <div class="skeleton-body container">
        <div class="skeleton-line w-60" />
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

    <div v-else-if="article" class="article-detail">
      <!-- Article header -->
      <div class="article-header">
        <div class="article-header__inner">
          <nav class="article-breadcrumb">
            <button class="breadcrumb-link" @click="router.back()">文章</button>
            <template v-if="article.category">
              <span class="breadcrumb-sep">/</span>
              <RouterLink
                :to="{ path: '/articles', query: { categoryId: article.category.id } }"
                class="breadcrumb-link"
              >{{ article.category.name }}</RouterLink>
            </template>
          </nav>
          <div class="article-header__label">
            <div class="header-rule"></div>
          </div>
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-header__foot">
            <span class="meta-date">{{ formatDate(article.publishTime) }}</span>
          </div>
        </div>
      </div>

      <!-- Article layout -->
      <div class="article-layout container view-enter">
        <article class="article-body">
          <!-- Tags -->
          <div v-if="article.tags.length" class="article-tags">
            <RouterLink
              v-for="tag in article.tags"
              :key="tag.id"
              :to="{ path: '/articles', query: { tagId: tag.id } }"
              class="article-tag"
            >{{ tag.name }}</RouterLink>
          </div>

          <ProseContent :content="article.content" />

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

        <!-- TOC sidebar -->
        <aside v-if="toc.length" class="toc-sidebar">
          <TocSidebar
            :items="visibleTocItems"
            :active-section="activeSection"
            @scroll-to="(id: string) => scrollToSection(id, HEADER_OFFSET)"
          />
        </aside>
      </div>
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

.skeleton-header {
  width: 100%;
  height: 320px;
  background: var(--color-hero-bg);
}

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 40px;
}

.skeleton-line {
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--color-bg-hover) 25%, var(--color-border) 50%, var(--color-bg-hover) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-line.w-60 { width: 60%; }
.skeleton-line.w-70 { width: 70%; }
.skeleton-line.w-80 { width: 80%; }
.skeleton-line.w-100 { width: 100%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.article-header {
  background: var(--color-hero-bg);
  padding-top: var(--spacing-header-height);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.article-header__inner {
  max-width: var(--spacing-container);
  margin: 0 auto;
  padding: 72px var(--spacing-page-padding) 80px;
}

.article-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 28px;
}

.breadcrumb-link {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(245, 243, 239, 0.4);
  padding-bottom: 1px;
  background-image: linear-gradient(var(--color-accent), var(--color-accent));
  background-repeat: no-repeat;
  background-size: 0% 1px;
  background-position: left bottom;
  transition: color var(--transition-base), background-size var(--transition-sweep);
}

.breadcrumb-link:hover {
  color: rgba(245, 243, 239, 0.75);
  background-size: 100% 1px;
}

.breadcrumb-sep {
  font-size: 11px;
  color: rgba(245, 243, 239, 0.2);
  user-select: none;
}

.article-header__label {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 32px;
}

.header-rule {
  flex: 1;
  height: 1px;
  background: rgba(245, 243, 239, 0.08);
}

.article-title {
  font-family: var(--font-serif);
  font-size: clamp(28px, 4.5vw, 54px);
  font-weight: 400;
  line-height: 1.28;
  color: var(--color-text-on-dark);
  max-width: 860px;
  letter-spacing: 0.01em;
}

.article-header__foot { margin-top: 28px; }

.meta-date {
  font-size: 12px;
  color: rgba(245, 243, 239, 0.3);
  letter-spacing: 0.08em;
  font-family: var(--font-sans);
}

.article-layout {
  display: flex;
  align-items: flex-start;
  gap: 48px;
  padding-top: 56px;
  padding-bottom: 100px;
  padding-left: var(--spacing-page-padding);
  padding-right: var(--spacing-page-padding);
}

.article-body {
  flex: 1;
  min-width: 0;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 36px;
}

.article-tag {
  font-size: 11.5px;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  padding: 3px 10px;
  border-radius: var(--radius-tag);
  transition: all var(--transition-base);
}

.article-tag:hover {
  color: var(--color-accent);
  border-color: var(--color-accent);
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

.toc-sidebar {
  width: 220px;
  flex-shrink: 0;
  position: sticky;
  top: calc(var(--spacing-header-height) + 24px);
  max-height: calc(100vh - var(--spacing-header-height) - 48px);
}

@media (max-width: 900px) {
  .toc-sidebar { display: none; }
  .article-title { font-size: 26px; }
  .article-layout { padding-top: 40px; }
}

@media (max-width: 768px) {
  .article-header__inner { padding: 48px 20px 56px; }
  .article-layout { padding-left: 20px; padding-right: 20px; }
}
</style>
