<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import { useRowMenu } from '@/admin/composables/useRowMenu'
import { VditorWriter } from '@bitlog/editor'
import { uploadFile } from '@/api/file'
import {
  createArticle,
  updateArticleDraft,
  getArticleDraft,
  publishArticle,
  updateArticleMeta,
  discardDraftAbovePublish,
  updateArticlesStatus,
  deleteArticles,
  getArticleVersions,
  generateAiMetadata,
  type ArticleDetailVO,
  type ArticleVersionVO,
} from '@/api/admin/article'
import { ApiError } from '@/utils/request'
import AdminIcon from '@/admin/components/AdminIcon.vue'
import ArticleMetaDialog from '@/admin/components/ArticleMetaDialog.vue'
import VersionSidebar from '@/admin/components/VersionSidebar.vue'
import {
  useLocalDraft,
  formatRelative,
  type LocalDraftPayload,
} from '@/admin/composables/useLocalDraft'
import { writePreviewHandoff } from '@/admin/composables/usePreviewHandoff'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const confirm = useConfirm()

const articleId = ref<number | null>(route.params.id ? Number(route.params.id) : null)

// ── Editor buffer ──────────────────────────────────────────────────────────────
const title = ref('')
const content = ref('') // 仅用于编辑器挂载前播种，挂载后一律走 setMarkdown
const liveContent = ref('') // 编辑器当前 markdown 的镜像，供按钮启用条件读取
const loading = ref(true)
const titleRef = ref<HTMLTextAreaElement | null>(null)
const vditorRef = ref<InstanceType<typeof VditorWriter> | null>(null)

// ── 已保存快照：脏判定与「放弃修改」的基准 ──────────────────────────────────────
const savedTitle = ref('')
const savedContent = ref('')

// ── 轴 A：生命周期，全部由服务器字段推导 ────────────────────────────────────────
const latestVersionId = ref<number | null>(null)
const publishedVersionId = ref<number | null>(null)
const publishTime = ref<string | null>(null)

type Lifecycle = 'new' | 'draft' | 'unpublished' | 'live' | 'liveAhead'

const lifecycle = computed<Lifecycle>(() => {
  if (articleId.value === null) return 'new'
  // 撤下只清 publishedVersionId，publishTime 留着，两者合起来才能分出「从未发布」和「已撤下」
  if (publishedVersionId.value === null) return publishTime.value ? 'unpublished' : 'draft'
  return publishedVersionId.value === latestVersionId.value ? 'live' : 'liveAhead'
})

const LIFECYCLE_LABEL: Record<Lifecycle, string> = {
  new: '',
  draft: '草稿',
  unpublished: '已撤下',
  live: '已发布',
  liveAhead: '已发布·有草稿',
}

const PILL_CLASS: Record<Lifecycle, string> = {
  new: '',
  draft: 'status-pill--draft',
  unpublished: 'status-pill--unpublished',
  live: 'status-pill--published',
  liveAhead: 'status-pill--live-ahead',
}

const lifecycleLabel = computed(() => LIFECYCLE_LABEL[lifecycle.value])
const pillClass = computed(() => PILL_CLASS[lifecycle.value])
const isOnline = computed(() => lifecycle.value === 'live' || lifecycle.value === 'liveAhead')

// ── 轴 B：缓冲区 ───────────────────────────────────────────────────────────────
const dirty = ref(false)
const saving = ref(false)
const publishing = ref(false)
const menuBusy = ref(false)
const lastSavedAt = ref<number | null>(null)
const busy = computed(() => saving.value || publishing.value || menuBusy.value)

// 「已保存 3 分钟前」要自己走字
const nowTs = ref(Date.now())
let clockTimer: ReturnType<typeof setInterval> | undefined

const savedHint = computed(() => {
  if (saving.value) return '保存中…'
  if (dirty.value) return '未保存'
  if (lastSavedAt.value === null) return ''
  void nowTs.value
  return `已保存 ${formatRelative(lastSavedAt.value)}`
})

const hasTitle = computed(() => title.value.trim().length > 0)
const hasBody = computed(() => liveContent.value.trim().length > 0)
/** 后端要求标题和正文都非空，所以这是能落库的最低条件 */
const canPersist = computed(() => hasTitle.value && hasBody.value)
const canSaveDraft = computed(() => canPersist.value && dirty.value && !busy.value)
/** 主按钮只看内容是否够发，从不因「还没保存」而禁用 */
const canPrimary = computed(() => canPersist.value && !busy.value)
const canPreview = computed(() => hasTitle.value || hasBody.value)

