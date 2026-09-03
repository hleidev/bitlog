/**
 * 写作页 → 预览页的内容交接
 *
 * 预览的语义是「看我编辑器里现在的样子」，含未保存的部分，所以不能让预览页去服务器
 * 拉草稿。写作页在点预览时把当前标题/正文放进 localStorage，预览页读出来用。
 *
 * 用 localStorage 而不是 sessionStorage：新标签页对 sessionStorage 只是开启时的一份
 * 拷贝，且 noopener / COOP 下根本不拷贝，不可靠。
 *
 * 只保留一条记录（同时只会有一个预览意图），payload 里带 id 做校验：预览了 A 又预览 B
 * 之后刷新 A 的标签页，id 不匹配就回退到服务器内容，不会串文章。
 */

const KEY = 'bitlog:preview'
const TTL_MS = 5 * 60 * 1000

export type PreviewHandoffId = number | 'new'

interface PreviewHandoff {
  id: PreviewHandoffId
  title: string
  content: string
  ts: number
}

export function writePreviewHandoff(id: PreviewHandoffId, title: string, content: string) {
  try {
    const payload: PreviewHandoff = { id, title, content, ts: Date.now() }
    localStorage.setItem(KEY, JSON.stringify(payload))
  } catch {
    // 容量满 / 隐私模式 —— 静默降级为「预览服务器上的草稿」
  }
}

export function readPreviewHandoff(id: PreviewHandoffId): PreviewHandoff | null {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return null
    const data = JSON.parse(raw) as PreviewHandoff
    if (data.id !== id) return null
    if (Date.now() - data.ts > TTL_MS) return null
    return data
  } catch {
    return null
  }
}
