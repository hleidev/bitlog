<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')

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
  loading.value = true
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
      <div class="login-brand">
        <span class="brand-name">Bitlog</span>
        <p class="brand-sub">管理后台</p>
      </div>

      <transition name="shake">
        <div v-if="errorMsg" class="error-bar">{{ errorMsg }}</div>
      </transition>

      <el-form class="login-form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
            :maxlength="16"
            autocomplete="username"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="login-btn"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

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
  background-color: #f0f2f5;
}

.login-card {
  width: 360px;
  background: #ffffff;
  border-radius: 12px;
  padding: 40px 36px 32px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

/* Brand */
.login-brand {
  text-align: center;
  margin-bottom: 32px;
}

.brand-name {
  display: block;
  font-size: 32px;
  font-weight: 800;
  color: #001529;
  letter-spacing: 1px;
  line-height: 1;
}

.brand-sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #8c8c8c;
}

/* Error */
.error-bar {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  padding: 10px 14px;
  font-size: 13px;
  color: #cf1322;
  margin-bottom: 20px;
}

/* Form */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 4px 12px;
}

.login-btn {
  width: 100%;
  border-radius: 8px;
  margin-top: 6px;
  font-size: 15px;
  font-weight: 500;
  background-color: #4a8db7;
  border-color: #4a8db7;
}

.login-btn:hover {
  background-color: #2d6a9f;
  border-color: #2d6a9f;
}

/* Footer */
.login-footer {
  text-align: center;
  margin-top: 24px;
}

.back-link {
  font-size: 13px;
  color: #8c8c8c;
  text-decoration: none;
  transition: color 0.2s;
}

.back-link:hover {
  color: #4a8db7;
}

/* Shake animation on error */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%       { transform: translateX(-6px); }
  40%       { transform: translateX(6px); }
  60%       { transform: translateX(-4px); }
  80%       { transform: translateX(4px); }
}

.shake-enter-active {
  animation: shake 0.4s ease;
}
</style>
