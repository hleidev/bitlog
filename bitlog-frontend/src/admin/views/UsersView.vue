<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, MoreFilled, Plus } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
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
const { userInfo } = storeToRefs(useUserStore())

const loading = ref(false)
const tableRef = ref()

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
  tableRef.value?.clearSelection()
  selectedRows.value = []
  pagination.pageNum = 1
  fetchUsers()
}

// ── Password dialog ───────────────────────────────────────────────────────────
const passwordDialogVisible = ref(false)
const newPassword = ref('')

async function copyPassword(pwd: string) {
  try {
    await navigator.clipboard.writeText(pwd)
  } catch {
    // 自动复制失败时静默，用户可手动复制
  }
}

// ── Detail dialog ─────────────────────────────────────────────────────────────
const detailVisible = ref(false)
const detailUser = ref<UserDetail | null>(null)
const detailLoading = ref(false)

async function openDetail(row: UserListItem) {
  detailVisible.value = true
  detailLoading.value = true
  detailUser.value = null
  try {
    detailUser.value = await getUserById(row.userId)
  } catch {
    detailVisible.value = false
    ElMessage.error('获取用户信息失败')
  } finally {
    detailLoading.value = false
  }
}

// ── Filters & pagination ──────────────────────────────────────────────────────
const filters = reactive({
  userName: '',
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
})

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
      userName: filters.userName || undefined,
      status: TAB_STATUS[activeTab.value],
      deleted: activeTab.value === 'deleted' ? 1 : 0,
    })
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  fetchUsers()
}

function handleReset() {
  filters.userName = ''
  pagination.pageNum = 1
  fetchUsers()
}

function handlePageChange() {
  fetchUsers()
}

function handleSizeChange() {
  pagination.pageNum = 1
  fetchUsers()
}

// ── Relative time ─────────────────────────────────────────────────────────────
function relativeTime(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (hours < 24) return `${hours} 小时前`
  if (days < 30) return `${days} 天前`
  if (days < 365) return `${Math.floor(days / 30)} 个月前`
  return `${Math.floor(days / 365)} 年前`
}

// ── Row selection ─────────────────────────────────────────────────────────────
const selectedRows = ref<UserListItem[]>([])

function handleSelectionChange(rows: UserListItem[]) {
  selectedRows.value = rows
}

function handleClearSelection() {
  tableRef.value?.clearSelection()
}

// ── Batch operations — active tab ─────────────────────────────────────────────
async function handleBatchStatus(status: 0 | 1) {
  const targets = selectedRows.value.filter((r) => r.userId !== userInfo.value?.userId)
  if (targets.length === 0) {
    ElMessage.warning('已排除当前登录账号，无可操作的用户')
    return
  }
  const label = status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确认批量${label}选中的 ${targets.length} 个用户？`,
      `批量${label}`,
      { confirmButtonText: label, cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await updateUsersStatus(targets.map((r) => r.userId), status)
    ElMessage.success(`已${label} ${targets.length} 个用户`)
    handleClearSelection()
    fetchUsers()
    fetchTabCounts()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleBatchDelete() {
  const targets = selectedRows.value.filter((r) => r.userId !== userInfo.value?.userId)
  if (targets.length === 0) {
    ElMessage.warning('已排除当前登录账号，无可删除的用户')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${targets.length} 个用户？删除后可在「已删除」中恢复。`,
      '批量删除',
      { confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await deleteUsers(targets.map((r) => r.userId))
    ElMessage.success(`已删除 ${targets.length} 个用户`)
    handleClearSelection()
    fetchUsers()
    fetchTabCounts()
  } catch {
    ElMessage.error('删除失败')
  }
}

