<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import {
  getNotificationPage,
  NOTIFICATION_TYPE,
  type NotificationActorVO,
  type NotificationVO,
} from '@/api/notification'
import { useListQuery } from '@/composables/useListQuery'
import { useNotificationStore } from '@/stores/useNotificationStore'
import { formatRelativeTime } from '@/utils/format'

interface DisplayNotification {
  item: NotificationVO
  message: string
  detail: string
  target: string | null
  time: string
}

const router = useRouter()
const notificationStore = useNotificationStore()
const { unreadCount, unreadIncreaseVersion } = storeToRefs(notificationStore)
const loadError = ref(false)
const initialLoading = ref(true)
const markingAll = ref(false)
const operationError = ref('')
const readErrors = ref<Record<number, string>>({})
const readAllBoundaryId = ref<number>()
const hasNewNotifications = ref(false)
const currentTime = ref(Date.now())
let minuteTimer: ReturnType<typeof setInterval> | null = null
let pageActive = false
let readRollbackEpoch = 0

const query = useListQuery({
  filters: { unreadOnly: 0 as 0 | 1 },
  toParams: (filters) => ({ unreadOnly: filters.unreadOnly ? true : undefined }),
  fetch: async (params) => {
    loadError.value = false
    return getNotificationPage(params)
  },
  syncUrl: true,
  sanitize: (filters) => {
    if (filters.unreadOnly !== 1) filters.unreadOnly = 0
  },
  immediate: false,
  onError: () => {
    loadError.value = true
  },
})

const notifications = query.items
const { filters, loading, pageNum, totalPages, hasPrevious, hasNext } = query
const visiblePages = query.pageNumbers
const pageLoading = computed(() => initialLoading.value || loading.value)

watch(notifications, (items) => {
  if (pageNum.value === 1) readAllBoundaryId.value = items[0]?.id
  readErrors.value = Object.fromEntries(
    Object.entries(readErrors.value).filter(([id]) => {
      const item = items.find((notification) => notification.id === Number(id))
      return item && !item.readTime
    }),
  )
})

watch(unreadIncreaseVersion, () => {
  if (!initialLoading.value) hasNewNotifications.value = true
})

function payloadString(payload: Record<string, unknown>, key: string): string | null {
  const value = payload[key]
  return typeof value === 'string' && value.trim() ? value.trim() : null
}

