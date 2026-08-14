import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  login as loginApi,
  logout as logoutApi,
  refresh as refreshApi,
  register as registerApi,
  type LoginReq,
  type RegisterReq,
} from '@/api/auth'
import { getUserProfile, type UserProfile } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserProfile | null>(null)
  const sessionInitialized = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.userRole === 1)

  // 等待 initSession 完成，用于路由守卫避免刷新时误判
  const _readyResolvers: Array<() => void> = []
  function waitForSession(): Promise<void> {
    if (sessionInitialized.value) return Promise.resolve()
    return new Promise((resolve) => _readyResolvers.push(resolve))
  }

  // 页面刷新后尝试用 HttpOnly Cookie 中的 Refresh Token 静默恢复会话
  async function initSession(): Promise<void> {
    try {
      const res = await refreshApi()
      token.value = res.accessToken
      await fetchProfile()
    } catch {
      // 无 Refresh Token 或已过期，用户未登录，正常情况
    } finally {
      sessionInitialized.value = true
      _readyResolvers.forEach((r) => r())
      _readyResolvers.length = 0
    }
  }

  async function login(payload: LoginReq): Promise<void> {
    const res = await loginApi(payload)
    token.value = res.accessToken
    await fetchProfile()
  }

  // 注册接口不返回 token，成功后直接用同一份凭证登录，省去用户再填一遍
  async function register(payload: RegisterReq): Promise<void> {
    await registerApi(payload)
    await login({ email: payload.email, password: payload.password })
  }

  async function fetchProfile(): Promise<void> {
    const info = await getUserProfile()
    userInfo.value = info
  }

  // 由 request.ts 401 拦截器调用，刷新 Access Token
  async function refreshToken(): Promise<void> {
    const res = await refreshApi()
    token.value = res.accessToken
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 即使接口失败，也清除本地状态
    }
    token.value = ''
    userInfo.value = null
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    sessionInitialized,
    waitForSession,
    initSession,
    login,
    register,
    fetchProfile,
    refreshToken,
    logout,
  }
})
