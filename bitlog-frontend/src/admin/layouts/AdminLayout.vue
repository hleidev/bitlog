<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import AdminSidebar from '@/admin/components/AdminSidebar.vue'
import AdminHeader from '@/admin/components/AdminHeader.vue'
import '@/admin/styles/variables.css'

const MOBILE_BP = 768

const collapsed = ref(localStorage.getItem('admin-sidebar-collapsed') === 'true')
const drawerOpen = ref(false)
const windowWidth = ref(window.innerWidth)

const isMobile = computed(() => windowWidth.value <= MOBILE_BP)

function onResize() {
  windowWidth.value = window.innerWidth
  if (!isMobile.value) drawerOpen.value = false
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
      <div class="admin-main">
        <RouterView />
      </div>
    </div>
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
  overflow: hidden;
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
</style>
