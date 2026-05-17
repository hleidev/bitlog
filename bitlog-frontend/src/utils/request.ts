import axios, { type InternalAxiosRequestConfig } from 'axios'

// 后端统一响应格式
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

const SUCCESS_CODES = new Set([0, 200])

const request = axios.create({
  baseURL: (import.meta.env.VITE_API_BASE_URL ?? '') + '/api',
  timeout: 10000,
})

// 不需要携带 Access Token 的端点（login/register/refresh 靠 Cookie 或无需认证）
const NO_AUTH_URLS = new Set(['/v1/auth/login', '/v1/auth/register', '/v1/auth/refresh', '/v1/auth/logout'])

// 请求拦截器：从 Pinia store 读取 Access Token（存内存，不存 storage）
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

// 响应拦截器：业务错误处理 + 401 自动刷新 Token 并重试
request.interceptors.response.use(
  (response) => {
    const res: Result = response.data
    if (!SUCCESS_CODES.has(res.code)) {
      return Promise.reject(new ApiError(res.message, res.code))
    }
    return res.data as never
  },
  async (error) => {
    const originalRequest = error.config as RetryableConfig
    const isAuthRoute = originalRequest?.url?.startsWith('/v1/auth/')

    if (error.response?.status === 401 && !originalRequest._retry && !isAuthRoute) {
      originalRequest._retry = true
      try {
        const { useUserStore } = await import('@/stores/useUserStore')
        const store = useUserStore()
        await store.refreshToken()
        originalRequest.headers.Authorization = `Bearer ${store.token}`
        return request(originalRequest)
      } catch {
        // Refresh Token 也已过期，让用户重新登录
        const { useUserStore } = await import('@/stores/useUserStore')
        const { useModalStore } = await import('@/stores/useModalStore')
        useUserStore().logout()
        useModalStore().open('login')
      }
    }

    const message = error.response?.data?.message ?? '网络错误，请稍后重试'
    return Promise.reject(new ApiError(message, error.response?.status))
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
