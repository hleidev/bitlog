<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/useUserStore'
import { useToast } from '@/admin/composables/useToast'
import {
  getUserProfile,
  updateUserInfo,
  updateAvatar,
  updatePassword,
  type UserProfile,
} from '@/api/user'
import { uploadFile } from '@/api/file'
import { validateUsername, validatePassword } from '@/utils/authValidation'
import PasswordInput from '@/components/common/PasswordInput.vue'

type TabKey = 'profile' | 'security'

const userStore = useUserStore()
const { userInfo } = storeToRefs(userStore)
const toast = useToast()

const profile = ref<UserProfile | null>(null)
const pageLoading = ref(false)
const activeTab = ref<TabKey>('profile')

// 头像
const fileInputRef = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)
const avatarError = ref(false)

// 基本信息
const basicForm = reactive({ username: '', position: '', company: '', profile: '' })
const basicSaving = ref(false)

// 密码
const passwordEditing = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordSaving = ref(false)

onMounted(loadProfile)

async function loadProfile() {
  pageLoading.value = true
  try {
    profile.value = await getUserProfile()
    syncBasicForm()
  } catch {
    toast.error('获取个人信息失败')
  } finally {
    pageLoading.value = false
  }
}

function syncBasicForm() {
  if (!profile.value) return
  basicForm.username = profile.value.username
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
    toast.success('头像已更新')
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '头像上传失败，请重试')
    profile.value = userInfo.value
  } finally {
    avatarUploading.value = false
  }
}

// ── 基本信息 ──────────────────────────────────────────────────────────────────

async function saveBasicInfo() {
  const usernameError = validateUsername(basicForm.username)
  if (usernameError) {
    toast.warning(usernameError)
    return
  }
  if (basicForm.position.length > 64) {
    toast.warning('职位最长 64 个字符')
    return
  }
  if (basicForm.company.length > 64) {
    toast.warning('公司最长 64 个字符')
    return
  }
  if (basicForm.profile.length > 500) {
    toast.warning('个人简介最长 500 个字符')
    return
  }
  basicSaving.value = true
  try {
    await updateUserInfo({
      username: basicForm.username.trim(),
      position: basicForm.position.trim() || undefined,
      company: basicForm.company.trim() || undefined,
      profile: basicForm.profile.trim() || undefined,
    })
    await userStore.fetchProfile()
    profile.value = userInfo.value
    toast.success('保存成功')
  } catch (err: unknown) {
    // 本接口的 42002 只可能来自用户名撞人；40011 是检查与写入之间并发撞唯一索引，同因
    const code = (err as { code?: number })?.code
    const taken = code === 42002 || code === 40011
    toast.error(taken ? '该用户名已被占用，换一个试试' : '保存失败，请重试')
  } finally {
    basicSaving.value = false
  }
}

// ── 密码 ──────────────────────────────────────────────────────────────────────

function togglePasswordEdit() {
  passwordEditing.value = !passwordEditing.value
  if (!passwordEditing.value) resetPasswordForm()
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

async function savePassword() {
  if (!passwordForm.oldPassword) {
    toast.warning('请输入当前密码')
    return
  }
  const newPasswordError = validatePassword(passwordForm.newPassword, '新密码')
  if (newPasswordError) {
    toast.warning(newPasswordError)
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    toast.warning('两次输入的新密码不一致')
    return
  }
  passwordSaving.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    toast.success('密码已修改')
    resetPasswordForm()
    passwordEditing.value = false
  } catch (err: unknown) {
    // 后端校验旧密码失败抛 ACCOUNT_OR_PASSWORD_ERROR(41002)，不是 USER_NOT_EXISTS(42001)
    const code = (err as { code?: number })?.code
    toast.error(code === 41002 ? '当前密码错误' : '修改失败，请重试')
  } finally {
    passwordSaving.value = false
  }
}

function roleLabel(role: number) {
  return role === 1 ? '管理员' : '普通用户'
}
</script>

