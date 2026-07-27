/**
 * vite-ssg 预渲染状态桥。
 *
 * vite-ssg 把入口路由的 initialState 对象挂在 `route.meta.state` 上：
 * 预渲染阶段（onServerPrefetch）写进去的数据会被序列化进 HTML 的
 * `window.__INITIAL_STATE__`，客户端 hydration 前读回同一个对象。
 *
 * 用途：让静态 HTML 的 DOM 与客户端首帧的初始状态一致，避免 hydration 不匹配。
 * 客户端路由跳转进来的页面没有这个对象，读取返回 undefined，走正常请求路径。
 */
import type { RouteLocationNormalizedLoaded } from 'vue-router'

type SSGState = Record<string, unknown>

function stateOf(route: RouteLocationNormalizedLoaded): SSGState | undefined {
  return route.meta.state as SSGState | undefined
}

/** 读取预渲染写入的数据；非预渲染入口（客户端跳转）返回 undefined。 */
export function readSSGState<T>(route: RouteLocationNormalizedLoaded, key: string): T | undefined {
  return stateOf(route)?.[key] as T | undefined
}

/** 预渲染阶段写入数据；仅在 SSR 下生效。 */
export function writeSSGState<T>(
  route: RouteLocationNormalizedLoaded,
  key: string,
  value: T,
): void {
  const state = stateOf(route)
  if (state) state[key] = value
}
