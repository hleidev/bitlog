<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/useUserStore'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import { formatDateTime } from '@/utils/format'
import AdminIcon from '@/admin/components/AdminIcon.vue'
import AdminListHeader from '@/admin/components/AdminListHeader.vue'
import AdminPagination from '@/admin/components/AdminPagination.vue'
import AdminSelectionBar from '@/admin/components/AdminSelectionBar.vue'
import { useRowMenu } from '@/admin/composables/useRowMenu'
import { useListQuery } from '@/composables/useListQuery'
import {
  getUsers,
  getUserById,
  getUserStats,
  updateUsersStatus,
  deactivateUsers,
  resetUserPassword,
  type UserListItem,
  type UserDetail,
  type UserState,
} from '@/api/admin/user'

const router = useRouter()
const toast = useToast()
const confirm = useConfirm()
const { userInfo } = storeToRefs(useUserStore())

/** 本页量词，批量条与操作提示共用 */
const UNIT = '人'

const { openMenuId, menuStyle, toggleMenu, closeMenu } = useRowMenu()

// ── Tab ───────────────────────────────────────────────────────────────────────
type TabKey = 'all' | 'enabled' | 'disabled' | 'deactivated'

const TAB_STATE: Record<TabKey, UserState | ''> = {
  all: '',
  enabled: 'ENABLED',
  disabled: 'DISABLED',
  deactivated: 'DEACTIVATED',
}

// AdminListHeader 的 model 是 string，这里做一层窄化桥接；切 tab 时清掉选中
const activeTab = computed({
  get: (): string => filters.tab,
  set: (key: string) => {
    if (filters.tab === key) return
    selected.clear()
    filters.tab = key as TabKey
  },
})

const tabs = computed(() => [
  { key: 'all', label: '全部', count: tabCounts.all },
  { key: 'enabled', label: '启用', count: tabCounts.enabled },
  { key: 'disabled', label: '禁用', count: tabCounts.disabled },
  { key: 'deactivated', label: '已注销', count: tabCounts.deactivated },
])

// ── Selection ─────────────────────────────────────────────────────────────────
const selected = reactive(new Set<number>())

const allChecked = computed(
  () => users.value.length > 0 && users.value.every((r) => selected.has(r.userId)),
)
const someChecked = computed(
  () => users.value.some((r) => selected.has(r.userId)) && !allChecked.value,
)

function toggleAll() {
  if (allChecked.value) users.value.forEach((r) => selected.delete(r.userId))
  else users.value.forEach((r) => selected.add(r.userId))
}

function toggleRow(id: number) {
  if (selected.has(id)) selected.delete(id)
  else selected.add(id)
}

const selectedCount = computed(() => selected.size)

// ── Filters & pagination ──────────────────────────────────────────────────────
const query = useListQuery({
  filters: { tab: 'all' as TabKey, keyword: '' },
  toParams: (f) => ({ keyword: f.keyword, state: TAB_STATE[f.tab] || undefined }),
  fetch: (params) => getUsers(params),
  debounce: ['keyword'],
  syncUrl: true,
  onFiltersApplied: fetchTabCounts,
  sanitize: (f) => {
    // hasOwn 而非 in：in 走原型链，?tab=constructor 会被放行
    if (!Object.hasOwn(TAB_STATE, f.tab)) f.tab = 'all'
  },
  onError: () => toast.error('加载用户失败'),
})

const { filters, loading, pageNum, pageSize, total, totalPages, pageNumbers } = query
const users = query.items
const fetchUsers = query.load

// 重置 = 清空全部筛选并回到「全部」，与其余列表页一致
function handleReset() {
  selected.clear()
  filters.keyword = ''
  filters.tab = 'all'
  query.applyFilters()
}

// ── Tab counts ────────────────────────────────────────────────────────────────
const tabCounts = reactive({ all: 0, enabled: 0, disabled: 0, deactivated: 0 })

