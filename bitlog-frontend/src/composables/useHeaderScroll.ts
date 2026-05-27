import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { HEADER_HEIGHT } from '@/constants/layout'

export function useHeaderScroll() {
  const isScrolled = ref(false)
  const route = useRoute()
  let mo: MutationObserver | null = null

  function update() {
    const hero = document.getElementById('hero')
    if (hero) {
      isScrolled.value = window.scrollY > hero.offsetHeight - HEADER_HEIGHT
      return
    }
    const articleHeader = document.querySelector<HTMLElement>('.article-header')
    if (articleHeader) {
      isScrolled.value = articleHeader.getBoundingClientRect().bottom <= HEADER_HEIGHT
      return
    }
    // No dark section in DOM — stay opaque
    isScrolled.value = true
  }

  function attach() {
    mo?.disconnect()
    mo = null
    // Pages without darkTop never have a dark hero — always opaque regardless of DOM state
    if (!route.meta.darkTop) {
      isScrolled.value = true
      return
    }
    update()
    // If this is a dark-top page but its sentinel isn't in DOM yet
    // (article still loading), watch for it
    if (
      !document.getElementById('hero') &&
      !document.querySelector('.article-header')
    ) {
      mo = new MutationObserver(() => {
        if (document.getElementById('hero') || document.querySelector('.article-header')) {
          update()
          mo?.disconnect()
          mo = null
        }
      })
      mo.observe(document.body, { childList: true, subtree: true })
    }
  }

  watch(() => route.path, () => nextTick(attach))

  onMounted(() => {
    attach()
    window.addEventListener('scroll', update, { passive: true })
  })

  onUnmounted(() => {
    mo?.disconnect()
    window.removeEventListener('scroll', update)
  })

  return { isScrolled }
}
