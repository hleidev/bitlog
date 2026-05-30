<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import { Crepe, CrepeFeature } from '@milkdown/crepe'
import '@milkdown/crepe/theme/frame.css'
import { getMarkdown, replaceAll } from '@milkdown/kit/utils'
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

const route   = useRoute()
const router  = useRouter()
const toast   = useToast()
const confirm = useConfirm()

const currentId = ref<number | null>(route.params.id ? Number(route.params.id) : null)
const isEdit    = computed(() => currentId.value !== null)

// ── Crepe editor ───────────────────────────────────────────────────────────────
const editorContainer = ref<HTMLDivElement>()
let crepe: Crepe | null = null

async function initCrepe(initialMarkdown = '') {
  if (!editorContainer.value) return
  crepe = new Crepe({
    root: editorContainer.value,
    defaultValue: initialMarkdown,
    features: {
      [CrepeFeature.BlockEdit]:       true,
      [CrepeFeature.Cursor]:         true,
      [CrepeFeature.ImageBlock]:     true,
      [CrepeFeature.LinkTooltip]:    true,
      [CrepeFeature.ListItem]:       true,
      [CrepeFeature.Placeholder]:    true,
      [CrepeFeature.Table]:          true,
      [CrepeFeature.Toolbar]:        true,
    },
    featureConfigs: {
      [CrepeFeature.Placeholder]: { text: '开始写作...' },
      [CrepeFeature.ImageBlock]: {
        onUpload: async (file: File) => {
          const result = await uploadFile(file, 'article')
          return result.fileUrl
        },
      },
    },
  })

  await crepe.create()

  crepe.on((listener) => {
    listener.markdownUpdated((_ctx, markdown) => {
      if (suppressFirstMarkdownUpdate) {
        suppressFirstMarkdownUpdate = false
        return
      }
      content.value = markdown
    })
  })
}

function crepeDestroy() {
  if (crepe) {
    crepe.destroy()
    crepe = null
  }
}

function getCrepeMarkdown(): string {
  if (!crepe) return content.value
  return crepe.getMarkdown()
}

function setCrepeMarkdown(md: string) {
  if (crepe) {
    crepe.action(replaceAll(md))
  }
  content.value = md
}

// ── Article metadata ───────────────────────────────────────────────────────────
const latestVersionId    = ref<number | null>(null)
const publishedVersionId = ref<number | null>(null)
const isPublished = computed(() => publishedVersionId.value !== null)

// ── Publish dialog ─────────────────────────────────────────────────────────────
const publishDialogVisible = ref(false)
const publishing           = ref(false)

const publishForm = ref({
  summary:    '',
  categoryId: null as number | null,
  tagIds:     [] as number[],
})

// ── AI metadata recommendation ─────────────────────────────────────────────────
const aiGenerating    = ref(false)
const aiSummaryResult = ref<string | null>(null)
const aiCatResult     = ref<{ id: number; name: string } | false | null>(null)
const aiTagsResult    = ref<{ existing: Array<{ id: number; name: string }>; suggested: string[] } | null>(null)

async function runAiRecommend() {
  if (!currentId.value) return
  aiGenerating.value = true
  aiSummaryResult.value = aiCatResult.value = aiTagsResult.value = null
  try {
    const data = await generateAiMetadata(currentId.value)
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
  selectCategory(aiCatResult.value.id, aiCatResult.value.name)
  aiCatResult.value = null
}

function applyAiExistingTag(tag: { id: number; name: string }) {
  if (publishForm.value.tagIds.includes(tag.id)) return
  publishForm.value.tagIds = [...publishForm.value.tagIds, tag.id]
  if (!tagOptions.value.find(t => t.id === tag.id))
    tagOptions.value = [...tagOptions.value, { id: tag.id, name: tag.name, articleCount: 0, createTime: '', updateTime: '' }]
}

function applyAiSuggestedTag(name: string) {
  if (!pendingTagNames.value.includes(name)) pendingTagNames.value = [...pendingTagNames.value, name]
}

// ── Category combobox ──────────────────────────────────────────────────────────
const catSearch     = ref('')
const catDropOpen   = ref(false)
const catInputRef   = ref<HTMLInputElement | null>(null)

const selectedCategoryName = computed(() => {
  const id = publishForm.value.categoryId
  if (!id) return ''
  return categoryOptions.value.find(c => c.id === id)?.name ?? ''
})

const filteredCats = computed(() => {
  const q = catSearch.value.trim().toLowerCase()
  if (!q) return categoryOptions.value
  return categoryOptions.value.filter(c => c.name.toLowerCase().includes(q))
})

const canCreateCat = computed(() => {
  const q = catSearch.value.trim()
  return q.length > 0 && !categoryOptions.value.some(c => c.name === q)
})

function openCatDrop() {
  catSearch.value = ''
  catDropOpen.value = true
  nextTick(() => catInputRef.value?.focus())
}

function closeCatDrop() { catDropOpen.value = false }

function selectCategory(id: number, name: string) {
  publishForm.value.categoryId = id
  catSearch.value  = ''
  catDropOpen.value = false
  if (!categoryOptions.value.find(c => c.id === id))
    categoryOptions.value = [...categoryOptions.value, { id, name, articleCount: 0, createTime: '' }]
}

function clearCategory() {
  publishForm.value.categoryId = null
  catSearch.value = ''
}

async function createCategory() {
  const name = catSearch.value.trim()
  if (!name) return
  creatingMeta.value = true
  closeCatDrop()
  try {
    const id = await getOrCreateCategory(name)
    selectCategory(id, name)
  } catch {
    toast.error('创建分类失败')
  } finally {
    creatingMeta.value = false
  }
}

// ── Tag multi-select ───────────────────────────────────────────────────────────
const tagSearch     = ref('')
const tagDropOpen   = ref(false)
const tagInputRef   = ref<HTMLInputElement | null>(null)
const pendingTagNames = ref<string[]>([])

const filteredTagOptions = computed(() => {
  const q     = tagSearch.value.trim().toLowerCase()
  const taken = new Set(publishForm.value.tagIds)
  const list  = tagOptions.value.filter(t => !taken.has(t.id))
  if (!q) return list
  return list.filter(t => t.name.toLowerCase().includes(q))
})

const canCreateTag = computed(() => {
  const q = tagSearch.value.trim()
  if (!q) return false
  const existsInOptions  = tagOptions.value.some(t => t.name === q)
  const existsInPending  = pendingTagNames.value.includes(q)
  const existsInSelected = tagOptions.value.filter(t => publishForm.value.tagIds.includes(t.id)).some(t => t.name === q)
  return !existsInOptions && !existsInPending && !existsInSelected
})

function openTagDrop() {
  tagSearch.value = ''
  tagDropOpen.value = true
  nextTick(() => tagInputRef.value?.focus())
}

function closeTagDrop() { tagDropOpen.value = false }

function addExistingTag(id: number) {
  if (!publishForm.value.tagIds.includes(id))
    publishForm.value.tagIds = [...publishForm.value.tagIds, id]
  tagSearch.value = ''
  tagInputRef.value?.focus()
}

function addNewTagName() {
  const name = tagSearch.value.trim()
  if (!name || pendingTagNames.value.includes(name)) return
  pendingTagNames.value = [...pendingTagNames.value, name]
  tagSearch.value = ''
  tagInputRef.value?.focus()
}

function removeTag(id: number) {
  publishForm.value.tagIds = publishForm.value.tagIds.filter(t => t !== id)
}

function removePendingTag(name: string) {
  pendingTagNames.value = pendingTagNames.value.filter(n => n !== name)
}

const creatingMeta = ref(false)

async function resolveNewTags(): Promise<number[]> {
  if (!pendingTagNames.value.length) return []
  creatingMeta.value = true
  try {
    const newIds = await Promise.all(pendingTagNames.value.map(name => getOrCreateTag(name)))
    for (let i = 0; i < pendingTagNames.value.length; i++) {
      if (!tagOptions.value.find(t => t.id === newIds[i]))
        tagOptions.value = [...tagOptions.value, { id: newIds[i], name: pendingTagNames.value[i], articleCount: 0, createTime: '', updateTime: '' }]
    }
    pendingTagNames.value = []
    return newIds
  } catch {
    toast.error('创建标签失败')
    return []
  } finally {
    creatingMeta.value = false
  }
}

const categoryOptions = ref<Category[]>([])
const tagOptions      = ref<Tag[]>([])

async function loadCategoriesAndTags() {
  try {
    const [cats, tags] = await Promise.all([getCategories(), getTags()])
    categoryOptions.value = cats
    tagOptions.value      = tags
  } catch { /* non-critical */ }
}

// ── Content state (synced from Crepe) ─────────────────────────────────────────
const content = ref('')

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
  } catch { /* non-critical */ }
}

