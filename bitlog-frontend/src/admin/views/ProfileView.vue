<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Loading } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/useUserStore'
import {
  getUserProfile,
  updateUserInfo,
  updateAvatar,
  updatePassword,
  type UserProfile,
} from '@/api/user'
import { uploadFile } from '@/api/file'

const userStore = useUserStore()
const { userInfo } = storeToRefs(userStore)

const profile = ref<UserProfile | null>(null)
const pageLoading = ref(false)

// 头像上传
const fileInputRef = ref<HTMLInputElement>()
const avatarUploading = ref(false)
const avatarError = ref(false)

// 基本信息表单
const basicForm = reactive({
  nickname: '',
  position: '',
  company: '',
  profile: '',
})
const basicSaving = ref(false)

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const passwordSaving = ref(false)

onMounted(loadProfile)

async function loadProfile() {
  pageLoading.value = true
  try {
    profile.value = await getUserProfile()
    syncBasicForm()
  } catch {
    ElMessage.error('获取个人信息失败')
  } finally {
    pageLoading.value = false
  }
}

function syncBasicForm() {
  if (!profile.value) return
  basicForm.nickname = profile.value.nickname
  basicForm.position = profile.value.position || ''
  basicForm.company = profile.value.company || ''
  basicForm.profile = profile.value.profile || ''
}

// ── 头像 ──────────────────────────────────────────────────────────────────────

function triggerAvatarInput() {
  if (avatarUploading.value) return
  fileInputRef.value?.click()
}

async function handleFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  ;(e.target as HTMLInputElement).value = ''

  avatarUploading.value = true
  try {
    const { fileUrl } = await uploadFile(file, 'avatar')
    await updateAvatar(fileUrl)
    await userStore.fetchProfile()
    profile.value = userInfo.value
    avatarError.value = false
    ElMessage.success('头像已更新')
  } catch {
    ElMessage.error('头像上传失败，请重试')
  } finally {
    avatarUploading.value = false
  }
}

// ── 基本信息 ──────────────────────────────────────────────────────────────────

async function saveBasicInfo() {
  if (!basicForm.nickname.trim()) {
    ElMessage.warning('别名不能为空')
    return
  }
  if (basicForm.nickname.length > 64) {
    ElMessage.warning('别名最长 64 个字符')
    return
  }
  if (basicForm.position.length > 64) {
    ElMessage.warning('职位最长 64 个字符')
    return
  }
  if (basicForm.company.length > 64) {
    ElMessage.warning('公司最长 64 个字符')
    return
  }
  if (basicForm.profile.length > 500) {
    ElMessage.warning('个人简介最长 500 个字符')
    return
  }
  basicSaving.value = true
  try {
    await updateUserInfo({
      nickname: basicForm.nickname.trim(),
      position: basicForm.position.trim() || undefined,
      company: basicForm.company.trim() || undefined,
      profile: basicForm.profile.trim() || undefined,
    })
    await userStore.fetchProfile()
    profile.value = userInfo.value
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    basicSaving.value = false
  }
}

// ── 密码 ──────────────────────────────────────────────────────────────────────

async function savePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写所有密码字段')
    return
  }
  if (!/^[a-zA-Z0-9_@#%&!$*-]{8,20}$/.test(passwordForm.newPassword)) {
    ElMessage.warning('新密码为 8-20 位，可包含字母、数字及 _@#%&!$*- 符号')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  passwordSaving.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码已修改')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (err: any) {
    if (err?.code === 42001) {
      ElMessage.error('当前密码错误')
    } else {
      ElMessage.error('修改失败，请重试')
    }
  } finally {
    passwordSaving.value = false
  }
}

// ── 工具函数 ──────────────────────────────────────────────────────────────────

function roleLabel(role: number) {
  return role === 1 ? '管理员' : '普通用户'
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}
</script>

