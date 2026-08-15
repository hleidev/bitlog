import { writeFileSync } from 'fs'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const PUBLIC_DIR = resolve(__dirname, '../public')
const API_BASE = 'https://api.harrylei.top/api'

/**
 * 友链朋友圈通用格式：每条是一个数组，位置固定。
 * 规范同时兼容三字段与四字段，第三格是对方的友链页地址——本站不收集该字段，故用三字段。
 *   ["站名", "站点地址", "头像地址"]
 * 放在站点根路径而非 API 域：聚合器默认到 https://<站点>/friend.json 找。
 */
function toFcFormat(links) {
  return {
    friends: links.map((link) => [link.name, link.url, link.avatar ?? '']),
  }
}

async function fetchFriendLinks() {
  const res = await fetch(`${API_BASE}/v1/links`)
  if (!res.ok) throw new Error(`API returned ${res.status}`)
  const { data } = await res.json()
  return data ?? []
}

async function main() {
  let links = []
  try {
    links = await fetchFriendLinks()
    console.log(`[friend-json] fetched ${links.length} links`)
  } catch (err) {
    // 友链不是核心内容，取数失败就产出空清单，不该让整个构建挂掉
    console.warn(`[friend-json] failed to fetch links: ${err.message}`)
  }

  writeFileSync(`${PUBLIC_DIR}/friend.json`, JSON.stringify(toFcFormat(links), null, 2), 'utf-8')
  console.log('[friend-json] wrote public/friend.json')
}

main()