/**
 * 主按钮永远叫「发布」，不随状态变形。它的语义始终是同一件事 —— 把当前内容发到线上；
 * 是否已在线上由胶囊表达，按钮不必重复。（「更新」没说清更新的对象，跟「保存」糊在一起。）
 */
const primaryLabel = computed(() => (publishing.value ? '处理中…' : '发布'))

// ── Sidebar & versions ─────────────────────────────────────────────────────────
const sidebarOpen = ref(false)
const versions = ref<ArticleVersionVO[]>([])

// ── 更多菜单：复用表格行菜单的开合与定位，这里只有一个所以 id 固定 ────────────────
const { openMenuId, menuStyle, toggleMenu, closeMenu } = useRowMenu()
const MENU_ID = 0
const menuOpen = computed(() => openMenuId.value === MENU_ID)

// ── Publish dialog ─────────────────────────────────────────────────────────────
const publishDialogVisible = ref(false)
const metaDialogRef = ref<InstanceType<typeof ArticleMetaDialog> | null>(null)

// Article metadata (seeds the dialog on open)
const draftSummary = ref('')
const draftCategory = ref<{ id: number; name: string } | null>(null)
const draftTags = ref<{ id: number; name: string }[]>([])

// ── AI recommendation ─────────────────────────────────────────────────────────
const aiGenerating = ref(false)
const aiSummaryResult = ref<string | null>(null)
const aiCatResult = ref<{ id: number; name: string } | false | null>(null)
const aiTagsResult = ref<{
  existing: Array<{ id: number; name: string }>
  suggested: string[]
} | null>(null)

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

function currentMarkdown(): string {
  return vditorRef.value?.getMarkdown() ?? liveContent.value
}

/** localStorage 崩溃兜底（见 useLocalDraft）。id 现取：新文章存下后键要跟着换 */
const localDraft = useLocalDraft(
  () => articleId.value ?? 'new',
  () => ({ title: title.value, content: currentMarkdown() }),
)

const localRestore = ref<LocalDraftPayload | null>(null)

/**
 * 脏判定做真值比较，而不是「碰过就算脏」：编辑器的 focus / blur 也会发 change，
 * 且改完又改回原样不该留下一条重复版本。
 */
function recomputeDirty() {
  liveContent.value = currentMarkdown()
  dirty.value = title.value !== savedTitle.value || liveContent.value !== savedContent.value
  if (dirty.value) localDraft.schedule()
}

let recheckTimer: ReturnType<typeof setTimeout> | undefined
function scheduleRecheck() {
  clearTimeout(recheckTimer)
  recheckTimer = setTimeout(recomputeDirty, 250)
}

function markSaved(t: string, c: string) {
  savedTitle.value = t
  savedContent.value = c
  liveContent.value = c
  dirty.value = false
  lastSavedAt.value = Date.now()
  localDraft.clear()
  localRestore.value = null
}

/** 把服务器草稿灌进编辑器。挂载后走 setMarkdown，挂载前只能靠 content prop 播种 */
async function applyDraft(data: ArticleDetailVO) {
  title.value = data.title
  if (vditorRef.value) {
    vditorRef.value.setMarkdown(data.content)
    await nextTick()
    // 灌进去再读回来才是基线：Lute 的 autoSpace / fixTermTypo 会重写 markdown
    savedContent.value = vditorRef.value.getMarkdown()
  } else {
    content.value = data.content // 挂载前只能播种，基线稍后由 ready 事件校准
    savedContent.value = data.content
  }
  savedTitle.value = data.title
  liveContent.value = savedContent.value
  dirty.value = false
  lastSavedAt.value = data.updateTime ? new Date(data.updateTime).getTime() : null
  latestVersionId.value = data.latestVersionId
  publishedVersionId.value = data.publishedVersionId
  publishTime.value = data.publishTime
  draftSummary.value = data.summary ?? ''
  draftCategory.value = data.category ?? null
  draftTags.value = data.tags
}

// ── Load ───────────────────────────────────────────────────────────────────────
async function loadVersions() {
  if (articleId.value === null) return
  try {
    const data = await getArticleVersions(articleId.value)
    versions.value = data
    const latest = data.find((v) => v.latest)
    if (latest) latestVersionId.value = latest.id
  } catch {
    // 侧栏非关键
  }
}

/**
 * 恢复提示的判断要等基线校准（ready）之后：在那之前 savedContent 还是服务器原串，
 * 而本地兜底那份是规范化过的，两者对不上，identity 判断永远不成立 → 提示会误报。
 */
