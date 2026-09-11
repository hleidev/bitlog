import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

export function useDropdown(route: RouteLocationNormalizedLoaded) {
  const isOpen = ref(false)
  const containerRef = ref<HTMLElement | null>(null)
  const triggerRef = ref<HTMLButtonElement | null>(null)

  function menuItems() {
    return Array.from(
      containerRef.value?.querySelectorAll<HTMLElement>('[role^="menuitem"]:not(:disabled)') ?? [],
    )
  }

  async function open(last = false) {
    isOpen.value = true
    await nextTick()
    if (!isOpen.value) return
    const items = menuItems()
    items[last ? items.length - 1 : 0]?.focus({ preventScroll: true })
  }

  function close(restoreFocus = false) {
    isOpen.value = false
    if (restoreFocus) triggerRef.value?.focus({ preventScroll: true })
  }

  function toggle() {
    if (isOpen.value) {
      close()
      return
    }
    void open()
  }

  function handlePointerDown(event: PointerEvent) {
    if (containerRef.value?.contains(event.target as Node)) return
    close()
  }

  function handleKeydown(event: KeyboardEvent) {
    if (!containerRef.value?.contains(event.target as Node)) return
    if (event.key === 'Escape' && isOpen.value) {
      event.preventDefault()
      close(true)
      return
    }
    const direction = event.key === 'ArrowDown' ? 1 : event.key === 'ArrowUp' ? -1 : 0
    if (!isOpen.value) {
      if (direction && event.target === triggerRef.value) {
        event.preventDefault()
        void open(direction < 0)
      }
      return
    }
    if (!direction && event.key !== 'Home' && event.key !== 'End') return
    event.preventDefault()
    const items = menuItems()
    if (!items.length) return
    const current = items.indexOf(document.activeElement as HTMLElement)
    const index =
      event.key === 'Home'
        ? 0
        : event.key === 'End'
          ? items.length - 1
          : (current + direction + items.length) % items.length
    items[index]?.focus({ preventScroll: true })
  }

  function handleFocusIn(event: FocusEvent) {
    if (!containerRef.value?.contains(event.target as Node)) close()
  }

  watch(
    () => route.path,
    () => close(),
  )

  onMounted(() => {
    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeydown)
    document.addEventListener('focusin', handleFocusIn)
  })

  onUnmounted(() => {
    document.removeEventListener('pointerdown', handlePointerDown)
    document.removeEventListener('keydown', handleKeydown)
    document.removeEventListener('focusin', handleFocusIn)
  })

  return { isOpen, containerRef, triggerRef, open, close, toggle }
}
