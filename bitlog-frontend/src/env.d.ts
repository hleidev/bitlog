/// <reference types="vite/client" />

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    parent?: string       // 面包屑父级标签
    requiresAuth?: boolean
    requiresAdmin?: boolean
  }
}
