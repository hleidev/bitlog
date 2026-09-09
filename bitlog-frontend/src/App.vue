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
 * 全局这份再挂一次就会叠出两层：遮罩双倍变暗，点击落在上层遮罩上会被判成取消，
 * 且 public 版不渲染第三出口（如「直接离开」），后台的三按钮确认框会少一个按钮。
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
