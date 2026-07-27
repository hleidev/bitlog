<script setup lang="ts">
import { ref, computed, reactive, onMounted, nextTick, watch } from 'vue'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import { getTags, createTag, updateTag, deleteTags, type Tag } from '@/api/admin/tag'

const toast = useToast()
const confirm = useConfirm()

// ── Data ──────────────────────────────────────────────────────────────────────
const loading = ref(false)
const tags = ref<Tag[]>([])

async function fetchTags() {
  loading.value = true
  try {
    tags.value = await getTags()
  } finally {
    loading.value = false
  }
}

onMounted(fetchTags)

// ── Search ────────────────────────────────────────────────────────────────────
const keyword = ref('')

const filteredTags = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return tags.value
  return tags.value.filter((t) => t.name.toLowerCase().includes(kw))
})

// ── Selection ──────────────────────────────────────────────────────────────────
const selectedIds = reactive(new Set<number>())

const allSelected = computed(
  () => filteredTags.value.length > 0 && filteredTags.value.every((t) => selectedIds.has(t.id)),
)

const someSelected = computed(
  () => filteredTags.value.some((t) => selectedIds.has(t.id)) && !allSelected.value,
)

function toggleAll() {
  if (allSelected.value) {
    filteredTags.value.forEach((t) => selectedIds.delete(t.id))
  } else {
    filteredTags.value.forEach((t) => selectedIds.add(t.id))
  }
}

function toggleRow(id: number) {
  if (selectedIds.has(id)) selectedIds.delete(id)
  else selectedIds.add(id)
}

function clearSelection() {
  selectedIds.clear()
}

watch(filteredTags, (list) => {
  const validIds = new Set(list.map((t) => t.id))
  for (const id of selectedIds) {
    if (!validIds.has(id)) selectedIds.delete(id)
  }
})

// ── Batch delete ───────────────────────────────────────────────────────────
const batchLoading = ref(false)

async function handleBatchDelete() {
  const ids = Array.from(selectedIds)
  const hasLinked = tags.value.filter((t) => ids.includes(t.id)).some((t) => t.articleCount > 0)

  const msg =
    ids.length === 1
      ? `确认删除「${tags.value.find((t) => t.id === ids[0])?.name}」？`
      : hasLinked
        ? `选中标签中部分有关联文章，删除后文章将移除这些标签。确认删除 ${ids.length} 个标签？`
        : `确认删除选中的 ${ids.length} 个标签？`

  try {
    await confirm(msg, '删除标签', { confirmText: '删除', danger: true })
  } catch {
    return
  }

  batchLoading.value = true
  try {
    await deleteTags(ids)
    toast.success(ids.length === 1 ? '已删除' : `已删除 ${ids.length} 个标签`)
    clearSelection()
    fetchTags()
  } catch {
    toast.error('删除失败，请重试')
  } finally {
    batchLoading.value = false
  }
}

// ── Single row actions ─────────────────────────────────────────────────────
async function handleSingleDelete(row: Tag) {
  const msg =
    row.articleCount > 0
      ? `「${row.name}」有关联文章，删除后文章将移除此标签。确认删除？`
      : `确认删除「${row.name}」？`
  try {
    await confirm(msg, '删除标签', { confirmText: '删除', danger: true })
  } catch {
    return
  }
  try {
    await deleteTags([row.id])
    toast.success('已删除')
    fetchTags()
  } catch {
    toast.error('删除失败，请重试')
  }
}

// ── Dialog (create + edit) ────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingTag = ref<Tag | null>(null)
const formName = ref('')
const formLoading = ref(false)
const dialogInputRef = ref<HTMLInputElement | null>(null)

watch(dialogVisible, (val) => {
  if (val) nextTick(() => dialogInputRef.value?.focus())
})

function openCreateDialog() {
  dialogMode.value = 'create'
  editingTag.value = null
  formName.value = ''
  dialogVisible.value = true
}

function openEditDialog(tag: Tag) {
  dialogMode.value = 'edit'
  editingTag.value = tag
  formName.value = tag.name
  dialogVisible.value = true
}

async function handleDialogSubmit() {
  const name = formName.value.trim()
  if (!name) {
    toast.warning('请输入标签名称')
    return
  }
  formLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createTag(name)
      toast.success('标签已创建')
    } else {
      await updateTag(editingTag.value!.id, name)
      toast.success('已更新')
    }
    dialogVisible.value = false
    formName.value = ''
    fetchTags()
  } catch (err: unknown) {
    const code = (err as { code?: number })?.code
    if (code === 43202) toast.warning('标签名已存在')
    else toast.error('操作失败')
  } finally {
    formLoading.value = false
  }
}
</script>

<template>
  <div class="tags-page">
    <div class="main-card">
      <!-- Header -->
      <div class="card-header">
        <div class="header-left">
          <p class="stats-text">共 {{ tags.length }} 个标签</p>
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
            <input v-model="keyword" class="search-input" placeholder="搜索标签" />
            <button v-if="keyword" class="search-clear" @click="keyword = ''">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path
                  d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
                />
              </svg>
            </button>
          </div>
          <button class="icon-btn" title="刷新" @click="fetchTags">
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
            新建标签
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
            <tr v-if="filteredTags.length === 0 && !loading">
              <td colspan="5" class="empty-cell">
                <div class="empty-state">
                  <svg viewBox="0 0 24 24" fill="currentColor" class="empty-icon">
                    <path
                      d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"
                    />
                  </svg>
                  <span>{{ keyword ? '没有匹配的标签' : '还没有标签，点击右上角新建' }}</span>
                </div>
              </td>
            </tr>
            <tr
              v-for="row in filteredTags"
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
        <h3 class="dialog-title">{{ dialogMode === 'create' ? '新建标签' : '编辑标签' }}</h3>
        <input
          ref="dialogInputRef"
          v-model="formName"
          class="dialog-input"
          placeholder="请输入标签名称"
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
.tags-page {
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
  background: rgba(var(--admin-accent-rgb), 0.05);
  border-bottom: 1px solid rgba(var(--admin-accent-rgb), 0.15);
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
  background: rgba(var(--admin-accent-rgb), 0.04);
}

.count-badge {
  font-size: 12px;
  font-weight: 500;
  color: var(--admin-accent-dark);
  background: rgba(var(--admin-accent-rgb), 0.07);
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
