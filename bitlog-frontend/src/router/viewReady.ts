/**
 * PublicLayout 的 <Transition mode="out-in"> 会让新视图晚于导航完成才挂载。
 * 在那之前页面里仍是上一页的 DOM，量到的 scrollHeight 属于旧视图 —— 滚动恢复
 * 若据此判断高度够不够，会在旧视图上判定通过，等新视图挂上、高度骤降时被浏览器
 * clamp 回顶部。
 */
let waiters: (() => void)[] = []

/** 新视图已插入 DOM（进入动画尚未开始）时调用。 */
export function notifyViewEntering(): void {
  const queue = waiters
  waiters = []
  for (const resolve of queue) resolve()
}

/** 等待下一次新视图插入 DOM。没有过渡的场景由调用方的超时兜底。 */
export function onceViewEntering(): Promise<void> {
  return new Promise((resolve) => waiters.push(resolve))
}
