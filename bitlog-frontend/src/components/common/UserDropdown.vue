<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useConfirm } from '@/composables/useConfirm'
import { useNotificationStore } from '@/stores/useNotificationStore'
import { useUserStore } from '@/stores/useUserStore'
import { storeToRefs } from 'pinia'

withDefaults(defineProps<{ showAdminLinks?: boolean }>(), {
  showAdminLinks: false,
})

const emit = defineEmits<{ close: [restoreFocus?: boolean] }>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { userInfo, isAdmin } = storeToRefs(userStore)
const { unreadCount } = storeToRefs(notificationStore)
const confirm = useConfirm()

const displayName = computed(() => userInfo.value?.username ?? '')
const avatarLetter = computed(() => displayName.value.charAt(0).toUpperCase() || '?')
const unreadLabel = computed(() => (unreadCount.value > 99 ? '99+' : String(unreadCount.value)))
const avatarFailed = ref(false)
watch(
  () => userInfo.value?.avatar,
  () => {
    avatarFailed.value = false
  },
)

function close() {
  emit('close')
}

async function handleLogout() {
  emit('close', true)
  await nextTick()
  try {
    await confirm('退出后，你仍可以浏览文章，登录后可继续评论和查看通知。', '退出登录？', {
      confirmText: '退出登录',
      cancelText: '取消',
    })
  } catch {
    return
  }
  await userStore.logout()
  router.push(route.path.startsWith('/admin') ? '/admin/login' : '/')
}
</script>

<template>
  <div class="user-dropdown" role="menu" aria-label="账号菜单">
    <!-- 顶部：头像 + 用户名 -->
    <div class="ud-profile" role="presentation">
      <img
        v-if="userInfo?.avatar && !avatarFailed"
        :src="userInfo.avatar"
        class="ud-avatar ud-avatar--img"
        :alt="displayName"
        @error="avatarFailed = true"
      />
      <span v-else class="ud-avatar ud-avatar--placeholder">{{ avatarLetter }}</span>
      <div class="ud-profile-info">
        <span class="ud-name" :title="displayName">{{ displayName }}</span>
        <span class="ud-role">{{ isAdmin ? '管理员' : '读者' }}</span>
      </div>
    </div>

    <div class="ud-divider" />

    <!-- Admin 专属（仅前台展示，admin 后台本身不需要） -->
    <template v-if="showAdminLinks && isAdmin">
      <RouterLink
        to="/admin/dashboard"
        class="ud-item"
        role="menuitem"
        tabindex="-1"
        @click="close"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="3" y="3" width="7" height="7" rx="1" />
          <rect x="14" y="3" width="7" height="7" rx="1" />
          <rect x="3" y="14" width="7" height="7" rx="1" />
          <rect x="14" y="14" width="7" height="7" rx="1" />
        </svg>
        工作台
      </RouterLink>
      <RouterLink to="/admin/articles" class="ud-item" role="menuitem" tabindex="-1" @click="close">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <polyline points="14 2 14 8 20 8" />
          <line x1="16" y1="13" x2="8" y2="13" />
          <line x1="16" y1="17" x2="8" y2="17" />
        </svg>
        文章
      </RouterLink>
      <RouterLink to="/admin/comments" class="ud-item" role="menuitem" tabindex="-1" @click="close">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
        评论
      </RouterLink>
      <RouterLink to="/admin/users" class="ud-item" role="menuitem" tabindex="-1" @click="close">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
        用户
      </RouterLink>
      <div class="ud-divider" />
    </template>

    <RouterLink to="/notifications" class="ud-item" role="menuitem" tabindex="-1" @click="close">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
      </svg>
      <span>通知</span>
      <span v-if="unreadCount > 0" class="ud-unread">{{ unreadLabel }}</span>
    </RouterLink>

    <!-- 个人资料 -->
    <RouterLink to="/admin/profile" class="ud-item" role="menuitem" tabindex="-1" @click="close">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
      个人资料
    </RouterLink>

    <div class="ud-divider" />

    <!-- 退出登录不同于注销账号 -->
    <button class="ud-item ud-item--danger" role="menuitem" tabindex="-1" @click="handleLogout">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
        <polyline points="16 17 21 12 16 7" />
        <line x1="21" y1="12" x2="9" y2="12" />
      </svg>
      退出登录
    </button>
  </div>
</template>

<style scoped>
.user-dropdown {
  width: 272px;
  max-width: calc(100vw - 32px);
  max-height: calc(100dvh - 88px);
  padding: 6px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow:
    0 12px 36px rgb(0 0 0 / 10%),
    0 2px 6px rgb(0 0 0 / 4%);
  overflow-y: auto;
  overscroll-behavior: contain;
}

/* Profile */
.ud-profile {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 14px 12px 16px;
}

.ud-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
}

.ud-avatar--img {
  object-fit: cover;
  display: block;
}

.ud-avatar--placeholder {
  background: var(--color-accent);
  color: var(--color-text-on-accent);
}

.ud-profile-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

/* 需明显大于 .ud-item 的 13px，否则用户名和菜单项同级，顶部这块读不出是标题 */
.ud-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Divider */
.ud-role {
  font-size: 11px;
  color: var(--color-text-muted);
  letter-spacing: 0.04em;
}

.ud-divider {
  height: 1px;
  margin: 5px 10px;
  background: var(--color-border);
}

/* Items */
.ud-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 40px;
  padding: 10px 12px;
  border: 0;
  border-radius: 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
  text-decoration: none;
  background: transparent;
  cursor: pointer;
  transition:
    background-color var(--transition-base),
    color var(--transition-base);
  text-align: left;
}

.ud-item svg {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  color: var(--color-text-faint);
  transition: color var(--transition-base);
}

.ud-item:hover,
.ud-item:focus-visible {
  background-color: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.ud-item:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: -2px;
}

.ud-item:hover svg {
  color: var(--color-text-muted);
}

.ud-unread {
  margin-left: auto;
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 500;
}

.ud-item--danger:hover,
.ud-item--danger:focus-visible {
  background-color: var(--color-danger-bg);
  color: var(--color-danger-on-soft);
}

.ud-item--danger:hover svg {
  color: var(--color-danger);
}
</style>
