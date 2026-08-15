import request from '@/utils/request'

/** 0-待审核，1-已通过，2-已拒绝 */
export type LinkStatus = 0 | 1 | 2

export const LINK_STATUS = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
} as const

export interface FriendLinkVO {
  id: number
  name: string
  url: string
  avatar?: string
  description?: string
}

export interface MyFriendLinkVO extends FriendLinkVO {
  applyMessage?: string
  status: LinkStatus
  rejectReason?: string
  createTime: string
}

export interface FriendLinkSaveParam {
  name: string
  url: string
  avatar?: string
  description?: string
  applyMessage?: string
}

/** 公开展示的友链 */
export function getFriendLinks(): Promise<FriendLinkVO[]> {
  return request.get('/v1/links')
}

/** 我的友链，未申请时返回 null */
export function getMyFriendLink(): Promise<MyFriendLinkVO | null> {
  return request.get('/v1/links/mine')
}

export function applyFriendLink(data: FriendLinkSaveParam): Promise<number> {
  return request.post('/v1/links/mine', data)
}

export function updateMyFriendLink(data: FriendLinkSaveParam): Promise<void> {
  return request.put('/v1/links/mine', data)
}

export function deleteMyFriendLink(): Promise<void> {
  return request.delete('/v1/links/mine')
}
