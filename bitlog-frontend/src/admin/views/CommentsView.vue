<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import AdminEmptyState from '@/admin/components/AdminEmptyState.vue'
import {
  getAdminCommentPage,
  updateCommentStatus,
  deleteComments,
  type CommentAdmin,
  type CommentStatus,
} from '@/api/admin/comment'

const toast = useToast()
const confirm = useConfirm()

const loading = ref(false)
const comments = ref<CommentAdmin[]>([])

const keyword = ref('')
const statusFilter = ref<CommentStatus | ''>('')

const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const totalPages = computed(() => Math.ceil(pagination.total / pagination.pageSize) || 1)

const selected = reactive(new Set<number>())

function clearSelection() {
  selected.clear()
}

async function fetchComments() {
  loading.value = true
  try {
    const res = await getAdminCommentPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value || undefined,
    })
    comments.value = res.content
    pagination.total = res.totalElements
    clearSelection()
    // 删光当前页最后一条后会停在空页，回退一页；pageNum > 1 保证递归终止
    if (res.content.length === 0 && pagination.pageNum > 1) {
      pagination.pageNum -= 1
      return await fetchComments()
    }
  } catch {
    toast.error('加载评论失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchComments)

function applyFilter() {
  pagination.pageNum = 1
  fetchComments()
}

function goPage(n: number) {
  if (n < 1 || n > totalPages.value || n === pagination.pageNum) return
  pagination.pageNum = n
  fetchComments()
}

function pageSizeChange(e: Event) {
  pagination.pageSize = Number((e.target as HTMLSelectElement).value)
  pagination.pageNum = 1
  fetchComments()
}

const pageNumbers = computed(() => {
  const cur = pagination.pageNum
  const total = totalPages.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages: (number | '…')[] = [1]
  if (cur > 3) pages.push('…')
  for (let i = Math.max(2, cur - 1); i <= Math.min(total - 1, cur + 1); i++) pages.push(i)
  if (cur < total - 2) pages.push('…')
  pages.push(total)
  return pages
})

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

function clearKeyword() {
  keyword.value = ''
  applyFilter()
}

function formatTime(iso: string) {
  return iso ? iso.slice(0, 16) : ''
}
</script>

<template>
  <div class="main-card">
    <!-- 筛选 -->
    <div class="card-toolbar">
      <div class="search-wrap">
        <svg
          class="search-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.3-4.3" />
        </svg>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索评论内容"
          @keyup.enter="applyFilter"
        />
        <button v-if="keyword" class="search-clear" @click="clearKeyword">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
      </div>
      <select v-model="statusFilter" class="filter-select" @change="applyFilter">
        <option value="">全部状态</option>
        <option :value="1">正常</option>
        <option :value="2">已隐藏</option>
      </select>
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
              <span v-if="row.rootId !== 0" class="reply-flag">回复</span>
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
                {{ row.status === 1 ? '正常' : '已隐藏' }}
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
              {{ row.status === 1 ? '正常' : '已隐藏' }}
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
      :message="keyword || statusFilter ? '没有匹配的评论' : '还没有评论'"
    />

    <!-- 分页 -->
    <div v-if="pagination.total > 0" class="pagination-bar">
      <span class="pagination-total">共 {{ pagination.total }} 条</span>
      <div class="pagination-controls">
        <button
          class="page-btn"
          :disabled="pagination.pageNum <= 1"
          @click="goPage(pagination.pageNum - 1)"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M15 18l-6-6 6-6" />
          </svg>
        </button>
        <template v-for="p in pageNumbers" :key="p">
          <span v-if="p === '…'" class="page-ellipsis">…</span>
          <button
            v-else
            class="page-btn page-btn--num"
            :class="{ 'page-btn--active': p === pagination.pageNum }"
            @click="goPage(p as number)"
          >
            {{ p }}
          </button>
        </template>
        <button
          class="page-btn"
          :disabled="pagination.pageNum >= totalPages"
          @click="goPage(pagination.pageNum + 1)"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 18l6-6-6-6" />
          </svg>
        </button>
      </div>
      <select class="page-size-select" :value="pagination.pageSize" @change="pageSizeChange">
        <option :value="10">10 / 页</option>
        <option :value="20">20 / 页</option>
        <option :value="50">50 / 页</option>
      </select>
    </div>
  </div>
</template>

<style scoped>
.card-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--admin-sidebar-border);
}

.table-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--admin-surface-rgb), 0.7);
  z-index: 1;
}

.filter-select {
  height: 32px;
  padding: 0 8px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: var(--admin-radius);
  background: var(--admin-surface-input);
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  outline: none;
  cursor: pointer;
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

.col-time {
  width: 130px;
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

.pagination-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--admin-sidebar-border);
}

.pagination-total {
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  white-space: nowrap;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-left: auto;
}

.page-btn {
  min-width: 28px;
  height: 28px;
  padding: 0 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: var(--admin-radius);
  background: var(--admin-surface-input);
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  cursor: pointer;
  transition:
    background 0.12s,
    border-color 0.12s;
}

.page-btn svg {
  width: 13px;
  height: 13px;
}

.page-btn:hover:not(:disabled) {
  background: var(--admin-sidebar-hover);
}

.page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.page-btn--active {
  background: var(--admin-accent);
  border-color: var(--admin-accent);
  color: var(--admin-text-on-accent);
  font-weight: 600;
}

.page-btn--active:hover {
  background: var(--admin-accent);
}

.page-ellipsis {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  font-size: 12.5px;
  color: var(--admin-sidebar-text-muted);
}

.page-size-select {
  height: 28px;
  padding: 0 6px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: var(--admin-radius);
  background: var(--admin-surface-input);
  font-size: 12px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  outline: none;
  cursor: pointer;
  margin-left: 8px;
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
