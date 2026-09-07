import type { RouteRecordRaw } from 'vue-router'

export const routes: RouteRecordRaw[] = [
  // DEV-only:编辑/阅读渲染一致性对照页,不进生产构建
  ...(import.meta.env.DEV
    ? [{ path: '/dev/vditor', component: () => import('@/views/DevVditorView.vue') }]
    : []),
  {
    path: '/',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      { path: '', component: () => import('@/views/HomeView.vue'), meta: { darkTop: true } },
      { path: 'articles', component: () => import('@/views/ArticleListView.vue') },
      { path: 'article/:id(\\d+)', component: () => import('@/views/ArticleDetailView.vue') },
      { path: 'friends', component: () => import('@/views/FriendsView.vue') },
      {
        path: 'notifications',
        component: () => import('@/views/NotificationsView.vue'),
        meta: { requiresAuth: true },
      },
      {
        // 放在公共布局内：授权失败时用户会停在这一页，裸页面既没有站点标识也无处可去。
        // 同时必须被预渲染成静态页，Google 回调是整页跳转，静态托管下缺这份 HTML 会 404
        path: 'auth/callback',
        component: () => import('@/views/AuthCallbackView.vue'),
      },
    ],
  },
  {
    path: '/admin/login',
    component: () => import('@/admin/views/AdminLoginView.vue'),
  },
  {
    // "new" 让还没保存过的新文章也能预览（内容由写作页通过 localStorage 交接）
    path: '/admin/preview/:id(\\d+|new)',
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
      {
        path: 'comments',
        component: () => import('@/admin/views/CommentsView.vue'),
        meta: { title: '评论', requiresAdmin: true },
      },
      {
        path: 'links',
        component: () => import('@/admin/views/LinksView.vue'),
        meta: { title: '友链', requiresAdmin: true },
      },
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
      {
        path: ':pathMatch(.*)*',
        component: () => import('@/admin/views/NotFoundView.vue'),
        meta: { title: '页面不存在' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/NotFoundView.vue'),
  },
]
