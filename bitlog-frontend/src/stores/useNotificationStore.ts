import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import {
  getNotificationUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/notification'
import { useUserStore } from '@/stores/useUserStore'

const POLLING_INTERVAL = 60_000

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const unreadIncreaseVersion = ref(0)
  const userStore = useUserStore()

  let pollingTimer: ReturnType<typeof setInterval> | null = null
  let visibilityListening = false
  let sessionVersion = 0
  let refreshSequence = 0
  let readRollbackEpoch = 0
  let lastServerUnreadCount: number | null = null
  const inFlightReads = new Map<number, { sessionVersion: number; promise: Promise<void> }>()
  const inFlightOperations = new Map<object, number>()

  function currentUserId(): number | null {
    return userStore.userInfo?.userId ?? null
  }

  function isCurrentSession(version: number, userId: number): boolean {
    return version === sessionVersion && userStore.isLoggedIn && currentUserId() === userId
  }

  function hasCurrentOperations(): boolean {
    return [...inFlightOperations.values()].some((version) => version === sessionVersion)
  }

  async function fetchUnreadCount(): Promise<void> {
    const userId = currentUserId()
    if (!userStore.isLoggedIn || userId === null) {
      unreadCount.value = 0
      return
    }
    if (hasCurrentOperations()) return

    const requestSessionVersion = sessionVersion
    const sequence = ++refreshSequence
    const count = await getNotificationUnreadCount()
    if (
      sequence !== refreshSequence ||
      !isCurrentSession(requestSessionVersion, userId) ||
      hasCurrentOperations()
    ) {
      return
    }

    if (lastServerUnreadCount !== null && count > unreadCount.value) {
      unreadIncreaseVersion.value += 1
    }
    lastServerUnreadCount = count
    unreadCount.value = count
  }

  function refreshUnread(): Promise<void> {
    return fetchUnreadCount()
  }

  function calibrateUnread(): void {
    void fetchUnreadCount().catch(() => undefined)
  }

  function finishOperation(operation: object, version: number): void {
    inFlightOperations.delete(operation)
    if (version === sessionVersion && !hasCurrentOperations()) calibrateUnread()
  }

  function markRead(id: number): Promise<void> {
    const existing = inFlightReads.get(id)
    if (existing?.sessionVersion === sessionVersion) return existing.promise

    const requestSessionVersion = sessionVersion
    const rollbackEpoch = readRollbackEpoch
    const operation = {}
    inFlightOperations.set(operation, requestSessionVersion)
    unreadCount.value = Math.max(0, unreadCount.value - 1)

    const promise = markNotificationRead(id)
      .catch((error: unknown) => {
        if (requestSessionVersion === sessionVersion && rollbackEpoch === readRollbackEpoch) {
          unreadCount.value += 1
        }
        throw error
      })
      .finally(() => {
        const current = inFlightReads.get(id)
        if (current?.promise === promise) inFlightReads.delete(id)
        finishOperation(operation, requestSessionVersion)
      })
    inFlightReads.set(id, { sessionVersion: requestSessionVersion, promise })
    return promise
  }

  async function markAllRead(lastNotificationId?: number): Promise<void> {
    const requestSessionVersion = sessionVersion
    const userId = currentUserId()
    const operation = {}
    inFlightOperations.set(operation, requestSessionVersion)
    try {
      await markAllNotificationsRead(lastNotificationId)
      if (userId !== null && isCurrentSession(requestSessionVersion, userId)) {
        readRollbackEpoch += 1
        unreadCount.value = 0
      }
    } finally {
      finishOperation(operation, requestSessionVersion)
    }
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

  watch(
    () => [userStore.isLoggedIn, currentUserId()] as const,
    ([loggedIn, userId], [previousLoggedIn, previousUserId]) => {
      if (loggedIn === previousLoggedIn && userId === previousUserId) return
      sessionVersion += 1
      refreshSequence += 1
      lastServerUnreadCount = null
      unreadCount.value = 0
      if (loggedIn && userId !== null && pollingTimer) poll()
    },
    { flush: 'sync' },
  )

  return {
    unreadCount,
    unreadIncreaseVersion,
    refreshUnread,
    markRead,
    markAllRead,
    startPolling,
    stopPolling,
  }
})
