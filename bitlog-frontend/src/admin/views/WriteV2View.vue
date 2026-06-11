<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import VditorWriter from '@/admin/components/VditorWriter.vue'
import { uploadFile } from '@/api/file'
import {
  createArticle,
  updateArticleDraft,
  getArticleDraft,
  publishArticle,
  getArticleVersions,
  generateAiMetadata,
  type ArticleVersionVO,
} from '@/api/admin/article'
import { ApiError } from '@/utils/request'
import ArticleMetaDialog from '@/admin/components/ArticleMetaDialog.vue'
import VersionSidebar from '@/admin/components/VersionSidebar.vue'
import { useLocalDraft, formatRelative, type LocalDraftPayload } from '@/admin/composables/useLocalDraft'

const route  = useRoute()
const router = useRouter()

const articleId = route.params.id ? Number(route.params.id) : null
const isNew = articleId === null
const toast = useToast()
const confirm = useConfirm()

// ── Editor state ───────────────────────────────────────────────────────────────
const title      = ref('')
const content    = ref('')
const loading    = ref(true)
const titleRef   = ref<HTMLTextAreaElement | null>(null)
const vditorRef = ref<InstanceType<typeof VditorWriter> | null>(null)

// ── Article metadata ───────────────────────────────────────────────────────────
const latestVersionId    = ref<number | null>(null)
const publishedVersionId    = ref<number | null>(null)
const isPublished           = computed(() => publishedVersionId.value !== null)
// true when a draft has been saved on top of the published version
const hasDraftAbovePublish  = ref(false)

// ── Save state ─────────────────────────────────────────────────────────────────
const saveState  = ref<'idle' | 'saving' | 'saved'>('saved')
const saving     = ref(false)
const hasUnsaved = ref(false)
let saveTimer: ReturnType<typeof setTimeout> | undefined

// LocalStorage 本地兜底草稿(见 useLocalDraft)
const localDraft = useLocalDraft(articleId ?? 'new', () => ({
  title:   title.value,
  content: vditorRef.value?.getMarkdown() ?? content.value,
}))

// ── Sidebar & versions ─────────────────────────────────────────────────────────
const sidebarOpen = ref(false)
const versions    = ref<ArticleVersionVO[]>([])

// ── Publish dialog ─────────────────────────────────────────────────────────────
const publishDialogVisible = ref(false)
const publishing           = ref(false)
const metaDialogRef        = ref<InstanceType<typeof ArticleMetaDialog> | null>(null)

// Article metadata (seeds the dialog on open)
const draftSummary  = ref('')
const draftCategory = ref<{ id: number; name: string } | null>(null)
const draftTags     = ref<{ id: number; name: string }[]>([])

// ── AI recommendation ─────────────────────────────────────────────────────────
const aiGenerating    = ref(false)
const aiSummaryResult = ref<string | null>(null)
const aiCatResult     = ref<{ id: number; name: string } | false | null>(null)
const aiTagsResult    = ref<{ existing: Array<{ id: number; name: string }>; suggested: string[] } | null>(null)

// ── Change suppression flag ────────────────────────────────────────────────────
let suppressChange = true

// ── Helpers ────────────────────────────────────────────────────────────────────
function handleError(err: unknown, fallback = '操作失败') {
  const msg = err instanceof ApiError ? err.message : null
  toast.error(msg || fallback)
}