function payloadNumber(payload: Record<string, unknown>, key: string): number | null {
  const value = payload[key]
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

function articleTarget(payload: Record<string, unknown>, commentId?: number | null): string | null {
  const articleId = payloadNumber(payload, 'articleId')
  if (articleId === null) return null
  return commentId === null || commentId === undefined
    ? `/article/${articleId}`
    : `/article/${articleId}?comment=${commentId}`
}

function systemMessage(payload: Record<string, unknown>): string {
  const preferredKeys = ['message', 'content', 'text', 'title']
  for (const key of preferredKeys) {
    const value = payloadString(payload, key)
    if (value) return value
  }
  const value = Object.values(payload).find(
    (item): item is string => typeof item === 'string' && Boolean(item.trim()),
  )
  return value?.trim() ?? '你有一条系统通知'
}

function buildDisplay(item: NotificationVO): DisplayNotification {
  const actor = item.actor?.username ?? '某位用户'
  const articleTitle = payloadString(item.payload, 'articleTitle') ?? '（文章已删除）'
  const commentSummary = payloadString(item.payload, 'commentSummary') ?? '评论内容暂不可用'
  const linkName = payloadString(item.payload, 'linkName') ?? '未命名站点'

  switch (item.type) {
    case NOTIFICATION_TYPE.COMMENT_REPLY:
      return {
        item,
        message: `${actor} 回复了你的评论`,
        detail: commentSummary,
        target: articleTarget(item.payload, item.targetId),
        time: formatRelativeTime(item.createTime, currentTime.value),
      }
    case NOTIFICATION_TYPE.ARTICLE_COMMENT:
      return {
        item,
        message: `${actor} 评论了你的文章《${articleTitle}》`,
        detail: commentSummary,
        target: articleTarget(item.payload, item.targetId),
        time: formatRelativeTime(item.createTime, currentTime.value),
      }
    case NOTIFICATION_TYPE.LINK_APPLIED:
      return {
        item,
        message: `${actor} 申请了友链「${linkName}」`,
        detail: payloadString(item.payload, 'applyMessage') ?? '未填写申请留言',
        target: '/admin/links',
        time: formatRelativeTime(item.createTime, currentTime.value),
      }
    case NOTIFICATION_TYPE.LINK_REVIEWED: {
      const approved = payloadNumber(item.payload, 'status') === 1
      return {
        item,
        message: `你的友链「${linkName}」${approved ? '已通过审核' : '未通过审核'}`,
        detail:
          payloadString(item.payload, 'rejectReason') ??
          (approved ? '审核结果已更新' : '未提供拒绝理由'),
        target: '/friends',
        time: formatRelativeTime(item.createTime, currentTime.value),
      }
    }
    case NOTIFICATION_TYPE.SYSTEM:
      return {
        item,
        message: systemMessage(item.payload),
        detail: '',
        target: null,
        time: formatRelativeTime(item.createTime, currentTime.value),
      }
    default:
      return {
        item,
        message: '你有一条新通知',
        detail: '',
        target: null,
        time: formatRelativeTime(item.createTime, currentTime.value),
      }
  }
}

const displayNotifications = computed(() => notifications.value.map(buildDisplay))

function avatarLetter(actor: NotificationActorVO | null): string {
  return actor?.username.trim().charAt(0).toUpperCase() || '系'
}

function navigateToTarget(id: number, target: string): void {
  void router.push(target).catch(() => {
    if (!pageActive) return
    readErrors.value = { ...readErrors.value, [id]: '跳转失败，请稍后重试' }
  })
}

async function handleNotification(display: DisplayNotification): Promise<void> {
  operationError.value = ''
  const id = display.item.id
  const nextReadErrors = { ...readErrors.value }
  delete nextReadErrors[id]
  readErrors.value = nextReadErrors

  if (display.item.readTime) {
    if (display.target) navigateToTarget(id, display.target)
    return
  }

  const rollbackEpoch = readRollbackEpoch
  const optimisticReadTime = new Date().toISOString()
  display.item.readTime = optimisticReadTime
  const markRequest = notificationStore.markRead(id)
  if (display.target) navigateToTarget(id, display.target)

  try {
    await markRequest
  } catch {
    if (!pageActive) return
    if (rollbackEpoch !== readRollbackEpoch) return
    const item = notifications.value.find((notification) => notification.id === id)
    if (item?.readTime !== optimisticReadTime) return
    item.readTime = null
    readErrors.value = { ...readErrors.value, [id]: '标记已读失败，请稍后重试' }
  }
}

async function handleMarkAllRead(): Promise<void> {
  if (unreadCount.value === 0 || notifications.value.length === 0 || markingAll.value) return
  markingAll.value = true
  operationError.value = ''
  try {
    await notificationStore.markAllRead(readAllBoundaryId.value)
    readRollbackEpoch += 1
    if (filters.unreadOnly) {
      await query.load()
    } else {
      const readTime = new Date().toISOString()
      notifications.value = notifications.value.map((item) => ({
        ...item,
        readTime: item.readTime ?? readTime,
      }))
    }
    readErrors.value = {}
  } catch {
    operationError.value = '全部标记已读失败，请稍后重试'
  } finally {
    markingAll.value = false
  }
}

function loadNewNotifications(): void {
  hasNewNotifications.value = false
  if (pageNum.value === 1) {
    void query.load()
  } else {
    query.goPage(1)
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function selectFilter(unreadOnly: 0 | 1): void {
  if (filters.unreadOnly !== unreadOnly) filters.unreadOnly = unreadOnly
}

function retryLoad(): void {
  void query.load()
}

function changePage(page: number): void {
  if (page === pageNum.value) return
  if (page < pageNum.value && !hasPrevious.value) return
  if (page > pageNum.value && !hasNext.value) return
  query.goPage(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  pageActive = true
  minuteTimer = setInterval(() => {
    currentTime.value = Date.now()
  }, 60_000)
  query.start()
  await query.load()
  initialLoading.value = false
})

onUnmounted(() => {
  pageActive = false
  if (minuteTimer) clearInterval(minuteTimer)
})
</script>

<template>
  <div class="notifications-page page-state">
    <main class="notifications-main">
      <header class="page-head">
        <h1 class="page-title">通知</h1>
        <button
          class="mark-all"
          type="button"
          :disabled="unreadCount === 0 || notifications.length === 0 || markingAll"
          @click="handleMarkAllRead"
        >
          全部标为已读
        </button>
      </header>

      <div class="notification-filters" aria-label="通知筛选">
        <button
          class="notification-filter"
          :class="{ 'notification-filter--active': filters.unreadOnly === 0 }"
          type="button"
          :aria-pressed="filters.unreadOnly === 0"
          @click="selectFilter(0)"
        >
          全部
        </button>
        <button
          class="notification-filter"
          :class="{ 'notification-filter--active': filters.unreadOnly === 1 }"
          type="button"
          :aria-pressed="filters.unreadOnly === 1"
          @click="selectFilter(1)"
        >
          未读
        </button>
      </div>

      <p v-if="operationError" class="operation-error" role="status">{{ operationError }}</p>

      <button
        v-if="hasNewNotifications"
        class="new-notification-hint"
        type="button"
        @click="loadNewNotifications"
      >
        有新通知，刷新查看
      </button>

      <p v-if="pageLoading && notifications.length === 0" class="notification-state page-state">
        加载中…
      </p>
      <p v-else-if="loadError" class="notification-state page-state">
        加载失败，<button class="retry-button" type="button" @click="retryLoad">重试</button>
      </p>
      <p v-else-if="notifications.length === 0" class="notification-state page-state">
        {{ filters.unreadOnly ? '没有未读通知。' : '暂无通知。' }}
      </p>

      <div v-else :class="['notification-content', { 'notification-content--loading': loading }]">
        <div class="notification-list">
          <component
            :is="display.target ? 'button' : 'div'"
            v-for="display in displayNotifications"
            :key="display.item.id"
            :type="display.target ? 'button' : undefined"
            class="notification-row"
            :class="{ 'notification-row--static': !display.target }"
            @click="display.target && handleNotification(display)"
          >
            <span class="unread-slot" aria-hidden="true">
              <span v-if="!display.item.readTime" class="unread-dot"></span>
            </span>

            <img
              v-if="display.item.actor?.avatar"
              class="actor-avatar"
              :src="display.item.actor.avatar"
              :alt="display.item.actor.username"
              loading="lazy"
            />
            <span
              v-else
              class="actor-avatar actor-avatar--placeholder"
              :class="{ 'actor-avatar--system': !display.item.actor }"
              aria-hidden="true"
            >
              {{ avatarLetter(display.item.actor) }}
            </span>

            <span class="notification-copy">
              <span v-if="!display.item.readTime" class="sr-only">未读</span>
              <span class="notification-message">{{ display.message }}</span>
              <span v-if="display.detail" class="notification-detail">{{ display.detail }}</span>
              <span v-if="readErrors[display.item.id]" class="notification-row-error" role="status">
                {{ readErrors[display.item.id] }}
              </span>
            </span>

            <template v-if="display.target">
              <time class="notification-time" :datetime="display.item.createTime">
                {{ display.time }}
              </time>
            </template>
            <span v-else class="notification-actions">
              <time class="notification-time" :datetime="display.item.createTime">
                {{ display.time }}
              </time>
              <button
                v-if="!display.item.readTime"
                class="mark-read"
                type="button"
                @click.stop="handleNotification(display)"
              >
                标为已读
              </button>
            </span>
          </component>
        </div>

        <div v-if="totalPages > 1" class="pagination">
          <button
            class="page-btn page-btn--arrow"
            type="button"
            :disabled="!hasPrevious"
            aria-label="上一页"
            @click="changePage(pageNum - 1)"
          >
            ←
          </button>
          <template v-for="(page, index) in visiblePages" :key="index">
            <span v-if="page === '…'" class="page-ellipsis">…</span>
            <button
              v-else
              class="page-btn"
              type="button"
              :class="{ 'page-btn--active': page === pageNum }"
              :aria-current="page === pageNum ? 'page' : undefined"
              @click="changePage(page)"
            >
              {{ page }}
            </button>
          </template>
          <button
            class="page-btn page-btn--arrow"
            type="button"
            :disabled="!hasNext"
            aria-label="下一页"
            @click="changePage(pageNum + 1)"
          >
            →
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.notifications-main {
  max-width: var(--spacing-prose);
  margin: 0 auto;
  padding: 72px var(--spacing-page-padding) 120px;
}

.page-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 40px;
}

.notification-filters {
  display: flex;
  gap: 20px;
  margin: -20px 0 28px;
}

.notification-filter,
.retry-button,
.mark-read {
  padding: 0;
  color: var(--color-text-secondary);
  font: inherit;
  background: transparent;
  border: none;
  cursor: pointer;
}

.notification-filter {
  font-size: 13px;
}

.notification-filter--active,
.notification-filter:hover,
.retry-button:hover,
.mark-read:hover {
  color: var(--color-accent);
}

.page-title {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 500;
  letter-spacing: -0.01em;
}

.mark-all {
  padding: 2px 0;
  color: var(--color-accent);
  font-family: var(--font-sans);
  font-size: 13px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color var(--transition-base);
}

.mark-all:hover:not(:disabled) {
  color: var(--color-accent-dark);
}

.mark-all:disabled {
  color: var(--color-text-faint);
  cursor: not-allowed;
}

.operation-error {
  margin: -20px 0 24px;
  color: var(--color-danger-on-soft);
  font-size: 12.5px;
  text-align: right;
}

.new-notification-hint {
  display: block;
  margin: -20px 0 24px auto;
  padding: 0;
  color: var(--color-accent);
  font: inherit;
  font-size: 12.5px;
  background: transparent;
  border: none;
  cursor: pointer;
}

.new-notification-hint:hover {
  color: var(--color-accent-dark);
}

.notification-state {
  margin: 0;
  padding: 80px 0;
  color: var(--color-text-muted);
  font-size: 13.5px;
  text-align: center;
}

.notification-content {
  transition: opacity var(--transition-base);
}

.notification-content--loading {
  opacity: 0.4;
  pointer-events: none;
}

.notification-list {
  border-top: 1px solid var(--color-border-light);
}

.notification-row {
  display: grid;
  grid-template-columns: 8px 30px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 18px 12px 18px 0;
  color: var(--color-text-primary);
  font-family: var(--font-sans);
  text-align: left;
  background: transparent;
  border: none;
  border-bottom: 1px solid var(--color-border-light);
  cursor: pointer;
  transition: background var(--transition-base);
}

.notification-row:hover {
  background: var(--color-bg-hover);
}

.notification-row--static {
  cursor: default;
}

.notification-row--static:hover {
  background: transparent;
}

.unread-slot {
  display: grid;
  width: 8px;
  place-items: center;
}

.unread-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-accent);
}

