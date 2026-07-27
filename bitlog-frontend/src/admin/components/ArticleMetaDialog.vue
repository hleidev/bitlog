<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useToast } from '@/admin/composables/useToast'
import { getCategories, getOrCreateCategory, type Category } from '@/api/admin/category'
import { getTags, getOrCreateTag, type Tag } from '@/api/admin/tag'

interface Props {
  visible: boolean
  summary?: string | null
  category?: { id: number; name: string } | null
  tags?: { id: number; name: string }[]
  saving?: boolean
  title?: string
  confirmText?: string
}

const props = withDefaults(defineProps<Props>(), {
  summary: null,
  category: null,
  tags: () => [],
  saving: false,
  title: '编辑文章信息',
  confirmText: '保存',
})

const emit = defineEmits<{
  'update:visible': [v: boolean]
  save: [
    data: {
      summary: string
      categoryId: number | null
      tagIds: number[]
      category: { id: number; name: string } | null
      tags: { id: number; name: string }[]
    },
  ]
}>()

const toast = useToast()

// ── Internal form state ────────────────────────────────────────────────────────
const internalSummary = ref('')
const internalCatId = ref<number | null>(null)

const categoryOpts = ref<Category[]>([])
const tagOpts = ref<Tag[]>([])

// Category combobox
const catInput = ref('')
const catOpen = ref(false)
const selectedCat = ref<{ id: number; name: string } | null>(null)
const filteredCats = computed(() => {
  const q = catInput.value.trim().toLowerCase()
  if (!q) return categoryOpts.value
  return categoryOpts.value.filter((c) => c.name.toLowerCase().includes(q))
})

// Tag combobox
const tagInput = ref('')
const tagOpen = ref(false)

function closeTagDropdown() {
  tagOpen.value = false
  tagInput.value = ''
}
const selectedTags = ref<{ id: number; name: string }[]>([])
const filteredTags = computed(() => {
  const ids = new Set(selectedTags.value.map((t) => t.id))
  const base = tagOpts.value.filter((t) => !ids.has(t.id))
  const q = tagInput.value.trim().toLowerCase()
  if (!q) return base
  return base.filter((t) => t.name.toLowerCase().includes(q))
})

// ── Seed state when dialog opens ───────────────────────────────────────────────
watch(
  () => props.visible,
  async (open) => {
    if (!open) return
    internalSummary.value = props.summary ?? ''
    internalCatId.value = props.category?.id ?? null
    selectedCat.value = props.category ? { ...props.category } : null
    catInput.value = props.category?.name ?? ''
    selectedTags.value = [...(props.tags ?? [])]
    tagInput.value = ''
    catOpen.value = false
    tagOpen.value = false

    try {
      const [cats, tags] = await Promise.all([getCategories(), getTags()])
      categoryOpts.value = cats
      tagOpts.value = tags
    } catch {
      // non-critical
    }
  },
)

// ── Category handlers ──────────────────────────────────────────────────────────
function selectCat(cat: { id: number; name: string }) {
  selectedCat.value = cat
  catInput.value = cat.name
  internalCatId.value = cat.id
  catOpen.value = false
}

function clearCat() {
  selectedCat.value = null
  catInput.value = ''
  internalCatId.value = null
}

function onCatInput() {
  if (selectedCat.value && catInput.value !== selectedCat.value.name) {
    selectedCat.value = null
    internalCatId.value = null
  }
  catOpen.value = true
}

function onCatEnter() {
  const q = catInput.value.trim()
  if (!q) return
  const first = filteredCats.value[0]
  if (first && first.name.toLowerCase() === q.toLowerCase()) {
    selectCat(first)
    return
  }
  if (filteredCats.value.length === 1) {
    selectCat(filteredCats.value[0])
    return
  }
  createAndSelectCat(q)
}

async function createAndSelectCat(name: string) {
  try {
    const id = await getOrCreateCategory(name)
    selectCat({ id, name })
    if (!categoryOpts.value.find((c) => c.id === id)) {
      categoryOpts.value = [...categoryOpts.value, { id, name, articleCount: 0, createTime: '' }]
    }
  } catch {
    toast.error('创建分类失败')
  }
}