// ── Version management ─────────────────────────────────────────────────────────
const versionManageMode  = ref(false)
const selectedVersionIds = ref<number[]>([])
const deletingVersions   = ref(false)

const deletableVersionIds = computed(() =>
  versions.value.filter(v => !v.latest && v.id !== publishedVersionId.value).map(v => v.id)
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
  try { await confirm(`确定删除选中的 ${count} 个版本？此操作不可恢复。`, '删除版本', { confirmText: '删除', danger: true }) }
  catch { return }
  deletingVersions.value = true
  try {
    await deleteArticleVersions(currentId.value, [...selectedVersionIds.value])
    await loadVersions()
    exitVersionManage()
    toast.success(`已删除 ${count} 个版本`)
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

const hasTitleDiff = computed(() => !!diffVersion.value && diffVersion.value.title !== title.value)

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
    for (const text of parts) lines.push({ text, added: !!part.added, removed: !!part.removed })
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
    toast.error('加载版本内容失败')
  } finally {
    diffLoading.value = false
  }
}

function exitDiff() { diffMode.value = false; diffVersion.value = null }

// ── Save state ────────────────────────────────────────────────────────────────
const saveState = ref<'idle' | 'saving' | 'saved'>(isEdit.value ? 'saved' : 'idle')
const saving    = ref(false)
let saveTimer: ReturnType<typeof setTimeout> | undefined

const hasUnsaved = ref(false)

function onBeforeUnload(e: BeforeUnloadEvent) {
  if (hasUnsaved.value) e.preventDefault()
}

onMounted(() => window.addEventListener('beforeunload', onBeforeUnload))
onUnmounted(() => { clearTimeout(saveTimer); window.removeEventListener('beforeunload', onBeforeUnload) })

onBeforeRouteLeave(async () => {
  if (!hasUnsaved.value) return true
  try {
    await confirm('你有未保存的修改，确认离开吗？', '离开页面', { confirmText: '离开' })
    return true
  } catch { return false }
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
  const markdown = getCrepeMarkdown()
  try {
    if (currentId.value === null) {
      const newId = await createArticle({ title: title.value, content: markdown })
      currentId.value = newId
      router.replace(`/admin/write/${newId}`)
      sidebarOpen.value = true
      const [data, versionData] = await Promise.all([getArticleDraft(newId), getArticleVersions(newId)])
      latestVersionId.value    = data.latestVersionId
      publishedVersionId.value = data.publishedVersionId
      versions.value           = versionData
    } else {
      await updateArticleDraft(currentId.value, { title: title.value, content: markdown })
      loadVersions()
    }
    saveState.value = 'saved'
    clearTimeout(saveTimer)
    saveTimer = setTimeout(() => { if (saveState.value === 'saved') saveState.value = 'idle' }, 3000)
  } catch (err) {
    saveState.value = 'idle'
    handleError(err, '保存失败')
  } finally {
    saving.value = false
  }
}

let suppressAutoSave = false
let suppressFirstMarkdownUpdate = false

watch([title, content], () => {
  if (suppressAutoSave) return
  if (!title.value.trim()) return
  hasUnsaved.value = true
  saveState.value = 'idle'
})

const pageLoading = ref(false)

async function loadDraft() {
  if (!currentId.value) return
  pageLoading.value = true
  suppressAutoSave  = true
  try {
    const data = await getArticleDraft(currentId.value)
    title.value              = data.title
    latestVersionId.value    = data.latestVersionId
    publishedVersionId.value = data.publishedVersionId
    publishForm.value = {
      summary:    data.summary ?? '',
      categoryId: data.categoryId,
      tagIds:     [...data.tagIds],
    }
    suppressFirstMarkdownUpdate = true
    setCrepeMarkdown(data.content)
    content.value = data.content
    await loadVersions()
    await nextTick()
    saveState.value = 'saved'
  } catch (err) {
    handleError(err, '加载文章失败')
  } finally {
    suppressAutoSave  = false
    pageLoading.value = false
  }
}

// ── Keyboard shortcut: Ctrl+S ─────────────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSave()
  }
}

