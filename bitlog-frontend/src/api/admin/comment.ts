import request from '@/utils/request'
import type { BasePageParams, PageResult } from '@/api/types'
import { stripEmpty } from '@/api/types'

/** 1-正常，2-已隐藏 */
export type CommentStatus = 1 | 2

export interface CommentAdminUser {
  userId: number
  username: string
  avatar: string
}

export interface CommentAdmin {
  id: number
  articleId: number
  /** 文章已删除时为 null */
  articleTitle: string | null
  content: string
  user: CommentAdminUser | null
  /** 0 表示自身即根评论 */
  rootId: number | null
  status: CommentStatus
  createTime: string
}

export interface CommentAdminPageParams extends BasePageParams {
  status?: CommentStatus
  keyword?: string
}

export interface CommentStats {
  total: number
  visible: number
  hidden: number
}

export function getAdminCommentPage(
  params: CommentAdminPageParams,
): Promise<PageResult<CommentAdmin>> {
  return request.get('/v1/admin/comments/page', { params })
}

/** 计数与列表同口径：关键词参与过滤 */
export function getCommentStats(params: { keyword?: string } = {}): Promise<CommentStats> {
  return request.get('/v1/admin/comments/stats', { params: stripEmpty(params) })
}

export function updateCommentStatus(id: number, status: CommentStatus): Promise<void> {
  return request.patch(`/v1/admin/comments/${id}/status`, { status })
}

export function deleteComments(ids: number[]): Promise<void> {
  return request.delete('/v1/admin/comments/batch', { data: { ids } })
}
