import request from '@/utils/request'

export interface ArticlePageParams {
  pageNum?: number
  pageSize?: number
  categoryId?: number
  tagIds?: number[]
  keyword?: string
}

export interface CategoryRef {
  id: number
  name: string
}

export interface TagRef {
  id: number
  name: string
}

export interface ArticleItemVO {
  id: number
  title: string
  summary: string | null
  category: CategoryRef | null
  tags: TagRef[]
  publishTime: string
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

export interface ArticleDetailVO {
  id: number
  title: string
  content: string
  category: CategoryRef | null
  tags: TagRef[]
  publishTime: string
}

export function getArticlePage(params?: ArticlePageParams): Promise<PageResult<ArticleItemVO>> {
  return request.get('/v1/article/page', { params })
}

export function getArticleDetail(id: number): Promise<ArticleDetailVO> {
  return request.get(`/v1/article/${id}`)
}
