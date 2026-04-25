<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Edit, Delete, Plus } from '@element-plus/icons-vue'
import { getTags, createTag, updateTag, deleteTags, type Tag } from '@/api/admin/tag'

// ── State ─────────────────────────────────────────────────────────────────────
const loading = ref(false)
const allTags = ref<Tag[]>([])

async function fetchTags() {
  loading.value = true
  try {
    allTags.value = await getTags()
  } finally {
    loading.value = false
  }
}

onMounted(fetchTags)

// ── Search ────────────────────────────────────────────────────────────────────
const keyword = ref('')

const filteredTags = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return allTags.value
  return allTags.value.filter((t) => t.name.toLowerCase().includes(kw))
})

// ── Selection ─────────────────────────────────────────────────────────────────
const selectedIds = ref<Set<number>>(new Set())

function toggleSelect(tag: Tag) {
  if (selectedIds.value.has(tag.id)) {
    selectedIds.value.delete(tag.id)
  } else {
    selectedIds.value.add(tag.id)
  }
  selectedIds.value = new Set(selectedIds.value)
}

function clearSelection() {
  selectedIds.value = new Set()
}

const selectedTags = computed(() =>
  allTags.value.filter((t) => selectedIds.value.has(t.id)),
)

// ── Create / Edit dialog ──────────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingTag = ref<Tag | null>(null)
const formName = ref('')
const formLoading = ref(false)

function openCreate() {
  dialogMode.value = 'create'
  editingTag.value = null
  formName.value = ''
  dialogVisible.value = true
}

function openEdit(tag: Tag) {
  dialogMode.value = 'edit'
  editingTag.value = tag
  formName.value = tag.name
  dialogVisible.value = true
}

async function handleDialogConfirm() {
  const name = formName.value.trim()
  if (!name) {
    ElMessage.warning('请输入标签名')
    return
  }
  formLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createTag(name)
      ElMessage.success('标签已创建')
    } else if (editingTag.value) {
      await updateTag(editingTag.value.id, name)
      ElMessage.success('标签已更新')
    }
    dialogVisible.value = false
    fetchTags()
  } catch (err: any) {
    if (err?.code === 43202) {
      ElMessage.warning('标签名已存在')
    } else {
      ElMessage.error('操作失败')
    }
  } finally {
    formLoading.value = false
  }
}

