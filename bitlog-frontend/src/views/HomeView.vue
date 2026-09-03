<script setup lang="ts">
import { ref, onMounted, onServerPrefetch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import HeroSection from '@/components/home/HeroSection.vue'
import { getArticlePage, type ArticleItemVO } from '@/api/article'
import { readSSGState, writeSSGState } from '@/utils/ssgState'
import ArticleListSkeleton from '@/components/common/ArticleListSkeleton.vue'
import ArticleRow from '@/components/common/ArticleRow.vue'

const route = useRoute()

const articles = ref<ArticleItemVO[]>([])
const loading = ref(false)

async function loadArticles() {
  loading.value = true
  try {
    const res = await getArticlePage({ pageNum: 1, pageSize: 7 })
    articles.value = res.content
  } finally {
    loading.value = false
  }
}

// ── 预渲染取数 ────────────────────────────────────────────────────────────────
// 预渲染阶段 onMounted 不执行，文章列表必须在 onServerPrefetch 里取，
// 才能进入首页的静态 HTML。
const SSG_KEY = 'homeArticles'

onServerPrefetch(async () => {
  try {
    await loadArticles()
    writeSSGState(route, SSG_KEY, articles.value)
  } catch (err) {
    // 静态产物会退化成空壳，构建后的 verify-ssg 会据此让构建失败
    console.error(`[ssg] 首页预渲染取数失败: ${(err as Error).message}`)
  }
})

// hydration：命中预渲染数据则跳过首次请求。发布文章会触发前端重建，
// 静态内容本身就是最新的，再请求一次只会让列表闪一下半透明。
const prerendered = readSSGState<ArticleItemVO[]>(route, SSG_KEY)
if (prerendered) articles.value = prerendered

onMounted(async () => {
  if (articles.value.length === 0) await loadArticles()
})
</script>

<template>
  <main>
    <HeroSection />

    <div id="content-area" class="home-main">
      <div class="section-header reveal">
        <span class="section-label">近期文章</span>
        <div class="section-rule"></div>
      </div>

      <ArticleListSkeleton v-if="loading && articles.length === 0" :rows="7" />

      <div v-else class="article-list" :class="{ 'article-list--loading': loading }">
        <ArticleRow
          v-for="article in articles"
          :key="article.id"
          :article="article"
          class="reveal"
        />
      </div>

      <RouterLink to="/articles" class="more-link reveal">
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

/* ── More link ── */

.more-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 44px;
  font-size: 12.5px;
  letter-spacing: 0.07em;
  color: var(--color-accent);
  background-image: linear-gradient(currentColor, currentColor);
  background-repeat: no-repeat;
  background-size: 0% 1px;
  background-position: left bottom;
  padding-bottom: 1px;
  transition:
    color var(--transition-base),
    background-size 0.28s ease;
}

.more-link:hover {
  color: var(--color-accent-dark);
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
}
</style>
