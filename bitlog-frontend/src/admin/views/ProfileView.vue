<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/useUserStore'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import {
  getUserProfile,
  updateUserInfo,
  updateAvatar,
  updatePassword,
  sendEmailChangeCode,
  updateEmail,
  initPassword,
  listIdentities,
  createBindIntent,
  unbindIdentity,
  type UserProfile,
  type UserIdentity,
} from '@/api/user'
import { uploadFile } from '@/api/file'
import {
  validateUsername,
  validatePassword,
  validateEmail,
  validateCode,
} from '@/utils/authValidation'
import { startOAuthBind } from '@/utils/oauth'
import PasswordInput from '@/components/common/PasswordInput.vue'

type TabKey = 'profile' | 'security'

const route = useRoute()
const router = useRouter()
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

// 第三方绑定
const identities = ref<UserIdentity[]>([])
const googleIdentity = computed(() => identities.value.find((i) => i.provider === 'google') ?? null)
// 老账号在 hasPassword 上线前的响应里没有该字段，缺省按「已设密码」处理，避免误显示设置入口
const hasPassword = computed(() => profile.value?.hasPassword !== false)

// 邮箱
const emailEditing = ref(false)
const emailForm = reactive({ newEmail: '', code: '' })
const emailSending = ref(false)
const emailSaving = ref(false)
const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | undefined

onMounted(() => {
  loadProfile()
  consumeBindResult()
})
onUnmounted(() => clearInterval(countdownTimer))

async function loadProfile() {
  pageLoading.value = true
  try {
    const [detail, bound] = await Promise.all([getUserProfile(), listIdentities()])
    profile.value = detail
    identities.value = bound
    syncBasicForm()
  } catch {
    toast.error('获取个人信息失败')
  } finally {
    pageLoading.value = false
  }
}

/** 绑定走整页跳转，结果只能由回跳参数带回；读取后清掉，刷新不再重复提示 */
function consumeBindResult() {
  const bind = route.query.bind
  if (!bind) return
  activeTab.value = 'security'
  if (bind === 'success') {
    toast.success('Google 账号已绑定')
  } else {
    toast.error((route.query.reason as string) || '绑定失败，请重试')
  }
  router.replace({ query: {} })
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
    // 空串必须原样提交：转成 undefined 会让字段在 JSON 里消失，后端收到 null
    // 视为「不更新」，用户清空职位/公司/简介的动作会被静默丢弃
    await updateUserInfo({
      username: basicForm.username.trim(),
      position: basicForm.position.trim(),
      company: basicForm.company.trim(),
      profile: basicForm.profile.trim(),
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
  // 无密码账号（Google 建号）走「设置密码」：会话本身已证明身份，不需要旧密码
  if (hasPassword.value && !passwordForm.oldPassword) {
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
  const initial = !hasPassword.value
  passwordSaving.value = true
  try {
    if (initial) {
      await initPassword(passwordForm.newPassword)
      // hasPassword 由 profile 提供，设完须重取，否则界面仍停在「设置密码」
      profile.value = await getUserProfile()
    } else {
      await updatePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      })
    }
    toast.success(initial ? '密码已设置' : '密码已修改')
    resetPasswordForm()
    passwordEditing.value = false
  } catch (err: unknown) {
    // 后端校验旧密码失败抛 ACCOUNT_OR_PASSWORD_ERROR(41002)，不是 USER_NOT_EXISTS(42001)
    const code = (err as { code?: number })?.code
    toast.error(code === 41002 ? '当前密码错误' : '操作失败，请重试')
  } finally {
    passwordSaving.value = false
  }
}

// ── 邮箱 ──────────────────────────────────────────────────────────────────────

function toggleEmailEdit() {
  emailEditing.value = !emailEditing.value
  if (!emailEditing.value) {
    emailForm.newEmail = ''
    emailForm.code = ''
  }
}

function startCountdown() {
  codeCountdown.value = 60
  clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    if (--codeCountdown.value <= 0) clearInterval(countdownTimer)
  }, 1000)
}

// 后端消息可直接展示，唯 42002 的「用户已存在」在改邮箱语境下读着别扭，单独换掉
function emailErrorMessage(err: unknown, fallback: string) {
  const { code, message } = (err ?? {}) as { code?: number; message?: string }
  if (code === 42002) return '该邮箱已被使用'
  const known = code === 41006 || code === 41005 || code === 40000
  return known && message ? message : fallback
}

async function sendEmailCode() {
  const error = validateEmail(emailForm.newEmail)
  if (error) {
    toast.warning(error)
    return
  }
  emailSending.value = true
  try {
    // 占用校验由后端在发码前完成，用户不会收到码之后才被告知邮箱不可用
    await sendEmailChangeCode(emailForm.newEmail.trim())
    startCountdown()
    toast.success('验证码已发送到新邮箱')
  } catch (err) {
    toast.error(emailErrorMessage(err, '发送失败，请重试'))
  } finally {
    emailSending.value = false
  }
}

