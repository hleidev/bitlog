import { type Ref, ref, nextTick, onMounted, onUnmounted } from 'vue'

// Detects whether the editor cursor is positioned inside this specific mark
// instance. Uses isActive for a quick type check, then domAtPos to disambiguate
// when multiple instances of the same mark type exist in the same paragraph.
export function useMarkViewCursor(
  getProps: () => { editor: any; view: any; mark?: any },
  rootEl: Ref<HTMLElement | null>,
) {
  const isCursorInside = ref(false)

  function checkCursor() {
    const { editor, view, mark } = getProps()
    if (!editor?.isEditable) {
      isCursorInside.value = false
      return
    }
    // Quick exit: this mark type is not active at cursor at all
    if (mark && !editor.isActive(mark.type.name)) {
      isCursorInside.value = false
      return
    }
    const { from } = editor.state.selection
    const contentEl = rootEl.value?.querySelector('[data-mark-view-content]') as HTMLElement | null
    if (!contentEl) {
      isCursorInside.value = false
      return
    }
    try {
      // side=1 (right bias) ensures start-of-mark position is included
      const { node: domNode } = view.domAtPos(from, 1)
      isCursorInside.value = !!(domNode && (contentEl === domNode || contentEl.contains(domNode)))
    } catch {
      isCursorInside.value = false
    }
  }

  onMounted(() => {
    const { editor } = getProps()
    editor?.on('selectionUpdate', checkCursor)
    nextTick(checkCursor)
  })

  onUnmounted(() => {
    const { editor } = getProps()
    editor?.off('selectionUpdate', checkCursor)
  })

  return { isCursorInside }
}
