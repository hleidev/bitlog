<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/useUserStore'
import { useToast } from '@/admin/composables/useToast'
import { getMyArticles, type ArticleCounts, type ArticleVO } from '@/api/admin/article'
import { formatDate } from '@/utils/format'
import AdminEmptyState from '@/admin/components/AdminEmptyState.vue'

const RECENT_LIMIT = 5

const ICON_ALL =
  'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm-1 1.5L18.5 9H13V3.5zM8 13h8v1.5H8V13zm0 3h8v1.5H8V16zm0-6h3v1.5H8V10z'
const ICON_PUBLISHED =
  'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z'
const ICON_DRAFT =
  'M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z'

const toast = useToast()
const { isAdmin } = storeToRefs(useUserStore())

const loading = ref(true)
// 取数失败必须和「确实没有文章」区分开：统计卡是以事实姿态呈现的数字，
// 失败时若沿用初始值就会显示三个 0，等于安静地谎报「你一篇文章都没有」。
const loadFailed = ref(false)
const counts = ref<ArticleCounts>({ total: 0, published: 0, draft: 0 })
const recent = ref<ArticleVO[]>([])

// 统计卡和「最近文章」共用同一个请求：/article/my 一次同时返回 counts 与首页分页数据。
// 排序下推到后端：只拿回 RECENT_LIMIT 条，前端重排无效。
onMounted(async () => {
  try {
    const res = await getMyArticles({ pageNum: 1, pageSize: RECENT_LIMIT, sortBy: 'PUBLISH_TIME' })
    counts.value = res.counts
    recent.value = res.page.content
  } catch {
    loadFailed.value = true
    toast.error('加载仪表盘数据失败')
  } finally {
    loading.value = false
  }
})

// 计数即入口：每张卡跳到文章管理页对应的筛选标签
const stats = computed(() => [
  { label: '全部文章', value: counts.value.total, tab: 'all', icon: ICON_ALL },
  { label: '已发布', value: counts.value.published, tab: 'published', icon: ICON_PUBLISHED },
  { label: '草稿', value: counts.value.draft, tab: 'draft', icon: ICON_DRAFT },
])

// 文章管理相关路由都带 requiresAdmin，普通用户点了会被守卫拦下,所以非管理员不给链接
const linkTag = computed(() => (isAdmin.value ? RouterLink : 'div'))

// 与 ArticlesView 保持一致的状态推导：后端 status 只有 DRAFT / PUBLISHED 二值，
// 「纯草稿」和「已发布但有新草稿」都要靠版本 ID 判定。
function isPureDraft(row: ArticleVO): boolean {
  return row.publishedVersionId === null
}
function hasDraftAbovePublish(row: ArticleVO): boolean {
  return row.publishedVersionId !== null && row.latestVersionId !== row.publishedVersionId
}

// 文章下架后 publish_time 不会清空（建表注释即「首次发布时间」），纯草稿沿用它
// 会显示一个当前并未生效的发布日期，所以这种情况回落到创建时间。
function displayDate(row: ArticleVO): string {
  return formatDate(isPureDraft(row) ? row.createTime : (row.publishTime ?? row.createTime))
}
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
      <component
        :is="linkTag"
        v-for="item in stats"
        :key="item.label"
        :to="isAdmin ? { path: '/admin/articles', query: { tab: item.tab } } : undefined"
        class="stat-card"
        :class="{ 'stat-card--link': isAdmin }"
      >
        <div class="stat-icon-wrap">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path :d="item.icon" />
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">{{ item.label }}</span>
          <div class="stat-value">
            <span v-if="loading || loadFailed" class="stat-pending">—</span>
            <template v-else> {{ item.value }}<span class="stat-unit">篇</span> </template>
          </div>
        </div>
      </component>
    </div>

    <!-- Recent articles -->
    <div class="section-card">
      <div class="section-header">
        <span class="section-label">最近文章</span>
        <div class="section-rule" />
        <RouterLink v-if="isAdmin" to="/admin/articles" class="section-more">全部</RouterLink>
      </div>

      <p v-if="loading" class="recent-pending">加载中…</p>

      <p v-else-if="loadFailed" class="recent-failed">加载失败，请刷新重试</p>

      <ul v-else-if="recent.length" class="recent-list">
        <li v-for="row in recent" :key="row.id" class="recent-item">
          <component
            :is="linkTag"
            :to="isAdmin ? `/admin/write/${row.id}` : undefined"
            class="recent-title"
            :class="{ 'recent-title--link': isAdmin }"
          >
            {{ row.title || '无标题' }}
          </component>
          <div class="recent-meta">
            <span v-if="!isPureDraft(row)" class="status-badge status-badge--published"
              >已发布</span
            >
            <span
              v-if="isPureDraft(row) || hasDraftAbovePublish(row)"
              class="status-badge status-badge--draft"
              >草稿</span
            >
            <span class="recent-date">{{ displayDate(row) }}</span>
            <span class="recent-views">{{
              isPureDraft(row) ? '—' : `${row.readCount} 次阅读`
            }}</span>
          </div>
        </li>
      </ul>

      <AdminEmptyState
        v-else
        message="还没有文章"
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
  background: var(--admin-sidebar-border);
}

