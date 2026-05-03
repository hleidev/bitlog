<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Fold, Expand, Picture, ArrowRight } from '@element-plus/icons-vue'

const route  = useRoute()
const router = useRouter()

const articleId = computed(() => (route.params.id ? Number(route.params.id) : null))
const isEdit    = computed(() => articleId.value !== null)

// ── Mock preset for edit mode ─────────────────────────────────────────────────
const MOCK_ARTICLE = {
  title: 'Vue 3 Composition API 深度解析',
  content: `Vue 3 的 Composition API 是一套全新的逻辑组织和代码复用方式，它解决了 Options API 在大型组件中遇到的可维护性问题。

## 为什么需要 Composition API？

在 Vue 2 中，随着组件逻辑变得复杂，相关的逻辑代码被拆散在 data、methods、computed、watch 各个选项中，让代码的理解和维护变得困难。Composition API 允许我们按照逻辑关注点来组织代码，而不是按照选项类型。

## setup() 函数

setup() 是 Composition API 的入口，在组件实例创建之前执行。它接收两个参数：props 和 context。

\`\`\`ts
import { ref, onMounted } from 'vue'

export default {
  setup() {
    const count = ref(0)
    onMounted(() => console.log('mounted'))
    return { count }
  }
}
\`\`\`

## 响应式 API

- \`ref()\` 适合包装基本类型值，通过 .value 访问
- \`reactive()\` 适合包装对象，直接访问属性
- \`computed()\` 创建依赖其他响应式数据的计算属性
- \`watch()\` / \`watchEffect()\` 监听响应式数据变化

## 生命周期钩子

在 setup 中，生命周期钩子需要加上 on 前缀，例如 onMounted、onUpdated、onUnmounted。`,
  status: 'published' as const,
  pinned: true,
  categoryId: 1,
  tags: ['Vue', 'JavaScript', 'Composition API'],
  summary: 'Vue 3 Composition API 的核心概念与实践，深入解析 setup、ref、reactive 等核心 API 的使用方式和最佳实践。',
}

// ── Form state ─────────────────────────────────────────────────────────────────
const title    = ref(isEdit.value ? MOCK_ARTICLE.title : '')
const content  = ref(isEdit.value ? MOCK_ARTICLE.content : '')
const status   = ref<'draft' | 'published'>(isEdit.value ? MOCK_ARTICLE.status : 'draft')
const pinned   = ref(isEdit.value ? MOCK_ARTICLE.pinned : false)
const category = ref<number | null>(isEdit.value ? MOCK_ARTICLE.categoryId : null)
const tags     = ref<string[]>(isEdit.value ? [...MOCK_ARTICLE.tags] : [])
const summary  = ref(isEdit.value ? MOCK_ARTICLE.summary : '')

// ── Options ────────────────────────────────────────────────────────────────────
const categoryOptions = [
  { id: 1, name: '前端' },
  { id: 2, name: '后端' },
  { id: 3, name: '数据库' },
  { id: 4, name: '运维' },
]

const tagOptions = ['Vue', 'React', 'TypeScript', 'JavaScript', 'CSS', 'Java', 'Spring Boot', 'Docker', 'Kubernetes', 'Redis', 'MySQL', 'PostgreSQL', 'Rust', 'Go']

// ── Sidebar & versions ─────────────────────────────────────────────────────────
const sidebarOpen       = ref(true)
const versionsExpanded  = ref(false)

const MOCK_VERSIONS = [
  { id: 3, label: '版本 3（当前）', time: '2026-05-01 10:00', isCurrent: true  },
  { id: 2, label: '版本 2',         time: '2026-04-28 09:30', isCurrent: false },
  { id: 1, label: '版本 1',         time: '2026-04-25 14:20', isCurrent: false },
]

// ── Save state ─────────────────────────────────────────────────────────────────
const saveState = ref<'idle' | 'saving' | 'saved'>('saved')
let saveTimer: ReturnType<typeof setTimeout> | undefined

onUnmounted(() => clearTimeout(saveTimer))

const saveStateText = computed(() => {
  if (saveState.value === 'saving') return '保存中...'
  if (saveState.value === 'saved' && isEdit.value) return '已保存'
  return ''
})

// ── Word count ─────────────────────────────────────────────────────────────────
const wordCount = computed(() =>
  (title.value + content.value).replace(/\s/g, '').length
)

// ── Textarea auto-resize ───────────────────────────────────────────────────────
const contentRef = ref<HTMLTextAreaElement>()