function closeCatDropdown() {
  if (!selectedCat.value) catInput.value = ''
  catOpen.value = false
}

// ── Tag handlers ───────────────────────────────────────────────────────────────
function addTag(tag: { id: number; name: string }) {
  if (selectedTags.value.find((t) => t.id === tag.id)) return
  selectedTags.value = [...selectedTags.value, tag]
  tagInput.value = ''
  tagOpen.value = false
}

async function createAndAddTag(name: string) {
  try {
    const id = await getOrCreateTag(name)
    if (!selectedTags.value.find((t) => t.id === id)) {
      selectedTags.value = [...selectedTags.value, { id, name }]
    }
    if (!tagOpts.value.find((t) => t.id === id)) {
      tagOpts.value = [
        ...tagOpts.value,
        { id, name, articleCount: 0, createTime: '', updateTime: '' },
      ]
    }
    tagInput.value = ''
    tagOpen.value = false
  } catch {
    toast.error('创建标签失败')
  }
}

function removeTag(id: number) {
  selectedTags.value = selectedTags.value.filter((t) => t.id !== id)
}

function onTagEnter() {
  const q = tagInput.value.trim()
  if (!q) return
  const exact = tagOpts.value.find((t) => t.name.toLowerCase() === q.toLowerCase())
  if (exact) addTag(exact)
  else createAndAddTag(q)
}

// ── Save ───────────────────────────────────────────────────────────────────────
function handleSave() {
  emit('save', {
    summary: internalSummary.value,
    categoryId: internalCatId.value,
    tagIds: selectedTags.value.map((t) => t.id),
    category: selectedCat.value,
    tags: [...selectedTags.value],
  })
}

// ── Expose for parent AI integration ──────────────────────────────────────────
function setSummary(v: string) {
  internalSummary.value = v
}
function setCategory(cat: { id: number; name: string }) {
  selectCat(cat)
}

