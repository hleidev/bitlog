import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import type {} from 'vite-ssg'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        // 必须保留原始 Host：Spring 用它推导 OAuth 的 redirect_uri，
        // 改写成后端地址会让 Google 回调绕开本服务器，Cookie 种到另一个 host 上
        target: 'http://127.0.0.1:12301',
        changeOrigin: false,
      },
    },
  },
  ssgOptions: {
    formatting: 'minify',
  },
})
