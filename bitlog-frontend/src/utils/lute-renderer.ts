/**
 * Lute 渲染器：Markdown → HTML
 *
 * 单一入口：renderMarkdownToHtml(md) 返回 sanitized HTML string。
 * 内部懒加载 Lute WASM (~3.8MB gzip)，整个应用只加载一次。
 *
 * 输出 HTML 结构（与项目历史 Tiptap 渲染的 .ProseMirror 输出对齐，
 * 这样 prose.css 现有样式不用改）：
 *   <pre><code class="language-xxx">CODE</code></pre>      代码块
 *   <div class="language-mermaid">CODE</div>              Mermaid 块
 *   其余元素：标准 HTML，与 ProseMirror 输出等价
 *
 * 安全：Lute 内部 Sanitize + SetSanitize:true 双层白名单过滤 XSS 载荷。
 *
 * SSR：vite-ssg 静态预渲染时也走这里；Lute 是纯客户端 WASM，
 * SSR 阶段 Lute 未加载会降级返回原始 Markdown 包在 <pre> 里。
 */
import { applySharedLuteConfig, SHARED_MARKDOWN_OPTIONS } from './markdown-render-config'
import type { LuteInstance, LuteNamespace } from './lute-types'

let _lute: LuteInstance | null = null
let _loading: Promise<LuteNamespace> | null = null

async function loadLute(): Promise<LuteNamespace> {
  if (_lute) return Promise.resolve((globalThis as unknown as { Lute: LuteNamespace }).Lute)
  if (_loading) return _loading
  _loading = (async () => {
    // 动态 import：包内文件路径以 .min.js 结尾；Lute 暴露到 globalThis.Lute
    // （Vditor 3.11.2 的 lute.min.js 是 Go→WASM 自执行脚本）
    await import('vditor/dist/js/lute/lute.min.js')
    const Lute = (globalThis as unknown as { Lute: LuteNamespace }).Lute
    if (!Lute) {
      throw new Error('Lute WASM 加载失败：globalThis.Lute 未定义')
    }
    const inst = Lute.New()
    applySharedLuteConfig(inst)
    _lute = inst
    return Lute
  })()
  return _loading
}

/**
 * 异步入口（推荐）：懒加载 Lute 并渲染。
 * 重试已加载的实例，零额外开销。
 */
export async function renderMarkdownToHtml(md: string): Promise<string> {
  await loadLute()
  const normalized = normalizeForLute(md)
  const raw = _lute!.Md2HTML(normalized)
  return postProcessHtml(raw)
}

/**
 * 同步入口：仅在 Lute 已加载过时可用。
 */
export function tryRenderMarkdownToHtml(md: string): string | null {
  if (!_lute) return null
  const raw = _lute.Md2HTML(normalizeForLute(md))
  return postProcessHtml(raw)
}

/**
 * Lute 与 Tiptap ProseMirror 在 GFM table 上的差异：
 *
 * - Tiptap: 允许表行间有空行
 * - Lute:   严格要求表行连续（GFM spec 行为）
 *
 * 之前在 @bitlog/editor 的 extensions.ts 里 `normalizeMarkdown` /
 * `stripTableRowBlankLines` 处理。包删除后这里接管。
 *
 * 去掉"两行 pipe 之间"的空行，让 Lute 正确解析为 <table>。
 */
function normalizeForLute(md: string): string {
  return stripTableRowBlankLines(md)
}

function stripTableRowBlankLines(md: string): string {
  const lines = md.split('\n')
  const out: string[] = []
  for (let i = 0; i < lines.length; i++) {
    const prev = out[out.length - 1] ?? ''
    const next = lines[i + 1] ?? ''
    if (
      lines[i].trim() === '' &&
      prev.trimStart().startsWith('|') &&
      next.trimStart().startsWith('|')
    ) {
      continue
    }
    out.push(lines[i])
  }
  return out.join('\n')
}

/**
 * 一些 Lute 输出的清理/规范化：
 *
 * 1. 任务列表：Lute 输出 <input type="checkbox"> 无 value，
 *    而低版本/高亮区会把 <input> 渲染成 form 控件。加 data-task
 *    标识便于 CSS / 后续处理。
 *
 * 2. 段落规范化：Lute 有时会在 block 之间输出多余空行，无害，
 *    不处理。
 */
function postProcessHtml(html: string): string {
  return html
    .replace(
      /<li class="vditor-task vditor-task--done"><input checked="" disabled="" type="checkbox" \/>/g,
      '<li class="vditor-task vditor-task--done" data-task="done"><input checked="" disabled="" type="checkbox" />',
    )
    .replace(
      /<li class="vditor-task"><input disabled="" type="checkbox" \/>/g,
      '<li class="vditor-task" data-task="todo"><input disabled="" type="checkbox" />',
    )
}

export { SHARED_MARKDOWN_OPTIONS }
