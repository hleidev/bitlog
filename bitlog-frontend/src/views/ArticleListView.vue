<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useSeoMeta, useHead } from '@unhead/vue'
import { getArticlePage, type ArticleItemVO } from '@/api/article'
import { getCategories, type CategoryVO } from '@/api/category'
import { getTags, type TagVO } from '@/api/tag'

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
let skipWatch = true

// ── Metadata ──────────────────────────────────────────────────────────────────

const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])

const categoryTabs = computed(() => [{ id: null as number | null, name: '全部' }, ...categories.value])

// ── Filter state ──────────────────────────────────────────────────────────────

const filterSearch = ref('')
const filterCategoryIdx = ref(0)
const filterTagIds = ref<number[]>([])
const searchFocused = ref(false)

const filterCategoryId = computed(
  () => categoryTabs.value[filterCategoryIdx.value]?.id ?? null,
)

const hasFilters = computed(
  () => filterSearch.value || filterCategoryIdx.value !== 0 || filterTagIds.value.length > 0,
)

function clearAll() {
  filterSearch.value = ''
  filterCategoryIdx.value = 0
  filterTagIds.value = []
  nextTick(updateIndicator)
}

function toggleTag(tagId: number) {
  const idx = filterTagIds.value.indexOf(tagId)
  if (idx === -1) filterTagIds.value = [...filterTagIds.value, tagId]
  else filterTagIds.value = filterTagIds.value.filter((id) => id !== tagId)
}

// ── Sliding category indicator ────────────────────────────────────────────────

const tabEls = ref<HTMLButtonElement[]>([])
const indicatorStyle = ref({ left: '4px', width: '60px', opacity: '0' })

async function selectCategory(idx: number) {
  filterCategoryIdx.value = idx
  await nextTick()
  updateIndicator()
}

function updateIndicator() {
  const el = tabEls.value[filterCategoryIdx.value]
  if (!el) return
  indicatorStyle.value = { left: `${el.offsetLeft}px`, width: `${el.offsetWidth}px`, opacity: '1' }
}

