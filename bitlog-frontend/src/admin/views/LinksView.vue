<script setup lang="ts">
import { computed, ref } from 'vue'
import { useListQuery } from '@/composables/useListQuery'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import AdminPagination from '@/admin/components/AdminPagination.vue'
import { ApiError } from '@/utils/request'
import { LINK_STATUS, type LinkStatus } from '@/api/link'
import {
  auditAdminLink,
  deleteAdminLink,
  getAdminLinkPage,
  type FriendLinkAdmin,
} from '@/api/admin/link'

const toast = useToast()
const confirm = useConfirm()

type TabKey = 'all' | 'pending' | 'approved' | 'rejected'

const TAB_STATUS: Record<TabKey, LinkStatus | undefined> = {
  all: undefined,
  pending: LINK_STATUS.PENDING,
  approved: LINK_STATUS.APPROVED,
  rejected: LINK_STATUS.REJECTED,
}

// 全部排第一并作为默认，与文章页、用户页一致
const TABS: { key: TabKey; label: string }[] = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待审核' },
  { key: 'approved', label: '展示中' },
  { key: 'rejected', label: '未通过' },
]

const query = useListQuery({
  filters: { tab: 'all' as TabKey, keyword: '' },
  toParams: (f) => ({ status: TAB_STATUS[f.tab], keyword: f.keyword }),
  fetch: (params) => getAdminLinkPage(params),
  debounce: ['keyword'],
  syncUrl: true,
})

const { filters, items: links, loading, pageNum, pageSize, total, totalPages, pageNumbers } = query

const acting = ref(false)

// ── 拒绝弹窗 ────────────────────────────────────────────────────────────────

const rejecting = ref<FriendLinkAdmin | null>(null)
const rejectReason = ref('')

function openReject(row: FriendLinkAdmin) {
  rejecting.value = row
  // 重新拒绝时带出原因，改错别字不必重打一遍
  rejectReason.value = row.rejectReason ?? ''
}

function closeReject() {
  rejecting.value = null
  rejectReason.value = ''
}

async function confirmReject() {
  if (!rejecting.value) return
  await runAction(
    () => auditAdminLink(rejecting.value!.id, LINK_STATUS.REJECTED, rejectReason.value),
    '已拒绝',
  )
  closeReject()
}

// ── 操作 ────────────────────────────────────────────────────────────────────

async function runAction(action: () => Promise<void>, okText: string) {
  if (acting.value) return
  acting.value = true
  try {
    await action()
    toast.success(okText)
    await query.load()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : '操作失败')
  } finally {
    acting.value = false
  }
}

function handleApprove(row: FriendLinkAdmin) {
  runAction(() => auditAdminLink(row.id, LINK_STATUS.APPROVED), '已通过')
}

async function handleDelete(row: FriendLinkAdmin) {
  try {
    await confirm(`删除「${row.name}」？`, '删除友链', { confirmText: '删除', danger: true })
  } catch {
    return
  }
  runAction(() => deleteAdminLink(row.id), '已删除')
}

// ── 展示 ────────────────────────────────────────────────────────────────────

const statusMeta: Record<LinkStatus, { label: string; cls: string }> = {
  [LINK_STATUS.PENDING]: { label: '待审核', cls: 'is-pending' },
  [LINK_STATUS.APPROVED]: { label: '展示中', cls: 'is-live' },
  [LINK_STATUS.REJECTED]: { label: '未通过', cls: 'is-rejected' },
}

const isEmpty = computed(() => !loading.value && links.value.length === 0)

function formatTime(value: string) {
  return value ? value.slice(0, 16).replace('T', ' ') : '—'
}

function hostOf(url: string) {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}
</script>

