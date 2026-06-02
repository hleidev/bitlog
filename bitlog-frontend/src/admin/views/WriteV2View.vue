<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import '@/assets/styles/prose.css'
import ArticleEditor from '@/components/ArticleEditor.vue'
import {
  createArticle,
  updateArticleDraft,
  getArticleDraft,
  publishArticle,
  getArticleVersions,
  generateAiMetadata,
  deleteArticleVersions,
  type ArticleVersionVO,
} from '@/api/admin/article'
import { getCategories, getOrCreateCategory, type Category } from '@/api/admin/category'
import { getTags, getOrCreateTag, type Tag } from '@/api/admin/tag'
import { ApiError } from '@/utils/request'

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
const editorRef = ref<InstanceType<typeof ArticleEditor> | null>(null)

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

const saveStateText = computed(() => {
  if (saveState.value === 'saving') return '保存中...'
  if (hasUnsaved.value) return '未保存'
  if (saveState.value === 'saved') return '已保存'
  return ''
})

// ── Sidebar & versions ─────────────────────────────────────────────────────────
const sidebarOpen = ref(false)
const versions    = ref<ArticleVersionVO[]>([])

// ── Version management ─────────────────────────────────────────────────────────
const versionManageMode  = ref(false)
const selectedVersionIds = ref<number[]>([])
const deletingVersions   = ref(false)

const deletableVersionIds = computed(() =>
  versions.value
    .filter(v => !v.latest && v.id !== publishedVersionId.value)
    .map(v => v.id)
)

const allDeletableSelected = computed(() =>
  deletableVersionIds.value.length > 0 &&
  deletableVersionIds.value.every(id => selectedVersionIds.value.includes(id))
)

// ── Publish dialog ─────────────────────────────────────────────────────────────
const publishDialogVisible = ref(false)
const publishing           = ref(false)

const publishForm = ref({
  summary:    '',
  categoryId: null as number | null,
  tagIds:     [] as number[],
})

// ── Category / tag options ─────────────────────────────────────────────────────
const categoryOptions = ref<Category[]>([])
const tagOptions      = ref<Tag[]>([])

// Category combobox
const categoryInput        = ref('')
const categoryDropdownOpen = ref(false)
const selectedCategory     = ref<{ id: number; name: string } | null>(null)
const filteredCategories   = computed(() => {
  const q = categoryInput.value.trim().toLowerCase()
  if (!q) return categoryOptions.value
  return categoryOptions.value.filter(c => c.name.toLowerCase().includes(q))
})

// Tags combobox
const tagInput        = ref('')
const tagDropdownOpen = ref(false)
const selectedTags    = ref<{ id: number; name: string }[]>([])
const filteredTags    = computed(() => {
  const selectedIds = new Set(selectedTags.value.map(t => t.id))
  const base = tagOptions.value.filter(t => !selectedIds.has(t.id))
  const q = tagInput.value.trim().toLowerCase()
  if (!q) return base
  return base.filter(t => t.name.toLowerCase().includes(q))
})

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

function shortTime(d: string) {
  return new Date(d)
    .toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
    .replace(/\//g, '-')
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
async function loadCategoriesAndTags() {
  try {
    const [cats, tags] = await Promise.all([getCategories(), getTags()])
    categoryOptions.value = cats
    tagOptions.value      = tags
  } catch {
    // non-critical
  }
}

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
    publishForm.value = {
      summary:    data.summary ?? '',
      categoryId: data.category?.id ?? null,
      tagIds:     data.tags.map(t => t.id),
    }
    await loadVersions()
    saveState.value = 'saved'
  } catch (err) {
    handleError(err, '加载文章失败')
  } finally {
    loading.value = false
    await nextTick()
    autoResizeTitle()
    suppressChange = false
  }
}

// ── Category combobox handlers ─────────────────────────────────────────────────
function selectCategory(cat: { id: number; name: string }) {
  selectedCategory.value = cat
  categoryInput.value    = cat.name
  publishForm.value.categoryId = cat.id
  categoryDropdownOpen.value   = false
}

async function createAndSelectCategory(name: string) {
  try {
    const id = await getOrCreateCategory(name)
    selectCategory({ id, name })
    if (!categoryOptions.value.find(c => c.id === id)) {
      categoryOptions.value = [...categoryOptions.value, { id, name, articleCount: 0, createTime: '' }]
    }
  } catch {
    toast.error('创建分类失败')
  }
}

function clearCategory() {
  selectedCategory.value       = null
  categoryInput.value          = ''
  publishForm.value.categoryId = null
}