async function fetchTabCounts() {
  try {
    const stats = await getUserStats({ keyword: filters.keyword })
    tabCounts.all = stats.total
    tabCounts.enabled = stats.enabled
    tabCounts.disabled = stats.disabled
    tabCounts.deactivated = stats.deactivated
  } catch {
    /* 统计失败不影响主流程 */
  }
}

// 列表首屏由 useListQuery 自行拉取，这里只补统计
onMounted(fetchTabCounts)

// 注销是终态，标记在 deleted 而非 status 上；后端不再允许改状态或重置密码
type StatusRow = { username: string; status: number; deleted: number }

function isDeactivated(row: StatusRow) {
  return row.deleted === 1
}

function statusLabel(row: StatusRow) {
  if (isDeactivated(row)) return '已注销'
  return row.status === 1 ? '启用' : '禁用'
}

function statusClass(row: StatusRow) {
  if (isDeactivated(row)) return 'status-badge--muted'
  return row.status === 1 ? 'status-badge--ok' : 'status-badge--danger'
}

// 注销时 username 被覆写成墓碑值（del_xxxxxxxx），直接渲染对管理员无意义
function displayName(row: StatusRow) {
  return isDeactivated(row) ? '已注销用户' : row.username
}

function canChangeStatus(row: UserListItem) {
  return row.userId !== userInfo.value?.userId && !isDeactivated(row)
}

// ── Single row operations ─────────────────────────────────────────────────────
async function handleToggleStatus(row: UserListItem) {
  if (row.userId === userInfo.value?.userId || isDeactivated(row)) return
  closeMenu()
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateUsersStatus([row.userId], newStatus as 0 | 1)
    toast.success(newStatus === 1 ? '已启用' : '已禁用')
    fetchUsers()
    fetchTabCounts()
  } catch {
    toast.error('操作失败')
  }
}

async function handleResetPassword(row: UserListItem) {
  closeMenu()
  try {
    await confirm(`确认重置「${displayName(row)}」的密码？`, '重置密码', {
      confirmText: '重置密码',
    })
  } catch {
    return
  }
  try {
    const result = await resetUserPassword(row.userId)
    newPassword.value = result.newPassword
    passwordDialogVisible.value = true
    try {
      await navigator.clipboard.writeText(result.newPassword)
    } catch {
      /* 静默 */
    }
  } catch {
    toast.error('重置失败')
  }
}

const DEACTIVATE_WARNING =
  '注销后邮箱与用户名会被释放、个人资料清空，其发表的评论保留但署名转为「已注销用户」。此操作不可撤销。'

async function handleDeactivate(row: UserListItem) {
  closeMenu()
  try {
    await confirm(`确认注销「${displayName(row)}」？${DEACTIVATE_WARNING}`, '注销账号', {
      confirmText: '注销',
      danger: true,
    })
  } catch {
    return
  }
  try {
    await deactivateUsers([row.userId])
    toast.success('账号已注销')
    fetchUsers()
    fetchTabCounts()
  } catch {
    toast.error('注销失败')
  }
}

// ── Batch operations ──────────────────────────────────────────────────────────
function getTargets() {
  return users.value.filter((r) => selected.has(r.userId) && r.userId !== userInfo.value?.userId)
}

async function handleBatchStatus(status: 0 | 1) {
  // 已注销是终态，混在批次里会被后端整批拒绝，先剔除
  const targets = getTargets().filter((r) => !isDeactivated(r))
  if (targets.length === 0) {
    toast.warning('已排除当前登录账号与已注销账号，无可操作的用户')
    return
  }
  const label = status === 1 ? '启用' : '禁用'
  try {
    await confirm(`确认批量${label}选中的 ${targets.length} 个用户？`, `批量${label}`, {
      confirmText: label,
    })
  } catch {
    return
  }
  try {
    await updateUsersStatus(
      targets.map((r) => r.userId),
      status,
    )
    toast.success(`已${label} ${targets.length} 个用户`)
    selected.clear()
    fetchUsers()
    fetchTabCounts()
  } catch {
    toast.error('操作失败')
  }
}

