import request from '@/utils/request'
import type { BasePageParams, PageResult } from '@/api/types'

/** 账号三终态，后端翻译成 status + deleted 两列条件 */
export type UserState = 'ENABLED' | 'DISABLED' | 'DEACTIVATED'

export interface UserPageQuery extends BasePageParams {
  keyword?: string
  /** 不传则返回全部未注销账号 */
  state?: UserState
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

export interface UserDetail {
  userId: number
  username: string
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
  deactivated: number
}

export function getUserStats(): Promise<UserStats> {
  return request.get<never, UserStats>('/v1/admin/users/stats')
}

export function getUsers(query: UserPageQuery): Promise<PageResult<UserListItem>> {
  return request.get<never, PageResult<UserListItem>>('/v1/admin/users', { params: query })
}

export function getUserById(userId: number): Promise<UserDetail> {
  return request.get<never, UserDetail>(`/v1/admin/users/${userId}`)
}

export function updateUsersStatus(userIds: number[], status: 0 | 1): Promise<void> {
  return request.patch<never, void>('/v1/admin/users/status', { userIds, status })
}

/** 注销即删号：清 PII、释放邮箱与用户名，不可逆 */
export function deactivateUsers(userIds: number[]): Promise<void> {
  return request.delete<never, void>('/v1/admin/users', { data: { userIds } })
}

export function resetUserPassword(userId: number): Promise<{ newPassword: string }> {
  return request.post<never, { newPassword: string }>(`/v1/admin/users/${userId}/password/reset`)
}

export interface CreateUserBody {
  username: string
  email: string
  userRole: 0 | 1
  position?: string
  company?: string
  profile?: string
}

export interface CreateUserResult {
  email: string
  username: string
  initialPassword: string
}

export function createUser(body: CreateUserBody): Promise<CreateUserResult> {
  return request.post<never, CreateUserResult>('/v1/admin/users', body)
}