<template>
  <div class="main-card">
    <!-- 头部与文章页、用户页同构：左 tabs、右操作，同一行 -->
    <div class="card-header">
      <div class="view-tabs">
        <button
          v-for="tab in TABS"
          :key="tab.key"
          class="view-tab"
          :class="{ 'view-tab--active': filters.tab === tab.key }"
          @click="filters.tab = tab.key"
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
          <input v-model="filters.keyword" class="search-input" placeholder="搜索站点名称或地址" />
          <button v-if="filters.keyword" class="search-clear" @click="filters.keyword = ''">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <button class="icon-btn" title="刷新" @click="query.load">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
            <path d="M3 3v5h5" />
          </svg>
        </button>
      </div>
    </div>

    <div v-if="loading || links.length > 0" class="table-wrap">
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
            <th>站点</th>
            <th class="col-url">地址</th>
            <th class="col-user">申请人</th>
            <th class="col-msg">留言</th>
            <th class="col-time">提交时间</th>
            <th class="col-status">状态</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in links" :key="row.id">
            <td>
              <div class="site-cell">
                <div class="site-avatar">
                  <img v-if="row.avatar" :src="row.avatar" :alt="row.name" />
                  <span v-else>{{ row.name.charAt(0).toUpperCase() }}</span>
                </div>
                <div class="site-names">
                  <span class="site-name">{{ row.name }}</span>
                  <span v-if="row.description" class="cell-muted">{{ row.description }}</span>
                </div>
              </div>
            </td>
            <td class="col-url">
              <a :href="row.url" target="_blank" rel="noopener noreferrer" class="url-link">
                {{ hostOf(row.url) }}
              </a>
            </td>
            <td class="col-user">
              <span class="cell-muted">{{ row.applicant?.username ?? '—' }}</span>
            </td>
            <td class="col-msg">
              <span class="cell-muted" :title="row.applyMessage">{{
                row.applyMessage || '—'
              }}</span>
            </td>
            <td class="col-time">
              <span class="cell-muted">{{ formatTime(row.createTime) }}</span>
            </td>
            <td class="col-status">
              <span class="status-badge" :class="statusMeta[row.status].cls">
                {{ statusMeta[row.status].label }}
              </span>
              <span
                v-if="row.status === LINK_STATUS.REJECTED && row.rejectReason"
                class="reject-hint"
                :title="row.rejectReason"
              >
                {{ row.rejectReason }}
              </span>
            </td>
            <td class="col-actions">
              <div class="row-actions">
                <button
                  v-if="row.status !== LINK_STATUS.APPROVED"
                  class="action-btn"
                  :disabled="acting"
                  @click="handleApprove(row)"
                >
                  通过
                </button>
                <button class="action-btn" :disabled="acting" @click="openReject(row)">
                  {{ row.status === LINK_STATUS.REJECTED ? '改理由' : '拒绝' }}
                </button>
                <button class="action-btn action-btn--danger" @click="handleDelete(row)">
                  删除
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="isEmpty" class="empty-state">
      <svg
        class="empty-icon"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.5"
      >
        <path d="M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.7 1.7" />
        <path d="M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7" />
      </svg>
      <p>暂无友链</p>
    </div>

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

    <!-- 拒绝理由 -->
    <div v-if="rejecting" class="dialog-overlay" @click.self="closeReject">
      <div class="dialog-box">
        <h3 class="dialog-title">拒绝「{{ rejecting.name }}」</h3>
        <input
          v-model="rejectReason"
          class="dialog-input"
          placeholder="填写理由，申请人可以看到"
          maxlength="255"
          @keyup.enter="confirmReject"
        />
        <p class="input-hint">留空也可以，但对方就不知道该怎么改了。</p>
        <div class="dialog-actions">
          <button class="dialog-btn dialog-btn--cancel" @click="closeReject">取消</button>
          <button class="dialog-btn dialog-btn--ok" :disabled="acting" @click="confirmReject">
            确认拒绝
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 以下取自 CommentsView / UsersView 的同名定义，两页保持一致，勿各写一套 */

.col-url {
  width: 160px;
}

.col-user {
  width: 110px;
}

.col-msg {
  width: 180px;
}

.col-status {
  width: 110px;
}

.col-actions {
  width: 150px;
}

/* 头像单元格：与 UsersView 的 .user-cell / .user-avatar 同构 */
.site-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.site-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--admin-accent-bg-strong);
  color: var(--admin-accent-dark);
  font-size: 12px;
  font-weight: 600;
}

.site-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.site-names {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.site-name {
  font-size: 13px;
  color: var(--admin-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.site-names .cell-muted,
.col-msg .cell-muted,
.url-link {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.url-link {
  color: var(--admin-text-secondary);
  text-decoration: none;
}

.url-link:hover {
  color: var(--admin-accent);
  text-decoration: underline;
}

/* 徽章：形状与 CommentsView 的 .status-badge 完全一致，只是多一档状态 */
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--admin-radius);
  font-size: 11.5px;
  white-space: nowrap;
}

.status-badge.is-pending {
  background: var(--admin-accent-bg-soft);
  color: var(--admin-accent-dark);
}

.status-badge.is-live {
  background: var(--admin-sidebar-hover);
  color: var(--admin-sidebar-text-muted);
}

.status-badge.is-rejected {
  background: var(--admin-danger-bg-soft);
  color: var(--admin-danger-on-soft);
}

.reject-hint {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: var(--admin-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .col-msg,
  .col-time {
    display: none;
  }
}
</style>
