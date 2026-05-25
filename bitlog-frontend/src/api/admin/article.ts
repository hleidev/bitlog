import request from '@/utils/request'

export type ArticleStatus = 'DRAFT' | 'PUBLISHED'

const STATUS_FROM_API: Record<number, ArticleStatus> = { 0: 'DRAFT', 1: 'PUBLISHED' }
const STATUS_TO_API: Record<ArticleStatus, number> = { DRAFT: 0, PUBLISHED: 1 }

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
  summary?: string | null
  categoryId?: number | null
  tagIds?: number[]
}

// ── Draft editing ─────────────────────────────────────────────────────────────

export function createArticle(data: { title: string; content: string }): Promise<number> {
  return request.post<never, number>('/v1/article', data)
}

export function updateArticleDraft(id: number, data: { title: string; content: string }): Promise<void> {
  return request.put<never, void>(`/v1/article/${id}`, data)
}

export function getArticleDraft(id: number): Promise<ArticleDetailVO> {
  return request.get<never, ArticleDetailVO>(`/v1/article/${id}/draft`)
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

export async function getMyArticles(params: GetMyArticlesParams): Promise<ArticleListResult> {
  const apiParams: Record<string, unknown> = {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  }
  if (params.status !== undefined) apiParams.status = STATUS_TO_API[params.status]
  if (params.keyword)              apiParams.keyword = params.keyword
  if (params.categoryId !== undefined) apiParams.categoryId = params.categoryId

  const res = await request.get<never, ArticleListResult>('/v1/article/my', { params: apiParams })
  res.page.content = res.page.content.map(a => ({
    ...a,
    status: STATUS_FROM_API[a.status as unknown as number] ?? a.status,
  }))
  return res
}

export function updateArticlesStatus(ids: number[], status: ArticleStatus): Promise<void> {
  return request.patch<never, void>('/v1/article/batch/status', { ids, status: STATUS_TO_API[status] })
}

export function deleteArticles(ids: number[]): Promise<void> {
  return request.delete<never, void>('/v1/article/batch', { data: { ids } })
}

export interface AiMetadataVO {
  summary:       string
  category:      { id: number; name: string } | null
  tags:          Array<{ id: number; name: string }>
  suggestedTags: string[]
}

export function generateAiMetadata(id: number): Promise<AiMetadataVO> {
  return request.post<never, AiMetadataVO>(`/v1/article/${id}/ai/metadata`)
}

export function deleteArticleVersions(articleId: number, versionIds: number[]): Promise<void> {
  return request.delete<never, void>(`/v1/article/${articleId}/versions/batch`, { data: { versionIds } })
}