async function saveEmail() {
  const emailError = validateEmail(emailForm.newEmail)
  if (emailError) {
    toast.warning(emailError)
    return
  }
  const codeError = validateCode(emailForm.code)
  if (codeError) {
    toast.warning(codeError)
    return
  }
  emailSaving.value = true
  try {
    await updateEmail({ email: emailForm.newEmail.trim(), code: emailForm.code })
    await userStore.fetchProfile()
    profile.value = userInfo.value
    toast.success('邮箱已更新')
    toggleEmailEdit()
  } catch (err) {
    toast.error(emailErrorMessage(err, '修改失败，请重试'))
  } finally {
    emailSaving.value = false
  }
}

// ── 第三方登录 ────────────────────────────────────────────────────────────────

const confirm = useConfirm()

async function bindGoogle() {
  try {
    const intent = await createBindIntent()
    // 整页跳转而非 XHR：授权链路要求浏览器导航到 Google
    startOAuthBind('google', intent)
  } catch {
    toast.error('发起绑定失败，请重试')
  }
}

async function unbindGoogle() {
  try {
    await confirm('解绑后将无法使用该 Google 账号登录。', '解绑 Google', {
      confirmText: '解绑',
      danger: true,
    })
  } catch {
    return
  }
  try {
    await unbindIdentity('google')
    identities.value = await listIdentities()
    toast.success('已解绑')
  } catch (err) {
    // 40010 是「解绑后将无法登录，请先设置密码」这类前置条件不满足，后端消息可直接展示
    const { code, message } = (err ?? {}) as { code?: number; message?: string }
    toast.error(code === 40010 && message ? message : '解绑失败，请重试')
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
          <!-- 邮箱 -->
          <div class="setting-item">
            <div class="setting-row">
              <div class="setting-main">
                <div class="setting-title">邮箱</div>
                <div class="setting-value">{{ profile?.email ?? '—' }}</div>
              </div>
              <button class="ghost-btn" @click="toggleEmailEdit">
                {{ emailEditing ? '取消' : '修改' }}
              </button>
            </div>

            <div v-if="emailEditing" class="setting-expand">
              <div class="field">
                <label class="field-label">新邮箱</label>
                <div class="field-inline">
                  <input
                    v-model="emailForm.newEmail"
                    class="field-input"
                    placeholder="new@example.com"
                    maxlength="128"
                  />
                  <button
                    class="ghost-btn"
                    :disabled="codeCountdown > 0 || emailSending"
                    @click="sendEmailCode"
                  >
                    {{ codeCountdown > 0 ? `${codeCountdown} 秒后重发` : '发送验证码' }}
                  </button>
                </div>
              </div>
              <div class="field field--narrow">
                <label class="field-label">验证码</label>
                <input
                  v-model="emailForm.code"
                  class="field-input"
                  placeholder="6 位数字"
                  maxlength="6"
                  inputmode="numeric"
                />
              </div>
              <div class="form-actions">
                <button class="primary-btn" :disabled="emailSaving" @click="saveEmail">
                  <span v-if="emailSaving" class="btn-spinner" />
                  确认修改
                </button>
              </div>
            </div>
          </div>

          <!-- 密码 -->
          <div class="setting-item">
            <div class="setting-row">
              <div class="setting-main">
                <div class="setting-title">密码</div>
                <div class="setting-value">{{ hasPassword ? '已设置' : '未设置' }}</div>
              </div>
              <button class="ghost-btn" @click="togglePasswordEdit">
                {{ passwordEditing ? '取消' : hasPassword ? '修改' : '设置密码' }}
              </button>
            </div>

            <div v-if="passwordEditing" class="setting-expand">
              <div v-if="hasPassword" class="field">
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
                  {{ hasPassword ? '确认修改' : '设置密码' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 第三方登录 -->
          <div class="setting-item">
            <div class="setting-row">
              <div class="setting-main setting-main--provider">
                <!-- Google 品牌规范要求 G 标使用未经修改的官方四色版本，不得改色 -->
                <svg class="provider-logo" viewBox="0 0 48 48" aria-hidden="true">
                  <path
                    fill="#EA4335"
                    d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
                  />
                  <path
                    fill="#4285F4"
                    d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"
                  />
                  <path
                    fill="#FBBC05"
                    d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"
                  />
                  <path
                    fill="#34A853"
                    d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
                  />
                </svg>
                <div class="provider-text">
                  <div class="setting-title">Google</div>
                  <div class="setting-value">
                    {{ googleIdentity ? (googleIdentity.providerEmail ?? '已绑定') : '未绑定' }}
                  </div>
                </div>
              </div>
              <button v-if="googleIdentity" class="ghost-btn" @click="unbindGoogle">解绑</button>
              <button v-else class="ghost-btn" @click="bindGoogle">绑定</button>
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

.field-inline {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* .field-input 是 width:100%，进 flex 容器后需显式收缩才能给按钮让位 */
.field-inline .field-input {
  flex: 1;
  min-width: 0;
}

.field--narrow {
  max-width: 220px;
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

.setting-main--provider {
  flex-direction: row;
  align-items: center;
  gap: 12px;
}

.provider-logo {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.provider-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
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
