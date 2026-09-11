<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import { useRowMenu } from '@/admin/composables/useRowMenu'
import { formatDateTime } from '@/utils/format'
import AdminIcon from '@/admin/components/AdminIcon.vue'
import AdminListHeader from '@/admin/components/AdminListHeader.vue'
import AdminPagination from '@/admin/components/AdminPagination.vue'
import AdminSelectionBar from '@/admin/components/AdminSelectionBar.vue'
import { useListQuery } from '@/composables/useListQuery'
import {
  getMyArticles,
  getMyArticleStats,
  updateArticlesStatus,
  deleteArticles,
  generateAiMetadata,
  updateArticleMeta,
  type ArticleVO,
  type ArticleCounts,
  type ArticleStatus,
} from '@/api/admin/article'
import { ApiError } from '@/utils/request'
import ArticleMetaDialog from '@/admin/components/ArticleMetaDialog.vue'
import {
  articleStateLabel,
  hasUnpublishedChanges,
  articlePreviewPath,
} from '@/admin/utils/articleState'

const router = useRouter()
const toast = useToast()
const confirm = useConfirm()

/** 本页量词，批量条与操作提示共用 */
const UNIT = '篇'

const { openMenuId, menuRef, menuId, menuStyle, toggleMenu, closeMenu } = useRowMenu()

// ── State ─────────────────────────────────────────────────────────────────────
const counts = ref<ArticleCounts>({ total: 0, published: 0, draft: 0 })
const countsReady = ref(false)
const loadFailed = ref(false)
let countRequestId = 0
let pageRequestId = 0
const selected = reactive(new Set<number>())

// ── Tabs ──────────────────────────────────────────────────────────────────────
type TabKey = 'all' | 'published' | 'draft'

const TAB_STATUS: Record<TabKey, ArticleStatus | undefined> = {
  all: undefined,
  published: 'PUBLISHED',
  draft: 'DRAFT',
}

// ── Query ─────────────────────────────────────────────────────────────────────
const query = useListQuery({
  filters: { tab: 'all' as TabKey, keyword: '' },
  toParams: (f) => ({ status: TAB_STATUS[f.tab], keyword: f.keyword }),
  fetch: async (params) => {
    const id = ++pageRequestId
    const page = await getMyArticles(params)
    if (id === pageRequestId) loadFailed.value = false
    return page
  },
  sortField: 'UPDATE_TIME',
  debounce: ['keyword'],
  syncUrl: true,
  onFiltersApplied: () => {
    clearSelection()
    void fetchTabCounts()
  },
  sanitize: (f) => {
    // hasOwn 而非 in：in 走原型链，?tab=constructor 会被放行
    if (!Object.hasOwn(TAB_STATUS, f.tab)) f.tab = 'all'
  },
  onError: (err) => {
    loadFailed.value = true
    handleApiError(err, '加载文章失败')
  },
})

const { filters, loading, pageNum, pageSize, total, totalPages, pageNumbers } = query
const articles = query.items
const menuRow = computed(() => articles.value.find((row) => row.id === openMenuId.value))
const fetchArticles = query.load

// 顺序：全部 → 主要工作对象 → 其余。计数恒显示，含 0
const tabs = computed(() => [
  { key: 'all', label: '全部', count: countsReady.value ? counts.value.total : undefined },
  {
    key: 'published',
    label: '已发布',
    count: countsReady.value ? counts.value.published : undefined,
  },
  { key: 'draft', label: '未发布', count: countsReady.value ? counts.value.draft : undefined },
])

// AdminListHeader 的 model 是 string，这里做一层窄化桥接；切 tab 时清掉选中
const activeTab = computed({
  get: (): string => filters.tab,
  set: (key: string) => {
    if (filters.tab === key) return
    clearSelection()
    filters.tab = key as TabKey
  },
})

async function fetchTabCounts() {
  const id = ++countRequestId
  countsReady.value = false
  try {
    const result = await getMyArticleStats({ keyword: filters.keyword })
    if (id !== countRequestId) return
    counts.value = result
    countsReady.value = true
  } catch {
    /* 统计失败不影响主流程 */
  }
}