async function handleBatchDeactivate() {
  // 已注销的再注销一次会被后端整批拒绝，先剔除
  const targets = getTargets().filter((r) => !isDeactivated(r))
  if (targets.length === 0) {
    toast.warning('已排除当前登录账号与已注销账号，无可操作的用户')
    return
  }
  try {
    await confirm(`确认注销选中的 ${targets.length} 个账号？${DEACTIVATE_WARNING}`, '批量注销', {
      confirmText: '注销',
      danger: true,
    })
  } catch {
    return
  }
  try {
    await deactivateUsers(targets.map((r) => r.userId))
    toast.success(`已注销 ${targets.length} 个账号`)
    selected.clear()
    fetchUsers()
    fetchTabCounts()
  } catch {
    toast.error('注销失败')
  }
}

// ── Password dialog ───────────────────────────────────────────────────────────
const passwordDialogVisible = ref(false)
const newPassword = ref('')

// ── Detail dialog ─────────────────────────────────────────────────────────────
const detailVisible = ref(false)
const detailUser = ref<UserDetail | null>(null)
const detailLoading = ref(false)

async function openDetail(row: UserListItem) {
  closeMenu()
  detailVisible.value = true
  detailLoading.value = true
  detailUser.value = null
  try {
    detailUser.value = await getUserById(row.userId)
  } catch {
    detailVisible.value = false
    toast.error('获取用户信息失败')
  } finally {
    detailLoading.value = false
  }
}
</script>