let pendingRestoreCheck: { serverUpdateTime: string | null } | null = null

async function loadArticle() {
  if (articleId.value === null) {
    loading.value = false
    pendingRestoreCheck = { serverUpdateTime: null }
    await nextTick()
    autoResizeTitle()
    suppressChange = false
    return
  }

  loading.value = true
  suppressChange = true
  try {
    const data = await getArticleDraft(articleId.value)
    await applyDraft(data)
    await loadVersions()
    pendingRestoreCheck = { serverUpdateTime: data.updateTime }
  } catch (err) {
    handleError(err, '加载文章失败')
  } finally {
    loading.value = false
    await nextTick()
    autoResizeTitle()
    suppressChange = false
  }
}

/**
 * 本地兜底内容比已保存内容新时，在工具栏下方挂一条内联提示 —— 不弹窗、不阻塞，
 * 在你明确选择前也不删本地副本。
 */
function offerLocalRestore(serverUpdateTime: string | null) {
  const cached = localDraft.read()
  if (!cached || (!cached.title.trim() && !cached.content.trim())) return
  if (cached.title === savedTitle.value && cached.content === savedContent.value) {
    localDraft.clear()
    return
  }
  if (serverUpdateTime && new Date(serverUpdateTime).getTime() > cached.ts) {
    localDraft.clear()
    return
  }
  localRestore.value = cached
}

async function acceptLocalRestore() {
  const cached = localRestore.value
  if (!cached) return
  suppressChange = true
  title.value = cached.title
  vditorRef.value?.setMarkdown(cached.content)
  liveContent.value = cached.content
  dirty.value = true
  localRestore.value = null
  await nextTick()
  autoResizeTitle()
  suppressChange = false
  toast.success('已恢复本地内容')
}

function dismissLocalRestore() {
  localDraft.clear()
  localRestore.value = null
}

// ── 保存：只提交标题和正文，不碰元数据，不动线上指针 ──────────────────────────────
async function saveDraft(): Promise<boolean> {
  if (!canPersist.value) {
    toast.warning(hasTitle.value ? '请先输入文章内容' : '请先输入文章标题')
    return false
  }
  saving.value = true
  try {
    const t = title.value
    const md = currentMarkdown()
    if (articleId.value === null) {
      const newId = await createArticle({ title: t, content: md })
      localDraft.clear() // 先清 "new" 键，articleId 换了之后就找不到它了
      articleId.value = newId
    } else {
      await updateArticleDraft(articleId.value, { title: t, content: md })
    }
    markSaved(t, md)
    await loadVersions()
    return true
  } catch (err) {
    handleError(err, '保存失败')
    return false
  } finally {
    saving.value = false
  }
}

/**
 * 新建保存后把 URL 补上 id。write 与 write/:id 是两条路由记录，这次导航会重挂组件，
 * 所以只在整个操作的最后一步调用 —— 放在中途会把「保存后接着发布」打断成两半。
 */
async function syncRouteToArticle() {
  if (articleId.value === null || route.params.id) return
  // 先让「关闭对话框」这类状态变更渲染落地：Vue 的 DOM 更新是异步的，抢在提交前导航
  // 会在重挂时把对话框的 DOM 留成孤儿，画面上就是一直卡在「保存中…」
  await nextTick()
  await router.replace(`/admin/write/${articleId.value}`)
}

async function handleSaveDraft() {
  if (!(await saveDraft())) return
  toast.success('已保存')
  await syncRouteToArticle()
}

// ── 放弃修改：缓冲区回到上次保存的内容 ──────────────────────────────────────────
async function discardBufferChanges() {
  try {
    await confirm('放弃当前未保存的修改，恢复到上次保存的内容？', '放弃修改', {
      confirmText: '放弃修改',
      danger: true,
    })
  } catch {
    return
  }
  suppressChange = true
  title.value = savedTitle.value
  vditorRef.value?.setMarkdown(savedContent.value)
  await nextTick()
  // 与 applyDraft 同理，回灌后重新取基线，免得规范化差异又把它判成脏
  liveContent.value = currentMarkdown()
  savedContent.value = liveContent.value
  dirty.value = false
  localDraft.clear()
  localRestore.value = null
  autoResizeTitle()
  suppressChange = false
  toast.success('已恢复到上次保存的内容')
}

// ── 预览：始终是编辑器里的当前内容，零服务器写入 ─────────────────────────────────
function openPreview() {
  if (!canPreview.value) return
  const target = articleId.value ?? 'new'
  writePreviewHandoff(target, title.value, currentMarkdown())
  window.open(`/admin/preview/${target}`, '_blank')
}

