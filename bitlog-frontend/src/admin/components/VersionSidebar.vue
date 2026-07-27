<script setup lang="ts">
/**
 * VersionSidebar — 写作页的历史版本侧栏(含批量删除管理模式)
 *
 * 自治组件:选择/全选/删除确认/调用删除 API 都在内部完成,
 * 删除成功后 emit('reload') 由父级刷新版本列表。
 */
import { ref, computed } from 'vue'
import { useToast } from '@/admin/composables/useToast'
import { useConfirm } from '@/admin/composables/useConfirm'
import { deleteArticleVersions, type ArticleVersionVO } from '@/api/admin/article'
import { ApiError } from '@/utils/request'

const props = defineProps<{
  open: boolean
  articleId: number | null
  versions: ArticleVersionVO[]
  publishedVersionId: number | null
}>()

const emit = defineEmits<{ reload: [] }>()

const toast = useToast()
const confirm = useConfirm()

const manageMode = ref(false)
const selectedIds = ref<number[]>([])
const deleting = ref(false)

const deletableIds = computed(() =>
  props.versions.filter((v) => !v.latest && v.id !== props.publishedVersionId).map((v) => v.id),
)

const allDeletableSelected = computed(
  () =>
    deletableIds.value.length > 0 &&
    deletableIds.value.every((id) => selectedIds.value.includes(id)),
)

function shortTime(d: string) {
  return new Date(d)
    .toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
    .replace(/\//g, '-')
}

function toggleSelect(id: number) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

function toggleSelectAll() {
  if (allDeletableSelected.value) selectedIds.value = []
  else selectedIds.value = [...deletableIds.value]
}

function exitManage() {
  manageMode.value = false
  selectedIds.value = []
}

async function handleDelete() {
  if (!selectedIds.value.length || props.articleId === null) return
  const count = selectedIds.value.length
  try {
    await confirm(`确定删除选中的 ${count} 个版本？此操作不可恢复。`, '删除版本', {
      confirmText: '删除',
      cancelText: '取消',
      danger: true,
    })
  } catch {
    return
  }
  deleting.value = true
  try {
    await deleteArticleVersions(props.articleId, [...selectedIds.value])
    exitManage()
    toast.success(`已删除 ${count} 个版本`)
    emit('reload')
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : '删除版本失败')
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="meta-sidebar" :class="{ 'meta-sidebar--closed': !open }">
    <div class="sidebar-scroll">
      <div class="sidebar-section">
        <div class="version-header-row">
          <span class="section-label">历史版本</span>
          <button
            class="version-manage-toggle"
            :class="{ 'version-manage-toggle--cancel': manageMode }"
            @click="manageMode ? exitManage() : (manageMode = true)"
          >
            {{ manageMode ? '取消' : '管理' }}
          </button>
        </div>

        <div class="version-list">
          <div v-if="manageMode" class="version-select-all">
            <label class="checkbox-label">
              <input
                type="checkbox"
                :checked="allDeletableSelected"
                :disabled="deletableIds.length === 0"
                @change="toggleSelectAll"
              />
              全选可删除
            </label>
          </div>

          <div
            v-for="v in versions"
            :key="v.id"
            class="version-item"
            :class="{
              'version-item--current': v.latest,
              'version-item--manage': manageMode,
              'version-item--undeletable': manageMode && (v.latest || v.id === publishedVersionId),
            }"
          >
            <input
              v-if="manageMode"
              type="checkbox"
              class="version-checkbox"
              :checked="selectedIds.includes(v.id)"
              :disabled="v.latest || v.id === publishedVersionId"
              @change="() => toggleSelect(v.id)"
            />

            <div class="version-info">
              <div class="version-label-row">
                <span class="version-label">版本 {{ v.version }}</span>
                <span v-if="v.latest" class="version-tag version-tag--current">当前</span>
                <span
                  v-else-if="v.id === publishedVersionId"
                  class="version-tag version-tag--published"
                  >已发布</span
                >
              </div>
              <span class="version-time">{{ shortTime(v.createTime) }}</span>
            </div>

            <template v-if="!manageMode">
              <span v-if="v.latest" class="current-dot" />
            </template>
          </div>

          <div v-if="manageMode" class="version-manage-footer">
            <span class="version-manage-count">
              已选 {{ selectedIds.length }} / {{ deletableIds.length }}
            </span>
            <button
              class="version-delete-btn"
              :disabled="selectedIds.length === 0 || deleting"
              @click="handleDelete"
            >
              {{ deleting ? '删除中…' : '删除' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.meta-sidebar {
  width: 260px;
  flex-shrink: 0;
  border-left: 1px solid var(--admin-border-soft);
  background: var(--admin-surface-soft);
  transition:
    width 0.25s ease,
    opacity 0.2s ease;
  overflow: hidden;
}
.meta-sidebar--closed {
  width: 0;
  opacity: 0;
}

.sidebar-scroll {
  width: 100%;
  height: 100%;
  overflow-y: auto;
}

.sidebar-section {
  padding: 16px 20px;
  border-bottom: 1px solid var(--admin-border-soft);
}

.section-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--admin-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.6px;
  margin-bottom: 10px;
}

.version-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.version-header-row .section-label {
  margin-bottom: 0;
}

.version-manage-toggle {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 500;
  color: var(--admin-accent);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: var(--admin-radius);
  transition: color 0.15s;
}
.version-manage-toggle:hover {
  color: var(--admin-accent);
}
.version-manage-toggle--cancel {
  color: var(--admin-text-muted);
}
.version-manage-toggle--cancel:hover {
  color: var(--admin-text-muted);
}

.version-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
}

.version-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--admin-surface-2);
  gap: 8px;
  border-radius: 4px;
  transition: background 0.15s;
}
.version-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.version-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.version-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--admin-text-secondary);
}
.version-time {
  font-size: 11px;
  color: var(--admin-text-muted);
}

.current-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--admin-success);
  flex-shrink: 0;
}

.version-select-all {
  padding: 8px 0 8px;
  border-bottom: 1px solid var(--admin-border-soft);
  margin-bottom: 2px;
}

.version-item--manage {
  gap: 10px;
}
.version-item--undeletable {
  opacity: 0.45;
}

.version-checkbox {
  flex-shrink: 0;
}

.version-label-row {
  display: flex;
  align-items: center;
  gap: 5px;
}

.version-tag {
  font-size: 10px;
  font-weight: 500;
  line-height: 1;
  padding: 2px 5px;
  border-radius: var(--admin-radius);
}
.version-tag--current {
  color: var(--admin-success);
  background: rgba(var(--admin-success-rgb), 0.15);
}
.version-tag--published {
  color: var(--admin-accent-dark);
  background: rgba(var(--admin-accent-rgb), 0.12);
}

.version-manage-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0 2px;
  border-top: 1px solid var(--admin-border-soft);
  margin-top: 6px;
}
.version-manage-count {
  font-size: 12px;
  color: var(--admin-text-muted);
}

.version-delete-btn {
  font-size: 12px;
  font-weight: 500;
  color: var(--admin-text-on-accent);
  background: var(--admin-danger-bg-strong);
  border: none;
  border-radius: 4px;
  padding: 4px 14px;
  cursor: pointer;
  transition: background 0.15s;
}
.version-delete-btn:hover:not(:disabled) {
  background: var(--admin-danger-strong);
}
.version-delete-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--admin-text-muted);
  cursor: pointer;
}
.checkbox-label input[type='checkbox'] {
  width: 14px;
  height: 14px;
  cursor: pointer;
}

@media (max-width: 768px) {
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
