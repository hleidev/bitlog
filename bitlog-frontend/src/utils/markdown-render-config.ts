/**
 * 共享 Markdown / Lute 配置
 *
 * 全站写作 (VditorWriter) 与阅读 (ArticleContent) 必须用同一份配置，
 * 才能保证「写出来什么样 = 读出来什么样」。
 *
 * 关键开关（已验证，关掉任何一个都会让 Lute 改写作者原文）：
 *  - autoSpace:false         Lute 默认会自动在中英文之间加空格
 *  - fixTermTypo:false       Lute 默认会改写中文拼写
 *  - sanitize:true           XSS 防护（用 Lute.Sanitize() 做白名单过滤）
 *
 * 其它 GFM 特性按需开关，与项目历史 CommonMark + GFM 范围对齐。
 */
import type { LuteInstance } from './lute-types'

export interface MarkdownRenderOptions {
  gfmAutoLink: boolean
  gfmTable: boolean
  gfmTaskListItem: boolean
  gfmStrikethrough: boolean
  footnotes: boolean
  toC: boolean
  mark: boolean
  autoSpace: boolean
  fixTermTypo: boolean
  sanitize: boolean
  codeSyntaxHighlight: boolean
  headingID: boolean
  headingAnchor: boolean
}

export const SHARED_MARKDOWN_OPTIONS: MarkdownRenderOptions = {
  gfmAutoLink: true,
  gfmTable: true,
  gfmTaskListItem: true,
  gfmStrikethrough: true,
  footnotes: true,
  toC: true,
  mark: true,
  // ── 关键：关掉自动改写 ──────────────────────────────────────────────────────
  autoSpace: false,
  fixTermTypo: false,
  // ── 安全阀 ────────────────────────────────────────────────────────────────
  sanitize: true,
  // ── 代码块 ────────────────────────────────────────────────────────────────
  // 关闭 Lute 内置 Chroma 高亮：Chroma 会给 <code> 注入浅色内联 background，
  // 与 .code-block-wrapper 的深色背景冲突。读取侧改用 highlight.js 客户端渲染。
  codeSyntaxHighlight: false,
  headingID: true,
  headingAnchor: true,
}

/**
 * 把共享配置套到 Lute 实例上。
 * Vditor 内部用 Lute 渲染时也调这个，保证两侧配置一致。
 */
export function applySharedLuteConfig(lute: LuteInstance): void {
  lute.SetGFMAutoLink(SHARED_MARKDOWN_OPTIONS.gfmAutoLink)
  lute.SetGFMTable(SHARED_MARKDOWN_OPTIONS.gfmTable)
  lute.SetGFMTaskListItem(SHARED_MARKDOWN_OPTIONS.gfmTaskListItem)
  lute.SetGFMStrikethrough(SHARED_MARKDOWN_OPTIONS.gfmStrikethrough)
  lute.SetFootnotes(SHARED_MARKDOWN_OPTIONS.footnotes)
  lute.SetToC(SHARED_MARKDOWN_OPTIONS.toC)
  lute.SetMark(SHARED_MARKDOWN_OPTIONS.mark)
  lute.SetAutoSpace(SHARED_MARKDOWN_OPTIONS.autoSpace)
  lute.SetFixTermTypo(SHARED_MARKDOWN_OPTIONS.fixTermTypo)
  lute.SetSanitize(SHARED_MARKDOWN_OPTIONS.sanitize)
  lute.SetCodeSyntaxHighlight(SHARED_MARKDOWN_OPTIONS.codeSyntaxHighlight)
  lute.SetHeadingID(SHARED_MARKDOWN_OPTIONS.headingID)
  lute.SetHeadingAnchor(SHARED_MARKDOWN_OPTIONS.headingAnchor)
}
