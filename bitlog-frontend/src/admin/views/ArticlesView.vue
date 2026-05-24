<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type TableInstance } from 'element-plus'
import { Search, RefreshLeft, Plus, MoreFilled } from '@element-plus/icons-vue'
import {
  getMyArticles,
  updateArticlesStatus,
  deleteArticles,
  type ArticleVO,
  type ArticleCounts,
  type ArticleStatus,
} from '@/api/admin/article'
import { ApiError } from '@/utils/request'

const route = useRoute()
const router = useRouter()

// ── State ─────────────────────────────────────────────────────────────────────
const loading = ref(false)
const articles = ref<ArticleVO[]>([])
const counts = ref<ArticleCounts>({ total: 0, published: 0, draft: 0 })
const tableRef = ref<TableInstance>()
const selectedRows = ref<ArticleVO[]>([])

// ── Tabs ──────────────────────────────────────────────────────────────────────
type TabKey = 'all' | 'published' | 'draft'

const TAB_STATUS: Record<TabKey, ArticleStatus | undefined> = {
  all: undefined,
  published: 'PUBLISHED',
  draft: 'DRAFT',
}

const VALID_TABS = new Set<TabKey>(['all', 'published', 'draft'])
function isValidTab(v: unknown): v is TabKey { return VALID_TABS.has(v as TabKey) }

// ── Init from URL ─────────────────────────────────────────────────────────────
const q = route.query
const activeTab = ref<TabKey>(isValidTab(q.tab) ? q.tab : 'all')
const keyword   = ref(typeof q.keyword === 'string' ? q.keyword : '')
const pagination = reactive({
  pageNum:  Math.max(1, Number(q.page)  || 1),
  pageSize: [10, 20, 50].includes(Number(q.size)) ? Number(q.size) : 10,
  total: 0,
})

function syncUrl() {
  const query: Record<string, string> = {}
  if (activeTab.value !== 'all')        query.tab     = activeTab.value
  if (keyword.value.trim())             query.keyword = keyword.value.trim()
  if (pagination.pageNum !== 1)         query.page    = String(pagination.pageNum)
  if (pagination.pageSize !== 10)       query.size    = String(pagination.pageSize)
  router.replace({ query })
}

function switchTab(tab: TabKey) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  pagination.pageNum = 1
  clearSelection()
  syncUrl()
  fetchArticles()
}

// ── Filters & pagination ──────────────────────────────────────────────────────
async function fetchArticles() {
  loading.value = true
  try {
    const res = await getMyArticles({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      status: TAB_STATUS[activeTab.value],
      keyword: keyword.value.trim() || undefined,
    })
    articles.value = res.page.content
    pagination.total = res.page.totalElements
    counts.value = res.counts
  } catch (err) {
    handleApiError(err, '加载文章失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  syncUrl()
  fetchArticles()
}

function handleReset() {
  keyword.value = ''
  pagination.pageNum = 1
  clearSelection()
  syncUrl()
  fetchArticles()
}

function handleCurrentChange() {
  syncUrl()
  fetchArticles()
}

function handleSizeChange() {
  pagination.pageNum = 1
  syncUrl()
  fetchArticles()
}

onMounted(fetchArticles)

// ── Selection ─────────────────────────────────────────────────────────────────
function handleSelectionChange(rows: ArticleVO[]) {
  selectedRows.value = rows
}

function clearSelection() {
  selectedRows.value = []
  tableRef.value?.clearSelection()
}

// ── Error handling ────────────────────────────────────────────────────────────
function handleApiError(err: unknown, fallback = '操作失败') {
  if (err instanceof ApiError) {
    if (err.code === 43001) {
      ElMessage.error('文章不存在，列表已刷新')
      fetchArticles()
    } else if (err.code === 43003) {
      ElMessage.error('无权操作该文章')
    } else {
      ElMessage.error(err.message || fallback)
    }
    return
  }
  ElMessage.error(fallback)
}

// ── Single row actions ────────────────────────────────────────────────────────
async function handleCommand(cmd: string, row: ArticleVO) {
  if (cmd === 'edit') {
    router.push(`/admin/write/${row.id}`)
  } else if (cmd === 'preview') {
    if (row.status === 'PUBLISHED') {
      window.open(`/article/${row.id}`, '_blank')
    } else {
      window.open(`/admin/preview/${row.id}`, '_blank')
    }
  } else if (cmd === 'togglePublish') {
    const next: ArticleStatus = row.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
    const label = next === 'PUBLISHED' ? '发布' : '取消发布'
    if (next === 'DRAFT') {
      try {
        await ElMessageBox.confirm(
          `确认取消发布「${row.title}」？`,
          '取消发布',
          { confirmButtonText: '取消发布', cancelButtonText: '取消', type: 'warning' },
        )
      } catch { return }
    }
    try {
      await updateArticlesStatus([row.id], next)
      ElMessage.success(next === 'PUBLISHED' ? '文章已发布' : '已取消发布')
      fetchArticles()
    } catch (err) {
      handleApiError(err, `${label}失败`)
    }
  } else if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm(`确认删除「${row.title}」？`, '删除文章', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
      })
    } catch { return }
    try {
      await deleteArticles([row.id])
      ElMessage.success('文章已删除')
      fetchArticles()
    } catch (err) {
      handleApiError(err, '删除失败')
    }
  }
}

