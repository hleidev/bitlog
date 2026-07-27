<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import { storeToRefs } from 'pinia'
import UserDropdown from '@/components/common/UserDropdown.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'

defineProps<{ collapsed: boolean; isMobile?: boolean }>()
const emit = defineEmits<{ toggle: [] }>()

const route = useRoute()
const userStore = useUserStore()
const { userInfo } = storeToRefs(userStore)

const menuOpen = ref(false)

const pageTitle = computed(() => route.meta.title ?? '')
const parentTitle = computed(() => route.meta.parent ?? '')
const displayName = computed(() => userInfo.value?.nickname ?? 'Admin')
const avatarLetter = computed(() => displayName.value.charAt(0).toUpperCase())
</script>

<template>
  <div class="admin-header">
    <div class="header-left">
      <!-- Sidebar toggle -->
      <button class="toggle-btn" aria-label="切换侧边栏" @click="emit('toggle')">
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
      <nav v-if="pageTitle" class="breadcrumb">
        <span v-if="parentTitle" class="breadcrumb-item breadcrumb-item--parent">{{
          parentTitle
        }}</span>
        <span v-if="parentTitle" class="breadcrumb-sep">/</span>
        <span class="breadcrumb-item">{{ pageTitle }}</span>
      </nav>
    </div>

    <div class="header-right">
      <ThemeToggle />
      <div class="user-menu" @mouseenter="menuOpen = true" @mouseleave="menuOpen = false">
        <button class="user-trigger">
          <img
            v-if="userInfo?.avatar"
            :src="userInfo.avatar"
            class="trigger-avatar trigger-avatar--img"
            :alt="displayName"
          />
          <span v-else class="trigger-avatar trigger-avatar--placeholder">{{ avatarLetter }}</span>
          <span class="trigger-name">{{ displayName }}</span>
        </button>

        <Transition name="dropdown">
          <UserDropdown v-if="menuOpen" class="admin-user-dropdown" />
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
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 4px;
  border-radius: 4px;
  cursor: pointer;
  background: transparent;
  border: none;
  transition: background 0.15s ease;
}

.user-trigger:hover {
  background: var(--admin-sidebar-hover);
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

.trigger-name {
  font-size: 13.5px;
  color: var(--color-text-secondary);
  font-weight: 500;
  font-family: var(--font-sans, 'Inter', sans-serif);
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

@media (max-width: 768px) {
  .trigger-name {
    display: none;
  }
}
</style>