onMounted(fetchTabCounts)
onBeforeUnmount(() => {
  countRequestId++
})

function handleSearch() {
  query.applyFilters()
}

// 重置 = 清空全部筛选并回到「全部」，与其余列表页一致
function handleReset() {
  clearSelection()
  filters.keyword = ''
  filters.tab = 'all'
  query.applyFilters()
}

// ── Selection ─────────────────────────────────────────────────────────────────
const allChecked = computed(
  () => articles.value.length > 0 && articles.value.every((a) => selected.has(a.id)),
)
const someChecked = computed(
  () => articles.value.some((a) => selected.has(a.id)) && !allChecked.value,
)

// ── Status derivation ──────────────────────────────────────────────────────────
// 后端 status 字段只有 DRAFT / PUBLISHED 二值,但列表要表达三种状态:
//   1) published       —  publishedVersionId !== null
//   2) draft (on top)  —  publishedVersionId !== null && latestVersionId !== publishedVersionId
//   3) pure draft      —  publishedVersionId === null
// 提供统一函数避免各处重复推导,并保证和未来 API 字段调整同步。
function isPureDraft(row: ArticleVO): boolean {
  return row.publishedVersionId === null
}

function toggleAll() {
  if (allChecked.value) articles.value.forEach((a) => selected.delete(a.id))
  else articles.value.forEach((a) => selected.add(a.id))
}

function toggleRow(id: number) {
  if (selected.has(id)) selected.delete(id)
  else selected.add(id)
}

function clearSelection() {
  selected.clear()
}

const selectedCount = computed(() => selected.size)
const selectedIds = computed(() => [...selected])

// ── Error handling ────────────────────────────────────────────────────────────
function handleApiError(err: unknown, fallback = '操作失败') {
  if (err instanceof ApiError) {
    if (err.code === 43001) {
      toast.error('文章不存在，列表已刷新')
      fetchArticles()
      fetchTabCounts()
    } else if (err.code === 43003) toast.error('无权操作该文章')
    else toast.error(err.message || fallback)
    return
  }
  toast.error(fallback)
}

// ── Single row actions ────────────────────────────────────────────────────────
function handlePreview(row: ArticleVO) {
  window.open(articlePreviewPath(row), '_blank', 'noopener')
}

async function handleTogglePublish(row: ArticleVO) {
  closeMenu(true)
  const next: ArticleStatus = row.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  if (next === 'DRAFT') {
    try {
      await confirm(`确认撤下「${row.title}」？文章内容和发布信息会保留。`, '撤下文章', {
        confirmText: '撤下',
      })
    } catch {
      return
    }
  }
  try {
    await updateArticlesStatus([row.id], next)
    toast.success(next === 'PUBLISHED' ? '文章已发布' : '文章已撤下')
    fetchArticles()
    fetchTabCounts()
  } catch (err) {
    handleApiError(err, '操作失败')
  }
}

async function handleDelete(row: ArticleVO) {
  closeMenu(true)
  try {
    await confirm(`确认删除「${row.title}」？`, '删除文章', { confirmText: '删除', danger: true })
  } catch {
    return
  }
  try {
    await deleteArticles([row.id])
    toast.success('文章已删除')
    fetchArticles()
    fetchTabCounts()
  } catch (err) {
    handleApiError(err, '删除失败')
  }
}

// ── Batch actions ─────────────────────────────────────────────────────────────
async function handleBatchPublish(publish: boolean) {
  const next: ArticleStatus = publish ? 'PUBLISHED' : 'DRAFT'
  const label = publish ? '发布' : '撤下'
  const ids = selectedIds.value
  try {
    await confirm(`确认${label}选中的 ${ids.length} 篇文章？`, `批量${label}`, {
      confirmText: label,
    })
  } catch {
    return
  }
  try {
    await updateArticlesStatus(ids, next)
    toast.success(`已${label} ${ids.length} 篇文章`)
    clearSelection()
    fetchArticles()
    fetchTabCounts()
  } catch (err) {
    handleApiError(err, `批量${label}失败`)
  }
}

