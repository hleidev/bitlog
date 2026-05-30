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

const userStore = useUserStore()
const { userInfo } = storeToRefs(userStore)
const toast = useToast()

const profile     = ref<UserProfile | null>(null)
const pageLoading = ref(false)

// 头像
const fileInputRef    = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)
const avatarError     = ref(false)

// 密码显示切换
const showOldPwd     = ref(false)
const showNewPwd     = ref(false)
const showConfirmPwd = ref(false)

// 基本信息
const basicForm   = reactive({ nickname: '', position: '', company: '', profile: '' })
const basicSaving = ref(false)

// 密码
const passwordForm   = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
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
  basicForm.nickname = profile.value.nickname
  basicForm.position = profile.value.position || ''
  basicForm.company  = profile.value.company  || ''
  basicForm.profile  = profile.value.profile  || ''
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
  if (!basicForm.nickname.trim())      { toast.warning('别名不能为空'); return }
  if (basicForm.nickname.length > 64)  { toast.warning('别名最长 64 个字符'); return }
  if (basicForm.position.length > 64)  { toast.warning('职位最长 64 个字符'); return }
  if (basicForm.company.length  > 64)  { toast.warning('公司最长 64 个字符'); return }
  if (basicForm.profile.length  > 500) { toast.warning('个人简介最长 500 个字符'); return }
  basicSaving.value = true
  try {
    await updateUserInfo({
      nickname: basicForm.nickname.trim(),
      position: basicForm.position.trim() || undefined,
      company:  basicForm.company.trim()  || undefined,
      profile:  basicForm.profile.trim()  || undefined,
    })
    await userStore.fetchProfile()
    profile.value = userInfo.value
    toast.success('保存成功')
  } catch {
    toast.error('保存失败，请重试')
  } finally {
    basicSaving.value = false
  }
}

// ── 密码 ──────────────────────────────────────────────────────────────────────

