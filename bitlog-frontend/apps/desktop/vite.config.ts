import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@bitlog/editor': fileURLToPath(new URL('../../packages/editor/src', import.meta.url)),
    },
  },
  // Tauri expects a fixed port in dev mode
  clearScreen: false,
  server: {
    port: 5174,
    strictPort: true,
    watch: {
      // Watch the shared editor package for hot reload
      ignored: ['!**/packages/editor/**'],
    },
  },
  envPrefix: ['VITE_', 'TAURI_'],
  build: {
    // Tauri uses Chromium on Linux/Windows, WebKit on macOS — both are modern
    target: ['es2021', 'chrome105', 'safari15'],
    minify: !process.env.TAURI_DEBUG,
    sourcemap: !!process.env.TAURI_DEBUG,
  },
})
