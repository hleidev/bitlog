/**
 * useLocalDraft — 写作页的 localStorage 本地兜底草稿
 *
 * 独立于"保存草稿"按钮:Vditor cache 已关,用户忘记手动保存时防丢字。
 * 键名按 articleId(或 "new") 隔离,避免不同文章覆盖。
 */

export interface LocalDraftPayload {
  title: string
  content: string
  ts: number
}

const KEY_PREFIX = 'bitlog:autosave:'
const DEBOUNCE_MS = 800
const TTL_MS = 7 * 24 * 3600 * 1000 // 7 天过期

export function useLocalDraft(
  id: number | 'new',
  getSnapshot: () => { title: string; content: string },
) {
  const key = `${KEY_PREFIX}${id}`
  let timer: ReturnType<typeof setTimeout> | undefined

  function write() {
    const { title, content } = getSnapshot()
    if (id === 'new' && !title.trim() && !content.trim()) return
    const payload: LocalDraftPayload = { title, content, ts: Date.now() }
    try {
      localStorage.setItem(key, JSON.stringify(payload))
    } catch {
      // localStorage 满 / 隐私模式不可用 —— 静默
    }
  }

  function read(): LocalDraftPayload | null {
    try {
      const raw = localStorage.getItem(key)
      if (!raw) return null
      const data = JSON.parse(raw) as LocalDraftPayload
      if (Date.now() - data.ts > TTL_MS) return null
      return data
    } catch {
      return null
    }
  }

  function clear() {
    try {
      localStorage.removeItem(key)
    } catch {
      /* noop */
    }
  }

  /** 输入防抖后写入(编辑器/标题 change 时调用) */
  function schedule() {
    clearTimeout(timer)
    timer = setTimeout(write, DEBOUNCE_MS)
  }

  function stop() {
    clearTimeout(timer)
  }

  return { write, read, clear, schedule, stop }
}

export function formatRelative(ts: number): string {
  const diff = Date.now() - ts
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return `${m} 分钟前`
  const h = Math.floor(diff / 3600000)
  if (h < 24) return `${h} 小时前`
  return `${Math.floor(diff / 86400000)} 天前`
}