async function savePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    toast.warning('请填写所有密码字段'); return
  }
  if (!/^[a-zA-Z0-9_@#%&!$*-]{8,20}$/.test(passwordForm.newPassword)) {
    toast.warning('新密码为 8-20 位，可包含字母、数字及 _@#%&!$*- 符号'); return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    toast.warning('两次输入的新密码不一致'); return
  }
  passwordSaving.value = true
  try {
    await updatePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    toast.success('密码已修改')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (err: unknown) {
    const code = (err as { code?: number })?.code
    toast.error(code === 42001 ? '当前密码错误' : '修改失败，请重试')
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
        <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" stroke-dasharray="40" stroke-dashoffset="15" />
      </svg>
    </div>

    <div v-else class="profile-forms">

        <!-- 用户概览 -->
        <section class="form-card">
          <div class="card-body overview">
            <div class="avatar-zone" :class="{ uploading: avatarUploading }" @click="triggerAvatarInput">
              <img v-if="profile?.avatar && !avatarError" :src="profile.avatar" class="avatar-img" alt="avatar" @error="avatarError = true" />
              <div v-else class="avatar-fallback">{{ profile?.nickname?.[0]?.toUpperCase() ?? '?' }}</div>
              <div class="avatar-overlay">
                <svg v-if="avatarUploading" class="overlay-spinner" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2.5" stroke-dasharray="40" stroke-dashoffset="15" />
                </svg>
                <template v-else>
                  <svg viewBox="0 0 24 24" fill="currentColor" class="overlay-icon"><path d="M9 16h6v-6h4l-7-7-7 7h4v6zm-4 2h14v2H5v-2z" /></svg>
                  <span>更换头像</span>
                </template>
              </div>
            </div>
            <input ref="fileInputRef" type="file" accept="image/jpeg,image/png,image/webp" style="display:none" @change="handleFileChange" />
            <div class="overview-info">
              <div class="overview-name">{{ profile?.nickname ?? '—' }}</div>
              <div class="overview-username">@{{ profile?.username ?? '—' }}</div>
              <div class="overview-meta">
                <span class="role-badge" :class="profile?.userRole === 1 ? 'role-badge--admin' : 'role-badge--user'">
                  {{ profile ? roleLabel(profile.userRole) : '' }}
                </span>
              </div>
            </div>
          </div>
        </section>

        <!-- 基本信息 -->
        <section class="form-card">
          <div class="card-head">
            <span class="section-label">基本信息</span>
            <div class="section-rule" />
          </div>
          <div class="card-body">
            <div class="field">
              <label class="field-label">
                用户名
                <span class="field-hint">登录账号，不可修改</span>
              </label>
              <input class="field-input field-input--readonly" :value="profile?.username ?? ''" readonly />
            </div>
            <div class="field">
              <label class="field-label">昵称</label>
              <input v-model="basicForm.nickname" class="field-input" placeholder="请输入昵称" maxlength="64" />
            </div>
            <div class="field-row">
              <div class="field">
                <label class="field-label">职位</label>
                <input v-model="basicForm.position" class="field-input" placeholder="如：前端工程师" maxlength="64" />
              </div>
              <div class="field">
                <label class="field-label">公司</label>
                <input v-model="basicForm.company" class="field-input" placeholder="如：Acme Inc." maxlength="64" />
              </div>
            </div>
            <div class="field">
              <label class="field-label">
                个人简介
                <span class="field-hint">{{ basicForm.profile.length }} / 500</span>
              </label>
              <textarea v-model="basicForm.profile" class="field-textarea" rows="3" placeholder="介绍一下自己…" maxlength="500" />
            </div>
            <div class="card-footer">
              <button class="primary-btn" :disabled="basicSaving" @click="saveBasicInfo">
                <span v-if="basicSaving" class="btn-spinner" />
                保存
              </button>
            </div>
          </div>
        </section>

        <!-- 邮箱 -->
        <section class="form-card">
          <div class="card-head">
            <span class="section-label">邮箱</span>
            <div class="section-rule" />
          </div>
          <div class="card-body">
            <div class="field-row field-row--align-end">
              <div class="field">
                <label class="field-label">当前邮箱</label>
                <input class="field-input field-input--readonly" :value="profile?.email ?? ''" readonly />
              </div>
              <button class="ghost-btn" disabled title="功能即将开放">修改</button>
            </div>
          </div>
        </section>

        <!-- 修改密码 -->
        <section class="form-card">
          <div class="card-head">
            <span class="section-label">修改密码</span>
            <div class="section-rule" />
          </div>
          <div class="card-body">
            <div class="field">
              <label class="field-label">当前密码</label>
              <div class="input-wrap">
                <input v-model="passwordForm.oldPassword" class="field-input field-input--pwd" :type="showOldPwd ? 'text' : 'password'" placeholder="请输入当前密码" maxlength="20" />
                <button type="button" class="pwd-toggle" @click="showOldPwd = !showOldPwd">
                  <svg v-if="showOldPwd" viewBox="0 0 24 24" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" /></svg>
                  <svg v-else viewBox="0 0 24 24" fill="currentColor"><path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.804 11.804 0 0 0 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78 3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z" /></svg>
                </button>
              </div>
            </div>
            <div class="field-row">
              <div class="field">
                <label class="field-label">新密码</label>
                <div class="input-wrap">
                  <input v-model="passwordForm.newPassword" class="field-input field-input--pwd" :type="showNewPwd ? 'text' : 'password'" placeholder="请输入新密码" maxlength="20" />
                  <button type="button" class="pwd-toggle" @click="showNewPwd = !showNewPwd">
                    <svg v-if="showNewPwd" viewBox="0 0 24 24" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" /></svg>
                    <svg v-else viewBox="0 0 24 24" fill="currentColor"><path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.804 11.804 0 0 0 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78 3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z" /></svg>
                  </button>
                </div>
              </div>
              <div class="field">
                <label class="field-label">确认新密码</label>
                <div class="input-wrap">
                  <input v-model="passwordForm.confirmPassword" class="field-input field-input--pwd" :type="showConfirmPwd ? 'text' : 'password'" placeholder="再次输入新密码" maxlength="20" />
                  <button type="button" class="pwd-toggle" @click="showConfirmPwd = !showConfirmPwd">
                    <svg v-if="showConfirmPwd" viewBox="0 0 24 24" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" /></svg>
                    <svg v-else viewBox="0 0 24 24" fill="currentColor"><path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.804 11.804 0 0 0 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78 3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z" /></svg>
                  </button>
                </div>
              </div>
            </div>
            <div class="card-footer">
              <button class="primary-btn" :disabled="passwordSaving" @click="savePassword">
                <span v-if="passwordSaving" class="btn-spinner" />
                确认修改
              </button>
            </div>
          </div>
        </section>

    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 600px;
  margin: 0 auto;
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
  color: var(--admin-sidebar-text-muted, #b0a89e);
  animation: spin 0.9s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ── Avatar ── */

.avatar-zone {
  position: relative;
  width: 68px;
  height: 68px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  border: 2px solid var(--admin-sidebar-border, #e8e4de);
}

.avatar-img { width: 100%; height: 100%; object-fit: cover; }

.avatar-fallback {
  width: 100%;
  height: 100%;
  background: rgba(184, 92, 56, 0.1);
  color: var(--admin-accent, #b85c38);
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
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: #fff;
  font-size: 10px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  opacity: 0;
  transition: opacity 0.18s;
}

.avatar-zone:hover .avatar-overlay,
.avatar-zone.uploading .avatar-overlay { opacity: 1; }

.overlay-icon { width: 16px; height: 16px; }
.overlay-spinner { width: 18px; height: 18px; animation: spin 0.9s linear infinite; }

/* ── Overview card ── */

.overview {
  flex-direction: row;
  align-items: center;
  gap: 18px;
}

.overview-info { display: flex; flex-direction: column; gap: 8px; min-width: 0; }

.overview-name {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 17px;
  font-weight: 600;
  color: #1a1610;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.overview-username {
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted, #b0a89e);
  margin-top: -4px;
}

.overview-meta { display: flex; align-items: center; gap: 10px; }

.role-badge {
  font-size: 11px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
}

.role-badge--admin { background: rgba(184, 92, 56, 0.1); color: var(--admin-accent, #b85c38); }
.role-badge--user  { background: rgba(176, 168, 158, 0.12); color: var(--admin-sidebar-text, #5a5248); }

/* ── Forms ── */

.profile-forms {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-card {
  background: var(--admin-header-bg, #faf9f7);
  border: 1px solid var(--admin-sidebar-border, #e8e4de);
  border-radius: 4px;
  overflow: hidden;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--admin-sidebar-border, #e8e4de);
}

.section-label {
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: 13.5px;
  font-weight: 600;
  color: #1a1610;
  white-space: nowrap;
}

.section-rule {
  flex: 1;
  height: 1px;
  background: var(--admin-sidebar-border, #e8e4de);
}

.card-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

/* ── Fields ── */

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.field-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  color: #1a1610;
}

.field-hint { font-size: 11px; font-weight: 400; color: var(--admin-sidebar-text-muted, #b0a89e); }

.field-row { display: flex; gap: 12px; }
.field-row--align-end { align-items: flex-end; }

.field-input {
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--admin-sidebar-border, #e8e4de);
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: #1a1610;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
  width: 100%;
}

.field-input:focus { border-color: var(--admin-accent, #b85c38); }
.field-input::placeholder { color: var(--admin-sidebar-text-muted, #b0a89e); }
.field-input--readonly { background: var(--admin-sidebar-hover, #ece9e4); color: var(--admin-sidebar-text, #5a5248); cursor: default; }
.field-input--pwd { padding-right: 36px; }

.field-textarea {
  padding: 8px 10px;
  border: 1px solid var(--admin-sidebar-border, #e8e4de);
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: #1a1610;
  outline: none;
  transition: border-color 0.15s;
  resize: vertical;
  box-sizing: border-box;
  width: 100%;
  min-height: 80px;
}

.field-textarea:focus { border-color: var(--admin-accent, #b85c38); }
.field-textarea::placeholder { color: var(--admin-sidebar-text-muted, #b0a89e); }

/* ── Password toggle ── */

.input-wrap { position: relative; display: flex; align-items: center; }

.pwd-toggle {
  position: absolute;
  right: 8px;
  display: flex;
  align-items: center;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--admin-sidebar-text-muted, #b0a89e);
  padding: 0;
  transition: color 0.15s;
}

.pwd-toggle svg { width: 15px; height: 15px; }
.pwd-toggle:hover { color: var(--admin-sidebar-text, #5a5248); }

/* ── Buttons ── */

.primary-btn {
  height: 34px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--admin-accent, #b85c38);
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.primary-btn:hover:not(:disabled) { background: var(--admin-accent-dark, #924530); }
.primary-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.ghost-btn {
  height: 36px;
  padding: 0 14px;
  flex-shrink: 0;
  border: 1px solid var(--admin-sidebar-border, #e8e4de);
  border-radius: 4px;
  background: transparent;
  color: var(--admin-sidebar-text, #5a5248);
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  cursor: pointer;
  transition: background 0.15s;
}

.ghost-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.btn-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

/* ── Responsive ── */

@media (max-width: 768px) {
  .profile-page { max-width: 100%; }
  .card-body { padding: 16px; }
  .field-row { flex-direction: column; }
  .field-row--align-end { align-items: stretch; }
}
</style>
