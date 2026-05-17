<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  categories: { name: string; count: number }[]
  tags: string[]
  totalCount: number
}>()

const search = defineModel<string>('search', { default: '' })
const category = defineModel<string | null>('category', { default: null })
const selectedTags = defineModel<string[]>('tags', { default: () => [] })

function toggleCategory(name: string) {
  category.value = category.value === name ? null : name
}

function toggleTag(name: string) {
  const current = selectedTags.value
  if (current.includes(name)) {
    selectedTags.value = current.filter((t) => t !== name)
  } else {
    selectedTags.value = [...current, name]
  }
}

const hasFilters = computed(
  () => search.value || category.value || selectedTags.value.length > 0,
)

function clearAll() {
  search.value = ''
  category.value = null
  selectedTags.value = []
}
</script>

<template>
  <aside class="filter-panel">
    <!-- Search -->
    <div class="panel-widget">
      <div class="search-box">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
          <circle cx="11" cy="11" r="7" /><line x1="20" y1="20" x2="15.5" y2="15.5" />
        </svg>
        <input
          v-model="search"
          class="search-input"
          placeholder="搜索文章..."
          type="search"
        />
      </div>
    </div>

    <!-- Categories -->
    <div class="panel-widget">
      <h3 class="panel-title">分类</h3>
      <ul class="category-list">
        <li>
          <button
            class="category-item"
            :class="{ 'category-item--active': category === null }"
            @click="category = null"
          >
            <span class="category-dot"></span>
            <span class="category-name">全部</span>
            <span class="category-count">{{ totalCount }}</span>
          </button>
        </li>
        <li v-for="cat in categories" :key="cat.name">
          <button
            class="category-item"
            :class="{ 'category-item--active': category === cat.name }"
            @click="toggleCategory(cat.name)"
          >
            <span class="category-dot"></span>
            <span class="category-name">{{ cat.name }}</span>
            <span class="category-count">{{ cat.count }}</span>
          </button>
        </li>
      </ul>
    </div>

    <!-- Tags -->
    <div class="panel-widget">
      <h3 class="panel-title">标签</h3>
      <div class="tag-cloud">
        <button
          v-for="tag in tags"
          :key="tag"
          class="tag-chip"
          :class="{ 'tag-chip--active': selectedTags.includes(tag) }"
          @click="toggleTag(tag)"
        >
          {{ tag }}
        </button>
      </div>
    </div>

    <!-- Clear -->
    <button v-if="hasFilters" class="clear-btn" @click="clearAll">清除所有过滤</button>
  </aside>
</template>

<style scoped>
.filter-panel {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: calc(var(--spacing-header-height) + 24px);
  align-self: flex-start;
}

.panel-widget {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: 18px 20px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);
}

.panel-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border);
}

/* Search */
.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-box svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--color-text-faint);
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: var(--font-sans);
}

.search-input::placeholder {
  color: var(--color-text-faint);
}

/* Categories */
.category-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 7px 8px;
  border-radius: 8px;
  font-size: 14px;
  font-family: var(--font-sans);
  color: var(--color-text-secondary);
  transition: background var(--transition-base), color var(--transition-base);
  cursor: pointer;
  text-align: left;
}

.category-item:hover {
  background: var(--color-bg-hover);
}

.category-item--active {
  background: rgba(74, 141, 183, 0.08);
  color: var(--color-accent);
}

.category-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-border);
  flex-shrink: 0;
  transition: background var(--transition-base);
}

.category-item--active .category-dot {
  background: var(--color-accent);
}

.category-name {
  flex: 1;
}

.category-count {
  font-size: 12px;
  color: var(--color-text-faint);
  background: var(--color-bg-hover);
  padding: 1px 7px;
  border-radius: 10px;
  transition: background var(--transition-base), color var(--transition-base);
}

.category-item--active .category-count {
  background: rgba(74, 141, 183, 0.12);
  color: var(--color-accent);
}

/* Tags */
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: var(--radius-tag);
  border: 1px solid var(--color-border);
  background: var(--color-bg-hover);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-base);
  font-family: var(--font-sans);
}

.tag-chip:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.tag-chip--active {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: #fff;
}

/* Clear button */
.clear-btn {
  font-size: 13px;
  color: var(--color-text-faint);
  text-align: center;
  padding: 8px;
  border-radius: 8px;
  border: 1px dashed var(--color-border);
  cursor: pointer;
  transition: all var(--transition-base);
  font-family: var(--font-sans);
}

.clear-btn:hover {
  border-color: #e57373;
  color: #e57373;
}

@media (max-width: 900px) {
  .filter-panel {
    width: 100%;
    position: static;
  }
}
</style>
