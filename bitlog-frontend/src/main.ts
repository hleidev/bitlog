import { ViteSSG } from 'vite-ssg'
import { createPinia } from 'pinia'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { routes } from '@/router'
import { scrollBehavior } from '@/router/scrollBehavior'
import { useModalStore } from '@/stores/useModalStore'
import { useUserStore } from '@/stores/useUserStore'
import App from './App.vue'
import '@/assets/styles/global.css'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

NProgress.configure({ showSpinner: false })

export const createApp = ViteSSG(App, { routes, scrollBehavior }, ({ app, router }) => {
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

      // 公开页不能在这里 await:vite-ssg 要 router.isReady() 之后才 mount,
      // 等会话会把整个 hydration 卡在刷新令牌的往返上,首屏因此晚一拍整页重绘。
      // 声明了 requiresAuth 的公开页除外，它必须先知道会话状态
      if (!to.path.startsWith('/admin') && !to.meta.requiresAuth) return

      const userStore = useUserStore()
      await userStore.waitForSession()

      if (to.path === '/admin/login') {
        if (userStore.isLoggedIn) return '/admin/dashboard'
        return
      }

      if (to.meta.requiresAuth && !userStore.isLoggedIn) {
        if (to.path.startsWith('/admin')) return '/admin/login'
        useModalStore().open('login')
        return '/'
      }

      if (to.meta.requiresAdmin && !userStore.isAdmin) {
        return '/admin/dashboard'
      }
    })

    router.afterEach(() => {
      NProgress.done()
    })
  }
})

const API_BASE = 'https://api.harrylei.top/api'

export async function includedRoutes(paths: string[]) {
  // 根布局的子路由可能被展开为相对路径。SSG 会导航两次，auth/callback
  // 第二次会相对当前目录变成 /auth/auth/callback，必须先统一为绝对路径。
  const staticPaths = paths
    .map((path) => (path.startsWith('/') ? path : `/${path}`))
    .filter((path) => !path.startsWith('/admin') && !path.includes(':') && !path.includes('*'))

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

  return [...new Set([...staticPaths, ...articlePaths])]
}
