<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import HeroSection from '@/components/home/HeroSection.vue'
import { getArticlePage, type ArticleItemVO } from '@/api/article'
import ArticleListSkeleton from '@/components/common/ArticleListSkeleton.vue'
import { prefetchArticleDetail } from '@/api/articleCache'

const articles = ref<ArticleItemVO[]>([])
const loading = ref(false)
const articleListRef = ref<HTMLElement | null>(null)

function initRowAnimation() {
  const rows = articleListRef.value?.querySelectorAll<HTMLElement>('.article-row')
  if (!rows?.length) return
  rows.forEach((row, i) => {
    row.style.animationDelay = `${i * 55}ms`
    row.classList.add('is-visible')
  })
}

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}`
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getArticlePage({ pageNum: 1, pageSize: 7 })
    articles.value = res.content
  } finally {
    loading.value = false
  }
  await nextTick()
  initRowAnimation()
})
</script>

<template>
  <main>
    <HeroSection />

    <div id="content-area" class="home-main view-enter">
      <div class="section-header">
        <span class="section-label">近期文章</span>
        <div class="section-rule"></div>
      </div>

      <ArticleListSkeleton v-if="loading && articles.length === 0" :rows="7" />

      <div
        v-else
        ref="articleListRef"
        class="article-list"
        :class="{ 'article-list--loading': loading }"
      >
        <RouterLink
          v-for="article in articles"
          :key="article.id"
          :to="`/article/${article.id}`"
          class="article-row"
          @mouseenter="prefetchArticleDetail(article.id)"
          @focus="prefetchArticleDetail(article.id)"
        >
          <div class="article-date">
            {{ formatDate(article.publishTime) }}
          </div>
          <div class="article-body">
            <span class="article-title">{{ article.title }}</span>
            <span v-if="article.summary" class="article-excerpt">{{ article.summary }}</span>
          </div>
          <div class="article-right">
            <span class="article-tag">{{ article.category?.name || article.tags[0]?.name || '' }}</span>
          </div>
        </RouterLink>
      </div>

      <RouterLink to="/articles" class="more-link">
        全部文章 <span class="more-arrow">→</span>
      </RouterLink>
    </div>
  </main>
</template>

<style scoped>
.home-main {
  max-width: var(--spacing-container);
  margin: 0 auto;
  padding: 72px var(--spacing-page-padding) 120px;
}

/* ── Section header ── */

.section-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
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

/* ── Article list ── */

.article-list {
  border-top: 1px solid var(--color-border);
  transition: opacity var(--transition-base);
  counter-reset: article-counter;
}

.article-list--loading {
  opacity: 0.4;
  pointer-events: none;
}

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

/* ── More link ── */

.more-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 44px;
  font-size: 12.5px;
  letter-spacing: 0.07em;
  color: var(--color-text-muted);
  background-image: linear-gradient(currentColor, currentColor);
  background-repeat: no-repeat;
  background-size: 0% 1px;
  background-position: left bottom;
  padding-bottom: 1px;
  transition: color var(--transition-base), background-size 0.28s ease;
}

.more-link:hover {
  color: var(--color-text-secondary);
  background-size: 100% 1px;
}

.more-arrow {
  display: inline-block;
  transition: transform var(--transition-base);
}

.more-link:hover .more-arrow {
  transform: translateX(4px);
}

/* ── Mobile ── */

@media (max-width: 768px) {
  .home-main {
    padding: 48px 20px 80px;
  }

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