// ── Delete ────────────────────────────────────────────────────────────────────
async function handleDelete(tag: Tag) {
  const msg = tag.articleCount > 0
    ? `标签「${tag.name}」下有 ${tag.articleCount} 篇文章，删除后这些文章将移除此标签，确认继续？`
    : `确认删除标签「${tag.name}」？`
  try {
    await ElMessageBox.confirm(msg, '删除标签', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch { return }
  try {
    await deleteTags([tag.id])
    selectedIds.value.delete(tag.id)
    selectedIds.value = new Set(selectedIds.value)
    ElMessage.success('标签已删除')
    fetchTags()
  } catch {
    ElMessage.error('删除失败')
  }
}

async function handleBatchDelete() {
  const hasLinked = selectedTags.value.some((t) => t.articleCount > 0)
  const msg = hasLinked
    ? `选中的标签中部分有关联文章，删除后文章将移除这些标签。确认批量删除 ${selectedTags.value.length} 个标签？`
    : `确认批量删除选中的 ${selectedTags.value.length} 个标签？`
  try {
    await ElMessageBox.confirm(msg, '批量删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch { return }
  try {
    const count = selectedTags.value.length
    await deleteTags(selectedTags.value.map((t) => t.id))
    clearSelection()
    ElMessage.success(`已删除 ${count} 个标签`)
    fetchTags()
  } catch {
    ElMessage.error('删除失败')
  }
}
</script>

<template>
  <div class="tags-page">
    <div class="main-card">
      <!-- 顶部 -->
      <div class="card-header">
        <span class="total-count">共 {{ allTags.length }} 个标签</span>
        <div class="header-actions">
          <el-input
            v-model="keyword"
            placeholder="搜索标签"
            clearable
            :prefix-icon="Search"
            style="width: 180px"
          />
          <el-button type="primary" :icon="Plus" @click="openCreate">新建标签</el-button>
        </div>
      </div>

      <!-- 批量操作栏 -->
      <Transition name="sel-bar">
        <div v-if="selectedIds.size > 0" class="selection-bar">
          <span class="sel-count">已选 <b>{{ selectedIds.size }}</b> 个</span>
          <el-button size="small" type="danger" plain @click="handleBatchDelete">批量删除</el-button>
          <el-button size="small" text class="sel-cancel" @click="clearSelection">取消选择</el-button>
        </div>
      </Transition>

      <!-- 标签云 -->
      <div v-loading="loading" class="tag-cloud">
        <el-empty v-if="!loading && filteredTags.length === 0" description="暂无标签" :image-size="80" />

        <div
          v-for="tag in filteredTags"
          :key="tag.id"
          class="tag-item"
          :class="{ 'tag-item--selected': selectedIds.has(tag.id) }"
          @click="toggleSelect(tag)"
        >
          <!-- 选中勾 -->
          <Transition name="check">
            <span v-if="selectedIds.has(tag.id)" class="tag-check">✓</span>
          </Transition>

          <span class="tag-name">{{ tag.name }}</span>

          <span v-if="tag.articleCount > 0" class="tag-count">{{ tag.articleCount }}</span>

          <!-- hover 操作按钮 -->
          <span class="tag-actions">
            <button class="tag-action-btn" title="编辑" @click.stop="openEdit(tag)">
              <el-icon><Edit /></el-icon>
            </button>
            <button class="tag-action-btn tag-action-btn--danger" title="删除" @click.stop="handleDelete(tag)">
              <el-icon><Delete /></el-icon>
            </button>
          </span>
        </div>
      </div>
    </div>
  </div>

  <!-- 新建 / 编辑弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogMode === 'create' ? '新建标签' : '编辑标签'"
    width="min(380px, 92vw)"
    :destroy-on-close="true"
    align-center
  >
    <div class="dialog-body">
      <el-input
        v-model="formName"
        placeholder="请输入标签名"
        maxlength="30"
        show-word-limit
        autofocus
        @keyup.enter="handleDialogConfirm"
      />
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="handleDialogConfirm">
        {{ dialogMode === 'create' ? '创建' : '保存' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.tags-page {
  display: flex;
  flex-direction: column;
}

.main-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

/* Header */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #f0f0f0;
  min-height: 56px;
  gap: 12px;
}

.total-count {
  font-size: 13px;
  color: #9ca3af;
  white-space: nowrap;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Selection bar */
.selection-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  background: #eff6ff;
  border-bottom: 1px solid #bfdbfe;
}

.sel-count {
  font-size: 13px;
  color: #1d4ed8;
}

.sel-count b { font-weight: 700; }

.sel-cancel {
  margin-left: auto;
  color: #6b7280;
}

.sel-bar-enter-active,
.sel-bar-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.sel-bar-enter-from,
.sel-bar-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* Tag cloud */
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 24px 20px;
  min-height: 200px;
  align-content: flex-start;
  justify-content: flex-start;
}

.tag-cloud :deep(.el-empty) {
  flex: 1;
  justify-content: center;
}

/* Tag item */
.tag-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fafafa;
  cursor: pointer;
  user-select: none;
  transition: border-color 0.15s, background 0.15s, box-shadow 0.15s, transform 0.1s;
}

.tag-item:hover {
  border-color: #c7d2fe;
  background: #f5f3ff;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
  transform: translateY(-1px);
}

.tag-item:hover .tag-actions {
  max-width: 52px;
  opacity: 1;
}

.tag-item--selected {
  border-color: #6366f1;
  background: #eef2ff;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.15);
}

.tag-item--selected .tag-name {
  color: #4338ca;
}

/* Check mark */
.tag-check {
  font-size: 11px;
  font-weight: 700;
  color: #6366f1;
  line-height: 1;
}

.check-enter-active,
.check-leave-active {
  transition: opacity 0.1s, transform 0.1s;
}
.check-enter-from,
.check-leave-to {
  opacity: 0;
  transform: scale(0.5);
}

/* Tag name */
.tag-name {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  white-space: nowrap;
  transition: color 0.15s;
}

/* Article count badge */
.tag-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 600;
  background: #e0e7ff;
  color: #4338ca;
  line-height: 1;
}

/* Hover action buttons */
.tag-actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  max-width: 0;
  overflow: hidden;
  opacity: 0;
  transition: max-width 0.2s ease, opacity 0.15s;
}

.tag-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  transition: background 0.1s, color 0.1s;
  font-size: 13px;
}

.tag-action-btn:hover {
  background: #e5e7eb;
  color: #111827;
}

.tag-action-btn--danger:hover {
  background: #fee2e2;
  color: #dc2626;
}

/* Dialog */
.dialog-body {
  padding: 8px 0 16px;
}
</style>