<template>
  <div class="profile-page">
    <div v-if="pageLoading" class="page-loading">
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

    <div v-else class="main-card">
      <div class="card-header">
        <div class="view-tabs">
          <button
            class="view-tab"
            :class="{ 'view-tab--active': activeTab === 'profile' }"
            @click="activeTab = 'profile'"
          >
            资料
          </button>
          <button
            class="view-tab"
            :class="{ 'view-tab--active': activeTab === 'security' }"
            @click="activeTab = 'security'"
          >
            账号安全
          </button>
        </div>
      </div>

      <!-- ── 资料 ── -->
      <div v-if="activeTab === 'profile'" class="tab-panel">
        <div class="form-col">
          <!-- 头像 -->
          <div class="avatar-row">
            <div
              class="avatar-zone"
              :class="{ uploading: avatarUploading }"
              @click="triggerAvatarInput"
            >
              <img
                v-if="profile?.avatar && !avatarError"
                :src="profile.avatar"
                class="avatar-img"
                alt="当前头像"
                @error="avatarError = true"
              />
              <div v-else class="avatar-fallback">
                {{ profile?.username?.[0]?.toUpperCase() ?? '?' }}
              </div>
              <div class="avatar-overlay">
                <svg v-if="avatarUploading" class="overlay-spinner" viewBox="0 0 24 24" fill="none">
                  <circle
                    cx="12"
                    cy="12"
                    r="9"
                    stroke="currentColor"
                    stroke-width="2.5"
                    stroke-dasharray="40"
                    stroke-dashoffset="15"
                  />
                </svg>
                <template v-else>
                  <svg viewBox="0 0 24 24" fill="currentColor" class="overlay-icon">
                    <path d="M9 16h6v-6h4l-7-7-7 7h4v6zm-4 2h14v2H5v-2z" />
                  </svg>
                  <span>更换</span>
                </template>
              </div>
            </div>
            <input
              ref="fileInputRef"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              style="display: none"
              @change="handleFileChange"
            />
            <div class="avatar-identity">
              <span class="identity-name">{{ profile?.username ?? '—' }}</span>
              <span
                class="role-badge"
                :class="profile?.userRole === 1 ? 'role-badge--admin' : 'role-badge--user'"
              >
                {{ profile ? roleLabel(profile.userRole) : '' }}
              </span>
            </div>
          </div>

          <div class="field">
            <label class="field-label">用户名</label>
            <input
              v-model="basicForm.username"
              class="field-input"
              placeholder="请输入用户名"
              maxlength="16"
            />
            <p class="field-help">2~16 位，可含汉字、字母、数字、下划线和连字符</p>
          </div>

          <div class="field-row">
            <div class="field">
              <label class="field-label">职位</label>
              <input
                v-model="basicForm.position"
                class="field-input"
                placeholder="如：前端工程师"
                maxlength="64"
              />
            </div>
            <div class="field">
              <label class="field-label">公司</label>
              <input
                v-model="basicForm.company"
                class="field-input"
                placeholder="如：Acme Inc."
                maxlength="64"
              />
            </div>
          </div>

          <div class="field">
            <label class="field-label">
              个人简介
              <span class="field-counter">{{ basicForm.profile.length }} / 500</span>
            </label>
            <textarea
              v-model="basicForm.profile"
              class="field-textarea"
              rows="3"
              placeholder="介绍一下自己…"
              maxlength="500"
            />
          </div>

          <div class="form-actions">
            <button class="primary-btn" :disabled="basicSaving" @click="saveBasicInfo">
              <span v-if="basicSaving" class="btn-spinner" />
              保存
            </button>
          </div>
        </div>
      </div>

      <!-- ── 账号安全 ── -->
      <div v-else class="tab-panel">
        <div class="form-col">
          <div class="setting-item">
            <div class="setting-row">
              <div class="setting-main">
                <div class="setting-title">邮箱</div>
                <div class="setting-value">{{ profile?.email ?? '—' }}</div>
              </div>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-row">
              <div class="setting-main">
                <div class="setting-title">密码</div>
              </div>
              <button class="ghost-btn" @click="togglePasswordEdit">
                {{ passwordEditing ? '取消' : '修改' }}
              </button>
            </div>

            <div v-if="passwordEditing" class="setting-expand">
              <div class="field">
                <label class="field-label">当前密码</label>
                <PasswordInput
                  v-model="passwordForm.oldPassword"
                  input-class="field-input field-input--pwd"
                  placeholder="请输入当前密码"
                  maxlength="20"
                />
              </div>
              <div class="field-row">
                <div class="field">
                  <label class="field-label">新密码</label>
                  <PasswordInput
                    v-model="passwordForm.newPassword"
                    input-class="field-input field-input--pwd"
                    placeholder="请输入新密码"
                    maxlength="20"
                  />
                </div>
                <div class="field">
                  <label class="field-label">确认新密码</label>
                  <PasswordInput
                    v-model="passwordForm.confirmPassword"
                    input-class="field-input field-input--pwd"
                    placeholder="再次输入新密码"
                    maxlength="20"
                  />
                </div>
              </div>
              <p class="field-help">8~20 位，可含字母、数字及 _@#%&!$*- 符号</p>
              <div class="form-actions">
                <button class="primary-btn" :disabled="passwordSaving" @click="savePassword">
                  <span v-if="passwordSaving" class="btn-spinner" />
                  确认修改
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 设置页内容天然少于列表页，限宽后居中让左右留白对称——靠左会把内容甩到宽屏一角。
   880 是表单可读行长的上限附近，再宽输入框就长得难扫视了 */
.profile-page {
  max-width: 880px;
  margin: 0 auto;
  --pwd-toggle-color: var(--admin-sidebar-text-muted);
  --pwd-toggle-color-hover: var(--admin-sidebar-text);
}

