import { onMounted, onUnmounted, ref } from 'vue'

/**
 * 表格行「更多」下拉的开合与定位。
 *
 * 菜单必须 Teleport 到 body 并用 fixed 定位：.table-wrap 的 overflow-x: auto 会让
 * overflow-y 被算成 auto，绝对定位的菜单一旦超出容器底边就会被裁掉（末行菜单
 * 因此被分页栏「吃掉」），这种裁剪不是层叠问题，调 z-index 无效。
 *
 * 原本只有 UsersView 有一份，文章页和友链页因为没有下拉，把 4 个按钮全平铺在
 * 操作列里，列宽被撑到 190px。
 */
export function useRowMenu() {
  const openMenuId = ref<number | null>(null)
  const menuStyle = ref<Record<string, string>>({})

  // 菜单最多 6 项 + 2 条分隔线，取略保守的高度用于判断翻转
  const MENU_MAX_HEIGHT = 220

  function toggleMenu(id: number, ev?: MouseEvent, align: 'right' | 'left' = 'right') {
    if (openMenuId.value === id) {
      openMenuId.value = null
      return
    }
    const btn = ev?.currentTarget as HTMLElement | undefined
    if (btn) {
      const r = btn.getBoundingClientRect()
      const flipUp = window.innerHeight - r.bottom < MENU_MAX_HEIGHT
      menuStyle.value = {
        ...(align === 'right'
          ? { right: `${window.innerWidth - r.right}px` }
          : { left: `${r.left}px` }),
        ...(flipUp
          ? { bottom: `${window.innerHeight - r.top + 4}px` }
          : { top: `${r.bottom + 4}px` }),
      }
    }
    openMenuId.value = id
  }

  function closeMenu() {
    openMenuId.value = null
  }

  // fixed 定位不跟随滚动，滚动时直接关闭而非重算位置
  onMounted(() => window.addEventListener('scroll', closeMenu, true))
  onUnmounted(() => window.removeEventListener('scroll', closeMenu, true))

  return { openMenuId, menuStyle, toggleMenu, closeMenu }
}
