<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Fold, Expand } from '@element-plus/icons-vue'
import { MdEditor, NormalToolbar } from 'md-editor-v3'
import type { ToolbarNames, ExposeParam } from 'md-editor-v3'
import { Image as ImageIcon } from 'lucide-vue-next'
import 'md-editor-v3/lib/style.css'
import { diffLines, diffWords } from 'diff'
import {
  createArticle,
  updateArticleDraft,
  getArticleDraft,
  publishArticle,
  getArticleVersions,
  getArticleVersionDetail,
  rollbackVersion,
  generateAiMetadata,
  deleteArticleVersions,
  type ArticleVersionVO,
  type ArticleVersionDetailVO,
} from '@/api/admin/article'
import { getCategories, getOrCreateCategory, type Category } from '@/api/admin/category'
import { getTags, getOrCreateTag, type Tag } from '@/api/admin/tag'
import { uploadFile } from '@/api/file'
import { ApiError } from '@/utils/request'

const route  = useRoute()
const router = useRouter()

const currentId = ref<number | null>(route.params.id ? Number(route.params.id) : null)
const isEdit    = computed(() => currentId.value !== null)

// ── Editor state ───────────────────────────────────────────────────────────────
const title   = ref('')
const content = ref('')

// ── Article metadata (populated from draft detail) ─────────────────────────────
const latestVersionId    = ref<number | null>(null)
const publishedVersionId = ref<number | null>(null)

const isPublished = computed(() => publishedVersionId.value !== null)

// ── Toolbar config ─────────────────────────────────────────────────────────────
const toolbars: ToolbarNames[] = [
  'bold', 'italic', 'strikeThrough', '-',
  'title', 'quote', '-',
  'unorderedList', 'orderedList', 'task', '-',
  'codeRow', 'code', 'link', 0, 'table',
  '=',
  'revoke', 'next', '-',
  'preview', 'pageFullscreen',
]

// ── Publish dialog ─────────────────────────────────────────────────────────────
const publishDialogVisible = ref(false)
const publishing           = ref(false)

const publishForm = ref({
  summary:    '',
  categoryId: null as number | null,
  tagIds:     [] as number[],
})

// ── AI metadata recommendation ────────────────────────────────────────────────
const aiGenerating    = ref(false)
const aiSummaryResult = ref<string | null>(null)
// null = not shown, false = shown but no match, object = match found
const aiCatResult     = ref<{ id: number; name: string } | false | null>(null)
const aiTagsResult    = ref<{ existing: Array<{ id: number; name: string }>; suggested: string[] } | null>(null)

async function runAiRecommend() {
  if (!currentId.value) return
  aiGenerating.value    = true
  aiSummaryResult.value = null
  aiCatResult.value     = null
  aiTagsResult.value    = null
  try {
    const data            = await generateAiMetadata(currentId.value)
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
  categorySelectVal.value = aiCatResult.value.id
  handleCategoryChange(aiCatResult.value.id)
  aiCatResult.value = null
}

function applyAiExistingTag(tag: { id: number; name: string }) {
  if (tagSelectVals.value.includes(tag.id)) return
  tagSelectVals.value      = [...tagSelectVals.value, tag.id]
  publishForm.value.tagIds = [...publishForm.value.tagIds, tag.id]
  if (!tagOptions.value.find(t => t.id === tag.id))
    tagOptions.value = [...tagOptions.value, { id: tag.id, name: tag.name, articleCount: 0, createTime: '', updateTime: '' }]
}

function applyAiSuggestedTag(name: string) {
  if (!tagSelectVals.value.includes(name)) tagSelectVals.value = [...tagSelectVals.value, name]
}

// ── Content image upload ──────────────────────────────────────────────────────
const editorRef    = ref<ExposeParam>()
const imgInputRef  = ref<HTMLInputElement | null>(null)

async function onImgFileChange(e: Event) {
  const files = [...((e.target as HTMLInputElement).files ?? [])]
  if (imgInputRef.value) imgInputRef.value.value = ''
  if (!files.length) return
  try {
    const results = await Promise.all(files.map(f => uploadFile(f, 'article')))
    results.forEach(r => editorRef.value?.insert(() => ({ targetValue: `![](${r.fileUrl})` })))
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '图片上传失败')
  }
}

// Separate select models to handle allow-create string phase before API resolves
const categorySelectVal = ref<number | string | null>(null)
const tagSelectVals     = ref<(number | string)[]>([])
const creatingMeta      = ref(false)