/* ── Loading ── */

.page-loading {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.spinner {
  width: 28px;
  height: 28px;
  color: var(--admin-sidebar-text-muted);
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── Card shell ── */
/* .main-card / .view-tabs / .view-tab 见 admin/styles/variables.css */

.card-header {
  display: flex;
  align-items: stretch;
  border-bottom: 1px solid var(--admin-sidebar-border);
  padding: 0 16px 0 20px;
}

.tab-panel {
  padding: 24px 20px;
}

.form-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ── Avatar ── */

.avatar-row {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar-zone {
  position: relative;
  width: 68px;
  height: 68px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  border: 2px solid var(--admin-sidebar-border);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  width: 100%;
  height: 100%;
  background: var(--admin-accent-bg-strong);
  color: var(--admin-accent-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 600;
  font-family: var(--font-serif, 'Lora', serif);
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  /* 遮罩盖在头像图片/占位底上，底下内容不可控，故固定 0.55 且不随主题：
     --admin-overlay 浅色下只有 0.3，白字压上去仅 2.44，远不达 AA。 */
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  /* 底恒为深色，文字不能跟主题翻转（on-accent 暗色下是近黑） */
  color: var(--color-text-on-dark);
  font-size: 10px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  opacity: 0;
  transition: opacity 0.18s;
}

.avatar-zone:hover .avatar-overlay,
.avatar-zone.uploading .avatar-overlay {
  opacity: 1;
}

.overlay-icon {
  width: 16px;
  height: 16px;
}
.overlay-spinner {
  width: 18px;
  height: 18px;
  animation: spin 0.9s linear infinite;
}

.avatar-identity {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.identity-name {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 17px;
  font-weight: 600;
  color: var(--admin-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-badge {
  font-size: 11px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
  flex-shrink: 0;
}

.role-badge--admin {
  background: var(--admin-accent-bg-strong);
  color: var(--admin-accent-dark);
}
.role-badge--user {
  background: rgba(var(--admin-muted-rgb), 0.12);
  color: var(--admin-sidebar-text);
}

/* ── Fields ── */

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

/* space-between 只在存在计数器时生效；说明文字一律走 .field-help 排在控件下方 */
.field-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  color: var(--admin-text-primary);
}

.field-counter {
  font-size: 11px;
  font-weight: 400;
  color: var(--admin-sidebar-text-muted);
}

.field-help {
  margin: 0;
  font-size: 11.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  line-height: 1.5;
}

.field-row {
  display: flex;
  gap: 12px;
}

/* :deep 并列是因为密码框的 input 位于 PasswordInput 内部，scoped 选择器匹配不到 */
.field-input,
:deep(.field-input) {
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  background: var(--admin-surface-input);
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-text-primary);
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
  width: 100%;
}

.field-input:focus,
:deep(.field-input:focus) {
  border-color: var(--admin-accent);
}
.field-input::placeholder,
:deep(.field-input::placeholder) {
  color: var(--admin-sidebar-text-muted);
}
:deep(.field-input--pwd) {
  padding-right: 36px;
}

.field-textarea {
  padding: 8px 10px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  background: var(--admin-surface-input);
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-text-primary);
  outline: none;
  transition: border-color 0.15s;
  resize: vertical;
  box-sizing: border-box;
  width: 100%;
  min-height: 80px;
}

.field-textarea:focus {
  border-color: var(--admin-accent);
}
.field-textarea::placeholder {
  color: var(--admin-sidebar-text-muted);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

/* ── Security list ── */

.setting-item {
  border-bottom: 1px solid var(--admin-sidebar-border);
  padding-bottom: 18px;
}

.setting-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.setting-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.setting-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.setting-title {
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  color: var(--admin-text-primary);
}

.setting-value {
  font-size: 13.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  word-break: break-all;
}

.setting-expand {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
  padding: 18px 16px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  background: var(--admin-sidebar-hover);
}

/* ── Buttons ── */

.primary-btn {
  height: 34px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--admin-accent);
  color: var(--admin-text-on-accent);
  border: none;
  border-radius: 4px;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.primary-btn:hover:not(:disabled) {
  background: var(--admin-accent-dark);
}
.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ghost-btn {
  height: 32px;
  padding: 0 14px;
  flex-shrink: 0;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  background: transparent;
  color: var(--admin-sidebar-text);
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  cursor: pointer;
  transition: background 0.15s;
}

.ghost-btn:hover:not(:disabled) {
  background: var(--admin-sidebar-hover);
}
.ghost-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.btn-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: var(--admin-text-on-accent);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

/* ── Responsive ── */

@media (max-width: 768px) {
  .card-header {
    padding: 0 12px;
  }
  .tab-panel {
    padding: 16px 12px;
  }
  .field-row {
    flex-direction: column;
  }
  .setting-expand {
    padding: 14px 12px;
  }
}
</style>
