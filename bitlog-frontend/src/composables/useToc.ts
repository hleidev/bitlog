import { ref, computed, onMounted, onUnmounted } from 'vue'

export interface TocItem {
  id: string
  level: number
  text: string
}

export function useToc() {
  const toc = ref<TocItem[]>([])
  const activeSection = ref('')

  const activeParentId = computed(() => {
    const idx = toc.value.findIndex((t) => t.id === activeSection.value)
    if (idx === -1) return toc.value.find((t) => t.level === 2)?.id ?? ''
    for (let i = idx; i >= 0; i--) {
      if (toc.value[i].level === 2) return toc.value[i].id
    }
    return ''
  })

  const visibleTocItems = computed(() =>
    toc.value.filter((item, idx) => {
      if (item.level === 2) return true
      for (let i = idx - 1; i >= 0; i--) {
        if (toc.value[i].level === 2) return toc.value[i].id === activeParentId.value
      }
      return false
    }),
  )

  function buildToc() {
    const headings = document.querySelectorAll<HTMLElement>(
      '.prose h1, .prose h2, .prose h3, .prose h4',
    )
    toc.value = Array.from(headings).map((el, i) => {
      if (!el.id) el.id = `heading-${i}`
      const tag = el.tagName
      const level = tag === 'H1' || tag === 'H2' ? 2 : 3
      return { id: el.id, level, text: el.textContent ?? '' }
    })
    if (toc.value.length) activeSection.value = toc.value[0].id
  }

  function scrollToSection(id: string) {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const onScroll = () => {
    for (let i = toc.value.length - 1; i >= 0; i--) {
      const el = document.getElementById(toc.value[i].id)
      if (el && el.getBoundingClientRect().top <= 100) {
        activeSection.value = toc.value[i].id
        break
      }
    }
  }

  onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
  onUnmounted(() => window.removeEventListener('scroll', onScroll))

  return { toc, activeSection, visibleTocItems, buildToc, scrollToSection }
}
