import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { HEADER_HEIGHT } from '@/constants/layout'

export function useHeaderScroll() {
  const isScrolled = ref(false)
  const route = useRoute()

  const handleScroll = () => {
    const hero = document.getElementById('hero')
    if (!hero) {
      isScrolled.value = true
      return
    }
    isScrolled.value = window.scrollY > hero.offsetHeight - HEADER_HEIGHT
  }

  watch(() => route.path, () => nextTick(handleScroll))

  onMounted(() => {
    handleScroll()
    window.addEventListener('scroll', handleScroll, { passive: true })
  })
  onUnmounted(() => window.removeEventListener('scroll', handleScroll))

  return { isScrolled }
}
