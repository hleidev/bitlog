<script setup lang="ts">
import { ref, computed, reactive, onMounted, nextTick, watch } from 'vue'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import { formatDateTime } from '@/utils/format'
import BaseModal from '@/components/common/BaseModal.vue'
import AdminIcon from '@/admin/components/AdminIcon.vue'
import AdminListHeader from '@/admin/components/AdminListHeader.vue'
import AdminSelectionBar from '@/admin/components/AdminSelectionBar.vue'
import { getTags, createTag, updateTag, deleteTags, type Tag } from '@/api/admin/tag'

const toast = useToast()
const confirm = useConfirm()

/** 本页量词，批量条与操作提示共用 */
const UNIT = '个'

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

// 标签没有状态维度，只留一个「全部」占位，让六个列表页的头部形态一致
const activeTab = ref('all')
const tabs = computed(() => [{ key: 'all', label: '全部', count: tags.value.length }])

function handleReset() {
  keyword.value = ''
  fetchTags()
}

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
  if (formLoading.value) return
  formError.value = ''
  const name = formName.value.trim()
  if (!name) {
    formError.value = '请输入标签名称'
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
    formError.value = code === 43202 ? '标签名已存在，请换一个名称。' : '保存失败，请重试。'
  } finally {
    formLoading.value = false
  }
}
</script>

<template>
  <div class="tags-page">
    <div class="main-card">
      <AdminListHeader
        v-model:active-tab="activeTab"
        v-model:keyword="keyword"
        :tabs="tabs"
        search-placeholder="搜索标签"
        action-label="新建标签"
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
            <tr v-if="filteredTags.length === 0 && !loading">
              <td colspan="5" class="empty-cell">
                <div class="empty-state">
                  <AdminIcon name="tag" class="empty-icon" />
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
    :aria-label="dialogMode === 'create' ? '新建标签' : '编辑标签'"
    @close="closeDialog"
  >
    <div class="dialog-box admin-form-dialog">
      <h3 class="dialog-title">{{ dialogMode === 'create' ? '新建标签' : '编辑标签' }}</h3>
      <input
        ref="dialogInputRef"
        v-model="formName"
        class="dialog-input"
        aria-label="标签名称"
        :aria-invalid="!!formError"
        :disabled="formLoading"
        placeholder="请输入标签名称"
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

.tags-page {
  display: flex;
  flex-direction: column;
}

.ghost-btn--danger {
  color: var(--admin-danger);
  border-color: var(--admin-danger-border);
}

/* 头部、批量条、列宽、空状态图标全部走 admin/styles/variables.css 与共享组件 */
</style>
