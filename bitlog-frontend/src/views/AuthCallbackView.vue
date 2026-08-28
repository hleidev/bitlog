<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import { isInternalPath, OAUTH_REDIRECT_KEY } from '@/utils/oauth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const errorMessage = ref('')

const ERROR_MESSAGES: Record<string, string> = {
  login_rejected: '无法用该 Google 账号登录：邮箱未验证或账号已被禁用',
  oauth_failed: 'Google 授权未完成，请重试',
  unsupported_provider: '暂不支持该登录方式',
  server_error: '登录处理失败，请稍后重试',
}

onMounted(async () => {
  const error = route.query.error
  if (typeof error === 'string') {
    errorMessage.value = ERROR_MESSAGES[error] ?? '登录失败，请重试'
    return
  }

  // 不能自己调 refresh：Refresh Token 单次使用强制轮换，与 initSession 并发会互相作废
  await userStore.waitForSession()

  if (!userStore.isLoggedIn) {
    errorMessage.value = '登录状态获取失败，请重试'
    return
  }

  const from = sessionStorage.getItem(OAUTH_REDIRECT_KEY)
  sessionStorage.removeItem(OAUTH_REDIRECT_KEY)
  router.replace(isInternalPath(from) ? from : '/')
})
</script>

<template>
  <div class="callback">
    <template v-if="errorMessage">
      <p class="callback__error">{{ errorMessage }}</p>
      <RouterLink to="/" class="callback__link">返回首页</RouterLink>
    </template>
    <p v-else class="callback__hint">正在登录…</p>
  </div>
</template>

<style scoped>
.callback {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px;
}

.callback__hint {
  font-size: 14px;
  color: var(--color-text-muted);
}

.callback__error {
  font-size: 14px;
  color: var(--color-danger);
  text-align: center;
  max-width: 420px;
}

.callback__link {
  font-size: 13px;
  color: var(--color-accent);
  text-decoration: none;
}

.callback__link:hover {
  opacity: 0.75;
}
</style>
