<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { MilkdownProvider } from '@milkdown/vue'
import '@milkdown/crepe/theme/classic.css'
import '@milkdown/crepe/theme/common/style.css'
import '@/assets/styles/prose.css'
import MilkdownEditor from '@/admin/components/MilkdownEditor.vue'
import { getArticleDraft } from '@/api/admin/article'

const route = useRoute()
const articleId = Number(route.params.id)

const title = ref('')
const content = ref('')
const loading = ref(true)
const titleRef = ref<HTMLTextAreaElement | null>(null)

function autoResizeTitle() {
  const el = titleRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

onMounted(async () => {
  try {
    const data = await getArticleDraft(articleId)
    title.value = data.title
    content.value = data.content
  } catch (err) {
    console.error('Failed to load article:', err)
  } finally {
    loading.value = false
    await nextTick()
    autoResizeTitle()
  }
})
</script>

<template>
  <MilkdownProvider>
    <div class="write-v2">
      <div v-if="loading" class="loading-state">
        <span class="loading-dot" />
        <span>加载中...</span>
      </div>

      <template v-else>
        <div class="editor-header">
          <textarea
            ref="titleRef"
            v-model="title"
            class="title-input"
            placeholder="文章标题..."
            maxlength="200"
            rows="1"
            @input="autoResizeTitle"
          />
        </div>

        <div class="editor-container">
          <MilkdownEditor :content="content" />
        </div>
      </template>
    </div>
  </MilkdownProvider>
</template>

<style scoped>
.write-v2 {
  margin: -24px;
  height: calc(100vh - var(--admin-header-height));
  background: #fff;
  overflow-y: auto;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 200px;
  color: #9ca3af;
  font-size: 14px;
}

.loading-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--admin-accent);
  animation: pulse 1.2s infinite ease-in-out;
}

@keyframes pulse {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40%           { transform: scale(1);   opacity: 1; }
}

.editor-header {
  padding: 28px 64px 16px;
}

.title-input {
  display: block;
  width: 100%;
  max-width: 860px;
  margin: 0 auto;
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  border: none;
  outline: none;
  background: transparent;
  line-height: 1.3;
  padding: 0;
  font-family: inherit;
  word-wrap: break-word;
  overflow-wrap: break-word;
  resize: none;
  overflow: hidden;
}

.title-input::placeholder {
  color: #d1d5db;
}

.editor-container {
  padding: 0 64px 60px;
}

