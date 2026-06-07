<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import '@/admin/styles/variables.css'

const router    = useRouter()
const userStore = useUserStore()

const form         = reactive({ username: '', password: '' })
const loading      = ref(false)
const errorMsg     = ref('')
const showPwd      = ref(false)
const usernameRef  = ref<HTMLInputElement | null>(null)

onMounted(() => setTimeout(() => usernameRef.value?.focus(), 50))

async function handleLogin() {
  if (!form.username || !form.password) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  if (!/^[a-zA-Z0-9_-]{4,16}$/.test(form.username)) {
    errorMsg.value = '用户名为 4-16 位字母、数字、下划线或连字符'
    return
  }
  if (!/^[a-zA-Z0-9_@#%&!$*-]{8,20}$/.test(form.password)) {
    errorMsg.value = '密码为 8-20 位，可包含字母、数字及 _@#%&!$*- 符号'
    return
  }
  loading.value  = true
  errorMsg.value = ''
  try {
    await userStore.login({ username: form.username, password: form.password })
    await userStore.fetchProfile()
    router.push('/admin/dashboard')
  } catch {
    errorMsg.value = '用户名或密码错误'
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
              <path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z" />
            </svg>
            <input
              ref="usernameRef"
              v-model="form.username"
              class="field-input"
              type="text"
              placeholder="用户名"
              maxlength="16"
              autocomplete="username"
              @keyup.enter="handleLogin"
            />
          </div>
        </div>

        <div class="field">
          <div class="input-wrap">
            <svg class="field-icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z" />
            </svg>
            <input
              v-model="form.password"
              class="field-input"
              :type="showPwd ? 'text' : 'password'"
              placeholder="密码"
              maxlength="20"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
            <button type="button" class="pwd-toggle" @click="showPwd = !showPwd">
              <svg v-if="showPwd" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.804 11.804 0 0 0 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78 3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z" />
              </svg>
            </button>
          </div>
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <svg v-if="loading" class="btn-spinner-icon" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" stroke-dasharray="40" stroke-dashoffset="15" />
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
  background: #f0ece6;
}

.login-card {
  width: 360px;
  max-width: calc(100vw - 32px);
  background: var(--admin-surface);
  border: 1px solid var(--admin-border);
  border-radius: 4px;
  padding: 40px 36px 32px;
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
  color: var(--admin-sidebar-text-muted, #b0a89e);
  letter-spacing: 0.5px;
}

/* ── Error ── */

.error-bar {
  background: rgba(192, 64, 64, 0.06);
  border: 1px solid rgba(192, 64, 64, 0.25);
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
  color: var(--admin-sidebar-text-muted, #b0a89e);
  pointer-events: none;
  flex-shrink: 0;
}

.field-input {
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

.field-input:focus { border-color: var(--admin-accent, #b85c38); }
.field-input::placeholder { color: var(--admin-sidebar-text-muted, #b0a89e); }

.pwd-toggle {
  position: absolute;
  right: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--admin-sidebar-text-muted, #b0a89e);
  padding: 0;
  transition: color 0.15s;
}

.pwd-toggle svg { width: 16px; height: 16px; }
.pwd-toggle:hover { color: var(--admin-sidebar-text, #5a5248); }

.submit-btn {
  width: 100%;
  height: 40px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: var(--admin-accent, #b85c38);
  color: var(--admin-text-on-accent);
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.submit-btn:hover:not(:disabled) { background: var(--admin-accent-dark, #924530); }
.submit-btn:disabled { opacity: 0.7; cursor: not-allowed; }

.btn-spinner-icon {
  width: 15px;
  height: 15px;
  animation: spin 0.9s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ── Footer ── */

.login-footer {
  text-align: center;
  margin-top: 24px;
}

.back-link {
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted, #b0a89e);
  text-decoration: none;
  transition: color 0.15s;
}

.back-link:hover { color: var(--admin-sidebar-text, #5a5248); }

/* ── Transitions ── */

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%       { transform: translateX(-6px); }
  40%       { transform: translateX(6px); }
  60%       { transform: translateX(-4px); }
  80%       { transform: translateX(4px); }
}

.shake-enter-active { animation: shake 0.4s ease; }
</style>
