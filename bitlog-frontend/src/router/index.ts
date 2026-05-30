import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/useUserStore'

NProgress.configure({ showSpinner: false })

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: (_to, _from, savedPosition) => savedPosition ?? { top: 0, left: 0 },
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      children: [
        { path: '', component: () => import('@/views/HomeView.vue'), meta: { darkTop: true } },
        { path: 'articles', component: () => import('@/views/ArticleListView.vue') },
        { path: 'article/:id(\\d+)', component: () => import('@/views/ArticleDetailView.vue'), meta: { darkTop: true } },
      ],
    },
    {
      path: '/admin/login',
      component: () => import('@/admin/views/AdminLoginView.vue'),
    },
    {
      path: '/admin/preview/:id(\\d+)',
      component: () => import('@/admin/views/PreviewView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin',
      component: () => import('@/admin/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        {
          path: 'dashboard',
          component: () => import('@/admin/views/DashboardView.vue'),
          meta: { title: '仪表盘' },
        },
        // 文章（admin only）
        {
          path: 'articles',
          component: () => import('@/admin/views/ArticlesView.vue'),
          meta: { title: '文章管理', parent: '文章', requiresAdmin: true },
        },
        {
          path: 'write',
          component: () => import('@/admin/views/WriteV2View.vue'),
          meta: { title: '写文章', parent: '文章', requiresAdmin: true },
        },
        {
          path: 'write/:id(\\d+)',
          component: () => import('@/admin/views/WriteV2View.vue'),
          meta: { title: '编辑文章', parent: '文章', requiresAdmin: true },
        },
                {
          path: 'categories',
          component: () => import('@/admin/views/CategoriesView.vue'),
          meta: { title: '分类管理', parent: '文章', requiresAdmin: true },
        },
        {
          path: 'tags',
          component: () => import('@/admin/views/TagsView.vue'),
          meta: { title: '标签管理', parent: '文章', requiresAdmin: true },
        },
        // 评论（admin only）
        {
          path: 'comments',
          component: () => import('@/admin/views/CommentsView.vue'),
          meta: { title: '评论', requiresAdmin: true },
        },
        // 用户
        {
          path: 'users',
          component: () => import('@/admin/views/UsersView.vue'),
          meta: { title: '用户管理', parent: '用户', requiresAdmin: true },
        },
        {
          path: 'users/add',
          component: () => import('@/admin/views/AddUserView.vue'),
          meta: { title: '新增用户', parent: '用户', requiresAdmin: true },
        },
        {
          path: 'profile',
          component: () => import('@/admin/views/ProfileView.vue'),
          meta: { title: '个人资料', parent: '用户' },
        },
        // 404
        {
          path: ':pathMatch(.*)*',
          component: () => import('@/admin/views/NotFoundView.vue'),
          meta: { title: '页面不存在' },
        },
      ],
    },
    // 全局 404
    {
      path: '/:pathMatch(.*)*',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

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

export default router
