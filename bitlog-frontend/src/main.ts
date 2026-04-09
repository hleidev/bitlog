import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import router from '@/router'
import App from './App.vue'
import '@/assets/styles/global.css'

const app = createApp(App)
app.use(createPinia())
app.use(ElementPlus, { locale: zhCn })
app.use(router)

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
