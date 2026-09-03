/** 后端 PageVO<T> 的线格式，全站唯一定义 */
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  totalPages: number
  totalElements: number
  hasPrevious: boolean
  hasNext: boolean
  content: T[]
}

/** 后端 SortOrderEnum，按名称绑定 */
export type SortOrder = 'ASC' | 'DESC'

/** 所有分页接口共有的查询参数，排序字段的取值由各接口的枚举决定 */
export interface BasePageParams {
  pageNum?: number
  pageSize?: number
  sortField?: string
  sortOrder?: SortOrder
}

/**
 * 剥掉不参与过滤的空值：null / undefined / 空白串 / 空数组
 *
 * 后端约定空值一律视为不过滤，前端统一在这里剥一次，不再各处写 `|| undefined`
 */
export function stripEmpty(params: Record<string, unknown>): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined) continue
    if (typeof value === 'string' && value.trim() === '') continue
    if (Array.isArray(value) && value.length === 0) continue
    out[key] = typeof value === 'string' ? value.trim() : value
  }
  return out
}
