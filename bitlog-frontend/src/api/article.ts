import request from '@/utils/request'

export interface ArticlePageParams {
  pageNum?: number
  pageSize?: number
  categoryId?: number
  keyword?: string
}

export interface ArticleItemVO {
  id: number
  title: string
  summary: string | null
  cover: string | null
  categoryName: string | null
  tags: string[]
  topping: boolean
  publishTime: string
  readCount: number
  commentCount: number
}

export interface PageResult<T> {
  pageNum: number
  pageSize: number
  totalPages: number
  totalElements: number
  hasPrevious: boolean
  hasNext: boolean
  content: T[]
}

export function getArticlePage(params?: ArticlePageParams): Promise<PageResult<ArticleItemVO>> {
  return request.get('/v1/article/page', { params })
}