<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

/**
 * 后台自己在 AdminLayout 里挂 AdminConfirmDialog，而两个对话框读的是同一个 pending，
 * 全局这份再挂一次就会叠出两层：遮罩双倍变暗，点击落在上层遮罩上会被判成取消，
 * 且 public 版不渲染第三出口（如「直接离开」），后台的三按钮确认框会少一个按钮。
 */
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
</script>

<template>
  <RouterView />
  <ConfirmDialog v-if="!isAdminRoute" />
</template>
