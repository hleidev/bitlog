import { ref, onMounted, onUnmounted } from 'vue'
import { HEADER_HEIGHT } from '@/constants/layout'

export function useHeaderScroll() {
  const isScrolled = ref(false)

  const handleScroll = () => {
    // hero 固定为 100vh，滚过 hero 底部前 HEADER_HEIGHT 时触发
    isScrolled.value = window.scrollY > window.innerHeight - HEADER_HEIGHT
  }

  onMounted(() => window.addEventListener('scroll', handleScroll, { passive: true }))
  onUnmounted(() => window.removeEventListener('scroll', handleScroll))

  return { isScrolled }
}
