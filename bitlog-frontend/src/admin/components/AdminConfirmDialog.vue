<script setup lang="ts">
import { pending } from '@/admin/composables/useConfirm'

function onConfirm() {
  pending.value?.resolve()
  pending.value = null
}

function onCancel() {
  pending.value?.reject()
  pending.value = null
}
</script>

<template>
  <Transition name="confirm-fade">
    <div v-if="pending" class="confirm-overlay" @click.self="onCancel">
      <Transition name="confirm-scale" appear>
        <div v-if="pending" class="confirm-dialog">
          <h3 class="confirm-title">{{ pending.title }}</h3>
          <p class="confirm-msg">{{ pending.message }}</p>
          <div class="confirm-actions">
            <button class="confirm-btn confirm-btn--cancel" @click="onCancel">
              {{ pending.options.cancelText }}
            </button>
            <button
              class="confirm-btn confirm-btn--ok"
              :class="{ 'confirm-btn--danger': pending.options.danger }"
              @click="onConfirm"
            >
              {{ pending.options.confirmText }}
            </button>
          </div>
        </div>
      </Transition>
    </div>
  </Transition>
</template>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  background: var(--admin-overlay);
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.confirm-dialog {
  background: var(--admin-surface);
  border: 1px solid var(--admin-border);
  border-radius: 4px;
  padding: 28px 28px 22px;
  width: 380px;
  max-width: calc(100vw - 40px);
}

.confirm-title {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 16px;
  font-weight: 600;
  color: var(--admin-text-primary);
  margin: 0 0 10px;
}

.confirm-msg {
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: 13.5px;
  color: var(--admin-sidebar-text, #5a5248);
  line-height: 1.6;
  margin: 0 0 22px;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.confirm-btn {
  height: 34px;
  padding: 0 18px;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid;
  transition:
    background 0.15s,
    color 0.15s,
    border-color 0.15s;
}

.confirm-btn--cancel {
  background: transparent;
  color: var(--admin-sidebar-text, #5a5248);
  border-color: var(--admin-border-strong);
}

.confirm-btn--cancel:hover {
  background: var(--admin-surface-hover);
  border-color: var(--admin-border-strong);
}

.confirm-btn--ok {
  background: var(--admin-accent, #b85c38);
  color: var(--admin-text-on-accent);
  border-color: var(--admin-accent, #b85c38);
  font-weight: 500;
}

.confirm-btn--ok:hover {
  background: var(--admin-accent-dark, #924530);
  border-color: var(--admin-accent-dark, #924530);
}

.confirm-btn--danger {
  background: var(--admin-danger-bg-strong);
  border-color: var(--admin-danger);
}

.confirm-btn--danger:hover {
  background: var(--admin-danger-strong);
  border-color: var(--admin-danger-strong);
}

/* Transitions */
.confirm-fade-enter-active,
.confirm-fade-leave-active {
  transition: opacity 0.18s ease;
}
.confirm-fade-enter-from,
.confirm-fade-leave-to {
  opacity: 0;
}

.confirm-scale-enter-active {
  transition:
    transform 0.2s ease,
    opacity 0.2s ease;
}
.confirm-scale-enter-from {
  transform: scale(0.96);
  opacity: 0;
}
</style>
