<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/useUserStore'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import {
  getUsers,
  getUserById,
  getUserStats,
  updateUsersStatus,
  deleteUsers,
  restoreUsers,
  permanentDeleteUsers,
  resetUserPassword,
  type UserListItem,
  type UserDetail,
  type PageVO,
} from '@/api/admin/user'

const router = useRouter()
const toast = useToast()
const confirm = useConfirm()
const { userInfo } = storeToRefs(useUserStore())

const loading = ref(false)

// ── Tab ───────────────────────────────────────────────────────────────────────
type TabKey = 'all' | 'enabled' | 'disabled' | 'deleted'
const activeTab = ref<TabKey>('all')

const TAB_STATUS: Record<TabKey, number | undefined> = {
  all: undefined,
  enabled: 1,
  disabled: 0,
  deleted: undefined,
}

function switchTab(tab: TabKey) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  selected.clear()
  pagination.pageNum = 1
  fetchUsers()
}

// ── Selection ─────────────────────────────────────────────────────────────────
const selected = reactive(new Set<number>())

const allChecked = computed(
  () =>
    pageData.value.content.length > 0 &&
    pageData.value.content.every((r) => selected.has(r.userId)),
)
const someChecked = computed(
  () => pageData.value.content.some((r) => selected.has(r.userId)) && !allChecked.value,
)

function toggleAll() {
  if (allChecked.value) pageData.value.content.forEach((r) => selected.delete(r.userId))
  else pageData.value.content.forEach((r) => selected.add(r.userId))
}

function toggleRow(id: number) {
  if (selected.has(id)) selected.delete(id)
  else selected.add(id)
}

const selectedCount = computed(() => selected.size)

// ── Filters & pagination ──────────────────────────────────────────────────────
const filters = reactive({ username: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10 })
const pageData = ref<PageVO<UserListItem>>({
  pageNum: 1,
  pageSize: 10,
  totalPages: 0,
  totalElements: 0,
  hasNext: false,
  hasPrevious: false,
  content: [],
})

async function fetchUsers() {
  loading.value = true
  try {
    pageData.value = await getUsers({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      username: filters.username || undefined,
      status: TAB_STATUS[activeTab.value],
      deleted: activeTab.value === 'deleted' ? 1 : 0,
    })
  } finally {
    loading.value = false
  }
}

function clearUsernameFilter() {
  filters.username = ''
  handleSearch()
}

function handleSearch() {
  pagination.pageNum = 1
  fetchUsers()
}
function handleReset() {
  filters.username = ''
  pagination.pageNum = 1
  fetchUsers()
}

// ── Tab counts ────────────────────────────────────────────────────────────────
const tabCounts = reactive({ all: 0, enabled: 0, disabled: 0, deleted: 0 })

async function fetchTabCounts() {
  try {
    const stats = await getUserStats()
    tabCounts.all = stats.total
    tabCounts.enabled = stats.enabled
    tabCounts.disabled = stats.disabled
    tabCounts.deleted = stats.deleted
  } catch {
    /* 统计失败不影响主流程 */
  }
}

onMounted(() => {
  fetchUsers()
  fetchTabCounts()
})

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

