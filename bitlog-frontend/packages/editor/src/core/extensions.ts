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
  ]
}

// GFM tables require no blank lines between rows; strip them before parsing.
export function normalizeMarkdown(md: string): string {
  return repairTables(stripTableRowBlankLines(md))
}

function stripTableRowBlankLines(md: string): string {
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

// GFM table delimiter row: |---|  :---  :---:  ---:  (with optional spaces).
const TABLE_DELIMITER_RE = /^\s*\|?\s*:?-+:?\s*(\|\s*:?-+:?\s*)+\|?\s*$/

function splitCells(line: string): string[] {
  const trimmed = line.replace(/^\s*\|/, '').replace(/\|\s*$/, '')
  if (trimmed === '') return []
  return trimmed.split('|').map(c => c.trim())
}

function padRowToCols(cells: string[], cols: number, fill = ''): string[] {
  if (cells.length < cols) return [...cells, ...Array(cols - cells.length).fill(fill)]
  if (cells.length > cols) return cells.slice(0, cols)
  return cells
}

// Three-state scanner: SCAN → SAW_HEADER → IN_TABLE → emit → SCAN.
// Goal: repair malformed GFM tables (header/delimiter blank gap, missing columns)
// without promoting unrelated pipe-looking paragraphs.
type State = 'SCAN' | 'SAW_HEADER' | 'IN_TABLE'

export function repairTables(md: string): string {
  const lines = md.split('\n')
  const out: string[] = []
  let i = 0
  let state: State = 'SCAN'
  let headerCells: string[] = []
  let tableCols = 0

  while (i < lines.length) {
    const line = lines[i]
    const isPipe = /^\s*\|/.test(line)

    if (state === 'SCAN') {
      if (isPipe) {
        const cells = splitCells(line)
        if (cells.length > 0) {
          headerCells = cells
          state = 'SAW_HEADER'
        } else {
          out.push(line)
        }
      } else {
        out.push(line)
      }
      i++
      continue
    }

    if (state === 'SAW_HEADER') {
      // Skip blank lines between header and delimiter.
      if (line.trim() === '') { i++; continue }
      if (TABLE_DELIMITER_RE.test(line)) {
        const delimCells = splitCells(line)
        tableCols = Math.max(headerCells.length, delimCells.length)
        out.push('| ' + padRowToCols(headerCells, tableCols).join(' | ') + ' |')
        out.push('| ' + Array(tableCols).fill('---').join(' | ') + ' |')
        state = 'IN_TABLE'
      } else {
        // False positive — emit the cached header as plain text, then this line, reset.
        out.push('| ' + headerCells.join(' | ') + ' |')
        out.push(line)
        headerCells = []
        state = 'SCAN'
      }
      i++
      continue
    }

    // IN_TABLE
    if (line.trim() === '') {
      state = 'SCAN'
      out.push(line)
      i++
      continue
    }
    if (!isPipe) {
      state = 'SCAN'
      out.push(line)
      i++
      continue
    }
    const cells = splitCells(line)
    out.push('| ' + padRowToCols(cells, tableCols).join(' | ') + ' |')
    i++
  }

  return out.join('\n')
}
