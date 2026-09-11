<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { useListQuery } from '@/composables/useListQuery'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import { useRowMenu } from '@/admin/composables/useRowMenu'
import { formatDateTime } from '@/utils/format'
import AdminIcon from '@/admin/components/AdminIcon.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import AdminListHeader from '@/admin/components/AdminListHeader.vue'
import AdminPagination from '@/admin/components/AdminPagination.vue'
import { ApiError } from '@/utils/request'
import { LINK_STATUS, type FriendLinkSaveParam, type LinkStatus } from '@/api/link'
import {
  auditAdminLink,
  createAdminLink,
  deleteAdminLink,
  getAdminLinkPage,
  getAdminLinkStats,
  updateAdminLink,
  type FriendLinkAdmin,
} from '@/api/admin/link'

const toast = useToast()
const confirm = useConfirm()

const { openMenuId, menuRef, menuId, menuStyle, toggleMenu, closeMenu } = useRowMenu()

type TabKey = 'all' | 'pending' | 'approved' | 'rejected'

const TAB_STATUS: Record<TabKey, LinkStatus | undefined> = {
  all: undefined,
  pending: LINK_STATUS.PENDING,
  approved: LINK_STATUS.APPROVED,
  rejected: LINK_STATUS.REJECTED,
}

// 顺序：全部 → 主要工作对象（友链的日常就是审核）→ 其余
const tabCounts = reactive({ all: 0, pending: 0, approved: 0, rejected: 0 })
const countsReady = ref(false)
const loadFailed = ref(false)
let countRequestId = 0
let pageRequestId = 0

const tabs = computed(() => [
  { key: 'all', label: '全部', count: countsReady.value ? tabCounts.all : undefined },
  { key: 'pending', label: '待审核', count: countsReady.value ? tabCounts.pending : undefined },
  { key: 'approved', label: '展示中', count: countsReady.value ? tabCounts.approved : undefined },
  { key: 'rejected', label: '未通过', count: countsReady.value ? tabCounts.rejected : undefined },
])

async function fetchTabCounts() {
  const id = ++countRequestId
  countsReady.value = false
  try {
    const stats = await getAdminLinkStats({ keyword: filters.keyword })
    if (id !== countRequestId) return
    tabCounts.all = stats.total
    tabCounts.pending = stats.pending
    tabCounts.approved = stats.approved
    tabCounts.rejected = stats.rejected
    countsReady.value = true
  } catch {
    /* 统计失败不影响主流程 */
  }
}

onMounted(fetchTabCounts)
onBeforeUnmount(() => {
  countRequestId++
  pageRequestId++
})

// AdminListHeader 的 model 是 string，这里做一层窄化桥接
const activeTab = computed({
  get: (): string => filters.tab,
  set: (key: string) => {
    filters.tab = key as TabKey
  },
})

function handleReset() {
  query.reset()
}

const query = useListQuery({
  filters: { tab: 'all' as TabKey, keyword: '' },
  toParams: (f) => ({ status: TAB_STATUS[f.tab], keyword: f.keyword }),
  fetch: async (params) => {
    const id = ++pageRequestId
    const result = await getAdminLinkPage(params)
    if (id === pageRequestId) loadFailed.value = false
    return result
  },
  debounce: ['keyword'],
  syncUrl: true,
  onFiltersApplied: fetchTabCounts,
  sanitize: (filters) => {
    if (!Object.hasOwn(TAB_STATUS, filters.tab)) filters.tab = 'all'
  },
  onError: () => {
    loadFailed.value = true
  },
})

const { filters, items: links, loading, pageNum, pageSize, total, totalPages, pageNumbers } = query

const acting = ref(false)
const actionError = ref('')
const menuRow = computed(() => links.value.find((row) => row.id === openMenuId.value))

// ── 拒绝弹窗 ────────────────────────────────────────────────────────────────

const rejecting = ref<FriendLinkAdmin | null>(null)
const rejectReason = ref('')

