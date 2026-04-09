import request from '@/utils/request'

export interface UserPageQuery {
  pageNum?: number
  pageSize?: number
  userName?: string
  status?: number
  startTime?: string
  endTime?: string
}

export interface UserListItem {
  userId: number
  userName: string
  status: number
  userRole: number
  avatar: string
  email: string
  deleted: number
  createTime: string
  updateTime: string
}

export interface PageVO<T> {
  pageNum: number
  pageSize: number
  totalPages: number
  totalElements: number
  hasNext: boolean
  hasPrevious: boolean
  content: T[]
}

export interface UserDetail {
  userId: number
  userName: string
  email: string
  status: number
  userRole: 'ADMIN' | 'USER'
  avatar: string
  deleted: 'NO' | 'YES'
  createTime: string
  updateTime: string
  position: string | null
  company: string | null
  profile: string | null
}

export function getUsers(query: UserPageQuery): Promise<PageVO<UserListItem>> {
  return request.get<never, PageVO<UserListItem>>('/v1/admin/users', { params: query })
}

export function getUserById(userId: number): Promise<UserDetail> {
  return request.get<never, UserDetail>(`/v1/admin/users/${userId}`)
}

export function updateUserStatus(userId: number, status: 0 | 1): Promise<void> {
  return request.patch<never, void>(`/v1/admin/users/${userId}/status`, null, { params: { status } })
}
