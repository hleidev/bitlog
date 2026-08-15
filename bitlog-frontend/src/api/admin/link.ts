import request from '@/utils/request'
import type { BasePageParams, PageResult } from '@/api/types'
import type { FriendLinkSaveParam, LinkStatus } from '@/api/link'

export interface FriendLinkApplicant {
  userId: number
  username: string
  avatar: string
}

export interface FriendLinkAdmin {
  id: number
  name: string
  url: string
  avatar?: string
  description?: string
  applyMessage?: string
  status: LinkStatus
  rejectReason?: string
  /** 站长手动录入的友链没有申请人 */
  applicant: FriendLinkApplicant | null
  createTime: string
  updateTime: string
}

export interface FriendLinkAdminPageParams extends BasePageParams {
  status?: LinkStatus
  keyword?: string
}

export function getAdminLinkPage(
  params: FriendLinkAdminPageParams,
): Promise<PageResult<FriendLinkAdmin>> {
  return request.get('/v1/admin/links/page', { params })
}

export function createAdminLink(data: FriendLinkSaveParam): Promise<number> {
  return request.post('/v1/admin/links', data)
}

export function updateAdminLink(id: number, data: FriendLinkSaveParam): Promise<void> {
  return request.put(`/v1/admin/links/${id}`, data)
}

/** status 只接受 1（通过）或 2（拒绝）；拒绝时 rejectReason 会展示给申请人 */
export function auditAdminLink(
  id: number,
  status: LinkStatus,
  rejectReason?: string,
): Promise<void> {
  return request.put(`/v1/admin/links/${id}/audit`, { status, rejectReason })
}

export function deleteAdminLink(id: number): Promise<void> {
  return request.delete(`/v1/admin/links/${id}`)
}
