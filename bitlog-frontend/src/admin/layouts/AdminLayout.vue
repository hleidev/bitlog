<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useHead } from '@unhead/vue'
import AdminSidebar from '@/admin/components/AdminSidebar.vue'
import AdminHeader from '@/admin/components/AdminHeader.vue'
import AdminToast from '@/admin/components/AdminToast.vue'
import AdminConfirmDialog from '@/admin/components/AdminConfirmDialog.vue'
import '@/admin/styles/variables.css'
import '@/admin/styles/workspace.css'

const MOBILE_BP = 768

const collapsed = ref(localStorage.getItem('admin-sidebar-collapsed') === 'true')
const drawerOpen = ref(false)
const windowWidth = ref(window.innerWidth)
const mainEl = ref<HTMLElement | null>(null)
const asideEl = ref<HTMLElement | null>(null)
const showBackTop = ref(false)

const isMobile = computed(() => windowWidth.value <= MOBILE_BP)

function onResize() {
  windowWidth.value = window.innerWidth
  if (!isMobile.value) drawerOpen.value = false
}

function onMainScroll() {
  if (!mainEl.value) return
  showBackTop.value = mainEl.value.scrollTop > 400
}

function scrollToTop() {
  mainEl.value?.scrollTo({
    top: 0,
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
  })
}

let drawerTrigger: HTMLElement | null = null
watch(
  drawerOpen,
  (open) => {
    if (open) {
      drawerTrigger = document.querySelector<HTMLElement>('.admin-header .toggle-btn')
      asideEl.value?.querySelector<HTMLElement>('a, button')?.focus()
    } else if (drawerTrigger?.isConnected) {
      drawerTrigger.focus()
      drawerTrigger = null
    }
  },
  { flush: 'post' },
)

function onDrawerKeydown(event: KeyboardEvent) {
  if (!isMobile.value || !drawerOpen.value) return
  if (event.key === 'Escape') {
    event.preventDefault()
    drawerOpen.value = false
  }
  if (event.key !== 'Tab') return
  const items = asideEl.value?.querySelectorAll<HTMLElement>('a[href], button:not([disabled])')
  if (!items?.length) return
  const first = items[0]!
  const last = items[items.length - 1]!
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

function handleToggle() {
  if (isMobile.value) {
    drawerOpen.value = !drawerOpen.value
  } else {
    collapsed.value = !collapsed.value
    localStorage.setItem('admin-sidebar-collapsed', String(collapsed.value))
  }
}

const route = useRoute()
useHead({ title: computed(() => `${route.meta.title || '后台'} | BitLog`) })
const isWriting = computed(() => route.path.startsWith('/admin/write'))
const showPageHeading = computed(() => !isWriting.value && route.path !== '/admin/dashboard')
watch(
  () => route.path,
  () => {
    if (isMobile.value) drawerOpen.value = false
    showBackTop.value = false
    mainEl.value?.scrollTo({ top: 0 })
  },
  { flush: 'post' },
)
</script>

<template>
  <div class="admin-layout" @keydown="onDrawerKeydown">
    <!-- Mobile backdrop -->
    <Transition name="backdrop">
      <div v-if="isMobile && drawerOpen" class="admin-backdrop" @click="drawerOpen = false" />
    </Transition>

    <!-- Sidebar -->
    <div
      id="admin-navigation"
      ref="asideEl"
      class="admin-aside"
      :inert="isMobile && !drawerOpen"
      :role="isMobile && drawerOpen ? 'dialog' : undefined"
      :aria-modal="isMobile && drawerOpen ? true : undefined"
      :aria-label="isMobile ? '后台导航' : undefined"
      :class="{
        'is-collapsed': !isMobile && collapsed,
        'is-mobile': isMobile,
        'drawer-open': isMobile && drawerOpen,
      }"
    >
      <button
        v-if="isMobile"
        class="drawer-close"
        aria-label="关闭导航"
        @click="drawerOpen = false"
      >
        ×
      </button>
      <AdminSidebar :collapsed="isMobile ? false : collapsed" />
    </div>

    <!-- Main area -->
    <div class="admin-main-container" :inert="isMobile && drawerOpen">
      <div class="admin-header-wrap">
        <AdminHeader
          :collapsed="isMobile ? !drawerOpen : collapsed"
          :is-mobile="isMobile"
          @toggle="handleToggle"
        />
      </div>
      <main
        ref="mainEl"
        class="admin-main"
        :class="{ 'admin-main--writing': isWriting }"
        @scroll="onMainScroll"
      >
        <div class="admin-content" :class="{ 'admin-content--writing': isWriting }">
          <header v-if="showPageHeading" class="workspace-page-heading">
            <h1>{{ route.meta.title }}</h1>
          </header>
          <RouterView />
        </div>
      </main>
    </div>

    <AdminToast />
    <AdminConfirmDialog />

    <Transition name="back-top">
      <button
        v-if="showBackTop"
        class="back-top"
        type="button"
        aria-label="回到顶部"
        @click="scrollToTop"
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="12" y1="19" x2="12" y2="5" />
          <polyline points="5 12 12 5 19 12" />
        </svg>
      </button>
    </Transition>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: var(--admin-body-bg);
}

