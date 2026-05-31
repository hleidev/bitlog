import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createHead } from '@unhead/vue/client'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from '@/router'
import App from './App.vue'
import '@/assets/styles/global.css'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(createHead())
app.use(ElementPlus)
app.component('ConfirmDialog', ConfirmDialog)

// 点击元素外部触发回调
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

app.mount('#app')
