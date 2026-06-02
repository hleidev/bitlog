<script lang="ts">
let mermaidGlobalId = 0
</script>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { NodeViewWrapper, NodeViewContent, nodeViewProps } from '@tiptap/vue-3'
import mermaid from 'mermaid'

const props = defineProps(nodeViewProps)

const copied = ref(false)
const mermaidSvg = ref('')
const mermaidError = ref(false)
const isCursorInside = ref(false)

const language = computed(() => props.node.attrs.language || '')
const isMermaid = computed(() => language.value === 'mermaid')
const displayLang = computed(() => language.value || 'text')

// Show code when: regular block, OR mermaid with cursor inside, OR mermaid not yet rendered
const showCode = computed(() =>
  !isMermaid.value ||
  isCursorInside.value ||
  (!mermaidSvg.value && !mermaidError.value)
)
// Show diagram when: mermaid, cursor outside, and SVG or error is ready
const showDiagram = computed(() =>
  isMermaid.value && !isCursorInside.value && (!!mermaidSvg.value || mermaidError.value)
)

function checkCursor() {
  if (!props.editor.isEditable) {
    isCursorInside.value = false
    return
  }
  const pos = props.getPos()
  if (pos === undefined) return
  const { from, to } = props.editor.state.selection
  isCursorInside.value = from >= pos && to <= pos + props.node.nodeSize
}

function handleDiagramClick() {
  if (!props.editor.isEditable) return
  const pos = props.getPos()
  if (pos === undefined) return
  props.editor.chain().focus().setTextSelection(pos + 1).run()
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(props.node.textContent)
    copied.value = true
    setTimeout(() => (copied.value = false), 2000)
  } catch {}
}

async function renderMermaid() {
  if (!isMermaid.value || !props.node.textContent.trim()) {
    mermaidSvg.value = ''
    return
  }
  try {
    const id = `mermaid-render-${++mermaidGlobalId}`
    const { svg } = await mermaid.render(id, props.node.textContent)
    mermaidSvg.value = svg.replace(/max-width:\s*[\d.]+px;?\s*/g, '')
    mermaidError.value = false
  } catch {
    mermaidError.value = true
    mermaidSvg.value = ''
  }
}

onMounted(() => {
  props.editor.on('selectionUpdate', checkCursor)
  checkCursor()
  renderMermaid()
})

onUnmounted(() => {
  props.editor.off('selectionUpdate', checkCursor)
})

watch(isMermaid, (is) => { if (is) renderMermaid(); else mermaidSvg.value = '' })
watch(() => props.node.textContent, renderMermaid, { flush: 'post' })
</script>

<template>
  <node-view-wrapper class="code-block-wrapper">

    <!-- Header: always for regular code; mermaid only when cursor inside (editing) -->
    <div v-if="!isMermaid || isCursorInside" class="code-header" contenteditable="false">
      <input
        v-if="editor.isEditable"
        class="code-lang-input"
        :value="displayLang"
        @change="props.updateAttributes({ language: ($event.target as HTMLInputElement).value.trim() || 'text' })"
        @keydown.enter.prevent="($event.target as HTMLInputElement).blur()"
        @click.stop
        @mousedown.stop
        spellcheck="false"
        autocomplete="off"
      />
      <span v-else class="code-lang-label">{{ displayLang }}</span>
      <button v-if="!isMermaid" class="copy-btn" @click.prevent="copyCode">
        {{ copied ? '已复制 ✓' : '复制' }}
      </button>
    </div>

    <!-- NodeViewContent must stay mounted; hide visually when showing diagram -->
    <pre v-show="showCode" class="code-body"><node-view-content as="code" /></pre>

    <!-- Mermaid diagram: shown when cursor is outside; click to enter edit mode -->
    <div
      v-if="showDiagram"
      class="mermaid-preview"
      :class="{ 'mermaid-preview--clickable': editor.isEditable }"
      @click="handleDiagramClick"
    >
      <div v-if="mermaidSvg" class="mermaid-svg-wrap" v-html="mermaidSvg" />
      <div v-else class="mermaid-error">Mermaid 语法错误，点击编辑代码</div>
    </div>

  </node-view-wrapper>
</template>
