<script setup lang="ts">
import { watch } from 'vue'
import { useUserStore } from '@/stores/useUserStore'

const userStore = useUserStore()

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
  <ConfirmDialog />
</template>
