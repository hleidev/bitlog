import { VueNodeViewRenderer, VueMarkViewRenderer } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import { Markdown } from '@tiptap/markdown'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import { Table, TableRow, TableCell, TableHeader } from '@tiptap/extension-table'
import { createLowlight, common } from 'lowlight'
import CodeBlockView from '../CodeBlockView.vue'
import ImageNodeView from '../ImageNodeView.vue'
import LinkMarkView from '../LinkMarkView.vue'
import { LiveMarkdownPlugin } from '../extensions/liveMarkdownPlugin'

const lowlight = createLowlight(common)

export interface ExtensionOptions {
  allowBase64?: boolean
}

export function createExtensions(options: ExtensionOptions = {}) {
  const { allowBase64 = false } = options
  return [
    StarterKit.configure({ codeBlock: false }),
    Table.configure({ resizable: false }),
    TableRow,
    TableCell,
    TableHeader,
    Link.configure({ openOnClick: false })
      .extend({ addMarkView() { return VueMarkViewRenderer(LinkMarkView) } }),
    Image.configure({ allowBase64 })
      .extend({ addNodeView() { return VueNodeViewRenderer(ImageNodeView) } }),
    Markdown,
    CodeBlockLowlight
      .extend({ addNodeView() { return VueNodeViewRenderer(CodeBlockView) } })
      .configure({ lowlight }),
    LiveMarkdownPlugin,
  ]
}

// GFM tables require no blank lines between rows; strip them before parsing.
export function normalizeMarkdown(md: string): string {
  const lines = md.split('\n')
  const result: string[] = []
  for (let i = 0; i < lines.length; i++) {
    const prev = result[result.length - 1] ?? ''
    const next = lines[i + 1] ?? ''
    if (lines[i].trim() === '' && prev.trimStart().startsWith('|') && next.trimStart().startsWith('|')) {
      continue
    }
    result.push(lines[i])
  }
  return result.join('\n')
}
