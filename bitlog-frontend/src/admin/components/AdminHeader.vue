<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import { storeToRefs } from 'pinia'
import { useDropdown } from '@/composables/useDropdown'
import { useNotificationStore } from '@/stores/useNotificationStore'
import UserDropdown from '@/components/common/UserDropdown.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'

defineProps<{ collapsed: boolean; isMobile?: boolean }>()
const emit = defineEmits<{ toggle: [] }>()

const route = useRoute()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { userInfo } = storeToRefs(userStore)
const { unreadCount } = storeToRefs(notificationStore)
const {
  isOpen: menuOpen,
  containerRef: menuRef,
  triggerRef: menuTriggerRef,
  close: closeMenu,
  toggle: toggleMenu,
} = useDropdown(route)

const pageTitle = computed(() => route.meta.title ?? '')
const parentTitle = computed(() => route.meta.parent ?? '')
const displayName = computed(() => userInfo.value?.username ?? '用户')
const avatarLetter = computed(() => displayName.value.charAt(0).toUpperCase() || '?')
const avatarFailed = ref(false)
watch(
  () => userInfo.value?.avatar,
  () => {
    avatarFailed.value = false
  },
)
</script>

<template>
  <div class="admin-header">
    <div class="header-left">
      <!-- Sidebar toggle -->
      <button
        class="toggle-btn"
        :aria-label="collapsed ? '展开导航' : '收起导航'"
        aria-controls="admin-navigation"
        :aria-expanded="!collapsed"
        @click="emit('toggle')"
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
        >
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <line x1="9" y1="3" x2="9" y2="21" />
        </svg>
      </button>

      <!-- Breadcrumb -->
      <nav v-if="pageTitle" class="breadcrumb" aria-label="当前位置">
        <span v-if="parentTitle" class="breadcrumb-item breadcrumb-item--parent">{{
          parentTitle
        }}</span>
        <span v-if="parentTitle" class="breadcrumb-sep">/</span>
        <span class="breadcrumb-item">{{ pageTitle }}</span>
      </nav>
    </div>

    <div class="header-right">
      <ThemeToggle />
      <div ref="menuRef" class="user-menu">
        <button
          ref="menuTriggerRef"
          class="user-trigger"
          :aria-label="`${displayName}，账号菜单${unreadCount ? `，${unreadCount} 条未读通知` : ''}`"
          aria-haspopup="menu"
          :aria-expanded="menuOpen"
          :aria-controls="menuOpen ? 'admin-account-menu' : undefined"
          @click="toggleMenu"
        >
          <img
            v-if="userInfo?.avatar && !avatarFailed"
            :src="userInfo.avatar"
            class="trigger-avatar trigger-avatar--img"
            :alt="displayName"
            @error="avatarFailed = true"
          />
          <span v-else class="trigger-avatar trigger-avatar--placeholder">{{ avatarLetter }}</span>
          <span v-if="unreadCount > 0" class="trigger-unread-dot" aria-hidden="true"></span>
          <svg
            class="trigger-chevron"
            viewBox="0 0 16 16"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            aria-hidden="true"
          >
            <path d="m4 6 4 4 4-4" />
          </svg>
        </button>

        <Transition name="dropdown">
          <UserDropdown
            v-if="menuOpen"
            id="admin-account-menu"
            class="admin-user-dropdown"
            @close="closeMenu"
          />
        </Transition>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-header {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px 0 12px;
  background: var(--admin-header-bg);
  border-bottom: 1px solid var(--admin-header-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* ── Toggle ── */

.toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  color: var(--color-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  flex-shrink: 0;
  transition:
    color 0.15s ease,
    background 0.15s ease;
}

.toggle-btn svg {
  width: 18px;
  height: 18px;
  display: block;
}

.toggle-btn:hover {
  color: var(--admin-accent-dark);
  background: var(--admin-sidebar-hover);
}

/* ── Breadcrumb ── */

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
}

.breadcrumb-item {
  font-size: 13px;
  color: var(--color-text-primary);
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
}

.breadcrumb-item--parent {
  color: var(--color-text-muted);
  font-weight: 400;
}

.breadcrumb-sep {
  font-size: 13px;
  color: var(--color-text-faint);
  user-select: none;
}

/* ── User menu ── */

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-menu {
  position: relative;
}

.user-trigger {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 40px;
  padding: 5px 6px;
  gap: 5px;
  border-radius: 4px;
  cursor: pointer;
  background: transparent;
  border: none;
  transition: background 0.15s ease;
}

.user-trigger:hover,
.user-trigger[aria-expanded='true'] {
  background: var(--admin-sidebar-hover);
}

.user-trigger:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.trigger-chevron {
  width: 12px;
  height: 12px;
  color: var(--color-text-muted);
  transition: transform 0.15s ease;
}

.user-trigger[aria-expanded='true'] .trigger-chevron {
  transform: rotate(180deg);
}

.trigger-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.trigger-avatar--img {
  object-fit: cover;
}

.trigger-avatar--placeholder {
  background: var(--admin-accent);
  color: var(--admin-text-on-accent);
  font-size: 12px;
  font-weight: 600;
}

.trigger-unread-dot {
  position: absolute;
  top: 2px;
  right: 22px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-accent);
  box-shadow: 0 0 0 2px var(--admin-header-bg);
}

.admin-user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 1001;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
