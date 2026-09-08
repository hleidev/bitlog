import { onMounted, onUnmounted, ref, watch } from 'vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

export function useDropdown(route: RouteLocationNormalizedLoaded) {
  const isOpen = ref(false)
  const containerRef = ref<HTMLElement | null>(null)
  const triggerRef = ref<HTMLButtonElement | null>(null)

  function open() {
    isOpen.value = true
  }

  function close(restoreFocus = false) {
    isOpen.value = false
    if (restoreFocus) triggerRef.value?.focus()
  }

  function toggle() {
    if (isOpen.value) {
      close()
      return
    }
    open()
  }

  function handlePointerDown(event: PointerEvent) {
    if (containerRef.value?.contains(event.target as Node)) return
    close()
  }

  function handleKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape' && isOpen.value) close(true)
  }

  watch(
    () => route.path,
    () => close(),
  )

  onMounted(() => {
    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeydown)
  })

  onUnmounted(() => {
    document.removeEventListener('pointerdown', handlePointerDown)
    document.removeEventListener('keydown', handleKeydown)
  })

  return { isOpen, containerRef, triggerRef, open, close, toggle }
}
