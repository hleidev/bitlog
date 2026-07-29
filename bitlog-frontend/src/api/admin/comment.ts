import request from '@/utils/request'
import type { PageResult } from '@/api/article'

/** 1-正常，2-已隐藏 */
export type CommentStatus = 1 | 2

export interface CommentAdminUser {
  userId: number
  nickname: string
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
  rootId: number
  status: CommentStatus
  createTime: string
}

export interface CommentAdminPageParams {
  pageNum?: number
  pageSize?: number
  articleId?: number
  userId?: number
  status?: CommentStatus
  keyword?: string
}

export function getAdminCommentPage(
  params: CommentAdminPageParams,
): Promise<PageResult<CommentAdmin>> {
  return request.get('/v1/admin/comments/page', { params })
}

export function updateCommentStatus(id: number, status: CommentStatus): Promise<void> {
  return request.patch(`/v1/admin/comments/${id}/status`, { status })
}

export function deleteComments(ids: number[]): Promise<void> {
  return request.delete('/v1/admin/comments/batch', { data: { ids } })
}
