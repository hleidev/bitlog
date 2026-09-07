import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getNotificationUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/notification'
import { useUserStore } from '@/stores/useUserStore'

const POLLING_INTERVAL = 60_000

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const userStore = useUserStore()

  let pollingTimer: ReturnType<typeof setInterval> | null = null
  let visibilityListening = false

  async function refreshUnread(): Promise<void> {
    if (!userStore.isLoggedIn) {
      unreadCount.value = 0
      return
    }

    const count = await getNotificationUnreadCount()
    unreadCount.value = userStore.isLoggedIn ? count : 0
  }

  async function markRead(id: number): Promise<void> {
    await markNotificationRead(id)
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }

  async function markAllRead(): Promise<void> {
    await markAllNotificationsRead()
    unreadCount.value = 0
  }

  function clearPollingTimer(): void {
    if (!pollingTimer) return
    clearInterval(pollingTimer)
    pollingTimer = null
  }

  function poll(): void {
    void refreshUnread().catch(() => undefined)
  }

  function resumePolling(): void {
    if (!userStore.isLoggedIn || document.hidden) return
    clearPollingTimer()
    poll()
    pollingTimer = setInterval(poll, POLLING_INTERVAL)
  }

  function handleVisibilityChange(): void {
    if (document.hidden) {
      clearPollingTimer()
      return
    }
    resumePolling()
  }

  function startPolling(): void {
    stopPolling()
    if (!userStore.isLoggedIn) {
      unreadCount.value = 0
      return
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    visibilityListening = true
    resumePolling()
  }

  function stopPolling(): void {
    clearPollingTimer()
    if (visibilityListening) {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      visibilityListening = false
    }
  }

  return { unreadCount, refreshUnread, markRead, markAllRead, startPolling, stopPolling }
})