// ── 主按钮：让线上等于我现在看到的内容 ──────────────────────────────────────────
function openPublishDialog() {
  if (!canPersist.value) {
    toast.warning(hasTitle.value ? '请先输入文章内容' : '请先输入文章标题')
    return
  }
  aiSummaryResult.value = null
  aiCatResult.value = null
  aiTagsResult.value = null
  aiGenerating.value = false
  publishDialogVisible.value = true
}

const PUBLISH_TOAST: Record<Lifecycle | 'meta', string> = {
  meta: '发布信息已更新',
  new: '文章已发布',
  draft: '文章已发布',
  unpublished: '文章已重新发布',
  live: '线上内容已更新',
  liveAhead: '线上内容已更新',
}

async function handlePublishConfirm(data: {
  summary: string
  categoryId: number | null
  tagIds: number[]
  category: { id: number; name: string } | null
  tags: { id: number; name: string }[]
}) {
  if (!data.categoryId) {
    toast.warning('请选择文章分类')
    return
  }

  // 计划必须在保存前定：保存会把 live 推成 liveAhead，事后再判断就晚了
  const before = lifecycle.value
  const needsSave = dirty.value || before === 'new'
  const metaOnly = before === 'live' && !dirty.value

  let done = false
  publishing.value = true
  try {
    if (needsSave && !(await saveDraft())) return

    const id = articleId.value!
    const payload = {
      summary: data.summary || null,
      categoryId: data.categoryId,
      tagIds: data.tagIds,
    }

    if (metaOnly) {
      // 内容一个字没变，能变的只有元数据
      await updateArticleMeta(id, payload)
    } else {
      await publishArticle(id, payload)
      publishedVersionId.value = latestVersionId.value
      if (publishTime.value === null) publishTime.value = new Date().toISOString()
    }

    draftSummary.value = data.summary
    draftCategory.value = data.category
    draftTags.value = data.tags
    publishDialogVisible.value = false
    toast.success(PUBLISH_TOAST[metaOnly ? 'meta' : before])
    done = true
  } catch (err) {
    handleError(err, '发布失败')
  } finally {
    publishing.value = false
  }

  // 补 URL 放在最后：这次导航会重挂组件（write 与 write/:id 是两条路由记录），
  // 留在 try 里的话 publishing 还没归零，重挂会把对话框连着「保存中…」一起带过去
  if (done) await syncRouteToArticle()
}

// ── 更多菜单 ───────────────────────────────────────────────────────────────────
function viewLive() {
  closeMenu()
  window.open(`/article/${articleId.value}`, '_blank')
}

async function handleDiscardDraftAbovePublish() {
  closeMenu()
  try {
    await confirm(
      '放弃已保存但未发布的草稿改动，回到线上那一版？被放弃的版本仍保留在版本历史里。',
      '回到线上版本',
      { confirmText: '确认', danger: true },
    )
  } catch {
    return
  }
  menuBusy.value = true
  suppressChange = true
  try {
    await discardDraftAbovePublish(articleId.value!)
    await applyDraft(await getArticleDraft(articleId.value!))
    await loadVersions()
    toast.success('已回到线上版本')
  } catch (err) {
    handleError(err, '操作失败')
  } finally {
    menuBusy.value = false
    await nextTick()
    autoResizeTitle()
    suppressChange = false
  }
}

async function handleUnpublish() {
  closeMenu()
  try {
    await confirm('取消发布后文章会从站点下线，内容和发布信息都保留。', '取消发布', {
      confirmText: '取消发布',
      danger: true,
    })
  } catch {
    return
  }
  menuBusy.value = true
  try {
    await updateArticlesStatus([articleId.value!], 'DRAFT')
    publishedVersionId.value = null
    toast.success('已取消发布')
  } catch (err) {
    handleError(err, '操作失败')
  } finally {
    menuBusy.value = false
  }
}

async function handleDelete() {
  closeMenu()
  try {
    await confirm(`确认删除「${title.value.trim() || '未命名'}」？`, '删除文章', {
      confirmText: '删除',
      danger: true,
    })
  } catch {
    return
  }
  menuBusy.value = true
  try {
    await deleteArticles([articleId.value!])
    dirty.value = false // 已删除，别再被离开守卫拦下
    localDraft.clear()
    toast.success('文章已删除')
    router.push('/admin/articles')
  } catch (err) {
    handleError(err, '删除失败')
  } finally {
    menuBusy.value = false
  }
}

