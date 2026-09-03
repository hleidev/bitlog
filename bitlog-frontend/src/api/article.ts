import request from '@/utils/request'
import type { BasePageParams, PageResult } from '@/api/types'

export type { PageResult }

export interface ArticlePageParams extends BasePageParams {
  categoryId?: number
  /** AND 语义：文章需同时包含所有选中标签 */
  allTagIds?: number[]
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

/** 「继续阅读」只渲染这三个字段；预渲染时按这个形状写入静态 HTML */
export type ArticleLink = Pick<ArticleItemVO, 'id' | 'title' | 'publishTime'>

export interface ArticleDetailVO {
  id: number
  userId: number
  title: string
  summary?: string
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
