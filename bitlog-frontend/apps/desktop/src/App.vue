<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import VditorWriter from './components/VditorWriter.vue'
import { open, save } from '@tauri-apps/plugin-dialog'
import { readTextFile, writeTextFile, writeFile } from '@tauri-apps/plugin-fs'
import { convertFileSrc, invoke } from '@tauri-apps/api/core'
import { listen, type UnlistenFn } from '@tauri-apps/api/event'

const content = ref('')
const filePath = ref<string | null>(null)
const editorRef = ref<InstanceType<typeof VditorWriter> | null>(null)
const isDirty = ref(false)
const saveError = ref<string | null>(null)

const displayPath = computed(() => {
  if (!filePath.value) return '未命名'
  return filePath.value.replace(/^\/Users\/[^/]+/, '~')
})

async function openFile() {
  const selected = await open({
    filters: [{ name: 'Markdown', extensions: ['md', 'markdown'] }],
    multiple: false,
  })
  if (!selected || typeof selected !== 'string') return
  await loadPath(selected)
}

async function loadPath(path: string) {
  const text = await readTextFile(path)
  content.value = text
  filePath.value = path
  isDirty.value = false
  saveError.value = null
}

async function saveFile() {
  saveError.value = null
  const md = editorRef.value?.getMarkdown() ?? ''
  if (filePath.value) {
    await writeTextFile(filePath.value, md)
    isDirty.value = false
  } else {
    await saveAs(md)
  }
}

async function saveAs(passedMd?: string) {
  const markdown = passedMd ?? editorRef.value?.getMarkdown() ?? ''
  const path = await save({
    filters: [{ name: 'Markdown', extensions: ['md'] }],
  })
  if (!path) return
  await writeTextFile(path, markdown)
  filePath.value = path
  isDirty.value = false
}

async function uploadImage(file: File): Promise<string> {
  const dir = filePath.value
    ? filePath.value.split('/').slice(0, -1).join('/')
    : null

  if (!dir) {
    // No file on disk yet — embed as base64
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result as string)
      reader.onerror = reject
      reader.readAsDataURL(file)
    })
  }

  const ext = file.name.split('.').pop() || 'png'
  const imgPath = `${dir}/${Date.now()}.${ext}`
  const buffer = await file.arrayBuffer()
  await writeFile(imgPath, new Uint8Array(buffer))
  return convertFileSrc(imgPath)
}

function onEditorChange() {
  isDirty.value = true
}

// Auto-save 1.5s after last change, only when file is already saved to disk
let autoSaveTimer: ReturnType<typeof setTimeout> | null = null
let unlistenOpened: UnlistenFn | null = null
watch(isDirty, (dirty) => {
  if (!dirty || !filePath.value) return
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(() => {
    saveFile().catch((err: unknown) => {
      saveError.value = err instanceof Error ? err.message : '自动保存失败'
    })
  }, 1500)
})

// ⌘O open  ·  ⌘S save  ·  ⌘⇧S save as
function handleKeydown(e: KeyboardEvent) {
  if (!(e.metaKey || e.ctrlKey)) return
  if (e.key === 'o') {
    e.preventDefault()
    openFile()
  } else if (e.key === 's') {
    e.preventDefault()
    if (e.shiftKey) {
      saveAs().catch((err: unknown) => {
        saveError.value = err instanceof Error ? err.message : '保存失败'
      })
    } else {
      saveFile().catch((err: unknown) => {
        saveError.value = err instanceof Error ? err.message : '保存失败'
      })
    }
  }
}

onMounted(async () => {
  window.addEventListener('keydown', handleKeydown)

  // Cold-start: drain URLs buffered by the Rust side before the webview was up.
  const buffered = await invoke<string[]>('opened_urls')
  if (buffered.length > 0) {
    await loadPath(buffered[0])
  }

  // Warm-runtime: handle subsequent `open file.md` invocations.
  unlistenOpened = await listen<string[]>('opened', (event) => {
    const first = event.payload[0]
    if (first) loadPath(first).catch((err: unknown) => {
      saveError.value = err instanceof Error ? err.message : '打开失败'
    })
  })
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  unlistenOpened?.()
})
</script>

<template>
  <div class="layout">
    <!-- Toolbar -->
    <header class="toolbar">
      <div class="toolbar-title" data-tauri-drag-region>
        <span class="file-path">{{ displayPath }}</span>
        <span v-if="isDirty" class="dirty-dot" title="未保存" />
      </div>
      <span v-if="saveError" class="save-error" @click="saveError = null" title="点击关闭">{{ saveError }}</span>
    </header>

    <!-- Editor -->
    <main class="editor-wrap">
      <VditorWriter
        ref="editorRef"
        :content="content"
        :editable="true"
        :upload-image="uploadImage"
        @change="onEditorChange"
      />
    </main>

    <!-- Empty state -->
    <div v-if="!content && !isDirty" class="empty-state">
      <p class="empty-hint">拖入 Markdown 文件，或按 ⌘O 打开</p>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

/* ── Toolbar ──────────────────────────────────────────────────────────────── */
.toolbar {
  display: flex;
  align-items: center;
  height: var(--toolbar-height);
  padding: 0 12px;
  background: var(--toolbar-bg);
  border-bottom: 1px solid var(--toolbar-border);
  flex-shrink: 0;
}

.toolbar-title {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 100%;
}

.file-path {
  font-size: 13px;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
  max-width: 500px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dirty-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-accent);
  flex-shrink: 0;
}

.save-error {
  font-size: 12px;
  color: #e06c75;
  cursor: pointer;
  white-space: nowrap;
  padding: 0 8px;
}

/* ── Editor area ──────────────────────────────────────────────────────────── */
.editor-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 48px max(48px, calc((100vw - 800px) / 2));
  background: transparent;
}

/* ── Empty state ──────────────────────────────────────────────────────────── */
.empty-state {
  position: absolute;
  inset: var(--toolbar-height) 0 0 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.empty-hint {
  font-size: 15px;
  color: var(--color-text-muted);
}
</style>
