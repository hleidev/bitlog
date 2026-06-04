import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Decoration, DecorationSet } from '@tiptap/pm/view'

// Adds `cursor-inside` CSS class to the heading or blockquote node containing
// the cursor. CSS in prose.css then switches the visual from rendered to source.
export const LiveMarkdownPlugin = Extension.create({
  name: 'liveMarkdown',
  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: new PluginKey('liveMarkdown'),
        props: {
          decorations(state) {
            const { $from } = state.selection
            const decorations: Decoration[] = []
            for (let depth = $from.depth; depth >= 1; depth--) {
              const node = $from.node(depth)
              const name = node.type.name
              if (name === 'heading' || name === 'blockquote') {
                decorations.push(
                  Decoration.node($from.before(depth), $from.after(depth), { class: 'cursor-inside' })
                )
              }
            }
            return DecorationSet.create(state.doc, decorations)
          },
        },
      }),
    ]
  },
})
