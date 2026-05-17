<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import ArticleCard from '@/components/home/ArticleCard.vue'
import { getArticlePage, type ArticleItemVO } from '@/api/article'
import { getCategories, type CategoryVO } from '@/api/category'
import { getTags, type TagVO } from '@/api/tag'

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

const PAGE_SIZE = 8
const pageNum = ref(1)
const loading = ref(false)
const articles = ref<ArticleItemVO[]>([])
const totalElements = ref(0)
const totalPages = ref(1)

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
  } finally {
    loading.value = false
  }
}

// Debounce search input; category/tag changes fetch immediately
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
  if (p < 1 || p > totalPages.value || p === pageNum.value) return
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
    if (idx !== -1) filterCategoryIdx.value = idx
  }
  if (tagId) filterTagIds.value = [Number(tagId)]

  skipWatch = false
  await nextTick()
  updateIndicator()
  fetchArticles()
})
</script>

<template>
  <div class="articles-page">
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
            <rect x="8" y="12" width="48" height="40" rx="4" />
            <line x1="20" y1="24" x2="44" y2="24" />
            <line x1="20" y1="32" x2="36" y2="32" />
          </svg>
          <p>暂无相关文章</p>
        </div>

        <!-- Article list with enter/leave animation -->
        <div v-else key="list" :class="{ 'list--loading': loading }">
          <TransitionGroup name="article" tag="div" class="article-list">
            <ArticleCard v-for="a in articles" :key="a.id" :article="a" />
          </TransitionGroup>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="pagination">
            <button
              class="page-btn page-btn--arrow"
              :disabled="pageNum <= 1"
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
              :disabled="pageNum >= totalPages"
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
  background: var(--color-bg);
  padding-top: var(--spacing-header-height);
}

/* ── Filter bar ─────────────────────────────────────────────────────────── */

.filter-bar {
  position: sticky;
  top: var(--spacing-header-height);
  z-index: 100;
  background: rgba(249, 248, 245, 0.88);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--color-border-light);
}

[data-theme='dark'] .filter-bar {
  background: rgba(15, 17, 23, 0.88);
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
  border-radius: 10px;
  padding: 7px 12px;
  width: 180px;
  transition: width 0.3s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  flex-shrink: 0;
}

.search-wrap--focused {
  width: 240px;
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(74, 141, 183, 0.12);
}

.search-wrap svg {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  color: var(--color-text-faint);
  transition: color 0.2s;
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
  transition: color 0.2s;
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
  border-radius: 10px;
  padding: 4px;
  gap: 2px;
}

.cat-tabs__indicator {
  position: absolute;
  top: 4px;
  bottom: 4px;
  background: var(--color-bg-card);
  border-radius: 7px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
  transition: left 0.25s cubic-bezier(0.4, 0, 0.2, 1),
              width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.15s;
  pointer-events: none;
}

.cat-tab {
  position: relative;
  z-index: 1;
  padding: 6px 14px;
  border-radius: 7px;
  font-size: 13px;
  font-family: var(--font-sans);
  color: var(--color-text-muted);
  white-space: nowrap;
  transition: color 0.2s ease;
  cursor: pointer;
}

.cat-tab--active {
  color: var(--color-text-primary);
  font-weight: 600;
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
  background: var(--color-bg-card);
  color: var(--color-text-muted);
  white-space: nowrap;
  flex-shrink: 0;
  cursor: pointer;
  transition: all 0.2s ease;
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

/* ── Content ──────────────────────────────────────────────────────────────── */

.page-content {
  padding-top: 36px;
  padding-bottom: 80px;
}

.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.result-info {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.result-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1;
}

.result-label {
  font-size: 14px;
  color: var(--color-text-faint);
}

.result-hint {
  font-size: 12px;
  color: var(--color-text-faint);
  margin-left: 4px;
}

.clear-btn {
  font-size: 12px;
  color: var(--color-text-faint);
  border: 1px dashed var(--color-border);
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: var(--font-sans);
}

.clear-btn:hover {
  border-color: #e57373;
  color: #e57373;
}

/* Article list */
.list--loading {
  opacity: 0.5;
  pointer-events: none;
  transition: opacity 0.2s ease;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: relative;
}

/* Empty state */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 80px 0;
  color: var(--color-text-faint);
}

.empty-state svg {
  width: 52px;
  height: 52px;
  opacity: 0.35;
}

.empty-state p {
  font-size: 14px;
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 48px;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  background: var(--color-bg-card);
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
  color: #fff !important;
}

.page-btn--arrow {
  font-size: 16px;
}

.page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.page-ellipsis {
  color: var(--color-text-faint);
  padding: 0 4px;
  user-select: none;
}

/* ── Transitions ──────────────────────────────────────────────────────────── */

/* Article list items */
.article-enter-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.article-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
  position: absolute;
  width: 100%;
}

.article-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.article-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.article-move {
  transition: transform 0.3s ease;
}

/* Result number */
.num-enter-active,
.num-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.num-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.num-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* Generic fade */
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
}
</style>
