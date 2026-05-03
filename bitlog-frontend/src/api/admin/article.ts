import request from '@/utils/request'

export type ArticleStatus = 'DRAFT' | 'PUBLISHED'

export interface ArticleVO {
  id: number
  title: string
  status: ArticleStatus
  cover: string | null
  summary: string | null
  categoryId: number | null
  categoryName: string | null
  tags: string[]
  topping: boolean
  publishTime: string | null
  createTime: string
  updateTime: string
  readCount: number
  commentCount: number
}

export interface ArticleCounts {
  total: number
  published: number
  draft: number
}

export interface ArticleListResult {
  counts: ArticleCounts
  page: {
    content: ArticleVO[]
    pageNum: number
    pageSize: number
    totalElements: number
    totalPages: number
    hasPrevious: boolean
    hasNext: boolean
  }
}

export interface GetMyArticlesParams {
  pageNum: number
  pageSize: number
  status?: ArticleStatus
  keyword?: string
  categoryId?: number
}

export function getMyArticles(params: GetMyArticlesParams): Promise<ArticleListResult> {
  return request.get<never, ArticleListResult>('/v1/article/my', { params })
}

export function updateArticlesStatus(ids: number[], status: ArticleStatus): Promise<void> {
  return request.patch<never, void>('/v1/article/batch/status', { ids, status })
}

export function deleteArticle(id: number): Promise<void> {
  return request.delete<never, void>(`/v1/article/${id}`)
}

export function deleteArticles(ids: number[]): Promise<void> {
  return request.delete<never, void>('/v1/article/batch', { data: { ids } })
}
