import request from '@/utils/request'

export interface UserProfile {
  userId: number
  username: string
  avatar: string
  email: string
  userRole: number // 0: 普通用户, 1: 管理员
  position: string
  company: string
  profile: string
  /** 第三方登录建号的账号为 false，界面据此在「修改密码」与「设置密码」间切换 */
  hasPassword: boolean
  createTime: string
  updateTime: string
}

export function getUserProfile(): Promise<UserProfile> {
  return request.get('/v1/user/profile')
}

export interface UpdateUserInfoReq {
  username: string
  position?: string
  company?: string
  profile?: string
}

export function updateUserInfo(data: UpdateUserInfoReq): Promise<void> {
  return request.put('/v1/user/info', data)
}

export function updateAvatar(fileUrl: string): Promise<void> {
  return request.put('/v1/user/avatar', null, { params: { avatar: fileUrl } })
}

export function updatePassword(data: { oldPassword: string; newPassword: string }): Promise<void> {
  return request.put('/v1/user/password', data)
}

/** 验证码发往待绑定的新邮箱，而非当前邮箱 */
export function sendEmailChangeCode(email: string): Promise<void> {
  return request.post('/v1/user/email/code', { email })
}

export function updateEmail(data: { email: string; code: string }): Promise<void> {
  return request.put('/v1/user/email', data)
}

/** 无密码账号（第三方登录建号）首次设置密码，会话本身已证明身份，无需旧密码 */
export function initPassword(password: string): Promise<void> {
  return request.post('/v1/user/password', { password })
}

export interface UserIdentity {
  provider: string
  /** 第三方平台侧邮箱，与本站登录邮箱可以不同 */
  providerEmail: string | null
}

export function listIdentities(): Promise<UserIdentity[]> {
  return request.get('/v1/user/identities')
}

/** 返回一次性意图令牌，随授权入口回传，供回调把绑定关联回当前账号 */
export function createBindIntent(): Promise<string> {
  return request.post('/v1/user/identities/bind-intent')
}

export function unbindIdentity(provider: string): Promise<void> {
  return request.delete(`/v1/user/identities/${provider}`)
}