/* ── Milkdown: 把编辑容器接入 prose 主题 ────────────────────────────────────── */
:deep(.milkdown) {
  --crepe-color-background: #fff;
  --crepe-color-primary: var(--admin-accent, #b85c38);
  --crepe-color-border: #e8e4de;
  --crepe-color-hover: #ece9e4;
  font-family: inherit;
}

/* 让 .editor 继承 prose.css 里的所有规则 */
:deep(.editor) {
  outline: none;
  min-height: 400px;
  max-width: 860px;
  margin: 0 auto;
  padding: 0;
}

/* prose.css 的选择器是 .prose h1 等，在编辑器里把 .editor 当作 .prose */
:deep(.editor h1) { font-family: var(--font-serif); font-size: 26px; font-weight: 500; color: var(--color-text-primary); margin: 48px 0 18px; padding-bottom: 12px; border-bottom: 1px solid var(--color-border); }
:deep(.editor h2) { font-family: var(--font-serif); font-size: 21px; font-weight: 500; color: var(--color-text-primary); margin: 40px 0 16px; padding-bottom: 10px; border-bottom: 1px solid var(--color-border); }
:deep(.editor h3) { font-size: 17px; font-weight: 600; color: var(--color-text-primary); margin: 28px 0 12px; }
:deep(.editor h4) { font-size: 15px; font-weight: 600; color: var(--color-text-primary); margin: 22px 0 10px; }
:deep(.editor p) { font-size: 16px; line-height: 1.85; color: var(--color-text-secondary); margin-bottom: 18px; }
:deep(.editor strong) { font-weight: 600; color: var(--color-text-primary); }
:deep(.editor em) { font-style: italic; color: var(--color-text-muted); }
:deep(.editor a) { color: var(--color-accent); text-decoration: underline; text-decoration-color: rgba(184, 92, 56, 0.35); text-underline-offset: 3px; }
:deep(.editor code) { font-family: var(--font-mono); font-size: 0.875em; background: var(--color-bg-hover); color: var(--color-accent-light); padding: 2px 6px; border-radius: 4px; border: 1px solid var(--color-border); }
:deep(.editor pre) { background: #282828; border-radius: 4px; padding: 20px 24px; overflow-x: auto; margin: 24px 0; font-family: var(--font-mono); font-size: 13.5px; line-height: 1.65; color: #fff; }
:deep(.editor pre code) { background: none; color: inherit; padding: 0; border: none; font-size: inherit; }
:deep(.prose-code-wrap .prose-code-block) { padding: 46px 22px 20px; margin: 0; }
:deep(.editor blockquote) { border-left: 2px solid var(--color-accent); margin: 28px 0; padding: 14px 20px; background: rgba(184, 92, 56, 0.04); position: relative; }
:deep(.editor blockquote::before) { display: none; }
:deep(.editor blockquote p) { margin: 0; color: var(--color-text-muted); font-style: italic; }
:deep(.editor ul), :deep(.editor ol) { padding-left: 24px; margin-bottom: 18px; }
:deep(.editor li) { margin-bottom: 4px; line-height: 1.75; }
:deep(.editor li p) { margin: 0 !important; padding: 0 !important; }
:deep(.editor ul li) { list-style: disc; }
:deep(.editor ol li) { list-style: decimal; }
:deep(.editor hr) { border: none; border-top: 1px solid var(--color-border); margin: 40px 0; }
:deep(.editor img) { max-width: 100%; border-radius: 4px; margin: 20px 0; }
:deep(.editor table) { width: 100%; border-collapse: collapse; margin: 24px 0; font-size: 14px; border: 1px solid var(--color-border); }
:deep(.editor th) { background: var(--color-bg-hover); color: var(--color-text-primary); font-weight: 600; text-align: left; padding: 10px 16px; border-bottom: 1px solid var(--color-border); }
:deep(.editor td) { padding: 10px 16px; border-bottom: 1px solid var(--color-border); color: var(--color-text-secondary); }
:deep(.editor td p), :deep(.editor th p) { margin: 0 !important; padding: 0 !important; }

:deep(.milkdown .toolbar) {
  border-radius: 8px;
  border: 1px solid #e8e4de;
}

:deep(.milkdown .block-handle) {
  color: #9ca3af;
}

/* ── Mermaid NodeView ──────────────────────────────────────────────────────── */
:deep(.me-mermaid) {
  border: 1px solid var(--color-border, #e8e4de);
  border-radius: 4px;
  margin: 24px 0;
  overflow: hidden;
}

:deep(.me-mermaid__preview) {
  padding: 24px 20px;
  background: #fff;
  text-align: center;
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  cursor: pointer;
}

:deep(.me-mermaid.is-editing) {
  border: 2px solid var(--color-accent, #b85c38);
}

:deep(.me-mermaid__preview svg) {
  pointer-events: none;
  max-width: 100%;
  height: auto;
  display: block;
  margin: 0 auto;
}

:deep(.mermaid-hint) {
  font-size: 12px;
  color: #9ca3af;
  font-family: var(--font-mono);
}

:deep(.mermaid-error) {
  font-size: 12px;
  color: #dc2626;
  font-family: var(--font-mono);
}

/* ── Raw markdown textarea (shared by mermaid + code block edit mode) ──────── */
:deep(.me-raw-editor) {
  display: block;
  width: 100%;
  background: var(--color-bg);
  color: var(--color-text-primary);
  caret-color: var(--color-text-primary);
  font-family: var(--font-mono);
  font-size: 14px;
  line-height: 1.7;
  padding: 14px 0;
  border: none;
  outline: none;
  resize: none;
  box-sizing: border-box;
  min-height: 60px;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: pre;
}

/* ── Code block edit layer ─────────────────────────────────────────────────── */
:deep(.me-code__edit) {
  border-radius: 0;
  overflow: hidden;
  margin: 26px 0;
  border: none;
  border-left: 2px solid var(--color-accent);
  padding-left: 16px;
}

@media (max-width: 768px) {
  .write-v2 { margin: -16px -12px; }
  .editor-header { padding: 24px 20px 16px; }
  .title-input { font-size: 24px; }
  .editor-container { padding: 0 20px 40px; }
}
</style>