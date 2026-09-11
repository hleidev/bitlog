import { nextTick, onMounted, onUnmounted, ref, useId } from 'vue'

/** 表格与移动卡片共享一个浮层，避免 Teleport 后出现两份菜单。 */
export function useRowMenu() {
  const openMenuId = ref<number | null>(null)
  const menuRef = ref<HTMLElement | null>(null)
  const menuId = useId()
  const menuStyle = ref<Record<string, string>>({})
  let trigger: HTMLElement | null = null

  function items() {
    return Array.from(
      menuRef.value?.querySelectorAll<HTMLElement>('[role="menuitem"]:not(:disabled)') ?? [],
    )
  }

  async function toggleMenu(id: number, event: MouseEvent, align: 'right' | 'left' = 'right') {
    if (openMenuId.value === id) {
      closeMenu()
      return
    }
    trigger = event.currentTarget as HTMLElement
    const anchor = trigger.getBoundingClientRect()
    // 定位前隐藏外观，不用 visibility 阻断随后紧接着的焦点转移。
    menuStyle.value = { opacity: '0' }
    openMenuId.value = id
    await nextTick()
    if (openMenuId.value !== id || !menuRef.value) return
    const bounds = menuRef.value.getBoundingClientRect()
    const gutter = 8
    const desiredLeft = align === 'right' ? anchor.right - bounds.width : anchor.left
    const desiredTop =
      anchor.bottom + bounds.height + gutter > window.innerHeight
        ? anchor.top - bounds.height - 4
        : anchor.bottom + 4
    menuStyle.value = {
      left: `${Math.max(gutter, Math.min(desiredLeft, window.innerWidth - bounds.width - gutter))}px`,
      top: `${Math.max(gutter, Math.min(desiredTop, window.innerHeight - bounds.height - gutter))}px`,
    }
    await nextTick()
    if (openMenuId.value === id) items()[0]?.focus({ preventScroll: true })
  }

  function closeMenu(restoreFocus = false) {
    const wasOpen = openMenuId.value !== null
    openMenuId.value = null
    if (restoreFocus && wasOpen && trigger?.isConnected) trigger.focus({ preventScroll: true })
  }

  function onOutside(event: Event) {
    const target = event.target as Node
    if (trigger?.contains(target) || menuRef.value?.contains(target)) return
    closeMenu()
  }

  function onKeydown(event: KeyboardEvent) {
    if (openMenuId.value === null) return
    if (event.key === 'Escape') {
      event.preventDefault()
      closeMenu(true)
      return
    }
    if (!menuRef.value?.contains(event.target as Node)) return
    const entries = items()
    if (!entries.length) return
    const step = event.key === 'ArrowDown' ? 1 : event.key === 'ArrowUp' ? -1 : 0
    if (!step && event.key !== 'Home' && event.key !== 'End') return
    event.preventDefault()
    const current = entries.indexOf(document.activeElement as HTMLElement)
    const index =
      event.key === 'Home'
        ? 0
        : event.key === 'End'
          ? entries.length - 1
          : (current + step + entries.length) % entries.length
    entries[index]?.focus({ preventScroll: true })
  }

  function onScroll(event: Event) {
    // 菜单自身在矮窗口中可以滚动；列表滚动则关闭，避免浮层脱离触发按钮。
    if (!(event.target instanceof Node) || !menuRef.value?.contains(event.target)) closeMenu()
  }

  function onResize() {
    closeMenu()
  }

  onMounted(() => {
    document.addEventListener('pointerdown', onOutside)
    document.addEventListener('focusin', onOutside)
    document.addEventListener('keydown', onKeydown)
    window.addEventListener('scroll', onScroll, true)
    window.addEventListener('resize', onResize)
  })
  onUnmounted(() => {
    document.removeEventListener('pointerdown', onOutside)
    document.removeEventListener('focusin', onOutside)
    document.removeEventListener('keydown', onKeydown)
    window.removeEventListener('scroll', onScroll, true)
    window.removeEventListener('resize', onResize)
  })

  return { openMenuId, menuRef, menuId, menuStyle, toggleMenu, closeMenu }
}
