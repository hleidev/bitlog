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
const loading    = ref(false)
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
  return categories.value.filter(c => c.name.toLowerCase().includes(kw))
})

// ── Dialog ────────────────────────────────────────────────────────────────────
const dialogVisible = ref(false)
const editingRow    = ref<Category | null>(null)
const formName      = ref('')
const formLoading   = ref(false)

const isEdit = computed(() => editingRow.value !== null)

function openCreate() {
  editingRow.value  = null
  formName.value    = ''
  dialogVisible.value = true
}

function openEdit(row: Category) {
  editingRow.value  = row
  formName.value    = row.name
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
    if (isEdit.value && editingRow.value) {
      await updateCategory(editingRow.value.id, name)
      ElMessage.success('已更新')
    } else {
      await createCategory(name)
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    fetchCategories()
  } catch (err: unknown) {
    const code = (err as { code?: number })?.code
    if (code === 43102) {
      ElMessage.warning('分类名已存在')
    } else {
      ElMessage.error('操作失败')
    }
  } finally {
    formLoading.value = false
  }
}

// ── Delete ────────────────────────────────────────────────────────────────────
async function handleDelete(row: Category) {
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
  } catch (err: unknown) {
    const code = (err as { code?: number })?.code
    if (code === 43104) {
      ElMessage.warning('该分类下存在文章，请先移除文章')
    } else if (code === 43101) {
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
        <span class="total-count">共 {{ categories.length }} 个分类</span>
        <div class="header-actions">
          <el-input
            v-model="keyword"
            placeholder="搜索分类"
            clearable
            :prefix-icon="Search"
            style="width: 180px"
          />
          <el-button :icon="RefreshLeft" @click="keyword = ''; fetchCategories()" />
          <el-button type="primary" :icon="Plus" @click="openCreate" />
        </div>
      </div>

      <!-- 分类列表 -->
      <div v-loading="loading" class="category-list">
        <el-empty v-if="filteredCategories.length === 0" description="暂无分类" :image-size="80" />

        <div
          v-for="cat in filteredCategories"
          :key="cat.id"
          class="category-item"
        >
          <span class="cat-name">{{ cat.name }}</span>

          <span v-if="cat.articleCount > 0" class="cat-count">{{ cat.articleCount }} 篇</span>

          <span class="cat-time">{{ cat.createTime }}</span>

          <span class="cat-actions">
            <button class="cat-btn cat-btn--edit" title="编辑" @click="openEdit(cat)">
              <el-icon><Edit /></el-icon>
            </button>
            <button class="cat-btn cat-btn--danger" title="删除" @click="handleDelete(cat)">
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
    :title="isEdit ? '编辑分类' : '新建分类'"
    width="min(400px, 92vw)"
    :destroy-on-close="true"
    align-center
  >
    <div class="dialog-body">
      <el-input
        v-model="formName"
        placeholder="请输入分类名称"
        maxlength="30"
        show-word-limit
        autofocus
        @keyup.enter="handleDialogConfirm"
      />
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="handleDialogConfirm">
        {{ isEdit ? '保存' : '创建' }}
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

/* List */
.category-list {
  padding: 8px 0 12px;
  display: flex;
  flex-direction: column;
  min-height: 160px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 20px;
  border-bottom: 1px solid #f9fafb;
  transition: background 0.12s;
}

.category-item:last-child {
  border-bottom: none;
}

.category-item:hover {
  background: #f9fafb;
}

.cat-name {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  min-width: 0;
  flex: 1;
}

.cat-count {
  font-size: 12px;
  font-weight: 500;
  color: #4338ca;
  background: #e0e7ff;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}

.cat-time {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
  flex-shrink: 0;
}

/* Actions */
.cat-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s;
}

.category-item:hover .cat-actions {
  opacity: 1;
}

.cat-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.1s, color 0.1s;
}

.cat-btn--edit:hover  { background: #dbeafe; color: #1d4ed8; }
.cat-btn--danger:hover { background: #fee2e2; color: #dc2626; }

/* Dialog */
.dialog-body {
  padding: 8px 0 16px;
}
</style>
