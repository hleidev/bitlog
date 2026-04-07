import request from '@/utils/request'

export interface UserProfile {
  userId: number
  userName: string
  avatar: string
  email: string
  userRole: number  // 0: NORMAL, 1: ADMIN
  status: number
  position: string
  company: string
  profile: string
  createTime: string
  updateTime: string
}

export function getUserProfile(): Promise<UserProfile> {
  return request.get('/v1/user/profile')
}
