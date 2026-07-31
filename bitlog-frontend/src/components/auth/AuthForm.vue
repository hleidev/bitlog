<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import PasswordInput from '@/components/common/PasswordInput.vue'
import { useUserStore } from '@/stores/useUserStore'
import { ApiError } from '@/utils/request'
import { resetPassword, sendRegisterCode, sendResetPasswordCode } from '@/api/auth'
import {
  CODE_LENGTH,
  EMAIL_MAX_LENGTH,
  isEmailFormat,
  validateCode,
  validateConfirmPassword,
  validateEmail,
  validatePassword,
  validateUsername,
} from '@/utils/authValidation'

type AuthMode = 'login' | 'register' | 'forgot'

const emit = defineEmits<{ success: [] }>()

const userStore = useUserStore()

const CODE_COOLDOWN = 60
const VERIFY_CODE_INVALID = 41006
const USER_ALREADY_EXISTS = 42002

const mode = ref<AuthMode>('login')
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const apiError = ref('')
const notice = ref('')

const form = ref({ email: '', code: '', username: '', password: '', confirmPassword: '' })
const errors = ref({ email: '', code: '', username: '', password: '', confirmPassword: '' })

const emailInputRef = ref<HTMLInputElement | null>(null)
let countdownTimer: ReturnType<typeof setInterval> | undefined

const title = computed(
  () => ({ login: '登录', register: '创建账号', forgot: '重置密码' })[mode.value],
)

const submitText = computed(() => {
  if (mode.value === 'login') return loading.value ? '登录中...' : '登录'
  if (mode.value === 'register') return loading.value ? '注册中...' : '注册'
  return loading.value ? '提交中...' : '重置密码'
})

const passwordLabel = computed(() => (mode.value === 'forgot' ? '新密码' : '密码'))

const codeButtonText = computed(() => {
  if (countdown.value > 0) return `${countdown.value}s`
  return sendingCode.value ? '发送中' : '获取验证码'
})

const canSendCode = computed(
  () => countdown.value === 0 && !sendingCode.value && isEmailFormat(form.value.email),
)

function checkEmail(): boolean {
  errors.value.email = validateEmail(form.value.email)
  return !errors.value.email
}

function checkCode(): boolean {
  errors.value.code = validateCode(form.value.code)
  return !errors.value.code
}

function checkUsername(): boolean {
  errors.value.username = validateUsername(form.value.username)
  return !errors.value.username
}

function checkPassword(): boolean {
  errors.value.password = validatePassword(form.value.password, passwordLabel.value)
  return !errors.value.password
}

function checkConfirmPassword(): boolean {
  errors.value.confirmPassword = validateConfirmPassword(
    form.value.confirmPassword,
    form.value.password,
  )
  return !errors.value.confirmPassword
}

function sanitizeCode(event: Event) {
  const raw = (event.target as HTMLInputElement).value
  form.value.code = raw.replace(/\D/g, '').slice(0, CODE_LENGTH)
}

function stopCountdown() {
  clearInterval(countdownTimer)
  countdownTimer = undefined
  countdown.value = 0
}

function startCountdown() {
  countdown.value = CODE_COOLDOWN
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) stopCountdown()
  }, 1000)
}

function switchMode(target: AuthMode) {
  mode.value = target
  apiError.value = ''
  notice.value = ''
  // 保留邮箱：切换模式时多半还是同一个账号
  form.value = {
    email: form.value.email,
    code: '',
    username: '',
    password: '',
    confirmPassword: '',
  }
  errors.value = { email: '', code: '', username: '', password: '', confirmPassword: '' }
  stopCountdown()
}

function reportError(err: unknown, fallback: string) {
  if (!(err instanceof ApiError)) {
    apiError.value = fallback
    return
  }
  if (err.code === VERIFY_CODE_INVALID) {
    errors.value.code = err.message
    return
  }
  // 后端只回「用户已存在: <冲突值>」，比对提交值才能定位到具体字段
  if (err.code === USER_ALREADY_EXISTS) {
    const email = form.value.email.trim().toLowerCase()
    if (email && err.message.toLowerCase().includes(email)) {
      errors.value.email = '该邮箱已注册'
    } else {
      errors.value.username = '该用户名已被占用'
    }
    return
  }
  apiError.value = err.message
}