// ── Batch actions ─────────────────────────────────────────────────────────────
async function handleBatchPublish(publish: boolean) {
  const next: ArticleStatus = publish ? 'PUBLISHED' : 'DRAFT'
  const label = publish ? '发布' : '撤回'
  // snapshot before confirm to avoid state desync across await
  const ids = selectedRows.value.map(a => a.id)
  const count = ids.length
  try {
    await ElMessageBox.confirm(
      `确认${label}选中的 ${count} 篇文章？`,
      `批量${label}`,
      { confirmButtonText: label, cancelButtonText: '取消' },
    )
  } catch { return }
  try {
    await updateArticlesStatus(ids, next)
    ElMessage.success(`已${label} ${count} 篇文章`)
    clearSelection()
    fetchArticles()
  } catch (err) {
    handleApiError(err, `批量${label}失败`)
  }
}

async function handleBatchDelete() {
  const ids = selectedRows.value.map(a => a.id)
  const count = ids.length
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${count} 篇文章？`,
      '批量删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
    )
  } catch { return }
  try {
    await deleteArticles(ids)
    ElMessage.success(`已删除 ${count} 篇文章`)
    clearSelection()
    fetchArticles()
  } catch (err) {
    handleApiError(err, '批量删除失败')
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function getRowClass({ row }: { row: ArticleVO }) {
  return row.status === 'DRAFT' ? 'row--draft' : ''
}

function relativeTime(d: string) {
  const diff = Date.now() - new Date(d).getTime()
  const m    = Math.floor(diff / 60000)
  const h    = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (m < 1)    return '刚刚'
  if (m < 60)   return `${m} 分钟前`
  if (h < 24)   return `${h} 小时前`
  if (days < 30) return `${days} 天前`
  if (days < 365) return `${Math.floor(days / 30)} 个月前`
  return `${Math.floor(days / 365)} 年前`
}

function formatViews(n: number) {
  if (n >= 10000) return `${(n / 10000).toFixed(1)}w`
  if (n >= 1000)  return `${(n / 1000).toFixed(1)}k`
  return String(n)
}
</script>

<template>
  <div class="articles-page">
    <div class="main-card">

      <!-- ── Header ─────────────────────────────────────────────────────────── -->
      <div class="card-header">
        <div class="view-tabs">
          <button class="view-tab" :class="{ 'view-tab--active': activeTab === 'all' }" @click="switchTab('all')">
            全部
            <span v-if="counts.total > 0" class="tab-count">{{ counts.total }}</span>
          </button>
          <button class="view-tab" :class="{ 'view-tab--active': activeTab === 'published' }" @click="switchTab('published')">
            已发布
            <span v-if="counts.published > 0" class="tab-count">{{ counts.published }}</span>
          </button>
          <button class="view-tab" :class="{ 'view-tab--active': activeTab === 'draft' }" @click="switchTab('draft')">
            草稿
            <span v-if="counts.draft > 0" class="tab-count">{{ counts.draft }}</span>
          </button>
        </div>

        <div class="header-actions">
          <el-input
            v-model="keyword"
            placeholder="搜索标题"
            clearable
            :prefix-icon="Search"
            style="width: 200px"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-button :icon="RefreshLeft" @click="handleReset" />
          <el-button type="primary" :icon="Plus" @click="router.push('/admin/write')">写文章</el-button>
        </div>
      </div>

      <!-- ── Selection bar ───────────────────────────────────────────────────── -->
      <Transition name="sel-bar">
        <div v-if="selectedRows.length > 0" class="selection-bar">
          <span class="sel-count">已选 <b>{{ selectedRows.length }}</b> 篇</span>
          <div class="sel-actions">
            <el-button size="small" @click="handleBatchPublish(true)">批量发布</el-button>
            <el-button size="small" @click="handleBatchPublish(false)">批量撤回</el-button>
            <el-button size="small" type="danger" plain @click="handleBatchDelete">批量删除</el-button>
          </div>
          <el-button size="small" text class="sel-cancel" @click="clearSelection">取消选择</el-button>
        </div>
      </Transition>

      <!-- ── Table ───────────────────────────────────────────────────────────── -->
      <div class="table-scroll-wrap">
        <el-table
          ref="tableRef"
          v-loading="loading"
          :data="articles"
          :row-key="(row: ArticleVO) => row.id"
          :row-class-name="getRowClass"
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="44" />

          <el-table-column label="文章" min-width="260">
            <template #default="{ row }">
              <div class="title-cell">
                <span
                  class="article-title"
                  :class="{ 'article-title--draft': row.status === 'DRAFT' }"
                  @click="handleCommand('edit', row)"
                >{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="分类" width="96">
            <template #default="{ row }">
              <span v-if="row.categoryName" class="category-tag">{{ row.categoryName }}</span>
              <span v-else class="cell-muted">—</span>
            </template>
          </el-table-column>

          <el-table-column label="标签" min-width="160">
            <template #default="{ row }">
              <div v-if="row.tags.length > 0" class="tags-cell">
                <span v-for="tag in row.tags.slice(0, 2)" :key="tag" class="tag-chip">{{ tag }}</span>
                <span v-if="row.tags.length > 2" class="tag-more">+{{ row.tags.length - 2 }}</span>
              </div>
              <span v-else class="cell-muted">—</span>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <div class="status-cell">
                <span v-if="row.publishedVersionId !== null" class="status-badge status-badge--published">已发布</span>
                <span v-if="row.latestVersionId !== row.publishedVersionId" class="status-badge status-badge--draft">草稿</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="阅读" width="76" align="right">
            <template #default="{ row }">
              <span class="cell-muted">{{ row.status === 'PUBLISHED' ? formatViews(row.readCount) : '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="更新时间" width="120">
            <template #default="{ row }">
              <el-tooltip :content="row.updateTime" placement="top">
                <span class="cell-muted">{{ relativeTime(row.updateTime) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>

          <el-table-column label="" width="96" align="right" fixed="right">
            <template #default="{ row }">
              <div class="row-actions">
                <button class="edit-btn" @click="handleCommand('edit', row)">编辑</button>
                <el-dropdown trigger="hover" @command="(cmd: string) => handleCommand(cmd, row)">
                  <button class="more-btn">
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="preview">预览</el-dropdown-item>
                      <el-dropdown-item v-if="row.status === 'PUBLISHED'" command="togglePublish">
                        取消发布
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" divided style="color: var(--el-color-danger)">
                        删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无文章" :image-size="80" />
          </template>
        </el-table>
      </div>

      <!-- ── Pagination ──────────────────────────────────────────────────────── -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>

    </div>
  </div>
</template>

<style scoped>
.articles-page {
  display: flex;
  flex-direction: column;
}

.main-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

/* ── Header ──────────────────────────────────────────────────────────────────── */
.card-header {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 20px;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
}

.header-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

/* ── Tabs ────────────────────────────────────────────────────────────────────── */
.view-tabs {
  display: flex;
}

.view-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 4px;
  margin-right: 20px;
  height: 48px;
  font-size: 14px;
  color: #6b7280;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
  white-space: nowrap;
}

.view-tab:hover {
  color: #374151;
}

.view-tab--active {
  color: #1d4ed8;
  border-bottom-color: #1d4ed8;
  font-weight: 500;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 600;
  background: #e0e7ff;
  color: #3730a3;
  border-radius: 10px;
}

/* ── Selection bar ───────────────────────────────────────────────────────────── */
.selection-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  background: #eff6ff;
  border-bottom: 1px solid #bfdbfe;
}

.sel-count {
  font-size: 13px;
  color: #1d4ed8;
  white-space: nowrap;
}

.sel-count b {
  font-weight: 700;
}

.sel-actions {
  display: flex;
  gap: 8px;
}

.sel-cancel {
  margin-left: auto;
  color: #6b7280;
}

.sel-bar-enter-active,
.sel-bar-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.sel-bar-enter-from,
.sel-bar-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ── Table ───────────────────────────────────────────────────────────────────── */
.table-scroll-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

:deep(.el-table) {
  --el-table-border-color: #f3f4f6;
  --el-table-header-bg-color: #f9fafb;
  --el-table-header-text-color: #6b7280;
  --el-table-row-hover-bg-color: #f5f7ff;
  --el-table-tr-bg-color: #fff;
}

:deep(.el-table th) {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

/* Title cell */
.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.pin-badge {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  color: #b45309;
  background: #fef3c7;
  padding: 1px 6px;
  border-radius: 4px;
  border: 1px solid #fde68a;
}

.article-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.1s;
}

.article-title:hover {
  color: #1d4ed8;
}

.article-title--draft {
  color: #6b7280;
}

/* Category */
.category-tag {
  display: inline-block;
  font-size: 12px;
  color: #374151;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

/* Tags */
.tags-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
}

.tag-chip {
  display: inline-flex;
  font-size: 11px;
  font-weight: 500;
  color: #4338ca;
  background: #e0e7ff;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

.tag-more {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 500;
}

/* Status */
.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  justify-content: center;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge--published {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.status-badge--draft {
  background: #f9fafb;
  color: #6b7280;
  border: 1px solid #e5e7eb;
}


/* Misc */
.cell-muted {
  font-size: 13px;
  color: #6b7280;
}

/* Row actions */
.row-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
  padding-right: 4px;
}

.edit-btn {
  font-size: 13px;
  font-weight: 500;
  color: #4338ca;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 5px;
  opacity: 0;
  transition: opacity 0.15s, background 0.15s;
  white-space: nowrap;
}

.edit-btn:hover {
  background: #ede9fe;
}

:deep(.el-table tr:hover) .edit-btn {
  opacity: 1;
}

.more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  outline: none;
  transition: background 0.15s, color 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.more-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.more-btn:focus,
.more-btn:active {
  outline: none;
}

/* Draft row: amber left accent */
:deep(.el-table .row--draft .el-table__cell:first-child) {
  box-shadow: inset 3px 0 0 #f59e0b;
}

/* ── Pagination ──────────────────────────────────────────────────────────────── */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 14px 20px;
  border-top: 1px solid #f3f4f6;
}

/* ── Mobile ──────────────────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: stretch;
    padding: 0 12px;
    gap: 0;
  }

  .view-tabs {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    border-bottom: 1px solid #f0f0f0;
  }

  .view-tabs::-webkit-scrollbar {
    display: none;
  }

  .view-tab {
    flex-shrink: 0;
    margin-right: 12px;
    height: 42px;
    font-size: 13px;
  }

  .header-actions {
    padding: 8px 0;
    flex-wrap: wrap;
  }

  .header-actions .el-input {
    width: 100% !important;
    flex: 1 1 120px;
  }

  .selection-bar {
    flex-wrap: wrap;
    gap: 8px;
    padding: 8px 12px;
  }

  .sel-actions {
    flex-wrap: wrap;
  }

  .sel-cancel {
    margin-left: 0;
  }

  .pagination-bar {
    padding: 12px;
    justify-content: center;
  }
}
</style>