// ── Batch operations — deleted tab ────────────────────────────────────────────
async function handleBatchRestore() {
  try {
    await ElMessageBox.confirm(
      `确认恢复选中的 ${selectedRows.value.length} 个用户？`,
      '批量恢复',
      { confirmButtonText: '恢复', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await restoreUsers(selectedRows.value.map((r) => r.userId))
    ElMessage.success(`已恢复 ${selectedRows.value.length} 个用户`)
    handleClearSelection()
    fetchUsers()
    fetchTabCounts()
  } catch {
    ElMessage.error('恢复失败')
  }
}

async function handleBatchPermanentDelete() {
  try {
    await ElMessageBox.confirm(
      `彻底删除后数据将无法恢复，确认继续？`,
      `彻底删除 ${selectedRows.value.length} 个用户`,
      {
        confirmButtonText: '彻底删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }
  try {
    await permanentDeleteUsers(selectedRows.value.map((r) => r.userId))
    ElMessage.success('已彻底删除')
    handleClearSelection()
    fetchUsers()
    fetchTabCounts()
  } catch {
    ElMessage.error('操作失败')
  }
}

// ── Single row operations ─────────────────────────────────────────────────────
async function handleCommand(command: string, row: UserListItem) {
  if (command === 'detail') {
    openDetail(row)
  } else if (command === 'toggleStatus') {
    if (row.userId === userInfo.value?.userId) return
    const newStatus = row.status === 1 ? 0 : 1
    try {
      await updateUsersStatus([row.userId], newStatus as 0 | 1)
      ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
      fetchUsers()
      fetchTabCounts()
    } catch {
      ElMessage.error('操作失败')
    }
  } else if (command === 'resetPassword') {
    try {
      await ElMessageBox.confirm(
        `确认重置「${row.userName}」的密码？`,
        '重置密码',
        { confirmButtonText: '重置密码', cancelButtonText: '取消' },
      )
    } catch {
      return
    }
    try {
      const result = await resetUserPassword(row.userId)
      newPassword.value = result.newPassword
      passwordDialogVisible.value = true
      copyPassword(result.newPassword)
    } catch {
      ElMessage.error('重置失败')
    }
  } else if (command === 'delete') {
    try {
      await ElMessageBox.confirm(
        `确认删除用户「${row.userName}」？删除后可在「已删除」中恢复。`,
        '删除用户',
        { confirmButtonText: '删除', cancelButtonText: '取消' },
      )
    } catch {
      return
    }
    try {
      await deleteUsers([row.userId])
      ElMessage.success('用户已删除')
      fetchUsers()
      fetchTabCounts()
    } catch {
      ElMessage.error('删除失败')
    }
  } else if (command === 'restore') {
    try {
      await ElMessageBox.confirm(
        `确认恢复用户「${row.userName}」？`,
        '恢复用户',
        { confirmButtonText: '恢复', cancelButtonText: '取消' },
      )
    } catch {
      return
    }
    try {
      await restoreUsers([row.userId])
      ElMessage.success('用户已恢复')
      fetchUsers()
      fetchTabCounts()
    } catch {
      ElMessage.error('恢复失败')
    }
  } else if (command === 'permanentDelete') {
    try {
      await ElMessageBox.confirm(
        `彻底删除「${row.userName}」后数据将无法恢复，确认继续？`,
        '彻底删除',
        { confirmButtonText: '彻底删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
      )
    } catch {
      return
    }
    try {
      await permanentDeleteUsers([row.userId])
      ElMessage.success('用户已彻底删除')
      fetchUsers()
      fetchTabCounts()
    } catch {
      ElMessage.error('操作失败')
    }
  }
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
    // 统计接口失败不影响主流程
  }
}

// ── Mobile detection ──────────────────────────────────────────────────────────
const windowWidth = ref(window.innerWidth)
const isMobile = computed(() => windowWidth.value <= 768)
function onResize() { windowWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

onMounted(() => {
  fetchUsers()
  fetchTabCounts()
})
</script>

<template>
  <div class="users-page">
    <div class="main-card">
      <!-- 顶部：tab + 搜索 + 操作 -->
      <div class="card-header">
        <div class="view-tabs">
          <button class="view-tab" :class="{ 'view-tab--active': activeTab === 'all' }" @click="switchTab('all')">
            全部
            <span v-if="tabCounts.all > 0" class="tab-count">{{ tabCounts.all }}</span>
          </button>
          <button class="view-tab" :class="{ 'view-tab--active': activeTab === 'enabled' }" @click="switchTab('enabled')">
            启用
            <span v-if="tabCounts.enabled > 0" class="tab-count">{{ tabCounts.enabled }}</span>
          </button>
          <button class="view-tab" :class="{ 'view-tab--active': activeTab === 'disabled' }" @click="switchTab('disabled')">
            禁用
            <span v-if="tabCounts.disabled > 0" class="tab-count">{{ tabCounts.disabled }}</span>
          </button>
          <button class="view-tab" :class="{ 'view-tab--active': activeTab === 'deleted' }" @click="switchTab('deleted')">
            已删除
            <span v-if="tabCounts.deleted > 0" class="tab-count">{{ tabCounts.deleted }}</span>
          </button>
        </div>

        <div class="header-actions">
          <el-input
            v-model="filters.userName"
            placeholder="搜索用户名"
            clearable
            :prefix-icon="Search"
            style="width: 200px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button :icon="RefreshLeft" @click="handleReset" />
          <el-button v-if="activeTab !== 'deleted'" type="primary" :icon="Plus" @click="router.push('/admin/users/add')" />
        </div>
      </div>

      <!-- 批量操作栏 -->
      <Transition name="sel-bar">
        <div v-if="selectedRows.length > 0" class="selection-bar">
          <span class="sel-count">已选 <b>{{ selectedRows.length }}</b> 项</span>
          <div class="sel-actions">
            <template v-if="activeTab !== 'deleted'">
              <el-button size="small" @click="handleBatchStatus(1)">批量启用</el-button>
              <el-button size="small" @click="handleBatchStatus(0)">批量禁用</el-button>
              <el-button size="small" type="danger" plain @click="handleBatchDelete">批量删除</el-button>
            </template>
            <template v-else>
              <el-button size="small" type="primary" plain @click="handleBatchRestore">批量恢复</el-button>
              <el-button size="small" type="danger" plain @click="handleBatchPermanentDelete">彻底删除</el-button>
            </template>
          </div>
          <el-button size="small" text class="sel-cancel" @click="handleClearSelection">取消选择</el-button>
        </div>
      </Transition>

      <!-- 桌面端：表格 -->
      <div v-if="!isMobile" class="table-scroll-wrap">
        <el-table
          ref="tableRef"
          :data="pageData.content"
          :row-key="(row: UserListItem) => row.userId"
          v-loading="loading"
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="44" />

          <el-table-column label="用户">
            <template #default="{ row }">
              <div class="user-cell">
                <div class="user-avatar" :class="{ 'user-avatar--placeholder': !row.avatar }">
                  <img v-if="row.avatar" :src="row.avatar" :alt="row.userName" />
                  <span v-else>{{ row.userName?.[0]?.toUpperCase() ?? '?' }}</span>
                </div>
                <span class="user-name">{{ row.userName }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="邮箱" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="cell-muted">{{ row.email || '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="角色" align="center">
            <template #default="{ row }">
              <span class="role-badge" :class="row.userRole === 1 ? 'role-badge--admin' : 'role-badge--user'">
                {{ row.userRole === 1 ? '管理员' : '普通用户' }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="状态" align="center">
            <template #default="{ row }">
              <span class="status-badge" :class="row.status === 1 ? 'status-badge--enabled' : 'status-badge--disabled'">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="注册时间">
            <template #default="{ row }">
              <el-tooltip :content="row.createTime" placement="top">
                <span class="cell-muted">{{ relativeTime(row.createTime) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>

          <el-table-column label="" width="52" align="center" fixed="right">
            <template #default="{ row }">
              <el-dropdown trigger="hover" @command="(cmd: string) => handleCommand(cmd, row)">
                <button class="more-btn">
                  <el-icon><MoreFilled /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu v-if="activeTab !== 'deleted'">
                    <el-dropdown-item command="detail">用户信息</el-dropdown-item>
                    <el-tooltip
                      :content="row.userId === userInfo?.userId ? '不能操作当前登录账号' : ''"
                      :disabled="row.userId !== userInfo?.userId"
                      placement="left"
                    >
                      <el-dropdown-item command="toggleStatus" divided :disabled="row.userId === userInfo?.userId">
                        {{ row.status === 1 ? '禁用' : '启用' }}
                      </el-dropdown-item>
                    </el-tooltip>
                    <el-dropdown-item command="resetPassword">重置密码</el-dropdown-item>
                    <el-dropdown-item command="delete" divided style="color: var(--el-color-danger)">删除</el-dropdown-item>
                  </el-dropdown-menu>
                  <el-dropdown-menu v-else>
                    <el-dropdown-item command="detail">用户信息</el-dropdown-item>
                    <el-dropdown-item command="restore" divided>恢复</el-dropdown-item>
                    <el-dropdown-item command="permanentDelete" style="color: var(--el-color-danger)">彻底删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty :description="activeTab === 'deleted' ? '没有已删除的用户' : '暂无用户'" :image-size="80" />
          </template>
        </el-table>
      </div>

      <!-- 移动端：卡片列表 -->
      <div v-else v-loading="loading" class="mobile-list">
        <el-empty
          v-if="!loading && pageData.content.length === 0"
          :description="activeTab === 'deleted' ? '没有已删除的用户' : '暂无用户'"
          :image-size="80"
          style="padding: 40px 0"
        />
        <div
          v-for="row in pageData.content"
          :key="row.userId"
          class="mobile-card"
        >
          <div class="mc-main">
            <div class="user-avatar" :class="{ 'user-avatar--placeholder': !row.avatar }">
              <img v-if="row.avatar" :src="row.avatar" :alt="row.userName" />
              <span v-else>{{ row.userName?.[0]?.toUpperCase() ?? '?' }}</span>
            </div>
            <div class="mc-info">
              <div class="mc-name-row">
                <span class="user-name">{{ row.userName }}</span>
                <span class="role-badge" :class="row.userRole === 1 ? 'role-badge--admin' : 'role-badge--user'">
                  {{ row.userRole === 1 ? '管理员' : '普通用户' }}
                </span>
                <span class="status-badge" :class="row.status === 1 ? 'status-badge--enabled' : 'status-badge--disabled'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </span>
              </div>
              <span class="cell-muted mc-email">{{ row.email || '暂无邮箱' }}</span>
              <span class="cell-muted mc-time">注册于 {{ relativeTime(row.createTime) }}</span>
            </div>
          </div>
          <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
            <button class="more-btn">
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu v-if="activeTab !== 'deleted'">
                <el-dropdown-item command="detail">用户信息</el-dropdown-item>
                <el-dropdown-item command="toggleStatus" divided :disabled="row.userId === userInfo?.userId">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-dropdown-item>
                <el-dropdown-item command="resetPassword">重置密码</el-dropdown-item>
                <el-dropdown-item command="delete" divided style="color: var(--el-color-danger)">删除</el-dropdown-item>
              </el-dropdown-menu>
              <el-dropdown-menu v-else>
                <el-dropdown-item command="detail">用户信息</el-dropdown-item>
                <el-dropdown-item command="restore" divided>恢复</el-dropdown-item>
                <el-dropdown-item command="permanentDelete" style="color: var(--el-color-danger)">彻底删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pageData.totalElements"
          :page-sizes="[10, 20, 50]"
          :layout="isMobile ? 'prev, pager, next' : 'total, sizes, prev, pager, next'"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>

  <!-- 密码重置结果弹窗 -->
  <el-dialog v-model="passwordDialogVisible" width="min(360px, 92vw)" :show-close="false" align-center>
    <template #header><span /></template>
    <div class="pwd-dialog-inner">
      <div class="pwd-dialog-icon">✓</div>
      <p class="pwd-dialog-title">密码已重置</p>
      <p class="pwd-dialog-sub">新密码</p>
      <div class="pwd-dialog-value">{{ newPassword }}</div>
      <p class="pwd-dialog-copied">已自动复制到剪贴板</p>
    </div>
    <template #footer>
      <el-button type="primary" @click="passwordDialogVisible = false">完成</el-button>
    </template>
  </el-dialog>

  <!-- 用户信息弹窗 -->
  <el-dialog
    v-model="detailVisible"
    width="460px"
    :destroy-on-close="true"
    :show-close="true"
    class="user-detail-dialog"
  >
    <template #header><span /></template>
    <div v-if="detailLoading" class="dg-loading">
      <el-skeleton :rows="4" animated />
    </div>

    <template v-else-if="detailUser">
      <div class="dg-banner">
        <div class="dg-avatar" :class="{ 'dg-avatar--placeholder': !detailUser.avatar }">
          <img v-if="detailUser.avatar" :src="detailUser.avatar" :alt="detailUser.userName" />
          <span v-else>{{ detailUser.userName?.[0]?.toUpperCase() ?? '?' }}</span>
        </div>
        <div class="dg-banner-info">
          <span class="dg-name">{{ detailUser.userName }}</span>
          <span class="dg-position">{{ detailUser.position || '暂无职位' }}</span>
          <div class="dg-badges">
            <el-tag :type="detailUser.userRole === 1 ? 'primary' : 'info'" size="small" effect="light">
              {{ detailUser.userRole === 1 ? '管理员' : '普通用户' }}
            </el-tag>
            <el-tag :type="detailUser.status === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ detailUser.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="dg-grid">
        <div class="dg-cell">
          <span class="dg-cell-label">用户 ID</span>
          <span class="dg-cell-value"># {{ detailUser.userId }}</span>
        </div>
        <div class="dg-cell">
          <span class="dg-cell-label">邮箱</span>
          <span class="dg-cell-value">{{ detailUser.email || '—' }}</span>
        </div>
        <div class="dg-cell">
          <span class="dg-cell-label">公司</span>
          <span class="dg-cell-value">{{ detailUser.company || '—' }}</span>
        </div>
        <div class="dg-cell">
          <span class="dg-cell-label">注册时间</span>
          <span class="dg-cell-value">{{ detailUser.createTime }}</span>
        </div>
        <div class="dg-cell">
          <span class="dg-cell-label">最后更新</span>
          <span class="dg-cell-value">{{ detailUser.updateTime }}</span>
        </div>
      </div>

      <div class="dg-profile">
        <span class="dg-cell-label">个人简介</span>
        <p class="dg-profile-text">{{ detailUser.profile || '—' }}</p>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.users-page {
  display: flex;
  flex-direction: column;
}

/* Main card */
.main-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

/* Card header: tabs left, actions right */
.card-header {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 20px;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
}

/* Tabs */
.view-tabs {
  display: flex;
}

.view-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 4px;
  margin-right: 20px;
  height: 48px;
  font-size: 14px;
  color: #6b7280;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
  white-space: nowrap;
}

.view-tab:hover {
  color: #374151;
}

.view-tab--active {
  color: #1d4ed8;
  border-bottom-color: #1d4ed8;
  font-weight: 500;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 600;
  background: #e0e7ff;
  color: #3730a3;
  border-radius: 10px;
}

/* Selection bar */
.selection-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  background: #eff6ff;
  border-bottom: 1px solid #bfdbfe;
}

.sel-count {
  font-size: 13px;
  color: #1d4ed8;
  white-space: nowrap;
}

.sel-count b {
  font-weight: 700;
}

.sel-actions {
  display: flex;
  gap: 8px;
}

.sel-cancel {
  margin-left: auto;
  color: #6b7280;
}

.sel-bar-enter-active,
.sel-bar-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.sel-bar-enter-from,
.sel-bar-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* Table wrapper padding */
:deep(.el-table) {
  --el-table-border-color: #f3f4f6;
  --el-table-header-bg-color: #f9fafb;
  --el-table-header-text-color: #6b7280;
  --el-table-row-hover-bg-color: #f5f7ff;
  --el-table-tr-bg-color: #fff;
}

:deep(.el-table th) {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

/* Pagination */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 14px 20px;
  border-top: 1px solid #f3f4f6;
}

/* User cell */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.user-avatar--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4a8db7, #2d6a9f);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}


.cell-muted {
  font-size: 13px;
  color: #6b7280;
}

/* Role badge */
.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.role-badge--admin {
  background: #eff6ff;
  color: #1d4ed8;
}

.role-badge--user {
  background: #f3f4f6;
  color: #4b5563;
}

/* Status badge */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge--enabled {
  background: #f0fdf4;
  color: #16a34a;
}

.status-badge--disabled {
  background: #fef2f2;
  color: #dc2626;
}

/* More button */
.more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  outline: none;
  transition: background 0.15s, color 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.more-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.more-btn:focus,
.more-btn:active {
  outline: none;
}


/* Password dialog */
.pwd-dialog-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 0 4px;
  text-align: center;
}

.pwd-dialog-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #dcfce7;
  color: #16a34a;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.pwd-dialog-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.pwd-dialog-sub {
  font-size: 12px;
  color: #9ca3af;
  margin: 4px 0 0;
}

.pwd-dialog-value {
  font-family: ui-monospace, monospace;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #1d4ed8;
  background: #eff6ff;
  border-radius: 8px;
  padding: 10px 20px;
  margin: 2px 0;
}

.pwd-dialog-copied {
  font-size: 12px;
  color: #16a34a;
  margin: 0;
}

/* User Detail Dialog */
:global(.user-detail-dialog .el-dialog__body) {
  padding: 16px 20px 20px;
}

:global(.user-detail-dialog .el-dialog__header) {
  padding: 0;
}

.dg-banner {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px 24px 24px;
  background: linear-gradient(135deg, #eaf2fb 0%, #f0f4ff 100%);
  border-radius: 8px;
  margin-bottom: 20px;
}

.dg-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(74, 141, 183, 0.3);
}

.dg-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.dg-avatar--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4a8db7, #2d6a9f);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
}

.dg-banner-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.dg-loading {
  padding: 12px 4px;
}

.dg-name {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.dg-position {
  font-size: 13px;
  color: #6b7280;
}

.dg-badges {
  display: flex;
  gap: 8px;
}

.dg-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 0 4px 8px;
}

.dg-cell {
  background: #f9fafb;
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dg-profile {
  margin-bottom: 14px;
  padding: 14px 16px;
  background: #f9fafb;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dg-profile-text {
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
  margin: 0;
}

.dg-cell-label {
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.dg-cell-value {
  font-size: 13px;
  color: #1f2937;
  font-weight: 500;
  word-break: break-all;
}

/* ── Table scroll wrapper ───────────────────────────────────── */
.table-scroll-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

/* ── Mobile card list ───────────────────────────────────────── */
.mobile-list {
  min-height: 120px;
}

.mobile-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
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
  max-width: 200px;
}

.mc-time {
  font-size: 12px;
}

/* ── Mobile responsive ──────────────────────────────────────── */
@media (max-width: 768px) {
  /* Card header: stack tabs above actions */
  .card-header {
    flex-direction: column;
    align-items: stretch;
    padding: 0 12px;
    gap: 0;
  }

  .view-tabs {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    border-bottom: 1px solid #f0f0f0;
  }

  .view-tabs::-webkit-scrollbar {
    display: none;
  }

  .view-tab {
    flex-shrink: 0;
    margin-right: 12px;
    height: 42px;
    font-size: 13px;
  }

  .header-actions {
    padding: 8px 0;
    flex-wrap: wrap;
  }

  .header-actions .el-input {
    width: 100% !important;
    flex: 1 1 120px;
  }

  /* Selection bar */
  .selection-bar {
    flex-wrap: wrap;
    gap: 8px;
    padding: 8px 12px;
  }

  .sel-actions {
    flex-wrap: wrap;
  }

  .sel-cancel {
    margin-left: 0;
  }

  /* Pagination */
  .pagination-bar {
    padding: 12px;
    justify-content: center;
  }

  /* Detail dialog: full width on mobile */
  :global(.user-detail-dialog) {
    --el-dialog-width: 92vw !important;
  }

  .dg-grid {
    grid-template-columns: 1fr;
  }

  .dg-banner {
    padding: 20px 16px;
    gap: 14px;
  }
}
</style>