// ── AI recommendation ─────────────────────────────────────────────────────────
async function runAiRecommend() {
  aiGenerating.value = true
  aiSummaryResult.value = null
  aiCatResult.value = null
  aiTagsResult.value = null
  try {
    const data = await generateAiMetadata(articleId.value!)
    aiSummaryResult.value = data.summary
    aiCatResult.value = data.category ?? false
    aiTagsResult.value = { existing: data.tags, suggested: data.suggestedTags }
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

// 已应用的 AI 建议：existing 按 id 比对，suggested 建标签前无 id，按 name 比对
const appliedTagIds = computed(
  () => new Set(metaDialogRef.value?.selectedTags?.map((t) => t.id) ?? []),
)
const appliedTagNames = computed(
  () => new Set(metaDialogRef.value?.selectedTags?.map((t) => t.name) ?? []),
)

// ── Watchers ───────────────────────────────────────────────────────────────────
watch(title, () => {
  if (suppressChange) return
  scheduleRecheck()
})

async function uploadImageFn(file: File): Promise<string> {
  const { fileUrl } = await uploadFile(file, 'article')
  return fileUrl
}

function onEditorChange() {
  if (suppressChange) return
  scheduleRecheck()
}

/**
 * 初始内容灌完才有真正的基线：Lute 的 autoSpace / fixTermTypo 会重写 markdown，
 * 拿播种的原始字符串当基线会让文章一打开就显示「未保存」。
 */
function onEditorReady() {
  // 初始化期间可能已排上复查和兜底写入，那时基线还不准，一并丢掉
  clearTimeout(recheckTimer)
  localDraft.stop()
  liveContent.value = currentMarkdown()
  savedContent.value = liveContent.value
  dirty.value = false
  if (pendingRestoreCheck) {
    offerLocalRestore(pendingRestoreCheck.serverUpdateTime)
    pendingRestoreCheck = null
  }
}

// ── Guards ─────────────────────────────────────────────────────────────────────
function onBeforeUnload(e: BeforeUnloadEvent) {
  recomputeDirty() // 同步兜底：防抖窗口内关页也要拦得住
  if (!dirty.value) return
  localDraft.write()
  e.preventDefault()
  e.returnValue = true
}

function onKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    if (publishDialogVisible.value) return // 发布对话框里按 Cmd+S 不该在背后偷偷存一版
    recomputeDirty()
    if (canSaveDraft.value) handleSaveDraft()
  }
}

// ── Lifecycle ──────────────────────────────────────────────────────────────────
onMounted(async () => {
  window.addEventListener('beforeunload', onBeforeUnload)
  window.addEventListener('keydown', onKeydown)
  clockTimer = setInterval(() => (nowTs.value = Date.now()), 30_000)
  await loadArticle()
})

onUnmounted(() => {
  clearTimeout(recheckTimer)
  clearInterval(clockTimer)
  localDraft.stop()
  window.removeEventListener('beforeunload', onBeforeUnload)
  window.removeEventListener('keydown', onKeydown)
})