onMounted(async () => {
  window.addEventListener('keydown', handleKeydown)
  await initCrepe()
  if (isEdit.value) await loadDraft()
  await loadCategoriesAndTags()
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  crepeDestroy()
})

// ── Error helper ───────────────────────────────────────────────────────────────
function handleError(err: unknown, fallback = '操作失败') {
  toast.error((err instanceof ApiError ? err.message : null) || fallback)
}

// ── Actions ─────────────────────────────────────────────────────────────────
async function handleSave() {
  if (!title.value.trim()) { toast.warning('请先输入文章标题'); return }
  await performSave()
  hasUnsaved.value = false
}

function openPreview() {
  if (isPublished.value) window.open(`/article/${currentId.value}`, '_blank')
  else                   window.open(`/admin/preview/${currentId.value}`, '_blank')
}

async function openPublishDialog() {
  if (!title.value.trim())   { toast.warning('请先输入文章标题'); return }
  if (!getCrepeMarkdown().trim()) { toast.warning('请先输入文章内容'); return }
  if (currentId.value === null || saveState.value !== 'saved') {
    await performSave()
    if (!currentId.value) return
  }
  aiSummaryResult.value = aiCatResult.value = aiTagsResult.value = null
  aiGenerating.value = false
  pendingTagNames.value = []
  publishDialogVisible.value = true
}

async function handlePublishConfirm() {
  if (!currentId.value) return
  if (!publishForm.value.categoryId) { toast.warning('请选择文章分类'); return }
  const newTagIds = await resolveNewTags()
  const allTagIds = [...new Set([...publishForm.value.tagIds, ...newTagIds])]
  publishing.value = true
  const wasPublished = isPublished.value
  try {
    await publishArticle(currentId.value, {
      summary:    publishForm.value.summary || null,
      categoryId: publishForm.value.categoryId,
      tagIds:     allTagIds,
    })
    publishForm.value.tagIds    = allTagIds
    publishedVersionId.value   = latestVersionId.value
    publishDialogVisible.value  = false
    hasUnsaved.value            = false
    toast.success(wasPublished ? '发布信息已更新' : '文章已发布')
    loadVersions()
  } catch (err) {
    handleError(err, '发布失败')
  } finally {
    publishing.value = false
  }
}

async function handleRollback(v: ArticleVersionDetailVO) {
  try { await confirm(`回滚到版本 ${v.version}？将基于该版本创建新草稿。`, '回滚版本', { confirmText: '确认回滚' }) }
  catch { return }
  try {
    await rollbackVersion(currentId.value!, v.id)
    exitDiff()
    await loadDraft()
    toast.success(`已回滚至版本 ${v.version}`)
  } catch (err) { handleError(err, '回滚失败') }
}