function autoResizeTitle() {
  const el = titleRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

function onBeforeUnload(e: BeforeUnloadEvent) {
  if (hasUnsaved.value) {
    e.preventDefault()
    e.returnValue = true
  }
}

// ── Load data ──────────────────────────────────────────────────────────────────
async function loadVersions() {
  try {
    const data = await getArticleVersions(articleId)
    versions.value = data
    const latest = data.find(v => v.latest)
    if (latest) latestVersionId.value = latest.id
  } catch {
    // non-critical
  }
}

async function loadDraft() {
  if (isNew) {
    loading.value = false
    saveState.value = 'saved'
    await nextTick()
    autoResizeTitle()
    suppressChange = false
    // 新建草稿:检查 localStorage 是否有未确认的内容,提示恢复
    const cached = localDraft.read()
    if (cached && (cached.title.trim() || cached.content.trim())) {
      await maybeRestoreFromCache(cached, null)
    }
    return
  }

  loading.value = true
  suppressChange = true
  try {
    const data = await getArticleDraft(articleId!)
    title.value              = data.title
    content.value            = data.content
    latestVersionId.value       = data.latestVersionId
    publishedVersionId.value    = data.publishedVersionId
    hasDraftAbovePublish.value  = data.publishedVersionId !== null && data.latestVersionId !== data.publishedVersionId
    draftSummary.value  = data.summary ?? ''
    draftCategory.value = data.category ?? null
    draftTags.value     = data.tags
    await loadVersions()
    saveState.value = 'saved'
    // 已有文章:localStorage 草稿比服务器版更新 → 提示恢复
    const cached = localDraft.read()
    if (cached && (cached.title.trim() || cached.content.trim()) &&
        (cached.title !== data.title || cached.content !== data.content)) {
      await maybeRestoreFromCache(cached, data.updateTime)
    }
  } catch (err) {
    handleError(err, '加载文章失败')
  } finally {
    loading.value = false
    await nextTick()
    autoResizeTitle()
    suppressChange = false
  }
}

async function maybeRestoreFromCache(
  cached: LocalDraftPayload,
  serverUpdateTime: string | null,
) {
  // 服务器版比 localStorage 还新 → 忽略缓存
  if (serverUpdateTime) {
    const serverTs = new Date(serverUpdateTime).getTime()
    if (serverTs > cached.ts) {
      localDraft.clear()
      return
    }
  }
  const ago = formatRelative(cached.ts)
  try {
    await confirm(
      `检测到 ${ago} 的本地未保存草稿,是否恢复？\n（选择「忽略」将清除本地草稿）`,
      '恢复本地草稿',
      { confirmText: '恢复', cancelText: '忽略' },
    )
    title.value   = cached.title
    content.value = cached.content
    vditorRef.value?.setMarkdown(cached.content)
    hasUnsaved.value = true
    saveState.value  = 'idle'
    await nextTick(); autoResizeTitle()
    toast.success('已恢复本地草稿')
  } catch {
    localDraft.clear()
  }
}

// ── Save ───────────────────────────────────────────────────────────────────────
async function performSave() {
  if (!title.value.trim() || saving.value) return
  saving.value    = true
  saveState.value = 'saving'
  try {
    const md = vditorRef.value?.getMarkdown() ?? ''
    if (isNew) {
      const newId = await createArticle({ title: title.value, content: md })
      // 切换到带 id 的路由:清掉 "new" 键,新 key 由后续写入建立
      localDraft.clear()
      router.replace(`/admin/write/${newId}`)
      saveState.value  = 'saved'
      hasUnsaved.value = false
      toast.success('已保存')
    } else {
      await updateArticleDraft(articleId!, { title: title.value, content: md })
      await loadVersions()
      localDraft.clear()
      saveState.value  = 'saved'
      hasUnsaved.value = false
      if (publishedVersionId.value !== null) hasDraftAbovePublish.value = true
      toast.success('已保存')
    }
    clearTimeout(saveTimer)
    saveTimer = setTimeout(() => {
      if (saveState.value === 'saved') saveState.value = 'idle'
    }, 3000)
  } catch (err) {
    saveState.value = 'idle'
    handleError(err, '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSave() {
  if (!title.value.trim()) {
    toast.warning('请先输入文章标题')
    return
  }
  await performSave()
}

// ── Preview ────────────────────────────────────────────────────────────────────
function openPreview() {
  if (isNew) {
    toast.warning('请先保存文章')
    return
  }
  // 有未发布的草稿内容 → 预览草稿页；内容一致或从未发布过 → 预览公开页
  const hasUnpublishedChanges = latestVersionId.value !== null &&
    latestVersionId.value !== publishedVersionId.value

  if (hasUnpublishedChanges) {
    window.open(`/admin/preview/${articleId}`, '_blank')
  } else {
    window.open(`/article/${articleId}`, '_blank')
  }
}

// ── Publish ────────────────────────────────────────────────────────────────────
async function openPublishDialog() {
  if (!title.value.trim()) {
    toast.warning('请先输入文章标题')
    return
  }
  const md = vditorRef.value?.getMarkdown() ?? ''
  if (!md.trim()) {
    toast.warning('请先输入文章内容')
    return
  }
  if (isNew) {
    toast.warning('请先保存文章')
    return
  }
  aiSummaryResult.value = null
  aiCatResult.value     = null
  aiTagsResult.value    = null
  aiGenerating.value    = false
  publishDialogVisible.value = true
}

async function handlePublishConfirm(data: {
  summary: string; categoryId: number | null; tagIds: number[]
  category: { id: number; name: string } | null
  tags: { id: number; name: string }[]
}) {
  if (!data.categoryId) {
    toast.warning('请选择文章分类')
    return
  }
  publishing.value = true
  const wasPublished = isPublished.value
  try {
    await publishArticle(articleId, {
      summary:    data.summary || null,
      categoryId: data.categoryId,
      tagIds:     data.tagIds,
    })
    publishedVersionId.value   = latestVersionId.value
    publishDialogVisible.value = false
    hasUnsaved.value           = false
    hasDraftAbovePublish.value = false
    draftSummary.value  = data.summary
    draftCategory.value = data.category
    draftTags.value     = data.tags
    toast.success(wasPublished ? '发布信息已更新' : '文章已发布')
    await loadVersions()
  } catch (err) {
    handleError(err, '发布失败')
  } finally {
    publishing.value = false
  }
}

// ── AI recommendation ─────────────────────────────────────────────────────────
async function runAiRecommend() {
  aiGenerating.value    = true
  aiSummaryResult.value = null
  aiCatResult.value     = null
  aiTagsResult.value    = null
  try {
    const data            = await generateAiMetadata(articleId)
    aiSummaryResult.value = data.summary
    aiCatResult.value     = data.category ?? false
    aiTagsResult.value    = { existing: data.tags, suggested: data.suggestedTags }
  } catch (err) {
    handleError(err, 'AI 推荐失败，请稍后重试')
  } finally {
    aiGenerating.value = false
  }
}

function acceptAiSummary() {
  if (!aiSummaryResult.value) return
  metaDialogRef.value?.setSummary(aiSummaryResult.value)
  aiSummaryResult.value = null
}

function applyAiCategory() {
  if (!aiCatResult.value) return
  metaDialogRef.value?.setCategory(aiCatResult.value as { id: number; name: string })
  aiCatResult.value = null
}

function applyAiExistingTag(tag: { id: number; name: string }) {
  metaDialogRef.value?.addTag(tag)
}

async function applyAiSuggestedTag(name: string) {
  await metaDialogRef.value?.createAndAddTag(name)
}

// ── Watchers ───────────────────────────────────────────────────────────────────
watch(title, () => {
  if (suppressChange) return
  hasUnsaved.value = true
  saveState.value  = 'idle'
  localDraft.schedule()
})

async function uploadImageFn(file: File): Promise<string> {
  const { fileUrl } = await uploadFile(file, 'article')
  return fileUrl
}

function onEditorChange() {
  if (suppressChange) return
  hasUnsaved.value = true
  saveState.value  = 'idle'
  localDraft.schedule()
}

// ── Keyboard shortcut ──────────────────────────────────────────────────────────
function onKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSave()
  }
}

// ── Lifecycle ──────────────────────────────────────────────────────────────────
onMounted(async () => {
  window.addEventListener('beforeunload', onBeforeUnload)
  window.addEventListener('keydown', onKeydown)
  await loadDraft()
})

onUnmounted(() => {
  clearTimeout(saveTimer)
  localDraft.stop()
  window.removeEventListener('beforeunload', onBeforeUnload)
  window.removeEventListener('keydown', onKeydown)
})

onBeforeRouteLeave(async () => {
  if (!hasUnsaved.value) return true
  try {
    await confirm('你有未保存的修改，确认离开吗？', '离开页面', {
      confirmText: '离开',
      cancelText:  '取消',
    })
    return true
  } catch {
    return false
  }
})
</script>

<template>
  <div class="write-v2">

      <!-- Loading state -->
      <div v-if="loading" class="loading-state">
        <span class="loading-dot" />
        <span>加载中...</span>
      </div>

      <template v-else>

        <!-- ── Toolbar ─────────────────────────────────────────────────────── -->
        <div class="write-toolbar">
          <div class="tb-left">
            <button class="back-btn" @click="router.push('/admin/articles')">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
              <span>文章列表</span>
            </button>
            <div class="tb-sep" />
            <span
              class="status-pill"
              :class="hasUnsaved ? 'status-pill--unsaved' : (isPublished && !hasDraftAbovePublish) ? 'status-pill--published' : 'status-pill--draft'"
            >
              {{ hasUnsaved ? '未保存' : (isPublished && !hasDraftAbovePublish) ? '已发布' : '草稿' }}
            </span>
          </div>

          <div class="tb-right">
            <button class="btn btn--default" @click="openPreview">预览</button>
            <button class="btn btn--default" :disabled="saving || !hasUnsaved" @click="handleSave">{{ saving ? '保存中…' : '保存' }}</button>
            <button class="btn btn--primary" :disabled="hasUnsaved || (isPublished && !hasDraftAbovePublish)" @click="openPublishDialog">发布</button>
            <button
              class="sidebar-toggle"
              :title="sidebarOpen ? '收起侧栏' : '展开侧栏'"
              @click="sidebarOpen = !sidebarOpen"
            >
              <svg v-if="sidebarOpen" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
              <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
            </button>
          </div>
        </div>

        <!-- ── Body ───────────────────────────────────────────────────────── -->
        <div class="write-body">

          <!-- Editor area -->
          <div class="editor-area">
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
              <VditorWriter
                ref="vditorRef"
                :content="content"
                :editable="true"
                :upload-image="uploadImageFn"
                @change="onEditorChange"
                @error="(msg) => toast.error(msg)"
              />
            </div>
          </div>

          <!-- Version sidebar -->
          <VersionSidebar
            :open="sidebarOpen"
            :article-id="articleId"
            :versions="versions"
            :published-version-id="publishedVersionId"
            @reload="loadVersions"
          />

        </div>

      </template>

      <!-- ── Publish Dialog ────────────────────────────────────────────────── -->
      <ArticleMetaDialog
        ref="metaDialogRef"
        v-model:visible="publishDialogVisible"
        :summary="draftSummary"
        :category="draftCategory"
        :tags="draftTags"
        :saving="publishing"
        :title="isPublished ? '更新发布' : '发布文章'"
        :confirm-text="publishing ? '发布中…' : (isPublished ? '更新' : '立即发布')"
        @save="handlePublishConfirm"
      >
        <template #header-extra>
          <button
            class="ai-trigger-btn"
            :class="{ 'ai-trigger-btn--loading': aiGenerating }"
            :disabled="aiGenerating"
            type="button"
            title="AI 智能填写"
            @click="runAiRecommend"
          >
            <span :style="aiGenerating ? 'display:inline-block;animation:ai-spin 1.2s linear infinite' : ''">✦</span>
          </button>
        </template>

        <template #summary-extra>
          <transition name="ai-slide">
            <div v-if="aiSummaryResult" class="pf-ai-inline">
              <p class="pf-ai-inline-body">{{ aiSummaryResult }}</p>
              <div class="pf-ai-inline-actions">
                <button type="button" class="ai-action ai-action--dismiss" @click="aiSummaryResult = null">忽略</button>
                <button type="button" class="ai-action ai-action--primary" @click="acceptAiSummary">应用</button>
              </div>
            </div>
          </transition>
        </template>

        <template #category-extra>
          <transition name="ai-slide">
            <div v-if="aiCatResult !== null" class="pf-ai-inline pf-ai-inline--row">
              <template v-if="aiCatResult">
                <span class="pf-ai-inline-val">{{ aiCatResult.name }}</span>
                <div class="pf-ai-inline-actions">
                  <button type="button" class="ai-action ai-action--dismiss" @click="aiCatResult = null">忽略</button>
                  <button type="button" class="ai-action ai-action--primary" @click="applyAiCategory">应用</button>
                </div>
              </template>
              <template v-else>
                <span class="pf-ai-inline-no-match">现有分类均不适配，请手动选择</span>
                <button type="button" class="pf-ai-inline-dismiss" @click="aiCatResult = null">
                  <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6L6 18M6 6l12 12"/></svg>
                </button>
              </template>
            </div>
          </transition>
        </template>

        <template #tags-extra>
          <transition name="ai-slide">
            <div v-if="aiTagsResult" class="pf-ai-inline pf-ai-inline--chips">
              <button
                v-for="tag in aiTagsResult.existing"
                :key="tag.id"
                type="button"
                class="ai-meta-chip ai-meta-chip--existing ai-meta-chip--action"
                @click="applyAiExistingTag(tag)"
              >{{ tag.name }}<span class="ai-meta-chip__plus">+</span></button>
              <button
                v-for="name in aiTagsResult.suggested"
                :key="name"
                type="button"
                class="ai-meta-chip ai-meta-chip--new ai-meta-chip--action"
                @click="applyAiSuggestedTag(name)"
              >{{ name }}<span class="ai-meta-chip__badge">新</span><span class="ai-meta-chip__plus">+</span></button>
              <button type="button" class="pf-ai-inline-dismiss" @click="aiTagsResult = null">
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
          </transition>
        </template>
      </ArticleMetaDialog>

    </div>
</template>

<style scoped>
/* ── Layout ──────────────────────────────────────────────────────────────────── */
.write-v2 {
  margin: -24px;
  height: calc(100vh - var(--admin-header-height));
  display: flex;
  flex-direction: column;
  background: var(--write-bg, var(--admin-surface-input));
  overflow: hidden;
}

/* ── Loading ─────────────────────────────────────────────────────────────────── */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 200px;
  color: var(--admin-text-muted);
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

/* ── Toolbar ─────────────────────────────────────────────────────────────────── */
.write-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 16px 0 20px;
  border-bottom: 1px solid var(--write-border, var(--admin-border-soft));
  flex-shrink: 0;
  gap: 12px;
  z-index: 10;
  background: var(--write-toolbar-bg, var(--admin-surface-input));
  color: var(--write-text, inherit);
}

