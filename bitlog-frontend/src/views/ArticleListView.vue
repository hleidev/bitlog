<script setup lang="ts">
import { ref, computed, watch, onMounted, onServerPrefetch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useSeoMeta, useHead } from '@unhead/vue'
import { getArticlePage, type ArticleItemVO } from '@/api/article'
import type { PageResult } from '@/api/types'
import { useListQuery } from '@/composables/useListQuery'
import { getCategories, type CategoryVO } from '@/api/category'
import { getTags, type TagVO } from '@/api/tag'
import { readSSGState, writeSSGState } from '@/utils/ssgState'
import ArticleListSkeleton from '@/components/common/ArticleListSkeleton.vue'
import ArticleRow from '@/components/common/ArticleRow.vue'

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

// ── Metadata ──────────────────────────────────────────────────────────────────

const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])

const categoryTabs = computed(() => [
  { id: null as number | null, name: '全部' },
  ...categories.value,
])

// ── Filter state ──────────────────────────────────────────────────────────────

const searchFocused = ref(false)

const query = useListQuery({
  filters: { keyword: '', categoryIdx: 0, tagIds: [] as number[] },
  toParams: (f) => ({
    keyword: f.keyword,
    categoryId: categoryTabs.value[f.categoryIdx]?.id ?? undefined,
    allTagIds: f.tagIds,
  }),
  fetch: (params) => getArticlePage(params),
  pageSize: 12,
  debounce: ['keyword'],
  debounceMs: 350,
  // SSG：预渲染结果注入首帧，挂载后再决定是否重拉
  immediate: false,
})

const { filters, loading, pageNum, total, totalPages, hasPrevious, hasNext } = query
const articles = query.items
const visiblePages = query.pageNumbers
const fetchArticles = query.load

const hasFilters = computed(
  () => filters.keyword || filters.categoryIdx !== 0 || filters.tagIds.length > 0,
)

function clearAll() {
  filters.keyword = ''
  filters.categoryIdx = 0
  filters.tagIds = []
  nextTick(updateIndicator)
}

function toggleTag(tagId: number) {
  const idx = filters.tagIds.indexOf(tagId)
  if (idx === -1) filters.tagIds = [...filters.tagIds, tagId]
  else filters.tagIds = filters.tagIds.filter((id) => id !== tagId)
}

// ── Sliding category indicator ────────────────────────────────────────────────

const tabEls = ref<HTMLButtonElement[]>([])
const indicatorStyle = ref({ left: '4px', width: '60px', opacity: '0' })

async function selectCategory(idx: number) {
  filters.categoryIdx = idx
  await nextTick()
  updateIndicator()
}