onBeforeRouteLeave(async () => {
  recomputeDirty()
  if (!dirty.value) return true

  // 标题或正文为空时存不进去，就别给「保存并离开」这个假选项
  const savable = canPersist.value
  let result: 'confirm' | 'extra'
  try {
    result = await confirm(
      savable ? '这篇文章有未保存的修改。' : '这篇文章有未保存的修改，但标题或正文为空，存不进去。',
      '离开写作页',
      savable
        ? { confirmText: '保存并离开', extraText: '直接离开', cancelText: '取消' }
        : { confirmText: '直接离开', cancelText: '取消', danger: true },
    )
  } catch {
    return false
  }
  if (!savable || result === 'extra') return true // 直接离开：本地兜底保留
  return await saveDraft()
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
            <svg
              viewBox="0 0 24 24"
              width="16"
              height="16"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            <span>文章列表</span>
          </button>
          <div class="tb-sep" />

          <!-- 轴 A：生命周期 -->
          <span v-if="lifecycleLabel" class="status-pill" :class="pillClass">
            {{ lifecycleLabel }}
          </span>

          <!-- 轴 B：缓冲区 -->
          <span v-if="savedHint" class="save-hint" :class="{ 'save-hint--dirty': dirty }">
            {{ savedHint }}
          </span>
          <button
            v-if="dirty && articleId !== null"
            class="link-btn"
            :disabled="busy"
            @click="discardBufferChanges"
          >
            放弃修改
          </button>
        </div>

        <div class="tb-right">
          <button class="btn btn--default" :disabled="!canPreview" @click="openPreview">
            预览
          </button>
          <button class="btn btn--default" :disabled="!canSaveDraft" @click="handleSaveDraft">
            {{ saving ? '保存中…' : '保存' }}
          </button>
          <button class="btn btn--primary" :disabled="!canPrimary" @click="openPublishDialog">
            {{ primaryLabel }}
          </button>

          <div v-if="articleId !== null" v-click-outside="closeMenu" class="menu-wrap">
            <button class="more-btn" title="更多" @click.stop="toggleMenu(MENU_ID, $event)">
              <AdminIcon name="more" />
            </button>
            <Teleport to="body">
              <div v-if="menuOpen" class="dropdown-menu" :style="menuStyle">
                <button v-if="isOnline" class="menu-item" @click="viewLive">查看线上</button>
                <button
                  v-if="lifecycle === 'liveAhead'"
                  class="menu-item"
                  :disabled="dirty || busy"
                  @click="handleDiscardDraftAbovePublish"
                >
                  回到线上版本
                </button>
                <button v-if="isOnline" class="menu-item" :disabled="busy" @click="handleUnpublish">
                  取消发布
                </button>
                <div v-if="isOnline" class="menu-divider" />
                <button class="menu-item menu-item--danger" :disabled="busy" @click="handleDelete">
                  删除文章
                </button>
              </div>
            </Teleport>
          </div>

          <button
            class="sidebar-toggle"
            :title="sidebarOpen ? '收起侧栏' : '展开侧栏'"
            @click="sidebarOpen = !sidebarOpen"
          >
            <svg
              v-if="sidebarOpen"
              viewBox="0 0 24 24"
              width="16"
              height="16"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M15 18l-6-6 6-6" />
            </svg>
            <svg
              v-else
              viewBox="0 0 24 24"
              width="16"
              height="16"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M9 18l6-6-6-6" />
            </svg>
          </button>
        </div>
      </div>

      <!-- ── 本地兜底内容恢复（内联，不弹窗） ──────────────────────────────── -->
      <div v-if="localRestore" class="restore-bar">
        <span class="restore-text">
          检测到本地未保存内容
          <span class="restore-time">{{ formatRelative(localRestore.ts) }}</span>
        </span>
        <button class="link-btn" @click="acceptLocalRestore">恢复</button>
        <button class="link-btn link-btn--muted" @click="dismissLocalRestore">丢弃</button>
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
              @ready="onEditorReady"
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
      title="发布文章"
      :confirm-text="primaryLabel"
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
          <span
            :style="
              aiGenerating ? 'display:inline-block;animation:ai-spin 1.2s linear infinite' : ''
            "
            >✦</span
          >
        </button>
      </template>

      <template #summary-extra>
        <transition name="ai-slide">
          <div v-if="aiSummaryResult" class="pf-ai-inline">
            <p class="pf-ai-inline-body">{{ aiSummaryResult }}</p>
            <div class="pf-ai-inline-actions">
              <button
                type="button"
                class="ai-action ai-action--dismiss"
                @click="aiSummaryResult = null"
              >
                忽略
              </button>
              <button type="button" class="ai-action ai-action--primary" @click="acceptAiSummary">
                应用
              </button>
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
                <button
                  type="button"
                  class="ai-action ai-action--dismiss"
                  @click="aiCatResult = null"
                >
                  忽略
                </button>
                <button type="button" class="ai-action ai-action--primary" @click="applyAiCategory">
                  应用
                </button>
              </div>
            </template>
            <template v-else>
              <span class="pf-ai-inline-no-match">现有分类均不适配，请手动选择</span>
              <button type="button" class="pf-ai-inline-dismiss" @click="aiCatResult = null">
                <svg
                  viewBox="0 0 24 24"
                  width="12"
                  height="12"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                >
                  <path d="M18 6L6 18M6 6l12 12" />
                </svg>
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
              :class="{ 'ai-meta-chip--applied': appliedTagIds.has(tag.id) }"
              :disabled="appliedTagIds.has(tag.id)"
              @click="applyAiExistingTag(tag)"
            >
              {{ tag.name
              }}<span class="ai-meta-chip__plus">{{ appliedTagIds.has(tag.id) ? '✓' : '+' }}</span>
            </button>
            <button
              v-for="name in aiTagsResult.suggested"
              :key="name"
              type="button"
              class="ai-meta-chip ai-meta-chip--new ai-meta-chip--action"
              :class="{ 'ai-meta-chip--applied': appliedTagNames.has(name) }"
              :disabled="appliedTagNames.has(name)"
              @click="applyAiSuggestedTag(name)"
            >
              {{ name }}<span class="ai-meta-chip__badge">新</span
              ><span class="ai-meta-chip__plus">{{ appliedTagNames.has(name) ? '✓' : '+' }}</span>
            </button>
            <button type="button" class="pf-ai-inline-dismiss" @click="aiTagsResult = null">
              <svg
                viewBox="0 0 24 24"
                width="12"
                height="12"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
              >
                <path d="M18 6L6 18M6 6l12 12" />
              </svg>
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
  0%,
  80%,
  100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
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

.tb-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.tb-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

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
  transition:
    color 0.15s,
    background 0.15s;
}
.back-btn:hover {
  color: var(--admin-text-primary);
  background: var(--admin-surface-soft);
}

.tb-sep {
  width: 1px;
  height: 18px;
  background: var(--admin-border-soft);
  flex-shrink: 0;
}

/* ── 轴 A：生命周期胶囊 ──────────────────────────────────────────────────────── */
.status-pill {
  font-size: 12px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 4px;
  white-space: nowrap;
  background: var(--admin-surface-soft);
}
.status-pill--published {
  color: var(--admin-status-ok);
}
/* 线上落后于草稿头 —— 要再发布一次才能追上 */
.status-pill--live-ahead {
  background: var(--admin-warning-bg);
  color: var(--admin-warning);
}
.status-pill--draft {
  color: var(--admin-text-muted);
  border: 1px solid var(--admin-border-soft);
}
.status-pill--unpublished {
  color: var(--admin-text-muted);
  border: 1px dashed var(--admin-border-strong);
}

/* ── 轴 B：缓冲区状态 ────────────────────────────────────────────────────────── */
.save-hint {
  font-size: 12px;
  color: var(--admin-text-muted);
  white-space: nowrap;
}
.save-hint--dirty {
  color: var(--admin-warning);
}

.link-btn {
  font-size: 12px;
  font-family: inherit;
  color: var(--admin-text-muted);
  background: transparent;
  border: none;
  padding: 2px 0;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
  white-space: nowrap;
  transition: color 0.15s;
}
.link-btn:hover:not(:disabled) {
  color: var(--admin-accent);
}
.link-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.link-btn--muted:hover:not(:disabled) {
  color: var(--admin-text-secondary);
}

/* ── 本地兜底恢复条 ──────────────────────────────────────────────────────────── */
.restore-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  padding: 7px 20px;
  border-bottom: 1px solid var(--write-border, var(--admin-border-soft));
  background: var(--admin-surface-soft);
}
.restore-text {
  font-size: 12px;
  color: var(--admin-text-secondary);
}
.restore-time {
  color: var(--admin-text-muted);
}

