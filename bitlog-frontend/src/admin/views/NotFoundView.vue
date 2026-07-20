<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="not-found">
    <p class="not-found-code">404</p>
    <p class="not-found-msg">你访问的页面不存在</p>
    <button class="not-found-btn" @click="router.push('/admin/dashboard')">返回概览</button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
  gap: 12px;
}

.not-found-code {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 72px;
  font-weight: 700;
  color: var(--admin-sidebar-border);
  line-height: 1;
  margin: 0;
}

.not-found-msg {
  font-size: 14px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  margin: 0 0 8px;
}

.not-found-btn {
  height: 34px;
  padding: 0 18px;
  background: var(--admin-accent);
  color: var(--admin-text-on-accent);
  border: none;
  border-radius: 4px;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.not-found-btn:hover {
  background: var(--admin-accent-dark);
}
</style>
