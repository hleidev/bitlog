<script setup lang="ts">
import { ref, watch, nextTick, onBeforeUnmount } from 'vue'

const props = defineProps<{
  open: boolean
  initialHref: string | null
}>()

const emit = defineEmits<{
  apply: [url: string | null]
  close: []
}>()

const url = ref('')
const inputRef = ref<HTMLInputElement | null>(null)

watch(
  () => props.open,
  async (isOpen) => {
    if (isOpen) {
      url.value = props.initialHref ?? ''
      await nextTick()
      inputRef.value?.focus()
      inputRef.value?.select()
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
  },
)

onBeforeUnmount(() => {
  document.body.style.overflow = ''
})

function onBackdrop() {
  emit('close')
}

function onApply() {
  const trimmed = url.value.trim()
  emit('apply', trimmed === '' ? null : trimmed)
}

function onRemove() {
  emit('apply', null)
}

function onKeydown(e: KeyboardEvent) {
  if (!props.open) return
  if (e.key === 'Escape') {
    e.preventDefault()
    emit('close')
  } else if (e.key === 'Enter') {
    e.preventDefault()
    onApply()
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="link-modal">
      <div
        v-if="open"
        class="link-modal-backdrop"
        role="dialog"
        aria-modal="true"
        aria-label="编辑链接"
        @click.self="onBackdrop"
        @keydown="onKeydown"
      >
        <div class="link-modal-card">
          <div class="link-modal-header">链接</div>
          <input
            ref="inputRef"
            v-model="url"
            class="link-modal-input"
            type="url"
            placeholder="https://..."
            @keydown="onKeydown"
          />
          <div class="link-modal-actions">
            <button
              v-if="initialHref"
              type="button"
              class="link-modal-btn link-modal-btn--remove"
              @mousedown.prevent="onRemove"
            >移除链接</button>
            <div class="link-modal-actions-right">
              <button
                type="button"
                class="link-modal-btn link-modal-btn--cancel"
                @mousedown.prevent="emit('close')"
              >取消</button>
              <button
                type="button"
                class="link-modal-btn link-modal-btn--primary"
                @mousedown.prevent="onApply"
              >应用</button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.link-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 9999;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 18vh;
}

.link-modal-card {
  background: #1c1917;
  border: 1px solid #3a3632;
  border-radius: 6px;
  width: 420px;
  max-width: calc(100vw - 32px);
  padding: 16px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: #f0ede8;
  font-family: inherit;
}

.link-modal-header {
  font-size: 14px;
  font-weight: 600;
  color: #f0ede8;
}

.link-modal-input {
  width: 100%;
  height: 34px;
  padding: 0 10px;
  background: #0e0c0b;
  border: 1px solid #3a3632;
  border-radius: 4px;
  color: #f0ede8;
  font-family: var(--font-mono, ui-monospace, monospace);
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.link-modal-input::placeholder { color: #6b6258; }
.link-modal-input:focus { border-color: var(--admin-accent, #b85c38); }

.link-modal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.link-modal-actions-right {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.link-modal-btn {
  height: 30px;
  padding: 0 14px;
  font-size: 13px;
  font-family: inherit;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  background: transparent;
}

.link-modal-btn--remove {
  color: #c0b8b0;
  border-color: #3a3632;
}
.link-modal-btn--remove:hover { background: #3a3632; color: #f0ede8; }

.link-modal-btn--cancel {
  color: #c0b8b0;
  border-color: #3a3632;
}
.link-modal-btn--cancel:hover { background: #3a3632; color: #f0ede8; }

.link-modal-btn--primary {
  background: var(--admin-accent, #b85c38);
  color: #fff;
  border-color: var(--admin-accent, #b85c38);
  font-weight: 500;
}
.link-modal-btn--primary:hover {
  background: var(--admin-accent-dark, #924530);
  border-color: var(--admin-accent-dark, #924530);
}

.link-modal-enter-active, .link-modal-leave-active { transition: opacity 0.15s ease; }
.link-modal-enter-active .link-modal-card, .link-modal-leave-active .link-modal-card { transition: transform 0.15s ease; }
.link-modal-enter-from, .link-modal-leave-to { opacity: 0; }
.link-modal-enter-from .link-modal-card, .link-modal-leave-to .link-modal-card { transform: translateY(-6px); }
</style>