// ── Single row operations ─────────────────────────────────────────────────────
async function handleToggleStatus(row: UserListItem) {
  if (row.userId === userInfo.value?.userId) return
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
    await confirm(`确认重置「${row.username}」的密码？`, '重置密码', { confirmText: '重置密码' })
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

async function handleDelete(row: UserListItem) {
  closeMenu()
  try {
    await confirm(`确认删除用户「${row.username}」？删除后可在「已删除」中恢复。`, '删除用户', {
      confirmText: '删除',
      danger: true,
    })
  } catch {
    return
  }
  try {
    await deleteUsers([row.userId])
    toast.success('用户已删除')
    fetchUsers()
    fetchTabCounts()
  } catch {
    toast.error('删除失败')
  }
}

async function handleRestore(row: UserListItem) {
  closeMenu()
  try {
    await confirm(`确认恢复用户「${row.username}」？`, '恢复用户', { confirmText: '恢复' })
  } catch {
    return
  }
  try {
    await restoreUsers([row.userId])
    toast.success('用户已恢复')
    fetchUsers()
    fetchTabCounts()
  } catch {
    toast.error('恢复失败')
  }
}

async function handlePermanentDelete(row: UserListItem) {
  closeMenu()
  try {
    await confirm(`彻底删除「${row.username}」后数据将无法恢复，确认继续？`, '彻底删除', {
      confirmText: '彻底删除',
      danger: true,
    })
  } catch {
    return
  }
  try {
    await permanentDeleteUsers([row.userId])
    toast.success('用户已彻底删除')
    fetchUsers()
    fetchTabCounts()
  } catch {
    toast.error('操作失败')
  }
}

// ── Batch operations ──────────────────────────────────────────────────────────
function getSelectedIds() {
  return [...selected]
}

function getTargets() {
  return pageData.value.content.filter(
    (r) => selected.has(r.userId) && r.userId !== userInfo.value?.userId,
  )
}

async function handleBatchStatus(status: 0 | 1) {
  const targets = getTargets()
  if (targets.length === 0) {
    toast.warning('已排除当前登录账号，无可操作的用户')
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

async function handleBatchDelete() {
  const targets = getTargets()
  if (targets.length === 0) {
    toast.warning('已排除当前登录账号，无可删除的用户')
    return
  }
  try {
    await confirm(
      `确认删除选中的 ${targets.length} 个用户？删除后可在「已删除」中恢复。`,
      '批量删除',
      { confirmText: '删除', danger: true },
    )
  } catch {
    return
  }
  try {
    await deleteUsers(targets.map((r) => r.userId))
    toast.success(`已删除 ${targets.length} 个用户`)
    selected.clear()
    fetchUsers()
    fetchTabCounts()
  } catch {
    toast.error('删除失败')
  }
}

async function handleBatchRestore() {
  const ids = getSelectedIds()
  try {
    await confirm(`确认恢复选中的 ${ids.length} 个用户？`, '批量恢复', { confirmText: '恢复' })
  } catch {
    return
  }
  try {
    await restoreUsers(ids)
    toast.success(`已恢复 ${ids.length} 个用户`)
    selected.clear()
    fetchUsers()
    fetchTabCounts()
  } catch {
    toast.error('恢复失败')
  }
}

async function handleBatchPermanentDelete() {
  const ids = getSelectedIds()
  try {
    await confirm(`彻底删除后数据将无法恢复，确认继续？`, `彻底删除 ${ids.length} 个用户`, {
      confirmText: '彻底删除',
      danger: true,
    })
  } catch {
    return
  }
  try {
    await permanentDeleteUsers(ids)
    toast.success('已彻底删除')
    selected.clear()
    fetchUsers()
    fetchTabCounts()
  } catch {
    toast.error('操作失败')
  }
}

// ── Pagination ────────────────────────────────────────────────────────────────
const totalPages = computed(
  () => Math.ceil(pageData.value.totalElements / pagination.pageSize) || 1,
)

function goPage(n: number) {
  pagination.pageNum = n
  fetchUsers()
}

function pageSizeChange(e: Event) {
  pagination.pageSize = Number((e.target as HTMLSelectElement).value)
  pagination.pageNum = 1
  fetchUsers()
}

const pageNumbers = computed(() => {
  const cur = pagination.pageNum,
    total = totalPages.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages: (number | '…')[] = [1]
  if (cur > 3) pages.push('…')
  for (let i = Math.max(2, cur - 1); i <= Math.min(total - 1, cur + 1); i++) pages.push(i)
  if (cur < total - 2) pages.push('…')
  pages.push(total)
  return pages
})

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
            v-for="tab in ['all', 'enabled', 'disabled', 'deleted'] as TabKey[]"
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
                    : '已删除'
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
              v-model="filters.username"
              class="search-input"
              placeholder="搜索用户名"
              @keyup.enter="handleSearch"
            />
            <button v-if="filters.username" class="search-clear" @click="clearUsernameFilter">
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
          <button
            v-if="activeTab !== 'deleted'"
            class="primary-btn"
            @click="router.push('/admin/users/add')"
          >
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
            <template v-if="activeTab !== 'deleted'">
              <button class="ghost-btn ghost-btn--sm" @click="handleBatchStatus(1)">
                批量启用
              </button>
              <button class="ghost-btn ghost-btn--sm" @click="handleBatchStatus(0)">
                批量禁用
              </button>
              <button class="ghost-btn ghost-btn--sm ghost-btn--danger" @click="handleBatchDelete">
                批量删除
              </button>
            </template>
            <template v-else>
              <button class="ghost-btn ghost-btn--sm" @click="handleBatchRestore">批量恢复</button>
              <button
                class="ghost-btn ghost-btn--sm ghost-btn--danger"
                @click="handleBatchPermanentDelete"
              >
                彻底删除
              </button>
            </template>
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
            <tr v-if="pageData.content.length === 0 && !loading">
              <td colspan="7" class="empty-cell">
                <div class="empty-state">
                  <svg viewBox="0 0 24 24" fill="currentColor" class="empty-icon">
                    <path
                      d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
                    />
                  </svg>
                  <span>{{ activeTab === 'deleted' ? '没有已删除的用户' : '暂无用户' }}</span>
                </div>
              </td>
            </tr>
            <tr
              v-for="row in pageData.content"
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
                    <img v-if="row.avatar" :src="row.avatar" :alt="row.username" />
                    <span v-else>{{ row.username?.[0]?.toUpperCase() ?? '?' }}</span>
                  </div>
                  <div class="user-names">
                    <span class="user-name">{{ row.username }}</span>
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
                <span
                  class="status-badge"
                  :class="row.status === 1 ? 'status-badge--enabled' : 'status-badge--disabled'"
                >
                  {{ row.status === 1 ? '启用' : '禁用' }}
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
                        :class="{ 'menu-item--disabled': row.userId === userInfo?.userId }"
                        :disabled="row.userId === userInfo?.userId"
                        @click="handleToggleStatus(row)"
                      >
                        {{ row.status === 1 ? '禁用' : '启用' }}
                      </button>
                      <button class="menu-item" @click="handleResetPassword(row)">重置密码</button>
                      <div class="menu-divider" />
                      <template v-if="activeTab !== 'deleted'">
                        <button class="menu-item menu-item--danger" @click="handleDelete(row)">
                          删除
                        </button>
                      </template>
                      <template v-else>
                        <button class="menu-item" @click="handleRestore(row)">恢复</button>
                        <button
                          class="menu-item menu-item--danger"
                          @click="handlePermanentDelete(row)"
                        >
                          彻底删除
                        </button>
                      </template>
                    </div>
                  </Teleport>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Mobile card list -->
        <div class="mobile-list">
          <div
            v-if="pageData.content.length === 0 && !loading"
            class="empty-state empty-state--mobile"
          >
            <svg viewBox="0 0 24 24" fill="currentColor" class="empty-icon">
              <path
                d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
              />
            </svg>
            <span>{{ activeTab === 'deleted' ? '没有已删除的用户' : '暂无用户' }}</span>
          </div>
          <div v-for="row in pageData.content" :key="row.userId" class="mobile-card">
            <div class="mc-main">
              <div class="user-avatar">
                <img v-if="row.avatar" :src="row.avatar" :alt="row.username" />
                <span v-else>{{ row.username?.[0]?.toUpperCase() ?? '?' }}</span>
              </div>
              <div class="mc-info">
                <div class="mc-name-row">
                  <span class="user-name">{{ row.username }}</span>
                  <span
                    class="role-badge"
                    :class="row.userRole === 1 ? 'role-badge--admin' : 'role-badge--user'"
                  >
                    {{ row.userRole === 1 ? '管理员' : '普通用户' }}
                  </span>
                  <span
                    class="status-badge"
                    :class="row.status === 1 ? 'status-badge--enabled' : 'status-badge--disabled'"
                  >
                    {{ row.status === 1 ? '启用' : '禁用' }}
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
                    :disabled="row.userId === userInfo?.userId"
                    @click="handleToggleStatus(row)"
                  >
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </button>
                  <button class="menu-item" @click="handleResetPassword(row)">重置密码</button>
                  <div class="menu-divider" />
                  <template v-if="activeTab !== 'deleted'">
                    <button class="menu-item menu-item--danger" @click="handleDelete(row)">
                      删除
                    </button>
                  </template>
                  <template v-else>
                    <button class="menu-item" @click="handleRestore(row)">恢复</button>
                    <button class="menu-item menu-item--danger" @click="handlePermanentDelete(row)">
                      彻底删除
                    </button>
                  </template>
                </div>
              </Teleport>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Pagination ── -->
      <div v-if="pageData.totalElements > 0" class="pagination-bar">
        <span class="pagination-total">共 {{ pageData.totalElements }} 人</span>
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
              <img v-if="detailUser.avatar" :src="detailUser.avatar" :alt="detailUser.username" />
              <span v-else>{{ detailUser.username?.[0]?.toUpperCase() ?? '?' }}</span>
            </div>
            <div class="dg-banner-info">
              <span class="dg-name">{{ detailUser.nickname || detailUser.username }}</span>
              <span class="dg-username">@{{ detailUser.username }}</span>
              <div class="dg-badges">
                <span
                  class="role-badge"
                  :class="detailUser.userRole === 1 ? 'role-badge--admin' : 'role-badge--user'"
                >
                  {{ detailUser.userRole === 1 ? '管理员' : '普通用户' }}
                </span>
                <span
                  class="status-badge"
                  :class="
                    detailUser.status === 1 ? 'status-badge--enabled' : 'status-badge--disabled'
                  "
                >
                  {{ detailUser.status === 1 ? '启用' : '禁用' }}
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
              <span class="dg-label">昵称</span>
              <span class="dg-value">{{ detailUser.nickname || '—' }}</span>
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
  margin-right: 4px;
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

.dg-username {
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
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
  .pagination-bar {
    padding: 10px 12px;
    flex-wrap: wrap;
    gap: 6px;
  }
  .page-size-select {
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
