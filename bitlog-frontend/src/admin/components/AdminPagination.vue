<script setup lang="ts">
import { PAGE_SIZE_OPTIONS } from '@/constants/pagination'

defineProps<{
  /** 仅用于判断是否渲染分页条；总数由 tab 上的计数呈现，这里不再重复显示 */
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
  pageNumbers: (number | '…')[]
  pageSizeOptions?: number[]
}>()

const emit = defineEmits<{
  (e: 'go', page: number): void
  (e: 'size', size: number): void
}>()

function onSizeChange(event: Event) {
  emit('size', Number((event.target as HTMLSelectElement).value))
}
</script>

<template>
  <nav v-if="total > 0" class="pagination-bar" aria-label="列表分页">
    <div class="pagination-controls">
      <button
        class="page-btn"
        aria-label="上一页"
        :disabled="pageNum <= 1"
        @click="emit('go', pageNum - 1)"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <template v-for="(p, i) in pageNumbers" :key="i">
        <span v-if="p === '…'" class="page-ellipsis">…</span>
        <button
          v-else
          class="page-btn page-btn--num"
          :class="{ 'page-btn--active': p === pageNum }"
          :aria-label="`第 ${p} 页`"
          :aria-current="p === pageNum ? 'page' : undefined"
          @click="emit('go', p as number)"
        >
          {{ p }}
        </button>
      </template>
      <span class="page-position" aria-live="polite">{{ pageNum }} / {{ totalPages }}</span>
      <button
        class="page-btn"
        aria-label="下一页"
        :disabled="pageNum >= totalPages"
        @click="emit('go', pageNum + 1)"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>
    </div>
    <select class="page-size-select" aria-label="每页条数" :value="pageSize" @change="onSizeChange">
      <option v-for="size in pageSizeOptions ?? PAGE_SIZE_OPTIONS" :key="size" :value="size">
        {{ size }} / 页
      </option>
    </select>
  </nav>
</template>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--admin-sidebar-border);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-left: auto;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: var(--admin-radius);
  background: var(--admin-surface-input);
  font-size: 12.5px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  cursor: pointer;
  transition:
    background 0.12s,
    border-color 0.12s;
}

.page-btn svg {
  width: 13px;
  height: 13px;
}

.page-btn:hover:not(:disabled):not(.page-btn--active) {
  background: var(--admin-sidebar-hover);
}

.page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.page-btn--num {
  min-width: 32px;
}

.page-btn--active {
  background: var(--admin-accent);
  border-color: var(--admin-accent);
  color: var(--admin-text-on-accent);
  font-weight: 600;
}

.page-btn--active:hover {
  background: var(--admin-accent);
}

.page-ellipsis {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  font-size: 12.5px;
  color: var(--admin-sidebar-text-muted);
}

.page-position {
  display: none;
}

.page-size-select {
  height: 32px;
  padding: 0 6px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: var(--admin-radius);
  background: var(--admin-surface-input);
  font-size: 12px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  outline: none;
  cursor: pointer;
  margin-left: 8px;
}

@media (max-width: 768px) {
  .pagination-bar {
    padding: 10px 12px;
    flex-wrap: wrap;
    gap: 6px;
  }
  .pagination-controls {
    flex-wrap: wrap;
    margin-left: 0;
  }
  .page-btn {
    min-width: 36px;
    height: 36px;
  }
  .page-size-select {
    height: 36px;
    margin-left: 0;
  }
}

@media (max-width: 480px) {
  .page-btn--num,
  .page-ellipsis {
    display: none;
  }
  .page-position {
    display: block;
    min-width: 64px;
    text-align: center;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    color: var(--admin-text-secondary);
  }
  .page-size-select {
    margin-left: auto;
  }
}
</style>
