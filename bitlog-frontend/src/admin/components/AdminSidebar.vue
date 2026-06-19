<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import { storeToRefs } from 'pinia'

const props = defineProps<{ collapsed: boolean }>()
const route = useRoute()
const router = useRouter()
const { isAdmin } = storeToRefs(useUserStore())

// ── Inline SVG paths (24×24 viewBox) ─────────────────────────────────────────

const ICONS: Record<string, string> = {
  dashboard: 'M3 3h8v8H3V3zm0 10h8v8H3v-8zm10-10h8v8h-8V3zm0 10h8v8h-8v-8z',
  articles:
    'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zM13 9V3.5L18.5 9H13zM8 13h8v1.5H8V13zm0 3h8v1.5H8V16zm0-6h3v1.5H8V10z',
  folder: 'M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z',
  tag: 'M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z',
  chat: 'M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z',
  users:
    'M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z',
  user: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z',
  list: 'M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z',
  chevron: 'M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z',
}

// ── Menu definition ───────────────────────────────────────────────────────────

type LeafItem = { title: string; icon: string; path: string }
type GroupItem = {
  title: string
  icon: string
  key: string
  defaultPath?: string
  children: LeafItem[]
}
type MenuItem = LeafItem | GroupItem

const isGroup = (item: MenuItem): item is GroupItem => 'children' in item

const adminMenus: MenuItem[] = [
  { title: '仪表盘', icon: 'dashboard', path: '/admin/dashboard' },
  {
    title: '文章',
    icon: 'articles',
    key: 'articles',
    defaultPath: '/admin/articles',
    children: [
      { title: '文章管理', icon: 'list', path: '/admin/articles' },
      { title: '分类管理', icon: 'folder', path: '/admin/categories' },
      { title: '标签管理', icon: 'tag', path: '/admin/tags' },
    ],
  },
  { title: '评论', icon: 'chat', path: '/admin/comments' },
  {
    title: '用户',
    icon: 'users',
    key: 'users',
    defaultPath: '/admin/users',
    children: [
      { title: '用户管理', icon: 'users', path: '/admin/users' },
      { title: '个人资料', icon: 'user', path: '/admin/profile' },
    ],
  },
]

const userMenus: MenuItem[] = [
  { title: '仪表盘', icon: 'dashboard', path: '/admin/dashboard' },
  { title: '个人资料', icon: 'user', path: '/admin/profile' },
]

const menus = computed<MenuItem[]>(() => (isAdmin.value ? adminMenus : userMenus))

// ── Sub-menu open state ───────────────────────────────────────────────────────

const openGroups = ref<Set<string>>(new Set())

// Per-trigger popover position. Popover is teleported to <body> with
// position:fixed so it escapes AdminLayout's overflow/isolation ancestors.
// A short close-delay (250ms) gives the cursor time to bridge the gap
// between the trigger and the popover without the popover dismissing.
const popoverPos = ref<{ top: number; left: number; key: string } | null>(null)
let closeTimer: ReturnType<typeof setTimeout> | null = null

function cancelClose() {
  if (closeTimer) {
    clearTimeout(closeTimer)
    closeTimer = null
  }
}

function positionPopover(key: string, triggerEl: HTMLElement) {
  cancelClose()
  const r = triggerEl.getBoundingClientRect()
  popoverPos.value = { top: r.top, left: r.right + 6, key }
}

function clearPopover() {
  cancelClose()
  closeTimer = setTimeout(() => {
    popoverPos.value = null
    closeTimer = null
  }, 250)
}

onBeforeUnmount(() => {
  cancelClose()
  popoverPos.value = null
})

function syncOpenGroups() {
  for (const item of menus.value) {
    if (isGroup(item) && item.children.some((c) => route.path.startsWith(c.path))) {
      openGroups.value.add(item.key)
    }
  }
}

syncOpenGroups()
watch(() => route.path, syncOpenGroups)

function toggleGroup(item: GroupItem) {
  if (props.collapsed) {
    router.push(item.children[0].path)
    return
  }
  if (openGroups.value.has(item.key)) {
    openGroups.value.delete(item.key)
  } else {
    openGroups.value.add(item.key)
  }
}

