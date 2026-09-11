<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/useUserStore'
import AdminIcon from './AdminIcon.vue'

defineProps<{ collapsed: boolean }>()
const route = useRoute()
const { isAdmin } = storeToRefs(useUserStore())
const groups = computed(() =>
  isAdmin.value
    ? [
        {
          label: '创作',
          items: [
            { title: '工作台', icon: 'workspace' as const, path: '/admin/dashboard' },
            { title: '文章', icon: 'article' as const, path: '/admin/articles' },
          ],
        },
        {
          label: '交流',
          items: [
            { title: '评论', icon: 'comment' as const, path: '/admin/comments' },
            { title: '友链', icon: 'link' as const, path: '/admin/links' },
          ],
        },
        {
          label: '管理',
          items: [
            { title: '分类', icon: 'category' as const, path: '/admin/categories' },
            { title: '标签', icon: 'tag' as const, path: '/admin/tags' },
            { title: '用户', icon: 'user' as const, path: '/admin/users' },
            { title: '个人资料', icon: 'profile' as const, path: '/admin/profile' },
          ],
        },
      ]
    : [
        {
          label: '个人',
          items: [
            { title: '工作台', icon: 'workspace' as const, path: '/admin/dashboard' },
            { title: '个人资料', icon: 'profile' as const, path: '/admin/profile' },
          ],
        },
      ],
)

function isActive(path: string) {
  if (path === '/admin/articles' && route.path.startsWith('/admin/write')) return true
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': collapsed }" aria-label="后台导航">
    <RouterLink to="/admin/dashboard" class="sidebar-brand" aria-label="BitLog 工作台">
      <span class="brand-letter" aria-hidden="true">b.</span>
      <span v-if="!collapsed" class="brand-name">BitLog<span>工作台</span></span>
    </RouterLink>
    <RouterLink
      v-if="isAdmin"
      to="/admin/write"
      class="compose-link"
      :title="collapsed ? '写文章' : undefined"
      aria-label="写文章"
    >
      <AdminIcon name="plus" /><span v-if="!collapsed">写文章</span>
    </RouterLink>
    <nav class="sidebar-nav">
      <div v-for="group in groups" :key="group.label" class="nav-group">
        <span v-if="!collapsed" class="group-label">{{ group.label }}</span>
        <RouterLink
          v-for="item in group.items"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ 'is-active': isActive(item.path) }"
          :title="collapsed ? item.title : undefined"
          :aria-label="item.title"
          :aria-current="isActive(item.path) ? 'page' : undefined"
        >
          <AdminIcon :name="item.icon" /><span v-if="!collapsed">{{ item.title }}</span>
          <span v-if="!collapsed && isActive(item.path)" class="active-dot" aria-hidden="true" />
        </RouterLink>
      </div>
    </nav>
    <a
      href="/"
      target="_blank"
      rel="noopener"
      class="site-link"
      :title="collapsed ? '查看博客（新窗口）' : undefined"
      aria-label="查看博客（新窗口）"
    >
      <AdminIcon name="external" /><span v-if="!collapsed">查看博客</span>
    </a>
  </aside>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--admin-sidebar-bg);
  padding: 0 18px 20px;
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 94px;
  color: var(--admin-text-primary);
  text-decoration: none;
}
.brand-letter {
  font-family: var(--font-editorial);
  font-size: 42px;
  line-height: 1;
  letter-spacing: -4px;
  color: var(--admin-accent);
  padding-right: 4px;
}
.brand-name {
  font-family: var(--font-editorial);
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.8px;
}
.brand-name span {
  display: block;
  margin-top: 2px;
  font-family: var(--font-sans);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.16em;
  color: var(--admin-text-muted);
}
.compose-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 40px;
  margin-bottom: 24px;
  border: 1px solid var(--admin-accent);
  border-radius: 6px;
  background: var(--admin-accent);
  color: var(--admin-text-on-accent);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}
.compose-link:hover {
  background: var(--admin-accent-dark);
  border-color: var(--admin-accent-dark);
}
svg {
  width: 17px;
  height: 17px;
  flex: none;
  stroke-width: 1.6;
}
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
.nav-group + .nav-group {
  margin-top: 24px;
}
.group-label {
  display: block;
  padding: 0 11px 8px;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--admin-text-muted);
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 39px;
  padding: 8px 11px;
  margin: 2px 0;
  border-radius: 5px;
  color: var(--admin-sidebar-text);
  font-size: 13px;
  text-decoration: none;
  transition:
    background 0.15s,
    color 0.15s;
}
.nav-item:hover {
  background: var(--admin-sidebar-hover);
  color: var(--admin-text-primary);
}
.nav-item.is-active {
  background: var(--admin-accent-bg-soft);
  color: var(--admin-accent-dark);
  font-weight: 600;
}
.active-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  margin-left: auto;
}
.site-link {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
  margin-top: 24px;
  padding: 12px 11px 0;
  border-top: 1px solid var(--admin-border);
  font-size: 12px;
  color: var(--admin-text-muted);
  text-decoration: none;
}
.site-link:hover {
  color: var(--admin-accent);
}
.sidebar--collapsed {
  padding-inline: 12px;
}
.sidebar--collapsed .sidebar-brand {
  justify-content: center;
}
.sidebar--collapsed .nav-item,
.sidebar--collapsed .site-link {
  justify-content: center;
  padding-inline: 0;
}
.sidebar--collapsed .nav-group + .nav-group {
  padding-top: 14px;
  margin-top: 14px;
  border-top: 1px solid var(--admin-border-soft);
}
.sidebar--collapsed .brand-letter {
  font-size: 36px;
}
</style>
