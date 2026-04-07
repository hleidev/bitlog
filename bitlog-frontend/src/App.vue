<script setup lang="ts">
import { watch, onMounted } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import LoginModal from '@/components/common/LoginModal.vue'
import { useUserStore } from '@/stores/useUserStore'

const userStore = useUserStore()

// 页面刷新后用 HttpOnly Cookie 中的 Refresh Token 静默恢复会话
onMounted(() => {
  userStore.initSession().catch(() => {
    // 无 Refresh Token 或已过期，用户未登录，正常情况
  })
})

// 登录状态变化时拉取用户信息
watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) userStore.fetchProfile().catch((err) => {
      console.error('[App] fetchProfile failed:', err)
    })
  },
)
</script>

<template>
  <AppHeader />
  <RouterView />
  <AppFooter />
  <LoginModal />
</template>