async function handleBatchDelete() {
  const ids = selectedIds.value
  try {
    await confirm(`确认删除选中的 ${ids.length} 篇文章？`, '批量删除', {
      confirmText: '删除',
      danger: true,
    })
  } catch {
    return
  }
  try {
    await deleteArticles(ids)
    toast.success(`已删除 ${ids.length} 篇文章`)
    clearSelection()
    fetchArticles()
    fetchTabCounts()
  } catch (err) {
    handleApiError(err, '批量删除失败')
  }
}

// ── Quick meta edit ───────────────────────────────────────────────────────────
const metaModalVisible = ref(false)
const metaModalSaving = ref(false)
const metaArticle = ref<ArticleVO | null>(null)
const metaDialogRef = ref<InstanceType<typeof ArticleMetaDialog> | null>(null)

const metaSummary = ref('')
const metaCategory = ref<{ id: number; name: string } | null>(null)
const metaTags = ref<{ id: number; name: string }[]>([])

// AI state
const aiGenerating = ref(false)
const aiSummaryResult = ref<string | null>(null)
const aiCatResult = ref<{ id: number; name: string } | false | null>(null)
const aiTagsResult = ref<{
  existing: Array<{ id: number; name: string }>
  suggested: string[]
} | null>(null)

function openMetaModal(row: ArticleVO) {
  metaArticle.value = row
  metaSummary.value = row.summary ?? ''
  metaCategory.value = row.category ?? null
  metaTags.value = [...row.tags]
  aiSummaryResult.value = null
  aiCatResult.value = null
  aiTagsResult.value = null
  aiGenerating.value = false
  metaModalVisible.value = true
}

async function runAiRecommend() {
  aiGenerating.value = true
  aiSummaryResult.value = null
  aiCatResult.value = null
  aiTagsResult.value = null
  try {
    const data = await generateAiMetadata(metaArticle.value!.id)
    aiSummaryResult.value = data.summary
    aiCatResult.value = data.category ?? false
    aiTagsResult.value = { existing: data.tags, suggested: data.suggestedTags }
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message || 'AI 推荐失败' : 'AI 推荐失败')
  } finally {
    aiGenerating.value = false
  }
}

function acceptAiSummary() {
  if (!aiSummaryResult.value) return
  metaDialogRef.value?.setSummary(aiSummaryResult.value)
  aiSummaryResult.value = null
}

function applyAiCategory() {
  if (!aiCatResult.value) return
  metaDialogRef.value?.setCategory(aiCatResult.value as { id: number; name: string })
  aiCatResult.value = null
}

function applyAiExistingTag(tag: { id: number; name: string }) {
  metaDialogRef.value?.addTag(tag)
}

async function applyAiSuggestedTag(name: string) {
  await metaDialogRef.value?.createAndAddTag(name)
}

// 已应用的 AI 建议：existing 按 id 比对，suggested 建标签前无 id，按 name 比对
const appliedTagIds = computed(
  () => new Set(metaDialogRef.value?.selectedTags?.map((t) => t.id) ?? []),
)
const appliedTagNames = computed(
  () => new Set(metaDialogRef.value?.selectedTags?.map((t) => t.name) ?? []),
)

