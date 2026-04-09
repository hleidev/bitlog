<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, MoreFilled } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/useUserStore'
import { getUsers, getUserById, updateUserStatus, type UserListItem, type UserDetail, type PageVO } from '@/api/admin/user'

const { userInfo } = storeToRefs(useUserStore())

const loading = ref(false)

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

const filters = reactive({
  userName: '',
  status: undefined as number | undefined,
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
    const query = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      userName: filters.userName || undefined,
      status: filters.status,
    }
    pageData.value = await getUsers(query)
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
  filters.status = undefined
  pagination.pageNum = 1
  fetchUsers()
}

function handlePageChange(page: number) {
  pagination.pageNum = page
  fetchUsers()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchUsers()
}

async function handleCommand(command: string, row: UserListItem) {
  if (command === 'detail') {
    openDetail(row)
  } else if (command === 'toggleStatus') {
    if (row.userId === userInfo.value?.userId) return
    const newStatus = row.status === 1 ? 0 : 1
    try {
      await updateUserStatus(row.userId, newStatus as 0 | 1)
      ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
      fetchUsers()
    } catch {
      ElMessage.error('操作失败')
    }
  } else if (command === 'resetPassword') {
    // TODO: 重置密码
  } else if (command === 'delete') {
    // TODO: 删除用户
  }
}

onMounted(fetchUsers)
</script>

<template>
  <div class="users-page">
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <el-input
          v-model="filters.userName"
          placeholder="搜索用户名"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="filters.status"
          placeholder="全部状态"
          clearable
          style="width: 140px"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="pageData.content"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column label="用户" min-width="160">
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

        <el-table-column label="邮箱" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-muted">{{ row.email || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="角色" min-width="160" align="center">
          <template #default="{ row }">
            <el-tag :type="row.userRole === 1 ? 'primary' : 'info'" size="small">
              {{ row.userRole === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" min-width="160" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="注册时间" prop="createTime" min-width="160" />

        <el-table-column label="" width="64" align="center" fixed="right">
          <template #default="{ row }">
            <el-dropdown trigger="hover" @command="(cmd: string) => handleCommand(cmd, row)">
              <button class="more-btn">
                <el-icon><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="detail">用户信息</el-dropdown-item>
                  <el-tooltip
                    :content="row.userId === userInfo?.userId ? '不能禁用当前登录账号' : ''"
                    :disabled="row.userId !== userInfo?.userId"
                    placement="left"
                  >
                    <el-dropdown-item
                      command="toggleStatus"
                      divided
                      :disabled="row.userId === userInfo?.userId"
                    >
                      {{ row.status === 1 ? '禁用' : '启用' }}
                    </el-dropdown-item>
                  </el-tooltip>
                  <el-dropdown-item command="resetPassword">重置密码</el-dropdown-item>
                  <el-dropdown-item command="delete" divided style="color: var(--el-color-danger)">
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无数据" :image-size="80" />
        </template>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pageData.totalElements"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>

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
      <!-- 顶部 Banner -->
      <div class="dg-banner">
        <div class="dg-avatar" :class="{ 'dg-avatar--placeholder': !detailUser.avatar }">
          <img v-if="detailUser.avatar" :src="detailUser.avatar" :alt="detailUser.userName" />
          <span v-else>{{ detailUser.userName?.[0]?.toUpperCase() ?? '?' }}</span>
        </div>
        <div class="dg-banner-info">
          <span class="dg-name">{{ detailUser.userName }}</span>
          <span class="dg-position">{{ detailUser.position || '暂无职位' }}</span>
          <div class="dg-badges">
            <el-tag :type="detailUser.userRole === 'ADMIN' ? 'primary' : 'info'" size="small" effect="light">
              {{ detailUser.userRole === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
            <el-tag :type="detailUser.status === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ detailUser.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 信息网格 -->
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

      <!-- 简介（独占一行） -->
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
  gap: 24px;
}

.filter-card,
.table-card {
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 36px;
  height: 36px;
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
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  outline: none;
  -webkit-tap-highlight-color: transparent;
}

.more-btn:focus,
.more-btn:active {
  background: transparent;
  outline: none;
}

.cell-muted {
  font-size: 13px;
  color: #6b7280;
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
</style>
