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

export const formatRelativeTime = (iso: string | null | undefined): string => {
  if (!iso) return ''
  const elapsed = Math.max(0, Date.now() - new Date(iso).getTime())
  const minutes = Math.floor(elapsed / 60_000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days} 天前`
  return formatDate(iso)
}
