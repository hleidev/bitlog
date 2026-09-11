<script setup lang="ts">
import BaseModal from '@/components/common/BaseModal.vue'
import { pending, type ConfirmResult } from '@/composables/useConfirm'

function resolve(result: ConfirmResult) {
  const current = pending.value
  pending.value = null
  current?.resolve(result)
}

function onCancel() {
  const current = pending.value
  pending.value = null
  current?.reject()
}
</script>

<template>
  <BaseModal :visible="!!pending" width="420px" :aria-label="pending?.title" @close="onCancel">
    <div v-if="pending" class="confirm-dialog">
      <h3 class="confirm-title">{{ pending.title }}</h3>
      <p class="confirm-msg">{{ pending.message }}</p>
      <div class="confirm-actions">
        <button class="confirm-btn confirm-btn--cancel" autofocus @click="onCancel">
          {{ pending.options.cancelText }}
        </button>
        <button
          v-if="pending.options.extraText"
          class="confirm-btn confirm-btn--extra"
          @click="resolve('extra')"
        >
          {{ pending.options.extraText }}
        </button>
        <button
          class="confirm-btn confirm-btn--ok"
          :class="{ 'confirm-btn--danger': pending.options.danger }"
          @click="resolve('confirm')"
        >
          {{ pending.options.confirmText }}
        </button>
      </div>
    </div>
  </BaseModal>
</template>

<style scoped>
.confirm-dialog {
  padding: 28px;
}
.confirm-title {
  margin: 0 0 12px;
  font-family: var(--font-display, var(--font-serif, serif));
  font-size: 24px;
  font-weight: 500;
  line-height: 1.4;
  color: var(--color-text-primary);
}
.confirm-msg {
  margin: 0 0 28px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  white-space: pre-line;
  overflow-wrap: anywhere;
}
.confirm-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}
.confirm-btn {
  min-height: 38px;
  padding: 8px 15px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: transparent;
  color: var(--color-text-secondary);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s;
}
.confirm-btn:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-border-strong);
}
.confirm-btn:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 3px;
}
.confirm-btn--extra {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
}
.confirm-btn--ok {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: var(--color-text-on-accent);
}
.confirm-btn--ok:hover {
  background: var(--color-accent-dark);
  border-color: var(--color-accent-dark);
}
.confirm-btn--danger {
  background: var(--color-danger);
  border-color: var(--color-danger);
}
.confirm-btn--danger:hover {
  background: var(--color-danger-strong);
  border-color: var(--color-danger-strong);
}
@media (max-width: 480px) {
  .confirm-dialog {
    padding: 24px 20px;
  }
  .confirm-title {
    font-size: 22px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .confirm-btn {
    transition: none;
  }
}
</style>