<template>
  <div class="users-page">
    <div class="main-card">
      <AdminListHeader
        v-model:active-tab="activeTab"
        v-model:keyword="filters.keyword"
        :tabs="tabs"
        search-placeholder="搜索用户名"
        action-label="新增用户"
        @search="query.applyFilters"
        @reset="handleReset"
        @action="router.push('/admin/users/add')"
      />

      <AdminSelectionBar :count="selectedCount" :unit="UNIT" @clear="selected.clear()">
        <button class="ghost-btn ghost-btn--sm" @click="handleBatchStatus(1)">批量启用</button>
        <button class="ghost-btn ghost-btn--sm" @click="handleBatchStatus(0)">批量禁用</button>
        <button class="ghost-btn ghost-btn--sm ghost-btn--danger" @click="handleBatchDeactivate">
          批量注销
        </button>
      </AdminSelectionBar>

      <!-- ── Table (desktop) ── -->
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
                  :checked="allChecked"
                  :indeterminate="someChecked"
                  @change="toggleAll"
                />
              </th>
              <th class="col-main">用户</th>
              <th class="col-text">邮箱</th>
              <th class="col-narrow">角色</th>
              <th class="col-status">状态</th>
              <th class="col-time">注册时间</th>
              <th class="col-actions" />
            </tr>
          </thead>
          <tbody>
            <tr v-if="users.length === 0 && !loading">
              <td colspan="7" class="empty-cell">
                <div class="empty-state">
                  <AdminIcon name="user" class="empty-icon" />
                  <span>{{ activeTab === 'deactivated' ? '没有已注销的账号' : '暂无用户' }}</span>
                </div>
              </td>
            </tr>
            <tr
              v-for="row in users"
              :key="row.userId"
              :class="{ 'row--selected': selected.has(row.userId) }"
            >
              <td class="col-check">
                <input
                  type="checkbox"
                  class="row-checkbox"
                  :checked="selected.has(row.userId)"
                  @change="toggleRow(row.userId)"
                />
              </td>
              <td class="col-main">
                <div class="user-cell">
                  <div class="user-avatar">
                    <img v-if="row.avatar" :src="row.avatar" :alt="displayName(row)" />
                    <span v-else>{{ displayName(row)?.[0]?.toUpperCase() ?? '?' }}</span>
                  </div>
                  <div class="user-names">
                    <span class="user-name">{{ displayName(row) }}</span>
                  </div>
                </div>
              </td>
              <td class="col-text">
                <span class="cell-muted" :title="row.email ?? ''">{{ row.email || '—' }}</span>
              </td>
              <td class="col-narrow">
                <span
                  class="role-badge"
                  :class="row.userRole === 1 ? 'role-badge--admin' : 'role-badge--user'"
                >
                  {{ row.userRole === 1 ? '管理员' : '普通用户' }}
                </span>
              </td>
              <td class="col-status">
                <span class="status-badge" :class="statusClass(row)">
                  {{ statusLabel(row) }}
                </span>
              </td>
              <td class="col-time">
                <span class="cell-muted">{{ formatDateTime(row.createTime) }}</span>
              </td>
              <td class="col-actions">
                <div v-click-outside="closeMenu" class="menu-wrap">
                  <button
                    class="more-btn"
                    title="更多"
                    @click.stop="toggleMenu(row.userId, $event)"
                  >
                    <AdminIcon name="more" />
                  </button>
                  <Teleport to="body">
                    <div v-if="openMenuId === row.userId" class="dropdown-menu" :style="menuStyle">
                      <button class="menu-item" @click="openDetail(row)">用户信息</button>
                      <div class="menu-divider" />
                      <button
                        class="menu-item"
                        :disabled="!canChangeStatus(row)"
                        @click="handleToggleStatus(row)"
                      >
                        {{ row.status === 1 ? '禁用' : '启用' }}
                      </button>
                      <button
                        class="menu-item"
                        :disabled="isDeactivated(row)"
                        @click="handleResetPassword(row)"
                      >
                        重置密码
                      </button>
                      <div class="menu-divider" />
                      <button
                        class="menu-item menu-item--danger"
                        :disabled="isDeactivated(row)"
                        @click="handleDeactivate(row)"
                      >
                        注销
                      </button>
                    </div>
                  </Teleport>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Mobile card list -->
        <div class="mobile-list">
          <div v-if="users.length === 0 && !loading" class="empty-state empty-state--mobile">
            <AdminIcon name="user" class="empty-icon" />
            <span>{{ activeTab === 'deactivated' ? '没有已注销的账号' : '暂无用户' }}</span>
          </div>
          <div v-for="row in users" :key="row.userId" class="mobile-card">
            <div class="mc-main">
              <div class="user-avatar">
                <img v-if="row.avatar" :src="row.avatar" :alt="displayName(row)" />
                <span v-else>{{ displayName(row)?.[0]?.toUpperCase() ?? '?' }}</span>
              </div>
              <div class="mc-info">
                <div class="mc-name-row">
                  <span class="user-name">{{ displayName(row) }}</span>
                  <span
                    class="role-badge"
                    :class="row.userRole === 1 ? 'role-badge--admin' : 'role-badge--user'"
                  >
                    {{ row.userRole === 1 ? '管理员' : '普通用户' }}
                  </span>
                  <span class="status-badge" :class="statusClass(row)">
                    {{ statusLabel(row) }}
                  </span>
                </div>
                <span class="cell-muted mc-email">{{ row.email || '暂无邮箱' }}</span>
                <span class="cell-muted mc-time">注册于 {{ formatDateTime(row.createTime) }}</span>
              </div>
            </div>
            <div v-click-outside="closeMenu" class="menu-wrap">
              <button
                class="more-btn"
                title="更多"
                @click.stop="toggleMenu(row.userId, $event, 'left')"
              >
                <AdminIcon name="more" />
              </button>
              <Teleport to="body">
                <div v-if="openMenuId === row.userId" class="dropdown-menu" :style="menuStyle">
                  <button class="menu-item" @click="openDetail(row)">用户信息</button>
                  <div class="menu-divider" />
                  <button
                    class="menu-item"
                    :disabled="!canChangeStatus(row)"
                    @click="handleToggleStatus(row)"
                  >
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </button>
                  <button
                    class="menu-item"
                    :disabled="isDeactivated(row)"
                    @click="handleResetPassword(row)"
                  >
                    重置密码
                  </button>
                  <div class="menu-divider" />
                  <button
                    class="menu-item menu-item--danger"
                    :disabled="isDeactivated(row)"
                    @click="handleDeactivate(row)"
                  >
                    注销
                  </button>
                </div>
              </Teleport>
            </div>
          </div>
        </div>
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
    </div>
  </div>

  <!-- ── Password reset dialog ── -->
  <Teleport to="body">
    <div v-if="passwordDialogVisible" class="dialog-mask">
      <div class="dialog">
        <div class="dialog-inner">
          <div class="dialog-check">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <p class="dialog-title">密码已重置</p>
          <p class="dialog-sub">新密码</p>
          <div class="dialog-password">{{ newPassword }}</div>
          <p class="dialog-copied">已自动复制到剪贴板</p>
        </div>
        <div class="dialog-footer">
          <button class="primary-btn" @click="passwordDialogVisible = false">完成</button>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- ── User detail dialog ── -->
  <Teleport to="body">
    <div v-if="detailVisible" class="dialog-mask" @click.self="detailVisible = false">
      <div class="dialog dialog--detail">
        <button class="dialog-close" @click="detailVisible = false">
          <AdminIcon name="close" />
        </button>

        <div v-if="detailLoading" class="detail-loading">
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

        <template v-else-if="detailUser">
          <div class="dg-banner">
            <div class="dg-avatar">
              <img
                v-if="detailUser.avatar"
                :src="detailUser.avatar"
                :alt="displayName(detailUser)"
              />
              <span v-else>{{ displayName(detailUser)?.[0]?.toUpperCase() ?? '?' }}</span>
            </div>
            <div class="dg-banner-info">
              <span class="dg-name">{{ displayName(detailUser) }}</span>
              <div class="dg-badges">
                <span
                  class="role-badge"
                  :class="detailUser.userRole === 1 ? 'role-badge--admin' : 'role-badge--user'"
                >
                  {{ detailUser.userRole === 1 ? '管理员' : '普通用户' }}
                </span>
                <span class="status-badge" :class="statusClass(detailUser)">
                  {{ statusLabel(detailUser) }}
                </span>
              </div>
            </div>
          </div>

          <div class="dg-grid">
            <div class="dg-cell">
              <span class="dg-label">用户 ID</span>
              <span class="dg-value"># {{ detailUser.userId }}</span>
            </div>
            <div class="dg-cell">
              <span class="dg-label">用户名</span>
              <span class="dg-value">{{ detailUser.username || '—' }}</span>
            </div>
            <div class="dg-cell">
              <span class="dg-label">邮箱</span>
              <span class="dg-value">{{ detailUser.email || '—' }}</span>
            </div>
            <div class="dg-cell">
              <span class="dg-label">职位</span>
              <span class="dg-value">{{ detailUser.position || '—' }}</span>
            </div>
            <div class="dg-cell">
              <span class="dg-label">公司</span>
              <span class="dg-value">{{ detailUser.company || '—' }}</span>
            </div>
            <div class="dg-cell">
              <span class="dg-label">注册时间</span>
              <span class="dg-value">{{ formatDateTime(detailUser.createTime) }}</span>
            </div>
          </div>

          <div v-if="detailUser.profile" class="dg-profile">
            <span class="dg-label">个人简介</span>
            <p class="dg-profile-text">{{ detailUser.profile }}</p>
          </div>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