<template>
  <div class="profile-page" v-loading="pageLoading">

    <!-- 用户概览 -->
    <section class="card">
      <div class="card-body overview">
        <!-- 头像 -->
        <div
          class="avatar-zone"
          :class="{ uploading: avatarUploading }"
          @click="triggerAvatarInput"
        >
          <img v-if="profile?.avatar && !avatarError" :src="profile.avatar" class="avatar-img" alt="avatar" @error="avatarError = true" />
          <div v-else class="avatar-fallback">{{ profile?.nickname?.[0]?.toUpperCase() ?? '?' }}</div>
          <div class="avatar-overlay">
            <el-icon v-if="avatarUploading" class="is-loading"><Loading /></el-icon>
            <el-icon v-else><Upload /></el-icon>
            <span v-if="!avatarUploading">更换头像</span>
          </div>
        </div>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleFileChange"
        />

        <div class="overview-info">
          <div class="overview-name">{{ profile?.nickname ?? '—' }}</div>
          <div class="overview-meta">
            <el-tag
              :type="profile?.userRole === 1 ? 'danger' : 'info'"
              size="small"
              effect="light"
            >
              {{ profile ? roleLabel(profile.userRole) : '' }}
            </el-tag>
            <span v-if="profile?.createTime" class="overview-date">
              注册于 {{ formatDate(profile.createTime) }}
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- 基本信息 -->
    <section class="card">
      <div class="card-head">
        <span class="card-title">基本信息</span>
      </div>
      <div class="card-body">
        <div class="field">
          <label class="field-label">登录账号</label>
          <el-input :model-value="profile?.username ?? ''" readonly />
        </div>
        <div class="field">
          <label class="field-label">别名</label>
          <el-input v-model="basicForm.nickname" placeholder="请输入别名" :maxlength="64" />
        </div>
        <div class="field-row">
          <div class="field">
            <label class="field-label">职位</label>
            <el-input v-model="basicForm.position" placeholder="如：前端工程师" :maxlength="64" />
          </div>
          <div class="field">
            <label class="field-label">公司</label>
            <el-input v-model="basicForm.company" placeholder="如：Acme Inc." :maxlength="64" />
          </div>
        </div>
        <div class="field">
          <label class="field-label">个人简介</label>
          <el-input
            v-model="basicForm.profile"
            type="textarea"
            :rows="3"
            placeholder="介绍一下自己…"
            :maxlength="500"
            show-word-limit
          />
        </div>
        <div class="card-footer">
          <el-button type="primary" :loading="basicSaving" @click="saveBasicInfo">保存</el-button>
        </div>
      </div>
    </section>

    <!-- 邮箱 -->
    <section class="card">
      <div class="card-head">
        <span class="card-title">邮箱</span>
      </div>
      <div class="card-body">
        <!-- TODO: 修改邮箱功能待实现 -->
        <div class="field-row field-row--align-end">
          <div class="field">
            <label class="field-label">当前邮箱</label>
            <el-input :model-value="profile?.email ?? ''" readonly />
          </div>
          <el-tooltip content="功能即将开放" placement="top">
            <span>
              <el-button disabled>修改</el-button>
            </span>
          </el-tooltip>
        </div>
      </div>
    </section>

    <!-- 修改密码 -->
    <section class="card">
      <div class="card-head">
        <span class="card-title">修改密码</span>
      </div>
      <div class="card-body">
        <div class="field">
          <label class="field-label">当前密码</label>
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label class="field-label">新密码</label>
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password
            />
          </div>
          <div class="field">
            <label class="field-label">确认新密码</label>
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="再次输入新密码"
              show-password
            />
          </div>
        </div>
        <div class="card-footer">
          <el-button type="primary" :loading="passwordSaving" @click="savePassword">
            确认修改
          </el-button>
        </div>
      </div>
    </section>

  </div>
</template>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 680px;
  margin: 0 auto;
}

/* Card */
.card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

.card-head {
  display: flex;
  align-items: center;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #f3f4f6;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.card-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

/* Overview */
.overview {
  flex-direction: row;
  align-items: center;
  gap: 20px;
}

.overview-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-name {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.overview-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.overview-date {
  font-size: 13px;
  color: #9ca3af;
}

/* Avatar */
.avatar-zone {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  width: 100%;
  height: 100%;
  background: #e0e7ff;
  color: #4f46e5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 600;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.48);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: #fff;
  font-size: 11px;
  opacity: 0;
  transition: opacity 0.18s;
}

.avatar-zone:hover .avatar-overlay,
.avatar-zone.uploading .avatar-overlay {
  opacity: 1;
}

/* Fields */
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.field-row {
  display: flex;
  gap: 12px;
}

.field-row--align-end {
  align-items: flex-end;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

@media (max-width: 768px) {
  .profile-page {
    max-width: 100%;
  }

  .field-row {
    flex-direction: column;
  }

  .field-row--align-end {
    align-items: stretch;
  }

  .card-body {
    padding: 16px;
  }
}
</style>
