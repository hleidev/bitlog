<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/useUserStore'
import { storeToRefs } from 'pinia'

withDefaults(defineProps<{ showAdminLinks?: boolean }>(), {
  showAdminLinks: false,
})

const emit = defineEmits<{ close: [] }>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { userInfo, isAdmin } = storeToRefs(userStore)
const confirm = useConfirm()

const displayName = computed(() => userInfo.value?.username ?? '')
const avatarLetter = computed(() => displayName.value.charAt(0).toUpperCase() || '?')

function close() {
  emit('close')
}

async function handleLogout() {
  close()
  try {
    await confirm('确认注销登录？', '注销', { confirmText: '注销', cancelText: '取消' })
  } catch {
    return
  }
  await userStore.logout()
  router.push(route.path.startsWith('/admin') ? '/admin/login' : '/')
}
</script>

<template>
  <div class="user-dropdown">
    <!-- 顶部：头像 + 用户名 -->
    <div class="ud-profile">
      <img
        v-if="userInfo?.avatar"
        :src="userInfo.avatar"
        class="ud-avatar ud-avatar--img"
        :alt="displayName"
      />
      <span v-else class="ud-avatar ud-avatar--placeholder">{{ avatarLetter }}</span>
      <div class="ud-profile-info">
        <span class="ud-name">{{ displayName }}</span>
      </div>
    </div>

    <div class="ud-divider" />

    <!-- Admin 专属（仅前台展示，admin 后台本身不需要） -->
    <template v-if="showAdminLinks && isAdmin">
      <RouterLink to="/admin/dashboard" class="ud-item" @click="close">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="3" y="3" width="7" height="7" rx="1" />
          <rect x="14" y="3" width="7" height="7" rx="1" />
          <rect x="3" y="14" width="7" height="7" rx="1" />
          <rect x="14" y="14" width="7" height="7" rx="1" />
        </svg>
        后台管理
      </RouterLink>
      <RouterLink to="/admin/articles" class="ud-item" @click="close">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <polyline points="14 2 14 8 20 8" />
          <line x1="16" y1="13" x2="8" y2="13" />
          <line x1="16" y1="17" x2="8" y2="17" />
        </svg>
        文章管理
      </RouterLink>
      <div class="ud-divider" />
    </template>

    <!-- 个人资料 -->
    <RouterLink to="/admin/profile" class="ud-item" @click="close">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
      个人资料
    </RouterLink>

    <div class="ud-divider" />

    <!-- 注销 -->
    <button class="ud-item ud-item--danger" @click="handleLogout">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
        <polyline points="16 17 21 12 16 7" />
        <line x1="21" y1="12" x2="9" y2="12" />
      </svg>
      注销
    </button>
  </div>
</template>

<style scoped>
.user-dropdown {
  width: 240px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}

/* Profile */
.ud-profile {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 14px 16px;
}

.ud-avatar {
  width: 38px;
  height: 38px;
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
  gap: 2px;
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
.ud-divider {
  height: 1px;
  background: var(--color-border);
}

/* Items */
.ud-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 16px;
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

.ud-item:hover {
  background-color: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.ud-item:hover svg {
  color: var(--color-text-muted);
}

.ud-item--danger:hover {
  background-color: var(--color-danger-bg);
  color: var(--color-danger-on-soft);
}

.ud-item--danger:hover svg {
  color: var(--color-danger);
}
</style>
