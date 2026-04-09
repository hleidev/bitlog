<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Odometer,
  Document,
  EditPen,
  Folder,
  CollectionTag,
  ChatLineRound,
  User,
  UserFilled,
  Plus,
  Avatar,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/useUserStore'
import { storeToRefs } from 'pinia'

defineProps<{ collapsed: boolean }>()

const route = useRoute()
const router = useRouter()
const { isAdmin } = storeToRefs(useUserStore())

type LeafItem = { title: string; icon: unknown; path: string }
type GroupItem = { title: string; icon: unknown; key: string; defaultPath?: string; children: LeafItem[] }
type MenuItem = LeafItem | GroupItem

const isGroup = (item: MenuItem): item is GroupItem => 'children' in item

// Admin 完整菜单
const adminMenus: MenuItem[] = [
  { title: '仪表盘', icon: Odometer, path: '/admin/dashboard' },
  {
    title: '文章',
    icon: Document,
    key: 'articles',
    defaultPath: '/admin/articles',
    children: [
      { title: '所有文章', icon: Document, path: '/admin/articles' },
      { title: '写文章', icon: EditPen, path: '/admin/write' },
      { title: '分类', icon: Folder, path: '/admin/categories' },
      { title: '标签', icon: CollectionTag, path: '/admin/tags' },
    ],
  },
  { title: '评论', icon: ChatLineRound, path: '/admin/comments' },
  {
    title: '用户',
    icon: User,
    key: 'users',
    defaultPath: '/admin/users',
    children: [
      { title: '所有用户', icon: UserFilled, path: '/admin/users' },
      { title: '添加用户', icon: Plus, path: '/admin/users/add' },
      { title: '个人资料', icon: Avatar, path: '/admin/profile' },
    ],
  },
]

// 普通用户菜单
const userMenus: MenuItem[] = [
  { title: '仪表盘', icon: Odometer, path: '/admin/dashboard' },
  { title: '个人资料', icon: Avatar, path: '/admin/profile' },
]

const menus = computed<MenuItem[]>(() => (isAdmin.value ? adminMenus : userMenus))
</script>

<template>
  <div class="sidebar-wrap">
    <a href="/" target="_blank" class="sidebar-logo" :class="{ collapsed }">
      <img src="@/assets/images/logo.jpeg" class="logo-mark" alt="Bitlog Logo" />
      <span v-show="!collapsed" class="logo-full">Bitlog</span>
    </a>

    <el-menu
      :default-active="route.path"
      :collapse="collapsed"
      :collapse-transition="false"
      router
      class="sidebar-menu"
    >
      <template v-for="item in menus" :key="isGroup(item) ? item.key : item.path">
        <!-- 有子菜单 -->
        <el-sub-menu v-if="isGroup(item)" :index="item.key">
          <template #title>
            <div
              class="sub-menu-title"
              @click.stop="item.defaultPath && router.push(item.defaultPath)"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </div>
          </template>
          <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
            <el-icon><component :is="child.icon" /></el-icon>
            <span>{{ child.title }}</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 直接叶子项 -->
        <el-menu-item v-else :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<style scoped>
.sidebar-wrap {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--admin-sidebar-bg);
  overflow: hidden;
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  overflow: hidden;
  white-space: nowrap;
  text-decoration: none;
  cursor: pointer;
  transition: opacity 0.2s;
}

.sidebar-logo:hover {
  opacity: 0.8;
}

.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  flex-shrink: 0;
}

.logo-full {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
}

.sidebar-menu {
  flex: 1;
  border-right: none !important;
  --el-menu-bg-color: var(--admin-sidebar-bg);
  --el-menu-text-color: var(--admin-sidebar-text);
  --el-menu-hover-bg-color: var(--admin-sidebar-hover);
  --el-menu-active-color: var(--admin-sidebar-text-active);
  --el-menu-item-height: 48px;
  --el-menu-sub-item-height: 42px;
  --el-menu-icon-width: 20px;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: var(--admin-sidebar-active) !important;
  color: var(--admin-sidebar-text-active) !important;
}

.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: var(--admin-sidebar-hover) !important;
}

.sidebar-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title),
.sidebar-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--admin-sidebar-text-active) !important;
}

.sidebar-menu :deep(.el-menu--popup) {
  background-color: var(--admin-sidebar-popup-bg) !important;
}

.sidebar-menu :deep(.el-menu--popup .el-menu-item:hover) {
  background-color: var(--admin-sidebar-hover) !important;
}

.sidebar-menu :deep(.el-menu--popup .el-menu-item.is-active) {
  background-color: var(--admin-sidebar-active) !important;
  color: var(--admin-sidebar-text-active) !important;
}

.sub-menu-title {
  display: flex;
  align-items: center;
  gap: 5px;
  flex: 1;
  min-width: 0;
}

.sidebar-menu :deep(.el-sub-menu__icon-arrow) {
  color: rgba(255, 255, 255, 0.35);
}
</style>
