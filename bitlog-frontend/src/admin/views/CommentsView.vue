<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import { formatDateTime } from '@/utils/format'
import AdminIcon from '@/admin/components/AdminIcon.vue'
import AdminListHeader from '@/admin/components/AdminListHeader.vue'
import AdminPagination from '@/admin/components/AdminPagination.vue'
import AdminSelectionBar from '@/admin/components/AdminSelectionBar.vue'
import { useListQuery } from '@/composables/useListQuery'
import {
  getAdminCommentPage,
  getCommentStats,
  updateCommentStatus,
  deleteComments,
  type CommentAdmin,
  type CommentStatus,
} from '@/api/admin/comment'

const toast = useToast()
const confirm = useConfirm()

/** 本页量词，批量条与操作提示共用 */
const UNIT = '条'

const selected = reactive(new Set<number>())

function clearSelection() {
  selected.clear()
}

const query = useListQuery({
  // status 用 0 表示「全部」而非 ''：useListQuery 按默认值类型还原 URL 参数，
  // 字符串默认值会让 ?status=1 变回 '1'，与 <option :value="1"> 匹配不上
  filters: { keyword: '', status: 0 as CommentStatus | 0 },
  toParams: (f) => ({ keyword: f.keyword, status: f.status || undefined }),
  fetch: (params) => getAdminCommentPage(params),
  pageSize: 20,
  debounce: ['keyword'],
  syncUrl: true,
  onFiltersApplied: fetchTabCounts,
  onError: () => toast.error('加载评论失败'),
})

const { filters, loading, pageNum, pageSize, total, totalPages, pageNumbers } = query
const comments = query.items
const fetchComments = query.load

// 每次列表刷新都清掉选中，避免选中项指向已翻页离开的行
watch(comments, clearSelection)

function applyFilter() {
  query.applyFilters()
}

const allSelected = computed(
  () => comments.value.length > 0 && comments.value.every((c) => selected.has(c.id)),
)

function toggleAll() {
  if (allSelected.value) clearSelection()
  else comments.value.forEach((c) => selected.add(c.id))
}

function toggleRow(id: number) {
  if (selected.has(id)) selected.delete(id)
  else selected.add(id)
}

const acting = ref(false)

async function handleToggleStatus(row: CommentAdmin) {
  const next: CommentStatus = row.status === 1 ? 2 : 1
  acting.value = true
  try {
    await updateCommentStatus(row.id, next)
    row.status = next
    toast.success(next === 2 ? '已隐藏' : '已恢复')
    fetchTabCounts()
  } catch {
    toast.error('操作失败')
  } finally {
    acting.value = false
  }
}

async function handleDelete(row: CommentAdmin) {
  try {
    await confirm('删除后读者与管理端都不再可见，确认删除？', '删除评论', {
      confirmText: '删除',
      danger: true,
    })
  } catch {
    return
  }
  try {
    await deleteComments([row.id])
    toast.success('已删除')
    await fetchComments()
    fetchTabCounts()
  } catch {
    toast.error('删除失败')
  }
}

async function handleBatchDelete() {
  const ids = [...selected]
  if (!ids.length) return
  try {
    await confirm(`确认删除选中的 ${ids.length} 条评论？`, '批量删除', {
      confirmText: '删除',
      danger: true,
    })
  } catch {
    return
  }
  acting.value = true
  try {
    await deleteComments(ids)
    toast.success(`已删除 ${ids.length} ${UNIT}`)
    await fetchComments()
    fetchTabCounts()
  } catch {
    toast.error('删除失败')
  } finally {
    acting.value = false
  }
}

// tab 顺序：全部 → 主要工作对象 → 其余。计数恒显示，含 0
const tabCounts = reactive({ all: 0, visible: 0, hidden: 0 })

const TAB_STATUS: Record<string, CommentStatus | 0> = { all: 0, visible: 1, hidden: 2 }

const tabs = computed(() => [
  { key: 'all', label: '全部', count: tabCounts.all },
  { key: 'visible', label: '显示中', count: tabCounts.visible },
  { key: 'hidden', label: '已隐藏', count: tabCounts.hidden },
])

const activeTab = computed({
  get: () => Object.keys(TAB_STATUS).find((k) => TAB_STATUS[k] === filters.status) ?? 'all',
  set: (key: string) => {
    filters.status = TAB_STATUS[key] ?? 0
  },
})

async function fetchTabCounts() {
  try {
    const stats = await getCommentStats({ keyword: filters.keyword })
    tabCounts.all = stats.total
    tabCounts.visible = stats.visible
    tabCounts.hidden = stats.hidden
  } catch {
    /* 统计失败不影响主流程 */
  }
}

onMounted(fetchTabCounts)

function handleReset() {
  filters.keyword = ''
  filters.status = 0
}
</script>

