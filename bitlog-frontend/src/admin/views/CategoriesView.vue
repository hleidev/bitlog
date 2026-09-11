<script setup lang="ts">
import { ref, computed, reactive, onMounted, nextTick, watch } from 'vue'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import { formatDateTime } from '@/utils/format'
import BaseModal from '@/components/common/BaseModal.vue'
import AdminIcon from '@/admin/components/AdminIcon.vue'
import AdminListHeader from '@/admin/components/AdminListHeader.vue'
import AdminSelectionBar from '@/admin/components/AdminSelectionBar.vue'
import {
  getCategories,
  createCategory,
  updateCategory,
  deleteCategories,
  type Category,
} from '@/api/admin/category'

const toast = useToast()
const confirm = useConfirm()

/** 本页量词，批量条与操作提示共用 */
const UNIT = '个'

// ── Data ──────────────────────────────────────────────────────────────────────
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
  return categories.value.filter((c) => c.name.toLowerCase().includes(kw))
})

// 分类没有状态维度，只留一个「全部」占位，让六个列表页的头部形态一致
const activeTab = ref('all')
const tabs = computed(() => [{ key: 'all', label: '全部', count: categories.value.length }])

function handleReset() {
  keyword.value = ''
  fetchCategories()
}

// ── Selection ──────────────────────────────────────────────────────────────────
const selectedIds = reactive(new Set<number>())

const allSelected = computed(
  () =>
    filteredCategories.value.length > 0 &&
    filteredCategories.value.every((c) => selectedIds.has(c.id)),
)

const someSelected = computed(
  () => filteredCategories.value.some((c) => selectedIds.has(c.id)) && !allSelected.value,
)

function toggleAll() {
  if (allSelected.value) {
    filteredCategories.value.forEach((c) => selectedIds.delete(c.id))
  } else {
    filteredCategories.value.forEach((c) => selectedIds.add(c.id))
  }
}

function toggleRow(id: number) {
  if (selectedIds.has(id)) selectedIds.delete(id)
  else selectedIds.add(id)
}

function clearSelection() {
  selectedIds.clear()
}

watch(filteredCategories, (cats) => {
  const validIds = new Set(cats.map((c) => c.id))
  for (const id of selectedIds) {
    if (!validIds.has(id)) selectedIds.delete(id)
  }
})

// ── Batch delete ───────────────────────────────────────────────────────────
const batchLoading = ref(false)

async function handleBatchDelete() {
  const ids = Array.from(selectedIds)
  try {
    await confirm(
      ids.length === 1
        ? `确认删除「${categories.value.find((c) => c.id === ids[0])?.name}」？`
        : `确认删除选中的 ${ids.length} 个分类？`,
      '删除分类',
      { confirmText: '删除', danger: true },
    )
  } catch {
    return
  }

  batchLoading.value = true
  try {
    await deleteCategories(ids)
    toast.success(ids.length === 1 ? '已删除' : `已删除 ${ids.length} 个分类`)
    clearSelection()
    fetchCategories()
  } catch {
    toast.error('删除失败，请重试')
  } finally {
    batchLoading.value = false
  }
}

// ── Single row actions ─────────────────────────────────────────────────────
async function handleSingleDelete(row: Category) {
  try {
    await confirm(`确认删除「${row.name}」？`, '删除分类', { confirmText: '删除', danger: true })
  } catch {
    return
  }
  try {
    await deleteCategories([row.id])
    toast.success('已删除')
    fetchCategories()
  } catch {
    toast.error('删除失败，请重试')
  }
}

// ── Dialog (create + edit) ────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingCategory = ref<Category | null>(null)
const formName = ref('')
const formLoading = ref(false)
const formError = ref('')

function closeDialog() {
  if (!formLoading.value) dialogVisible.value = false
}
const dialogInputRef = ref<HTMLInputElement | null>(null)

watch(dialogVisible, (val) => {
  if (val) {
    formError.value = ''
    nextTick(() => dialogInputRef.value?.focus())
  }
})

function openCreateDialog() {
  dialogMode.value = 'create'
  editingCategory.value = null
  formName.value = ''
  dialogVisible.value = true
}

function openEditDialog(cat: Category) {
  dialogMode.value = 'edit'
  editingCategory.value = cat
  formName.value = cat.name
  dialogVisible.value = true
}

async function handleDialogSubmit() {
  if (formLoading.value) return
  formError.value = ''
  const name = formName.value.trim()
  if (!name) {
    formError.value = '请输入分类名称'
    return
  }
  formLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createCategory(name)
      toast.success('分类已创建')
    } else {
      await updateCategory(editingCategory.value!.id, name)
      toast.success('已更新')
    }
    dialogVisible.value = false
    formName.value = ''
    fetchCategories()
  } catch (err: unknown) {
    const code = (err as { code?: number })?.code
    formError.value = code === 43102 ? '分类名已存在，请换一个名称。' : '保存失败，请重试。'
  } finally {
    formLoading.value = false
  }
}
</script>

