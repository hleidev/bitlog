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

const toast   = useToast()
const confirm = useConfirm()

// ── Data ──────────────────────────────────────────────────────────────────────
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

// ── Selection ─────────────────────────────────────────────────────────────────
const selectedIds = reactive(new Set<number>())

const allSelected = computed(
  () => filteredCategories.value.length > 0
    && filteredCategories.value.every(c => selectedIds.has(c.id)),
)

function onCardClick(cat: Category) {
  if (editingId.value === cat.id) return
  selectedIds.has(cat.id) ? selectedIds.delete(cat.id) : selectedIds.add(cat.id)
}

function toggleSelectAll() {
  if (allSelected.value) {
    filteredCategories.value.forEach(c => selectedIds.delete(c.id))
  } else {
    filteredCategories.value.forEach(c => selectedIds.add(c.id))
  }
}

function clearSelection() {
  selectedIds.clear()
}

watch(filteredCategories, cats => {
  const validIds = new Set(cats.map(c => c.id))
  for (const id of selectedIds) {
    if (!validIds.has(id)) selectedIds.delete(id)
  }
})

// ── Delete (batch only) ───────────────────────────────────────────────────────
const batchLoading = ref(false)

async function handleDelete() {
  const ids = Array.from(selectedIds)
  try {
    await confirm(
      ids.length === 1
        ? `确认删除「${categories.value.find(c => c.id === ids[0])?.name}」？`
        : `确认删除选中的 ${ids.length} 个分类？`,
      '删除分类',
      { confirmText: '删除', danger: true },
    )
  } catch { return }

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

// ── Inline edit ───────────────────────────────────────────────────────────────
const editingId   = ref<number | null>(null)
const editingName = ref('')

function startEdit(cat: Category, e: MouseEvent) {
  e.stopPropagation()
  editingId.value   = cat.id
  editingName.value = cat.name
  nextTick(() => {
    const input = document.querySelector<HTMLInputElement>('.cat-edit-input')
    input?.focus()
    input?.select()
  })
}

function cancelEdit() {
  editingId.value   = null
  editingName.value = ''
}

async function saveEdit(e?: KeyboardEvent | MouseEvent) {
  e?.stopPropagation?.()
  const name = editingName.value.trim()
  const cat  = categories.value.find(c => c.id === editingId.value)
  if (!cat) return
  if (!name) { toast.warning('请输入分类名称'); return }
  if (name === cat.name) { cancelEdit(); return }

  try {
    await updateCategory(cat.id, name)
    toast.success('已更新')
    cancelEdit()
    fetchCategories()
  } catch (err: unknown) {
    const code = (err as { code?: number })?.code
    if (code === 43102) toast.warning('分类名已存在')
    else toast.error('操作失败')
  }
}

// ── Create dialog ─────────────────────────────────────────────────────────────
const dialogVisible = ref(false)
const formName      = ref('')
const formLoading   = ref(false)
const dialogInputRef = ref<HTMLInputElement | null>(null)

watch(dialogVisible, val => {
  if (val) nextTick(() => dialogInputRef.value?.focus())
})

async function handleCreate() {
  const name = formName.value.trim()
  if (!name) { toast.warning('请输入分类名称'); return }
  formLoading.value = true
  try {
    await createCategory(name)
    toast.success('分类已创建')
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

    <!-- Header -->
    <div class="page-header">
      <div class="header-actions">
        <div class="search-wrap">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="7" /><path d="m21 21-4.35-4.35" />
          </svg>
          <input v-model="keyword" class="search-input" placeholder="搜索分类" />
          <button v-if="keyword" class="search-clear" @click="keyword = ''">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
            </svg>
          </button>
        </div>
        <button class="icon-btn" title="刷新" @click="fetchCategories">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z" />
          </svg>
        </button>
        <button class="primary-btn" @click="dialogVisible = true">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" /></svg>
          新建分类
        </button>
      </div>
    </div>

    <!-- Stats / Action bar — fixed height, no layout jump -->
    <div class="bar-area">
      <Transition name="bar-swap" mode="out-in">
        <div v-if="selectedIds.size > 0" key="bar" class="action-bar">
          <span class="action-bar-count">已选 {{ selectedIds.size }} 个</span>
          <button class="bar-btn" @click="toggleSelectAll">
            {{ allSelected ? '取消全选' : '全选' }}
          </button>
          <div class="bar-sep" />
          <button class="bar-btn bar-btn--danger" :disabled="batchLoading" @click="handleDelete">
            <span v-if="batchLoading" class="btn-spinner btn-spinner--dark" />
            删除
          </button>
          <button class="bar-btn" @click="clearSelection">取消</button>
        </div>
        <p v-else key="stats" class="stats-text">共 {{ categories.length }} 个分类</p>
      </Transition>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <svg class="spinner" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" stroke-dasharray="40" stroke-dashoffset="15" />
      </svg>
    </div>

    <!-- Empty -->
    <div v-else-if="filteredCategories.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" fill="currentColor" class="empty-icon">
        <path d="M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z" />
      </svg>
      <span>{{ keyword ? '没有匹配的分类' : '还没有分类，点击右上角新建' }}</span>
    </div>

    <!-- Grid -->
    <div v-else class="category-grid">
      <div
        v-for="cat in filteredCategories"
        :key="cat.id"
        class="cat-card"
        :class="{
          'cat-card--selected': selectedIds.has(cat.id) && editingId !== cat.id,
          'cat-card--editing': editingId === cat.id,
        }"
        @click="onCardClick(cat)"
      >
        <!-- Edit button (hover, non-editing mode) -->
        <button
          v-if="editingId !== cat.id"
          class="cat-edit-btn"
          title="编辑"
          @click.stop="startEdit(cat, $event)"
        >
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z" />
          </svg>
        </button>

        <!-- Normal view -->
        <template v-if="editingId !== cat.id">
          <h3 class="cat-name">{{ cat.name }}</h3>
          <div class="cat-meta">
            <span class="cat-count" :class="{ 'cat-count--zero': cat.articleCount === 0 }">
              {{ cat.articleCount }} 篇
            </span>
            <span class="cat-date">{{ cat.createTime }}</span>
          </div>
        </template>

        <!-- Inline edit view -->
        <template v-else>
          <input
            v-model="editingName"
            class="cat-edit-input"
            maxlength="30"
            @click.stop
            @keyup.enter="saveEdit"
            @keyup.escape="cancelEdit"
            @blur="saveEdit"
          />
        </template>
      </div>
    </div>

  </div>

  <!-- Create dialog -->
  <Transition name="dialog-fade">
    <div v-if="dialogVisible" class="dialog-overlay" @click.self="dialogVisible = false">
      <div class="dialog-box">
        <h3 class="dialog-title">新建分类</h3>
        <input
          ref="dialogInputRef"
          v-model="formName"
          class="dialog-input"
          placeholder="请输入分类名称"
          maxlength="30"
          @keyup.enter="handleCreate"
        />
        <p class="input-hint">{{ formName.length }} / 30</p>
        <div class="dialog-actions">
          <button class="dialog-btn dialog-btn--cancel" @click="dialogVisible = false">取消</button>
          <button class="dialog-btn dialog-btn--ok" :disabled="formLoading" @click="handleCreate">
            <span v-if="formLoading" class="btn-spinner" />
            创建
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
  gap: 16px;
}

/* ── Header ── */

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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
  left: 9px;
  width: 14px;
  height: 14px;
  color: var(--admin-sidebar-text-muted, #b0a89e);
  pointer-events: none;
}

.search-input {
  height: 34px;
  padding: 0 30px 0 30px;
  width: 180px;
  border: 1px solid var(--admin-sidebar-border, #e8e4de);
  border-radius: 4px;
  background: var(--admin-header-bg, #faf9f7);
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: #1a1610;
  outline: none;
  transition: border-color 0.15s;
}

.search-input:focus { border-color: var(--admin-accent, #b85c38); }
.search-input::placeholder { color: var(--admin-sidebar-text-muted, #b0a89e); }

.search-clear {
  position: absolute;
  right: 8px;
  display: flex;
  align-items: center;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--admin-sidebar-text-muted, #b0a89e);
  padding: 0;
  transition: color 0.15s;
}

.search-clear svg { width: 13px; height: 13px; }
.search-clear:hover { color: var(--admin-sidebar-text, #5a5248); }

/* ── Buttons ── */

.icon-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--admin-sidebar-border, #e8e4de);
  border-radius: 4px;
  background: var(--admin-header-bg, #faf9f7);
  color: var(--admin-sidebar-text, #5a5248);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.icon-btn svg { width: 16px; height: 16px; }
.icon-btn:hover { background: var(--admin-sidebar-hover, #ece9e4); border-color: #d4cfc9; }

.primary-btn {
  height: 34px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--admin-accent, #b85c38);
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.primary-btn svg { width: 16px; height: 16px; }
.primary-btn:hover { background: var(--admin-accent-dark, #924530); }

/* ── Bar area — fixed height, no layout jump ── */

.bar-area {
  height: 36px;
  position: relative;
}

.stats-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  font-size: 13px;
  color: var(--admin-sidebar-text-muted, #b0a89e);
  margin: 0;
}

.action-bar {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  background: rgba(184, 92, 56, 0.06);
  border: 1px solid rgba(184, 92, 56, 0.2);
  border-radius: 4px;
}

.action-bar-count {
  font-size: 13px;
  font-weight: 500;
  color: var(--admin-accent, #b85c38);
  font-family: var(--font-sans, 'Inter', sans-serif);
  margin-right: 2px;
}

.bar-btn {
  height: 26px;
  padding: 0 10px;
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  border-radius: 3px;
  cursor: pointer;
  border: 1px solid #d4cfc9;
  background: transparent;
  color: var(--admin-sidebar-text, #5a5248);
  display: flex;
  align-items: center;
  gap: 5px;
  transition: background 0.12s;
}

.bar-btn:hover:not(:disabled) { background: var(--admin-sidebar-hover, #ece9e4); }
.bar-btn--danger { color: #c04040; border-color: rgba(192, 64, 64, 0.3); }
.bar-btn--danger:hover:not(:disabled) { background: rgba(192, 64, 64, 0.07); }
.bar-btn:disabled { opacity: 0.55; cursor: not-allowed; }

.bar-sep { flex: 1; }

/* ── Loading / Empty ── */

.loading-state {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.spinner {
  width: 28px;
  height: 28px;
  color: var(--admin-sidebar-text-muted, #b0a89e);
  animation: spin 0.9s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--admin-sidebar-text-muted, #b0a89e);
}

.empty-icon { width: 36px; height: 36px; opacity: 0.35; }
.empty-state span { font-size: 13px; font-family: var(--font-sans, 'Inter', sans-serif); }

/* ── Card grid ── */

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.cat-card {
  position: relative;
  background: var(--admin-header-bg, #faf9f7);
  border: 1px solid var(--admin-sidebar-border, #e8e4de);
  border-radius: 4px;
  padding: 14px;
  cursor: pointer;
  user-select: none;
  transition: border-color 0.15s, background 0.15s;
}

.cat-card:hover { border-color: #d4cfc9; background: var(--admin-sidebar-hover, #ece9e4); }
.cat-card--selected { border-color: var(--admin-accent, #b85c38) !important; background: rgba(184, 92, 56, 0.04) !important; }
.cat-card--editing { cursor: default; border-color: var(--admin-accent, #b85c38); }

/* Edit button — top-right, hover only */
.cat-edit-btn {
  position: absolute;
  top: 9px;
  right: 9px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 3px;
  background: rgba(90, 82, 72, 0.08);
  color: var(--admin-sidebar-text, #5a5248);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, background 0.12s;
}

.cat-edit-btn svg { width: 12px; height: 12px; }
.cat-card:hover .cat-edit-btn { opacity: 1; }
.cat-edit-btn:hover { background: rgba(90, 82, 72, 0.15); }

/* Card content */
.cat-name {
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: 14px;
  font-weight: 500;
  color: #1a1610;
  margin: 0 0 12px;
  padding-right: 28px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cat-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.cat-count {
  font-size: 12px;
  font-weight: 500;
  color: var(--admin-accent, #b85c38);
  background: rgba(184, 92, 56, 0.07);
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
  text-decoration: none;
  transition: background 0.15s;
}

.cat-count--zero { color: var(--admin-sidebar-text-muted, #b0a89e); background: rgba(176, 168, 158, 0.1); }

.cat-date {
  font-size: 11px;
  color: var(--admin-sidebar-text-muted, #b0a89e);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── Inline edit ── */

.cat-edit-input {
  display: block;
  width: 100%;
  margin: 10px 0 10px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid var(--admin-accent, #b85c38);
  border-radius: 3px;
  background: #fff;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: #1a1610;
  outline: none;
  box-sizing: border-box;
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-box {
  background: #faf9f7;
  border: 1px solid var(--admin-sidebar-border, #e8e4de);
  border-radius: 4px;
  padding: 28px 28px 22px;
  width: 360px;
  max-width: calc(100vw - 40px);
}

.dialog-title {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 16px;
  font-weight: 600;
  color: #1a1610;
  margin: 0 0 16px;
}

.dialog-input {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--admin-sidebar-border, #e8e4de);
  border-radius: 4px;
  background: #fff;
  font-size: 13.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: #1a1610;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.dialog-input:focus { border-color: var(--admin-accent, #b85c38); }
.dialog-input::placeholder { color: var(--admin-sidebar-text-muted, #b0a89e); }

.input-hint {
  font-size: 11px;
  color: var(--admin-sidebar-text-muted, #b0a89e);
  text-align: right;
  margin: 4px 0 20px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.dialog-btn {
  height: 34px;
  padding: 0 18px;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background 0.15s, border-color 0.15s;
}

.dialog-btn--cancel {
  background: transparent;
  color: var(--admin-sidebar-text, #5a5248);
  border-color: #d4cfc9;
}

.dialog-btn--cancel:hover { background: var(--admin-sidebar-hover, #ece9e4); }

.dialog-btn--ok {
  background: var(--admin-accent, #b85c38);
  color: #fff;
  border-color: var(--admin-accent, #b85c38);
  font-weight: 500;
}

.dialog-btn--ok:hover:not(:disabled) { background: var(--admin-accent-dark, #924530); border-color: var(--admin-accent-dark, #924530); }
.dialog-btn--ok:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

.btn-spinner--dark {
  border-color: rgba(90, 82, 72, 0.3);
  border-top-color: var(--admin-sidebar-text, #5a5248);
}

/* ── Transitions ── */

.bar-swap-enter-active,
.bar-swap-leave-active {
  transition: opacity 0.14s ease;
}

.bar-swap-enter-from,
.bar-swap-leave-to {
  opacity: 0;
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.18s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
</style>
