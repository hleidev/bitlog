<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import { MilkdownProvider } from '@milkdown/vue'
import '@milkdown/crepe/theme/classic.css'
import '@milkdown/crepe/theme/common/style.css'
import '@/assets/styles/prose.css'
import MilkdownEditor from '@/admin/components/MilkdownEditor.vue'
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
const milkdownRef = ref<InstanceType<typeof MilkdownEditor> | null>(null)

// ── Article metadata ───────────────────────────────────────────────────────────
const latestVersionId    = ref<number | null>(null)
const publishedVersionId = ref<number | null>(null)
const isPublished        = computed(() => publishedVersionId.value !== null)

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

// ── Category / tag selects ─────────────────────────────────────────────────────
const categorySelectVal = ref<number | string | null>(null)
const tagSelectVals     = ref<(number)[]>([])
const categoryOptions   = ref<Category[]>([])
const tagOptions        = ref<Tag[]>([])
const showNewCatInput   = ref(false)
const newCategoryName   = ref('')
const newTagName        = ref('')

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
  if (hasUnsaved.value) e.preventDefault()
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
    setTimeout(() => { suppressChange = false }, 200)
    return
  }

  loading.value = true
  suppressChange = true
  try {
    const data = await getArticleDraft(articleId!)
    title.value              = data.title
    content.value            = data.content
    latestVersionId.value    = data.latestVersionId
    publishedVersionId.value = data.publishedVersionId
    publishForm.value = {
      summary:    data.summary ?? '',
      categoryId: data.categoryId,
      tagIds:     [...data.tagIds],
    }
    categorySelectVal.value = data.categoryId
    tagSelectVals.value     = [...data.tagIds]
    await loadVersions()
    saveState.value = 'saved'
  } catch (err) {
    handleError(err, '加载文章失败')
  } finally {
    loading.value = false
    await nextTick()
    autoResizeTitle()
    setTimeout(() => { suppressChange = false }, 200)
  }
}

// ── Category / tag handlers ────────────────────────────────────────────────────
function onCategorySelect() {
  if (categorySelectVal.value === '__new__') {
    showNewCatInput.value = true
    categorySelectVal.value = null
  } else {
    publishForm.value.categoryId = categorySelectVal.value as number | null
  }
}

async function confirmNewCategory() {
  const name = newCategoryName.value.trim()
  if (!name) return
  try {
    const id = await getOrCreateCategory(name)
    categorySelectVal.value  = id
    publishForm.value.categoryId = id
    showNewCatInput.value    = false
    newCategoryName.value    = ''
    if (!categoryOptions.value.find(c => c.id === id)) {
      categoryOptions.value = [...categoryOptions.value, { id, name, articleCount: 0, createTime: '' }]
    }
  } catch {
    toast.error('创建分类失败')
  }
}

function cancelNewCategory() {
  showNewCatInput.value = false
  newCategoryName.value = ''
  categorySelectVal.value = null
}

function toggleTag(id: number) {
  const idx = tagSelectVals.value.indexOf(id)
  if (idx >= 0) {
    tagSelectVals.value = tagSelectVals.value.filter(i => i !== id)
  } else {
    tagSelectVals.value = [...tagSelectVals.value, id]
  }
  publishForm.value.tagIds = [...tagSelectVals.value]
}

async function addNewTag() {
  const name = newTagName.value.trim()
  if (!name) return
  try {
    const id = await getOrCreateTag(name)
    if (!tagSelectVals.value.includes(id)) {
      tagSelectVals.value = [...tagSelectVals.value, id]
      publishForm.value.tagIds = [...tagSelectVals.value]
    }
    if (!tagOptions.value.find(t => t.id === id)) {
      tagOptions.value = [...tagOptions.value, { id, name, articleCount: 0, createTime: '', updateTime: '' }]
    }
    newTagName.value = ''
  } catch {
    toast.error('创建标签失败')
  }
}