/* ── Stat grid ── */

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: var(--admin-header-bg);
  border: 1px solid var(--admin-sidebar-border);
  border-radius: var(--admin-radius);
  text-decoration: none;
}

.stat-card--link {
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease;
}

.stat-card--link:hover {
  border-color: var(--admin-accent-dark);
  background: var(--admin-sidebar-hover);
}

.stat-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: var(--admin-radius);
  background: var(--admin-accent-bg-soft);
  color: var(--admin-accent-dark);
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
  color: var(--admin-sidebar-text-muted);
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-text-primary);
  line-height: 1;
}

.stat-pending {
  color: var(--admin-sidebar-text-muted);
  font-weight: 400;
}

.stat-unit {
  font-size: 12px;
  font-weight: 400;
  color: var(--admin-sidebar-text-muted);
  margin-left: 3px;
}

/* ── Section card ── */

.section-card {
  background: var(--admin-header-bg);
  border: 1px solid var(--admin-sidebar-border);
  border-radius: var(--admin-radius);
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
  background: var(--admin-sidebar-border);
}

.section-more {
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: 12.5px;
  color: var(--admin-sidebar-text-muted);
  text-decoration: none;
  white-space: nowrap;
  transition: color 0.18s ease;
}

.section-more:hover {
  color: var(--admin-accent-dark);
}

/* ── Recent list ── */

.recent-pending {
  margin: 0;
  padding: 8px 0;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: 13px;
  color: var(--admin-sidebar-text-muted);
}

.recent-failed {
  margin: 0;
  padding: 8px 0;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: 13px;
  color: var(--admin-danger);
}

.recent-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 11px 0;
  border-bottom: 1px solid var(--admin-sidebar-border);
}

.recent-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.recent-item:first-child {
  padding-top: 0;
}

.recent-title {
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: 13.5px;
  color: var(--admin-text-primary);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.recent-title--link {
  transition: color 0.18s ease;
}

.recent-title--link:hover {
  color: var(--admin-accent-dark);
}

.recent-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--admin-radius);
  font-size: 11.5px;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.status-badge--published {
  background: rgba(var(--admin-status-ok-rgb), 0.08);
  color: var(--admin-status-ok);
  border: 1px solid rgba(var(--admin-status-ok-rgb), 0.2);
}

.status-badge--draft {
  background: var(--admin-sidebar-hover);
  color: var(--admin-sidebar-text-muted);
  border: 1px solid var(--admin-sidebar-border);
}

.recent-date,
.recent-views {
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: 12.5px;
  color: var(--admin-sidebar-text-muted);
  white-space: nowrap;
}

.recent-views {
  min-width: 62px;
  text-align: right;
}

/* ── Responsive ── */

@media (max-width: 900px) {
  .stat-grid {
    gap: 10px;
  }
  .stat-card {
    padding: 14px;
  }
  .stat-value {
    font-size: 18px;
  }
}

@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
  .recent-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .recent-title {
    white-space: normal;
    overflow: visible;
  }
  .recent-views {
    min-width: 0;
    text-align: left;
  }
}
</style>