async function handleSendCode() {
  if (!canSendCode.value || !checkEmail()) return
  apiError.value = ''
  notice.value = ''
  errors.value.code = ''
  sendingCode.value = true
  try {
    const email = form.value.email.trim()
    if (mode.value === 'register') {
      await sendRegisterCode(email)
    } else {
      await sendResetPasswordCode(email)
    }
    notice.value = '验证码已发送，5 分钟内有效'
    startCountdown()
  } catch (err) {
    reportError(err, '验证码发送失败，请稍后重试')
  } finally {
    sendingCode.value = false
  }
}

function validateCurrentMode(): boolean {
  const checks =
    mode.value === 'login'
      ? [checkEmail, checkPassword]
      : mode.value === 'register'
        ? [checkUsername, checkEmail, checkCode, checkPassword, checkConfirmPassword]
        : [checkEmail, checkCode, checkPassword, checkConfirmPassword]
  return checks.map((check) => check()).every(Boolean)
}

async function handleSubmit() {
  if (loading.value) return
  apiError.value = ''
  notice.value = ''
  if (!validateCurrentMode()) return

  loading.value = true
  const email = form.value.email.trim()
  try {
    if (mode.value === 'login') {
      await userStore.login({ email, password: form.value.password })
      emit('success')
    } else if (mode.value === 'register') {
      await userStore.register({
        email,
        username: form.value.username.trim(),
        password: form.value.password,
        code: form.value.code,
      })
      emit('success')
    } else {
      await resetPassword({ email, code: form.value.code, newPassword: form.value.password })
      // 回到登录而不是自动登录：让用户用新密码走一遍，确认改密确实生效
      switchMode('login')
      notice.value = '密码重置成功，请用新密码登录'
    }
  } catch (err) {
    reportError(err, '操作失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => nextTick(() => emailInputRef.value?.focus()))
onUnmounted(stopCountdown)
</script>

<template>
  <div class="auth-form-wrap">
    <h2 class="auth-form__title">{{ title }}</h2>

    <form class="auth-form" @submit.prevent="handleSubmit">
      <div v-if="mode === 'register'" class="form-field">
        <label class="form-label" for="auth-username">用户名</label>
        <input
          id="auth-username"
          v-model="form.username"
          class="form-input"
          :class="{ 'form-input--error': errors.username }"
          type="text"
          placeholder="2~16 位，公开展示"
          autocomplete="nickname"
          maxlength="16"
          :disabled="loading"
          @blur="checkUsername"
        />
        <p v-if="errors.username" class="field-error">{{ errors.username }}</p>
      </div>

      <div class="form-field">
        <label class="form-label" for="auth-email">邮箱</label>
        <input
          id="auth-email"
          ref="emailInputRef"
          v-model="form.email"
          class="form-input"
          :class="{ 'form-input--error': errors.email }"
          type="email"
          placeholder="请输入邮箱"
          autocomplete="username"
          :maxlength="EMAIL_MAX_LENGTH"
          :disabled="loading"
          @blur="checkEmail"
        />
        <p v-if="errors.email" class="field-error">{{ errors.email }}</p>
      </div>

      <div v-if="mode !== 'login'" class="form-field">
        <label class="form-label" for="auth-code">验证码</label>
        <div class="code-row">
          <input
            id="auth-code"
            class="form-input"
            :class="{ 'form-input--error': errors.code }"
            :value="form.code"
            type="text"
            inputmode="numeric"
            autocomplete="one-time-code"
            placeholder="6 位数字"
            :maxlength="CODE_LENGTH"
            :disabled="loading"
            @input="sanitizeCode"
            @blur="checkCode"
          />
          <button
            type="button"
            class="code-btn"
            :disabled="!canSendCode || loading"
            @click="handleSendCode"
          >
            {{ codeButtonText }}
          </button>
        </div>
        <p v-if="errors.code" class="field-error">{{ errors.code }}</p>
      </div>

      <div class="form-field">
        <div class="form-field__header">
          <label class="form-label" for="auth-password">{{ passwordLabel }}</label>
          <button
            v-if="mode === 'login'"
            type="button"
            class="form-forgot"
            @click="switchMode('forgot')"
          >
            忘记密码？
          </button>
        </div>
        <PasswordInput
          id="auth-password"
          v-model="form.password"
          :input-class="['form-input', 'form-input--pwd', { 'form-input--error': errors.password }]"
          :placeholder="`请输入${passwordLabel}`"
          :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
          maxlength="20"
          :disabled="loading"
          @blur="checkPassword"
        />
        <p v-if="errors.password" class="field-error">{{ errors.password }}</p>
      </div>

      <div v-if="mode !== 'login'" class="form-field">
        <label class="form-label" for="auth-confirm">确认{{ passwordLabel }}</label>
        <PasswordInput
          id="auth-confirm"
          v-model="form.confirmPassword"
          :input-class="[
            'form-input',
            'form-input--pwd',
            { 'form-input--error': errors.confirmPassword },
          ]"
          :placeholder="`再次输入${passwordLabel}`"
          autocomplete="new-password"
          maxlength="20"
          :disabled="loading"
          @blur="checkConfirmPassword"
        />
        <p v-if="errors.confirmPassword" class="field-error">{{ errors.confirmPassword }}</p>
      </div>

      <p v-if="notice" class="form-notice">{{ notice }}</p>
      <p v-if="apiError" class="form-error">{{ apiError }}</p>

      <button class="form-submit" type="submit" :disabled="loading">{{ submitText }}</button>

      <p class="auth-switch">
        <template v-if="mode === 'login'">
          没有账号？
          <button type="button" class="auth-switch__btn" @click="switchMode('register')">
            立即注册
          </button>
        </template>
        <template v-else-if="mode === 'register'">
          已有账号？
          <button type="button" class="auth-switch__btn" @click="switchMode('login')">
            立即登录
          </button>
        </template>
        <template v-else>
          <button type="button" class="auth-switch__btn" @click="switchMode('login')">
            返回登录
          </button>
        </template>
      </p>
    </form>
  </div>
</template>

<style scoped>
.auth-form__title {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  text-align: center;
  margin-bottom: 28px;
}

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

/* :deep 并列是因为密码框的 input 位于 PasswordInput 内部，scoped 选择器匹配不到 */
.form-input,
:deep(.form-input) {
  height: 44px;
  padding: 0 14px;
  border: 1.5px solid var(--color-border);
  border-radius: 4px;
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-bg);
  transition: border-color var(--transition-base);
}

.form-input::placeholder,
:deep(.form-input::placeholder) {
  color: var(--color-text-faint);
}

.form-input:focus,
:deep(.form-input:focus) {
  border-color: var(--color-accent);
  outline: none;
}

.form-input--error,
:deep(.form-input--error) {
  border-color: var(--color-danger);
}

.form-input--error:focus,
:deep(.form-input--error:focus) {
  border-color: var(--color-danger);
}

.form-input:disabled,
:deep(.form-input:disabled) {
  opacity: 0.6;
  cursor: not-allowed;
}

:deep(.form-input--pwd) {
  padding-right: 42px;
}

.code-row {
  display: flex;
  gap: 8px;
}

.code-row .form-input {
  flex: 1;
  min-width: 0;
  font-variant-numeric: tabular-nums;
}

.code-btn {
  flex-shrink: 0;
  width: 104px;
  height: 44px;
  border: 1.5px solid var(--color-border);
  border-radius: 4px;
  font-size: 13px;
  color: var(--color-accent);
  background: var(--color-bg);
  font-variant-numeric: tabular-nums;
  transition:
    border-color var(--transition-base),
    color var(--transition-base);
}

.code-btn:hover:not(:disabled) {
  border-color: var(--color-accent);
}

.code-btn:disabled {
  color: var(--color-text-faint);
  cursor: not-allowed;
}

.field-error {
  font-size: 12px;
  color: var(--color-danger);
}

.form-notice {
  font-size: 13px;
  color: var(--color-text-secondary);
  text-align: center;
  margin-top: -4px;
}

.form-error {
  font-size: 13px;
  color: var(--color-danger);
  text-align: center;
  margin-top: -4px;
}

.form-submit {
  height: 44px;
  background: var(--color-accent);
  color: var(--color-text-on-accent);
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
</style>
