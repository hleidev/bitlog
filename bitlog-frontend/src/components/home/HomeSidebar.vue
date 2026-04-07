<script setup lang="ts">
const categories = [
  { name: '前端开发', count: 24 },
  { name: '后端技术', count: 18 },
  { name: 'Java', count: 15 },
  { name: '系统设计', count: 9 },
  { name: '工具效率', count: 7 },
]

const tags = [
  'Vue', 'TypeScript', 'Spring Boot', 'MySQL', 'Redis',
  'Docker', 'Linux', 'Git', 'Vite', 'Kafka',
]

</script>

<template>
  <aside class="sidebar">
    <!-- Categories -->
    <div class="sidebar-widget">
      <h3 class="sidebar-widget__title">分类</h3>
      <ul class="sidebar-widget__list">
        <li v-for="cat in categories" :key="cat.name" class="sidebar-category">
          <a :href="`/category/${cat.name}`" class="sidebar-category__link">
            <span class="sidebar-category__dot"></span>
            <span class="sidebar-category__name">{{ cat.name }}</span>
          </a>
          <span class="sidebar-category__count">{{ cat.count }}</span>
        </li>
      </ul>
    </div>

    <!-- Tags -->
    <div class="sidebar-widget">
      <h3 class="sidebar-widget__title">标签</h3>
      <div class="sidebar-tags">
        <a v-for="tag in tags" :key="tag" :href="`/tag/${tag}`" class="sidebar-tag">
          {{ tag }}
        </a>
      </div>
    </div>

  </aside>
</template>

<style scoped>
.sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: sticky;
  top: calc(var(--spacing-header-height) + 24px);
  align-self: flex-start;
}

.sidebar-widget {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: 20px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);
}

.sidebar-widget__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border);
}

/* Categories */
.sidebar-widget__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-category {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-category__link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 8px;
  border-radius: 8px;
  flex: 1;
  transition: background var(--transition-base);
}

.sidebar-category__link:hover {
  background: var(--color-bg-hover);
}

.sidebar-category__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-accent);
  flex-shrink: 0;
}

.sidebar-category__name {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.sidebar-category__count {
  font-size: 12px;
  color: var(--color-text-faint);
  background: var(--color-bg-hover);
  padding: 2px 8px;
  border-radius: 10px;
}

/* Tags */
.sidebar-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sidebar-tag {
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  padding: 4px 12px;
  border-radius: var(--radius-tag);
  border: 1px solid var(--color-border);
  transition: all var(--transition-base);
}

.sidebar-tag:hover {
  color: var(--color-accent);
  border-color: var(--color-accent);
  background: rgba(74, 141, 183, 0.06);
}


@media (max-width: 900px) {
  .sidebar {
    width: 100%;
    position: static;
  }
}
</style>