<template>
  <div class="categories-page">
    <div class="main-card">
      <AdminListHeader
        v-model:active-tab="activeTab"
        v-model:keyword="keyword"
        :tabs="tabs"
        search-placeholder="搜索分类"
        action-label="新建分类"
        @reset="handleReset"
        @action="openCreateDialog"
      />

      <AdminSelectionBar :count="selectedIds.size" :unit="UNIT" @clear="clearSelection">
        <button
          class="ghost-btn ghost-btn--sm ghost-btn--danger"
          :disabled="batchLoading"
          @click="handleBatchDelete"
        >
          <span v-if="batchLoading" class="btn-spinner btn-spinner--dark" />
          批量删除
        </button>
      </AdminSelectionBar>

      <!-- Table -->
      <div class="table-wrap" :class="{ 'table-wrap--loading': loading }">
        <div v-if="loading" class="table-loading">
          <svg class="spinner" viewBox="0 0 24 24" fill="none">
            <circle
              cx="12"
              cy="12"
              r="9"
              stroke="currentColor"
              stroke-width="2"
              stroke-dasharray="40"
              stroke-dashoffset="15"
            />
          </svg>
        </div>

        <table class="data-table">
          <thead>
            <tr>
              <th class="col-check">
                <input
                  type="checkbox"
                  class="row-checkbox"
                  :checked="allSelected"
                  :indeterminate="someSelected"
                  @change="toggleAll"
                />
              </th>
              <th class="col-main">名称</th>
              <th class="col-num">文章数</th>
              <th class="col-time">创建时间</th>
              <th class="col-actions" />
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredCategories.length === 0 && !loading">
              <td colspan="5" class="empty-cell">
                <div class="empty-state">
                  <AdminIcon name="category" class="empty-icon" />
                  <span>{{ keyword ? '没有匹配的分类' : '还没有分类，点击右上角新建' }}</span>
                </div>
              </td>
            </tr>
            <tr
              v-for="row in filteredCategories"
              :key="row.id"
              :class="{ 'row--selected': selectedIds.has(row.id) }"
            >
              <td class="col-check">
                <input
                  type="checkbox"
                  class="row-checkbox"
                  :checked="selectedIds.has(row.id)"
                  @change="toggleRow(row.id)"
                />
              </td>
              <td class="col-main" :title="row.name">{{ row.name }}</td>
              <td class="col-num">
                <span :class="row.articleCount === 0 ? 'cell-muted' : ''">{{
                  row.articleCount
                }}</span>
              </td>
              <td class="col-time">
                <span class="cell-muted">{{ formatDateTime(row.createTime) }}</span>
              </td>
              <td class="col-actions">
                <div class="row-actions">
                  <button class="action-btn" @click="openEditDialog(row)">编辑</button>
                  <button class="action-btn action-btn--danger" @click="handleSingleDelete(row)">
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- Create / Edit dialog -->
  <BaseModal
    :visible="dialogVisible"
    width="420px"
    :aria-label="dialogMode === 'create' ? '新建分类' : '编辑分类'"
    @close="closeDialog"
  >
    <div class="dialog-box admin-form-dialog">
      <h3 class="dialog-title">{{ dialogMode === 'create' ? '新建分类' : '编辑分类' }}</h3>
      <input
        ref="dialogInputRef"
        v-model="formName"
        class="dialog-input"
        aria-label="分类名称"
        :aria-invalid="!!formError"
        :disabled="formLoading"
        placeholder="请输入分类名称"
        maxlength="30"
        @keyup.enter="handleDialogSubmit"
      />
      <p class="input-hint">{{ formName.length }} / 30</p>
      <p v-if="formError" class="dialog-error" role="alert">{{ formError }}</p>
      <div class="dialog-actions">
        <button class="dialog-btn dialog-btn--cancel" :disabled="formLoading" @click="closeDialog">
          取消
        </button>
        <button
          class="dialog-btn dialog-btn--ok"
          :disabled="formLoading || !formName.trim()"
          @click="handleDialogSubmit"
        >
          <span v-if="formLoading" class="btn-spinner" />
          {{ dialogMode === 'create' ? '创建' : '保存' }}
        </button>
      </div>
    </div>
  </BaseModal>
</template>

<style scoped>
/* 固定列合计 40+80+150+148=418，再给主列留 240px 下限；窄于此宽度改为横向滚动，
   而不是把主列压成 0（见 variables.css 中 .data-table 的说明） */
.data-table {
  min-width: 660px;
}

.categories-page {
  display: flex;
  flex-direction: column;
}

.ghost-btn--danger {
  color: var(--admin-danger);
  border-color: var(--admin-danger-border);
}

/* 头部、批量条、列宽、空状态图标全部走 admin/styles/variables.css 与共享组件 */
</style>
