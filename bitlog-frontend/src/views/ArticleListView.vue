<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, onServerPrefetch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSeoMeta, useHead } from '@unhead/vue'
import { getArticlePage, type ArticleItemVO } from '@/api/article'
import type { PageResult } from '@/api/types'
import { useListQuery } from '@/composables/useListQuery'
import { getCategories, type CategoryVO } from '@/api/category'
import { getTags, type TagVO } from '@/api/tag'
import { readSSGState, writeSSGState } from '@/utils/ssgState'
import ArticleListSkeleton from '@/components/common/ArticleListSkeleton.vue'
import ArticleRow from '@/components/common/ArticleRow.vue'
import { readArticleListQuery, writeArticleListQuery } from '@/utils/articleListQuery'

useHead({
  title: '文章 | BitLog',
  link: [{ rel: 'canonical', href: 'https://bitlog.harrylei.top/articles' }],
})
useSeoMeta({
  description: '浏览 BitLog 全部文章，按分类和标签筛选。',
  ogTitle: '文章 | BitLog',
  ogDescription: '浏览 BitLog 全部文章，按分类和标签筛选。',
  ogUrl: 'https://bitlog.harrylei.top/articles',
})

const route = useRoute()
const router = useRouter()

// ── Metadata ──────────────────────────────────────────────────────────────────

const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])

const categoryTabs = computed(() => [
  { id: null as number | null, name: '全部' },
  ...categories.value,
])

// ── Filter state ──────────────────────────────────────────────────────────────

const searchFocused = ref(false)
const initializing = ref(false)
const loadError = ref(false)
let initialized = false
let applyingRoute = false
let disposed = false
let searchTimer: ReturnType<typeof setTimeout> | null = null

const query = useListQuery({
  filters: { keyword: '', categoryIdx: 0, tagIds: [] as number[] },
  toParams: (f) => ({
    keyword: f.keyword,
    categoryId: categoryTabs.value[f.categoryIdx]?.id ?? undefined,
    allTagIds: f.tagIds,
  }),
  fetch: (params) => {
    loadError.value = false
    return getArticlePage(params)
  },
  onError: () => {
    loadError.value = true
  },
  pageSize: 12,
  // URL 是查询的来源，组件负责导航和搜索防抖，不启动内部过滤监听。
  immediate: false,
})

const { filters, pageNum, total, totalPages, hasPrevious, hasNext } = query
const loading = computed(() => initializing.value || query.loading.value)
const articles = query.items
const visiblePages = query.pageNumbers
async function fetchArticles() {
  const requestedQuery = route.query
  await query.load()
  // 接口发现页码越界时，useListQuery 会回到最后一页，地址也一起校正。
  if (
    !disposed &&
    route.query === requestedQuery &&
    !loadError.value &&
    pageNum.value !== readArticleListQuery(route.query).page
  ) {
    await updateUrl(pageNum.value)
  }
}

const hasFilters = computed(
  () => filters.keyword || filters.categoryIdx !== 0 || filters.tagIds.length > 0,
)

function clearAll() {
  filters.keyword = ''
  filters.categoryIdx = 0
  filters.tagIds = []
  void updateUrl(1)
}

function toggleTag(tagId: number) {
  const idx = filters.tagIds.indexOf(tagId)
  if (idx === -1) filters.tagIds = [...filters.tagIds, tagId]
  else filters.tagIds = filters.tagIds.filter((id) => id !== tagId)
  void updateUrl(1)
}

function selectCategory(idx: number) {
  filters.categoryIdx = idx
  void updateUrl(1)
}

// ── Article list ──────────────────────────────────────────────────────────────

const slow = ref(false)
let slowTimer: ReturnType<typeof setTimeout> | null = null

// 超过 6s 仍在加载才提示，避免正常速度下闪一下
watch(loading, (busy) => {
  if (slowTimer) clearTimeout(slowTimer)
  if (busy) {
    slowTimer = setTimeout(() => {
      if (loading.value) slow.value = true
    }, 6000)
  } else {
    slow.value = false
  }
})

async function applyRouteFilters() {
  if (searchTimer) clearTimeout(searchTimer)
  applyingRoute = true
  const state = readArticleListQuery(route.query)
  filters.keyword = state.keyword
  const idx = categoryTabs.value.findIndex((c) => c.id === state.categoryId)
  filters.categoryIdx = idx !== -1 ? idx : 0
  filters.tagIds = state.tagIds
  pageNum.value = state.page
  await nextTick()
  applyingRoute = false
}

