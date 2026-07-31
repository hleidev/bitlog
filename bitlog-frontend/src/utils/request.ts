import axios, { type InternalAxiosRequestConfig, type AxiosError } from 'axios'

// 后端统一响应格式
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

// 需要刷新 Token 的业务错误码
const TOKEN_EXPIRED_CODES = new Set([40001, 40003])
// Refresh Token 失效的错误码
const REFRESH_EXPIRED_CODE = 41003

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

const SUCCESS_CODES = new Set([0, 200])

const request = axios.create({
  baseURL: (import.meta.env.VITE_API_BASE_URL ?? '') + '/api',
  timeout: 10000,
  withCredentials: true,
})

// 不需要携带 Access Token 的端点（login/register/refresh 靠 Cookie 或无需认证）
const NO_AUTH_URLS = new Set([
  '/v1/auth/login',
  '/v1/auth/register',
  '/v1/auth/register/code',
  '/v1/auth/refresh',
  '/v1/auth/logout',
  '/v1/auth/password/reset',
  '/v1/auth/password/reset/code',
])

// --- Token 刷新队列机制 ---
let isRefreshing = false
let subscribers: Array<(token: string) => void> = []

function subscribeTokenRefresh(callback: (token: string) => void) {
  subscribers.push(callback)
}

function onTokenRefreshed(newToken: string) {
  subscribers.forEach((cb) => cb(newToken))
  subscribers = []
}

// --- 请求拦截器：从 Pinia store 读取 Access Token（存内存，不存 storage）---
request.interceptors.request.use(async (config) => {
  if (!NO_AUTH_URLS.has(config.url ?? '')) {
    const { useUserStore } = await import('@/stores/useUserStore')
    const token = useUserStore().token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

// --- 响应拦截器：捕获 Token 过期，自动刷新并重试排队请求 ---
request.interceptors.response.use(
  (response) => {
    const res: Result = response.data
    if (!SUCCESS_CODES.has(res.code)) {
      return Promise.reject(new ApiError(res.message, res.code))
    }
    return res.data as never
  },
  async (error) => {
    const axiosError = error as AxiosError<Result>
    const originalRequest = axiosError.config as RetryableConfig | undefined
    const status = axiosError.response?.status
    const resCode = axiosError.response?.data?.code

    // 判断是否需要触发 Token 刷新
    const isAuthRoute = originalRequest?.url?.startsWith('/v1/auth/')
    const needsTokenRefresh =
      (status === 401 || status === 403 || TOKEN_EXPIRED_CODES.has(resCode!)) &&
      !originalRequest?._retry &&
      !isAuthRoute

    if (!needsTokenRefresh) {
      const message = axiosError.response?.data?.message ?? '网络错误，请稍后重试'
      return Promise.reject(new ApiError(message, resCode ?? status))
    }

    // 标记重试，防止递归
    if (originalRequest) {
      originalRequest._retry = true
    }

    // 当前有刷新中的请求，将当前请求加入排队
    if (isRefreshing) {
      return new Promise((resolve) => {
        subscribeTokenRefresh((newToken: string) => {
          if (originalRequest) {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(request(originalRequest))
          } else {
            resolve(Promise.reject(new ApiError('请求配置不存在', resCode)))
          }
        })
      })
    }

    // 没有正在刷新的请求，开始刷新
    isRefreshing = true

    try {
      const { useUserStore } = await import('@/stores/useUserStore')
      const store = useUserStore()
      await store.refreshToken()
      const newToken = store.token!

      // 刷新成功，重放排队的请求
      onTokenRefreshed(newToken)
      isRefreshing = false

      if (originalRequest) {
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return request(originalRequest)
      }
      return Promise.reject(new ApiError('请求配置不存在', resCode))
    } catch (refreshError) {
      isRefreshing = false
      subscribers = []

      // Refresh Token 也过期（41003），清空登录态并跳转登录
      const isRefreshExpired =
        (refreshError as unknown as { response?: { data?: { code?: number } } })?.response?.data
          ?.code === REFRESH_EXPIRED_CODE
      if (isRefreshExpired || !originalRequest) {
        const { useUserStore } = await import('@/stores/useUserStore')
        const { useModalStore } = await import('@/stores/useModalStore')
        useUserStore().logout()
        useModalStore().open('login')
      }

      const message =
        (refreshError as unknown as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ?? '登录已过期，请重新登录'
      return Promise.reject(new ApiError(message, REFRESH_EXPIRED_CODE))
    }
  },
)

export class ApiError extends Error {
  code: number | undefined
  constructor(message: string, code?: number) {
    super(message)
    this.code = code
  }
}

export default request
