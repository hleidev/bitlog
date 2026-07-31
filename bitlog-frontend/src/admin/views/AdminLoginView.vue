<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import PasswordInput from '@/components/common/PasswordInput.vue'
import { ApiError } from '@/utils/request'
import { EMAIL_MAX_LENGTH, validateEmail, validatePassword } from '@/utils/authValidation'
import '@/admin/styles/variables.css'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({ email: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')
const emailRef = ref<HTMLInputElement | null>(null)

onMounted(() => setTimeout(() => emailRef.value?.focus(), 50))

async function handleLogin() {
  errorMsg.value = validateEmail(form.email) || validatePassword(form.password)
  if (errorMsg.value) return

  loading.value = true
  try {
    await userStore.login({ email: form.email.trim(), password: form.password })
    await userStore.fetchProfile()
    router.push('/admin/dashboard')
  } catch (err) {
    errorMsg.value = err instanceof ApiError ? err.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <!-- Brand -->
      <div class="login-brand">
        <span class="brand-name">Bitlog</span>
        <p class="brand-sub">管理后台</p>
      </div>

      <!-- Error -->
      <Transition name="shake">
        <div v-if="errorMsg" class="error-bar">{{ errorMsg }}</div>
      </Transition>

      <!-- Form -->
      <form class="login-form" @submit.prevent="handleLogin">
        <div class="field">
          <div class="input-wrap">
            <svg class="field-icon" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4-8 5-8-5V6l8 5 8-5v2z"
              />
            </svg>
            <input
              ref="emailRef"
              v-model="form.email"
              class="field-input"
              type="email"
              placeholder="邮箱"
              :maxlength="EMAIL_MAX_LENGTH"
              autocomplete="email"
              @keyup.enter="handleLogin"
            />
          </div>
        </div>

        <div class="field">
          <PasswordInput
            v-model="form.password"
            input-class="field-input"
            placeholder="密码"
            maxlength="20"
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <svg class="field-icon" viewBox="0 0 24 24" fill="currentColor">
                <path
                  d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"
                />
              </svg>
            </template>
          </PasswordInput>
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <svg v-if="loading" class="btn-spinner-icon" viewBox="0 0 24 24" fill="none">
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
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </form>

      <div class="login-footer">
        <a href="/" class="back-link">← 返回网站</a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--admin-surface-hover);
}

.login-card {
  width: 360px;
  max-width: calc(100vw - 32px);
  background: var(--admin-surface);
  border: 1px solid var(--admin-border);
  border-radius: 4px;
  padding: 40px 36px 32px;
  --pwd-toggle-color: var(--admin-sidebar-text-muted);
  --pwd-toggle-color-hover: var(--admin-sidebar-text);
}

/* ── Brand ── */

.login-brand {
  text-align: center;
  margin-bottom: 32px;
}

.brand-name {
  display: block;
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 30px;
  font-weight: 700;
  color: var(--admin-text-primary);
  letter-spacing: 0.5px;
  line-height: 1;
}

.brand-sub {
  margin: 8px 0 0;
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  letter-spacing: 0.5px;
}

/* ── Error ── */

.error-bar {
  background: rgba(var(--admin-danger-strong-rgb), 0.06);
  border: 1px solid rgba(var(--admin-danger-strong-rgb), 0.25);
  border-radius: 4px;
  padding: 10px 14px;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-danger);
  margin-bottom: 20px;
}

/* ── Form ── */

.login-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.field-icon {
  position: absolute;
  left: 11px;
  width: 15px;
  height: 15px;
  color: var(--admin-sidebar-text-muted);
  pointer-events: none;
  flex-shrink: 0;
}

/* :deep 并列是因为密码框的 input 位于 PasswordInput 内部，scoped 选择器匹配不到 */
.field-input,
:deep(.field-input) {
  width: 100%;
  height: 40px;
  padding: 0 38px 0 36px;
  border: 1px solid var(--admin-border);
  border-radius: 4px;
  background: var(--admin-surface-input);
  font-size: 13.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-text-primary);
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.field-input:focus,
:deep(.field-input:focus) {
  border-color: var(--admin-accent);
}
.field-input::placeholder,
:deep(.field-input::placeholder) {
  color: var(--admin-sidebar-text-muted);
}

.submit-btn {
  width: 100%;
  height: 40px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: var(--admin-accent);
  color: var(--admin-text-on-accent);
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.submit-btn:hover:not(:disabled) {
  background: var(--admin-accent-dark);
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-spinner-icon {
  width: 15px;
  height: 15px;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── Footer ── */

.login-footer {
  text-align: center;
  margin-top: 24px;
}

.back-link {
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  text-decoration: none;
  transition: color 0.15s;
}

.back-link:hover {
  color: var(--admin-sidebar-text);
}

/* ── Transitions ── */

@keyframes shake {
  0%,
  100% {
    transform: translateX(0);
  }
  20% {
    transform: translateX(-6px);
  }
  40% {
    transform: translateX(6px);
  }
  60% {
    transform: translateX(-4px);
  }
  80% {
    transform: translateX(4px);
  }
}

.shake-enter-active {
  animation: shake 0.4s ease;
}
</style>
