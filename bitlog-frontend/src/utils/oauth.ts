export const OAUTH_REDIRECT_KEY = 'oauth_redirect'

export type OAuthProvider = 'google'

/**
 * 单个前导斜杠不足以判定是站内地址：`//evil.com` 是协议相对 URL，
 * 交给 location 或路由都会跳出本站
 */
export function isInternalPath(path: string | null | undefined): path is string {
  return !!path && path.startsWith('/') && !path.startsWith('//')
}

/**
 * 跳转到后端的授权入口。必须整页跳转而不是 XHR——OAuth 流程要求浏览器
 * 在授权服务器与本站之间做顶层导航，Ajax 会被跨域策略挡下。
 */
export function startOAuthLogin(provider: OAuthProvider, redirectAfterLogin?: string) {
  if (isInternalPath(redirectAfterLogin)) {
    sessionStorage.setItem(OAUTH_REDIRECT_KEY, redirectAfterLogin)
  }
  const base = import.meta.env.VITE_API_BASE_URL ?? ''
  window.location.href = `${base}/api/oauth2/authorization/${provider}`
}