function shortTime(d: string) {
  return new Date(d)
    .toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
    .replace(/\//g, '-')
}
</script>

<template>
  <div class="write-view">

    <!-- ── Toolbar ── -->
    <div class="write-toolbar">
      <div class="tb-left">
        <button class="back-btn" @click="router.push('/admin/articles')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 5l-7 7 7 7"/></svg>
          <span>文章列表</span>
        </button>
        <div class="tb-sep" />
        <span class="status-pill" :class="isPublished ? 'status-pill--published' : 'status-pill--draft'">
          {{ isPublished ? '已发布' : '草稿' }}
        </span>
      </div>

      <div class="tb-right">
        <span v-if="saveStateText" class="save-hint">{{ saveStateText }}</span>
        <button v-if="isEdit" class="tb-btn" @click="openPreview">预览</button>
        <button class="tb-btn" :disabled="saving" @click="handleSave">
          <svg v-if="saving" class="btn-spinner-icon" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" stroke-dasharray="40" stroke-dashoffset="15"/>
          </svg>
          {{ saving ? '保存中…' : '保存' }}
        </button>
        <button class="tb-btn tb-btn--primary" @click="openPublishDialog">发布</button>
        <button
          v-if="isEdit"
          class="sidebar-toggle"
          :title="sidebarOpen ? '收起侧栏' : '展开侧栏'"
          @click="sidebarOpen = !sidebarOpen"
        >
          <svg v-if="sidebarOpen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <rect x="3" y="3" width="18" height="18" rx="2"/><path d="M15 3v18"/>
            <path d="M19 9l-3 3 3 3"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <rect x="3" y="3" width="18" height="18" rx="2"/><path d="M15 3v18"/>
            <path d="M11 9l3 3-3 3"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- ── Body ── -->
    <div class="write-body">

      <!-- Editor area -->
      <div class="editor-area">
        <div v-if="pageLoading" class="editor-loading">
          <svg class="spinner" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" stroke-dasharray="40" stroke-dashoffset="15"/>
          </svg>
        </div>

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

        <!-- Diff banner -->
        <Transition name="diff-banner">
          <div v-if="diffMode && diffVersion" class="diff-banner">
            <div class="diff-banner-left">
              <span class="diff-badge">对比模式</span>
              <span class="diff-desc">版本 {{ diffVersion.version }}（{{ shortTime(diffVersion.createTime) }}）↔ 当前版本</span>
            </div>
            <div class="diff-banner-right">
              <button class="tb-btn" @click="exitDiff">退出对比</button>
              <button class="tb-btn tb-btn--primary" @click="handleRollback(diffVersion)">回滚到此版本</button>
            </div>
          </div>
        </Transition>

        <!-- Editor -->
        <div v-show="!diffMode" class="md-wrap" ref="editorContainer" />

        <!-- Diff view -->
        <div v-if="diffMode" class="diff-view">
          <div v-if="hasTitleDiff" class="diff-section">
            <div class="diff-section-label">标题变更</div>
            <div class="diff-title-row diff-title-row--removed">
              <span class="diff-marker">−</span>
              <span class="diff-title-text">{{ diffVersion!.title }}</span>
            </div>
            <div class="diff-title-row diff-title-row--added">
              <span class="diff-marker">+</span>
              <span class="diff-title-text">
                <span v-for="(part, i) in titleDiff" :key="i" :class="{ 'diff-word--added': part.added, 'diff-word--removed': part.removed }">{{ part.value }}</span>
              </span>
            </div>
          </div>
          <div class="diff-section diff-section--content">
            <div class="diff-section-label">正文变更</div>
            <div class="diff-lines">
              <div v-for="(line, i) in renderedContentLines" :key="i" class="diff-line"
                :class="{ 'diff-line--added': line.added, 'diff-line--removed': line.removed, 'diff-line--unchanged': !line.added && !line.removed }">
                <span class="diff-marker">{{ line.added ? '+' : line.removed ? '−' : ' ' }}</span>
                <span class="diff-line-text">{{ line.text || ' ' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Sidebar -->
      <div v-if="isEdit" class="meta-sidebar" :class="{ 'meta-sidebar--closed': !sidebarOpen }">
        <div class="sidebar-scroll">
          <div class="sidebar-section">
            <div class="version-header-row">
              <span class="section-label">历史版本</span>
              <button
                class="version-manage-toggle"
                :class="{ 'version-manage-toggle--cancel': versionManageMode }"
                @click="versionManageMode ? exitVersionManage() : (versionManageMode = true)"
              >{{ versionManageMode ? '取消' : '管理' }}</button>
            </div>

            <div class="version-list">
              <div v-if="versionManageMode" class="version-select-all">
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    class="version-checkbox-input"
                    :checked="allDeletableSelected"
                    :indeterminate="selectedVersionIds.length > 0 && !allDeletableSelected"
                    :disabled="deletableVersionIds.length === 0"
                    @change="toggleSelectAll"
                  />
                  全选可删除
                </label>
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
                <input
                  v-if="versionManageMode"
                  type="checkbox"
                  class="version-checkbox-input"
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
                  <button
                    v-else
                    class="compare-btn"
                    :class="{ 'compare-btn--active': diffMode && diffVersion?.id === v.id }"
                    :disabled="diffLoading"
                    @click="enterDiff(v)"
                  >{{ diffMode && diffVersion?.id === v.id ? '对比中' : '对比' }}</button>
                </template>
              </div>

              <div v-if="versionManageMode" class="version-manage-footer">
                <span class="version-manage-count">已选 {{ selectedVersionIds.length }} / {{ deletableVersionIds.length }}</span>
                <button class="version-delete-btn" :disabled="selectedVersionIds.length === 0 || deletingVersions" @click="handleDeleteVersions">
                  {{ deletingVersions ? '删除中…' : '删除' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Publish Dialog ── -->
    <Teleport to="body">
      <div v-if="publishDialogVisible" class="dialog-mask" @click.self="publishDialogVisible = false">
        <div class="publish-dialog">
          <div class="pd-header">
            <span class="pd-title">{{ isPublished ? '更新发布信息' : '发布文章' }}</span>
            <button class="pd-close" @click="publishDialogVisible = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12"/></svg>
            </button>
          </div>

          <div class="publish-form">

            <div class="pf-ai-bar">
              <button class="ai-gen-btn ai-gen-btn--full" :class="{ 'ai-gen-btn--loading': aiGenerating }" :disabled="aiGenerating" type="button" @click="runAiRecommend">
                <span class="ai-gen-btn__icon">✦</span>
                <span>{{ aiGenerating ? 'AI 分析中…' : 'AI 一键推荐' }}</span>
              </button>
            </div>

            <!-- Summary -->
            <div class="pf-item">
              <div class="pf-label">摘要 <span class="pf-optional">可选</span></div>
              <div class="textarea-wrap">
                <textarea v-model="publishForm.summary" class="pf-textarea" rows="3" placeholder="留空则自动截取正文前 200 字..." maxlength="512" />
                <span class="textarea-count">{{ publishForm.summary.length }} / 512</span>
              </div>
              <Transition name="ai-preview">
                <div v-if="aiSummaryResult" class="ai-preview">
                  <p class="ai-preview__text">{{ aiSummaryResult }}</p>
                  <div class="ai-preview__actions">
                    <button type="button" class="ai-action ai-action--dismiss" @click="aiSummaryResult = null">丢弃</button>
                    <button type="button" class="ai-action ai-action--primary" @click="acceptAiSummary">写入摘要</button>
                  </div>
                </div>
              </Transition>
            </div>

            <!-- Category combobox -->
            <div class="pf-item">
              <div class="pf-label">分类 <span class="pf-required">必填</span></div>
              <div class="combobox" v-click-outside="closeCatDrop">
                <div class="combobox-trigger" :class="{ 'combobox-trigger--active': catDropOpen }" @click="catDropOpen ? closeCatDrop() : openCatDrop()">
                  <span v-if="selectedCategoryName && !catDropOpen" class="combobox-value">{{ selectedCategoryName }}</span>
                  <input
                    v-show="catDropOpen || !selectedCategoryName"
                    ref="catInputRef"
                    v-model="catSearch"
                    class="combobox-input"
                    :placeholder="selectedCategoryName || '搜索或输入分类名，按 Enter 创建'"
                    @keydown.enter.prevent="canCreateCat ? createCategory() : (filteredCats[0] && selectCategory(filteredCats[0].id, filteredCats[0].name))"
                    @keydown.escape="closeCatDrop"
                  />
                  <button v-if="publishForm.categoryId && !catDropOpen" type="button" class="combobox-clear" @click.stop="clearCategory()">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12"/></svg>
                  </button>
                  <svg v-else class="combobox-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9l6 6 6-6"/></svg>
                </div>
                <div v-if="catDropOpen" class="combobox-dropdown">
                  <button v-for="cat in filteredCats" :key="cat.id" type="button" class="combobox-option" @click="selectCategory(cat.id, cat.name)">
                    {{ cat.name }}
                  </button>
                  <button v-if="canCreateCat" type="button" class="combobox-option combobox-option--create" @click="createCategory()">
                    创建分类「{{ catSearch.trim() }}」
                  </button>
                  <div v-if="filteredCats.length === 0 && !canCreateCat" class="combobox-empty">无匹配分类</div>
                </div>
              </div>
              <Transition name="ai-preview">
                <div v-if="aiCatResult !== null" class="ai-preview">
                  <template v-if="aiCatResult">
                    <div class="ai-preview__meta"><span class="ai-meta-chip ai-meta-chip--existing">{{ aiCatResult.name }}</span></div>
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
              </Transition>
            </div>

            <!-- Tags multi-select -->
            <div class="pf-item">
              <div class="pf-label">标签 <span class="pf-optional">可选</span></div>
              <div class="tag-select" v-click-outside="closeTagDrop">
                <div class="tag-select-box" @click="openTagDrop">
                  <span v-for="id in publishForm.tagIds" :key="id" class="tag-chip">
                    {{ tagOptions.find(t => t.id === id)?.name ?? id }}
                    <button type="button" class="tag-chip-remove" @click.stop="removeTag(id)">×</button>
                  </span>
                  <span v-for="name in pendingTagNames" :key="name" class="tag-chip tag-chip--new">
                    {{ name }}
                    <button type="button" class="tag-chip-remove" @click.stop="removePendingTag(name)">×</button>
                  </span>
                  <input
                    ref="tagInputRef"
                    v-model="tagSearch"
                    class="tag-input"
                    :placeholder="(publishForm.tagIds.length + pendingTagNames.length) === 0 ? '搜索或输入标签，按 Enter 添加' : ''"
                    @keydown.enter.prevent="canCreateTag ? addNewTagName() : (filteredTagOptions[0] && addExistingTag(filteredTagOptions[0].id))"
                    @keydown.escape="closeTagDrop"
                    @focus="tagDropOpen = true"
                  />
                </div>
                <div v-if="tagDropOpen" class="combobox-dropdown">
                  <button v-for="tag in filteredTagOptions" :key="tag.id" type="button" class="combobox-option" @click="addExistingTag(tag.id)">
                    {{ tag.name }}
                  </button>
                  <button v-if="canCreateTag" type="button" class="combobox-option combobox-option--create" @click="addNewTagName()">
                    添加标签「{{ tagSearch.trim() }}」
                  </button>
                  <div v-if="filteredTagOptions.length === 0 && !canCreateTag" class="combobox-empty">无匹配标签</div>
                </div>
              </div>
              <Transition name="ai-preview">
                <div v-if="aiTagsResult" class="ai-preview">
                  <div class="ai-preview__meta">
                    <button v-for="tag in aiTagsResult.existing" :key="tag.id" type="button"
                      :class="['ai-meta-chip ai-meta-chip--existing ai-meta-chip--action', { 'ai-meta-chip--applied': publishForm.tagIds.includes(tag.id) }]"
                      :disabled="publishForm.tagIds.includes(tag.id)"
                      @click="applyAiExistingTag(tag)">
                      {{ tag.name }}<span class="ai-meta-chip__plus">+</span>
                    </button>
                    <button v-for="name in aiTagsResult.suggested" :key="name" type="button"
                      :class="['ai-meta-chip ai-meta-chip--new ai-meta-chip--action', { 'ai-meta-chip--applied': pendingTagNames.includes(name) }]"
                      :disabled="pendingTagNames.includes(name)"
                      @click="applyAiSuggestedTag(name)">
                      {{ name }}<span class="ai-meta-chip__badge">新</span><span class="ai-meta-chip__plus">+</span>
                    </button>
                  </div>
                  <div class="ai-preview__actions">
                    <button type="button" class="ai-action ai-action--dismiss" @click="aiTagsResult = null">丢弃</button>
                  </div>
                </div>
              </Transition>
            </div>

          </div>

          <div class="pd-footer">
            <button class="tb-btn" @click="publishDialogVisible = false">取消</button>
            <button class="tb-btn tb-btn--primary" :disabled="publishing || creatingMeta" @click="handlePublishConfirm">
              <svg v-if="publishing || creatingMeta" class="btn-spinner-icon" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" stroke-dasharray="40" stroke-dashoffset="15"/>
              </svg>
              {{ isPublished ? '更新' : '立即发布' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<style scoped>
/* ── Layout ── */

.write-view {
  margin: -24px;
  height: calc(100vh - var(--admin-header-height));
  display: flex; flex-direction: column;
  background: #fff; overflow: hidden;
}

/* ── Toolbar ── */

.write-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  height: 52px; padding: 0 16px 0 20px;
  border-bottom: 1px solid #f0f0f0; flex-shrink: 0; gap: 12px; z-index: 10;
}

.tb-left  { display: flex; align-items: center; gap: 12px; min-width: 0; }
.tb-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

.back-btn {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 13px; color: #6b7280; background: transparent; border: none;
  cursor: pointer; padding: 5px 8px; border-radius: 4px; white-space: nowrap;
  transition: color 0.15s, background 0.15s;
}
.back-btn svg  { width: 14px; height: 14px; }
.back-btn:hover { color: #111827; background: #f3f4f6; }

.tb-sep { width: 1px; height: 18px; background: #e5e7eb; flex-shrink: 0; }

.status-pill { font-size: 12px; font-weight: 500; padding: 3px 8px; border-radius: 4px; white-space: nowrap; }
.status-pill--published { background: #f0fdf4; color: #16a34a; }
.status-pill--draft     { background: #f9fafb; color: #6b7280; border: 1px solid #e5e7eb; }

.save-hint { font-size: 12px; color: #9ca3af; }

.tb-btn {
  height: 30px; padding: 0 12px;
  display: inline-flex; align-items: center; gap: 5px;
  border: 1px solid #e5e7eb; border-radius: 4px; background: #fff;
  font-size: 13px; font-family: inherit; color: #374151;
  cursor: pointer; transition: background 0.15s; white-space: nowrap;
}
.tb-btn:hover:not(:disabled) { background: #f9fafb; }
.tb-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.tb-btn--primary { background: #b85c38; border-color: #b85c38; color: #fff; }
.tb-btn--primary:hover:not(:disabled) { background: #924530; border-color: #924530; }

.btn-spinner-icon { width: 13px; height: 13px; animation: spin 0.9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.sidebar-toggle {
  display: flex; align-items: center; justify-content: center;
  width: 30px; height: 30px;
  border: 1px solid #e5e7eb; border-radius: 4px; background: transparent;
  color: #6b7280; cursor: pointer; transition: border-color 0.15s, background 0.15s;
  flex-shrink: 0;
}
.sidebar-toggle svg   { width: 15px; height: 15px; }
.sidebar-toggle:hover { border-color: #c7d2fe; background: #f5f3ff; color: #4338ca; }

/* ── Body ── */

.write-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }

/* ── Editor area ── */

.editor-area {
  flex: 1; display: flex; flex-direction: column;
  min-width: 0; min-height: 0; overflow: hidden; position: relative;
}

.editor-loading {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255, 255, 255, 0.8); z-index: 5;
}

.spinner { width: 28px; height: 28px; color: #b0a89e; animation: spin 0.9s linear infinite; }

.title-section { padding: 24px 48px 14px; flex-shrink: 0; }

.title-input {
  display: block; width: 100%;
  font-size: 28px; font-weight: 700; color: #111827;
  border: none; outline: none; background: transparent;
  line-height: 1.35; padding: 0; font-family: inherit;
}
.title-input::placeholder { color: #d1d5db; }
.title-input:disabled { opacity: 1; cursor: default; }

/* ── Diff banner ── */

.diff-banner {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 10px 48px;
  background: #fffbeb; border-top: 1px solid #fde68a; border-bottom: 1px solid #fde68a;
  flex-shrink: 0;
}
.diff-banner-enter-active, .diff-banner-leave-active { transition: opacity 0.2s, transform 0.2s; }
.diff-banner-enter-from, .diff-banner-leave-to { opacity: 0; transform: translateY(-6px); }

.diff-banner-left  { display: flex; align-items: center; gap: 10px; min-width: 0; }
.diff-banner-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

.diff-badge { font-size: 11px; font-weight: 600; color: #92400e; background: #fde68a; padding: 2px 8px; border-radius: 4px; white-space: nowrap; flex-shrink: 0; }
.diff-desc  { font-size: 13px; color: #78350f; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* ── Crepe editor ── */

.md-wrap {
  flex: 1; min-height: 0; overflow: hidden;
}

/* Crepe uses its own internal DOM structure; style via :deep if needed */
.md-wrap :deep(.crepe) {
  height: 100%;
}

/* ── Diff view ── */

.diff-view {
  flex: 1; overflow-y: auto; padding: 28px 48px 60px;
  display: flex; flex-direction: column; gap: 24px; background: #fff;
}
.diff-section { display: flex; flex-direction: column; gap: 0; }
.diff-section-label { font-size: 11px; font-weight: 600; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.6px; margin-bottom: 10px; }
.diff-title-row { display: flex; align-items: baseline; gap: 10px; padding: 8px 12px; border-radius: 6px; margin-bottom: 4px; font-size: 22px; font-weight: 700; line-height: 1.35; }
.diff-title-row--removed { background: #fff5f5; }
.diff-title-row--added   { background: #f0fdf4; }
.diff-title-text { flex: 1; }
.diff-word--added   { background: #bbf7d0; border-radius: 2px; }
.diff-word--removed { background: #fecdd3; text-decoration: line-through; border-radius: 2px; }
.diff-section--content { flex: 1; }
.diff-lines { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 14px; line-height: 1.7; border: 1px solid #f0f0f0; border-radius: 8px; overflow: hidden; }
.diff-line { display: flex; align-items: baseline; padding: 1px 0; min-height: 24px; }
.diff-line--added     { background: #f0fdf4; }
.diff-line--removed   { background: #fff5f5; }
.diff-line--unchanged { background: #fff; }
.diff-marker { flex-shrink: 0; width: 36px; text-align: center; font-size: 13px; font-weight: 600; user-select: none; }
.diff-line--added   .diff-marker { color: #16a34a; }
.diff-line--removed .diff-marker { color: #dc2626; }
.diff-line--unchanged .diff-marker { color: #d1d5db; }
.diff-line-text { flex: 1; padding: 0 16px 0 0; white-space: pre-wrap; word-break: break-all; color: #374151; }
.diff-line--added   .diff-line-text { color: #166534; }
.diff-line--removed .diff-line-text { color: #991b1b; }

/* ── Sidebar ── */

.meta-sidebar {
  width: 260px; flex-shrink: 0;
  border-left: 1px solid #f0f0f0; background: #fafafa;
  transition: width 0.25s ease, opacity 0.2s ease;
  overflow: hidden;
}
.meta-sidebar--closed { width: 0; opacity: 0; }

.sidebar-scroll { width: 100%; height: 100%; overflow-y: auto; }
.sidebar-section { padding: 16px 20px; border-bottom: 1px solid #f0f0f0; }

.section-label { font-size: 11px; font-weight: 600; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.6px; }

.version-header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }

.version-manage-toggle { font-size: 11px; font-weight: 500; color: #6366f1; background: transparent; border: none; cursor: pointer; padding: 2px 4px; border-radius: 3px; transition: color 0.15s; }
.version-manage-toggle:hover { color: #4338ca; }
.version-manage-toggle--cancel { color: #9ca3af; }
.version-manage-toggle--cancel:hover { color: #6b7280; }

.version-list { display: flex; flex-direction: column; }

.version-select-all { padding: 4px 0 10px; border-bottom: 1px solid #f0f0f0; margin-bottom: 2px; }

.checkbox-label { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #6b7280; cursor: pointer; }
.version-checkbox-input { cursor: pointer; accent-color: #b85c38; }

.version-item { display: flex; align-items: center; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f3f4f6; gap: 8px; border-radius: 4px; transition: background 0.15s; }
.version-item:last-child { border-bottom: none; padding-bottom: 0; }
.version-item--active { background: #f5f3ff; margin: 0 -4px; padding-left: 4px; padding-right: 4px; }
.version-item--manage { gap: 10px; }
.version-item--undeletable { opacity: 0.45; }

.version-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.version-label-row { display: flex; align-items: center; gap: 5px; }
.version-label { font-size: 13px; font-weight: 500; color: #374151; }
.version-time  { font-size: 11px; color: #9ca3af; }

.current-dot { width: 8px; height: 8px; border-radius: 50%; background: #16a34a; flex-shrink: 0; }

.compare-btn { flex-shrink: 0; font-size: 12px; color: #4338ca; background: transparent; border: 1px solid #c7d2fe; border-radius: 4px; padding: 3px 10px; cursor: pointer; transition: background 0.1s, border-color 0.1s; }
.compare-btn:hover:not(:disabled)       { background: #ede9fe; border-color: #a5b4fc; }
.compare-btn--active     { background: #4338ca; border-color: #4338ca; color: #fff; }
.compare-btn--active:hover { background: #3730a3; border-color: #3730a3; }
.compare-btn:disabled    { opacity: 0.5; cursor: not-allowed; }

.version-tag { font-size: 10px; font-weight: 500; line-height: 1; padding: 2px 5px; border-radius: 3px; }
.version-tag--current   { color: #166534; background: #dcfce7; }
.version-tag--published { color: #1e40af; background: #dbeafe; }

.version-manage-footer { display: flex; align-items: center; justify-content: space-between; padding: 10px 0 2px; border-top: 1px solid #f0f0f0; margin-top: 6px; }
.version-manage-count { font-size: 12px; color: #9ca3af; }
.version-delete-btn { font-size: 12px; font-weight: 500; color: #fff; background: #ef4444; border: none; border-radius: 4px; padding: 4px 14px; cursor: pointer; transition: background 0.15s; }
.version-delete-btn:hover:not(:disabled) { background: #dc2626; }
.version-delete-btn:disabled { opacity: 0.45; cursor: not-allowed; }

/* ── Publish dialog ── */

.dialog-mask {
  position: fixed; inset: 0; background: rgba(0, 0, 0, 0.4);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}

.publish-dialog {
  width: min(540px, calc(100vw - 32px));
  background: #fff;
  border: 1px solid #e8e4de;
  border-radius: 4px; overflow: hidden;
  max-height: calc(100vh - 48px); display: flex; flex-direction: column;
}

.pd-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid #f0f0f0; flex-shrink: 0;
}
.pd-title { font-size: 15px; font-weight: 600; color: #1a1610; font-family: var(--font-serif, 'Lora', serif); }
.pd-close { display: flex; align-items: center; justify-content: center; width: 28px; height: 28px; border: none; border-radius: 4px; background: transparent; color: #9ca3af; cursor: pointer; transition: background 0.15s; }
.pd-close svg { width: 14px; height: 14px; }
.pd-close:hover { background: #f3f4f6; }

.publish-form { padding: 20px; display: flex; flex-direction: column; gap: 20px; overflow-y: auto; }

.pf-ai-bar { display: flex; justify-content: flex-end; margin-bottom: -4px; }
.pf-item   { display: flex; flex-direction: column; gap: 8px; }
.pf-label  { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 500; color: #374151; }
.pf-optional { font-size: 11px; font-weight: 400; color: #9ca3af; background: #f3f4f6; padding: 1px 6px; border-radius: 3px; }
.pf-required { font-size: 11px; font-weight: 500; color: #dc2626; background: #fef2f2; padding: 1px 6px; border-radius: 3px; }

.textarea-wrap { position: relative; }
.pf-textarea {
  width: 100%; box-sizing: border-box;
  padding: 8px 10px; padding-bottom: 22px;
  border: 1px solid #e8e4de; border-radius: 4px; background: #fff;
  font-size: 13px; font-family: inherit; color: #1a1610; outline: none;
  transition: border-color 0.15s; resize: none;
}
.pf-textarea:focus { border-color: #b85c38; }
.pf-textarea::placeholder { color: #b0a89e; }
.textarea-count { position: absolute; bottom: 6px; right: 10px; font-size: 11px; color: #b0a89e; }

.pd-footer {
  display: flex; justify-content: flex-end; gap: 8px;
  padding: 14px 20px; border-top: 1px solid #f0f0f0; flex-shrink: 0;
}

/* ── Combobox ── */

.combobox { position: relative; }

.combobox-trigger {
  min-height: 34px; padding: 0 30px 0 10px;
  display: flex; align-items: center; flex-wrap: wrap; gap: 4px;
  border: 1px solid #e8e4de; border-radius: 4px; background: #fff;
  cursor: text; transition: border-color 0.15s; position: relative;
}
.combobox-trigger--active { border-color: #b85c38; }

.combobox-value { font-size: 13px; color: #1a1610; }
.combobox-input { flex: 1; min-width: 80px; border: none; outline: none; font-size: 13px; font-family: inherit; color: #1a1610; background: transparent; }
.combobox-input::placeholder { color: #b0a89e; }

.combobox-clear, .combobox-chevron {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  display: flex; align-items: center; background: none; border: none;
  color: #b0a89e; cursor: pointer; padding: 0;
}
.combobox-clear svg    { width: 12px; height: 12px; }
.combobox-chevron      { pointer-events: none; }
.combobox-chevron      { width: 14px; height: 14px; }

.combobox-dropdown {
  position: absolute; top: calc(100% + 4px); left: 0; right: 0;
  background: #fff; border: 1px solid #e8e4de; border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); z-index: 200;
  max-height: 200px; overflow-y: auto;
}

.combobox-option {
  display: block; width: 100%; padding: 8px 12px; text-align: left;
  font-size: 13px; font-family: inherit; color: #1a1610;
  background: none; border: none; cursor: pointer; transition: background 0.1s;
}
.combobox-option:hover { background: #ece9e4; }
.combobox-option--create { color: #b85c38; font-style: italic; border-top: 1px solid #f0f0f0; }

.combobox-empty { padding: 10px 12px; font-size: 13px; color: #b0a89e; text-align: center; }

/* ── Tag multi-select ── */

.tag-select { position: relative; }

.tag-select-box {
  min-height: 34px; padding: 4px 8px;
  display: flex; flex-wrap: wrap; align-items: center; gap: 4px;
  border: 1px solid #e8e4de; border-radius: 4px; background: #fff;
  cursor: text; transition: border-color 0.15s;
}
.tag-select-box:focus-within { border-color: #b85c38; }

.tag-chip {
  display: inline-flex; align-items: center; gap: 3px;
  padding: 2px 6px; border-radius: 3px;
  font-size: 12px; font-weight: 500;
  background: rgba(184, 92, 56, 0.1); color: #b85c38;
}
.tag-chip--new { background: #f3f4f6; color: #6b7280; font-style: italic; }

.tag-chip-remove { font-size: 13px; line-height: 1; background: none; border: none; cursor: pointer; color: inherit; opacity: 0.7; padding: 0; }
.tag-chip-remove:hover { opacity: 1; }

.tag-input { flex: 1; min-width: 100px; border: none; outline: none; font-size: 13px; font-family: inherit; color: #1a1610; background: transparent; }
.tag-input::placeholder { color: #b0a89e; }

/* ── AI generate button ── */

.ai-gen-btn {
  margin-left: auto; display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 10px; font-size: 12px; font-weight: 500;
  color: #7c3aed; background: #f5f3ff; border: 1px solid #ddd6fe;
  border-radius: 4px; cursor: pointer; transition: background 0.15s, opacity 0.15s; line-height: 20px;
}
.ai-gen-btn--full { padding: 4px 14px; font-size: 13px; }
.ai-gen-btn:hover:not(:disabled) { background: #ede9fe; }
.ai-gen-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.ai-gen-btn__icon { font-size: 11px; }
.ai-gen-btn--loading .ai-gen-btn__icon { display: inline-block; animation: ai-spin 1.2s linear infinite; }
@keyframes ai-spin { to { transform: rotate(360deg); } }

/* ── AI preview ── */

.ai-preview { border: 1px solid #ddd6fe; border-radius: 6px; background: #faf9ff; overflow: hidden; }
.ai-preview__no-match { margin: 0; padding: 12px 16px 8px; font-size: 12px; color: #9ca3af; font-style: italic; }
.ai-preview__text { margin: 0; padding: 14px 16px 10px; font-size: 13px; line-height: 1.7; color: #374151; }
.ai-preview__actions { display: flex; justify-content: flex-end; gap: 8px; padding: 0 12px 12px; }
.ai-preview__meta { display: flex; flex-wrap: wrap; gap: 6px; padding: 12px 16px 10px; }

.ai-action { padding: 4px 12px; font-size: 12px; font-weight: 500; border-radius: 4px; border: 1px solid transparent; cursor: pointer; transition: background 0.15s; }
.ai-action--dismiss { color: #6b7280; background: #f3f4f6; border-color: #e5e7eb; }
.ai-action--dismiss:hover { background: #e5e7eb; }
.ai-action--primary { color: #fff; background: #7c3aed; border-color: #7c3aed; }
.ai-action--primary:hover { background: #6d28d9; }

.ai-meta-chip { display: inline-flex; align-items: center; gap: 4px; padding: 3px 10px; font-size: 12px; font-weight: 500; border-radius: 4px; border: 1px solid transparent; line-height: 20px; }
.ai-meta-chip--existing { color: #7c3aed; background: #ede9fe; border-color: #ddd6fe; }
.ai-meta-chip--new      { color: #6b7280; background: #f9fafb; border-color: #d1d5db; border-style: dashed; }
.ai-meta-chip--action   { cursor: pointer; transition: background 0.15s; }
.ai-meta-chip--action.ai-meta-chip--existing:hover { background: #ddd6fe; }
.ai-meta-chip--action.ai-meta-chip--new:hover      { background: #f3f4f6; }
.ai-meta-chip__badge { font-size: 10px; font-weight: 600; color: #9ca3af; background: #e5e7eb; padding: 0 4px; border-radius: 3px; }
.ai-meta-chip__plus  { font-size: 14px; font-weight: 400; line-height: 1; color: #a78bfa; margin-left: 1px; }
.ai-meta-chip--new .ai-meta-chip__plus { color: #9ca3af; }
.ai-meta-chip--applied { opacity: 0.4; cursor: not-allowed; }

.ai-preview-enter-active, .ai-preview-leave-active { transition: opacity 0.2s, transform 0.2s; }
.ai-preview-enter-from, .ai-preview-leave-to { opacity: 0; transform: translateY(-4px); }

/* ── Mobile ── */

@media (max-width: 768px) {
  .write-view    { margin: -16px -12px; }
  .write-toolbar { padding: 0 12px; gap: 8px; }
  .save-hint     { display: none; }
  .title-section { padding: 24px 20px 14px; }
  .title-input   { font-size: 22px; }
  .diff-banner   { padding: 10px 20px; }
  .diff-view     { padding: 20px 20px 40px; }
  .meta-sidebar  { position: fixed; top: var(--admin-header-height); right: 0; height: calc(100vh - var(--admin-header-height)); z-index: 100; box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1); }
  .meta-sidebar--closed { width: 0; opacity: 0; box-shadow: none; }
}
</style>