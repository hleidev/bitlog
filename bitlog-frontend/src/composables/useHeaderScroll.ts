import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { HEADER_HEIGHT } from '@/constants/layout'

export function useHeaderScroll() {
  const isScrolled = ref(false)
  const route = useRoute()

  const handleScroll = () => {
    if (!route.meta.darkTop) {
      isScrolled.value = true
      return
    }
    const darkSection =
      document.getElementById('hero') ??
      document.querySelector<HTMLElement>('.article-header')
    const threshold = darkSection ? darkSection.offsetHeight - HEADER_HEIGHT : 260
    isScrolled.value = window.scrollY > threshold
  }

  watch(() => route.path, () => nextTick(handleScroll))

  onMounted(() => {
    handleScroll()
    window.addEventListener('scroll', handleScroll, { passive: true })
  })
  onUnmounted(() => window.removeEventListener('scroll', handleScroll))

  return { isScrolled }
}