/* 固定列合计 40+200+100+100+150+148=738，再给主列留 240px 下限；窄于此宽度改为横向滚动，
   而不是把主列压成 0（见 variables.css 中 .data-table 的说明） */
.data-table {
  min-width: 980px;
}

.users-page {
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
  border-color: var(--admin-danger-border);
}

/* ── Table ── */

/* Mobile list hidden by default, shown at breakpoint */
.mobile-list {
  display: none;
}

/* ── User cell ── */

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
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
  border: 1px solid var(--admin-sidebar-border);
}
.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.user-names {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.user-name {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--admin-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── Badges ── */

.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--admin-radius);
  font-size: 11.5px;
  font-weight: 500;
  white-space: nowrap;
}
.role-badge--admin {
  background: var(--admin-accent-bg-strong);
  color: var(--admin-accent-dark);
}
.role-badge--user {
  background: var(--admin-sidebar-hover);
  color: var(--admin-sidebar-text);
}

/* ── Empty ── */

.empty-state--mobile {
  padding: 40px 0;
}

/* ── Pagination ── */

/* ── Dialogs ── */

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  width: min(360px, calc(100vw - 32px));
  background: var(--admin-header-bg);
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  overflow: hidden;
}

.dialog--detail {
  width: min(460px, calc(100vw - 32px));
  position: relative;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
}

