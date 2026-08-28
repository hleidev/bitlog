import type { RouterScrollBehavior } from 'vue-router'

// 超时兜底：取数失败或目标位置本就不可达（返回时列表变短）时不至于一直挂着
const RESTORE_TIMEOUT_MS = 1000

/**
 * 后退时恢复上次的滚动位置。
 *
 * 直接返回 savedPosition 会落空：PublicLayout 是 <Transition mode="out-in">，
 * 新视图要等旧视图离场后才挂载；而客户端跳转进来的页面读不到预渲染数据
 * （见 utils/ssgState），列表还得再等一次请求。scrollBehavior 在导航一完成就
 * 执行，那一刻文档高度撑不到目标位置，浏览器只能滚到它能滚的最远处 —— 顶部。
 */
export const scrollBehavior: RouterScrollBehavior = (_to, _from, savedPosition) => {
  if (!savedPosition) return { top: 0, left: 0 }
  if (typeof window === 'undefined') return savedPosition

  return new Promise((resolve) => {
    const deadline = performance.now() + RESTORE_TIMEOUT_MS
    const done = () => {
      clearTimeout(timer)
      resolve(savedPosition)
    }
    const tick = () => {
      const maxTop = document.documentElement.scrollHeight - window.innerHeight
      if (maxTop >= savedPosition.top || performance.now() >= deadline) return done()
      requestAnimationFrame(tick)
    }
    // rAF 在后台标签页里不触发，只靠它轮询会让 Promise 一直悬着
    const timer = setTimeout(done, RESTORE_TIMEOUT_MS)
    requestAnimationFrame(tick)
  })
}
