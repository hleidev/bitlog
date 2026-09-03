import {
  getArticlePage,
  type ArticleDetailVO,
  type ArticleItemVO,
  type ArticleLink,
} from './article'

const RELATED_COUNT = 3
/** 候选池取最新 N 篇。超出这个量级就该由后端出相关接口了，见 API_CLEANUP.md */
const CANDIDATE_POOL = 100

// 纯内存缓存：预渲染阶段整个构建共用一次请求（vite-ssg 单进程，模块只初始化一次），
// 浏览器里则是整个 SPA 会话共用一次，刷新即清空。
let poolPromise: Promise<ArticleItemVO[]> | null = null

function loadPool(): Promise<ArticleItemVO[]> {
  poolPromise ??= getArticlePage({ pageNum: 1, pageSize: CANDIDATE_POOL })
    .then((res) => res.content)
    .catch((err) => {
      poolPromise = null
      throw err
    })
  return poolPromise
}

/**
 * 「继续阅读」的三篇：按与当前文章的标签重合度排序，同分类加一点权重，
 * 完全打平时才落到发布时间上。
 *
 * 早先的做法是「同分类里最新的三篇」，那等于按时间排序：读最新的文章时推荐的
 * 永远更早，读旧文章时又永远更新，跟相关性无关。
 */
export async function getRelatedArticles(current: ArticleDetailVO): Promise<ArticleLink[]> {
  const pool = await loadPool()
  const currentTags = new Set(current.tags.map((t) => t.id))

  return pool
    .filter((a) => a.id !== current.id)
    .map((a) => ({
      article: a,
      score:
        a.tags.filter((t) => currentTags.has(t.id)).length * 2 +
        (a.category && current.category && a.category.id === current.category.id ? 1 : 0),
    }))
    .sort(
      (x, y) =>
        y.score - x.score || Date.parse(y.article.publishTime) - Date.parse(x.article.publishTime),
    )
    .slice(0, RELATED_COUNT)
    .map(({ article }) => ({
      id: article.id,
      title: article.title,
      publishTime: article.publishTime,
    }))
}