// selectedTags 暴露给父组件，用于把已应用的 AI 建议标记为 --applied
defineExpose({ setSummary, setCategory, addTag, createAndAddTag, selectedTags })
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="visible" class="dialog-overlay" @click.self="emit('update:visible', false)">
        <div class="dialog-panel">
          <div class="dialog-header">
            <div class="dialog-title-row">
              <span class="dialog-title">{{ title }}</span>
              <slot name="header-extra" />
            </div>
            <button class="dialog-x" @click="emit('update:visible', false)">
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M18 6L6 18M6 6l12 12" />
              </svg>
            </button>
          </div>

          <div class="dialog-body">
            <div class="publish-form">
              <!-- Summary -->
              <div class="pf-item">
                <div class="pf-label">摘要 <span class="pf-optional">可选</span></div>
                <textarea
                  v-model="internalSummary"
                  class="pf-textarea"
                  placeholder="留空则自动截取正文前 200 字..."
                  maxlength="512"
                  rows="3"
                />
                <div class="pf-char-count">{{ internalSummary.length }} / 512</div>
                <slot name="summary-extra" />
              </div>

              <!-- Category -->
              <div class="pf-item">
                <div class="pf-label">分类 <span class="pf-required">必填</span></div>
                <div v-click-outside="closeCatDropdown" class="pf-combo">
                  <div class="pf-combo-field" :class="{ 'pf-combo-field--focused': catOpen }">
                    <input
                      v-model="catInput"
                      class="pf-combo-input"
                      placeholder="搜索或输入新分类..."
                      @input="onCatInput"
                      @focus="catOpen = true"
                      @keydown.enter.prevent="onCatEnter"
                      @keydown.escape="closeCatDropdown"
                      @keydown.tab="closeCatDropdown"
                    />
                    <button
                      v-if="selectedCat"
                      type="button"
                      class="pf-combo-clear"
                      tabindex="-1"
                      @click="clearCat"
                    >
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
                  <div v-if="catOpen" class="pf-combo-dropdown">
                    <button
                      v-for="cat in filteredCats"
                      :key="cat.id"
                      type="button"
                      class="pf-combo-opt"
                      :class="{ 'pf-combo-opt--active': selectedCat?.id === cat.id }"
                      @mousedown.prevent="selectCat(cat)"
                    >
                      {{ cat.name }}
                    </button>
                    <button
                      v-if="
                        catInput.trim() &&
                        !filteredCats.find(
                          (c) => c.name.toLowerCase() === catInput.trim().toLowerCase(),
                        )
                      "
                      type="button"
                      class="pf-combo-opt"
                      @mousedown.prevent="createAndSelectCat(catInput.trim())"
                    >
                      {{ catInput.trim() }}
                    </button>
                    <div v-if="!filteredCats.length && !catInput.trim()" class="pf-combo-empty">
                      暂无分类，输入名称即可创建
                    </div>
                  </div>
                </div>
                <slot name="category-extra" />
              </div>

              <!-- Tags -->
              <div class="pf-item">
                <div class="pf-label">标签 <span class="pf-optional">可选</span></div>
                <div v-if="selectedTags.length" class="pf-selected-chips">
                  <span v-for="tag in selectedTags" :key="tag.id" class="pf-sel-chip">
                    {{ tag.name }}
                    <button type="button" class="pf-sel-chip-x" @click="removeTag(tag.id)">
                      <svg
                        viewBox="0 0 24 24"
                        width="10"
                        height="10"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2.5"
                      >
                        <path d="M18 6L6 18M6 6l12 12" />
                      </svg>
                    </button>
                  </span>
                </div>
                <div
                  v-click-outside="
                    () => {
                      tagOpen = false
                      tagInput = ''
                    }
                  "
                  class="pf-combo"
                >
                  <div class="pf-combo-field" :class="{ 'pf-combo-field--focused': tagOpen }">
                    <input
                      v-model="tagInput"
                      class="pf-combo-input"
                      placeholder="搜索或输入新标签..."
                      @input="tagOpen = true"
                      @focus="tagOpen = true"
                      @keydown.enter.prevent="onTagEnter"
                      @keydown.escape="closeTagDropdown"
                      @keydown.tab="tagOpen = false"
                    />
                  </div>
                  <div v-if="tagOpen" class="pf-combo-dropdown">
                    <button
                      v-for="tag in filteredTags"
                      :key="tag.id"
                      type="button"
                      class="pf-combo-opt"
                      @mousedown.prevent="addTag(tag)"
                    >
                      {{ tag.name }}
                    </button>
                    <button
                      v-if="
                        tagInput.trim() &&
                        !tagOpts.find((t) => t.name.toLowerCase() === tagInput.trim().toLowerCase())
                      "
                      type="button"
                      class="pf-combo-opt"
                      @mousedown.prevent="createAndAddTag(tagInput.trim())"
                    >
                      {{ tagInput.trim() }}
                    </button>
                    <div v-if="!filteredTags.length && !tagInput.trim()" class="pf-combo-empty">
                      暂无更多标签，输入名称即可创建
                    </div>
                  </div>
                </div>
                <slot name="tags-extra" />
              </div>
            </div>
          </div>

          <div class="dialog-footer">
            <button class="btn btn--cancel" @click="emit('update:visible', false)">取消</button>
            <button class="btn btn--primary" :disabled="saving" @click="handleSave">
              {{ saving ? '保存中…' : confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ── Dialog chrome ───────────────────────────────────────────────────────────── */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: var(--admin-overlay);
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-panel {
  background: var(--admin-surface);
  border: 1px solid var(--admin-border);
  border-radius: 4px;
  width: 540px;
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}

.dialog-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.dialog-title {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--admin-text-primary);
  flex-shrink: 0;
  line-height: 1.2;
  margin: 0;
}

.dialog-x {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--admin-text-muted);
  border-radius: 4px;
  transition:
    background 0.15s,
    color 0.15s;
}
.dialog-x:hover {
  background: var(--admin-surface-hover);
  color: var(--admin-text-primary);
}

.dialog-body {
  padding: 20px 24px 16px;
  overflow: visible;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 24px 20px;
  flex-shrink: 0;
}

