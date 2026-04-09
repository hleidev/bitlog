<script setup lang="ts">
import { watch, onMounted } from 'vue'
import { useUserStore } from '@/stores/useUserStore'

const userStore = useUserStore()

onMounted(() => {
  userStore.initSession().catch(() => {
    // 无 Refresh Token 或已过期，用户未登录，正常情况
  })
})

watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn)
      userStore.fetchProfile().catch((err) => {
        console.error('[App] fetchProfile failed:', err)
      })
  },
)
</script>

<template>
  <RouterView />
</template>
