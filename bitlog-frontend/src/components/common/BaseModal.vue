<script setup lang="ts">
import { onMounted, onUnmounted, watch } from 'vue'

const props = defineProps<{
  visible: boolean
  width?: string
}>()

const emit = defineEmits<{
  close: []
}>()

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') emit('close')
}

let savedScrollY = 0

function lockScroll() {
  savedScrollY = window.scrollY
  const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth
  document.documentElement.style.setProperty('--scrollbar-width', `${scrollbarWidth}px`)
  // 用 position:fixed 锁定，保证还原时滚动位置不丢失
  document.body.style.position = 'fixed'
  document.body.style.top = `-${savedScrollY}px`
  document.body.style.width = '100%'
  document.body.style.paddingRight = `${scrollbarWidth}px`
}

function unlockScroll() {
  document.documentElement.style.removeProperty('--scrollbar-width')
  document.body.style.position = ''
  document.body.style.top = ''
  document.body.style.width = ''
  document.body.style.paddingRight = ''
  window.scrollTo(0, savedScrollY)
}

watch(() => props.visible, (val) => {
  if (val) lockScroll()
  else unlockScroll()
})

onMounted(() => window.addEventListener('keydown', handleKeydown))
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  unlockScroll()
})
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="modal-backdrop"
        role="dialog"
        aria-modal="true"
        @click.self="emit('close')"
      >
        <div class="modal-container" :style="{ width: width ?? '420px' }">
          <slot />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.modal-container {
  background: var(--color-bg-card);
  border-radius: 20px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.18);
  max-width: 100%;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
}

/* 动画 */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.25s ease;
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.25s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.94) translateY(12px);
  opacity: 0;
}
</style>
