<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useNotificationStore } from '@/stores/useNotificationStore'
import { useUserStore } from '@/stores/useUserStore'

const route = useRoute()
const notificationStore = useNotificationStore()
const userStore = useUserStore()
const { isLoggedIn } = storeToRefs(userStore)

/**
 * 后台自己在 AdminLayout 里挂 AdminConfirmDialog，而两个对话框读的是同一个 pending，
 * 同一时间只挂一份，避免重复打开原生 dialog，导致遮罩叠加和焦点恢复冲突。
 */
const isAdminRoute = computed(() => route.path.startsWith('/admin'))

let stopLoginWatch: (() => void) | undefined

onMounted(() => {
  stopLoginWatch = watch(
    isLoggedIn,
    (loggedIn) => {
      if (loggedIn) {
        notificationStore.startPolling()
        return
      }
      notificationStore.stopPolling()
      void notificationStore.refreshUnread()
    },
    { immediate: true },
  )
})

onUnmounted(() => {
  stopLoginWatch?.()
  notificationStore.stopPolling()
})
</script>

<template>
  <RouterView />
  <ConfirmDialog v-if="!isAdminRoute" />
</template>