.admin-aside {
  width: var(--admin-sidebar-width);
  flex-shrink: 0;
  transition: width 0.22s ease;
  /* No overflow:hidden here — the collapsed sidebar must let the sub-menu
     popover (teleported to <body>) escape naturally. */
  border-right: 1px solid var(--admin-sidebar-border);
}

.admin-aside.is-collapsed {
  width: var(--admin-sidebar-width-collapsed);
}

.admin-aside.is-mobile {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  height: 100dvh;
  width: var(--admin-sidebar-width) !important;
  z-index: 1001;
  transform: translateX(-100%);
  transition: transform 0.25s ease;
}

.admin-aside.is-mobile.drawer-open {
  transform: translateX(0);
}

.admin-backdrop {
  position: fixed;
  inset: 0;
  background: var(--admin-overlay);
  z-index: 1000;
}

.backdrop-enter-active,
.backdrop-leave-active {
  transition: opacity 0.25s ease;
}

.backdrop-enter-from,
.backdrop-leave-to {
  opacity: 0;
}

.admin-main-container {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-header-wrap {
  height: var(--admin-header-height);
  flex-shrink: 0;
}

.admin-main {
  flex: 1;
  overflow-y: auto;
  padding: 36px 40px 48px;
  background: var(--admin-body-bg);
}
.admin-content {
  max-width: 1360px;
  margin: 0 auto;
}
.admin-main--writing {
  padding: 0 !important;
  overflow: hidden;
}
.admin-content--writing {
  max-width: none;
  height: 100%;
}
.drawer-close {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border: 0;
  background: transparent;
  color: var(--admin-text-muted);
  font-size: 24px;
  cursor: pointer;
}

@media (max-width: 768px) {
  .admin-main {
    padding: 24px 16px 32px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .admin-aside,
  .admin-aside.is-mobile,
  .backdrop-enter-active,
  .backdrop-leave-active,
  .back-top-enter-active,
  .back-top-leave-active {
    transition: none;
  }
}

.back-top {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--admin-surface);
  border: 1px solid var(--admin-border);
  border-radius: 4px;
  color: var(--admin-text-muted);
  cursor: pointer;
  z-index: 200;
  transition:
    color 0.15s,
    border-color 0.15s,
    background 0.15s;
}
.back-top svg {
  width: 15px;
  height: 15px;
  display: block;
}
.back-top:hover {
  color: var(--admin-accent);
  border-color: var(--admin-accent);
}

.back-top-enter-active,
.back-top-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}
.back-top-enter-from,
.back-top-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 768px) {
  .back-top {
    bottom: 20px;
    right: 16px;
  }
}
</style>
