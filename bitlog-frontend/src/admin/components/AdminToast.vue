<script setup lang="ts">
import { toasts, useToast } from '@/admin/composables/useToast'

const { remove } = useToast()
</script>

<template>
  <div class="toast-container">
    <TransitionGroup name="toast" tag="div">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast"
        :class="`toast--${t.type}`"
      >
        <svg class="toast-icon" viewBox="0 0 24 24" fill="currentColor">
          <path v-if="t.type === 'success'" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
          <path v-else-if="t.type === 'error'" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
          <path v-else d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
        </svg>
        <span class="toast-msg">{{ t.message }}</span>
        <button class="toast-close" @click="remove(t.id)">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
          </svg>
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9100;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  background: #faf9f7;
  border: 1px solid #e8e4de;
  border-left-width: 3px;
  border-radius: 4px;
  min-width: 240px;
  max-width: 360px;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text, #5a5248);
  pointer-events: auto;
}

.toast--success { border-left-color: #5c8a5c; }
.toast--error   { border-left-color: #c04040; }
.toast--warning { border-left-color: #b87028; }

.toast-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.toast--success .toast-icon { color: #5c8a5c; }
.toast--error   .toast-icon { color: #c04040; }
.toast--warning .toast-icon { color: #b87028; }

.toast-msg {
  flex: 1;
  line-height: 1.4;
}

.toast-close {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--admin-sidebar-text-muted, #b0a89e);
  padding: 0;
  border-radius: 2px;
  transition: color 0.15s;
}

.toast-close:hover {
  color: var(--admin-sidebar-text, #5a5248);
}

.toast-close svg {
  width: 14px;
  height: 14px;
}

/* Transition */
.toast-enter-active {
  transition: all 0.22s ease;
}
.toast-leave-active {
  transition: all 0.18s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
.toast-move {
  transition: transform 0.2s ease;
}
</style>
