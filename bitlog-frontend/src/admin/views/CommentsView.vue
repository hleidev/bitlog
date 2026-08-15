<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import AdminEmptyState from '@/admin/components/AdminEmptyState.vue'
import AdminPagination from '@/admin/components/AdminPagination.vue'
import { useListQuery } from '@/composables/useListQuery'
import {
  getAdminCommentPage,
  updateCommentStatus,
  deleteComments,
  type CommentAdmin,
  type CommentStatus,
} from '@/api/admin/comment'

const toast = useToast()
const confirm = useConfirm()

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
    toast.success(`已删除 ${ids.length} 条`)
    await fetchComments()
  } catch {
    toast.error('删除失败')
  } finally {
    acting.value = false
  }
}

const STATUS_TABS: { value: CommentStatus | 0; label: string }[] = [
  { value: 0, label: '全部' },
  { value: 1, label: '显示中' },
  { value: 2, label: '已隐藏' },
]

function clearKeyword() {
  filters.keyword = ''
}

function formatTime(iso: string) {
  // 去掉 ISO 的 T，否则窄列里会从 T 处折行
  return iso ? iso.slice(0, 16).replace('T', ' ') : ''
}
</script>

<template>
  <div class="main-card">
    <!-- 头部与文章页、用户页同构：左 tabs、右操作，同一行 -->
    <div class="card-header">
      <div class="view-tabs">
        <button
          v-for="tab in STATUS_TABS"
          :key="tab.value"
          class="view-tab"
          :class="{ 'view-tab--active': filters.status === tab.value }"
          @click="filters.status = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="header-actions">
        <div class="search-wrap">
          <svg
            class="search-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
          </svg>
          <input
            v-model="filters.keyword"
            class="search-input"
            placeholder="搜索评论内容"
            @keyup.enter="applyFilter"
          />
          <button v-if="filters.keyword" class="search-clear" @click="clearKeyword">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <button class="icon-btn" title="刷新" @click="fetchComments">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
            <path d="M3 3v5h5" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 批量操作条 -->
    <Transition name="sel-bar">
      <div v-if="selected.size > 0" class="selection-bar">
        <span class="sel-count">已选 {{ selected.size }} 条</span>
        <div class="sel-actions">
          <button class="ghost-btn ghost-btn--sm" @click="clearSelection">取消选择</button>
          <button
            class="ghost-btn ghost-btn--sm ghost-btn--danger"
            :disabled="acting"
            @click="handleBatchDelete"
          >
            删除
          </button>
        </div>
      </div>
    </Transition>

    <div v-if="loading || comments.length > 0" class="table-wrap">
      <div v-if="loading" class="table-loading">
        <svg class="spinner" viewBox="0 0 24 24" fill="none">
          <circle
            cx="12"
            cy="12"
            r="9"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-dasharray="40 20"
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
            <th>评论内容</th>
            <th class="col-user">评论者</th>
            <th class="col-article">所属文章</th>
            <th class="col-time">时间</th>
            <th class="col-status">状态</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
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
            <td>
              <span class="comment-text">{{ row.content }}</span>
              <span v-if="row.rootId !== null" class="reply-flag">回复</span>
            </td>
            <td class="col-user">
              <span class="cell-muted">{{ row.user?.username ?? '—' }}</span>
            </td>
            <td class="col-article">
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
              <span class="cell-muted">{{ formatTime(row.createTime) }}</span>
            </td>
            <td class="col-status">
              <span class="status-badge" :class="row.status === 1 ? 'is-normal' : 'is-hidden'">
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
            <span class="status-badge" :class="row.status === 1 ? 'is-normal' : 'is-hidden'">
              {{ row.status === 1 ? '显示中' : '已隐藏' }}
            </span>
          </div>
          <p class="data-card__text">{{ row.content }}</p>
          <div class="data-card__meta">
            <span class="cell-muted">{{ row.articleTitle ?? '文章已删除' }}</span>
            <span class="cell-muted">{{ formatTime(row.createTime) }}</span>
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

    <AdminEmptyState
      v-if="!loading && comments.length === 0"
      :message="filters.keyword || filters.status ? '没有匹配的评论' : '还没有评论'"
    />

    <!-- 分页 -->
    <AdminPagination
      :total="total"
      :page-num="pageNum"
      :page-size="pageSize"
      :total-pages="totalPages"
      :page-numbers="pageNumbers"
      unit="条"
      @go="query.goPage"
      @size="query.setPageSize"
    />
  </div>
</template>

<style scoped>
.table-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--admin-surface-rgb), 0.7);
  z-index: 1;
}

.comment-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
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

.col-user {
  width: 110px;
}

.col-article {
  width: 200px;
}

.col-status {
  width: 80px;
}

.col-actions {
  width: 130px;
}

.article-link {
  display: block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--admin-sidebar-text);
  text-decoration: none;
}

.article-link:hover {
  color: var(--admin-accent);
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--admin-radius);
  font-size: 11.5px;
  white-space: nowrap;
}

.status-badge.is-normal {
  background: var(--admin-sidebar-hover);
  color: var(--admin-sidebar-text-muted);
}

.status-badge.is-hidden {
  background: var(--color-danger-bg, #fef2f2);
  color: var(--color-danger, #dc2626);
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