function openReject(row: FriendLinkAdmin) {
  actionError.value = ''
  rejecting.value = row
  // 重新拒绝时带出原因，改错别字不必重打一遍
  rejectReason.value = row.rejectReason ?? ''
}

function closeReject() {
  if (acting.value) return
  rejecting.value = null
  rejectReason.value = ''
}

async function confirmReject() {
  if (!rejecting.value) return
  const ok = await runAction(
    () => auditAdminLink(rejecting.value!.id, LINK_STATUS.REJECTED, rejectReason.value),
    '已拒绝',
  )
  if (ok) closeReject()
}

// ── 添加 / 编辑弹窗 ─────────────────────────────────────────────────────────

const formVisible = ref(false)
// null 表示新增；非 null 表示正在编辑的那一行
const formTarget = ref<FriendLinkAdmin | null>(null)

const form = reactive({ name: '', url: '', avatar: '', description: '' })

const formValid = computed(() => form.name.trim() !== '' && form.url.trim() !== '')

function openCreate() {
  actionError.value = ''
  formTarget.value = null
  Object.assign(form, { name: '', url: '', avatar: '', description: '' })
  formVisible.value = true
}

function openEdit(row: FriendLinkAdmin) {
  closeMenu(true)
  actionError.value = ''
  formTarget.value = row
  Object.assign(form, {
    name: row.name,
    url: row.url,
    avatar: row.avatar ?? '',
    description: row.description ?? '',
  })
  formVisible.value = true
}

function closeForm() {
  if (acting.value) return
  formVisible.value = false
  formTarget.value = null
}

async function submitForm() {
  if (!formValid.value) return
  const row = formTarget.value
  const payload: FriendLinkSaveParam = {
    name: form.name.trim(),
    url: form.url.trim(),
    avatar: form.avatar.trim() || undefined,
    description: form.description.trim() || undefined,
    // 后端按整体覆盖写，不回传就会把申请人当初的留言抹成空
    applyMessage: row?.applyMessage || undefined,
  }
  const ok = await runAction(
    async () => {
      if (row) {
        await updateAdminLink(row.id, payload)
      } else {
        await createAdminLink(payload)
      }
    },
    row ? '已保存' : '已添加',
  )
  if (ok) closeForm()
}

// ── 操作 ────────────────────────────────────────────────────────────────────

async function runAction(action: () => Promise<void>, okText: string): Promise<boolean> {
  if (acting.value) return false
  actionError.value = ''
  acting.value = true
  try {
    await action()
    toast.success(okText)
    await query.load()
    fetchTabCounts()
    return true
  } catch (err) {
    actionError.value = err instanceof ApiError ? err.message : '操作失败，请重试。'
    if (!formVisible.value && !rejecting.value) toast.error(actionError.value)
    return false
  } finally {
    acting.value = false
  }
}

function handleApprove(row: FriendLinkAdmin) {
  runAction(() => auditAdminLink(row.id, LINK_STATUS.APPROVED), '已通过')
}

async function handleDelete(row: FriendLinkAdmin) {
  closeMenu(true)
  try {
    await confirm(`删除「${row.name}」？`, '删除友链', { confirmText: '删除', danger: true })
  } catch {
    return
  }
  runAction(() => deleteAdminLink(row.id), '已删除')
}

// ── 展示 ────────────────────────────────────────────────────────────────────

