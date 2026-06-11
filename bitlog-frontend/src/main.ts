import { ViteSSG } from 'vite-ssg'
import { createPinia } from 'pinia'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { routes } from '@/router'
import { useUserStore } from '@/stores/useUserStore'
import App from './App.vue'
import '@/assets/styles/global.css'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

NProgress.configure({ showSpinner: false })

export const createApp = ViteSSG(
  App,
  { routes, scrollBehavior: (_to, _from, savedPosition) => savedPosition ?? { top: 0, left: 0 } },
  ({ app, router }) => {
    app.use(createPinia())
    app.component('ConfirmDialog', ConfirmDialog)

    app.directive('click-outside', {
      mounted(el, binding) {
        el._clickOutsideHandler = (e: MouseEvent) => {
          if (!el.contains(e.target as Node)) {
            binding.value()
          }
        }
        document.addEventListener('click', el._clickOutsideHandler)
      },
      unmounted(el) {
        document.removeEventListener('click', el._clickOutsideHandler)
      },
    })

    if (!import.meta.env.SSR) {
      useUserStore().initSession()

      router.beforeEach(async (to) => {
        NProgress.start()
        const userStore = useUserStore()
        await userStore.waitForSession()

        if (to.path === '/admin/login') {
          if (userStore.isLoggedIn) return '/admin/dashboard'
          return
        }

        if (to.meta.requiresAuth && !userStore.isLoggedIn) {
          return '/admin/login'
        }

        if (to.meta.requiresAdmin && !userStore.isAdmin) {
          return '/admin/dashboard'
        }
      })

      router.afterEach(() => {
        NProgress.done()
      })
    }
  },
)

const API_BASE = 'https://api.harrylei.top/api'

export async function includedRoutes(paths: string[]) {
  const staticPaths = paths.filter(
    (p) => !p.startsWith('/admin') && !p.includes(':') && !p.includes('*'),
  )

  const articlePaths: string[] = []
  try {
    let pageNum = 1
    while (true) {
      const res = await fetch(`${API_BASE}/v1/article/page?pageNum=${pageNum}&pageSize=100`)
      if (!res.ok) throw new Error(`API returned ${res.status}`)
      const { data } = await res.json()
      for (const article of data.content) {
        articlePaths.push(`/article/${article.id}`)
      }
      if (!data.hasNext) break
      pageNum++
    }
  } catch (err) {
    console.warn(`[ssg] failed to fetch articles: ${(err as Error).message}`)
    console.warn('[ssg] prerendering static pages only')
  }

  return [...staticPaths, ...articlePaths]
}