// ── Article list ──────────────────────────────────────────────────────────────

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}`
}

const PAGE_SIZE = 12
const pageNum = ref(1)
const loading = ref(false)
const articles = ref<ArticleItemVO[]>([])
const totalElements = ref(0)
const totalPages = ref(1)
const hasPrevious = ref(false)
const hasNext = ref(false)

async function fetchArticles() {
  loading.value = true
  try {
    const res = await getArticlePage({
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE,
      categoryId: filterCategoryId.value ?? undefined,
      tagIds: filterTagIds.value.length > 0 ? filterTagIds.value : undefined,
      keyword: filterSearch.value.trim() || undefined,
    })
    articles.value = res.content
    totalElements.value = res.totalElements
    totalPages.value = res.totalPages
    hasPrevious.value = res.hasPrevious
    hasNext.value = res.hasNext
  } finally {
    loading.value = false
  }
}

let searchTimer: ReturnType<typeof setTimeout> | null = null

watch(filterSearch, () => {
  if (skipWatch) return
  pageNum.value = 1
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(fetchArticles, 350)
})

watch([filterCategoryIdx, filterTagIds], () => {
  if (skipWatch) return
  pageNum.value = 1
  fetchArticles()
})

// ── Sync filter state from URL on navigation without component remount ──────
watch(
  () => route.query,
  (q) => {
    if (skipWatch) return
    const { categoryId, tagId, keyword } = q
    if (keyword) filterSearch.value = keyword as string
    else filterSearch.value = ''

    if (categoryId) {
      const idx = categoryTabs.value.findIndex((c) => c.id === Number(categoryId))
      filterCategoryIdx.value = idx !== -1 ? idx : 0
    } else {
      filterCategoryIdx.value = 0
    }

    if (tagId) filterTagIds.value = [Number(tagId)]
    else filterTagIds.value = []

    pageNum.value = 1
    fetchArticles()
  },
)

const visiblePages = computed(() => {
  const total = totalPages.value
  const cur = pageNum.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1) as (number | '...')[]
  const pages: (number | '...')[] = [1]
  if (cur > 3) pages.push('...')
  for (let p = Math.max(2, cur - 1); p <= Math.min(total - 1, cur + 1); p++) pages.push(p)
  if (cur < total - 2) pages.push('...')
  pages.push(total)
  return pages
})

function changePage(p: number) {
  if (p === pageNum.value) return
  if (p < pageNum.value && !hasPrevious.value) return
  if (p > pageNum.value && !hasNext.value) return
  pageNum.value = p
  fetchArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// ── Init ──────────────────────────────────────────────────────────────────────

onMounted(async () => {
  const [cats, tgs] = await Promise.all([getCategories(), getTags()])
  categories.value = cats
  tags.value = tgs

  const { categoryId, tagId, keyword } = route.query
  if (keyword) filterSearch.value = keyword as string
  if (categoryId) {
    const idx = categoryTabs.value.findIndex((c) => c.id === Number(categoryId))
    filterCategoryIdx.value = idx !== -1 ? idx : 0
  } else {
    filterCategoryIdx.value = 0
  }
  if (tagId) filterTagIds.value = [Number(tagId)]

  skipWatch = false
  await nextTick()
  updateIndicator()
  fetchArticles()
})
</script>

<template>
  <div class="articles-page view-enter">
    <!-- Sticky filter bar -->
    <div class="filter-bar">
      <div class="filter-bar__row container">
        <!-- Search -->
        <div class="search-wrap" :class="{ 'search-wrap--focused': searchFocused }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
            <circle cx="11" cy="11" r="7" /><line x1="20" y1="20" x2="15.5" y2="15.5" />
          </svg>
          <input
            v-model="filterSearch"
            class="search-input"
            placeholder="搜索文章..."
            @focus="searchFocused = true"
            @blur="searchFocused = false"
          />
          <Transition name="fade">
            <button v-if="filterSearch" class="search-clear" @click="filterSearch = ''" tabindex="-1">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
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
            :ref="(el) => { if (el) tabEls[i] = el as HTMLButtonElement }"
            class="cat-tab"
            :class="{ 'cat-tab--active': i === filterCategoryIdx }"
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
            :class="{ 'tag-chip--active': filterTagIds.includes(tag.id) }"
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
            <span :key="totalElements" class="result-num">{{ totalElements }}</span>
          </Transition>
          <span class="result-label">篇文章</span>
          <Transition name="fade">
            <span v-if="filterTagIds.length > 1" class="result-hint">（同时满足所有标签）</span>
          </Transition>
        </div>
        <Transition name="fade">
          <button v-if="hasFilters" class="clear-btn" @click="clearAll">清除过滤</button>
        </Transition>
      </div>

      <!-- Empty -->
      <Transition name="fade" mode="out-in">
        <div v-if="!loading && articles.length === 0" key="empty" class="empty-state">
          <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="8" y="12" width="48" height="40" rx="2" />
            <line x1="20" y1="24" x2="44" y2="24" />
            <line x1="20" y1="32" x2="36" y2="32" />
          </svg>
          <p>暂无相关文章</p>
        </div>

        <div v-else key="list" :class="{ 'list--loading': loading }">
          <!-- Article list -->
          <div class="article-list">
            <RouterLink
              v-for="article in articles"
              :key="article.id"
              :to="`/article/${article.id}`"
              class="article-row"
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

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="pagination">
            <button
              class="page-btn page-btn--arrow"
              :disabled="!hasPrevious"
              @click="changePage(pageNum - 1)"
            >←</button>
            <template v-for="(p, i) in visiblePages" :key="i">
              <span v-if="p === '...'" class="page-ellipsis">…</span>
              <button
                v-else
                class="page-btn"
                :class="{ 'page-btn--active': p === pageNum }"
                @click="changePage(p as number)"
              >{{ p }}</button>
            </template>
            <button
              class="page-btn page-btn--arrow"
              :disabled="!hasNext"
              @click="changePage(pageNum + 1)"
            >→</button>
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
  background: rgba(250, 249, 247, 0.92);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--color-border-light);
}

[data-theme='dark'] .filter-bar {
  background: rgba(14, 12, 11, 0.92);
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
  transition: width 0.3s ease, border-color var(--transition-base);
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
  border-radius: 2px;
  border: 1px solid var(--color-border-light);
  transition: left 0.25s cubic-bezier(0.4, 0, 0.2, 1),
              width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.15s;
  pointer-events: none;
}

.cat-tab {
  position: relative;
  z-index: 1;
  padding: 6px 14px;
  border-radius: 2px;
  font-size: 13px;
  font-family: var(--font-sans);
  color: var(--color-text-muted);
  white-space: nowrap;
  transition: color var(--transition-base);
  cursor: pointer;
}

.cat-tab--active {
  color: var(--color-text-primary);
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
  color: #fff;
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
  from { transform: translateX(-100%); }
  to   { transform: translateX(100%); }
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

.article-row {
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 28px 0;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
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
  color: #fff;
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
    overflow-x: auto;
    scrollbar-width: none;
  }

  .cat-tabs::-webkit-scrollbar {
    display: none;
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
