<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import AdminSidebar from '@/admin/components/AdminSidebar.vue'
import AdminHeader from '@/admin/components/AdminHeader.vue'
import AdminToast from '@/admin/components/AdminToast.vue'
import AdminConfirmDialog from '@/admin/components/AdminConfirmDialog.vue'
import '@/admin/styles/variables.css'

const MOBILE_BP = 768

const collapsed = ref(localStorage.getItem('admin-sidebar-collapsed') === 'true')
const drawerOpen = ref(false)
const windowWidth = ref(window.innerWidth)
const mainEl = ref<HTMLElement | null>(null)
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
  mainEl.value?.scrollTo({ top: 0, behavior: 'smooth' })
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
watch(() => route.path, () => {
  if (isMobile.value) drawerOpen.value = false
  showBackTop.value = false
})
</script>

<template>
  <div class="admin-layout">
    <!-- Mobile backdrop -->
    <Transition name="backdrop">
      <div
        v-if="isMobile && drawerOpen"
        class="admin-backdrop"
        @click="drawerOpen = false"
      />
    </Transition>

    <!-- Sidebar -->
    <div
      class="admin-aside"
      :class="{ 'is-collapsed': !isMobile && collapsed, 'is-mobile': isMobile, 'drawer-open': isMobile && drawerOpen }"
    >
      <AdminSidebar :collapsed="isMobile ? false : collapsed" />
    </div>

    <!-- Main area -->
    <div class="admin-main-container">
      <div class="admin-header-wrap">
        <AdminHeader
          :collapsed="isMobile ? !drawerOpen : collapsed"
          :is-mobile="isMobile"
          @toggle="handleToggle"
        />
      </div>
      <div ref="mainEl" class="admin-main" @scroll="onMainScroll">
        <RouterView />
      </div>
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
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/>
        </svg>
      </button>
    </Transition>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
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
  background: rgba(0, 0, 0, 0.3);
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
  padding: 24px;
  background: var(--admin-body-bg);
}

@media (max-width: 768px) {
  .admin-main {
    padding: 16px 12px;
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
  transition: color 0.15s, border-color 0.15s, background 0.15s;
}
.back-top svg { width: 15px; height: 15px; display: block; }
.back-top:hover {
  color: var(--admin-accent);
  border-color: var(--admin-accent);
}

.back-top-enter-active,
.back-top-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.back-top-enter-from,
.back-top-leave-to { opacity: 0; transform: translateY(8px); }

@media (max-width: 768px) {
  .back-top { bottom: 20px; right: 16px; }
}
</style>
