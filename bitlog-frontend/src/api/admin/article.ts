import request from '@/utils/request'

export type ArticleStatus = 'DRAFT' | 'PUBLISHED'

// ── List-item type (no content body) ─────────────────────────────────────────

export interface ArticleVO {
  id: number
  userId: number
  latestVersionId: number
  publishedVersionId: number | null
  title: string
  cover: string | null
  summary: string | null
  status: ArticleStatus
  categoryId: number | null
  categoryName: string | null
  tagIds: number[]
  tags: string[]
  topping: boolean
  publishTime: string | null
  createTime: string
  updateTime: string
  readCount: number
  commentCount: number
}

// ── Draft detail (editor load) ────────────────────────────────────────────────

export interface ArticleDetailVO {
  id: number
  userId: number
  versionId: number
  version: number
  latestVersionId: number
  publishedVersionId: number | null
  title: string
  content: string
  cover: string | null
  summary: string | null
  status: ArticleStatus
  categoryId: number | null
  categoryName: string | null
  tagIds: number[]
  tags: string[]
  topping: boolean
  publishTime: string | null
  createTime: string
  updateTime: string
  readCount: number
  commentCount: number
}

// ── Version types ─────────────────────────────────────────────────────────────

export interface ArticleVersionVO {
  id: number
  articleId: number
  version: number
  title: string
  latest: boolean
  createTime: string
}

export interface ArticleVersionDetailVO {
  id: number
  articleId: number
  version: number
  title: string
  content: string
  createTime: string
}

// ── List result ───────────────────────────────────────────────────────────────

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

export interface PublishArticleParams {
  cover?: string | null
  summary?: string | null
  categoryId?: number | null
  tagIds?: number[]
}

// ── Backend ↔ Frontend mapping helpers ───────────────────────────────────────

function toStatusInt(s: ArticleStatus): 0 | 1 {
  return s === 'PUBLISHED' ? 1 : 0
}

function fromStatusInt(s: number): ArticleStatus {
  return s === 1 ? 'PUBLISHED' : 'DRAFT'
}

function mapArticleVO(raw: Record<string, unknown>): ArticleVO {
  return {
    ...(raw as ArticleVO),
    status: fromStatusInt(raw.status as number),
    topping: raw.topping === 1,
    publishedVersionId: (raw.publishedVersionId as number | null) ?? null,
  }
}

function mapArticleDetailVO(raw: Record<string, unknown>): ArticleDetailVO {
  return {
    ...(raw as ArticleDetailVO),
    status: fromStatusInt(raw.status as number),
    topping: raw.topping === 1,
    publishedVersionId: (raw.publishedVersionId as number | null) ?? null,
  }
}

// ── Draft editing ─────────────────────────────────────────────────────────────

export function createArticle(data: { title: string; content: string }): Promise<number> {
  return request.post<never, number>('/v1/article', data)
}

export function updateArticleDraft(id: number, data: { title: string; content: string }): Promise<void> {
  return request.put<never, void>(`/v1/article/${id}`, data)
}

export function getArticleDraft(id: number): Promise<ArticleDetailVO> {
  return request
    .get<never, Record<string, unknown>>(`/v1/article/${id}/draft`)
    .then(mapArticleDetailVO)
}

export function publishArticle(id: number, params: PublishArticleParams): Promise<void> {
  return request.post<never, void>(`/v1/article/${id}/publish`, params)
}

// ── Version history ───────────────────────────────────────────────────────────

export function getArticleVersions(id: number): Promise<ArticleVersionVO[]> {
  return request.get<never, ArticleVersionVO[]>(`/v1/article/${id}/versions`)
}

export function getArticleVersionDetail(id: number, versionId: number): Promise<ArticleVersionDetailVO> {
  return request.get<never, ArticleVersionDetailVO>(`/v1/article/${id}/versions/${versionId}`)
}

export function rollbackVersion(id: number, versionId: number): Promise<void> {
  return request.post<never, void>(`/v1/article/${id}/versions/${versionId}/rollback`)
}

// ── Article management ────────────────────────────────────────────────────────

export function getMyArticles(params: GetMyArticlesParams): Promise<ArticleListResult> {
  const apiParams: Record<string, unknown> = {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  }
  if (params.status !== undefined) apiParams.status = toStatusInt(params.status)
  if (params.keyword)              apiParams.keyword = params.keyword
  if (params.categoryId !== undefined) apiParams.categoryId = params.categoryId

  return request
    .get<never, Record<string, unknown>>('/v1/article/my', { params: apiParams })
    .then(raw => {
      const r = raw as { counts: ArticleCounts; page: { content: unknown[]; [k: string]: unknown } }
      return {
        counts: r.counts,
        page: {
          ...(r.page as object),
          content: r.page.content.map(item => mapArticleVO(item as Record<string, unknown>)),
        } as ArticleListResult['page'],
      }
    })
}

export function updateArticlesStatus(ids: number[], status: ArticleStatus): Promise<void> {
  return request.patch<never, void>('/v1/article/batch/status', { ids, status: toStatusInt(status) })
}

export function deleteArticle(id: number): Promise<void> {
  return request.delete<never, void>(`/v1/article/${id}`)
}

export function deleteArticles(ids: number[]): Promise<void> {
  return request.delete<never, void>('/v1/article/batch', { data: { ids } })
}
