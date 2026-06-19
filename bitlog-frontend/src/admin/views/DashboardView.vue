<script setup lang="ts">
import AdminEmptyState from '@/admin/components/AdminEmptyState.vue'

const stats = [
  {
    label: '文章总数',
    value: '—',
    unit: '篇',
    icon: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm-1 1.5L18.5 9H13V3.5zM8 13h8v1.5H8V13zm0 3h8v1.5H8V16zm0-6h3v1.5H8V10z',
  },
  {
    label: '注册用户',
    value: '—',
    unit: '人',
    icon: 'M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z',
  },
  {
    label: '评论总数',
    value: '—',
    unit: '条',
    icon: 'M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z',
  },
  {
    label: '总访问量',
    value: '—',
    unit: '次',
    icon: 'M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6h-6z',
  },
]
</script>

<template>
  <div class="dashboard">
    <!-- Page title -->
    <div class="page-header">
      <div class="title-row">
        <h2 class="page-title">概览</h2>
        <div class="title-rule" />
      </div>
    </div>

    <!-- Stat cards -->
    <div class="stat-grid">
      <div v-for="item in stats" :key="item.label" class="stat-card">
        <div class="stat-icon-wrap">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path :d="item.icon" />
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">{{ item.label }}</span>
          <div class="stat-value">
            {{ item.value }}<span class="stat-unit">{{ item.unit }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent articles placeholder -->
    <div class="section-card">
      <div class="section-header">
        <span class="section-label">最近文章</span>
        <div class="section-rule" />
      </div>
      <AdminEmptyState
        message="暂无数据"
        icon-path="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm-1 1.5L18.5 9H13V3.5z"
      />
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ── Header ── */

.page-header {
  display: flex;
  align-items: center;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.page-title {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--admin-text-primary);
  white-space: nowrap;
  margin: 0;
}

.title-rule {
  flex: 1;
  height: 1px;
  background: var(--admin-sidebar-border, #e8e4de);
}

/* ── Stat grid ── */

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: var(--admin-header-bg, #faf9f7);
  border: 1px solid var(--admin-sidebar-border, #e8e4de);
  border-radius: 4px;
}

.stat-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  background: rgba(184, 92, 56, 0.08);
  color: var(--admin-accent, #b85c38);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-wrap svg {
  width: 20px;
  height: 20px;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted, #b0a89e);
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-text-primary);
  line-height: 1;
}

.stat-unit {
  font-size: 12px;
  font-weight: 400;
  color: var(--admin-sidebar-text-muted, #b0a89e);
  margin-left: 3px;
}

/* ── Section card ── */

.section-card {
  background: var(--admin-header-bg, #faf9f7);
  border: 1px solid var(--admin-sidebar-border, #e8e4de);
  border-radius: 4px;
  padding: 18px 20px 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.section-label {
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: 13.5px;
  font-weight: 600;
  color: var(--admin-text-primary);
  white-space: nowrap;
}

.section-rule {
  flex: 1;
  height: 1px;
  background: var(--admin-sidebar-border, #e8e4de);
}

/* ── Responsive ── */

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  .stat-card {
    padding: 14px;
  }
  .stat-value {
    font-size: 18px;
  }
}
</style>