.actor-avatar {
  display: block;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 50%;
  object-fit: cover;
}

.actor-avatar--placeholder {
  display: grid;
  place-items: center;
  color: var(--color-text-on-accent);
  font-size: 12px;
  font-weight: 600;
  background: var(--color-accent);
}

.actor-avatar--system {
  color: var(--color-text-secondary);
  background: var(--color-bg-hover);
  border: 1px solid var(--color-border);
}

.notification-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.notification-message {
  overflow: hidden;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-detail {
  display: -webkit-box;
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: 12.5px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

.notification-row-error {
  color: var(--color-danger-on-soft);
  font-size: 12px;
}

.notification-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mark-read {
  color: var(--color-accent);
  font-size: 12px;
  white-space: nowrap;
}

.notification-time {
  color: var(--color-text-faint);
  font-size: 11.5px;
  white-space: nowrap;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 56px;
}

.page-btn {
  min-width: 34px;
  height: 34px;
  padding: 0 10px;
  color: var(--color-text-secondary);
  font-family: var(--font-sans);
  font-size: 13px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  cursor: pointer;
  transition:
    color var(--transition-base),
    background var(--transition-base),
    border-color var(--transition-base);
}

.page-btn:hover:not(:disabled) {
  color: var(--color-accent);
  border-color: var(--color-accent);
}

.page-btn--active {
  color: var(--color-text-on-accent);
  background: var(--color-accent);
  border-color: var(--color-accent);
}

.page-btn--active:hover:not(:disabled) {
  color: var(--color-text-on-accent);
  background: var(--color-accent-dark);
  border-color: var(--color-accent-dark);
}

.page-btn--arrow {
  font-size: 15px;
}

.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-ellipsis {
  padding: 0 4px;
  color: var(--color-text-faint);
  user-select: none;
}

@media (max-width: 768px) {
  .notifications-main {
    padding: 48px 20px 80px;
  }

  .page-head {
    margin-bottom: 32px;
  }

  .notification-row {
    grid-template-columns: 8px 30px minmax(0, 1fr);
    gap: 12px;
    padding-right: 8px;
  }

  .notification-row > .notification-time,
  .notification-row > .notification-actions {
    grid-column: 3;
    justify-self: start;
  }
}
</style>