async function updateUrl(page: number) {
  if (disposed || !initialized || route.path !== '/articles') return
  if (searchTimer) clearTimeout(searchTimer)
  const nextQuery = { ...route.query }
  for (const key of ['keyword', 'categoryId', 'tagId', 'page']) delete nextQuery[key]
  Object.assign(
    nextQuery,
    writeArticleListQuery({
      keyword: filters.keyword,
      categoryId: categoryTabs.value[filters.categoryIdx]?.id ?? undefined,
      tagIds: filters.tagIds,
      page,
    }),
  )
  await router.replace({ query: nextQuery })
}

watch(
  () => filters.keyword,
  () => {
    if (!initialized || applyingRoute) return
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(() => {
      void updateUrl(1)
    }, 350)
  },
)

watch(
  () => route.query,
  async () => {
    if (!initialized || disposed || route.path !== '/articles') return
    await applyRouteFilters()
    if (!disposed) await fetchArticles()
  },
)

async function changePage(p: number) {
  if (p === pageNum.value) return
  if (p < pageNum.value && !hasPrevious.value) return
  if (p > pageNum.value && !hasNext.value) return
  await updateUrl(p)
  if (disposed) return
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  window.scrollTo({ top: 0, behavior: reduced ? 'auto' : 'smooth' })
}

// ── 预渲染取数 ────────────────────────────────────────────────────────────────
// 预渲染阶段 onMounted 不执行，分类、标签、首页文章都必须在 onServerPrefetch
// 里取，才能进入 /articles 的静态 HTML。预渲染的是无筛选条件的第一页。
const SSG_KEY = 'articleList'

interface ArticleListState {
  categories: CategoryVO[]
  tags: TagVO[]
  page: PageResult<ArticleItemVO>
}

onServerPrefetch(async () => {
  try {
    const [cats, tgs] = await Promise.all([getCategories(), getTags()])
    categories.value = cats
    tags.value = tgs
    const page = await getArticlePage({ pageNum: 1, pageSize: 12 })
    query.applyPrerendered(page)
    writeSSGState<ArticleListState>(route, SSG_KEY, { categories: cats, tags: tgs, page })
  } catch (err) {
    // 静态产物会退化成空壳，构建后的 verify-ssg 会据此让构建失败
    console.error(`[ssg] 文章列表页预渲染取数失败: ${(err as Error).message}`)
  }
})

// hydration：用预渲染的数据初始化首帧，与静态 HTML 保持一致
const prerendered = readSSGState<ArticleListState>(route, SSG_KEY)
if (prerendered) {
  categories.value = prerendered.categories
  tags.value = prerendered.tags
  query.applyPrerendered(prerendered.page)
}

// ── Init ──────────────────────────────────────────────────────────────────────

async function initialize() {
  initializing.value = true
  loadError.value = false
  try {
    // 静态 HTML 按 /articles 无筛选预渲染；带 query 落地时内容对不上，仍需重新请求
    const hasQueryFilters = Boolean(
      route.query.categoryId || route.query.tagId || route.query.keyword || route.query.page,
    )

    if (!prerendered) {
      const [cats, tgs] = await Promise.all([getCategories(), getTags()])
      categories.value = cats
      tags.value = tgs
    }

    if (disposed) return
    await applyRouteFilters()
    if (disposed) return
    initialized = true

    // 注：静态 HTML 是无筛选的第一页，带 query 落地时会先闪一眼未筛选的列表。
    // 曾试过先清空列表让骨架屏顶上，但 list → skeleton → empty 的快速切换会让
    // 外层 <Transition mode="out-in"> 卡住，DOM 停在旧列表上，反而更糟。
    if (!prerendered || hasQueryFilters) await fetchArticles()
  } catch {
    loadError.value = true
  } finally {
    initializing.value = false
  }
}
onMounted(initialize)
onUnmounted(() => {
  disposed = true
  if (searchTimer) clearTimeout(searchTimer)
  if (slowTimer) clearTimeout(slowTimer)
})
</script>

