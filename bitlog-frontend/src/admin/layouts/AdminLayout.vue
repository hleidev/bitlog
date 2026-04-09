<script setup lang="ts">
import { ref } from 'vue'
import AdminSidebar from '@/admin/components/AdminSidebar.vue'
import AdminHeader from '@/admin/components/AdminHeader.vue'
import '@/admin/styles/variables.css'

const collapsed = ref(false)
</script>

<template>
  <el-container class="admin-layout">
    <el-aside
      :width="collapsed ? 'var(--admin-sidebar-width-collapsed)' : 'var(--admin-sidebar-width)'"
      class="admin-aside"
    >
      <AdminSidebar :collapsed="collapsed" />
    </el-aside>

    <el-container class="admin-main-container">
      <el-header height="var(--admin-header-height)" class="admin-header-wrap">
        <AdminHeader :collapsed="collapsed" @toggle="collapsed = !collapsed" />
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
</style>