const categoryOptions = ref<Category[]>([])
const tagOptions      = ref<Tag[]>([])

async function loadCategoriesAndTags() {
  try {
    const [cats, tags] = await Promise.all([getCategories(), getTags()])
    categoryOptions.value = cats
    tagOptions.value      = tags
  } catch {
    // non-critical
  }
}

async function handleCategoryChange(val: number | string | null) {
  if (typeof val !== 'string') {
    publishForm.value.categoryId = val as number | null
    return
  }
  const name = val.trim()
  if (!name) {
    categorySelectVal.value      = null
    publishForm.value.categoryId = null
    return
  }
  creatingMeta.value = true
  try {
    const id = await getOrCreateCategory(name)
    categorySelectVal.value      = id
    publishForm.value.categoryId = id
    // Add to local options if newly created so the label shows correctly
    if (!categoryOptions.value.find(c => c.id === id)) {
      categoryOptions.value = [...categoryOptions.value, { id, name, articleCount: 0, createTime: '' }]
    }
  } catch {
    categorySelectVal.value      = null
    publishForm.value.categoryId = null
    ElMessage.error('创建分类失败')
  } finally {
    creatingMeta.value = false
  }
}

async function handleTagsChange(vals: (number | string)[]) {
  const existingIds = vals.filter((v): v is number => typeof v === 'number')
  const newNames    = vals.filter((v): v is string => typeof v === 'string').map(s => s.trim()).filter(Boolean)
  if (!newNames.length) {
    publishForm.value.tagIds = existingIds
    return
  }
  creatingMeta.value = true
  try {
    const newIds = await Promise.all(newNames.map(name => getOrCreateTag(name)))
    for (let i = 0; i < newNames.length; i++) {
      if (!tagOptions.value.find(t => t.id === newIds[i])) {
        tagOptions.value = [...tagOptions.value, { id: newIds[i], name: newNames[i], articleCount: 0, createTime: '', updateTime: '' }]
      }
    }
    const merged = [...new Set([...existingIds, ...newIds])]
    tagSelectVals.value      = merged
    publishForm.value.tagIds = merged
  } catch {
    tagSelectVals.value      = existingIds
    publishForm.value.tagIds = existingIds
    ElMessage.error('创建标签失败')
  } finally {
    creatingMeta.value = false
  }
}

// ── Sidebar & versions ─────────────────────────────────────────────────────────
const sidebarOpen = ref(false)
const versions    = ref<ArticleVersionVO[]>([])

