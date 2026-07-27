<script setup lang="ts">
import { ref, computed, reactive, onMounted, nextTick, watch } from 'vue'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import {
  getCategories,
  createCategory,
  updateCategory,
  deleteCategories,
  type Category,
} from '@/api/admin/category'

const toast = useToast()
const confirm = useConfirm()

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
const dialogInputRef = ref<HTMLInputElement | null>(null)

watch(dialogVisible, (val) => {
  if (val) nextTick(() => dialogInputRef.value?.focus())
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
  const name = formName.value.trim()
  if (!name) {
    toast.warning('请输入分类名称')
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
    if (code === 43102) toast.warning('分类名已存在')
    else toast.error('操作失败')
  } finally {
    formLoading.value = false
  }
}
</script>

<template>
  <div class="categories-page">
    <div class="main-card">
      <!-- Header -->
      <div class="card-header">
        <div class="header-left">
          <p class="stats-text">共 {{ categories.length }} 个分类</p>
        </div>
        <div class="header-actions">
          <div class="search-wrap">
            <svg
              class="search-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="11" cy="11" r="7" />
              <path d="m21 21-4.35-4.35" />
            </svg>
            <input v-model="keyword" class="search-input" placeholder="搜索分类" />
            <button v-if="keyword" class="search-clear" @click="keyword = ''">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path
                  d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
                />
              </svg>
            </button>
          </div>
          <button class="icon-btn" title="刷新" @click="fetchCategories">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"
              />
            </svg>
          </button>
          <button class="primary-btn" @click="openCreateDialog">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
            </svg>
            新建分类
          </button>
        </div>
      </div>

      <!-- Selection bar -->
      <Transition name="sel-bar">
        <div v-if="selectedIds.size > 0" class="selection-bar">
          <span class="sel-count"
            >已选 <b>{{ selectedIds.size }}</b> 个</span
          >
          <div class="sel-actions">
            <button class="ghost-btn ghost-btn--sm" @click="toggleAll">
              {{ allSelected ? '取消全选' : '全选' }}
            </button>
            <button
              class="ghost-btn ghost-btn--sm ghost-btn--danger"
              :disabled="batchLoading"
              @click="handleBatchDelete"
            >
              <span v-if="batchLoading" class="btn-spinner btn-spinner--dark" />
              批量删除
            </button>
          </div>
          <button class="cancel-btn" @click="clearSelection">取消选择</button>
        </div>
      </Transition>

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
              <th class="col-name">名称</th>
              <th class="col-count">文章数</th>
              <th class="col-time">创建时间</th>
              <th class="col-actions" />
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredCategories.length === 0 && !loading">
              <td colspan="5" class="empty-cell">
                <div class="empty-state">
                  <svg viewBox="0 0 24 24" fill="currentColor" class="empty-icon">
                    <path
                      d="M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"
                    />
                  </svg>
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
              <td class="col-name">{{ row.name }}</td>
              <td class="col-count">
                <span class="count-badge" :class="{ 'count-badge--zero': row.articleCount === 0 }">
                  {{ row.articleCount }} 篇
                </span>
              </td>
              <td class="col-time">
                <span class="cell-muted">{{ row.createTime }}</span>
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
  <Transition name="dialog-fade">
    <div v-if="dialogVisible" class="dialog-overlay" @click.self="dialogVisible = false">
      <div class="dialog-box">
        <h3 class="dialog-title">{{ dialogMode === 'create' ? '新建分类' : '编辑分类' }}</h3>
        <input
          ref="dialogInputRef"
          v-model="formName"
          class="dialog-input"
          :placeholder="dialogMode === 'create' ? '请输入分类名称' : '请输入分类名称'"
          maxlength="30"
          @keyup.enter="handleDialogSubmit"
        />
        <p class="input-hint">{{ formName.length }} / 30</p>
        <div class="dialog-actions">
          <button class="dialog-btn dialog-btn--cancel" @click="dialogVisible = false">取消</button>
          <button
            class="dialog-btn dialog-btn--ok"
            :disabled="formLoading"
            @click="handleDialogSubmit"
          >
            <span v-if="formLoading" class="btn-spinner" />
            {{ dialogMode === 'create' ? '创建' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.categories-page {
  display: flex;
  flex-direction: column;
}

.main-card {
  background: var(--admin-header-bg);
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  overflow: hidden;
}

/* ── Header ── */

.card-header {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  border-bottom: 1px solid var(--admin-sidebar-border);
  padding: 0 20px;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
  flex-shrink: 0;
}

/* ── Search ── */

.search-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 8px;
  width: 14px;
  height: 14px;
  color: var(--admin-sidebar-text-muted);
  pointer-events: none;
}

.search-input {
  height: 32px;
  padding: 0 28px 0 28px;
  width: 180px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  background: var(--admin-surface-input);
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-text-primary);
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: var(--admin-accent);
}
.search-input::placeholder {
  color: var(--admin-sidebar-text-muted);
}

.search-clear {
  position: absolute;
  right: 6px;
  display: flex;
  align-items: center;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--admin-sidebar-text-muted);
  padding: 0;
}