.dialog-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--admin-sidebar-text-muted);
  cursor: pointer;
  transition: background 0.15s;
}
.dialog-close svg {
  width: 14px;
  height: 14px;
}
.dialog-close:hover {
  background: var(--admin-sidebar-hover);
}

.dialog-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px 24px 20px;
  text-align: center;
}

.dialog-check {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--admin-accent-bg-strong);
  color: var(--admin-accent-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}
.dialog-check svg {
  width: 20px;
  height: 20px;
}

.dialog-title {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 16px;
  font-weight: 600;
  color: var(--admin-text-primary);
  margin: 0;
}
.dialog-sub {
  font-size: 12px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  margin: 2px 0 0;
}

.dialog-password {
  font-family: 'ui-monospace', 'Menlo', monospace;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: var(--admin-accent-dark);
  background: var(--admin-accent-bg-soft);
  border: 1px solid var(--admin-accent-border);
  border-radius: 4px;
  padding: 10px 20px;
  margin: 4px 0;
}

.dialog-copied {
  font-size: 12px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  margin: 0;
}

.dialog-footer {
  padding: 0 24px 24px;
  display: flex;
  justify-content: center;
}

.detail-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

/* ── Detail dialog content ── */

.dg-banner {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px 24px 20px;
  background: var(--admin-accent-bg-subtle);
  border-bottom: 1px solid var(--admin-sidebar-border);
}

.dg-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--admin-accent-bg-strong);
  color: var(--admin-accent-dark);
  font-size: 24px;
  font-weight: 700;
  font-family: var(--font-serif, 'Lora', serif);
  border: 2px solid var(--admin-sidebar-border);
}
.dg-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.dg-banner-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.dg-name {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 17px;
  font-weight: 600;
  color: var(--admin-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dg-badges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 2px;
}

.dg-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  background: var(--admin-sidebar-border);
  margin: 0;
  border-bottom: 1px solid var(--admin-sidebar-border);
}

.dg-cell {
  background: var(--admin-header-bg);
  padding: 12px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dg-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--admin-sidebar-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.dg-value {
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-text-primary);
  font-weight: 500;
  word-break: break-all;
}

.dg-profile {
  padding: 16px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.dg-profile-text {
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  line-height: 1.6;
  margin: 0;
}

/* ── Mobile ── */

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
  .selection-bar {
    flex-wrap: wrap;
    padding: 8px 12px;
  }
  .sel-cancel {
    margin-left: 0;
  }
  .dg-grid {
    grid-template-columns: 1fr;
  }

  /* Switch from table to card list on mobile */
  .data-table {
    display: none;
  }
  .mobile-list {
    display: block;
  }

  .mobile-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid var(--admin-sidebar-border);
    gap: 12px;
  }
  .mobile-card:last-child {
    border-bottom: none;
  }

  .mc-main {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }
  .mc-info {
    display: flex;
    flex-direction: column;
    gap: 3px;
    min-width: 0;
  }
  .mc-name-row {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
  }
  .mc-email {
    font-size: 12px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 180px;
  }
  .mc-time {
    font-size: 12px;
  }
}
</style>