<template>
  <main class="articles-page">
    <header class="journal-page-head container">
      <div>
        <span class="journal-kicker">THE NOTEBOOK</span>
        <h1>文章<span class="page-title-dot">.</span></h1>
      </div>
      <p>折腾笔记，生活随记。</p>
    </header>
    <!-- Sticky filter bar -->
    <div class="filter-bar">
      <div class="filter-bar__row container">
        <!-- Search -->
        <div class="search-wrap" :class="{ 'search-wrap--focused': searchFocused }">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
          >
            <circle cx="11" cy="11" r="7" />
            <line x1="20" y1="20" x2="15.5" y2="15.5" />
          </svg>
          <input
            v-model="filters.keyword"
            class="search-input"
            placeholder="搜索文章…"
            aria-label="搜索文章"
            type="search"
            @keydown.enter="!$event.isComposing && updateUrl(1)"
            @focus="searchFocused = true"
            @blur="searchFocused = false"
          />
          <Transition name="fade">
            <button
              v-if="filters.keyword"
              class="search-clear"
              aria-label="清除搜索"
              @click="filters.keyword = ''"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </Transition>
        </div>

        <!-- Category tabs with sliding indicator -->
        <div class="cat-tabs">
          <button
            v-for="(cat, i) in categoryTabs"
            :key="cat.name"
            class="cat-tab"
            :aria-pressed="i === filters.categoryIdx"
            :class="{ 'cat-tab--active': i === filters.categoryIdx }"
            @click="selectCategory(i)"
          >
            {{ cat.name }}
          </button>
        </div>
      </div>

      <!-- Tag chips row -->
      <div class="filter-bar__tags container">
        <div class="tag-scroll">
          <button
            v-for="tag in tags"
            :key="tag.id"
            class="tag-chip"
            :aria-pressed="filters.tagIds.includes(tag.id)"
            :class="{ 'tag-chip--active': filters.tagIds.includes(tag.id) }"
            @click="toggleTag(tag.id)"
          >
            {{ tag.name }}
          </button>
        </div>
      </div>
      <div class="filter-loading" :class="{ 'filter-loading--active': loading }"></div>
    </div>

    <!-- Content -->
    <div class="container page-content">
      <!-- Result bar -->
      <div class="result-bar">
        <h2 class="sr-only">文章列表</h2>
        <div class="result-info">
          <Transition name="num" mode="out-in">
            <span :key="total" class="result-num">{{ total }}</span>
          </Transition>
          <span class="result-label">篇文章</span>
          <Transition name="fade">
            <span v-if="filters.tagIds.length > 1" class="result-hint">（同时满足所有标签）</span>
          </Transition>
        </div>
        <Transition name="fade">
          <button v-if="hasFilters" class="clear-btn" @click="clearAll">清除筛选</button>
        </Transition>
      </div>

      <!-- Skeleton (首屏加载，尚无数据) / Empty / List -->
      <div v-if="loadError" class="empty-state" role="alert">
        <p>文章暂时没能加载出来，请重试。</p>
        <button
          class="journal-link"
          :disabled="loading"
          @click="initialized ? fetchArticles() : initialize()"
        >
          重新加载
        </button>
      </div>
      <Transition name="fade" mode="out-in">
        <div v-if="loading && articles.length === 0" key="skeleton">
          <ArticleListSkeleton :rows="6" />
          <Transition name="fade">
            <p v-if="slow" class="slow-hint">加载较慢，请稍候…</p>
          </Transition>
        </div>

        <div v-else-if="articles.length === 0 && !loadError" key="empty" class="empty-state">
          <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="8" y="12" width="48" height="40" rx="2" />
            <line x1="20" y1="24" x2="44" y2="24" />
            <line x1="20" y1="32" x2="36" y2="32" />
          </svg>
          <p>暂无相关文章</p>
        </div>

        <div v-else-if="articles.length" key="list" :class="{ 'list--loading': loading }">
          <!-- Article list -->
          <div class="article-list">
            <ArticleRow v-for="article in articles" :key="article.id" :article="article" />
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="pagination">
            <button
              class="page-btn page-btn--arrow"
              aria-label="上一页"
              :disabled="!hasPrevious"
              @click="changePage(pageNum - 1)"
            >
              ←
            </button>
            <template v-for="(p, i) in visiblePages" :key="i">
              <span v-if="p === '…'" class="page-ellipsis">…</span>
              <button
                v-else
                class="page-btn"
                :class="{ 'page-btn--active': p === pageNum }"
                :aria-current="p === pageNum ? 'page' : undefined"
                @click="changePage(p as number)"
              >
                {{ p }}
              </button>
            </template>
            <button
              class="page-btn page-btn--arrow"
              aria-label="下一页"
              :disabled="!hasNext"
              @click="changePage(pageNum + 1)"
            >
              →
            </button>
          </div>
        </div>
      </Transition>
    </div>
  </main>
