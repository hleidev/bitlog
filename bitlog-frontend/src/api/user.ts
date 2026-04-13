import request from '@/utils/request'

export interface UserProfile {
  userId: number
  username: string
  nickname: string
  avatar: string
  email: string
  userRole: number  // 0: 普通用户, 1: 管理员
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
  nickname: string
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

// TODO: updateEmail - 邮箱修改接口，待实现