<template>
  <div class="main-card">
    <AdminListHeader
      v-model:active-tab="activeTab"
      v-model:keyword="filters.keyword"
      :tabs="tabs"
      search-placeholder="搜索评论内容"
      @search="applyFilter"
      @reset="handleReset"
    />

    <AdminSelectionBar :count="selected.size" :unit="UNIT" @clear="clearSelection">
      <button
        class="ghost-btn ghost-btn--sm ghost-btn--danger"
        :disabled="acting"
        @click="handleBatchDelete"
      >
        批量删除
      </button>
    </AdminSelectionBar>

    <div class="table-wrap" :class="{ 'table-wrap--loading': loading }">
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
                :checked="allSelected"
                @change="toggleAll"
              />
            </th>
            <th class="col-main">评论内容</th>
            <th class="col-name">评论者</th>
            <th class="col-text">所属文章</th>
            <th class="col-time">时间</th>
            <th class="col-status">状态</th>
            <th class="col-actions" />
          </tr>
        </thead>
        <tbody>
          <tr v-if="comments.length === 0 && !loading">
            <td colspan="7" class="empty-cell">
              <div class="empty-state">
                <AdminIcon name="comment" class="empty-icon" />
                <span>{{
                  filters.keyword || filters.status ? '没有匹配的评论' : '还没有评论'
                }}</span>
              </div>
            </td>
          </tr>
          <tr
            v-for="row in comments"
            :key="row.id"
            :class="{ 'row--selected': selected.has(row.id), 'row--hidden': row.status === 2 }"
          >
            <td class="col-check">
              <input
                type="checkbox"
                class="row-checkbox"
                :checked="selected.has(row.id)"
                @change="toggleRow(row.id)"
              />
            </td>
            <td class="col-main" :title="row.content">
              <span v-if="row.rootId !== null" class="reply-flag">回复</span>
              {{ row.content }}
            </td>
            <td class="col-name">
              <span class="cell-muted">{{ row.user?.username ?? '—' }}</span>
            </td>
            <td class="col-text">
              <RouterLink
                v-if="row.articleTitle"
                :to="`/article/${row.articleId}`"
                target="_blank"
                class="article-link"
                :title="row.articleTitle"
                >{{ row.articleTitle }}</RouterLink
              >
              <span v-else class="cell-muted">文章已删除</span>
            </td>
            <td class="col-time">
              <span class="cell-muted">{{ formatDateTime(row.createTime) }}</span>
            </td>
            <td class="col-status">
              <span
                class="status-badge"
                :class="row.status === 1 ? 'status-badge--ok' : 'status-badge--danger'"
              >
                {{ row.status === 1 ? '显示中' : '已隐藏' }}
              </span>
            </td>
            <td class="col-actions">
              <div class="row-actions">
                <button class="action-btn" :disabled="acting" @click="handleToggleStatus(row)">
                  {{ row.status === 1 ? '隐藏' : '恢复' }}
                </button>
                <button class="action-btn action-btn--danger" @click="handleDelete(row)">
                  删除
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 移动端卡片 -->
      <ul class="data-cards">
        <li
          v-for="row in comments"
          :key="`c-${row.id}`"
          class="data-card"
          :class="{ 'data-card--selected': selected.has(row.id) }"
        >
          <div class="data-card__head">
            <input
              type="checkbox"
              class="row-checkbox"
              :checked="selected.has(row.id)"
              @change="toggleRow(row.id)"
            />
            <span class="data-card__author">{{ row.user?.username ?? '—' }}</span>
            <span
              class="status-badge"
              :class="row.status === 1 ? 'status-badge--ok' : 'status-badge--danger'"
            >
              {{ row.status === 1 ? '显示中' : '已隐藏' }}
            </span>
          </div>
          <p class="data-card__text">{{ row.content }}</p>
          <div class="data-card__meta">
            <span class="cell-muted">{{ row.articleTitle ?? '文章已删除' }}</span>
            <span class="cell-muted">{{ formatDateTime(row.createTime) }}</span>
          </div>
          <div class="data-card__actions">
            <button class="action-btn" :disabled="acting" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '隐藏' : '恢复' }}
            </button>
            <button class="action-btn action-btn--danger" @click="handleDelete(row)">删除</button>
          </div>
        </li>
      </ul>
    </div>

    <!-- 分页 -->
    <AdminPagination
      :total="total"
      :page-num="pageNum"
      :page-size="pageSize"
      :total-pages="totalPages"
      :page-numbers="pageNumbers"
      @go="query.goPage"
      @size="query.setPageSize"
    />
  </div>
</template>

<style scoped>
/* 固定列合计 40+140+200+150+100+148=778，再给主列留 240px 下限；窄于此宽度改为横向滚动，
   而不是把主列压成 0（见 variables.css 中 .data-table 的说明） */
.data-table {
  min-width: 1020px;
}

.reply-flag {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 5px;
  border-radius: var(--admin-radius);
  background: var(--admin-sidebar-hover);
  font-size: 11px;
  color: var(--admin-sidebar-text-muted);
  vertical-align: middle;
}

.article-link {
  color: var(--admin-sidebar-text);
  text-decoration: none;
}

.article-link:hover {
  color: var(--admin-accent);
}

.row--hidden td {
  opacity: 0.55;
}

.data-cards {
  display: none;
  list-style: none;
  margin: 0;
  padding: 0;
}

.data-card {
  padding: 14px 16px;
  border-bottom: 1px solid var(--admin-sidebar-border);
}

.data-card--selected {
  background: var(--admin-sidebar-hover);
}

.data-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.data-card__author {
  font-size: 13px;
  font-weight: 500;
  color: var(--admin-sidebar-text);
  margin-right: auto;
}

.data-card__text {
  margin: 0 0 8px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--admin-sidebar-text);
  word-break: break-word;
}

.data-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 12px;
}

.data-card__actions {
  display: flex;
  gap: 8px;
}

.sel-bar-leave-active {
  transition: opacity 0.15s ease;
}

.sel-bar-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .data-table {
    display: none;
  }

  .data-cards {
    display: block;
  }

  .card-toolbar {
    flex-wrap: wrap;
  }
}
</style>
