import { ref, onMounted, onUnmounted } from 'vue'
import { HEADER_HEIGHT } from '@/constants/layout'

export function useHeaderScroll() {
  const isScrolled = ref(false)

  const handleScroll = () => {
    const hero = document.getElementById('hero')
    if (!hero) {
      isScrolled.value = true
      return
    }
    isScrolled.value = window.scrollY > hero.offsetHeight - HEADER_HEIGHT
  }

  onMounted(() => {
    handleScroll()
    window.addEventListener('scroll', handleScroll, { passive: true })
  })
  onUnmounted(() => window.removeEventListener('scroll', handleScroll))

  return { isScrolled }
}