function onCategoryInput() {
  if (selectedCategory.value && categoryInput.value !== selectedCategory.value.name) {
    selectedCategory.value       = null
    publishForm.value.categoryId = null
  }
  categoryDropdownOpen.value = true
}

function onCategoryEnter() {
  const q = categoryInput.value.trim()
  if (!q) return
  const exact = filteredCategories.value[0]
  if (exact && exact.name.toLowerCase() === q.toLowerCase()) {
    selectCategory(exact)
  } else if (filteredCategories.value.length === 1) {
    selectCategory(filteredCategories.value[0])
  } else {
    createAndSelectCategory(q)
  }
}

function closeCategoryDropdown() {
  if (!selectedCategory.value) categoryInput.value = ''
  categoryDropdownOpen.value = false
}

// ── Tag combobox handlers ──────────────────────────────────────────────────────
function addTag(tag: { id: number; name: string }) {
  if (selectedTags.value.find(t => t.id === tag.id)) return
  selectedTags.value       = [...selectedTags.value, tag]
  publishForm.value.tagIds = selectedTags.value.map(t => t.id)
  tagInput.value           = ''
  tagDropdownOpen.value    = false
}

async function createAndAddTag(name: string) {
  try {
    const id = await getOrCreateTag(name)
    if (!selectedTags.value.find(t => t.id === id)) {
      selectedTags.value       = [...selectedTags.value, { id, name }]
      publishForm.value.tagIds = selectedTags.value.map(t => t.id)
    }
    if (!tagOptions.value.find(t => t.id === id)) {
      tagOptions.value = [...tagOptions.value, { id, name, articleCount: 0, createTime: '', updateTime: '' }]
    }
    tagInput.value        = ''
    tagDropdownOpen.value = false
  } catch {
    toast.error('创建标签失败')
  }
}

function removeTag(id: number) {
  selectedTags.value       = selectedTags.value.filter(t => t.id !== id)
  publishForm.value.tagIds = selectedTags.value.map(t => t.id)
}

function onTagEnter() {
  const q = tagInput.value.trim()
  if (!q) return
  const exact = tagOptions.value.find(t => t.name.toLowerCase() === q.toLowerCase())
  if (exact) {
    addTag(exact)
  } else {
    createAndAddTag(q)
  }
}