</template>

<style scoped>
.articles-page {
  min-height: 80vh;
  padding-top: var(--spacing-header-height);
}
.page-title-dot {
  color: var(--color-accent);
}
.filter-bar {
  border-top: 1px solid var(--color-border-strong);
  border-bottom: 1px solid var(--color-border);
  position: relative;
  background: var(--color-bg);
}
.filter-bar__row {
  display: flex;
  align-items: center;
  gap: 32px;
  padding-top: 20px;
  padding-bottom: 16px;
}
.search-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid var(--color-border-strong);
  width: 260px;
  padding: 10px 0;
  flex: none;
}
.search-wrap--focused {
  border-color: var(--color-accent);
}
.search-wrap > svg {
  width: 18px;
  height: 18px;
  color: var(--color-text-muted);
  flex: none;
}
.search-input {
  flex: 1;
  min-width: 0;
  width: 100%;
  border: none;
  background: transparent;
  font-size: 15px;
  color: var(--color-text-primary);
}
.search-input::placeholder {
  color: var(--color-text-muted);
}
.search-clear {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  color: var(--color-text-muted);
}
.search-clear svg {
  width: 14px;
  height: 14px;
}
.cat-tabs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-left: auto;
  gap: 8px;
}
.cat-tab {
  min-height: 40px;
  padding: 8px 18px;
  border: 1px solid transparent;
  font-size: 14px;
  color: var(--color-text-secondary);
  transition:
    color 0.2s,
    background 0.2s;
}
.cat-tab:hover {
  color: var(--color-accent);
}
.cat-tab--active {
  background: var(--color-text-primary);
  color: var(--color-bg);
}
.cat-tab--active:hover {
  color: var(--color-bg);
}
.filter-bar__tags {
  padding-bottom: 20px;
}
.tag-scroll {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 2px 0 4px;
}
.tag-chip {
  padding: 5px 10px;
  min-height: 32px;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  font-size: 12px;
  white-space: nowrap;
  transition:
    border-color 0.2s,
    color 0.2s;
}
.tag-chip::before {
  content: '#';
  opacity: 0.55;
  margin-right: 4px;
}
.tag-chip:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.tag-chip--active {
  background: var(--journal-soft);
  color: var(--color-accent);
  border-color: var(--color-accent);
}
.filter-loading {
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}
.filter-loading--active {
  opacity: 1;
}
.filter-loading::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, var(--color-accent), transparent);
  animation: sweep 1.2s infinite;
}
@keyframes sweep {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(100%);
  }
}
.page-content {
  padding-top: 32px;
  padding-bottom: 100px;
}
.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 32px;
}
.result-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.result-num {
  font: italic 28px var(--font-editorial);
  color: var(--color-accent);
}
.result-label,
.result-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}
.clear-btn {
  color: var(--color-accent);
  font-size: 13px;
  padding: 8px 0;
}
.article-list {
  max-width: 1000px;
  margin-left: auto;
}
.list--loading {
  opacity: 0.45;
  pointer-events: none;
  transition: opacity 0.2s;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 80px 0;
  color: var(--color-text-muted);
}
.empty-state svg {
  width: 56px;
  height: 56px;
  opacity: 0.6;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 40px;
}
.page-btn {
  min-width: 40px;
  height: 40px;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  font-size: 14px;
  padding: 0 8px;
  transition:
    color 0.2s,
    border-color 0.2s;
}
.page-btn:hover:not(:disabled) {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.page-btn--active {
  background: var(--color-accent);
  color: var(--color-text-on-accent);
  border-color: var(--color-accent);
}
.page-btn--active:hover:not(:disabled) {
  color: var(--color-text-on-accent);
}
.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.page-ellipsis {
  padding: 0 4px;
  color: var(--color-text-muted);
}
.slow-hint {
  color: var(--color-text-muted);
  font-size: 14px;
  margin-top: 20px;
}
.num-enter-active,
.num-leave-active,
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}
.num-enter-from,
.num-leave-to,
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
@media (max-width: 768px) {
  .filter-bar__row {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  .search-wrap {
    width: 100%;
  }
  .cat-tabs {
    margin: 0;
    gap: 4px;
  }
  .cat-tab {
    padding: 7px 15px;
  }
  .page-content {
    padding-top: 24px;
    padding-bottom: 64px;
  }
  .pagination {
    justify-content: center;
    gap: 5px;
  }
  .page-btn {
    min-width: 36px;
    height: 40px;
  }
}
</style>