// ── Save ───────────────────────────────────────────────────────────────────────
async function performSave() {
  if (!title.value.trim() || saving.value) return
  saving.value    = true
  saveState.value = 'saving'
  try {
    const md = milkdownRef.value?.getMarkdown() ?? ''
    if (isNew) {
      const newId = await createArticle({ title: title.value, content: md })
      router.replace(`/admin/write/${newId}`)
      saveState.value  = 'saved'
      hasUnsaved.value = false
      toast.success('已保存')
    } else {
      await updateArticleDraft(articleId!, { title: title.value, content: md })
      loadVersions()
      saveState.value  = 'saved'
      hasUnsaved.value = false
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
  if (isPublished.value) {
    window.open(`/article/${articleId}`, '_blank')
  } else {
    window.open(`/admin/preview/${articleId}`, '_blank')
  }
}

// ── Publish ────────────────────────────────────────────────────────────────────
async function openPublishDialog() {
  if (!title.value.trim()) {
    toast.warning('请先输入文章标题')
    return
  }
  const md = milkdownRef.value?.getMarkdown() ?? ''
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
    toast.success(wasPublished ? '发布信息已更新' : '文章已发布')
    loadVersions()
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
  showNewCatInput.value = false
  categorySelectVal.value = aiCatResult.value.id
  publishForm.value.categoryId = aiCatResult.value.id
  aiCatResult.value = null
}

function applyAiExistingTag(tag: { id: number; name: string }) {
  if (tagSelectVals.value.includes(tag.id)) return
  tagSelectVals.value      = [...tagSelectVals.value, tag.id]
  publishForm.value.tagIds = [...publishForm.value.tagIds, tag.id]
  if (!tagOptions.value.find(t => t.id === tag.id))
    tagOptions.value = [...tagOptions.value, { id: tag.id, name: tag.name, articleCount: 0, createTime: '', updateTime: '' }]
}

function isSuggestedTagApplied(name: string): boolean {
  const tag = tagOptions.value.find(t => t.name === name)
  return tag ? tagSelectVals.value.includes(tag.id) : false
}

async function applyAiSuggestedTag(name: string) {
  try {
    const id = await getOrCreateTag(name)
    if (!tagSelectVals.value.includes(id)) {
      tagSelectVals.value = [...tagSelectVals.value, id]
      publishForm.value.tagIds = [...tagSelectVals.value]
    }
    if (!tagOptions.value.find(t => t.id === id)) {
      tagOptions.value = [...tagOptions.value, { id, name, articleCount: 0, createTime: '', updateTime: '' }]
    }
  } catch {
    toast.error('创建标签失败')
  }
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
  await loadDraft()
  loadCategoriesAndTags()
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
  <MilkdownProvider>
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
            <span class="status-pill" :class="isPublished ? 'status-pill--published' : 'status-pill--draft'">
              {{ isPublished ? '已发布' : '草稿' }}
            </span>
          </div>

          <div class="tb-right">
            <button class="btn btn--default" @click="openPreview">预览</button>
            <button class="btn btn--default" :disabled="saving" @click="handleSave">{{ saving ? '保存中…' : '保存' }}</button>
            <button class="btn btn--primary" @click="openPublishDialog">发布</button>
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
              <MilkdownEditor ref="milkdownRef" :content="content" @change="onEditorChange" />
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
                <span class="dialog-title">{{ isPublished ? '更新发布信息' : '发布文章' }}</span>
                <button class="dialog-x" @click="publishDialogVisible = false">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
                </button>
              </div>

              <div class="dialog-body">
                <div class="publish-form">

                  <div class="pf-ai-bar">
                    <button
                      class="ai-gen-btn ai-gen-btn--full"
                      :class="{ 'ai-gen-btn--loading': aiGenerating }"
                      :disabled="aiGenerating"
                      type="button"
                      @click="runAiRecommend"
                    >
                      <span class="ai-gen-btn__icon">✦</span>
                      <span>{{ aiGenerating ? 'AI 分析中…' : 'AI 一键推荐' }}</span>
                    </button>
                  </div>

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
                    <transition name="ai-preview">
                      <div v-if="aiSummaryResult" class="ai-preview">
                        <p class="ai-preview__text">{{ aiSummaryResult }}</p>
                        <div class="ai-preview__actions">
                          <button type="button" class="ai-action ai-action--dismiss" @click="aiSummaryResult = null">丢弃</button>
                          <button type="button" class="ai-action ai-action--primary" @click="acceptAiSummary">写入摘要</button>
                        </div>
                      </div>
                    </transition>
                  </div>

                  <div class="pf-item">
                    <div class="pf-label">分类 <span class="pf-required">必填</span></div>
                    <template v-if="!showNewCatInput">
                      <select v-model="categorySelectVal" @change="onCategorySelect" class="pf-select">
                        <option :value="null" disabled>请选择分类...</option>
                        <option v-for="cat in categoryOptions" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
                        <option value="__new__">✚ 创建新分类...</option>
                      </select>
                    </template>
                    <div v-else class="pf-inline-row">
                      <input v-model="newCategoryName" placeholder="输入新分类名称" @keyup.enter="confirmNewCategory" class="pf-input" />
                      <button class="btn btn--default btn--sm" @click="confirmNewCategory">确认</button>
                      <button class="btn btn--cancel btn--sm" @click="cancelNewCategory">取消</button>
                    </div>
                    <transition name="ai-preview">
                      <div v-if="aiCatResult !== null" class="ai-preview">
                        <template v-if="aiCatResult">
                          <div class="ai-preview__meta">
                            <span class="ai-meta-chip ai-meta-chip--existing">{{ aiCatResult.name }}</span>
                          </div>
                          <div class="ai-preview__actions">
                            <button type="button" class="ai-action ai-action--dismiss" @click="aiCatResult = null">丢弃</button>
                            <button type="button" class="ai-action ai-action--primary" @click="applyAiCategory">应用</button>
                          </div>
                        </template>
                        <template v-else>
                          <p class="ai-preview__no-match">现有分类均不适配，请手动选择</p>
                          <div class="ai-preview__actions">
                            <button type="button" class="ai-action ai-action--dismiss" @click="aiCatResult = null">知道了</button>
                          </div>
                        </template>
                      </div>
                    </transition>
                  </div>

                  <div class="pf-item">
                    <div class="pf-label">标签 <span class="pf-optional">可选</span></div>
                    <div class="tag-chips">
                      <button
                        v-for="tag in tagOptions"
                        :key="tag.id"
                        type="button"
                        class="tag-chip"
                        :class="{ 'tag-chip--selected': tagSelectVals.includes(tag.id) }"
                        @click="toggleTag(tag.id)"
                      >{{ tag.name }}</button>
                      <span v-if="!tagOptions.length" class="tag-chips-empty">暂无标签，请在下方添加</span>
                    </div>
                    <div class="pf-inline-row">
                      <input v-model="newTagName" placeholder="输入新标签名称" @keyup.enter="addNewTag" class="pf-input" />
                      <button class="btn btn--default btn--sm" :disabled="!newTagName.trim()" @click="addNewTag">添加</button>
                    </div>
                    <transition name="ai-preview">
                      <div v-if="aiTagsResult" class="ai-preview">
                        <div class="ai-preview__meta">
                          <button
                            v-for="tag in aiTagsResult.existing"
                            :key="tag.id"
                            type="button"
                            :class="['ai-meta-chip ai-meta-chip--existing ai-meta-chip--action', { 'ai-meta-chip--applied': tagSelectVals.includes(tag.id) }]"
                            :disabled="tagSelectVals.includes(tag.id)"
                            @click="applyAiExistingTag(tag)"
                          >
                            {{ tag.name }}<span class="ai-meta-chip__plus">+</span>
                          </button>
                          <button
                            v-for="name in aiTagsResult.suggested"
                            :key="name"
                            type="button"
                            :class="['ai-meta-chip ai-meta-chip--new ai-meta-chip--action', { 'ai-meta-chip--applied': isSuggestedTagApplied(name) }]"
                            :disabled="isSuggestedTagApplied(name)"
                            @click="applyAiSuggestedTag(name)"
                          >
                            {{ name }}<span class="ai-meta-chip__badge">新</span><span class="ai-meta-chip__plus">+</span>
                          </button>
                        </div>
                        <div class="ai-preview__actions">
                          <button type="button" class="ai-action ai-action--dismiss" @click="aiTagsResult = null">丢弃</button>
                        </div>
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
  </MilkdownProvider>
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
.pf-ai-bar    { display: flex; justify-content: flex-end; margin-bottom: -4px; }
.pf-item      { display: flex; flex-direction: column; gap: 8px; }
.pf-label     { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 500; color: #374151; }
.pf-optional  { font-size: 11px; font-weight: 400; color: #9ca3af; background: #f3f4f6; padding: 1px 6px; border-radius: 3px; }
.pf-required  { font-size: 11px; font-weight: 500; color: #dc2626; background: #fef2f2; padding: 1px 6px; border-radius: 3px; }

/* ── AI generate button ────────────────────────────────────────────────────────*/
.ai-gen-btn {
  margin-left: auto;
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 10px;
  font-size: 12px; font-weight: 500;
  color: #7c3aed;
  background: #f5f3ff;
  border: 1px solid #ddd6fe;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
  line-height: 20px;
}
.ai-gen-btn--full { padding: 4px 14px; font-size: 13px; }
.ai-gen-btn:hover:not(:disabled) { background: #ede9fe; }
.ai-gen-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.ai-gen-btn__icon { font-size: 11px; }
.ai-gen-btn--loading .ai-gen-btn__icon {
  display: inline-block;
  animation: ai-spin 1.2s linear infinite;
}
@keyframes ai-spin { to { transform: rotate(360deg); } }

/* ── AI preview card ───────────────────────────────────────────────────────────*/
.ai-preview {
  border: 1px solid #ddd6fe;
  border-radius: 6px;
  background: #faf9ff;
  overflow: hidden;
}
.ai-preview__no-match {
  margin: 0;
  padding: 12px 16px 8px;
  font-size: 12px; color: #9ca3af; font-style: italic;
}
.ai-preview__text {
  margin: 0;
  padding: 14px 16px 10px;
  font-size: 13px; line-height: 1.7; color: #374151;
}
.ai-preview__actions {
  display: flex; justify-content: flex-end; gap: 8px;
  padding: 0 12px 12px;
}

/* ── AI action buttons ─────────────────────────────────────────────────────────*/
.ai-action {
  padding: 4px 12px;
  font-size: 12px; font-weight: 500;
  border-radius: 4px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.15s;
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
.ai-preview__meta {
  display: flex; flex-wrap: wrap; gap: 6px;
  padding: 12px 16px 10px;
}
.ai-meta-chip {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 10px;
  font-size: 12px; font-weight: 500;
  border-radius: 4px;
  border: 1px solid transparent;
  line-height: 20px;
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

/* ── Transition ────────────────────────────────────────────────────────────────*/
.ai-preview-enter-active, .ai-preview-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.ai-preview-enter-from, .ai-preview-leave-to {
  opacity: 0; transform: translateY(-4px);
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

/* ── Dialog transition ───────────────────────────────────────────────────────── */
.dialog-fade-enter-active { transition: opacity 0.2s ease; }
.dialog-fade-leave-active { transition: opacity 0.15s ease; }
.dialog-fade-enter-from,
.dialog-fade-leave-to { opacity: 0; }
.dialog-fade-enter-active .dialog-panel { transition: transform 0.2s ease; }
.dialog-fade-leave-active .dialog-panel { transition: transform 0.15s ease; }
.dialog-fade-enter-from .dialog-panel { transform: scale(0.97); }

/* ── Form controls ───────────────────────────────────────────────────────────── */
.pf-select, .pf-input, .pf-textarea {
  display: block;
  width: 100%;
  height: 36px;
  padding: 0 10px;
  font-size: 13px;
  font-family: inherit;
  border: 1px solid #d4cfc9;
  border-radius: 4px;
  background: #fff;
  color: #374151;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}
.pf-select:focus, .pf-input:focus, .pf-textarea:focus { border-color: var(--admin-accent, #b85c38); }
.pf-textarea { height: auto; padding: 8px 10px; resize: none; line-height: 1.6; }
.pf-char-count { font-size: 11px; color: #9ca3af; text-align: right; margin-top: 2px; }
.pf-inline-row { display: flex; gap: 8px; align-items: center; }
.pf-inline-row .pf-input { flex: 1; }

/* ── Tag chips ───────────────────────────────────────────────────────────────── */
.tag-chips { display: flex; flex-wrap: wrap; gap: 6px; min-height: 28px; }
.tag-chip {
  display: inline-flex; align-items: center;
  padding: 3px 10px; font-size: 12px; font-weight: 500;
  border-radius: 4px; border: 1px solid #d1d5db;
  background: #f9fafb; color: #6b7280;
  cursor: pointer; transition: all 0.15s;
  font-family: inherit; line-height: 20px;
}
.tag-chip:hover { background: #f3f4f6; border-color: #9ca3af; }
.tag-chip--selected { background: #ede9fe; color: #7c3aed; border-color: #ddd6fe; }
.tag-chip--selected:hover { background: #e5d9fd; }
.tag-chips-empty { font-size: 12px; color: #9ca3af; font-style: italic; align-self: center; }

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