async function handleMetaSave(data: {
  summary: string
  categoryId: number | null
  tagIds: number[]
  category: { id: number; name: string } | null
  tags: { id: number; name: string }[]
}) {
  if (!data.categoryId) {
    toast.warning('请选择文章分类')
    return
  }
  metaModalSaving.value = true
  try {
    await updateArticleMeta(metaArticle.value!.id, {
      summary: data.summary || null,
      categoryId: data.categoryId!,
      tagIds: data.tagIds,
    })
    const idx = articles.value.findIndex((a) => a.id === metaArticle.value!.id)
    if (idx !== -1) {
      articles.value[idx] = {
        ...articles.value[idx],
        summary: data.summary || null,
        category: data.category,
        tags: data.tags,
      }
    }
    metaModalVisible.value = false
    toast.success('已保存')
  } catch (err) {
    handleApiError(err, '保存失败')
  } finally {
    metaModalSaving.value = false
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function formatViews(n: number) {
  if (n >= 10000) return `${(n / 10000).toFixed(1)}w`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}k`
  return String(n)
}
</script>

<template>
  <div class="articles-page">
    <div class="main-card">
      <AdminListHeader
        v-model:active-tab="activeTab"
        v-model:keyword="filters.keyword"
        :tabs="tabs"
        search-placeholder="搜索标题"
        action-label="写文章"
        @search="handleSearch"
        @reset="handleReset"
        @action="router.push('/admin/write')"
      />

      <AdminSelectionBar :count="selectedCount" :unit="UNIT" @clear="clearSelection">
        <button class="ghost-btn ghost-btn--sm" @click="handleBatchPublish(true)">批量发布</button>
        <button class="ghost-btn ghost-btn--sm" @click="handleBatchPublish(false)">批量撤下</button>
        <button class="ghost-btn ghost-btn--sm ghost-btn--danger" @click="handleBatchDelete">
          批量删除
        </button>
      </AdminSelectionBar>

      <!-- ── Table ── -->
      <div v-if="loadFailed && !loading" class="admin-error-state" role="alert">
        <p>文章加载失败，请重试。</p>
        <button class="ghost-btn" @click="fetchArticles">重新加载</button>
      </div>
      <div
        v-else
        class="table-wrap"
        :class="{ 'table-wrap--loading': loading }"
        :aria-busy="loading"
      >
        <div v-if="loading" class="table-loading">
          <svg class="spinner" viewBox="0 0 24 24" fill="none">
            <circle
              cx="12"
              cy="12"
              r="9"
              stroke="currentColor"
              stroke-width="2"
              stroke-dasharray="40"
              stroke-dashoffset="15"
            />
          </svg>
        </div>

        <table class="data-table">
          <thead>
            <tr>
              <th class="col-check">
                <input
                  type="checkbox"
                  class="row-checkbox"
                  aria-label="选择本页全部文章"
                  :checked="allChecked"
                  :indeterminate="someChecked"
                  @change="toggleAll"
                />
              </th>
              <th class="col-main">文章</th>
              <th class="col-status col-status--wide">状态</th>
              <th class="col-num">阅读</th>
              <th class="col-time">更新时间</th>
              <th class="col-actions" />
            </tr>
          </thead>
          <tbody>
            <tr v-if="articles.length === 0 && !loading">
              <td colspan="6" class="empty-cell">
                <div class="empty-state">
                  <AdminIcon name="article" class="empty-icon" />
                  <span>{{
                    filters.keyword
                      ? '没有匹配的文章'
                      : filters.tab === 'draft'
                        ? '没有未发布的文章'
                        : '暂无文章'
                  }}</span>
                </div>
              </td>
            </tr>
            <tr
              v-for="row in articles"
              :key="row.id"
              :class="{ 'row--draft': isPureDraft(row), 'row--selected': selected.has(row.id) }"
            >
              <td class="col-check">
                <input
                  type="checkbox"
                  class="row-checkbox"
                  :aria-label="`选择文章：${row.title}`"
                  :checked="selected.has(row.id)"
                  @change="toggleRow(row.id)"
                />
              </td>
              <td class="col-main" :title="row.title">
                <RouterLink class="article-title" :to="`/admin/write/${row.id}`">
                  {{ row.title }}
                </RouterLink>
                <div class="article-taxonomy">
                  <span>{{ row.category?.name || '未分类' }}</span>
                  <span v-for="tag in row.tags.slice(0, 2)" :key="tag.id">#{{ tag.name }}</span>
                  <span
                    v-if="row.tags.length > 2"
                    :title="row.tags.map((tag) => tag.name).join('、')"
                    >+{{ row.tags.length - 2 }}</span
                  >
                </div>
              </td>
              <td class="col-status col-status--wide">
                <div class="status-cell">
                  <span
                    class="status-badge"
                    :class="isPureDraft(row) ? 'status-badge--muted' : 'status-badge--ok'"
                    >{{ articleStateLabel(row) }}</span
                  >
                  <span v-if="hasUnpublishedChanges(row)" class="unpublished-note"
                    >有未发布修改</span
                  >
                </div>
              </td>
              <td class="col-num">
                <span class="cell-muted">{{
                  isPureDraft(row) ? '—' : formatViews(row.readCount)
                }}</span>
              </td>
              <td class="col-time">
                <span class="cell-muted" :title="row.updateTime">{{
                  formatDateTime(row.updateTime)
                }}</span>
              </td>
              <td class="col-actions">
                <div class="row-actions">
                  <button class="action-btn" @click="handlePreview(row)">预览</button>
                  <button class="action-btn" @click="openMetaModal(row)">信息</button>
                  <div class="menu-wrap">
                    <button
                      class="more-btn"
                      aria-haspopup="menu"
                      :aria-expanded="openMenuId === row.id"
                      :aria-controls="openMenuId === row.id ? menuId : undefined"
                      title="更多"
                      @click.stop="toggleMenu(row.id, $event)"
                    >
                      <AdminIcon name="more" />
                    </button>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Mobile cards (同源数据,CSS 在 <768px 隐藏表格显示卡片) -->
        <div v-if="!articles.length && !loading" class="mobile-empty">
          {{
            filters.keyword
              ? '没有匹配的文章'
              : filters.tab === 'draft'
                ? '没有未发布的文章'
                : '暂无文章'
          }}
        </div>
        <ul class="data-cards">
          <li
            v-for="row in articles"
            :key="`c-${row.id}`"
            class="data-card"
            :class="{
              'data-card--draft': isPureDraft(row),
              'data-card--selected': selected.has(row.id),
            }"
          >
            <div class="data-card__head">
              <input
                type="checkbox"
                class="row-checkbox"
                :aria-label="`选择文章：${row.title}`"
                :checked="selected.has(row.id)"
                @change="toggleRow(row.id)"
              />
              <RouterLink class="data-card__title" :to="`/admin/write/${row.id}`">{{
                row.title
              }}</RouterLink>
            </div>
            <div class="data-card__meta">
              <span v-if="row.category" class="category-tag">{{ row.category.name }}</span>
              <div v-if="row.tags.length > 0" class="data-card__tags">
                <span v-for="tag in row.tags" :key="tag.id" class="tag-chip">{{ tag.name }}</span>
              </div>
              <div class="data-card__status">
                <span
                  class="status-badge"
                  :class="isPureDraft(row) ? 'status-badge--muted' : 'status-badge--ok'"
                  >{{ articleStateLabel(row) }}</span
                >
                <span v-if="hasUnpublishedChanges(row)" class="unpublished-note">有未发布修改</span>
                <span class="cell-muted">{{ formatDateTime(row.updateTime) }}</span>
              </div>
            </div>
            <div class="data-card__actions">
              <button class="action-btn" @click="handlePreview(row)">预览</button>
              <button class="action-btn" @click="openMetaModal(row)">信息</button>
              <button v-if="!isPureDraft(row)" class="action-btn" @click="handleTogglePublish(row)">
                撤下
              </button>
              <button class="action-btn action-btn--danger" @click="handleDelete(row)">删除</button>
            </div>
          </li>
        </ul>
      </div>

      <!-- ── Pagination ── -->
      <AdminPagination
        :total="total"
        :page-num="pageNum"
        :page-size="pageSize"
        :total-pages="totalPages"
        :page-numbers="pageNumbers"
        unit="篇"
        @go="query.goPage"
        @size="query.setPageSize"
      />
    </div>
  </div>

  <ArticleMetaDialog
    ref="metaDialogRef"
    v-model:visible="metaModalVisible"
    :summary="metaSummary"
    :category="metaCategory"
    :tags="metaTags"
    :saving="metaModalSaving"
    title="编辑文章信息"
    confirm-text="保存"
    @save="handleMetaSave"
  >
    <template #header-extra>
      <button
        class="ai-trigger-btn"
        :class="{ 'ai-trigger-btn--loading': aiGenerating }"
        :disabled="aiGenerating"
        type="button"
        title="AI 智能填写"
        @click="runAiRecommend"
      >
        <span
          :style="aiGenerating ? 'display:inline-block;animation:ai-spin 1.2s linear infinite' : ''"
          >✦</span
        >
      </button>
    </template>

    <template #summary-extra>
      <transition name="ai-slide">
        <div v-if="aiSummaryResult" class="pf-ai-inline">
          <p class="pf-ai-inline-body">{{ aiSummaryResult }}</p>
          <div class="pf-ai-inline-actions">
            <button
              type="button"
              class="ai-action ai-action--dismiss"
              @click="aiSummaryResult = null"
            >
              忽略
            </button>
            <button type="button" class="ai-action ai-action--primary" @click="acceptAiSummary">
              应用
            </button>
          </div>
        </div>
      </transition>
    </template>

    <template #category-extra>
      <transition name="ai-slide">
        <div v-if="aiCatResult !== null" class="pf-ai-inline pf-ai-inline--row">
          <template v-if="aiCatResult">
            <span class="pf-ai-inline-val">{{ aiCatResult.name }}</span>
            <div class="pf-ai-inline-actions">
              <button
                type="button"
                class="ai-action ai-action--dismiss"
                @click="aiCatResult = null"
              >
                忽略
              </button>
              <button type="button" class="ai-action ai-action--primary" @click="applyAiCategory">
                应用
              </button>
            </div>
          </template>
          <template v-else>
            <span class="pf-ai-inline-no-match">现有分类均不适配，请手动选择</span>
            <button type="button" class="pf-ai-inline-dismiss" @click="aiCatResult = null">
              <svg
                viewBox="0 0 24 24"
                width="12"
                height="12"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
              >
                <path d="M18 6L6 18M6 6l12 12" />
              </svg>
            </button>
          </template>
        </div>
      </transition>
    </template>

    <template #tags-extra>
      <transition name="ai-slide">
        <div v-if="aiTagsResult" class="pf-ai-inline pf-ai-inline--chips">
          <button
            v-for="tag in aiTagsResult.existing"
            :key="tag.id"
            type="button"
            class="ai-meta-chip ai-meta-chip--existing ai-meta-chip--action"
            :class="{ 'ai-meta-chip--applied': appliedTagIds.has(tag.id) }"
            :disabled="appliedTagIds.has(tag.id)"
            @click="applyAiExistingTag(tag)"
          >
            {{ tag.name
            }}<span class="ai-meta-chip__plus">{{ appliedTagIds.has(tag.id) ? '✓' : '+' }}</span>
          </button>
          <button
            v-for="name in aiTagsResult.suggested"
            :key="name"
            type="button"
            class="ai-meta-chip ai-meta-chip--new ai-meta-chip--action"
            :class="{ 'ai-meta-chip--applied': appliedTagNames.has(name) }"
            :disabled="appliedTagNames.has(name)"
            @click="applyAiSuggestedTag(name)"
          >
            {{ name }}<span class="ai-meta-chip__badge">新</span
            ><span class="ai-meta-chip__plus">{{ appliedTagNames.has(name) ? '✓' : '+' }}</span>
          </button>
          <button type="button" class="pf-ai-inline-dismiss" @click="aiTagsResult = null">
            <svg
              viewBox="0 0 24 24"
              width="12"
              height="12"
              fill="none"
              stroke="currentColor"
              stroke-width="2.5"
            >
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
      </transition>
    </template>
  </ArticleMetaDialog>

  <Teleport to="body">
    <div
      v-if="menuRow"
      :id="menuId"
      ref="menuRef"
      role="menu"
      aria-label="文章操作"
      class="dropdown-menu"
      :style="menuStyle"
    >
      <button
        v-if="!isPureDraft(menuRow)"
        role="menuitem"
        tabindex="-1"
        class="menu-item"
        @click="handleTogglePublish(menuRow)"
      >
        撤下
      </button>
      <div v-if="!isPureDraft(menuRow)" class="menu-divider" role="separator" />
      <button
        role="menuitem"
        tabindex="-1"
        class="menu-item menu-item--danger"
        @click="handleDelete(menuRow)"
      >
        删除
      </button>
    </div>
  </Teleport>
</template>

<style scoped>
/* 分类与标签归入标题列，为标题留出可阅读宽度；更窄时在表格内横向滚动。 */
.data-table {
  min-width: 860px;
}
.article-taxonomy {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-top: 8px;
  color: var(--admin-text-muted);
  font-size: 11px;
}
.status-cell {
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}
.unpublished-note {
  font-size: 10px;
  color: var(--admin-warning);
}
.mobile-empty {
  display: none;
  padding: 48px 20px;
  text-align: center;
  color: var(--admin-text-muted);
  font-size: 13px;
}
.data-card__title {
  text-decoration: none;
}
@media (max-width: 767px) {
  .mobile-empty {
    display: block;
  }
}

.articles-page {
  display: flex;
  flex-direction: column;
}

/* ── Header ── */

/* ── Tabs ── */

/* .view-tabs / .view-tab / .tab-count 见 admin/styles/variables.css */

/* ── Search ── */

/* ── Buttons ── */

/* ── Selection bar ── */

.ghost-btn--danger {
  color: var(--admin-danger);
  border-color: rgba(var(--admin-danger-rgb), 0.25);
}

.cancel-btn {
  margin-left: auto;
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.15s;
}

.sel-bar-enter-active,

.sel-bar-enter-from,

/* ── Table ── */


.table-wrap--loading {
  pointer-events: none;
}

.row--draft td:first-child {
  box-shadow: inset 3px 0 0 var(--admin-sidebar-border);
}

/* ── Table cells ── */

.article-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--admin-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
  display: block;
  overflow-wrap: anywhere;
  line-height: 1.7;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.12s;
}

.article-title:hover {
  color: var(--admin-accent);
}
.article-title--draft {
  color: var(--admin-text-subtle);
}

.category-tag {
  display: inline-block;
  font-size: 11.5px;
  color: var(--admin-sidebar-text);
  background: var(--admin-sidebar-hover);
  padding: 2px 8px;
  border-radius: var(--admin-radius);
  white-space: nowrap;
}

.tags-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

/* 标签不上色。强调色只留给「主操作 / 当前位置」——一屏十几个 accent 色块
   之后，右上角的「写文章」主按钮就不再是视觉焦点了。
   与分类的层次靠形状区分：分类是实心 chip，标签是描边 chip。 */
.tag-chip {
  display: inline-flex;
  font-size: 11px;
  font-weight: 500;
  color: var(--admin-text-muted);
  background: transparent;
  border: 1px solid var(--admin-border);
  padding: 1px 6px;
  border-radius: var(--admin-radius);
  white-space: nowrap;
}

/* ── Pagination ── */

/* ── AI (rendered via slots into ArticleMetaDialog) ─────────────────────────── */
.ai-trigger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  background: transparent;
  border: 1px solid var(--admin-border);
  border-radius: 4px;
  cursor: pointer;
  color: var(--admin-accent);
  font-size: 13px;
  font-family: inherit;
  transition:
    background 0.15s,
    border-color 0.15s;
}
.ai-trigger-btn:hover:not(:disabled) {
  background: var(--admin-accent-bg-soft);
  border-color: var(--admin-accent);
}
.ai-trigger-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.pf-ai-inline {
  border: 1px solid var(--admin-border);
  border-radius: 4px;
  background: var(--admin-surface-soft);
  padding: 10px 12px;
}
.pf-ai-inline--row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.pf-ai-inline--chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.pf-ai-inline-body {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--admin-text-primary);
  padding-bottom: 10px;
}
.pf-ai-inline-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: flex-end;
}
.pf-ai-inline--row .pf-ai-inline-actions {
  margin-left: auto;
  flex-shrink: 0;
}
.pf-ai-inline-val {
  font-size: 13px;
  color: var(--admin-text-primary);
  font-weight: 500;
  flex: 1;
}
.pf-ai-inline-no-match {
  font-size: 12px;
  color: var(--admin-text-muted);
  font-style: italic;
  flex: 1;
}
.pf-ai-inline-dismiss {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--admin-text-muted);
  border-radius: var(--admin-radius);
  padding: 0;
  margin-left: auto;
  transition: color 0.15s;
}
.pf-ai-inline-dismiss:hover {
  color: var(--admin-text-primary);
}

.ai-action {
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 4px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}
.ai-action--dismiss {
  color: var(--admin-text-secondary);
  background: var(--admin-surface-2);
  border-color: var(--admin-border);
}
.ai-action--dismiss:hover {
  background: var(--admin-border);
}
.ai-action--primary {
  color: var(--admin-text-on-accent);
  background: var(--admin-accent);
  border-color: var(--admin-accent);
}
.ai-action--primary:hover {
  background: var(--admin-accent-dark);
}

.ai-meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 4px;
  border: 1px solid transparent;
  line-height: 20px;
  font-family: inherit;
}
/* 已存在于标签库 —— 实线 */
.ai-meta-chip--existing {
  color: var(--admin-text-secondary);
  background: var(--admin-surface-2);
  border-color: var(--admin-border);
}
/* 会新建 —— 虚线，配合「新」badge */
.ai-meta-chip--new {
  color: var(--admin-text-secondary);
  background: var(--admin-surface);
  border-color: var(--admin-border-strong);
  border-style: dashed;
}
.ai-meta-chip--action {
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s;
}
.ai-meta-chip--action:not(:disabled):hover {
  border-color: var(--admin-accent);
}
.ai-meta-chip--action:not(:disabled).ai-meta-chip--existing:hover {
  background: var(--admin-accent-bg-soft);
}
.ai-meta-chip--action:not(:disabled).ai-meta-chip--new:hover {
  background: var(--admin-accent-bg-soft);
}
.ai-meta-chip__badge {
  font-size: 10px;
  font-weight: 600;
  color: var(--admin-text-secondary);
  background: var(--admin-border);
  padding: 0 4px;
  border-radius: var(--admin-radius);
}
.ai-meta-chip__plus {
  font-size: 14px;
  font-weight: 400;
  line-height: 1;
  color: var(--admin-text-muted);
  margin-left: 1px;
  transition: color 0.15s;
}
/* accent 只在鼠标表达意图的瞬间出现 */
.ai-meta-chip--action:not(:disabled):hover .ai-meta-chip__plus {
  color: var(--admin-accent);
}
/* 已加入该文章的建议 —— 置灰且不可再点 */
.ai-meta-chip--applied {
  opacity: 0.4;
  cursor: not-allowed;
}

.ai-slide-enter-active,
.ai-slide-leave-active {
  transition:
    opacity 0.18s,
    transform 0.18s;
}
.ai-slide-enter-from,
.ai-slide-leave-to {
  opacity: 0;
  transform: translateY(-3px);
}

@keyframes ai-spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── Mobile ── */

.data-cards {
  display: none;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: stretch;
    padding: 0 12px;
    gap: 0;
  }
  .header-actions {
    padding: 8px 0;
    flex-wrap: wrap;
  }
  .search-input {
    width: 140px;
  }
  .selection-bar {
    flex-wrap: wrap;
    padding: 8px 12px;
  }
  .sel-cancel {
    margin-left: 0;
  }
  .pagination-bar {
    padding: 10px 12px;
    flex-wrap: wrap;
    gap: 6px;
  }
  .page-size-select {
    margin-left: 0;
  }

  /* 切到卡片视图 */
  .data-table {
    display: none;
  }
  .data-cards {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 12px;
  }
  .data-card {
    background: var(--admin-surface-input);
    border: 1px solid var(--admin-sidebar-border);
    border-radius: 4px;
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .data-card--draft {
    border-left: 3px solid var(--admin-sidebar-border);
  }
  .data-card--selected {
    background: var(--admin-accent-bg-subtle);
  }
  .data-card__head {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .data-card__title {
    flex: 1;
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-primary);
    cursor: pointer;
  }
  .data-card__meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    font-size: 12px;
  }
  .data-card__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }
  .data-card__status {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-left: auto;
  }
  .data-card__actions {
    display: flex;
    gap: 2px;
    flex-wrap: wrap;
    border-top: 1px solid var(--admin-sidebar-border);
    padding-top: 8px;
  }
}
</style>
