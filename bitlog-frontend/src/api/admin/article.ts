import request from '@/utils/request'
import { stripEmpty, type BasePageParams, type PageResult } from '@/api/types'

export type ArticleStatus = 'DRAFT' | 'PUBLISHED'

// 后端按枚举名绑定，不是 status 那样的数字码
export type MyArticleSort = 'CREATE_TIME' | 'UPDATE_TIME' | 'DISPLAY_TIME'

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
  category: { id: number; name: string } | null
  tags: { id: number; name: string }[]
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
  category: { id: number; name: string } | null
  tags: { id: number; name: string }[]
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

export interface GetMyArticlesParams extends BasePageParams {
  status?: ArticleStatus
  keyword?: string
  sortField?: MyArticleSort
}

export interface PublishArticleParams {
  summary?: string | null
  categoryId?: number | null
  tagIds?: number[]
}

export interface UpdateArticleMetaParams {
  summary?: string | null
  categoryId: number
  tagIds?: number[]
}

// ── Draft editing ─────────────────────────────────────────────────────────────

export function createArticle(data: { title: string; content: string }): Promise<number> {
  return request.post<never, number>('/v1/article', data)
}

export function updateArticleDraft(
  id: number,
  data: { title: string; content: string },
): Promise<void> {
  return request.put<never, void>(`/v1/article/${id}`, data)
}

export function getArticleDraft(id: number): Promise<ArticleDetailVO> {
  return request.get<never, ArticleDetailVO>(`/v1/article/${id}/draft`)
}

export function publishArticle(id: number, params: PublishArticleParams): Promise<void> {
  return request.post<never, void>(`/v1/article/${id}/publish`, params)
}

export function updateArticleMeta(id: number, params: UpdateArticleMetaParams): Promise<void> {
  return request.patch<never, void>(`/v1/article/${id}/meta`, params)
}

/** 放弃未发布的草稿改动：草稿头回退到已发布版本，被放弃的版本仍留在历史里 */
export function discardDraftAbovePublish(id: number): Promise<void> {
  return request.post<never, void>(`/v1/article/${id}/draft/discard`)
}

// ── Version history ───────────────────────────────────────────────────────────

export function getArticleVersions(id: number): Promise<ArticleVersionVO[]> {
  return request.get<never, ArticleVersionVO[]>(`/v1/article/${id}/versions`)
}

export function getArticleVersionDetail(
  id: number,
  versionId: number,
): Promise<ArticleVersionDetailVO> {
  return request.get<never, ArticleVersionDetailVO>(`/v1/article/${id}/versions/${versionId}`)
}

export function rollbackVersion(id: number, versionId: number): Promise<void> {
  return request.post<never, void>(`/v1/article/${id}/versions/${versionId}/rollback`)
}

// ── Article management ────────────────────────────────────────────────────────

export async function getMyArticles(params: GetMyArticlesParams): Promise<PageResult<ArticleVO>> {
  const apiParams = stripEmpty({
    ...params,
    status: params.status === undefined ? undefined : STATUS_TO_API[params.status],
  })

  const page = await request.get<never, PageResult<ArticleVO>>('/v1/article/my', {
    params: apiParams,
  })
  page.content = page.content.map((a) => ({
    ...a,
    status: STATUS_FROM_API[a.status as unknown as number] ?? a.status,
  }))
  return page
}

/** 计数与列表同口径：关键词参与过滤，状态分桶由后端一次算出 */
export function getMyArticleStats(params: { keyword?: string } = {}): Promise<ArticleCounts> {
  return request.get<never, ArticleCounts>('/v1/article/my/stats', { params: stripEmpty(params) })
}

export function updateArticlesStatus(ids: number[], status: ArticleStatus): Promise<void> {
  return request.patch<never, void>('/v1/article/batch/status', {
    ids,
    status: STATUS_TO_API[status],
  })
}

export function deleteArticles(ids: number[]): Promise<void> {
  return request.delete<never, void>('/v1/article/batch', { data: { ids } })
}

export interface AiMetadataVO {
  summary: string
  category: { id: number; name: string } | null
  tags: Array<{ id: number; name: string }>
  suggestedTags: string[]
}

export function generateAiMetadata(id: number): Promise<AiMetadataVO> {
  return request.post<never, AiMetadataVO>(`/v1/article/${id}/ai/metadata`)
}

export function deleteArticleVersions(articleId: number, versionIds: number[]): Promise<void> {
  return request.delete<never, void>(`/v1/article/${articleId}/versions/batch`, {
    data: { versionIds },
  })
}