/* ── Dialog transition ───────────────────────────────────────────────────────── */
.dialog-fade-enter-active {
  transition: opacity 0.2s ease;
}
.dialog-fade-leave-active {
  transition: opacity 0.15s ease;
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
.dialog-fade-enter-active .dialog-panel {
  transition: transform 0.2s ease;
}
.dialog-fade-leave-active .dialog-panel {
  transition: transform 0.15s ease;
}
.dialog-fade-enter-from .dialog-panel {
  transform: scale(0.97);
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
.btn--cancel {
  background: transparent;
  color: var(--admin-text-secondary);
  border-color: var(--admin-border-strong);
}
.btn--cancel:hover:not(:disabled) {
  background: var(--admin-surface-hover);
  border-color: var(--admin-border-strong);
}

/* ── Form ────────────────────────────────────────────────────────────────────── */
.publish-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.pf-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.pf-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--admin-text-primary);
}
.pf-optional {
  font-size: 11px;
  font-weight: 400;
  color: var(--admin-text-muted);
  background: var(--admin-surface-2);
  padding: 1px 6px;
  border-radius: var(--admin-radius);
}
.pf-required {
  font-size: 11px;
  font-weight: 500;
  color: var(--admin-danger-on-soft);
  background: var(--admin-error-bg);
  padding: 1px 6px;
  border-radius: var(--admin-radius);
}

.pf-textarea {
  display: block;
  width: 100%;
  height: auto;
  padding: 8px 10px;
  font-size: 13px;
  font-family: inherit;
  border: 1px solid var(--admin-border-strong);
  border-radius: 4px;
  background: var(--admin-surface-input);
  color: var(--admin-text-primary);
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
  resize: none;
  line-height: 1.6;
}
.pf-textarea:focus {
  border-color: var(--admin-accent);
}
.pf-char-count {
  font-size: 11px;
  color: var(--admin-text-muted);
  text-align: right;
  margin-top: 2px;
}

/* ── Combobox ─────────────────────────────────────────────────────────────────── */
.pf-combo {
  position: relative;
}
.pf-combo-field {
  display: flex;
  align-items: center;
  border: 1px solid var(--admin-border-strong);
  border-radius: 4px;
  background: var(--admin-surface-input);
  transition: border-color 0.15s;
  padding: 0 6px 0 10px;
}
.pf-combo-field--focused {
  border-color: var(--admin-accent);
}
.pf-combo-input {
  flex: 1;
  height: 34px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  font-family: inherit;
  color: var(--admin-text-primary);
}
.pf-combo-input::placeholder {
  color: var(--admin-text-muted);
}
.pf-combo-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--admin-text-muted);
  border-radius: var(--admin-radius);
  transition:
    color 0.15s,
    background 0.15s;
}
.pf-combo-clear:hover {
  color: var(--admin-text-primary);
  background: var(--admin-surface-2);
}
.pf-combo-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--admin-surface-input);
  border: 1px solid var(--admin-border);
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  z-index: 200;
  max-height: 176px;
  overflow-y: auto;
  padding: 4px 0;
}
.pf-combo-opt {
  display: block;
  width: 100%;
  text-align: left;
  padding: 7px 12px;
  font-size: 13px;
  color: var(--admin-text-primary);
  background: transparent;
  border: none;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.1s;
}
.pf-combo-opt:hover {
  background: var(--admin-surface-2);
}
.pf-combo-opt--active {
  color: var(--admin-accent);
  font-weight: 500;
}
.pf-combo-empty {
  padding: 8px 12px;
  font-size: 12px;
  color: var(--admin-text-muted);
  font-style: italic;
}

/* ── Selected tag chips ──────────────────────────────────────────────────────── */
.pf-selected-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.pf-sel-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 6px 3px 10px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 4px;
  background: var(--admin-accent-bg-soft);
  color: var(--admin-accent-dark);
  border: 1px solid var(--admin-accent-border);
}
.pf-sel-chip-x {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--admin-accent-light);
  border-radius: var(--admin-radius);
  padding: 0;
  transition:
    color 0.15s,
    background 0.15s;
}
.pf-sel-chip-x:hover {
  color: var(--admin-accent-dark);
  background: var(--admin-accent-bg-strong);
}
</style>
