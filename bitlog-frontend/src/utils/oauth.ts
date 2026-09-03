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
 * 授权入口必须带上 API 基址：生产环境前端与后端分属两个域名，
 * 用相对路径会跳到前端域下不存在的 /api，直接 404
 */
function authorizationUrl(provider: OAuthProvider) {
  const base = import.meta.env.VITE_API_BASE_URL ?? ''
  return `${base}/api/oauth2/authorization/${provider}`
}

/**
 * 跳转到后端的授权入口。必须整页跳转而不是 XHR——OAuth 流程要求浏览器
 * 在授权服务器与本站之间做顶层导航，Ajax 会被跨域策略挡下。
 */
export function startOAuthLogin(provider: OAuthProvider, redirectAfterLogin?: string) {
  if (isInternalPath(redirectAfterLogin)) {
    sessionStorage.setItem(OAUTH_REDIRECT_KEY, redirectAfterLogin)
  }
  window.location.href = authorizationUrl(provider)
}

/**
 * 跳转到授权入口以绑定第三方账号，intent 为后端签发的一次性意图令牌
 */
export function startOAuthBind(provider: OAuthProvider, intent: string) {
  window.location.href = `${authorizationUrl(provider)}?intent=${encodeURIComponent(intent)}`
}
