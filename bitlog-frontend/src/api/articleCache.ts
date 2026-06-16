import { getArticleDetail, type ArticleDetailVO } from '@/api/article'

// 纯内存缓存：页面刷新即清空，保证内容实时性；仅用于会话内秒开 + 预取。
const detailCache = new Map<number, ArticleDetailVO>()
const inflight = new Map<number, Promise<ArticleDetailVO>>()

/** 同步读取已缓存的文章详情；未命中返回 undefined。 */
export function getCachedArticleDetail(id: number): ArticleDetailVO | undefined {
  return detailCache.get(id)
}

/** 取详情：命中在途请求则复用，成功后写入缓存。 */
export function fetchArticleDetail(id: number): Promise<ArticleDetailVO> {
  const existing = inflight.get(id)
  if (existing) return existing

  const p = getArticleDetail(id)
    .then((data) => {
      detailCache.set(id, data)
      return data
    })
    .finally(() => {
      inflight.delete(id)
    })

  inflight.set(id, p)
  return p
}

/** 预取：已缓存或已在途则跳过；静默拉取，忽略错误。 */
export function prefetchArticleDetail(id: number): void {
  if (detailCache.has(id) || inflight.has(id)) return
  void fetchArticleDetail(id).catch(() => {})
}