.menu-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.sidebar-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--admin-border-soft);
  border-radius: 6px;
  background: transparent;
  color: var(--admin-text-muted);
  cursor: pointer;
  transition:
    border-color 0.15s,
    background 0.15s,
    color 0.15s;
  flex-shrink: 0;
}
.sidebar-toggle:hover {
  border-color: var(--admin-border-soft);
  background: var(--admin-surface-soft);
  color: var(--admin-accent);
}

/* ── Body ────────────────────────────────────────────────────────────────────── */
.write-body {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}

/* ── Editor area ─────────────────────────────────────────────────────────────── */
.editor-area {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  background: transparent;
}

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

.title-input::placeholder {
  color: var(--write-placeholder, var(--admin-text-muted));
}

.editor-container {
  padding: 0 64px 60px;
  background: transparent;
}

/* ── AI trigger button (rendered via slot into ArticleMetaDialog) ────────────*/
.ai-trigger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  background: transparent;
  border: 1px solid var(--admin-border);
  border-radius: 4px;
  cursor: pointer;
  color: var(--admin-accent);
  font-size: 13px;
  font-family: inherit;
  transition:
    background 0.15s,
    border-color 0.15s;
}
.ai-trigger-btn:hover:not(:disabled) {
  background: var(--admin-accent-bg-soft);
  border-color: var(--admin-accent);
}
.ai-trigger-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ── Inline AI suggestion cards ───────────────────────────────────────────────*/
.pf-ai-inline {
  border: 1px solid var(--admin-border);
  border-radius: 4px;
  background: var(--admin-surface-soft);
  padding: 10px 12px;
}
.pf-ai-inline--row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.pf-ai-inline--chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.pf-ai-inline-body {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--admin-text-secondary);
  padding-bottom: 10px;
}
.pf-ai-inline-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: flex-end;
}
.pf-ai-inline--row .pf-ai-inline-actions {
  margin-left: auto;
  flex-shrink: 0;
}
.pf-ai-inline-val {
  font-size: 13px;
  color: var(--admin-text-secondary);
  font-weight: 500;
  flex: 1;
}
.pf-ai-inline-no-match {
  font-size: 12px;
  color: var(--admin-text-muted);
  font-style: italic;
  flex: 1;
}
.pf-ai-inline-dismiss {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--admin-text-muted);
  border-radius: var(--admin-radius);
  padding: 0;
  margin-left: auto;
  transition: color 0.15s;
}
.pf-ai-inline-dismiss:hover {
  color: var(--admin-text-secondary);
}

