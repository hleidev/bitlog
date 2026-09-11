<script setup lang="ts">
import { ref, computed, onMounted, onServerPrefetch, onUnmounted, useTemplateRef } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useSeoMeta, useHead } from '@unhead/vue'
import { type ArticleDetailVO, type ArticleLink } from '@/api/article'
import { getRelatedArticles } from '@/api/relatedArticles'
import { getCachedArticleDetail, fetchArticleDetail } from '@/api/articleCache'
import { readSSGState, writeSSGState } from '@/utils/ssgState'
import { formatDate } from '@/utils/format'
import { useUserStore } from '@/stores/useUserStore'
import ArticleContent from '@/components/ArticleContent.vue'
import ArticleToc from '@/components/ArticleToc.vue'
import CommentSection from '@/components/CommentSection.vue'
import RelatedArticles from '@/components/RelatedArticles.vue'

const router = useRouter()
const route = useRoute()
const articleListUrl = ref('/articles')

function returnToArticles(event: MouseEvent) {
  if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
  event.preventDefault()
  const back = router.options.history.state.back
  if (typeof back === 'string' && back.split(/[?#]/)[0] === '/articles') router.back()
  else void router.push(articleListUrl.value)
}

const targetCommentId = computed(() => {
  const comment = route.query.comment
  if (typeof comment !== 'string' || !/^[1-9]\d*$/.test(comment)) return undefined
  const id = Number(comment)
  return Number.isSafeInteger(id) ? id : undefined
})

const { isAdmin, userInfo } = storeToRefs(useUserStore())

const article = ref<ArticleDetailVO | null>(null)
const related = ref<ArticleLink[]>([])
const loading = ref(true)
const error = ref(false)
const slow = ref(false)
let slowTimer: ReturnType<typeof setTimeout> | null = null
const scrollProgress = ref(0)

// 两个条件缺一不可：写接口挂 @RequiresAdmin，后端 checkOwner 又只认属主
const canEdit = computed(
  () => isAdmin.value && !!article.value && article.value.userId === userInfo.value?.userId,
)

const SITE_URL = 'https://bitlog.harrylei.top'

const articleUrl = computed(() =>
  article.value ? `${SITE_URL}/article/${article.value.id}` : SITE_URL,
)
const articleDescription = computed(() => article.value?.summary ?? 'BitLog — 个人技术博客')

const jsonLd = computed(() => {
  if (!article.value) return null
  const iso = new Date(article.value.publishTime).toISOString()
  return {
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    mainEntityOfPage: { '@type': 'WebPage', '@id': articleUrl.value },
    headline: article.value.title,
    description: articleDescription.value,
    datePublished: iso,
    dateModified: iso,
    author: { '@type': 'Person', name: 'Harry Lei', url: SITE_URL },
    publisher: {
      '@type': 'Organization',
      name: 'BitLog',
      logo: { '@type': 'ImageObject', url: `${SITE_URL}/favicon.png` },
    },
    image: `${SITE_URL}/og-image.png`,
  }
})

useHead({
  title: () => (article.value ? `${article.value.title} | BitLog` : '文章详情 | BitLog'),
  link: [{ rel: 'canonical', href: articleUrl }],
  script: () =>
    jsonLd.value ? [{ type: 'application/ld+json', innerHTML: JSON.stringify(jsonLd.value) }] : [],
})
useSeoMeta({
  description: articleDescription,
  ogType: 'article',
  ogTitle: () => article.value?.title ?? 'BitLog',
  ogDescription: articleDescription,
  ogUrl: articleUrl,
  ogImage: `${SITE_URL}/og-image.png`,
  articlePublishedTime: () =>
    article.value ? new Date(article.value.publishTime).toISOString() : undefined,
  articleAuthor: () => (article.value ? ['https://bitlog.harrylei.top'] : undefined),
  articleSection: () => article.value?.category?.name,
  articleTag: () => article.value?.tags.map((t) => t.name),
  twitterCard: 'summary_large_image',
  twitterTitle: () => article.value?.title ?? 'BitLog',
  twitterDescription: articleDescription,
  twitterImage: `${SITE_URL}/og-image.png`,
})

// 正文每次渲染完（首次 / SWR 刷新 / 切文章）都重建一次目录
const tocRef = useTemplateRef<InstanceType<typeof ArticleToc>>('tocRef')

const onContentRendered = (root: HTMLElement | null) => {
  tocRef.value?.build(root)
}

const onScroll = () => {
  const el = document.documentElement
  const total = el.scrollHeight - el.clientHeight
  scrollProgress.value = total > 0 ? (el.scrollTop / total) * 100 : 0
}

async function loadRelated(current: ArticleDetailVO) {
  try {
    related.value = await getRelatedArticles(current)
  } catch (err) {
    // 相关文章是锦上添花，失败不该让文章页报错；但预渲染阶段静默降级会让静态产物
    // 悄悄少一块，构建日志里必须留一句
    if (import.meta.env.SSR) {
      console.warn(`[ssg] 文章 ${current.id} 相关文章取数失败: ${(err as Error).message}`)
    }
    related.value = []
  }
}

// ── 预渲染取数 ────────────────────────────────────────────────────────────────
// 预渲染阶段 onMounted 不执行，数据必须在 onServerPrefetch 里取，
// 上面的 useHead / useSeoMeta 取值函数才能拿到 article，把标题、og、
// JSON-LD 写进静态 HTML。
//
// 正文 content 不写进 initialState：它由客户端 Lute WASM 渲染，不参与服务端
// DOM，省掉每篇文章 HTML 里再带一份 Markdown 全文的体积。
const SSG_KEY = 'article'
const RELATED_KEY = 'related'

onServerPrefetch(async () => {
  try {
    const data = await fetchArticleDetail(Number(route.params.id))
    article.value = data
    loading.value = false
    writeSSGState(route, SSG_KEY, { ...data, content: '' })
    await loadRelated(data)
    writeSSGState(route, RELATED_KEY, related.value)
  } catch (err) {
    // 静态产物会退化成骨架屏空壳，构建后的 verify-ssg 会据此让构建失败
    console.error(`[ssg] 文章 ${route.params.id} 预渲染取数失败: ${(err as Error).message}`)
  }
})

// hydration：用预渲染的数据初始化首帧，与静态 HTML 保持一致，避免不匹配
const prerendered = readSSGState<ArticleDetailVO>(route, SSG_KEY)
if (prerendered) {
  article.value = prerendered
  loading.value = false
}

const prerenderedRelated = readSSGState<ArticleLink[]>(route, RELATED_KEY)
if (prerenderedRelated?.length) related.value = prerenderedRelated

onMounted(async () => {
  const back = router.options.history.state.back
  if (typeof back === 'string' && back.split(/[?#]/)[0] === '/articles') articleListUrl.value = back
  const id = Number(route.params.id)

  slowTimer = setTimeout(() => {
    if (loading.value) slow.value = true
  }, 6000)

  // 命中缓存（预取或上次访问）→ 立即渲染，跳过骨架
  const cached = getCachedArticleDetail(id)
  if (cached) {
    article.value = cached
    loading.value = false
  }

  // 始终拉一次最新，刷新缓存内容（SWR）
  try {
    article.value = await fetchArticleDetail(id)
  } catch {
    // 缓存或预渲染已有内容时保留展示，只有什么都没有才报错
    if (!article.value) error.value = true
  } finally {
    loading.value = false
    if (slowTimer) {
      clearTimeout(slowTimer)
      slowTimer = null
    }
    slow.value = false
  }

  if (!related.value.length && article.value) void loadRelated(article.value)

  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  if (slowTimer) clearTimeout(slowTimer)
})
</script>

<template>
  <main class="article-page">
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
        <Transition name="fade">
          <p v-if="slow" class="slow-hint">加载较慢，请稍候…</p>
        </Transition>
      </div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="page-state page-state--error">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="8" x2="12" y2="12" />
        <line x1="12" y1="16" x2="12.01" y2="16" />
      </svg>
      <p>文章加载失败</p>
      <button @click="router.back()">返回上一页</button>
    </div>

    <div v-else-if="article" class="article-layout container">
      <ArticleToc ref="tocRef" />

      <article class="article-body">
        <header class="article-heading">
          <a :href="articleListUrl" class="article-back" @click="returnToArticles"
            >← 返回文章列表</a
          >
          <!-- Category -->
          <RouterLink
            v-if="article.category"
            :to="{ path: '/articles', query: { categoryId: article.category.id } }"
            class="article-category"
            >{{ article.category.name }}</RouterLink
          >

          <!-- Title -->
          <h1 class="article-title">{{ article.title }}</h1>

          <div class="article-meta">
            <span class="article-author"><span aria-hidden="true">H</span> Harry</span>
            <span class="article-meta-sep" aria-hidden="true">/</span>
            <time
              v-if="article.publishTime"
              :datetime="article.publishTime"
              class="article-meta-date"
            >
              {{ formatDate(article.publishTime) }}
            </time>
            <span v-if="article.publishTime && canEdit" class="article-meta-sep" aria-hidden="true"
              >·</span
            >
            <!-- /admin/write/:id 加载 latestVersionId 对应的草稿，即最新版本而非当前阅读的已发布版本 -->
            <RouterLink v-if="canEdit" :to="`/admin/write/${article.id}`" class="article-meta-edit">
              编辑
            </RouterLink>
          </div>

          <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
        </header>
        <div class="article-divider" />

        <ArticleContent :content="article.content" @rendered="onContentRendered" />

        <!-- Footer tags -->
        <div class="article-footer">
          <div class="article-footer__tags">
            <RouterLink
              v-for="tag in article.tags"
              :key="tag.id"
              :to="{ path: '/articles', query: { tagId: tag.id } }"
              class="footer-tag"
              >{{ tag.name }}</RouterLink
            >
          </div>
        </div>

        <RelatedArticles :articles="related" class="reveal" />

        <!-- Comment section -->
        <div class="comment-section reveal">
          <div class="section-header">
            <h2 class="section-label">聊聊这篇文章</h2>
            <div class="section-rule"></div>
          </div>
          <CommentSection :article-id="article.id" :target-comment-id="targetCommentId" />
        </div>
      </article>
    </div>
  </main>
</template>

<style scoped>
.article-page {
  min-height: 80vh;
}
.progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  background: var(--color-accent);
  z-index: 1100;
  transition: width 0.1s linear;
}
.page-state {
  min-height: 65vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--color-text-muted);
}
.page-state--error svg {
  width: 40px;
  height: 40px;
}
.page-state--error button {
  padding: 10px 20px;
  color: var(--color-accent);
  border: 1px solid var(--color-border);
  font-size: 14px;
}
.skeleton-body {
  width: 100%;
  max-width: var(--spacing-prose);
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-top: calc(var(--spacing-header-height) + 48px);
}
.skeleton-line {
  height: 16px;
  background: var(--color-bg-hover);
  animation: pulse 1.2s ease-in-out infinite alternate;
}
.skeleton-line.w-20 {
  width: 20%;
}
.skeleton-line.w-50 {
  width: 50%;
}
.skeleton-line.w-70 {
  width: 70%;
  height: 32px;
}
.skeleton-line.w-80 {
  width: 80%;
}
.skeleton-line.w-100 {
  width: 100%;
}
@keyframes pulse {
  to {
    opacity: 0.4;
  }
}
.article-layout {
  padding-top: calc(var(--spacing-header-height) + 48px);
  padding-bottom: 100px;
}
.article-body {
  max-width: var(--spacing-prose);
  width: 100%;
  margin: 0 auto;
}
.article-heading {
  padding-bottom: 32px;
}
.article-back {
  display: block;
  width: fit-content;
  color: var(--color-text-muted);
  font-size: 13px;
  margin-bottom: 40px;
  padding: 6px 0;
}
.article-back:hover {
  color: var(--color-accent);
}
.article-category {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--color-accent);
  margin-bottom: 18px;
}
.article-category::before {
  content: '';
  width: 7px;
  height: 7px;
  background: currentColor;
}
.article-title {
  font: 600 clamp(30px, 3.6vw, 46px)/1.5 var(--font-display);
  letter-spacing: -0.035em;
  text-wrap: balance;
  overflow-wrap: anywhere;
  margin-bottom: 24px;
}
.article-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  color: var(--color-text-muted);
  font-size: 13px;
}
.article-author {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-text-primary);
}
.article-author > span {
  display: grid;
  place-items: center;
  height: 28px;
  width: 28px;
  color: var(--color-accent);
  border: 1px solid var(--color-border-strong);
  font: italic 18px var(--font-editorial);
}
.article-meta-sep {
  color: var(--color-border-strong);
}
.article-meta-edit:hover {
  color: var(--color-accent);
}
.article-summary {
  margin-top: 32px;
  padding: 20px 24px;
  background: var(--color-bg-hover);
  border-left: 2px solid var(--color-accent);
  font-size: 15px;
  line-height: 1.95;
  color: var(--color-text-secondary);
}
.article-divider {
  height: 1px;
  background: var(--color-border-strong);
  margin-bottom: 40px;
}
.article-body :deep(.article-content) {
  font-size: 17px;
  line-height: 1.95;
  color: var(--color-text-secondary);
  overflow-wrap: anywhere;
}
.article-body :deep(.article-content h2) {
  font-family: var(--font-display);
  font-size: 27px;
  margin-top: 52px;
}
.article-body :deep(.article-content h3) {
  font-size: 21px;
  margin-top: 36px;
}
.article-body :deep(.article-content pre) {
  overflow-wrap: normal;
}
.article-body :deep(.code-block-wrapper) {
  border-radius: 0;
}
.article-footer {
  border-top: 1px solid var(--color-border-strong);
  margin-top: 56px;
  padding-top: 24px;
}
.article-footer__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.footer-tag {
  font-size: 12px;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  padding: 6px 12px;
}
.footer-tag::before {
  content: '#';
  margin-right: 6px;
  color: var(--color-text-muted);
}
.footer-tag:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.comment-section {
  margin-top: 64px;
}
.section-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
}
.section-label {
  font: 500 22px var(--font-display);
  white-space: nowrap;
}
.section-rule {
  flex: 1;
  height: 1px;
  background: var(--color-border);
}
.slow-hint {
  color: var(--color-text-muted);
  font-size: 14px;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
@media (max-width: 768px) {
  .article-layout {
    padding-top: calc(var(--spacing-header-height) + 24px);
    padding-bottom: 64px;
  }
  .article-back {
    margin-bottom: 24px;
  }
  .article-title {
    font-size: 30px;
  }
  .article-summary {
    padding: 16px 18px;
    font-size: 14px;
    margin-top: 24px;
  }
  .article-heading {
    padding-bottom: 24px;
  }
  .article-divider {
    margin-bottom: 28px;
  }
  .article-body :deep(.article-content) {
    font-size: 16px;
    line-height: 1.9;
  }
  .article-body :deep(.article-content h2) {
    font-size: 24px;
  }
  .article-body :deep(.article-content h3) {
    font-size: 20px;
  }
  .section-label {
    font-size: 20px;
  }
}
</style>
