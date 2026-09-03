import type { RouterScrollBehavior } from 'vue-router'
import { onceViewEntering } from './viewReady'

// 没有过渡的场景（如后台路由）不会有 before-enter，靠它兜底
const VIEW_ENTER_TIMEOUT_MS = 400
// 取数失败或目标位置本就不可达（返回时列表变短）时不至于一直挂着
const RESTORE_TIMEOUT_MS = 1000

function wait(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * 后退时恢复上次的滚动位置。
 *
 * 直接返回 savedPosition 会落空：客户端跳转进来的页面读不到预渲染数据
 * （见 utils/ssgState），列表要再等一次请求，导航刚完成时文档高度撑不到目标
 * 位置，浏览器只能滚到它能滚的最远处 —— 顶部。
 *
 * 但只加高度轮询还不够。PublicLayout 是 Transition mode="out-in"，新视图要等
 * 旧视图离场后才挂载，那段时间量到的是上一页的高度：从长文章返回时会立刻判定
 * 通过并滚动，随后新视图挂上、高度骤降，滚动被 clamp 回顶部。所以必须先等新
 * 视图进入 DOM，再量高度。
 */
export const scrollBehavior: RouterScrollBehavior = async (_to, _from, savedPosition) => {
  if (!savedPosition) return { top: 0, left: 0 }
  if (typeof window === 'undefined') return savedPosition

  await Promise.race([onceViewEntering(), wait(VIEW_ENTER_TIMEOUT_MS)])

  const deadline = performance.now() + RESTORE_TIMEOUT_MS
  await new Promise<void>((resolve) => {
    const done = () => {
      clearTimeout(timer)
      resolve()
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

  return savedPosition
}
