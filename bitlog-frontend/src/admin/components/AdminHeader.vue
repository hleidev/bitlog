<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Fold, Expand } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/useUserStore'
import { storeToRefs } from 'pinia'
import UserDropdown from '@/components/common/UserDropdown.vue'

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
      <el-button
        text
        :icon="collapsed ? Expand : Fold"
        class="toggle-btn"
        @click="emit('toggle')"
      />
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-if="parentTitle">{{ parentTitle }}</el-breadcrumb-item>
        <el-breadcrumb-item v-if="pageTitle">{{ pageTitle }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="header-right">
      <div class="user-menu" @mouseenter="menuOpen = true" @mouseleave="menuOpen = false">
        <button class="user-trigger">
          <img
            v-if="userInfo?.avatar"
            :src="userInfo.avatar"
            class="trigger-avatar trigger-avatar--img"
            :alt="displayName"
          />
          <span v-else class="trigger-avatar trigger-avatar--placeholder">{{ avatarLetter }}</span>
          <span class="trigger-name trigger-name--desktop">{{ displayName }}</span>
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
  padding: 0 20px 0 4px;
  background: var(--admin-header-bg);
  border-bottom: 1px solid var(--admin-header-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toggle-btn {
  font-size: 18px;
  color: #595959;
  padding: 8px;
}

.toggle-btn:hover {
  color: var(--admin-accent);
  background-color: #f5f5f5;
}

/* User menu */
.user-menu {
  position: relative;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  background: transparent;
}

.user-trigger:hover {
  background-color: #f5f5f5;
}

.trigger-avatar {
  width: 30px;
  height: 30px;
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
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.trigger-name {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

@media (max-width: 768px) {
  .trigger-name--desktop {
    display: none;
  }
}

/* Dropdown */
.admin-user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 1001;
}

/* Transition */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
