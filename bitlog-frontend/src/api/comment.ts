import request from '@/utils/request'
import type { PageResult } from '@/api/article'

export interface CommentUserVO {
  userId: number
  username: string
  avatar: string
  /** 已注销：username 已被后端换成「已注销用户」，avatar 为空 */
  deactivated: boolean
}

export interface CommentReplyVO {
  id: number
  content: string
  user: CommentUserVO
  replyToUser: CommentUserVO | null
  createTime: string
}

export interface CommentVO {
  id: number
  /** removed 为 true 时为 null */
  content: string | null
  /** removed 为 true 时为 null */
  user: CommentUserVO | null
  /** 已被删除或隐藏，渲染为占位 */
  removed: boolean
  replies: CommentReplyVO[]
  replyCount: number
  createTime: string
}

export interface CommentSaveParams {
  content: string
  parentId?: number
}

export interface CommentPageParams {
  pageNum?: number
  pageSize?: number
}

export function getCommentPage(
  articleId: number,
  params?: CommentPageParams,
): Promise<PageResult<CommentVO>> {
  return request.get(`/v1/article/${articleId}/comments/page`, { params })
}

export function saveComment(articleId: number, data: CommentSaveParams): Promise<number> {
  return request.post(`/v1/article/${articleId}/comments`, data)
}

export function deleteComment(articleId: number, commentId: number): Promise<void> {
  return request.delete(`/v1/article/${articleId}/comments/${commentId}`)
}
