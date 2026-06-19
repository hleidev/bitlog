<script setup lang="ts">
defineProps<{
  message?: string
  iconPath?: string
}>()
</script>

<template>
  <div class="empty-state">
    <svg v-if="iconPath" viewBox="0 0 24 24" fill="currentColor" class="empty-icon">
      <path :d="iconPath" />
    </svg>
    <span>{{ message ?? '暂无数据' }}</span>
    <slot />
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 60px 0;
  color: var(--admin-text-muted);
}

.empty-icon {
  width: 32px;
  height: 32px;
  opacity: 0.3;
}
.empty-state span {
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
}
</style>
