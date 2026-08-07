<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/useUserStore'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import AdminPagination from '@/admin/components/AdminPagination.vue'
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

// ── Tab ───────────────────────────────────────────────────────────────────────
type TabKey = 'all' | 'enabled' | 'disabled' | 'deactivated'

const TAB_STATE: Record<TabKey, UserState | ''> = {
  all: '',
  enabled: 'ENABLED',
  disabled: 'DISABLED',
  deactivated: 'DEACTIVATED',
}

function switchTab(tab: TabKey) {
  if (activeTab.value === tab) return
  selected.clear()
  filters.tab = tab
}

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
  sanitize: (f) => {
    if (!(f.tab in TAB_STATE)) f.tab = 'all'
  },
  onError: () => toast.error('加载用户失败'),
})

const { filters, loading, pageNum, pageSize, total, totalPages, pageNumbers } = query
const users = query.items
const activeTab = computed(() => filters.tab)
const fetchUsers = query.load

function clearKeywordFilter() {
  filters.keyword = ''
}

function handleReset() {
  selected.clear()
  query.reset()
}

// ── Tab counts ────────────────────────────────────────────────────────────────
const tabCounts = reactive({ all: 0, enabled: 0, disabled: 0, deactivated: 0 })

async function fetchTabCounts() {
  try {
    const stats = await getUserStats()
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

// ── Row dropdown menu ─────────────────────────────────────────────────────────
// 菜单 Teleport 到 body 并用 fixed 定位：.table-wrap 的 overflow-x: auto 会让
// overflow-y 被算成 auto，绝对定位的菜单一旦超出容器底边就会被裁掉（末行菜单
// 因此被分页栏"吃掉"），这种裁剪不是层叠问题，调 z-index 无效。
const openMenuId = ref<number | null>(null)
const menuStyle = ref<Record<string, string>>({})

// 菜单最多 6 项 + 2 条分隔线，取略保守的高度用于判断翻转
const MENU_MAX_HEIGHT = 220

function toggleMenu(id: number, ev?: MouseEvent, align: 'right' | 'left' = 'right') {
  if (openMenuId.value === id) {
    openMenuId.value = null
    return
  }
  const btn = ev?.currentTarget as HTMLElement | undefined
  if (btn) {
    const r = btn.getBoundingClientRect()
    const flipUp = window.innerHeight - r.bottom < MENU_MAX_HEIGHT
    menuStyle.value = {
      ...(align === 'right'
        ? { right: `${window.innerWidth - r.right}px` }
        : { left: `${r.left}px` }),
      ...(flipUp
        ? { bottom: `${window.innerHeight - r.top + 4}px` }
        : { top: `${r.bottom + 4}px` }),
    }
  }
  openMenuId.value = id
}

function closeMenu() {
  openMenuId.value = null
}

// fixed 定位不跟随滚动，滚动时直接关闭而非重算位置
onMounted(() => window.addEventListener('scroll', closeMenu, true))
onUnmounted(() => window.removeEventListener('scroll', closeMenu, true))

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
  if (isDeactivated(row)) return 'status-badge--deactivated'
  return row.status === 1 ? 'status-badge--enabled' : 'status-badge--disabled'
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

// ── Helpers ───────────────────────────────────────────────────────────────────
function relativeTime(d: string) {
  const diff = Date.now() - new Date(d).getTime()
  const m = Math.floor(diff / 60000),
    h = Math.floor(diff / 3600000),
    days = Math.floor(diff / 86400000)
  if (m < 1) return '刚刚'
  if (m < 60) return `${m} 分钟前`
  if (h < 24) return `${h} 小时前`
  if (days < 30) return `${days} 天前`
  if (days < 365) return `${Math.floor(days / 30)} 个月前`
  return `${Math.floor(days / 365)} 年前`
}
</script>

<template>
  <div class="users-page">
    <div class="main-card">
      <!-- ── Header ── -->
      <div class="card-header">
        <div class="view-tabs">
          <button
            v-for="tab in ['all', 'enabled', 'disabled', 'deactivated'] as TabKey[]"
            :key="tab"
            class="view-tab"
            :class="{ 'view-tab--active': activeTab === tab }"
            @click="switchTab(tab)"
          >
            {{
              tab === 'all'
                ? '全部'
                : tab === 'enabled'
                  ? '启用'
                  : tab === 'disabled'
                    ? '禁用'
                    : '已注销'
            }}
            <span v-if="tabCounts[tab] > 0" class="tab-count">{{ tabCounts[tab] }}</span>
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
              placeholder="搜索用户名"
              @keyup.enter="query.applyFilters"
            />
            <button v-if="filters.keyword" class="search-clear" @click="clearKeywordFilter">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6 6 18M6 6l12 12" />
              </svg>
            </button>
          </div>
          <button class="icon-btn" title="重置" @click="handleReset">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
              <path d="M3 3v5h5" />
            </svg>
          </button>
          <button class="primary-btn" @click="router.push('/admin/users/add')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14" />
            </svg>
            新增用户
          </button>
        </div>
      </div>

      <!-- ── Selection bar ── -->
      <Transition name="sel-bar">
        <div v-if="selectedCount > 0" class="selection-bar">
          <span class="sel-count"
            >已选 <b>{{ selectedCount }}</b> 项</span
          >
          <div class="sel-actions">
            <button class="ghost-btn ghost-btn--sm" @click="handleBatchStatus(1)">批量启用</button>
            <button class="ghost-btn ghost-btn--sm" @click="handleBatchStatus(0)">批量禁用</button>
            <button
              class="ghost-btn ghost-btn--sm ghost-btn--danger"
              @click="handleBatchDeactivate"
            >
              批量注销
            </button>
          </div>
          <button class="cancel-btn" @click="selected.clear()">取消选择</button>
        </div>
      </Transition>

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
              <th class="col-user">用户</th>
              <th class="col-email">邮箱</th>
              <th class="col-role">角色</th>
              <th class="col-status">状态</th>
              <th class="col-time">注册时间</th>
              <th class="col-actions" />
            </tr>
          </thead>
          <tbody>
            <tr v-if="users.length === 0 && !loading">
              <td colspan="7" class="empty-cell">
                <div class="empty-state">
                  <svg viewBox="0 0 24 24" fill="currentColor" class="empty-icon">
                    <path
                      d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
                    />
                  </svg>
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
              <td class="col-user">
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
              <td class="col-email">
                <span class="cell-muted">{{ row.email || '—' }}</span>
              </td>
              <td class="col-role">
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
                <span class="cell-muted" :title="row.createTime">{{
                  relativeTime(row.createTime)
                }}</span>
              </td>
              <td class="col-actions">
                <div v-click-outside="closeMenu" class="menu-wrap">
                  <button class="more-btn" @click.stop="toggleMenu(row.userId, $event)">
                    <svg viewBox="0 0 24 24" fill="currentColor">
                      <circle cx="12" cy="5" r="1.5" />
                      <circle cx="12" cy="12" r="1.5" />
                      <circle cx="12" cy="19" r="1.5" />
                    </svg>
                  </button>
                  <Teleport to="body">
                    <div v-if="openMenuId === row.userId" class="dropdown-menu" :style="menuStyle">
                      <button class="menu-item" @click="openDetail(row)">用户信息</button>
                      <div class="menu-divider" />
                      <button
                        class="menu-item"
                        :class="{ 'menu-item--disabled': !canChangeStatus(row) }"
                        :disabled="!canChangeStatus(row)"
                        @click="handleToggleStatus(row)"
                      >
                        {{ row.status === 1 ? '禁用' : '启用' }}
                      </button>
                      <button
                        class="menu-item"
                        :class="{ 'menu-item--disabled': isDeactivated(row) }"
                        :disabled="isDeactivated(row)"
                        @click="handleResetPassword(row)"
                      >
                        重置密码
                      </button>
                      <div class="menu-divider" />
                      <button
                        class="menu-item menu-item--danger"
                        :class="{ 'menu-item--disabled': isDeactivated(row) }"
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
            <svg viewBox="0 0 24 24" fill="currentColor" class="empty-icon">
              <path
                d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
              />
            </svg>
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
                <span class="cell-muted mc-time">注册于 {{ relativeTime(row.createTime) }}</span>
              </div>
            </div>
            <div v-click-outside="closeMenu" class="menu-wrap">
              <button class="more-btn" @click.stop="toggleMenu(row.userId, $event, 'left')">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <circle cx="12" cy="5" r="1.5" />
                  <circle cx="12" cy="12" r="1.5" />
                  <circle cx="12" cy="19" r="1.5" />
                </svg>
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

      <!-- ── Pagination ── -->
      <AdminPagination
        :total="total"
        :page-num="pageNum"
        :page-size="pageSize"
        :total-pages="totalPages"
        :page-numbers="pageNumbers"
        unit="人"
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
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
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
              <span class="dg-value">{{ detailUser.createTime }}</span>
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
.users-page {
  display: flex;
  flex-direction: column;
}

/* ── Header ── */

.card-header {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  border-bottom: 1px solid var(--admin-sidebar-border);
  padding: 0 16px 0 20px;
  gap: 12px;
}

/* ── Tabs ── */

/* .view-tabs / .view-tab / .tab-count 见 admin/styles/variables.css */

/* ── Search ── */

.search-input {
  height: 32px;
  width: 180px;
  padding: 0 28px 0 28px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  background: var(--admin-surface-input);
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-text-primary);
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

/* ── Buttons ── */

/* ── Selection bar ── */

.ghost-btn--danger {
  color: var(--admin-danger);
  border-color: var(--admin-danger-border);
}

.cancel-btn {
  margin-left: auto;
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  background: none;
  border: none;
  cursor: pointer;
}

.sel-bar-enter-active,

.sel-bar-enter-from,

/* ── Table ── */

.table-wrap {
  position: relative;
  min-height: 120px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
.table-wrap--loading {
  pointer-events: none;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.col-user {
  min-width: 160px;
}
.col-email {
  min-width: 160px;
}
.col-role {
  width: 90px;
}
.col-status {
  width: 72px;
}
.col-time {
  width: 110px;
}
.col-actions {
  width: 52px;
  text-align: center;
}

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

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--admin-radius);
  font-size: 11.5px;
  font-weight: 500;
  white-space: nowrap;
}
.status-badge--enabled {
  background: rgba(var(--admin-status-ok-rgb), 0.08);
  color: var(--admin-status-ok);
  border: 1px solid rgba(var(--admin-status-ok-rgb), 0.2);
}
.status-badge--disabled {
  background: var(--admin-danger-bg-soft);
  color: var(--admin-danger-on-soft);
  border: 1px solid var(--admin-danger-border);
}
/* 注销是用户自己的终态，不是告警，用中性灰与「禁用」的红区分开 */
.status-badge--deactivated {
  background: transparent;
  color: var(--admin-sidebar-text-muted);
  border: 1px solid var(--admin-sidebar-border);
}

/* ── Dropdown menu ── */

.menu-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.more-btn {
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
  transition:
    background 0.15s,
    color 0.15s;
}
.more-btn svg {
  width: 16px;
  height: 16px;
}
.more-btn:hover {
  background: var(--admin-sidebar-hover);
  color: var(--admin-sidebar-text);
}

/* Teleport 到 body，位置由 toggleMenu 按触发按钮实测坐标写入 inline style */
.dropdown-menu {
  position: fixed;
  /* 低于 .dialog-overlay(1000)：菜单不该盖住对话框 */
  z-index: 900;
  min-width: 130px;
  background: var(--admin-surface-input);
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.menu-item {
  display: block;
  width: 100%;
  padding: 8px 14px;
  text-align: left;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  background: none;
  border: none;
  cursor: pointer;
  transition: background 0.12s;
}
.menu-item:hover {
  background: var(--admin-sidebar-hover);
}
.menu-item--danger {
  color: var(--admin-danger);
}
.menu-item--danger:hover {
  background: var(--admin-danger-bg-soft);
}
.menu-item--disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.menu-item--disabled:hover {
  background: none;
}

.menu-divider {
  height: 1px;
  background: var(--admin-sidebar-border);
  margin: 2px 0;
}

/* ── Empty ── */

.empty-cell {
  padding: 0 !important;
  border: none !important;
}

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
