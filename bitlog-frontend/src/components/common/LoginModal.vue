<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import BaseModal from './BaseModal.vue'
import { useModalStore } from '@/stores/useModalStore'
import { useUserStore } from '@/stores/useUserStore'
import { ApiError } from '@/utils/request'

type ModeType = 'login' | 'register'

const modalStore = useModalStore()
const userStore = useUserStore()

const mode = ref<ModeType>('login')

// 登录表单
const loginForm = ref({ username: '', password: '' })
const loginErrors = ref({ username: '', password: '' })

// 注册表单
const registerForm = ref({ username: '', password: '', confirmPassword: '' })
const registerErrors = ref({ username: '', password: '', confirmPassword: '' })

const loading        = ref(false)
const apiError       = ref('')
const usernameInputRef = ref<HTMLInputElement | null>(null)

const USERNAME_RE = /^[a-zA-Z0-9_-]{4,16}$/
const PASSWORD_RE = /^[a-zA-Z0-9_@#%&!$*-]{8,20}$/

// ——— 登录校验 ———
function validateLoginUsername(): boolean {
  const val = loginForm.value.username.trim()
  if (!val) {
    loginErrors.value.username = '请输入用户名'
    return false
  }
  if (!USERNAME_RE.test(val)) {
    loginErrors.value.username = '用户名为 4~16 位，只能含字母、数字、_ 或 -'
    return false
  }
  loginErrors.value.username = ''
  return true
}

function validateLoginPassword(): boolean {
  const val = loginForm.value.password
  if (!val) {
    loginErrors.value.password = '请输入密码'
    return false
  }
  if (!PASSWORD_RE.test(val)) {
    loginErrors.value.password = '密码为 8~20 位，可含字母、数字及常用符号'
    return false
  }
  loginErrors.value.password = ''
  return true
}

const handleLogin = async () => {
  apiError.value = ''
  const okUser = validateLoginUsername()
  const okPass = validateLoginPassword()
  if (!okUser || !okPass) return
  loading.value = true
  try {
    await userStore.login({
      username: loginForm.value.username.trim(),
      password: loginForm.value.password,
    })
    modalStore.close()
  } catch (err) {
    apiError.value = err instanceof ApiError ? err.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// ——— 注册校验 ———
function validateRegUsername(): boolean {
  const val = registerForm.value.username.trim()
  if (!val) {
    registerErrors.value.username = '请输入用户名'
    return false
  }
  if (!USERNAME_RE.test(val)) {
    registerErrors.value.username = '用户名为 4~16 位，只能含字母、数字、_ 或 -'
    return false
  }
  registerErrors.value.username = ''
  return true
}

function validateRegPassword(): boolean {
  const val = registerForm.value.password
  if (!val) {
    registerErrors.value.password = '请输入密码'
    return false
  }
  if (!PASSWORD_RE.test(val)) {
    registerErrors.value.password = '密码为 8~20 位，可含字母、数字及常用符号'
    return false
  }
  registerErrors.value.password = ''
  return true
}

function validateRegConfirm(): boolean {
  const val = registerForm.value.confirmPassword
  if (!val) {
    registerErrors.value.confirmPassword = '请再次输入密码'
    return false
  }
  if (val !== registerForm.value.password) {
    registerErrors.value.confirmPassword = '两次密码输入不一致'
    return false
  }
  registerErrors.value.confirmPassword = ''
  return true
}

const handleRegister = async () => {
  apiError.value = ''
  const ok = [validateRegUsername(), validateRegPassword(), validateRegConfirm()].every(Boolean)
  if (!ok) return
  loading.value = true
  try {
    // TODO: 调用注册接口 POST /api/v1/auth/register，参数 { username, password }
    // TODO: 注册成功后自动登录或跳转到登录 tab
    await new Promise((resolve) => setTimeout(resolve, 800))
    modalStore.close()
  } catch (err) {
    // TODO: 根据接口错误码区分「用户名已存在」等提示
    apiError.value = err instanceof ApiError ? err.message : '注册失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

const switchMode = (target: ModeType) => {
  mode.value = target
  apiError.value = ''
  loginErrors.value = { username: '', password: '' }
  registerErrors.value = { username: '', password: '', confirmPassword: '' }
}

// 每次打开 modal 时重置表单，避免上次的数据残留
watch(
  () => modalStore.visible,
  (visible) => {
    if (visible) {
      mode.value = 'login'
      loginForm.value = { username: '', password: '' }
      loginErrors.value = { username: '', password: '' }
      registerForm.value = { username: '', password: '', confirmPassword: '' }
      registerErrors.value = { username: '', password: '', confirmPassword: '' }
      apiError.value = ''
      nextTick(() => usernameInputRef.value?.focus())
    }
  },
)
</script>

<template>
  <BaseModal :visible="modalStore.visible && modalStore.activeModal === 'login'" @close="modalStore.close()">
    <div class="auth-modal">
      <button class="auth-modal__close" @click="modalStore.close()" aria-label="关闭">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M18 6 6 18M6 6l12 12"/>
        </svg>
      </button>

      <!-- 标题 -->
      <h2 class="auth-modal__title">{{ mode === 'login' ? '登录' : '创建账号' }}</h2>

      <!-- 登录表单 -->
      <form v-if="mode === 'login'" class="auth-form" @submit.prevent="handleLogin">
        <div class="form-field">
          <label class="form-label" for="login-username">用户名</label>
          <input
            id="login-username"
            ref="usernameInputRef"
            v-model="loginForm.username"
            class="form-input"
            :class="{ 'form-input--error': loginErrors.username }"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
            :disabled="loading"
            @blur="validateLoginUsername"
            @keyup.enter="handleLogin"
          />
          <p v-if="loginErrors.username" class="field-error">{{ loginErrors.username }}</p>
        </div>

        <div class="form-field">
          <div class="form-field__header">
            <label class="form-label" for="login-password">密码</label>
            <!-- TODO: 跳转忘记密码页面 / 触发重置密码流程接口 -->
            <button type="button" class="form-forgot">忘记密码？</button>
          </div>
          <input
            id="login-password"
            v-model="loginForm.password"
            class="form-input"
            :class="{ 'form-input--error': loginErrors.password }"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            :disabled="loading"
            @blur="validateLoginPassword"
            @keyup.enter="handleLogin"
          />
          <p v-if="loginErrors.password" class="field-error">{{ loginErrors.password }}</p>
        </div>

        <p v-if="apiError" class="form-error">{{ apiError }}</p>

        <button class="form-submit" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <p class="auth-switch">
          没有账号？
          <button type="button" class="auth-switch__btn auth-switch__btn--disabled" disabled title="注册功能暂未开放">立即注册</button>
        </p>
      </form>

      <!-- 注册表单 -->
      <form v-else class="auth-form" @submit.prevent="handleRegister">
        <div class="form-field">
          <label class="form-label" for="reg-username">用户名</label>
          <input
            id="reg-username"
            v-model="registerForm.username"
            class="form-input"
            :class="{ 'form-input--error': registerErrors.username }"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
            :disabled="loading"
            @blur="validateRegUsername"
          />
          <p v-if="registerErrors.username" class="field-error">{{ registerErrors.username }}</p>
        </div>

        <div class="form-field">
          <label class="form-label" for="reg-password">密码</label>
          <input
            id="reg-password"
            v-model="registerForm.password"
            class="form-input"
            :class="{ 'form-input--error': registerErrors.password }"
            type="password"
            placeholder="请输入密码"
            autocomplete="new-password"
            :disabled="loading"
            @blur="validateRegPassword"
          />
          <p v-if="registerErrors.password" class="field-error">{{ registerErrors.password }}</p>
        </div>

        <div class="form-field">
          <label class="form-label" for="reg-confirm">确认密码</label>
          <input
            id="reg-confirm"
            v-model="registerForm.confirmPassword"
            class="form-input"
            :class="{ 'form-input--error': registerErrors.confirmPassword }"
            type="password"
            placeholder="再次输入密码"
            autocomplete="new-password"
            :disabled="loading"
            @blur="validateRegConfirm"
          />
          <p v-if="registerErrors.confirmPassword" class="field-error">{{ registerErrors.confirmPassword }}</p>
        </div>

        <p v-if="apiError" class="form-error">{{ apiError }}</p>

        <button class="form-submit" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <p class="auth-switch">
          已有账号？
          <button type="button" class="auth-switch__btn" @click="switchMode('login')">立即登录</button>
        </p>
      </form>
    </div>
  </BaseModal>
</template>

<style scoped>
.auth-modal {
  padding: 40px;
  position: relative;
}

.auth-modal__close {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: var(--color-text-faint);
  transition: all var(--transition-base);
}

.auth-modal__close svg {
  width: 16px;
  height: 16px;
}

.auth-modal__close:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}


.auth-modal__title {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  text-align: center;
  margin-bottom: 28px;
}

.auth-switch {
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.auth-switch__btn {
  color: var(--color-accent);
  font-size: 13px;
  transition: opacity var(--transition-base);
}

.auth-switch__btn:hover {
  opacity: 0.75;
}

.auth-switch__btn--disabled {
  color: var(--color-text-faint);
  cursor: not-allowed;
  pointer-events: auto;
}

.auth-switch__btn--disabled:hover {
  opacity: 1;
}

/* Form */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-field__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.form-forgot {
  font-size: 12px;
  color: var(--color-text-faint);
  transition: color var(--transition-base);
}

.form-forgot:hover {
  color: var(--color-accent);
}

.form-input {
  height: 44px;
  padding: 0 14px;
  border: 1.5px solid var(--color-border);
  border-radius: 4px;
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-bg);
  transition: border-color var(--transition-base);
}

.form-input::placeholder {
  color: var(--color-text-faint);
}

.form-input:focus {
  border-color: var(--color-accent);
  outline: none;
}

.form-input--error {
  border-color: #ef4444;
}

.form-input--error:focus {
  border-color: #ef4444;
}

.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.field-error {
  font-size: 12px;
  color: #ef4444;
}

.form-error {
  font-size: 13px;
  color: #ef4444;
  text-align: center;
  margin-top: -4px;
}

.form-submit {
  height: 44px;
  background: var(--color-accent);
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  border-radius: 4px;
  transition: opacity var(--transition-base);
  margin-top: 4px;
}

.form-submit:hover:not(:disabled) {
  opacity: 0.88;
}

.form-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