async function loadVersions() {
  if (!currentId.value) return
  try {
    const data = await getArticleVersions(currentId.value)
    versions.value = data
    const latest = data.find(v => v.latest)
    if (latest) latestVersionId.value = latest.id
  } catch {
    // non-critical
  }
}

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
  if (!selectedVersionIds.value.length || !currentId.value) return
  const count = selectedVersionIds.value.length
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${count} 个版本？此操作不可恢复。`,
      '删除版本',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  deletingVersions.value = true
  try {
    await deleteArticleVersions(currentId.value, [...selectedVersionIds.value])
    await loadVersions()
    exitVersionManage()
    ElMessage.success(`已删除 ${count} 个版本`)
  } catch (err) {
    handleError(err, '删除版本失败')
  } finally {
    deletingVersions.value = false
  }
}

// ── Diff state ─────────────────────────────────────────────────────────────────
const diffMode    = ref(false)
const diffVersion = ref<ArticleVersionDetailVO | null>(null)
const diffLoading = ref(false)

const hasTitleDiff = computed(() =>
  !!diffVersion.value && diffVersion.value.title !== title.value
)

const titleDiff = computed(() => {
  if (!diffVersion.value) return []
  return diffWords(diffVersion.value.title, title.value)
})

const renderedContentLines = computed(() => {
  if (!diffVersion.value) return []
  const changes = diffLines(diffVersion.value.content, content.value)
  const lines: { text: string; added: boolean; removed: boolean }[] = []
  for (const part of changes) {
    const parts = part.value.split('\n')
    if (parts[parts.length - 1] === '') parts.pop()
    for (const text of parts) {
      lines.push({ text, added: !!part.added, removed: !!part.removed })
    }
  }
  return lines
})

async function enterDiff(v: ArticleVersionVO) {
  if (diffLoading.value || !currentId.value) return
  diffLoading.value = true
  try {
    const detail = await getArticleVersionDetail(currentId.value, v.id)
    diffVersion.value = detail
    diffMode.value    = true
  } catch {
    ElMessage.error('加载版本内容失败')
  } finally {
    diffLoading.value = false
  }
}

function exitDiff() {
  diffMode.value    = false
  diffVersion.value = null
}

// ── Save state ─────────────────────────────────────────────────────────────────
const saveState = ref<'idle' | 'saving' | 'saved'>(isEdit.value ? 'saved' : 'idle')
const saving    = ref(false)
let saveTimer: ReturnType<typeof setTimeout> | undefined

const hasUnsaved = ref(false)

function onBeforeUnload(e: BeforeUnloadEvent) {
  if (hasUnsaved.value) e.preventDefault()
}

onMounted(() => window.addEventListener('beforeunload', onBeforeUnload))
onUnmounted(() => {
  clearTimeout(saveTimer)
  window.removeEventListener('beforeunload', onBeforeUnload)
})

onBeforeRouteLeave(async () => {
  if (!hasUnsaved.value) return true
  try {
    await ElMessageBox.confirm('你有未保存的修改，确认离开吗？', '离开页面', {
      confirmButtonText: '离开',
      cancelButtonText: '取消',
      type: 'warning',
    })
    return true
  } catch {
    return false
  }
})

const saveStateText = computed(() => {
  if (saveState.value === 'saving') return '保存中...'
  if (saveState.value === 'saved')  return '已保存'
  return ''
})

async function performSave() {
  if (!title.value.trim() || saving.value) return
  saving.value    = true
  saveState.value = 'saving'
  try {
    if (currentId.value === null) {
      const newId = await createArticle({ title: title.value, content: content.value })
      currentId.value = newId
      router.replace(`/admin/write/${newId}`)
      sidebarOpen.value = true
      const [data, versionData] = await Promise.all([getArticleDraft(newId), getArticleVersions(newId)])
      latestVersionId.value    = data.latestVersionId
      publishedVersionId.value = data.publishedVersionId
      versions.value           = versionData
    } else {
      await updateArticleDraft(currentId.value, { title: title.value, content: content.value })
      loadVersions()
    }
    saveState.value = 'saved'
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

// ── Change tracking (no auto-save) ────────────────────────────────────────────
let suppressAutoSave = false

watch([title, content], () => {
  if (suppressAutoSave) return
  if (!title.value.trim()) return
  hasUnsaved.value = true
  saveState.value = 'idle'
})

// ── Load draft ─────────────────────────────────────────────────────────────────
const pageLoading = ref(false)

async function loadDraft() {
  if (!currentId.value) return
  pageLoading.value  = true
  suppressAutoSave   = true
  try {
    const data = await getArticleDraft(currentId.value)
    title.value              = data.title
    content.value            = data.content
    latestVersionId.value    = data.latestVersionId
    publishedVersionId.value = data.publishedVersionId
    publishForm.value = {
      summary:    data.summary ?? '',
      categoryId: data.category?.id ?? null,
      tagIds:     data.tags.map(t => t.id),
    }
    categorySelectVal.value = data.category?.id ?? null
    tagSelectVals.value     = data.tags.map(t => t.id)
    await loadVersions()
    // Wait for Vue to flush the watchers triggered by title/content assignment
    // before re-enabling auto-save, so the load itself never triggers a save.
    await nextTick()
    saveState.value = 'saved'
  } catch (err) {
    handleError(err, '加载文章失败')
  } finally {
    suppressAutoSave  = false
    pageLoading.value = false
  }
}

onMounted(() => {
  if (isEdit.value) loadDraft()
  loadCategoriesAndTags()
})

// ── Error helper ───────────────────────────────────────────────────────────────
function handleError(err: unknown, fallback = '操作失败') {
  const msg = err instanceof ApiError ? err.message : null
  ElMessage.error(msg || fallback)
}

// ── Actions ────────────────────────────────────────────────────────────────────
async function handleSave() {
  if (!title.value.trim()) {
    ElMessage.warning('请先输入文章标题')
    return
  }
  await performSave()
  hasUnsaved.value = false
}

function openPreview() {
  // 有未发布的草稿内容（latestVersionId !== publishedVersionId）→ 预览草稿页
  // 内容一致或从未发布过 → 预览公开页
  const hasUnpublishedChanges = latestVersionId.value !== null &&
    publishedVersionId.value !== null &&
    latestVersionId.value !== publishedVersionId.value

  if (hasUnpublishedChanges) {
    window.open(`/admin/preview/${currentId.value}`, '_blank')
  } else {
    window.open(`/article/${currentId.value}`, '_blank')
  }
}

async function openPublishDialog() {
  if (!title.value.trim()) {
    ElMessage.warning('请先输入文章标题')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('请先输入文章内容')
    return
  }
  // Ensure the article is created/saved before opening publish dialog
  if (currentId.value === null || saveState.value !== 'saved') {
    await performSave()
    if (!currentId.value) return
  }
  aiSummaryResult.value = null
  aiCatResult.value     = null
  aiTagsResult.value    = null
  aiGenerating.value    = false
  publishDialogVisible.value = true
}

async function handlePublishConfirm() {
  if (!currentId.value) return
  if (!publishForm.value.categoryId) {
    ElMessage.warning('请选择文章分类')
    return
  }
  publishing.value = true
  const wasPublished = isPublished.value
  try {
    await publishArticle(currentId.value, {
      summary:    publishForm.value.summary || null,
      categoryId: publishForm.value.categoryId,
      tagIds:     publishForm.value.tagIds,
    })
    publishedVersionId.value   = latestVersionId.value
    publishDialogVisible.value = false
    hasUnsaved.value           = false
    ElMessage.success(wasPublished ? '发布信息已更新' : '文章已发布')
    loadVersions() // fire-and-forget: refresh sidebar version list in background
  } catch (err) {
    handleError(err, '发布失败')
  } finally {
    publishing.value = false
  }
}

async function handleRollback(v: ArticleVersionDetailVO) {
  try {
    await ElMessageBox.confirm(
      `回滚到版本 ${v.version}？将基于该版本创建新草稿。`,
      '回滚版本',
      { confirmButtonText: '确认回滚', cancelButtonText: '取消' },
    )
  } catch { return }
  try {
    await rollbackVersion(currentId.value!, v.id)
    exitDiff()
    await loadDraft()
    ElMessage.success(`已回滚至版本 ${v.version}`)
  } catch (err) {
    handleError(err, '回滚失败')
  }
}

// ── Helpers ────────────────────────────────────────────────────────────────────
function shortTime(d: string) {
  return new Date(d)
    .toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
    .replace(/\//g, '-')
}
</script>

<template>
  <div class="write-view">

    <!-- ── Toolbar ─────────────────────────────────────────────────────────── -->
    <div class="write-toolbar">
      <div class="tb-left">
        <button class="back-btn" @click="router.push('/admin/articles')">
          <el-icon><ArrowLeft /></el-icon>
          <span>文章列表</span>
        </button>
        <div class="tb-sep" />
        <span class="status-pill" :class="isPublished ? 'status-pill--published' : 'status-pill--draft'">
          {{ isPublished ? '已发布' : '草稿' }}
        </span>
      </div>

      <div class="tb-right">
        <span v-if="saveStateText" class="save-hint">{{ saveStateText }}</span>

        <el-button v-if="isEdit" size="small" @click="openPreview">预览</el-button>
        <el-button size="small" :loading="saving" @click="handleSave">保存</el-button>
        <el-button size="small" type="primary" @click="openPublishDialog">发布</el-button>

        <button
          v-if="isEdit"
          class="sidebar-toggle"
          :title="sidebarOpen ? '收起侧栏' : '展开侧栏'"
          @click="sidebarOpen = !sidebarOpen"
        >
          <el-icon><component :is="sidebarOpen ? Fold : Expand" /></el-icon>
        </button>
      </div>
    </div>

    <!-- ── Body ───────────────────────────────────────────────────────────── -->
    <div class="write-body">

      <!-- Editor area -->
      <div class="editor-area" v-loading="pageLoading">

        <!-- Title -->
        <div class="title-section">
          <input
            v-model="title"
            class="title-input"
            placeholder="文章标题..."
            maxlength="200"
            :disabled="diffMode"
          />
        </div>

        <!-- Diff mode banner -->
        <Transition name="diff-banner">
          <div v-if="diffMode && diffVersion" class="diff-banner">
            <div class="diff-banner-left">
              <span class="diff-badge">对比模式</span>
              <span class="diff-desc">
                版本 {{ diffVersion.version }}（{{ shortTime(diffVersion.createTime) }}）↔ 当前版本
              </span>
            </div>
            <div class="diff-banner-right">
              <el-button size="small" @click="exitDiff">退出对比</el-button>
              <el-button size="small" type="primary" @click="handleRollback(diffVersion)">
                回滚到此版本
              </el-button>
            </div>
          </div>
        </Transition>

        <!-- Editor (hidden in diff mode) -->
        <div v-show="!diffMode" class="md-wrap">
          <MdEditor
            ref="editorRef"
            v-model="content"
            editor-id="write-editor"
            :toolbars="toolbars"
            preview-theme="github"
            code-theme="atom"
            :show-code-row-number="true"
            style="height: 100%"
            @save="handleSave"
          >
            <template #defToolbars>
              <NormalToolbar title="图片" @onClick="imgInputRef?.click()">
                <ImageIcon :size="16" />
              </NormalToolbar>
            </template>
          </MdEditor>
          <input
            ref="imgInputRef"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple
            style="display: none"
            @change="onImgFileChange"
          />
        </div>

        <!-- Diff view -->
        <div v-if="diffMode" class="diff-view">

          <!-- Title diff -->
          <div v-if="hasTitleDiff" class="diff-section">
            <div class="diff-section-label">标题变更</div>
            <div class="diff-title-row diff-title-row--removed">
              <span class="diff-marker">−</span>
              <span class="diff-title-text">{{ diffVersion!.title }}</span>
            </div>
            <div class="diff-title-row diff-title-row--added">
              <span class="diff-marker">+</span>
              <span class="diff-title-text">
                <span
                  v-for="(part, i) in titleDiff"
                  :key="i"
                  :class="{
                    'diff-word--added':   part.added,
                    'diff-word--removed': part.removed,
                  }"
                >{{ part.value }}</span>
              </span>
            </div>
          </div>

          <!-- Content diff -->
          <div class="diff-section diff-section--content">
            <div class="diff-section-label">正文变更</div>
            <div class="diff-lines">
              <div
                v-for="(line, i) in renderedContentLines"
                :key="i"
                class="diff-line"
                :class="{
                  'diff-line--added':     line.added,
                  'diff-line--removed':   line.removed,
                  'diff-line--unchanged': !line.added && !line.removed,
                }"
              >
                <span class="diff-marker">{{ line.added ? '+' : line.removed ? '−' : ' ' }}</span>
                <span class="diff-line-text">{{ line.text || ' ' }}</span>
              </div>
            </div>
          </div>

        </div>
      </div>

      <!-- Sidebar (edit mode only) -->
      <div
        v-if="isEdit"
        class="meta-sidebar"
        :class="{ 'meta-sidebar--closed': !sidebarOpen }"
      >
        <div class="sidebar-scroll">

          <!-- Version history -->
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
                  <el-checkbox
                    :model-value="allDeletableSelected"
                    :indeterminate="selectedVersionIds.length > 0 && !allDeletableSelected"
                    :disabled="deletableVersionIds.length === 0"
                    @change="toggleSelectAll"
                  >全选可删除</el-checkbox>
                </div>

                <div
                  v-for="v in versions"
                  :key="v.id"
                  class="version-item"
                  :class="{
                    'version-item--current':     v.latest,
                    'version-item--active':      !versionManageMode && diffMode && diffVersion?.id === v.id,
                    'version-item--manage':      versionManageMode,
                    'version-item--undeletable': versionManageMode && (v.latest || v.id === publishedVersionId),
                  }"
                >
                  <el-checkbox
                    v-if="versionManageMode"
                    class="version-checkbox"
                    :model-value="selectedVersionIds.includes(v.id)"
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
                    <button
                      v-else
                      class="compare-btn"
                      :class="{ 'compare-btn--active': diffMode && diffVersion?.id === v.id }"
                      :disabled="diffLoading"
                      @click="enterDiff(v)"
                    >
                      {{ diffMode && diffVersion?.id === v.id ? '对比中' : '对比' }}
                    </button>
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

    <!-- ── Publish Dialog ──────────────────────────────────────────────────── -->
    <el-dialog
      v-model="publishDialogVisible"
      :title="isPublished ? '更新发布信息' : '发布文章'"
      width="540px"
      :close-on-click-modal="false"
    >
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
          <el-input
            v-model="publishForm.summary"
            type="textarea"
            :rows="3"
            placeholder="留空则自动截取正文前 200 字..."
            :maxlength="512"
            show-word-limit
            resize="none"
          />
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
          <el-select
            v-model="categorySelectVal"
            filterable
            allow-create
            clearable
            :disabled="creatingMeta"
            placeholder="搜索或输入分类名称，按 Enter 创建"
            @change="handleCategoryChange"
          >
            <el-option v-for="cat in categoryOptions" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
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
          <el-select
            v-model="tagSelectVals"
            multiple
            filterable
            allow-create
            :disabled="creatingMeta"
            placeholder="搜索或输入标签名称，按 Enter 创建"
            @change="handleTagsChange"
          >
            <el-option v-for="t in tagOptions" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
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
                  :class="['ai-meta-chip ai-meta-chip--new ai-meta-chip--action', { 'ai-meta-chip--applied': tagSelectVals.includes(name) }]"
                  :disabled="tagSelectVals.includes(name)"
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

      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="handlePublishConfirm">
          {{ isPublished ? '更新' : '立即发布' }}
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
/* ── Layout ──────────────────────────────────────────────────────────────────── */
.write-view {
  margin: -24px;
  height: calc(100vh - var(--admin-header-height));
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
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
}

.tb-left  { display: flex; align-items: center; gap: 12px; min-width: 0; }
.tb-right { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.tb-right :deep(.el-button + .el-button) { margin-left: 0; }

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
.editor-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.title-section { padding: 24px 48px 14px; flex-shrink: 0; }

.title-input {
  display: block; width: 100%;
  font-size: 28px; font-weight: 700; color: #111827;
  border: none; outline: none; background: transparent;
  line-height: 1.35; padding: 0; font-family: inherit;
}
.title-input::placeholder { color: #d1d5db; }
.title-input:disabled     { opacity: 1; cursor: default; }

/* ── Diff banner ─────────────────────────────────────────────────────────────── */
.diff-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 48px;
  background: #fffbeb;
  border-top: 1px solid #fde68a;
  border-bottom: 1px solid #fde68a;
  flex-shrink: 0;
}

.diff-banner-enter-active,
.diff-banner-leave-active { transition: opacity 0.2s, transform 0.2s; }
.diff-banner-enter-from,
.diff-banner-leave-to     { opacity: 0; transform: translateY(-6px); }

.diff-banner-left  { display: flex; align-items: center; gap: 10px; min-width: 0; }
.diff-banner-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.diff-banner-right :deep(.el-button + .el-button) { margin-left: 0; }

.diff-badge {
  font-size: 11px; font-weight: 600;
  color: #92400e; background: #fde68a;
  padding: 2px 8px; border-radius: 4px;
  white-space: nowrap; flex-shrink: 0;
}

.diff-desc {
  font-size: 13px; color: #78350f;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* ── md-editor-v3 ────────────────────────────────────────────────────────────── */
.md-wrap { flex: 1; min-height: 0; overflow: hidden; }

:deep(.md-editor)                   { border: none !important; border-radius: 0; font-family: inherit; }
:deep(.md-editor-toolbar-wrapper)   { border-bottom: 1px solid #f0f0f0; background: #fafafa; }
:deep(.md-editor-toolbar)           { padding: 0 48px; }
:deep(.md-editor-input-wrapper)     { background: #fff; }
:deep(.md-editor-input)             { font-size: 15px; line-height: 1.8; color: #374151; padding: 28px 48px !important; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; }
:deep(.md-editor-preview-wrapper)   { background: #fff; }
:deep(.md-editor-preview)           { padding: 28px 48px !important; }
:deep(.md-editor-preview h1),
:deep(.md-editor-preview h2)        { border-bottom: 1px solid #f0f0f0; padding-bottom: 0.3em; }
:deep(.md-editor-preview code:not(pre code)) { background: #f3f4f6; color: #e53e3e; padding: 0.15em 0.4em; border-radius: 4px; font-size: 0.9em; }
:deep(.md-editor-preview pre)       { border-radius: 8px; font-size: 14px; }
:deep(.md-editor-preview ul),
:deep(.md-editor-preview ol)        { padding-left: 24px; margin-bottom: 12px; }
:deep(.md-editor-preview ul li)     { list-style: disc; }
:deep(.md-editor-preview ol li)     { list-style: decimal; }

/* ── Diff view ───────────────────────────────────────────────────────────────── */
.diff-view {
  flex: 1;
  overflow-y: auto;
  padding: 28px 48px 60px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: #fff;
}

.diff-section { display: flex; flex-direction: column; gap: 0; }

.diff-section-label {
  font-size: 11px; font-weight: 600;
  color: #9ca3af; text-transform: uppercase; letter-spacing: 0.6px;
  margin-bottom: 10px;
}

/* Title diff */
.diff-title-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 4px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
}
.diff-title-row--removed { background: #fff5f5; }
.diff-title-row--added   { background: #f0fdf4; }

.diff-title-text { flex: 1; }

/* Word-level highlighting inside title added row */
.diff-word--added   { background: #bbf7d0; border-radius: 2px; }
.diff-word--removed { background: #fecdd3; text-decoration: line-through; border-radius: 2px; }

/* Content diff */
.diff-section--content { flex: 1; }

.diff-lines {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 14px;
  line-height: 1.7;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.diff-line {
  display: flex;
  align-items: baseline;
  gap: 0;
  padding: 1px 0;
  min-height: 24px;
}

.diff-line--added     { background: #f0fdf4; }
.diff-line--removed   { background: #fff5f5; }
.diff-line--unchanged { background: #fff; }

.diff-marker {
  flex-shrink: 0;
  width: 36px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  user-select: none;
}

.diff-line--added   .diff-marker { color: #16a34a; }
.diff-line--removed .diff-marker { color: #dc2626; }
.diff-line--unchanged .diff-marker { color: #d1d5db; }

.diff-line-text {
  flex: 1;
  padding: 0 16px 0 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: #374151;
}
.diff-line--added   .diff-line-text { color: #166534; }
.diff-line--removed .diff-line-text { color: #991b1b; }

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
.version-item--active    { background: #f5f3ff; margin: 0 -4px; padding-left: 4px; padding-right: 4px; }

.version-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.version-label { font-size: 13px; font-weight: 500; color: #374151; }
.version-time  { font-size: 11px; color: #9ca3af; }

.current-dot { width: 8px; height: 8px; border-radius: 50%; background: #16a34a; flex-shrink: 0; }

.compare-btn {
  flex-shrink: 0; font-size: 12px; color: #4338ca;
  background: transparent; border: 1px solid #c7d2fe;
  border-radius: 4px; padding: 3px 10px; cursor: pointer;
  transition: background 0.1s, border-color 0.1s, color 0.1s;
}
.compare-btn:hover       { background: #ede9fe; border-color: #a5b4fc; }
.compare-btn--active     { background: #4338ca; border-color: #4338ca; color: #fff; }
.compare-btn--active:hover { background: #3730a3; border-color: #3730a3; }
.compare-btn:disabled    { opacity: 0.5; cursor: not-allowed; }

.version-select-all {
  padding: 8px 0 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 2px;
}
.version-select-all :deep(.el-checkbox__label) { font-size: 12px; color: #6b7280; }

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
.pf-item :deep(.el-select) { width: 100%; }

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
.ai-preview__loading {
  display: flex; align-items: center; gap: 6px;
  padding: 14px 16px;
  color: #7c3aed; font-size: 13px;
}
.ai-preview__dot {
  display: inline-block;
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #7c3aed;
  animation: ai-dot 1.2s infinite ease-in-out;
}
.ai-preview__dot:nth-child(2) { animation-delay: 0.2s; }
.ai-preview__dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes ai-dot {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40%            { transform: scale(1);   opacity: 1; }
}
.ai-preview__hint { margin-left: 4px; }
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
.ai-action--secondary {
  color: #7c3aed; background: #ede9fe; border-color: #ddd6fe;
}
.ai-action--secondary:hover { background: #ddd6fe; }
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

/* ── Mobile ──────────────────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .write-view     { margin: -16px -12px; }
  .write-toolbar  { padding: 0 12px; gap: 8px; }
  .save-hint { display: none; }
  .title-section  { padding: 24px 20px 14px; }
  .title-input    { font-size: 22px; }
  .diff-banner    { padding: 10px 20px; }
  .diff-view      { padding: 20px 20px 40px; }
  :deep(.md-editor-toolbar) { padding: 0 12px; }
  :deep(.md-editor-input)   { padding: 20px !important; }
  :deep(.md-editor-preview) { padding: 20px !important; }

  .meta-sidebar {
    position: fixed; top: var(--admin-header-height); right: 0;
    height: calc(100vh - var(--admin-header-height)); z-index: 100;
    box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  }
  .meta-sidebar--closed { width: 0; opacity: 0; box-shadow: none; }
}
</style>
