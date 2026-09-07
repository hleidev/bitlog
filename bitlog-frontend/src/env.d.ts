/// <reference types="vite/client" />

import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    parent?: string // 面包屑父级标签
    requiresAuth?: boolean
    requiresAdmin?: boolean
  }
}
