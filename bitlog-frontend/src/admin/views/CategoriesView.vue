<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft, Edit, Delete } from '@element-plus/icons-vue'
import {
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  type Category,
} from '@/api/admin/category'

// ── State ─────────────────────────────────────────────────────────────────────
const loading = ref(false)
const categories = ref<Category[]>([])

async function fetchCategories() {
  loading.value = true
  try {
    categories.value = await getCategories()
  } finally {
    loading.value = false
  }
}

onMounted(fetchCategories)

// ── Search ────────────────────────────────────────────────────────────────────
const keyword = ref('')

const filteredCategories = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return categories.value
  return categories.value
    .map((parent) => {
      const matchChildren = parent.children?.filter((c) =>
        c.name.toLowerCase().includes(kw),
      ) ?? []
      if (parent.name.toLowerCase().includes(kw)) {
        // 父分类名匹配时展示完整卡片（含全部子分类），不再裁剪子分类
        return { ...parent }
      }
      if (matchChildren.length > 0) {
        return { ...parent, children: matchChildren }
      }
      return null
    })
    .filter(Boolean) as Category[]
})

// ── Stats ─────────────────────────────────────────────────────────────────────
const totalCount = computed(() => {
  let n = 0
  categories.value.forEach((p) => {
    n++
    n += p.children?.length ?? 0
  })
  return n
})

// ── Dialog ────────────────────────────────────────────────────────────────────
type DialogMode = 'create-parent' | 'create-child' | 'edit'

const dialogVisible = ref(false)
const dialogMode = ref<DialogMode>('create-parent')
const dialogParent = ref<Category | null>(null)
const editingRow = ref<Category | null>(null)
const formName = ref('')
const formParentId = ref<number>(0)   // 0 = 顶级，正数 = 父分类 ID
const formLoading = ref(false)

// 编辑时归属下拉选项（顶级分类列表，排除自身）
const parentOptions = computed(() => {
  const base = [{ label: '顶级分类', value: 0 }]
  const tops = categories.value
    .filter((p) => p.id !== editingRow.value?.id)
    .map((p) => ({ label: p.name, value: p.id }))
  return [...base, ...tops]
})

// 编辑的分类有子分类时不允许修改归属
const canChangeParent = computed(
  () => dialogMode.value === 'edit' && !(editingRow.value?.children?.length),
)

const dialogTitle = computed(() => {
  if (dialogMode.value === 'create-parent') return '新建分类'
  if (dialogMode.value === 'create-child') return `在「${dialogParent.value?.name}」下新建子分类`
  return '编辑分类'
})

function openCreateParent() {
  dialogMode.value = 'create-parent'
  dialogParent.value = null
  editingRow.value = null
  formName.value = ''
  formParentId.value = 0
  dialogVisible.value = true
}

function openCreateChild(parent: Category) {
  dialogMode.value = 'create-child'
  dialogParent.value = parent
  editingRow.value = null
  formName.value = ''
  formParentId.value = 0
  dialogVisible.value = true
}

function openEdit(row: Category) {
  dialogMode.value = 'edit'
  dialogParent.value = null
  editingRow.value = row
  formName.value = row.name
  formParentId.value = row.parentId ?? 0
  dialogVisible.value = true
}

async function handleDialogConfirm() {
  const name = formName.value.trim()
  if (!name) {
    ElMessage.warning('请输入分类名称')
    return
  }

  formLoading.value = true
  try {
    if (dialogMode.value === 'create-parent') {
      await createCategory(name)
      ElMessage.success('分类已创建')
    } else if (dialogMode.value === 'create-child' && dialogParent.value) {
      await createCategory(name, dialogParent.value.id)
      ElMessage.success('子分类已创建')
    } else if (dialogMode.value === 'edit' && editingRow.value) {
      const originalParentId = editingRow.value.parentId ?? 0
      const parentId = formParentId.value !== originalParentId ? formParentId.value : undefined
      await updateCategory(editingRow.value.id, name, parentId)
      ElMessage.success('已更新')
    }
    dialogVisible.value = false
    fetchCategories()
  } catch (err: any) {
    if (err?.code === 43102) {
      ElMessage.warning('分类名已存在')
    } else if (err?.code === 43105) {
      ElMessage.warning('父分类不存在或不是顶级分类')
    } else if (err?.code === 43106) {
      ElMessage.warning('存在子分类，不可修改归属')
    } else {
      ElMessage.error('操作失败')
    }
  } finally {
    formLoading.value = false
  }
}

