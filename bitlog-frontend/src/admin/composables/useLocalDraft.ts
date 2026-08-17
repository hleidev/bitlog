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
  getId: () => number | 'new',
  getSnapshot: () => { title: string; content: string },
) {
  // id 必须每次现取：新文章保存后会拿到真实 id，键要跟着换，否则后续兜底全写进 "new"
  // 键里，重新打开 /admin/write/{id} 时读不到。
  const keyOf = () => `${KEY_PREFIX}${getId()}`
  let timer: ReturnType<typeof setTimeout> | undefined

  function write() {
    const id = getId()
    const { title, content } = getSnapshot()
    if (id === 'new' && !title.trim() && !content.trim()) return
    const payload: LocalDraftPayload = { title, content, ts: Date.now() }
    try {
      localStorage.setItem(`${KEY_PREFIX}${id}`, JSON.stringify(payload))
    } catch {
      // localStorage 满 / 隐私模式不可用 —— 静默
    }
  }

  function read(): LocalDraftPayload | null {
    try {
      const raw = localStorage.getItem(keyOf())
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
      localStorage.removeItem(keyOf())
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
