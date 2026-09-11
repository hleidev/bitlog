<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps<{
  visible: boolean
  width?: string
  ariaLabel?: string
}>()

const emit = defineEmits<{
  close: []
}>()

const dialogRef = ref<HTMLDialogElement | null>(null)
let scrollLocked = false
let savedScrollY = 0
let previouslyFocused: HTMLElement | null = null
const savedStyles = { position: '', top: '', width: '', paddingRight: '' }
let savedScrollbarWidth = ''

function lockScroll() {
  if (scrollLocked) return
  scrollLocked = true
  previouslyFocused = document.activeElement as HTMLElement
  savedScrollY = window.scrollY
  for (const key of Object.keys(savedStyles) as (keyof typeof savedStyles)[]) {
    savedStyles[key] = document.body.style[key]
  }
  savedScrollbarWidth = document.documentElement.style.getPropertyValue('--scrollbar-width')
  const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth
  document.documentElement.style.setProperty('--scrollbar-width', `${scrollbarWidth}px`)
  // 用 position:fixed 锁定，保证还原时滚动位置不丢失
  document.body.style.position = 'fixed'
  document.body.style.top = `-${savedScrollY}px`
  document.body.style.width = '100%'
  document.body.style.paddingRight = `${scrollbarWidth}px`
}

function unlockScroll() {
  if (!scrollLocked) return
  scrollLocked = false
  if (savedScrollbarWidth)
    document.documentElement.style.setProperty('--scrollbar-width', savedScrollbarWidth)
  else document.documentElement.style.removeProperty('--scrollbar-width')
  Object.assign(document.body.style, savedStyles)
  window.scrollTo(0, savedScrollY)
  // 将焦点还给触发元素，preventScroll 阻止 Safari 滚动到被聚焦元素
  if (previouslyFocused?.isConnected) previouslyFocused.focus({ preventScroll: true })
  previouslyFocused = null
}

function syncDialog() {
  const dialog = dialogRef.value
  if (!dialog) return
  if (props.visible) {
    if (dialog.open) return
    lockScroll()
    dialog.showModal()
  } else {
    dialog.close()
    unlockScroll()
  }
}

function onDialogClose() {
  // close 事件是异步派发的，快速重开时不能被上次的关闭事件再次关掉。
  if (dialogRef.value?.open) return
  unlockScroll()
  if (props.visible) emit('close')
}

watch(() => props.visible, syncDialog, { flush: 'post' })
onMounted(syncDialog)
onBeforeUnmount(() => {
  dialogRef.value?.close()
  unlockScroll()
})
</script>

<template>
  <Teleport to="body">
    <dialog
      ref="dialogRef"
      class="modal-backdrop"
      :aria-label="ariaLabel ?? '对话框'"
      @cancel.prevent="emit('close')"
      @close="onDialogClose"
      @click.self="emit('close')"
    >
      <div v-if="visible" class="modal-container" :style="{ width: width ?? '420px' }">
        <slot />
      </div>
    </dialog>
  </Teleport>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100dvh;
  max-width: none;
  max-height: none;
  margin: 0;
  border: 0;
  color: var(--color-text-primary);
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.modal-backdrop[open] {
  display: flex;
  animation: modal-appear 0.18s ease-out;
}

.modal-backdrop::backdrop {
  background: transparent;
}

.modal-container {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  max-width: 100%;
  max-height: calc(100dvh - 48px);
  overflow-y: auto;
}

@keyframes modal-appear {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