function updateIndicator() {
  const el = tabEls.value[filters.categoryIdx]
  if (!el) return
  indicatorStyle.value = { left: `${el.offsetLeft}px`, width: `${el.offsetWidth}px`, opacity: '1' }
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

// 入站链接契约：文章详情页用 /articles?categoryId=X、?tagId=Y、?keyword=Z 跳进来
function applyRouteFilters(q: typeof route.query) {
  filters.keyword = typeof q.keyword === 'string' ? q.keyword : ''
  const idx = q.categoryId ? categoryTabs.value.findIndex((c) => c.id === Number(q.categoryId)) : -1
  filters.categoryIdx = idx !== -1 ? idx : 0
  filters.tagIds = q.tagId ? [Number(q.tagId)] : []
}

// ── Sync filter state from URL on navigation without component remount ──────
watch(
  () => route.query,
  (q) => {
    applyRouteFilters(q)
  },
)

function changePage(p: number) {
  if (p === pageNum.value) return
  if (p < pageNum.value && !hasPrevious.value) return
  if (p > pageNum.value && !hasNext.value) return
  query.goPage(p)
  window.scrollTo({ top: 0, behavior: 'smooth' })
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

// 换页与筛选后 :key 变化重建列表，让入场动画重播。scroll-driven 的 reveal
// 在这里没用：行早已在视口内，滚动进度已是终态，换新数据不会重新触发。
// watch 建在 applyPrerendered 之后，静态首屏因此不会多播一次。
const listSeq = ref(0)
watch(articles, () => listSeq.value++)

// ── Init ──────────────────────────────────────────────────────────────────────

onMounted(async () => {
  // 静态 HTML 按 /articles 无筛选预渲染；带 query 落地时内容对不上，仍需重新请求
  const hasQueryFilters = Boolean(
    route.query.categoryId || route.query.tagId || route.query.keyword,
  )

  if (!prerendered) {
    const [cats, tgs] = await Promise.all([getCategories(), getTags()])
    categories.value = cats
    tags.value = tgs
  }

  // 赋值触发的过滤监听排在 pre-flush 队列里，必须等它跑完再 start()，否则开关等于没关
  applyRouteFilters(route.query)
  await nextTick()
  query.start()
  updateIndicator()

  // 注：静态 HTML 是无筛选的第一页，带 query 落地时会先闪一眼未筛选的列表。
  // 曾试过先清空列表让骨架屏顶上，但 list → skeleton → empty 的快速切换会让
  // 外层 <Transition mode="out-in"> 卡住，DOM 停在旧列表上，反而更糟。
  if (!prerendered || hasQueryFilters) fetchArticles()
})
</script>

<template>
  <div class="articles-page">
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
            placeholder="搜索文章..."
            @focus="searchFocused = true"
            @blur="searchFocused = false"
          />
          <Transition name="fade">
            <button
              v-if="filters.keyword"
              class="search-clear"
              tabindex="-1"
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
          <div class="cat-tabs__indicator" :style="indicatorStyle"></div>
          <button
            v-for="(cat, i) in categoryTabs"
            :key="cat.name"
            :ref="
              (el) => {
                if (el) tabEls[i] = el as HTMLButtonElement
              }
            "
            class="cat-tab"
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
          <button v-if="hasFilters" class="clear-btn" @click="clearAll">清除过滤</button>
        </Transition>
      </div>

      <!-- Skeleton (首屏加载，尚无数据) / Empty / List -->
      <Transition name="fade" mode="out-in">
        <div v-if="loading && articles.length === 0" key="skeleton">
          <ArticleListSkeleton :rows="6" />
          <Transition name="fade">
            <p v-if="slow" class="slow-hint">加载较慢，仍在努力…</p>
          </Transition>
        </div>

        <div v-else-if="articles.length === 0" key="empty" class="empty-state">
          <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="8" y="12" width="48" height="40" rx="2" />
            <line x1="20" y1="24" x2="44" y2="24" />
            <line x1="20" y1="32" x2="36" y2="32" />
          </svg>
          <p>暂无相关文章</p>
        </div>

        <div v-else key="list" :class="{ 'list--loading': loading }">
          <!-- Article list -->
          <div :key="listSeq" class="article-list stagger">
            <ArticleRow
              v-for="(article, i) in articles"
              :key="article.id"
              :article="article"
              :style="{ '--i': i }"
            />
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="pagination">
            <button
              class="page-btn page-btn--arrow"
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
                @click="changePage(p as number)"
              >
                {{ p }}
              </button>
            </template>
            <button
              class="page-btn page-btn--arrow"
              :disabled="!hasNext"
              @click="changePage(pageNum + 1)"
            >
              →
            </button>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<style scoped>
.articles-page {
  min-height: 100vh;
  padding-top: var(--spacing-header-height);
}

/* ── Filter bar ─────────────────────────────────────────────────────────── */

.filter-bar {
  position: sticky;
  top: var(--spacing-header-height);
  z-index: 100;
  background: rgba(var(--color-bg-rgb), 0.92);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--color-border-light);
}

.filter-bar__row {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-top: 14px;
  padding-bottom: 14px;
}

/* Search */
.search-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 7px 12px;
  width: 180px;
  transition:
    width 0.3s ease,
    border-color var(--transition-base);
  flex-shrink: 0;
}

.search-wrap--focused {
  width: 240px;
  border-color: var(--color-accent);
}

.search-wrap svg {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  color: var(--color-text-faint);
  transition: color var(--transition-base);
}

.search-wrap--focused svg {
  color: var(--color-accent);
}

.search-input {
  flex: 1;
  min-width: 0;
  background: transparent;
  border: none;
  outline: none;
  font-size: 13px;
  color: var(--color-text-primary);
  font-family: var(--font-sans);
}

.search-input::placeholder {
  color: var(--color-text-faint);
}

.search-clear {
  display: flex;
  align-items: center;
  color: var(--color-text-faint);
  transition: color var(--transition-base);
  flex-shrink: 0;
}

.search-clear:hover {
  color: var(--color-text-muted);
}

.search-clear svg {
  width: 12px;
  height: 12px;
}

/* Category tabs */
.cat-tabs {
  position: relative;
  margin-left: auto;
  display: flex;
  align-items: center;
  background: var(--color-bg-hover);
  border-radius: 4px;
  padding: 4px;
  gap: 2px;
}

.cat-tabs__indicator {
  position: absolute;
  top: 4px;
  bottom: 4px;
  background: var(--color-bg-card);
  border-radius: var(--radius-tag);
  border: 1px solid var(--color-border-light);
  transition:
    left 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.15s;
  pointer-events: none;
}

.cat-tab {
  position: relative;
  z-index: 1;
  padding: 6px 14px;
  border-radius: var(--radius-tag);
  font-size: 13px;
  font-family: var(--font-sans);
  color: var(--color-text-muted);
  white-space: nowrap;
  transition: color var(--transition-base);
  cursor: pointer;
}

