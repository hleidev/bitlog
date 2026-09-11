<script setup lang="ts">
import { computed, ref, onMounted, onServerPrefetch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useHead, useSeoMeta } from '@unhead/vue'
import HeroSection from '@/components/home/HeroSection.vue'
import { getArticlePage, type ArticleItemVO } from '@/api/article'
import { readSSGState, writeSSGState } from '@/utils/ssgState'
import ArticleListSkeleton from '@/components/common/ArticleListSkeleton.vue'
import { prefetchArticleDetail } from '@/api/articleCache'
import { formatDate } from '@/utils/format'
import ArticleRow from '@/components/common/ArticleRow.vue'

const pageTitle = '首页 | BitLog'
const pageDescription = '我是 Harry，一名后端工程师。爱折腾，也在奔波里记录生活的边角料。'

useHead({
  title: pageTitle,
  link: [{ rel: 'canonical', href: 'https://bitlog.harrylei.top/' }],
})
useSeoMeta({
  description: pageDescription,
  ogTitle: pageTitle,
  ogDescription: pageDescription,
  ogUrl: 'https://bitlog.harrylei.top/',
})

const route = useRoute()

const articles = ref<ArticleItemVO[]>([])
const loading = ref(false)
const error = ref(false)
const latest = computed(() => articles.value[0])

async function loadArticles() {
  loading.value = true
  error.value = false
  try {
    const res = await getArticlePage({ pageNum: 1, pageSize: 7 })
    articles.value = res.content
  } catch (err) {
    error.value = true
    if (import.meta.env.SSR) throw err
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

    <div id="content-area" class="home-main container">
      <section id="latest" class="latest-section" aria-labelledby="latest-heading">
        <div class="section-aside">
          <span class="section-number" aria-hidden="true">01 /</span>
          <h2 id="latest-heading">最新一篇</h2>
          <span class="section-english">THE LATEST</span>
        </div>
        <ArticleListSkeleton v-if="loading && !articles.length" :rows="1" />
        <div v-else-if="error && !articles.length" class="home-state" role="status">
          <p>文章暂时没能加载出来。</p>
          <button class="journal-link" @click="loadArticles">
            重新加载 <span aria-hidden="true">↻</span>
          </button>
        </div>
        <RouterLink
          v-else-if="latest"
          :to="`/article/${latest.id}`"
          class="featured"
          @mouseenter="prefetchArticleDetail(latest.id)"
          @focus="prefetchArticleDetail(latest.id)"
        >
          <div class="featured__meta">
            <span>{{ latest.category?.name || '随记' }}</span>
            <time :datetime="latest.publishTime">{{ formatDate(latest.publishTime) }}</time>
          </div>
          <h3>{{ latest.title }}</h3>
          <p v-if="latest.summary">{{ latest.summary }}</p>
          <span class="featured__read">阅读全文 <span aria-hidden="true">↗</span></span>
        </RouterLink>
        <p v-else class="home-state">还没有发布文章。</p>
      </section>

      <section v-if="articles.length > 1" class="recent-section" aria-labelledby="recent-heading">
        <div class="section-aside">
          <span class="section-number" aria-hidden="true">02 /</span>
          <h2 id="recent-heading">往期记录</h2>
          <span class="section-english">MORE NOTES</span>
          <RouterLink to="/articles" class="journal-link section-more"
            >全部文章 <span aria-hidden="true">↗</span></RouterLink
          >
        </div>
        <div class="article-list">
          <ArticleRow v-for="article in articles.slice(1)" :key="article.id" :article="article" />
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.home-main {
  padding-bottom: 100px;
}
.latest-section,
.recent-section {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 48px;
  border-top: 1px solid var(--color-text-primary);
}
.latest-section {
  padding: 38px 0 48px;
  scroll-margin-top: 96px;
}
.recent-section {
  padding-top: 32px;
  border-top-color: var(--color-border-strong);
}
.section-aside {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.section-number {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--color-accent);
}
.section-aside h2 {
  font-size: 22px;
  font-weight: 500;
  margin: 20px 0 5px;
  letter-spacing: -0.03em;
}
.section-english {
  color: var(--color-text-muted);
  font: 12px var(--font-mono);
  letter-spacing: 0.08em;
}
.section-more {
  margin-top: 40px;
  min-width: 130px;
}
.featured {
  position: relative;
  display: block;
  padding-right: 44px;
}
.featured__meta {
  display: flex;
  gap: 16px;
  align-items: center;
  font-size: 12px;
  color: var(--color-text-muted);
}
.featured__meta > span {
  background: var(--journal-soft);
  color: var(--color-accent);
  padding: 4px 10px;
}
.featured h3 {
  font-family: var(--font-display);
  font-size: clamp(26px, 3vw, 38px);
  font-weight: 600;
  letter-spacing: -0.035em;
  line-height: 1.5;
  margin: 18px 0 16px;
  text-wrap: pretty;
  transition: color 0.2s;
}
.featured p {
  color: var(--color-text-secondary);
  font-size: 15px;
  line-height: 1.9;
  max-width: 52em;
}
.featured__read {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-top: 24px;
  font-size: 14px;
}
.featured__read > span {
  color: var(--color-accent);
  font-size: 24px;
  transition: transform 0.2s;
}
.featured:hover h3 {
  color: var(--color-accent);
}
.featured:hover .featured__read > span {
  transform: translate(3px, -3px);
}
.article-list {
  min-width: 0;
}
.home-state {
  color: var(--color-text-muted);
  padding: 24px 0;
}
@media (max-width: 960px) {
  .latest-section,
  .recent-section {
    grid-template-columns: 150px minmax(0, 1fr);
    gap: 32px;
  }
  .featured {
    padding-right: 0;
  }
}
@media (max-width: 640px) {
  .home-main {
    padding-bottom: 64px;
  }
  .latest-section,
  .recent-section {
    grid-template-columns: minmax(0, 1fr);
    gap: 24px;
    padding-top: 24px;
  }
  .latest-section {
    padding-bottom: 32px;
  }
  .section-aside {
    flex-direction: row;
    align-items: baseline;
    flex-wrap: wrap;
    gap: 10px;
  }
  .section-aside h2 {
    font-size: 18px;
    margin: 0;
  }
  .section-english {
    margin-left: auto;
    font-size: 12px;
  }
  .section-more {
    margin: 0 0 0 auto;
    min-width: auto;
    gap: 12px;
    min-height: 32px;
  }
  .recent-section .section-english {
    display: none;
  }
  .featured h3 {
    font-size: 27px;
  }
  .featured p {
    font-size: 14px;
  }
}
</style>
