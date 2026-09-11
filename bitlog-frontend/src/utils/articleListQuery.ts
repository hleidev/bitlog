import type { LocationQuery, LocationQueryRaw } from 'vue-router'

export interface ArticleListQuery {
  keyword: string
  categoryId?: number
  tagIds: number[]
  page: number
}

function positiveInteger(value: unknown): number | undefined {
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value)) return undefined
  const number = Number(value)
  return Number.isSafeInteger(number) ? number : undefined
}

/** 保留旧的单标签 tagId 链接，多标签使用重复的 tagId 参数。 */
export function readArticleListQuery(query: LocationQuery): ArticleListQuery {
  const rawTags = Array.isArray(query.tagId) ? query.tagId : [query.tagId]
  const page = positiveInteger(query.page) ?? 1
  return {
    keyword: typeof query.keyword === 'string' ? query.keyword : '',
    categoryId: positiveInteger(query.categoryId),
    tagIds: [...new Set(rawTags.map(positiveInteger).filter((id) => id !== undefined))],
    page: page <= 2147483647 ? page : 1,
  }
}

export function writeArticleListQuery(state: ArticleListQuery): LocationQueryRaw {
  return {
    ...(state.keyword ? { keyword: state.keyword } : {}),
    ...(state.categoryId ? { categoryId: String(state.categoryId) } : {}),
    ...(state.tagIds.length ? { tagId: state.tagIds.map(String) } : {}),
    ...(state.page > 1 ? { page: String(state.page) } : {}),
  }
}