const statusMeta: Record<LinkStatus, { label: string; cls: string }> = {
  [LINK_STATUS.PENDING]: { label: '待审核', cls: 'status-badge--pending' },
  [LINK_STATUS.APPROVED]: { label: '展示中', cls: 'status-badge--ok' },
  [LINK_STATUS.REJECTED]: { label: '未通过', cls: 'status-badge--danger' },
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
    <AdminListHeader
      v-model:active-tab="activeTab"
      v-model:keyword="filters.keyword"
      :tabs="tabs"
      search-placeholder="搜索站点名称或地址"
      action-label="新增友链"
      @search="query.applyFilters"
      @reset="handleReset"
      @action="openCreate"
    />

    <div v-if="loadFailed && !loading" class="admin-error-state" role="alert">
      <p>友链加载失败，请重试。</p>
      <button class="ghost-btn" @click="query.load">重新加载</button>
    </div>
    <div v-else class="table-wrap" :class="{ 'table-wrap--loading': loading }" :aria-busy="loading">
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
            <th class="col-main">站点</th>
            <th class="col-time">申请信息</th>
            <th class="col-text">留言</th>
            <th class="col-status">状态</th>
            <th class="col-actions" />
          </tr>
        </thead>
        <tbody>
          <tr v-if="links.length === 0 && !loading">
            <td colspan="5" class="empty-cell">
              <div class="empty-state">
                <AdminIcon name="link" class="empty-icon" />
                <span>{{ filters.keyword ? '没有匹配的友链' : '暂无友链' }}</span>
              </div>
            </td>
          </tr>
          <tr v-for="row in links" :key="row.id">
            <td class="col-main">
              <div class="site-cell">
                <div class="site-avatar">
                  <img v-if="row.avatar" :src="row.avatar" :alt="row.name" />
                  <span v-else>{{ row.name.charAt(0).toUpperCase() }}</span>
                </div>
                <div class="site-names">
                  <span class="site-name">{{ row.name }}</span>
                  <a
                    :href="row.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="url-link"
                    :title="row.url"
                    >{{ hostOf(row.url) }} ↗</a
                  >
                  <span v-if="row.description" class="cell-muted">{{ row.description }}</span>
                </div>
              </div>
            </td>
            <td class="col-time applicant-cell">
              <span>{{ row.applicant?.username ?? '手动添加' }}</span>
              <time class="cell-muted">{{ formatDateTime(row.createTime) }}</time>
            </td>
            <td class="col-text application-message">
              <span :title="row.applyMessage">{{ row.applyMessage || '—' }}</span>
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
    </div>

    <AdminPagination
      :total="total"
      :page-num="pageNum"
      :page-size="pageSize"
      :total-pages="totalPages"
      :page-numbers="pageNumbers"
      @go="query.goPage"
      @size="query.setPageSize"
    />

    <!-- 拒绝理由 -->
    <BaseModal :visible="!!rejecting" width="420px" aria-label="拒绝友链" @close="closeReject">
      <div v-if="rejecting" class="dialog-box admin-form-dialog">
        <h3 class="dialog-title">拒绝「{{ rejecting.name }}」</h3>
        <input
          v-model="rejectReason"
          class="dialog-input"
          aria-label="拒绝理由"
          autofocus
          :disabled="acting"
          placeholder="填写理由，申请人可以看到"
          maxlength="255"
          @keyup.enter="confirmReject"
        />
        <p class="input-hint">留空也可以，但对方就不知道该怎么改了。</p>
        <p v-if="actionError" class="dialog-error" role="alert">{{ actionError }}</p>
        <div class="dialog-actions">
          <button class="dialog-btn dialog-btn--cancel" :disabled="acting" @click="closeReject">
            取消
          </button>
          <button class="dialog-btn dialog-btn--ok" :disabled="acting" @click="confirmReject">
            {{ acting ? '提交中…' : '确认拒绝' }}
          </button>
        </div>
      </div>
    </BaseModal>

    <!-- 添加 / 编辑：站长手动添加的友链直接进入展示中，不再走审核 -->
    <BaseModal
      :visible="formVisible"
      width="420px"
      :aria-label="formTarget ? '编辑友链' : '新增友链'"
      @close="closeForm"
    >
      <div class="dialog-box admin-form-dialog">
        <h3 class="dialog-title">{{ formTarget ? '编辑友链' : '新增友链' }}</h3>

        <div class="form-field">
          <label for="link-name" class="form-label">站点名称</label>
          <input
            id="link-name"
            v-model="form.name"
            class="dialog-input"
            autofocus
            :disabled="acting"
            placeholder="必填"
            maxlength="64"
          />
        </div>

        <div class="form-field">
          <label for="link-url" class="form-label">站点地址</label>
          <input
            id="link-url"
            v-model="form.url"
            :disabled="acting"
            class="dialog-input"
            placeholder="https://"
            maxlength="512"
            spellcheck="false"
          />
        </div>

        <div class="form-field">
          <label for="link-avatar" class="form-label">头像地址</label>
          <input
            id="link-avatar"
            v-model="form.avatar"
            :disabled="acting"
            class="dialog-input"
            placeholder="选填，留空取站名首字"
            maxlength="512"
            spellcheck="false"
          />
        </div>

        <div class="form-field">
          <label for="link-description" class="form-label">站点简介</label>
          <input
            id="link-description"
            v-model="form.description"
            :disabled="acting"
            class="dialog-input"
            placeholder="选填"
            maxlength="255"
          />
        </div>

        <p v-if="formTarget" class="input-hint">只改内容，不影响当前状态。</p>
        <p v-if="actionError" class="dialog-error" role="alert">{{ actionError }}</p>

        <div class="dialog-actions">
          <button class="dialog-btn dialog-btn--cancel" :disabled="acting" @click="closeForm">
            取消
          </button>
          <button
            class="dialog-btn dialog-btn--ok"
            :disabled="acting || !formValid"
            @click="submitForm"
          >
            {{ acting ? '保存中…' : formTarget ? '保存' : '添加' }}
          </button>
        </div>
      </div>
    </BaseModal>
  </div>

  <Teleport to="body">
    <div
      v-if="menuRow"
      :id="menuId"
      ref="menuRef"
      role="menu"
      aria-label="友链操作"
      class="dropdown-menu"
      :style="menuStyle"
    >
      <button
        role="menuitem"
        tabindex="-1"
        class="menu-item"
        :disabled="acting"
        @click="openEdit(menuRow)"
      >
        编辑
      </button>
      <div class="menu-divider" role="separator" />
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
/* 地址归入站点、申请人与时间归为一列，给审核留言留出阅读空间。 */
.data-table {
  min-width: 880px;
}
.applicant-cell > span,
.applicant-cell > time {
  display: block;
}
.applicant-cell > time {
  margin-top: 6px;
  font-size: 11px;
}
.data-table td.application-message {
  white-space: normal;
}
.application-message > span {
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
  font-size: 12px;
  line-height: 1.8;
  color: var(--admin-text-secondary);
}

/* 列宽、状态徽章、下拉菜单均在 admin/styles/variables.css，这里只留本页独有的单元格 */

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

.site-names .cell-muted {
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

.reject-hint {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: var(--admin-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-field {
  margin-bottom: 14px;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--admin-text-secondary);
}

/* 手机保留完整审核信息，按站点、申请、留言、操作纵向阅读。 */
@media (max-width: 768px) {
  .data-table {
    min-width: 0;
    display: block;
  }
  .data-table thead {
    display: none;
  }
  .data-table tbody {
    display: block;
  }
  .data-table tbody tr {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    padding: 20px 16px;
    border-bottom: 1px solid var(--admin-border);
    gap: 14px 8px;
  }
  .data-table td {
    display: block;
    width: auto;
    border: 0;
    padding: 0;
  }
  .data-table td.col-main,
  .data-table td.applicant-cell,
  .data-table td.application-message,
  .data-table td.empty-cell {
    grid-column: 1 / -1;
  }
  .data-table td.applicant-cell {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 8px 12px;
    font-size: 11px;
  }
  .applicant-cell > time {
    margin: 0;
  }
  .application-message > span {
    -webkit-line-clamp: unset;
  }
  .data-table td.col-status,
  .data-table td.col-actions {
    padding-top: 12px;
    border-top: 1px solid var(--admin-border-soft);
  }
  .data-table td.col-actions {
    min-width: 120px;
  }
}
</style>