function isGroupActive(item: GroupItem): boolean {
  return item.children.some((c) => route.path === c.path)
}
</script>

<template>
  <div class="sidebar" :class="{ 'sidebar--collapsed': collapsed }">
    <!-- Logo -->
    <a href="/" target="_blank" class="sidebar-logo">
      <img src="@/assets/images/logo.png" class="logo-img" alt="Bitlog" />
      <span v-show="!collapsed" class="logo-text">Bitlog</span>
    </a>

    <!-- Nav -->
    <nav class="sidebar-nav">
      <template v-for="item in menus" :key="isGroup(item) ? item.key : item.path">
        <!-- Group -->
        <div v-if="isGroup(item)" class="nav-group">
          <!-- Collapsed: click → first child, hover/focus → popover (position:fixed, JS-synced) -->
          <div
            v-if="collapsed"
            class="nav-group-popover-wrap"
            @mouseenter="(e) => positionPopover(item.key, e.currentTarget.querySelector('button'))"
            @mouseleave="clearPopover"
          >
            <button
              class="nav-item nav-item--group nav-group-trigger"
              :class="{ 'nav-item--active': isGroupActive(item) }"
              :aria-haspopup="'menu'"
              :aria-expanded="popoverPos?.key === item.key"
              @click="toggleGroup(item)"
            >
              <svg class="nav-icon" viewBox="0 0 24 24" fill="currentColor">
                <path :d="ICONS[item.icon]" />
              </svg>
            </button>
            <Teleport v-if="popoverPos?.key === item.key" to="body">
              <div
                class="sub-menu-popover"
                role="menu"
                :style="{ top: popoverPos.top + 'px', left: popoverPos.left + 'px' }"
                @mouseenter="cancelClose"
                @mouseleave="clearPopover"
              >
                <RouterLink
                  v-for="child in item.children"
                  :key="child.path"
                  :to="child.path"
                  class="nav-item nav-item--child"
                  :class="{ 'nav-item--active': route.path === child.path }"
                  role="menuitem"
                >
                  <svg class="nav-icon nav-icon--small" viewBox="0 0 24 24" fill="currentColor">
                    <path :d="ICONS[child.icon]" />
                  </svg>
                  <span class="nav-label">{{ child.title }}</span>
                </RouterLink>
              </div>
            </Teleport>
          </div>

          <!-- Expanded: normal behavior -->
          <template v-else>
            <button
              class="nav-item nav-item--group"
              :class="{ 'nav-item--active': isGroupActive(item) }"
              @click="toggleGroup(item)"
            >
              <svg class="nav-icon" viewBox="0 0 24 24" fill="currentColor">
                <path :d="ICONS[item.icon]" />
              </svg>
              <span class="nav-label">{{ item.title }}</span>
              <svg
                class="nav-chevron"
                :class="{ 'nav-chevron--open': openGroups.has(item.key) }"
                viewBox="0 0 24 24"
                fill="currentColor"
              >
                <path :d="ICONS.chevron" />
              </svg>
            </button>

            <!-- Sub-items -->
            <div
              class="sub-items-wrap"
              :class="{ 'sub-items-wrap--open': openGroups.has(item.key) }"
            >
              <div class="sub-items-inner">
                <RouterLink
                  v-for="child in item.children"
                  :key="child.path"
                  :to="child.path"
                  class="nav-item nav-item--child"
                  :class="{ 'nav-item--active': route.path === child.path }"
                >
                  <svg class="nav-icon nav-icon--small" viewBox="0 0 24 24" fill="currentColor">
                    <path :d="ICONS[child.icon]" />
                  </svg>
                  <span class="nav-label">{{ child.title }}</span>
                </RouterLink>
              </div>
            </div>
          </template>
        </div>

        <!-- Leaf -->
        <RouterLink
          v-else
          :to="item.path"
          class="nav-item"
          :class="{ 'nav-item--active': route.path === item.path }"
        >
          <svg class="nav-icon" viewBox="0 0 24 24" fill="currentColor">
            <path :d="ICONS[item.icon]" />
          </svg>
          <span v-show="!collapsed" class="nav-label">{{ item.title }}</span>
        </RouterLink>
      </template>
    </nav>
  </div>