function autoResize() {
  const el = contentRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${el.scrollHeight}px`
}

onMounted(() => nextTick(autoResize))

// ── Actions ────────────────────────────────────────────────────────────────────
function handleSaveDraft() {
  if (!title.value.trim()) {
    ElMessage.warning('请先输入文章标题')
    return
  }
  saveState.value = 'saving'
  saveTimer = setTimeout(() => {
    saveState.value = 'saved'
    status.value = 'draft'
    ElMessage.success('草稿已保存')
  }, 500)
}

function handlePublish() {
  if (!title.value.trim()) {
    ElMessage.warning('请先输入文章标题')
    return
  }
  status.value = 'published'
  ElMessage.success(isEdit.value ? '已更新发布' : '文章已发布')
}

async function handleUnpublish() {
  try {
    await ElMessageBox.confirm(
      '取消发布后读者将无法查看该文章，确认继续？',
      '取消发布',
      { confirmButtonText: '取消发布', cancelButtonText: '留下' },
    )
  } catch { return }
  status.value = 'draft'
  ElMessage.success('已取消发布，文章已变为草稿')
}

async function handleRollback(v: (typeof MOCK_VERSIONS)[0]) {
  try {
    await ElMessageBox.confirm(
      `回滚到「${v.label}」（${v.time}）？这将基于该版本创建一个新草稿。`,
      '回滚版本',
      { confirmButtonText: '回滚', cancelButtonText: '取消' },
    )
  } catch { return }
  ElMessage.success(`已回滚至 ${v.label}`)
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
        <span class="status-pill" :class="`status-pill--${status}`">
          {{ status === 'published' ? '已发布' : '草稿' }}
        </span>
      </div>

      <div class="tb-right">
        <span v-if="saveStateText" class="save-hint">{{ saveStateText }}</span>
        <span class="word-count">{{ wordCount }} 字</span>

        <template v-if="status === 'draft'">
          <el-button size="small" @click="handleSaveDraft">保存草稿</el-button>
          <el-button size="small" type="primary" @click="handlePublish">发布</el-button>
        </template>
        <template v-else>
          <el-button size="small" @click="handleSaveDraft">保存修改</el-button>
          <el-button size="small" type="danger" plain @click="handleUnpublish">取消发布</el-button>
          <el-button size="small" type="primary" @click="handlePublish">更新发布</el-button>
        </template>

        <button
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

      <!-- Editor -->
      <div class="editor-wrap">
        <div class="editor-inner">
          <input
            v-model="title"
            class="title-input"
            placeholder="文章标题..."
            maxlength="200"
          />
          <div class="editor-divider" />
          <textarea
            ref="contentRef"
            v-model="content"
            class="content-textarea"
            placeholder="开始写作..."
            @input="autoResize"
          />
        </div>
      </div>

      <!-- Sidebar -->
      <div class="meta-sidebar" :class="{ 'meta-sidebar--closed': !sidebarOpen }">
        <div class="sidebar-scroll">

          <!-- Cover -->
          <div class="sidebar-section">
            <div class="section-label">封面图</div>
            <div class="cover-upload">
              <el-icon class="cover-icon"><Picture /></el-icon>
              <span class="cover-tip">点击上传封面图</span>
              <span class="cover-hint">建议尺寸 1200 × 630</span>
            </div>
          </div>

          <!-- Summary -->
          <div class="sidebar-section">
            <div class="section-label">摘要</div>
            <el-input
              v-model="summary"
              type="textarea"
              :rows="3"
              placeholder="文章简短描述，将显示在列表页..."
              :maxlength="200"
              show-word-limit
              resize="none"
            />
          </div>

          <!-- Category -->
          <div class="sidebar-section">
            <div class="section-label">分类</div>
            <el-select v-model="category" placeholder="选择分类" clearable style="width: 100%">
              <el-option
                v-for="opt in categoryOptions"
                :key="opt.id"
                :label="opt.name"
                :value="opt.id"
              />
            </el-select>
          </div>

          <!-- Tags -->
          <div class="sidebar-section">
            <div class="section-label">标签</div>
            <el-select
              v-model="tags"
              multiple
              filterable
              allow-create
              collapse-tags
              collapse-tags-tooltip
              :reserve-keyword="false"
              placeholder="输入标签，回车创建"
              style="width: 100%"
            >
              <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </div>

          <!-- Settings -->
          <div class="sidebar-section">
            <div class="section-label">其他设置</div>
            <div class="setting-row">
              <div class="setting-info">
                <span class="setting-name">置顶文章</span>
                <span class="setting-desc">在列表顶部展示</span>
              </div>
              <el-switch v-model="pinned" />
            </div>
          </div>

          <!-- Version history (edit only) -->
          <div v-if="isEdit" class="sidebar-section">
            <button class="version-header" @click="versionsExpanded = !versionsExpanded">
              <span class="section-label" style="margin-bottom: 0">历史版本</span>
              <el-icon
                class="chevron"
                :style="{ transform: versionsExpanded ? 'rotate(90deg)' : 'rotate(0deg)' }"
              >
                <ArrowRight />
              </el-icon>
            </button>

            <Transition name="version-list">
              <div v-if="versionsExpanded" class="version-list">
                <div
                  v-for="v in MOCK_VERSIONS"
                  :key="v.id"
                  class="version-item"
                  :class="{ 'version-item--current': v.isCurrent }"
                >
                  <div class="version-info">
                    <span class="version-label">{{ v.label }}</span>
                    <span class="version-time">{{ v.time }}</span>
                  </div>
                  <span v-if="v.isCurrent" class="current-dot" />
                  <button v-else class="rollback-btn" @click="handleRollback(v)">回滚</button>
                </div>
              </div>
            </Transition>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Layout: escape admin-main padding ───────────────────────────────────────── */
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
}

.tb-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

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

.back-btn:hover {
  color: #111827;
  background: #f3f4f6;
}

.tb-sep {
  width: 1px;
  height: 18px;
  background: #e5e7eb;
  flex-shrink: 0;
}

.status-pill {
  font-size: 12px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.status-pill--published {
  background: #f0fdf4;
  color: #16a34a;
}

.status-pill--draft {
  background: #f9fafb;
  color: #6b7280;
  border: 1px solid #e5e7eb;
}

.tb-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.tb-right :deep(.el-button + .el-button) {
  margin-left: 0;
}

.save-hint {
  font-size: 12px;
  color: #9ca3af;
}

.word-count {
  font-size: 12px;
  color: #9ca3af;
}

.sidebar-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
  flex-shrink: 0;
}

.sidebar-toggle:hover {
  border-color: #c7d2fe;
  background: #f5f3ff;
  color: #4338ca;
}

/* ── Body ────────────────────────────────────────────────────────────────────── */
.write-body {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}

/* ── Editor ──────────────────────────────────────────────────────────────────── */
.editor-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 52px 0 80px;
  min-width: 0;
}

.editor-inner {
  max-width: 740px;
  margin: 0 auto;
  padding: 0 48px;
}

.title-input {
  display: block;
  width: 100%;
  font-size: 30px;
  font-weight: 700;
  color: #111827;
  border: none;
  outline: none;
  background: transparent;
  line-height: 1.3;
  padding: 0;
  margin-bottom: 20px;
  font-family: inherit;
}

.title-input::placeholder {
  color: #d1d5db;
}

.editor-divider {
  height: 1px;
  background: #f0f0f0;
  margin-bottom: 24px;
}

.content-textarea {
  display: block;
  width: 100%;
  font-size: 16px;
  line-height: 1.875;
  color: #374151;
  border: none;
  outline: none;
  background: transparent;
  resize: none;
  min-height: 480px;
  font-family: inherit;
  overflow: hidden;
}

.content-textarea::placeholder {
  color: #d1d5db;
}

/* ── Sidebar ─────────────────────────────────────────────────────────────────── */
.meta-sidebar {
  width: 300px;
  flex-shrink: 0;
  border-left: 1px solid #f0f0f0;
  background: #fafafa;
  transition: width 0.25s ease, opacity 0.2s ease;
  overflow: hidden;
}

.meta-sidebar--closed {
  width: 0;
  opacity: 0;
}

.sidebar-scroll {
  width: 300px;
  height: 100%;
  overflow-y: auto;
}

/* ── Sidebar sections ────────────────────────────────────────────────────────── */
.sidebar-section {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.section-label {
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  margin-bottom: 10px;
}

/* Cover upload */
.cover-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 96px;
  border: 2px dashed #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.cover-upload:hover {
  border-color: #c7d2fe;
  background: #f5f3ff;
}

.cover-icon {
  font-size: 22px;
  color: #9ca3af;
}

.cover-tip {
  font-size: 12px;
  color: #6b7280;
}

.cover-hint {
  font-size: 11px;
  color: #9ca3af;
}

/* Settings */
.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.setting-name {
  font-size: 13px;
  color: #374151;
  font-weight: 500;
}

.setting-desc {
  font-size: 11px;
  color: #9ca3af;
}

/* Version history */
.version-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
}

.chevron {
  color: #9ca3af;
  transition: transform 0.2s ease;
}

.version-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.version-list-enter-active,
.version-list-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.version-list-enter-from,
.version-list-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.version-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
  gap: 8px;
}

.version-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.version-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.version-label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.version-time {
  font-size: 11px;
  color: #9ca3af;
}

.current-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #16a34a;
  flex-shrink: 0;
}

.rollback-btn {
  flex-shrink: 0;
  font-size: 12px;
  color: #4338ca;
  background: transparent;
  border: 1px solid #c7d2fe;
  border-radius: 4px;
  padding: 3px 10px;
  cursor: pointer;
  transition: background 0.1s, border-color 0.1s;
}

.rollback-btn:hover {
  background: #ede9fe;
  border-color: #a5b4fc;
}

/* ── Mobile ──────────────────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .write-view {
    margin: -16px -12px;
  }

  .write-toolbar {
    padding: 0 12px;
    gap: 8px;
  }

  .save-hint,
  .word-count {
    display: none;
  }

  .editor-inner {
    padding: 0 20px;
  }

  .editor-wrap {
    padding: 28px 0 60px;
  }

  .title-input {
    font-size: 22px;
  }

  .meta-sidebar {
    position: fixed;
    top: var(--admin-header-height);
    right: 0;
    height: calc(100vh - var(--admin-header-height));
    z-index: 100;
    box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  }

  .meta-sidebar--closed {
    width: 0;
    opacity: 0;
    box-shadow: none;
  }
}
</style>