@keyframes ai-spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── AI action buttons ─────────────────────────────────────────────────────────*/
.ai-action {
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 4px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}
.ai-action--dismiss {
  color: var(--admin-text-secondary);
  background: var(--admin-surface-2);
  border-color: var(--admin-border);
}
.ai-action--dismiss:hover {
  background: var(--admin-border);
}
.ai-action--primary {
  color: var(--admin-text-on-accent);
  background: var(--admin-accent);
  border-color: var(--admin-accent);
}
.ai-action--primary:hover {
  background: var(--admin-accent-dark);
}

/* ── AI meta chips (category / tag suggestions) ───────────────────────────────*/
.ai-meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 4px;
  border: 1px solid transparent;
  line-height: 20px;
  font-family: inherit;
}
/* 已存在于标签库 —— 实线 */
.ai-meta-chip--existing {
  color: var(--admin-text-secondary);
  background: var(--admin-surface-2);
  border-color: var(--admin-border);
}
/* 会新建 —— 虚线，配合「新」badge */
.ai-meta-chip--new {
  color: var(--admin-text-secondary);
  background: var(--admin-surface);
  border-color: var(--admin-border-strong);
  border-style: dashed;
}
.ai-meta-chip--action {
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s;
}
.ai-meta-chip--action:not(:disabled):hover {
  border-color: var(--admin-accent);
}
.ai-meta-chip--action:not(:disabled).ai-meta-chip--existing:hover {
  background: var(--admin-accent-bg-soft);
}
.ai-meta-chip--action:not(:disabled).ai-meta-chip--new:hover {
  background: var(--admin-accent-bg-soft);
}
.ai-meta-chip__badge {
  font-size: 10px;
  font-weight: 600;
  color: var(--admin-text-secondary);
  background: var(--admin-border);
  padding: 0 4px;
  border-radius: var(--admin-radius);
}
.ai-meta-chip__plus {
  font-size: 14px;
  font-weight: 400;
  line-height: 1;
  color: var(--admin-text-muted);
  margin-left: 1px;
  transition: color 0.15s;
}
/* accent 只在鼠标表达意图的瞬间出现 */
.ai-meta-chip--action:not(:disabled):hover .ai-meta-chip__plus {
  color: var(--admin-accent);
}
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
  transition:
    background 0.15s,
    color 0.15s,
    border-color 0.15s;
  white-space: nowrap;
  line-height: 1;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--default {
  background: transparent;
  color: var(--admin-text-muted);
  border-color: var(--admin-border-soft);
}
.btn--default:hover:not(:disabled) {
  background: var(--admin-surface-soft);
  border-color: var(--admin-text-muted);
  color: var(--admin-text-secondary);
}
.btn--primary {
  background: var(--admin-accent);
  color: var(--admin-text-on-accent);
  border-color: var(--admin-accent);
  font-weight: 500;
}
.btn--primary:hover:not(:disabled) {
  background: var(--admin-accent-dark);
  border-color: var(--admin-accent-dark);
}

/* ── AI inline transition ────────────────────────────────────────────────────── */
.ai-slide-enter-active,
.ai-slide-leave-active {
  transition:
    opacity 0.18s,
    transform 0.18s;
}
.ai-slide-enter-from,
.ai-slide-leave-to {
  opacity: 0;
  transform: translateY(-3px);
}

/* ── Mobile ──────────────────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .write-v2 {
    margin: -16px -12px;
  }
  .write-toolbar {
    padding: 0 12px;
    gap: 8px;
  }
  .back-btn span,
  .tb-sep,
  .save-hint {
    display: none;
  }
  .btn {
    padding: 0 12px;
  }
  .restore-bar {
    padding: 7px 12px;
  }
  .editor-header {
    padding: 24px 20px 16px;
  }
  .title-input {
    font-size: 24px;
  }
  .editor-container {
    padding: 0 20px 40px;
  }
}
</style>
