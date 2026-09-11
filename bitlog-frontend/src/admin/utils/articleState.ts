import type { ArticleVO } from '@/api/admin/article'

type ArticleState = Pick<ArticleVO, 'publishedVersionId' | 'latestVersionId' | 'publishTime'>

/** 发布状态与尚未上线的修改分别表达，不能把撤下的文章称为首次草稿。 */
export function articleStateLabel(article: ArticleState): string {
  if (article.publishedVersionId === null) return article.publishTime ? '已撤下' : '草稿'
  return '已发布'
}

export function hasUnpublishedChanges(article: ArticleState): boolean {
  return (
    article.publishedVersionId !== null && article.latestVersionId !== article.publishedVersionId
  )
}

export function articlePreviewPath(article: ArticleState & { id: number }): string {
  return article.publishedVersionId === null || hasUnpublishedChanges(article)
    ? `/admin/preview/${article.id}`
    : `/article/${article.id}`
}
