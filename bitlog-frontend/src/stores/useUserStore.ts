import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, refresh as refreshApi, type LoginReq } from '@/api/auth'
import { getUserProfile, type UserProfile } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserProfile | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.userRole === 1)

  // 页面刷新后尝试用 HttpOnly Cookie 中的 Refresh Token 静默恢复会话
  async function initSession(): Promise<void> {
    const res = await refreshApi()
    token.value = res.accessToken
  }

  async function login(payload: LoginReq): Promise<void> {
    const res = await loginApi(payload)
    token.value = res.accessToken
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

  return { token, userInfo, isLoggedIn, isAdmin, initSession, login, fetchProfile, refreshToken, logout }
})
