<script setup lang="ts">
import { toasts, useToast } from '@/admin/composables/useToast'

const { remove } = useToast()
</script>

<template>
  <div class="toast-container">
    <TransitionGroup name="toast" tag="div" class="toast-stack">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast"
        :class="`toast--${t.type}`"
        :role="t.type === 'error' ? 'alert' : 'status'"
        aria-atomic="true"
      >
        <svg class="toast-icon" viewBox="0 0 24 24" fill="currentColor">
          <path v-if="t.type === 'success'" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
          <path
            v-else-if="t.type === 'error'"
            d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
          />
          <path
            v-else
            d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"
          />
        </svg>
        <span class="toast-msg">{{ t.message }}</span>
        <button class="toast-close" aria-label="关闭提示" @click="remove(t.id)">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
            />
          </svg>
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 68px;
  right: 16px;
  z-index: 9100;
  width: min(360px, calc(100vw - 32px));
  pointer-events: none;
}

.toast-stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  background: var(--admin-surface);
  border: 1px solid var(--admin-border);
  border-left-width: 3px;
  border-radius: 6px;
  width: 100%;
  box-shadow: 0 6px 24px rgb(0 0 0 / 8%);
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  pointer-events: auto;
}

.toast--success {
  border-left-color: var(--admin-success);
}
.toast--error {
  border-left-color: var(--admin-danger);
}
.toast--warning {
  border-left-color: var(--admin-warning);
}

.toast-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.toast--success .toast-icon {
  color: var(--admin-success);
}
.toast--error .toast-icon {
  color: var(--admin-danger);
}
.toast--warning .toast-icon {
  color: var(--admin-warning);
}

.toast-msg {
  flex: 1;
  min-width: 0;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.toast-close {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--admin-sidebar-text-muted);
  padding: 0;
  border-radius: var(--admin-radius);
  transition: color 0.15s;
}

.toast-close:hover {
  color: var(--admin-sidebar-text);
}

.toast-close:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
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

@media (prefers-reduced-motion: reduce) {
  .toast-enter-active,
  .toast-leave-active,
  .toast-move {
    transition: none;
  }
}
</style>
