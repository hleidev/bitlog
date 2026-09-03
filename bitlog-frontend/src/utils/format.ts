export const formatDate = (iso: string | null | undefined): string => (iso ? iso.slice(0, 10) : '')

/**
 * 后台列表页时间列的统一格式：YYYY-MM-DD HH:mm。
 *
 * 六个页面原本有三套写法：分类/标签直接渲染原始 ISO（2026-05-16T14:57:13Z），
 * 评论/友链各写一份 slice+replace，文章/用户显示「3 天前」。.col-time 的 150px
 * 正是按这个格式定的宽。
 */
export const formatDateTime = (iso: string | null | undefined): string =>
  iso ? iso.slice(0, 16).replace('T', ' ') : '—'

/** 列表侧的紧凑发布时间：YYYY.MM。 */
export const formatYearMonth = (iso: string | null | undefined): string => {
  if (!iso) return '—'
  const d = new Date(iso)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}`
}