.tb-left  { display: flex; align-items: center; gap: 12px; min-width: 0; }
.tb-right { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--admin-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 5px 8px;
  border-radius: 6px;
  white-space: nowrap;
  transition: color 0.15s, background 0.15s;
}
.back-btn:hover { color: var(--admin-text-primary); background: var(--admin-surface-soft); }

.tb-sep { width: 1px; height: 18px; background: var(--admin-border-soft); flex-shrink: 0; }

.status-pill { font-size: 12px; font-weight: 500; padding: 3px 8px; border-radius: 4px; white-space: nowrap; }
.status-pill--published { background: var(--admin-surface-soft); color: var(--admin-success); }
.status-pill--draft     { background: var(--admin-surface-soft); color: var(--admin-text-muted); border: 1px solid var(--admin-border-soft); }
.status-pill--unsaved   { background: #fff7ed; color: var(--admin-warning); border: 1px solid var(--admin-border-soft); }

.save-hint { font-size: 12px; color: var(--admin-text-muted); }

.sidebar-toggle {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
  border: 1px solid var(--admin-border-soft); border-radius: 6px;
  background: transparent; color: var(--admin-text-muted); cursor: pointer;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
  flex-shrink: 0;
}
.sidebar-toggle:hover { border-color: var(--admin-border-soft); background: var(--admin-surface-soft); color: var(--admin-accent); }

/* ── Body ────────────────────────────────────────────────────────────────────── */
.write-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }

/* ── Editor area ─────────────────────────────────────────────────────────────── */
.editor-area { flex: 1; min-width: 0; overflow-y: auto; background: transparent; }

.editor-header {
  padding: 28px 64px 16px;
}

.title-input {
  display: block;
  width: 100%;
  max-width: var(--spacing-prose);
  margin: 0 auto;
  font-size: 32px;
  font-weight: 700;
  color: var(--write-text, var(--admin-text-primary));
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

.title-input::placeholder { color: var(--write-placeholder, var(--admin-text-muted)); }

.editor-container {
  padding: 0 64px 60px;
  background: transparent;
}

/* ── AI trigger button (rendered via slot into ArticleMetaDialog) ────────────*/
.ai-trigger-btn {
  display: flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; flex-shrink: 0;
  background: var(--admin-surface-soft); border: 1px solid rgba(184, 92, 56, 0.22);
  border-radius: 4px; cursor: pointer;
  color: var(--admin-accent); font-size: 13px; font-family: inherit;
  transition: background 0.15s;
}
.ai-trigger-btn:hover:not(:disabled) { background: rgba(184, 92, 56, 0.08); }
.ai-trigger-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* ── Inline AI suggestion cards ───────────────────────────────────────────────*/
.pf-ai-inline {
  border: 1px solid #ede9fe;
  border-radius: 4px;
  background: var(--admin-surface-soft);
  padding: 10px 12px;
}
.pf-ai-inline--row {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
}
.pf-ai-inline--chips {
  display: flex; align-items: center; flex-wrap: wrap; gap: 6px;
}
.pf-ai-inline-body {
  margin: 0;
  font-size: 13px; line-height: 1.7; color: var(--admin-text-secondary);
  padding-bottom: 10px;
}
.pf-ai-inline-actions {
  display: flex; align-items: center; gap: 6px; justify-content: flex-end;
}
.pf-ai-inline--row .pf-ai-inline-actions { margin-left: auto; flex-shrink: 0; }
.pf-ai-inline-val { font-size: 13px; color: var(--admin-text-secondary); font-weight: 500; flex: 1; }
.pf-ai-inline-no-match { font-size: 12px; color: var(--admin-text-muted); font-style: italic; flex: 1; }
.pf-ai-inline-dismiss {
  display: flex; align-items: center; justify-content: center;
  width: 20px; height: 20px; flex-shrink: 0;
  border: none; background: transparent; cursor: pointer;
  color: var(--admin-text-muted); border-radius: var(--admin-radius); padding: 0;
  margin-left: auto;
  transition: color 0.15s;
}
.pf-ai-inline-dismiss:hover { color: var(--admin-text-secondary); }

@keyframes ai-spin { to { transform: rotate(360deg); } }

/* ── AI action buttons ─────────────────────────────────────────────────────────*/
.ai-action {
  padding: 3px 10px;
  font-size: 12px; font-weight: 500;
  border-radius: 4px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}
.ai-action--dismiss {
  color: var(--admin-text-muted); background: var(--admin-surface-soft); border-color: var(--admin-border-soft);
}
.ai-action--dismiss:hover { background: var(--admin-border-soft); }
.ai-action--primary {
  color: var(--admin-text-on-accent); background: #7c3aed; border-color: var(--admin-accent);
}
.ai-action--primary:hover { background: var(--admin-accent-dark); }

/* ── AI meta chips (category / tag suggestions) ───────────────────────────────*/
.ai-meta-chip {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 10px;
  font-size: 12px; font-weight: 500;
  border-radius: 4px;
  border: 1px solid transparent;
  line-height: 20px;
  font-family: inherit;
}
.ai-meta-chip--existing {
  color: var(--admin-accent); background: rgba(184, 92, 56, 0.08); border-color: #ddd6fe;
}
.ai-meta-chip--new {
  color: var(--admin-text-muted); background: var(--admin-surface-soft); border-color: var(--admin-text-muted); border-style: dashed;
}
.ai-meta-chip--action {
  cursor: pointer;
  transition: background 0.15s;
}
.ai-meta-chip--action.ai-meta-chip--existing:hover { background: rgba(184, 92, 56, 0.14); }
.ai-meta-chip--action.ai-meta-chip--new:hover { background: var(--admin-surface-soft); }
.ai-meta-chip__badge {
  font-size: 10px; font-weight: 600;
  color: var(--admin-text-muted); background: var(--admin-border-soft);
  padding: 0 4px; border-radius: var(--admin-radius);
}
.ai-meta-chip__plus {
  font-size: 14px; font-weight: 400; line-height: 1;
  color: var(--admin-accent-light); margin-left: 1px;
}
.ai-meta-chip--new .ai-meta-chip__plus { color: var(--admin-text-muted); }
.ai-meta-chip--applied {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ── Buttons ─────────────────────────────────────────────────────────────────── */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 16px;
  font-size: 13px;
  font-family: inherit;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  white-space: nowrap;
  line-height: 1;
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--sm { height: 28px; padding: 0 12px; font-size: 12px; }
.btn--default { background: transparent; color: var(--admin-text-muted); border-color: var(--admin-border-soft); }
.btn--default:hover:not(:disabled) { background: var(--admin-surface-soft); border-color: var(--admin-text-muted); color: var(--admin-text-secondary); }
.btn--primary { background: var(--admin-accent, #b85c38); color: var(--admin-text-on-accent); border-color: var(--admin-accent, #b85c38); font-weight: 500; }
.btn--primary:hover:not(:disabled) { background: var(--admin-accent-dark, #924530); border-color: var(--admin-accent-dark, #924530); }
.btn--cancel { background: transparent; color: var(--admin-text-muted); border-color: #d4cfc9; }
.btn--cancel:hover:not(:disabled) { background: var(--admin-surface-hover); border-color: var(--admin-border-strong); }

/* ── AI inline transition ────────────────────────────────────────────────────── */
.ai-slide-enter-active, .ai-slide-leave-active { transition: opacity 0.18s, transform 0.18s; }
.ai-slide-enter-from, .ai-slide-leave-to { opacity: 0; transform: translateY(-3px); }

/* ── Mobile ──────────────────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .write-v2    { margin: -16px -12px; }
  .write-toolbar { padding: 0 12px; gap: 8px; }
  .save-hint   { display: none; }
  .editor-header { padding: 24px 20px 16px; }
  .title-input { font-size: 24px; }
  .editor-container { padding: 0 20px 40px; }
}
</style>
