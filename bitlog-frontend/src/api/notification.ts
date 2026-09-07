import request from '@/utils/request'
import type { BasePageParams, PageResult } from '@/api/types'

export type NotificationType = 1 | 2 | 3 | 4 | 5

export const NOTIFICATION_TYPE = {
  COMMENT_REPLY: 1,
  ARTICLE_COMMENT: 2,
  LINK_APPLIED: 3,
  LINK_REVIEWED: 4,
  SYSTEM: 5,
} as const

export interface NotificationActorVO {
  userId: number
  username: string
  avatar?: string
  deactivated?: boolean
}

export interface NotificationVO {
  id: number
  type: NotificationType
  actor: NotificationActorVO | null
  targetType: number
  targetId: number | null
  payload: Record<string, unknown>
  readTime: string | null
  createTime: string
}

export type NotificationPageParams = BasePageParams

export function getNotificationPage(
  params?: NotificationPageParams,
): Promise<PageResult<NotificationVO>> {
  return request.get('/v1/notifications/page', { params })
}

export function getNotificationUnreadCount(): Promise<number> {
  return request.get('/v1/notifications/unread-count')
}

export function markNotificationRead(id: number): Promise<void> {
  return request.patch<never, void>(`/v1/notifications/${id}/read`)
}

export function markAllNotificationsRead(): Promise<void> {
  return request.patch<never, void>('/v1/notifications/read-all')
}
