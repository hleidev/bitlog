<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import HeroSection from '@/components/home/HeroSection.vue'
import { getArticlePage, type ArticleItemVO } from '@/api/article'

const articles = ref<ArticleItemVO[]>([])
const loading = ref(false)

const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

function formatMonth(iso: string | null): string {
  if (!iso) return '—'
  return MONTHS[new Date(iso).getMonth()]
}

function formatYear(iso: string | null): string {
  if (!iso) return ''
  return String(new Date(iso).getFullYear())
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getArticlePage({ pageNum: 1, pageSize: 7 })
    articles.value = res.content
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main>
    <HeroSection />

    <div id="content-area" class="home-main">
      <div class="section-header">
        <span class="section-label">近期文章</span>
        <div class="section-rule"></div>
      </div>

      <div class="article-list" :class="{ 'article-list--loading': loading }">
        <RouterLink
          v-for="article in articles"
          :key="article.id"
          :to="`/article/${article.id}`"
          class="article-row"
        >
          <div class="article-date">
            {{ formatMonth(article.publishTime) }}<span class="article-year">{{ formatYear(article.publishTime) }}</span>
          </div>
          <div class="article-body">
            <span class="article-title">{{ article.title }}</span>
            <span v-if="article.summary" class="article-excerpt">{{ article.summary }}</span>
          </div>
          <div class="article-right">
            <span class="article-tag">{{ article.categoryName ?? article.tags[0] ?? '' }}</span>
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
}

.article-list--loading {
  opacity: 0.4;
  pointer-events: none;
}

.article-row {
  display: flex;
  align-items: flex-start;
  gap: 40px;
  padding: 28px 0;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
}

.article-date {
  width: 72px;
  flex-shrink: 0;
  font-size: 11px;
  color: var(--color-text-muted);
  letter-spacing: 0.03em;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  padding-top: 3px;
  line-height: 1.4;
  transition: color var(--transition-base);
}

.article-year {
  display: block;
  font-size: 10px;
  margin-top: 2px;
  opacity: 0.65;
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
