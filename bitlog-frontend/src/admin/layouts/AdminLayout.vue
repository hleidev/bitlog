<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import AdminSidebar from '@/admin/components/AdminSidebar.vue'
import AdminHeader from '@/admin/components/AdminHeader.vue'
import '@/admin/styles/variables.css'

const MOBILE_BP = 768

const collapsed = ref(false)
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
  }
}

const route = useRoute()
watch(() => route.path, () => {
  if (isMobile.value) drawerOpen.value = false
})
</script>

<template>
  <el-container class="admin-layout">
    <!-- Mobile backdrop -->
    <Transition name="backdrop">
      <div
        v-if="isMobile && drawerOpen"
        class="admin-backdrop"
        @click="drawerOpen = false"
      />
    </Transition>

    <el-aside
      :width="collapsed ? 'var(--admin-sidebar-width-collapsed)' : 'var(--admin-sidebar-width)'"
      class="admin-aside"
      :class="{ 'drawer-open': isMobile && drawerOpen, 'is-mobile': isMobile }"
    >
      <AdminSidebar :collapsed="isMobile ? false : collapsed" />
    </el-aside>

    <el-container class="admin-main-container">
      <el-header height="var(--admin-header-height)" class="admin-header-wrap">
        <AdminHeader
          :collapsed="isMobile ? !drawerOpen : collapsed"
          :is-mobile="isMobile"
          @toggle="handleToggle"
        />
      </el-header>

      <el-main class="admin-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

.admin-aside {
  transition: width 0.22s ease;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
}

/* Mobile: sidebar becomes a fixed drawer overlay */
.admin-aside.is-mobile {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  width: var(--admin-sidebar-width) !important;
  z-index: 1001;
  transform: translateX(-100%);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.admin-aside.is-mobile.drawer-open {
  transform: translateX(0);
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.3);
}

/* Backdrop */
.admin-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
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
  padding: 0;
  flex-shrink: 0;
}

.admin-main {
  background: var(--admin-body-bg);
  overflow-y: auto;
  padding: 24px;
}

@media (max-width: 768px) {
  .admin-main {
    padding: 16px 12px;
  }
}
</style>