.search-clear svg {
  width: 12px;
  height: 12px;
}
.search-clear:hover {
  color: var(--admin-sidebar-text);
}

/* ── Buttons ── */

.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  background: transparent;
  color: var(--admin-sidebar-text);
  cursor: pointer;
  transition: background 0.15s;
  flex-shrink: 0;
}

.icon-btn svg {
  width: 14px;
  height: 14px;
}
.icon-btn:hover {
  background: var(--admin-sidebar-hover);
}

.primary-btn {
  height: 32px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: var(--admin-accent);
  color: var(--admin-text-on-accent);
  border: none;
  border-radius: 4px;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}

.primary-btn svg {
  width: 13px;
  height: 13px;
}
.primary-btn:hover {
  background: var(--admin-accent-dark);
}

/* ── Stats text ── */

.stats-text {
  font-size: 13px;
  color: var(--admin-sidebar-text-muted);
  white-space: nowrap;
}

/* ── Selection bar ── */

.selection-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 20px;
  background: var(--admin-accent-bg-subtle);
  border-bottom: 1px solid var(--admin-accent-border);
}

.sel-count {
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-accent);
  white-space: nowrap;
}

.sel-count b {
  font-weight: 700;
}

.sel-actions {
  display: flex;
  gap: 6px;
}

.ghost-btn {
  height: 28px;
  padding: 0 12px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  background: var(--admin-surface-input);
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}

.ghost-btn--sm {
  height: 26px;
}
.ghost-btn:hover {
  background: var(--admin-sidebar-hover);
}
.ghost-btn--danger {
  color: var(--admin-danger);
  border-color: var(--admin-danger-border);
}
.ghost-btn--danger:hover {
  background: rgba(var(--admin-danger-rgb), 0.05);
}

.cancel-btn {
  margin-left: auto;
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.15s;
}

.cancel-btn:hover {
  color: var(--admin-sidebar-text);
}

.sel-bar-enter-active,
.sel-bar-leave-active {
  transition:
    opacity 0.15s,
    transform 0.15s;
}
.sel-bar-enter-from,
.sel-bar-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ── Table ── */

.col-check {
  width: 40px;
}
.col-name {
  max-width: 200px;
}
.col-count {
  width: 80px;
  text-align: center;
}
.col-time {
  width: 120px;
  white-space: nowrap;
}
.col-actions {
  width: 100px;
  text-align: right;
}

.col-name {
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.row--selected td {
  background: var(--admin-accent-bg-subtle);
}

.count-badge {
  font-size: 12px;
  font-weight: 500;
  color: var(--admin-accent-dark);
  background: var(--admin-accent-bg-soft);
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
}

.count-badge--zero {
  color: var(--admin-sidebar-text-muted);
  background: rgba(var(--admin-muted-rgb), 0.1);
}

.cell-muted {
  font-size: 12.5px;
  color: var(--admin-sidebar-text-muted);
}

/* ── Dialog ── */
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.15s;
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
</style>