.cat-tab--active {
  color: var(--color-accent);
  font-weight: 500;
}

/* Tag chips row */
.filter-bar__tags {
  padding-bottom: 12px;
}

.tag-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  padding: 2px 0;
}

.tag-scroll::-webkit-scrollbar {
  display: none;
}

.tag-chip {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: var(--radius-tag);
  border: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text-muted);
  white-space: nowrap;
  flex-shrink: 0;
  cursor: pointer;
  transition: all var(--transition-base);
  font-family: var(--font-sans);
}

.tag-chip:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.tag-chip--active {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: var(--color-text-on-accent);
}

/* 选中态必须自带 hover：否则 .tag-chip:hover(0,2,0) 特异性高于
   .tag-chip--active(0,1,0)，会把文字改回 accent 压在 accent 底上，字直接消失。 */
.tag-chip--active:hover {
  background: var(--color-accent-dark);
  border-color: var(--color-accent-dark);
  color: var(--color-text-on-accent);
}

/* Loading bar */
.filter-loading {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  overflow: hidden;
  opacity: 0;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

.filter-loading--active {
  opacity: 1;
}

.filter-loading::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent 0%, var(--color-accent) 50%, transparent 100%);
  animation: filter-sweep 1.1s ease-in-out infinite;
}

@keyframes filter-sweep {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(100%);
  }
}

/* ── Content ──────────────────────────────────────────────────────────────── */

.page-content {
  padding-top: 40px;
  padding-bottom: 120px;
}

.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.result-info {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.result-num {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1;
}

.result-label {
  font-size: 13px;
  color: var(--color-text-faint);
}

.result-hint {
  font-size: 12px;
  color: var(--color-text-faint);
  margin-left: 4px;
}

.clear-btn {
  font-size: 12px;
  letter-spacing: 0.03em;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  padding: 5px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all var(--transition-base);
  font-family: var(--font-sans);
  background: transparent;
}

.clear-btn:hover {
  border-color: var(--color-text-muted);
  color: var(--color-text-primary);
}

/* ── Article list ──────────────────────────────────────────────────────────── */

.list--loading {
  opacity: 0.4;
  pointer-events: none;
  transition: opacity var(--transition-base);
}

.article-list {
  border-top: 1px solid var(--color-border);
  counter-reset: article-counter;
}

/* revealUp 定义在 global.css：本文件没有同名 keyframes，scoped 不会改写引用。 */
@media (prefers-reduced-motion: no-preference) {
  .stagger > * {
    animation: revealUp var(--transition-reveal) both;
    /* 延迟封顶在第 8 行：再往下本来就要滚动才看得到，继续累加只会让换页拖尾。 */
    animation-delay: calc(38ms * min(var(--i, 0), 7));
  }
}

/* ── Empty state ──────────────────────────────────────────────────────────── */

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 80px 0;
  color: var(--color-text-faint);
}

.empty-state svg {
  width: 48px;
  height: 48px;
  opacity: 0.3;
}

.empty-state p {
  font-size: 14px;
}

/* ── Pagination ───────────────────────────────────────────────────────────── */

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 56px;
}

.page-btn {
  min-width: 34px;
  height: 34px;
  padding: 0 10px;
  border-radius: 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  background: transparent;
  cursor: pointer;
  transition: all var(--transition-base);
  font-family: var(--font-sans);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.page-btn--active {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: var(--color-text-on-accent);
}

/* 同 .tag-chip--active：hover 规则特异性更高，不单独覆盖会让页码字消失 */
.page-btn--active:hover:not(:disabled) {
  background: var(--color-accent-dark);
  border-color: var(--color-accent-dark);
  color: var(--color-text-on-accent);
}

.page-btn--arrow {
  font-size: 15px;
}

.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-ellipsis {
  color: var(--color-text-faint);
  padding: 0 4px;
  user-select: none;
}

/* ── Transitions ──────────────────────────────────────────────────────────── */

.num-enter-active,
.num-leave-active {
  transition: opacity 0.15s ease;
}

.num-enter-from,
.num-leave-to {
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ── Mobile ───────────────────────────────────────────────────────────────── */

@media (max-width: 768px) {
  .articles-page {
    padding-top: 56px;
  }

  .filter-bar__row {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    padding-top: 12px;
    padding-bottom: 10px;
  }

  .search-wrap,
  .search-wrap--focused {
    width: 100%;
  }

  .cat-tabs {
    /* 窄屏是 column + stretch，auto margin 会盖掉 stretch 让它缩成靠右一小条 */
    margin-left: 0;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .cat-tabs::-webkit-scrollbar {
    display: none;
  }
}

.slow-hint {
  margin-top: 20px;
  font-size: 13px;
  color: var(--color-text-faint);
  text-align: center;
}
</style>