</template>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--admin-sidebar-bg);
  overflow: hidden;
}

/* ── Logo ── */

.sidebar-logo {
  height: var(--admin-header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-shrink: 0;
  padding: 0 16px;
  border-bottom: 1px solid var(--admin-sidebar-border);
  text-decoration: none;
  overflow: hidden;
  white-space: nowrap;
  transition: opacity var(--transition-base, 0.2s ease);
}

.sidebar-logo:hover {
  opacity: 0.75;
}

.logo-img {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.logo-text {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 17px;
  font-weight: 600;
  color: var(--admin-sidebar-text);
  letter-spacing: 0.01em;
}

/* ── Nav ── */

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
  scrollbar-width: none;
}

.sidebar-nav::-webkit-scrollbar {
  display: none;
}

/* ── Nav item (shared) ── */

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 0 16px;
  height: 42px;
  font-size: 13.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  cursor: pointer;
  background: transparent;
  border: none;
  border-left: 2px solid transparent;
  transition:
    background 0.15s ease,
    color 0.15s ease,
    border-color 0.15s ease;
}

.nav-item:hover {
  background: var(--admin-sidebar-hover);
  color: var(--color-text-primary, #1a1610);
}

.nav-item--active {
  background: var(--admin-sidebar-active-bg);
  color: var(--admin-sidebar-text-active);
  border-left-color: var(--admin-sidebar-text-active);
  font-weight: 500;
}

.nav-item--active:hover {
  color: var(--admin-sidebar-text-active);
}

/* ── Icons ── */

.nav-icon {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
  opacity: 0.7;
}

.nav-item--active .nav-icon,
.nav-item:hover .nav-icon {
  opacity: 1;
}

.nav-icon--small {
  width: 14px;
  height: 14px;
  opacity: 0.5;
}

.nav-item--active .nav-icon--small {
  opacity: 0.9;
}

/* ── Group ── */

.nav-item--group {
  text-align: left;
}

.nav-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-chevron {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  opacity: 0.4;
  transition: transform 0.22s ease;
}

.nav-chevron--open {
  transform: rotate(180deg);
}

/* ── Sub-items (height animation via grid) ── */

.sub-items-wrap {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.22s ease;
  overflow: hidden;
}

.sub-items-wrap--open {
  grid-template-rows: 1fr;
}

.sub-items-inner {
  min-height: 0;
  overflow: hidden;
}

.nav-item--child {
  height: 38px;
  padding-left: 40px;
  font-size: 13px;
  color: var(--admin-sidebar-text-muted);
}

.nav-item--child.nav-item--active {
  color: var(--admin-sidebar-text-active);
}

/* ── Collapsed popover ── */
/* Teleported to <body> with position:fixed so it escapes AdminLayout's
   overflow/isolation ancestors. Position is JS-synced on hover/focus
   from the trigger's bounding rect. */

.sub-menu-popover {
  position: fixed;
  min-width: 140px;
  background: var(--admin-sidebar-bg);
  border: 1px solid var(--admin-sidebar-border);
  border-radius: var(--admin-radius);
  padding: 4px;
  z-index: 1100;
  display: flex;
  flex-direction: column;
  gap: 2px;
  box-shadow: 0 2px 8px var(--admin-overlay);
  /* Slide-in feel */
  animation: popoverIn 0.12s ease-out;
}

@keyframes popoverIn {
  from {
    opacity: 0;
    transform: translateX(-4px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.sub-menu-popover .nav-item--child {
  padding-left: 12px;
  height: 34px;
  gap: 6px;
}

.sub-menu-popover .nav-icon--small {
  width: 12px;
  height: 12px;
}

.sub-menu-popover .nav-label {
  font-size: 13px;
}

/* ── Collapsed state ── */

.sidebar--collapsed .nav-item {
  justify-content: center;
  padding: 0;
}

.sidebar--collapsed .sidebar-logo {
  padding: 0;
}
</style>
