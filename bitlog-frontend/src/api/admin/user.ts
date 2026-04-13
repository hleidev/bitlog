import request from '@/utils/request'

export interface UserPageQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  status?: number
  deleted?: number
  startTime?: string
  endTime?: string
}

export interface UserListItem {
  userId: number
  username: string
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
  username: string
  nickname: string
  email: string
  status: number
  userRole: number
  avatar: string
  deleted: number
  createTime: string
  updateTime: string
  position: string | null
  company: string | null
  profile: string | null
}

export interface UserStats {
  total: number
  enabled: number
  disabled: number
  deleted: number
}

export function getUserStats(): Promise<UserStats> {
  return request.get<never, UserStats>('/v1/admin/users/stats')
}

export function getUsers(query: UserPageQuery): Promise<PageVO<UserListItem>> {
  return request.get<never, PageVO<UserListItem>>('/v1/admin/users', { params: query })
}

export function getUserById(userId: number): Promise<UserDetail> {
  return request.get<never, UserDetail>(`/v1/admin/users/${userId}`)
}

export function updateUsersStatus(userIds: number[], status: 0 | 1): Promise<void> {
  return request.patch<never, void>('/v1/admin/users/status', { userIds, status })
}

export function deleteUsers(userIds: number[]): Promise<void> {
  return request.delete<never, void>('/v1/admin/users', { data: { userIds } })
}

export function restoreUsers(userIds: number[]): Promise<void> {
  return request.patch<never, void>('/v1/admin/users/restore', { userIds })
}

export function permanentDeleteUsers(userIds: number[]): Promise<void> {
  return request.delete<never, void>('/v1/admin/users/permanent', { data: { userIds } })
}

export function resetUserPassword(userId: number): Promise<{ newPassword: string }> {
  return request.post<never, { newPassword: string }>(`/v1/admin/users/${userId}/password/reset`)
}

export interface CreateUserBody {
  username: string
  email?: string
  userRole: 0 | 1
  position?: string
  company?: string
  profile?: string
}

export interface CreateUserResult {
  username: string
  initialPassword: string
}

export function createUser(body: CreateUserBody): Promise<CreateUserResult> {
  return request.post<never, CreateUserResult>('/v1/admin/users', body)
}
