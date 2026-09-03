/**
 * Lute 引擎的最小类型声明
 *
 * Lute 是 Vditor 内嵌的 Go→WASM Markdown 引擎 (3.8MB gzip)。
 * 完整 API 在 vditor/dist/js/lute/lute.min.js 里通过 global.Lute 暴露。
 * 这里只声明本项目用到的子集，够覆盖写作 + 阅读路径。
 *
 * 参考：https://ld246.com/article/1588412297062
 */
export interface LuteNamespace {
  New(): LuteInstance
  /** 静态 sanitize：strip <script>、javascript: 等 XSS 载荷 */
  Sanitize(html: string): string
}

export interface LuteInstance {
  // ── Markdown → HTML ──
  Md2HTML(md: string): string

  // ── 双向 IR DOM（VditorWriter 用） ──
  SpinVditorIRDOM(md: string): unknown
  VditorIRDOM2HTML(dom: unknown): string
  VditorIRDOM2Md(dom: unknown): string
  Md2VditorIRDOM(md: string): unknown
  HTML2VditorIRDOM(html: string): unknown

  // ── GFM / 行为开关 ──
  SetGFMAutoLink(v: boolean): void
  SetGFMTable(v: boolean): void
  SetGFMTaskListItem(v: boolean): void
  SetGFMStrikethrough(v: boolean): void
  SetGFMStrikethrough1(v: boolean): void
  SetFootnotes(v: boolean): void
  SetToC(v: boolean): void
  SetMark(v: boolean): void
  SetAutoSpace(v: boolean): void
  SetFixTermTypo(v: boolean): void
  SetSanitize(v: boolean): void
  SetCodeSyntaxHighlight(v: boolean): void
  SetCodeSyntaxHighlightLineNum(v: boolean): void
  SetCodeSyntaxHighlightInlineStyle(v: boolean): void
  SetCodeSyntaxHighlightStyleName(v: string): void
  SetHeadingID(v: boolean): void
  SetHeadingAnchor(v: boolean): void
  SetVditorIR(v: boolean): void
  SetVditorWYSIWYG(v: boolean): void
  SetVditorSV(v: boolean): void
  SetInlineMath(v: boolean): void
  SetEmoji(v: boolean): void
  SetImgPathAllowSpace(v: boolean): void
  SetLinkPrefix(prefix: string): void
  SetLinkBase(base: string): void
  SetImageLazyLoading(v: boolean): void
}