// ── Save ───────────────────────────────────────────────────────────────────────
async function performSave() {
  if (!title.value.trim() || saving.value) return
  saving.value    = true
  saveState.value = 'saving'
  try {
    const md = editorRef.value?.getMarkdown() ?? ''
    if (isNew) {
      const newId = await createArticle({ title: title.value, content: md })
      router.replace(`/admin/write/${newId}`)
      saveState.value  = 'saved'
      hasUnsaved.value = false
      toast.success('已保存')
    } else {
      await updateArticleDraft(articleId!, { title: title.value, content: md })
      await loadVersions()
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
  const md = editorRef.value?.getMarkdown() ?? ''
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

async function handlePublishConfirm() {
  if (!publishForm.value.categoryId) {
    toast.warning('请选择文章分类')
    return
  }
  publishing.value = true
  const wasPublished = isPublished.value
  try {
    await publishArticle(articleId, {
      summary:    publishForm.value.summary || null,
      categoryId: publishForm.value.categoryId,
      tagIds:     publishForm.value.tagIds,
    })
    publishedVersionId.value   = latestVersionId.value
    publishDialogVisible.value = false
    hasUnsaved.value           = false
    hasDraftAbovePublish.value = false
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
  publishForm.value.summary = aiSummaryResult.value
  aiSummaryResult.value     = null
}

function applyAiCategory() {
  if (!aiCatResult.value) return
  selectCategory(aiCatResult.value as { id: number; name: string })
  aiCatResult.value = null
}

function applyAiExistingTag(tag: { id: number; name: string }) {
  addTag(tag)
}

function isSuggestedTagApplied(name: string): boolean {
  return selectedTags.value.some(t => t.name === name)
}

async function applyAiSuggestedTag(name: string) {
  await createAndAddTag(name)
}

// ── Version management ─────────────────────────────────────────────────────────
function toggleVersionSelect(id: number) {
  const idx = selectedVersionIds.value.indexOf(id)
  if (idx >= 0) selectedVersionIds.value.splice(idx, 1)
  else          selectedVersionIds.value.push(id)
}

function toggleSelectAll() {
  if (allDeletableSelected.value) selectedVersionIds.value = []
  else selectedVersionIds.value = [...deletableVersionIds.value]
}

function exitVersionManage() {
  versionManageMode.value  = false
  selectedVersionIds.value = []
}

async function handleDeleteVersions() {
  if (!selectedVersionIds.value.length) return
  const count = selectedVersionIds.value.length
  try {
    await confirm(
      `确定删除选中的 ${count} 个版本？此操作不可恢复。`,
      '删除版本',
      { confirmText: '删除', cancelText: '取消', danger: true }
    )
  } catch { return }
  deletingVersions.value = true
  try {
    await deleteArticleVersions(articleId, [...selectedVersionIds.value])
    await loadVersions()
    exitVersionManage()
    toast.success(`已删除 ${count} 个版本`)
  } catch (err) {
    handleError(err, '删除版本失败')
  } finally {
    deletingVersions.value = false
  }
}

// ── Watchers ───────────────────────────────────────────────────────────────────
watch(title, () => {
  if (suppressChange) return
  hasUnsaved.value = true
  saveState.value  = 'idle'
})

function onEditorChange() {
  if (suppressChange) return
  hasUnsaved.value = true
  saveState.value  = 'idle'
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
  await Promise.all([loadDraft(), loadCategoriesAndTags()])
  // Resolve display names for pre-selected category / tags after both loads complete
  if (publishForm.value.categoryId) {
    const cat = categoryOptions.value.find(c => c.id === publishForm.value.categoryId)
    if (cat) selectCategory(cat)
  }
  selectedTags.value = publishForm.value.tagIds
    .flatMap(id => {
      const t = tagOptions.value.find(tag => tag.id === id)
      return t ? [{ id: t.id, name: t.name }] : []
    })
})

onUnmounted(() => {
  clearTimeout(saveTimer)
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
              <ArticleEditor ref="editorRef" :content="content" :editable="true" @change="onEditorChange" @error="(msg) => toast.error(msg)" />
            </div>
          </div>

          <!-- Version sidebar -->
          <div class="meta-sidebar" :class="{ 'meta-sidebar--closed': !sidebarOpen }">
            <div class="sidebar-scroll">
              <div class="sidebar-section">
                <div class="version-header-row">
                  <span class="section-label">历史版本</span>
                  <button
                    class="version-manage-toggle"
                    :class="{ 'version-manage-toggle--cancel': versionManageMode }"
                    @click="versionManageMode ? exitVersionManage() : (versionManageMode = true)"
                  >
                    {{ versionManageMode ? '取消' : '管理' }}
                  </button>
                </div>

                <div class="version-list">

                  <div v-if="versionManageMode" class="version-select-all">
                    <label class="checkbox-label">
                      <input type="checkbox" :checked="allDeletableSelected" :disabled="deletableVersionIds.length === 0" @change="toggleSelectAll" />
                      全选可删除
                    </label>
                  </div>

                  <div
                    v-for="v in versions"
                    :key="v.id"
                    class="version-item"
                    :class="{
                      'version-item--current':     v.latest,
                      'version-item--manage':      versionManageMode,
                      'version-item--undeletable': versionManageMode && (v.latest || v.id === publishedVersionId),
                    }"
                  >
                    <input
                      v-if="versionManageMode"
                      type="checkbox"
                      class="version-checkbox"
                      :checked="selectedVersionIds.includes(v.id)"
                      :disabled="v.latest || v.id === publishedVersionId"
                      @change="() => toggleVersionSelect(v.id)"
                    />

                    <div class="version-info">
                      <div class="version-label-row">
                        <span class="version-label">版本 {{ v.version }}</span>
                        <span v-if="v.latest" class="version-tag version-tag--current">当前</span>
                        <span v-else-if="v.id === publishedVersionId" class="version-tag version-tag--published">已发布</span>
                      </div>
                      <span class="version-time">{{ shortTime(v.createTime) }}</span>
                    </div>

                    <template v-if="!versionManageMode">
                      <span v-if="v.latest" class="current-dot" />
                    </template>
                  </div>

                  <div v-if="versionManageMode" class="version-manage-footer">
                    <span class="version-manage-count">
                      已选 {{ selectedVersionIds.length }} / {{ deletableVersionIds.length }}
                    </span>
                    <button
                      class="version-delete-btn"
                      :disabled="selectedVersionIds.length === 0 || deletingVersions"
                      @click="handleDeleteVersions"
                    >
                      {{ deletingVersions ? '删除中…' : '删除' }}
                    </button>
                  </div>

                </div>
              </div>
            </div>
          </div>

        </div>

      </template>

      <!-- ── Publish Dialog ────────────────────────────────────────────────── -->
      <Teleport to="body">
        <Transition name="dialog-fade">
          <div v-if="publishDialogVisible" class="dialog-overlay" @click.self="publishDialogVisible = false">
            <div class="dialog-panel">
              <div class="dialog-header">
                <div class="dialog-title-row">
                  <span class="dialog-title">{{ isPublished ? '更新发布' : '发布文章' }}</span>
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
                </div>
                <button class="dialog-x" @click="publishDialogVisible = false">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
                </button>
              </div>

              <div class="dialog-body">
                <div class="publish-form">

                  <div class="pf-item">
                    <div class="pf-label">摘要 <span class="pf-optional">可选</span></div>
                    <textarea
                      v-model="publishForm.summary"
                      class="pf-textarea"
                      placeholder="留空则自动截取正文前 200 字..."
                      maxlength="512"
                      rows="3"
                    />
                    <div class="pf-char-count">{{ publishForm.summary.length }} / 512</div>
                    <transition name="ai-slide">
                      <div v-if="aiSummaryResult" class="pf-ai-inline">
                        <p class="pf-ai-inline-body">{{ aiSummaryResult }}</p>
                        <div class="pf-ai-inline-actions">
                          <span class="pf-ai-badge">✦ AI</span>
                          <button type="button" class="ai-action ai-action--dismiss" @click="aiSummaryResult = null">忽略</button>
                          <button type="button" class="ai-action ai-action--primary" @click="acceptAiSummary">应用</button>
                        </div>
                      </div>
                    </transition>
                  </div>

                  <div class="pf-item">
                    <div class="pf-label">分类 <span class="pf-required">必填</span></div>
                    <div class="pf-combo" v-click-outside="closeCategoryDropdown">
                      <div class="pf-combo-field" :class="{ 'pf-combo-field--focused': categoryDropdownOpen }">
                        <input
                          v-model="categoryInput"
                          class="pf-combo-input"
                          placeholder="搜索或输入新分类..."
                          @input="onCategoryInput"
                          @focus="categoryDropdownOpen = true"
                          @keydown.enter.prevent="onCategoryEnter"
                          @keydown.escape="closeCategoryDropdown"
                          @keydown.tab="closeCategoryDropdown"
                        />
                        <button
                          v-if="selectedCategory"
                          type="button"
                          class="pf-combo-clear"
                          tabindex="-1"
                          @click="clearCategory"
                        >
                          <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6L6 18M6 6l12 12"/></svg>
                        </button>
                      </div>
                      <div v-if="categoryDropdownOpen" class="pf-combo-dropdown">
                        <button
                          v-for="cat in filteredCategories"
                          :key="cat.id"
                          type="button"
                          class="pf-combo-opt"
                          :class="{ 'pf-combo-opt--active': selectedCategory?.id === cat.id }"
                          @mousedown.prevent="selectCategory(cat)"
                        >{{ cat.name }}</button>
                        <button
                          v-if="categoryInput.trim() && !filteredCategories.find(c => c.name.toLowerCase() === categoryInput.trim().toLowerCase())"
                          type="button"
                          class="pf-combo-opt"
                          @mousedown.prevent="createAndSelectCategory(categoryInput.trim())"
                        >{{ categoryInput.trim() }}</button>
                        <div v-if="!filteredCategories.length && !categoryInput.trim()" class="pf-combo-empty">
                          暂无分类，输入名称即可创建
                        </div>
                      </div>
                    </div>
                    <transition name="ai-slide">
                      <div v-if="aiCatResult !== null" class="pf-ai-inline pf-ai-inline--row">
                        <span class="pf-ai-badge">✦ AI</span>
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
                  </div>

                  <div class="pf-item">
                    <div class="pf-label">标签 <span class="pf-optional">可选</span></div>
                    <div v-if="selectedTags.length" class="pf-selected-chips">
                      <span v-for="tag in selectedTags" :key="tag.id" class="pf-sel-chip">
                        {{ tag.name }}
                        <button type="button" class="pf-sel-chip-x" @click="removeTag(tag.id)">
                          <svg viewBox="0 0 24 24" width="10" height="10" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6L6 18M6 6l12 12"/></svg>
                        </button>
                      </span>
                    </div>
                    <div class="pf-combo" v-click-outside="() => { tagDropdownOpen = false; tagInput = '' }">
                      <div class="pf-combo-field" :class="{ 'pf-combo-field--focused': tagDropdownOpen }">
                        <input
                          v-model="tagInput"
                          class="pf-combo-input"
                          placeholder="搜索或输入新标签..."
                          @input="tagDropdownOpen = true"
                          @focus="tagDropdownOpen = true"
                          @keydown.enter.prevent="onTagEnter"
                          @keydown.escape="tagDropdownOpen = false; tagInput = ''"
                          @keydown.tab="tagDropdownOpen = false"
                        />
                      </div>
                      <div v-if="tagDropdownOpen" class="pf-combo-dropdown">
                        <button
                          v-for="tag in filteredTags"
                          :key="tag.id"
                          type="button"
                          class="pf-combo-opt"
                          @mousedown.prevent="addTag(tag)"
                        >{{ tag.name }}</button>
                        <button
                          v-if="tagInput.trim() && !tagOptions.find(t => t.name.toLowerCase() === tagInput.trim().toLowerCase())"
                          type="button"
                          class="pf-combo-opt"
                          @mousedown.prevent="createAndAddTag(tagInput.trim())"
                        >{{ tagInput.trim() }}</button>
                        <div v-if="!filteredTags.length && !tagInput.trim()" class="pf-combo-empty">
                          暂无更多标签，输入名称即可创建
                        </div>
                      </div>
                    </div>
                    <transition name="ai-slide">
                      <div v-if="aiTagsResult" class="pf-ai-inline pf-ai-inline--chips">
                        <span class="pf-ai-badge">✦ AI</span>
                        <button
                          v-for="tag in aiTagsResult.existing"
                          :key="tag.id"
                          type="button"
                          :class="['ai-meta-chip ai-meta-chip--existing ai-meta-chip--action', { 'ai-meta-chip--applied': !!selectedTags.find(t => t.id === tag.id) }]"
                          :disabled="!!selectedTags.find(t => t.id === tag.id)"
                          @click="applyAiExistingTag(tag)"
                        >{{ tag.name }}<span v-if="!selectedTags.find(t => t.id === tag.id)" class="ai-meta-chip__plus">+</span></button>
                        <button
                          v-for="name in aiTagsResult.suggested"
                          :key="name"
                          type="button"
                          :class="['ai-meta-chip ai-meta-chip--new ai-meta-chip--action', { 'ai-meta-chip--applied': isSuggestedTagApplied(name) }]"
                          :disabled="isSuggestedTagApplied(name)"
                          @click="applyAiSuggestedTag(name)"
                        >{{ name }}<span class="ai-meta-chip__badge">新</span><span v-if="!isSuggestedTagApplied(name)" class="ai-meta-chip__plus">+</span></button>
                        <button type="button" class="pf-ai-inline-dismiss" @click="aiTagsResult = null">
                          <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6L6 18M6 6l12 12"/></svg>
                        </button>
                      </div>
                    </transition>
                  </div>

                </div>
              </div>

              <div class="dialog-footer">
                <button class="btn btn--cancel" @click="publishDialogVisible = false">取消</button>
                <button class="btn btn--primary" :disabled="publishing" @click="handlePublishConfirm">
                  {{ publishing ? '发布中…' : (isPublished ? '更新' : '立即发布') }}
                </button>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>

    </div>
</template>

<style scoped>
/* ── Layout ──────────────────────────────────────────────────────────────────── */
.write-v2 {
  margin: -24px;
  height: calc(100vh - var(--admin-header-height));
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
}

/* ── Loading ─────────────────────────────────────────────────────────────────── */
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

/* ── Toolbar ─────────────────────────────────────────────────────────────────── */
.write-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 16px 0 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  gap: 12px;
  z-index: 10;
  background: #fff;
}

.tb-left  { display: flex; align-items: center; gap: 12px; min-width: 0; }
.tb-right { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #6b7280;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 5px 8px;
  border-radius: 6px;
  white-space: nowrap;
  transition: color 0.15s, background 0.15s;
}
.back-btn:hover { color: #111827; background: #f3f4f6; }

.tb-sep { width: 1px; height: 18px; background: #e5e7eb; flex-shrink: 0; }

.status-pill { font-size: 12px; font-weight: 500; padding: 3px 8px; border-radius: 4px; white-space: nowrap; }
.status-pill--published { background: #f0fdf4; color: #16a34a; }
.status-pill--draft     { background: #f9fafb; color: #6b7280; border: 1px solid #e5e7eb; }
.status-pill--unsaved   { background: #fff7ed; color: #c2410c; border: 1px solid #fed7aa; }

.save-hint { font-size: 12px; color: #9ca3af; }

.sidebar-toggle {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
  border: 1px solid #e5e7eb; border-radius: 6px;
  background: transparent; color: #6b7280; cursor: pointer;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
  flex-shrink: 0;
}
.sidebar-toggle:hover { border-color: #c7d2fe; background: #f5f3ff; color: #4338ca; }

/* ── Body ────────────────────────────────────────────────────────────────────── */
.write-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }

/* ── Editor area ─────────────────────────────────────────────────────────────── */
.editor-area { flex: 1; min-width: 0; overflow-y: auto; }

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

.title-input::placeholder { color: #d1d5db; }

.editor-container {
  padding: 0 64px 60px;
}

/* ── Sidebar ─────────────────────────────────────────────────────────────────── */
.meta-sidebar {
  width: 260px; flex-shrink: 0;
  border-left: 1px solid #f0f0f0; background: #fafafa;
  transition: width 0.25s ease, opacity 0.2s ease;
  overflow: hidden;
}
.meta-sidebar--closed { width: 0; opacity: 0; }

.sidebar-scroll { width: 100%; height: 100%; overflow-y: auto; }

.sidebar-section { padding: 16px 20px; border-bottom: 1px solid #f0f0f0; }

.section-label {
  font-size: 11px; font-weight: 600; color: #9ca3af;
  text-transform: uppercase; letter-spacing: 0.6px; margin-bottom: 10px;
}

.version-header-row {
  display: flex; align-items: center; justify-content: space-between;
}
.version-header-row .section-label { margin-bottom: 0; }

.version-manage-toggle {
  flex-shrink: 0;
  font-size: 11px; font-weight: 500; color: #6366f1;
  background: transparent; border: none; cursor: pointer;
  padding: 2px 4px; border-radius: 3px;
  transition: color 0.15s;
}
.version-manage-toggle:hover { color: #4338ca; }
.version-manage-toggle--cancel { color: #9ca3af; }
.version-manage-toggle--cancel:hover { color: #6b7280; }

.version-list { margin-top: 12px; display: flex; flex-direction: column; }

.version-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 0; border-bottom: 1px solid #f3f4f6; gap: 8px;
  border-radius: 4px;
  transition: background 0.15s;
}
.version-item:last-child { border-bottom: none; padding-bottom: 0; }

.version-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.version-label { font-size: 13px; font-weight: 500; color: #374151; }
.version-time  { font-size: 11px; color: #9ca3af; }

.current-dot { width: 8px; height: 8px; border-radius: 50%; background: #16a34a; flex-shrink: 0; }

.version-select-all {
  padding: 8px 0 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 2px;
}

.version-item--manage      { gap: 10px; }
.version-item--undeletable { opacity: 0.45; }

.version-checkbox { flex-shrink: 0; }

.version-label-row { display: flex; align-items: center; gap: 5px; }

.version-tag {
  font-size: 10px; font-weight: 500; line-height: 1;
  padding: 2px 5px; border-radius: 3px;
}
.version-tag--current   { color: #166534; background: #dcfce7; }
.version-tag--published { color: #1e40af; background: #dbeafe; }

.version-manage-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 0 2px;
  border-top: 1px solid #f0f0f0;
  margin-top: 6px;
}
.version-manage-count { font-size: 12px; color: #9ca3af; }

.version-delete-btn {
  font-size: 12px; font-weight: 500;
  color: #fff; background: #ef4444;
  border: none; border-radius: 4px;
  padding: 4px 14px; cursor: pointer;
  transition: background 0.15s;
}
.version-delete-btn:hover:not(:disabled) { background: #dc2626; }
.version-delete-btn:disabled { opacity: 0.45; cursor: not-allowed; }

/* ── Publish dialog ───────────────────────────────────────────────────────────── */
.publish-form { display: flex; flex-direction: column; gap: 20px; }
.pf-item      { display: flex; flex-direction: column; gap: 8px; }
.pf-label     { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 500; color: #374151; }
.pf-optional  { font-size: 11px; font-weight: 400; color: #9ca3af; background: #f3f4f6; padding: 1px 6px; border-radius: 3px; }
.pf-required  { font-size: 11px; font-weight: 500; color: #dc2626; background: #fef2f2; padding: 1px 6px; border-radius: 3px; }

/* ── Dialog title row with AI button ─────────────────────────────────────────*/
.dialog-title-row { display: flex; align-items: center; gap: 8px; }
.ai-trigger-btn {
  display: flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; flex-shrink: 0;
  background: #f5f3ff; border: 1px solid #ddd6fe;
  border-radius: 4px; cursor: pointer;
  color: #7c3aed; font-size: 13px; font-family: inherit;
  transition: background 0.15s;
}
.ai-trigger-btn:hover:not(:disabled) { background: #ede9fe; }
.ai-trigger-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* ── Inline AI suggestion cards ───────────────────────────────────────────────*/
.pf-ai-inline {
  border: 1px solid #ede9fe;
  border-radius: 4px;
  background: #faf9ff;
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
  font-size: 13px; line-height: 1.7; color: #374151;
  padding-bottom: 10px;
}
.pf-ai-inline-actions {
  display: flex; align-items: center; gap: 6px; justify-content: flex-end;
}
.pf-ai-inline--row .pf-ai-inline-actions { margin-left: auto; flex-shrink: 0; }
.pf-ai-badge { font-size: 11px; font-weight: 500; color: #a78bfa; flex-shrink: 0; }
.pf-ai-inline-val { font-size: 13px; color: #374151; font-weight: 500; flex: 1; }
.pf-ai-inline-no-match { font-size: 12px; color: #9ca3af; font-style: italic; flex: 1; }
.pf-ai-inline-dismiss {
  display: flex; align-items: center; justify-content: center;
  width: 20px; height: 20px; flex-shrink: 0;
  border: none; background: transparent; cursor: pointer;
  color: #9ca3af; border-radius: 3px; padding: 0;
  margin-left: auto;
  transition: color 0.15s;
}
.pf-ai-inline-dismiss:hover { color: #374151; }

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
  color: #6b7280; background: #f3f4f6; border-color: #e5e7eb;
}
.ai-action--dismiss:hover { background: #e5e7eb; }
.ai-action--primary {
  color: #fff; background: #7c3aed; border-color: #7c3aed;
}
.ai-action--primary:hover { background: #6d28d9; }

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
  color: #7c3aed; background: #ede9fe; border-color: #ddd6fe;
}
.ai-meta-chip--new {
  color: #6b7280; background: #f9fafb; border-color: #d1d5db; border-style: dashed;
}
.ai-meta-chip--action {
  cursor: pointer;
  transition: background 0.15s;
}
.ai-meta-chip--action.ai-meta-chip--existing:hover { background: #ddd6fe; }
.ai-meta-chip--action.ai-meta-chip--new:hover { background: #f3f4f6; }
.ai-meta-chip__badge {
  font-size: 10px; font-weight: 600;
  color: #9ca3af; background: #e5e7eb;
  padding: 0 4px; border-radius: 3px;
}
.ai-meta-chip__plus {
  font-size: 14px; font-weight: 400; line-height: 1;
  color: #a78bfa; margin-left: 1px;
}
.ai-meta-chip--new .ai-meta-chip__plus { color: #9ca3af; }
.ai-meta-chip--applied {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ── Editor max-width constraint ─────────────────────────────────────────── */
:deep(.article-editor.is-editable .ProseMirror) {
  max-width: var(--spacing-prose);
  margin: 0 auto;
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
.btn--default { background: transparent; color: #6b7280; border-color: #e5e7eb; }
.btn--default:hover:not(:disabled) { background: #f3f4f6; border-color: #d1d5db; color: #374151; }
.btn--primary { background: var(--admin-accent, #b85c38); color: #fff; border-color: var(--admin-accent, #b85c38); font-weight: 500; }
.btn--primary:hover:not(:disabled) { background: var(--admin-accent-dark, #924530); border-color: var(--admin-accent-dark, #924530); }
.btn--cancel { background: transparent; color: #6b7280; border-color: #d4cfc9; }
.btn--cancel:hover:not(:disabled) { background: #ece9e4; border-color: #c8c2ba; }

/* ── Dialog ──────────────────────────────────────────────────────────────────── */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dialog-panel {
  background: #faf9f7;
  border: 1px solid #e8e4de;
  border-radius: 4px;
  width: 540px;
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}
.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}
.dialog-title {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 18px;
  font-weight: 600;
  color: #1a1610;
}
.dialog-x {
  display: flex; align-items: center; justify-content: center;
  width: 28px; height: 28px;
  background: none; border: none; cursor: pointer;
  color: #9ca3af; border-radius: 4px;
  transition: background 0.15s, color 0.15s;
}
.dialog-x:hover { background: #ece9e4; color: #374151; }
.dialog-body { padding: 20px 24px 16px; overflow-y: auto; flex: 1; }
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 24px 20px;
  flex-shrink: 0;
}

/* ── AI inline transition ────────────────────────────────────────────────────── */
.ai-slide-enter-active, .ai-slide-leave-active { transition: opacity 0.18s, transform 0.18s; }
.ai-slide-enter-from, .ai-slide-leave-to { opacity: 0; transform: translateY(-3px); }

/* ── Dialog transition ───────────────────────────────────────────────────────── */
.dialog-fade-enter-active { transition: opacity 0.2s ease; }
.dialog-fade-leave-active { transition: opacity 0.15s ease; }
.dialog-fade-enter-from,
.dialog-fade-leave-to { opacity: 0; }
.dialog-fade-enter-active .dialog-panel { transition: transform 0.2s ease; }
.dialog-fade-leave-active .dialog-panel { transition: transform 0.15s ease; }
.dialog-fade-enter-from .dialog-panel { transform: scale(0.97); }

/* ── Form controls ───────────────────────────────────────────────────────────── */
.pf-textarea {
  display: block;
  width: 100%;
  height: auto;
  padding: 8px 10px;
  font-size: 13px;
  font-family: inherit;
  border: 1px solid #d4cfc9;
  border-radius: 4px;
  background: #fff;
  color: #374151;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
  resize: none;
  line-height: 1.6;
}
.pf-textarea:focus { border-color: var(--admin-accent, #b85c38); }
.pf-char-count { font-size: 11px; color: #9ca3af; text-align: right; margin-top: 2px; }

/* ── Combobox ─────────────────────────────────────────────────────────────────── */
.pf-combo { position: relative; }
.pf-combo-field {
  display: flex; align-items: center;
  border: 1px solid #d4cfc9; border-radius: 4px; background: #fff;
  transition: border-color 0.15s;
  padding: 0 6px 0 10px;
}
.pf-combo-field--focused { border-color: var(--admin-accent, #b85c38); }
.pf-combo-input {
  flex: 1; height: 34px;
  border: none; outline: none; background: transparent;
  font-size: 13px; font-family: inherit; color: #374151;
}
.pf-combo-input::placeholder { color: #9ca3af; }
.pf-combo-clear {
  display: flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; flex-shrink: 0;
  border: none; background: transparent; cursor: pointer;
  color: #9ca3af; border-radius: 3px;
  transition: color 0.15s, background 0.15s;
}
.pf-combo-clear:hover { color: #374151; background: #f3f4f6; }
.pf-combo-dropdown {
  position: absolute; top: calc(100% + 4px); left: 0; right: 0;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  z-index: 200; max-height: 176px; overflow-y: auto; padding: 4px 0;
}
.pf-combo-opt {
  display: block; width: 100%; text-align: left;
  padding: 7px 12px; font-size: 13px; color: #374151;
  background: transparent; border: none; cursor: pointer;
  font-family: inherit; transition: background 0.1s;
}
.pf-combo-opt:hover { background: #f3f4f6; }
.pf-combo-opt--active { color: var(--admin-accent, #b85c38); font-weight: 500; }
.pf-combo-empty { padding: 8px 12px; font-size: 12px; color: #9ca3af; font-style: italic; }

/* ── Selected tag chips ──────────────────────────────────────────────────────── */
.pf-selected-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.pf-sel-chip {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 6px 3px 10px; font-size: 12px; font-weight: 500;
  border-radius: 4px; background: #ede9fe; color: #7c3aed; border: 1px solid #ddd6fe;
}
.pf-sel-chip-x {
  display: flex; align-items: center; justify-content: center;
  width: 16px; height: 16px; border: none; background: transparent;
  cursor: pointer; color: #a78bfa; border-radius: 2px; padding: 0;
  transition: color 0.15s, background 0.15s;
}
.pf-sel-chip-x:hover { color: #7c3aed; background: #ddd6fe; }

/* ── Checkbox ────────────────────────────────────────────────────────────────── */
.checkbox-label {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; color: #6b7280; cursor: pointer;
}
.checkbox-label input[type="checkbox"] { width: 14px; height: 14px; cursor: pointer; }

/* ── Mobile ──────────────────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .write-v2    { margin: -16px -12px; }
  .write-toolbar { padding: 0 12px; gap: 8px; }
  .save-hint   { display: none; }
  .editor-header { padding: 24px 20px 16px; }
  .title-input { font-size: 24px; }
  .editor-container { padding: 0 20px 40px; }

  .meta-sidebar {
    position: fixed; top: var(--admin-header-height); right: 0;
    height: calc(100vh - var(--admin-header-height)); z-index: 100;
    box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  }
  .meta-sidebar--closed { width: 0; opacity: 0; box-shadow: none; }
}
</style>
