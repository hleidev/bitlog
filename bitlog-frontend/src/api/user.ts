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