// ── Article count ─────────────────────────────────────────────────────────────
// 仅支持两级结构：父分类显示自身 + 所有直接子分类的文章数之和
function displayCount(row: Category): number {
  if (row.parentId !== null) return row.articleCount
  return (row.children ?? []).reduce((sum, c) => sum + c.articleCount, row.articleCount)
}

// ── Delete ────────────────────────────────────────────────────────────────────
async function handleDelete(row: Category, parent?: Category) {
  const childCount = row.children?.length ?? 0
  if (!parent && childCount > 0) {
    ElMessage.warning(`请先删除「${row.name}」下的 ${childCount} 个子分类`)
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除分类「${row.name}」？`, '删除分类', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch { return }

  try {
    await deleteCategory(row.id)
    ElMessage.success('已删除')
    fetchCategories()
  } catch (err: any) {
    if (err?.code === 43103) {
      ElMessage.warning('请先删除该分类下的所有子分类')
    } else if (err?.code === 43104) {
      ElMessage.warning('该分类下存在文章，请先移除文章')
    } else if (err?.code === 43101) {
      ElMessage.warning('分类不存在')
    } else {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<template>
  <div class="categories-page">
    <div class="main-card">
      <!-- 顶部 -->
      <div class="card-header">
        <span class="total-count">共 {{ totalCount }} 个分类</span>
        <div class="header-actions">
          <el-input
            v-model="keyword"
            placeholder="搜索分类"
            clearable
            :prefix-icon="Search"
            style="width: 180px"
          />
          <el-button :icon="RefreshLeft" @click="keyword = ''; fetchCategories()" />
          <el-button type="primary" :icon="Plus" @click="openCreateParent" />
        </div>
      </div>

      <!-- 分类卡片列表 -->
      <div v-loading="loading" class="category-list">
        <el-empty v-if="filteredCategories.length === 0" description="暂无分类" :image-size="80" />

        <div v-for="parent in filteredCategories" :key="parent.id" class="category-card">
          <!-- 父分类标题行 -->
          <div class="cat-header">
            <span class="cat-title">{{ parent.name }}</span>

            <span class="cat-meta">
              <span v-if="displayCount(parent) > 0" class="meta-badge">
                {{ displayCount(parent) }} 篇
              </span>
              <span v-if="parent.children?.length" class="meta-badge meta-badge--sub">
                {{ parent.children.length }} 个子分类
              </span>
            </span>

            <span class="cat-actions">
              <button class="cat-btn cat-btn--add" title="添加子分类" @click="openCreateChild(parent)">
                <el-icon><Plus /></el-icon>
              </button>
              <button class="cat-btn cat-btn--edit" title="编辑" @click="openEdit(parent)">
                <el-icon><Edit /></el-icon>
              </button>
              <button class="cat-btn cat-btn--danger" title="删除" @click="handleDelete(parent)">
                <el-icon><Delete /></el-icon>
              </button>
              <span class="meta-time">{{ parent.createTime }}</span>
            </span>
          </div>

          <!-- 子分类 chips -->
          <div v-if="parent.children?.length" class="cat-children">
            <div
              v-for="child in parent.children"
              :key="child.id"
              class="child-chip"
            >
              <span class="child-name">{{ child.name }}</span>
              <span v-if="child.articleCount > 0" class="child-count">{{ child.articleCount }}</span>
              <span class="child-actions">
                <button class="child-btn" title="编辑" @click="openEdit(child)">
                  <el-icon><Edit /></el-icon>
                </button>
                <button class="child-btn child-btn--danger" title="删除" @click="handleDelete(child, parent)">
                  <el-icon><Delete /></el-icon>
                </button>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 新建 / 编辑弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="min(400px, 92vw)"
    :destroy-on-close="true"
    align-center
  >
    <div class="dialog-body">
      <div class="form-item">
        <label class="form-label">名称</label>
        <el-input
          v-model="formName"
          placeholder="请输入分类名称"
          maxlength="30"
          show-word-limit
          autofocus
          @keyup.enter="handleDialogConfirm"
        />
      </div>

      <div v-if="dialogMode === 'edit'" class="form-item">
        <label class="form-label">归属</label>
        <el-tooltip
          :disabled="canChangeParent"
          content="存在子分类时不可修改归属"
          placement="top"
        >
          <el-select
            v-model="formParentId"
            :disabled="!canChangeParent"
            style="width: 100%"
          >
            <el-option
              v-for="opt in parentOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-tooltip>
      </div>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="handleDialogConfirm">
        {{ dialogMode === 'edit' ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.categories-page {
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

.header-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

/* Category list */
.category-list {
  padding: 16px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 200px;
}

/* Category card */
.category-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.category-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.08);
}

/* Parent header row */
.cat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f9fafb;
  border-bottom: 1px solid transparent;
  transition: background 0.15s;
}

.category-card:has(.cat-children) .cat-header {
  border-bottom-color: #f0f0f0;
}

.cat-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  flex-shrink: 0;
}

.cat-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.meta-badge {
  font-size: 11px;
  font-weight: 500;
  color: #4338ca;
  background: #e0e7ff;
  padding: 2px 7px;
  border-radius: 10px;
  white-space: nowrap;
}

.meta-badge--sub {
  color: #374151;
  background: #f3f4f6;
}

.meta-time {
  font-size: 12px;
  color: #9ca3af;
}

/* Parent action buttons */
.cat-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  flex-shrink: 0;
}

.cat-actions .cat-btn {
  opacity: 0;
  transition: opacity 0.15s;
}

.category-card:hover .cat-actions .cat-btn {
  opacity: 1;
}

.cat-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 5px;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.1s, color 0.1s;
}

.cat-btn--add {
  color: #6366f1;
}

.cat-btn--add:hover {
  background: #ede9fe;
  color: #4338ca;
}

.cat-btn--edit {
  color: #3b82f6;
}

.cat-btn--edit:hover {
  background: #dbeafe;
  color: #1d4ed8;
}

.cat-btn--danger {
  color: #f87171;
}

.cat-btn--danger:hover {
  background: #fee2e2;
  color: #dc2626;
}

/* Children chip area */
.cat-children {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  background: #fff;
}

/* Child chip */
.child-chip {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #fafafa;
  cursor: default;
  transition: border-color 0.15s, background 0.15s;
}

.child-chip:hover {
  border-color: #c7d2fe;
  background: #f5f3ff;
}

.child-name {
  font-size: 13px;
  color: #374151;
  white-space: nowrap;
}

.child-count {
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

/* Child action buttons — slide in on hover */
.child-actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  max-width: 0;
  overflow: hidden;
  opacity: 0;
  transition: max-width 0.2s ease, opacity 0.15s;
}

.child-chip:hover .child-actions {
  max-width: 50px;
  opacity: 1;
}

.child-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.1s, color 0.1s;
}

.child-btn:hover {
  background: #e5e7eb;
  color: #111827;
}

.child-btn--danger:hover {
  background: #fee2e2;
  color: #dc2626;
}

/* Dialog */
.dialog-body {
  padding: 8px 0 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  color: #374151;
  font-weight: 500;
}
</style>
