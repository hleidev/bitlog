<script lang="ts">
let mermaidGlobalId = 0
</script>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { NodeViewWrapper, NodeViewContent, nodeViewProps } from '@tiptap/vue-3'
import mermaid from 'mermaid'

const props = defineProps(nodeViewProps)

const copied = ref(false)
const mermaidSvg = ref('')
const mermaidError = ref(false)

const language = computed(() => props.node.attrs.language || '')
const isMermaid = computed(() => language.value === 'mermaid')
const displayLang = computed(() => language.value || 'text')

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
  }
}

// Re-render when language switches to mermaid, or when code content changes
watch(isMermaid, (is) => { if (is) renderMermaid(); else mermaidSvg.value = '' })
watch(() => props.node.textContent, renderMermaid, { flush: 'post' })
onMounted(renderMermaid)
</script>

<template>
  <node-view-wrapper class="code-block-wrapper">

    <div class="code-header" contenteditable="false">
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

    <pre class="code-body"><node-view-content as="code" /></pre>

    <!-- Mermaid live preview: auto-renders below the code as you type -->
    <div v-if="isMermaid && (mermaidSvg || mermaidError)" class="mermaid-preview">
      <div v-if="mermaidSvg" class="mermaid-svg-wrap" v-html="mermaidSvg" />
      <div v-else class="mermaid-error">Mermaid 语法错误，请检查代码</div>
    </div>

  </node-view-wrapper>
</template>
