<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/useUserStore'
import {
  getMyArticles,
  getMyArticleStats,
  type ArticleCounts,
  type ArticleVO,
} from '@/api/admin/article'
import { getAdminLinkStats, type FriendLinkStats } from '@/api/admin/link'
import { getAdminCommentPage, type CommentAdmin } from '@/api/admin/comment'
import { articleStateLabel, hasUnpublishedChanges } from '@/admin/utils/articleState'
import { formatDate } from '@/utils/format'
import AdminIcon from '@/admin/components/AdminIcon.vue'

const { isAdmin, userInfo } = storeToRefs(useUserStore())
const loading = ref(false)
const recent = ref<ArticleVO[] | null>(null)
const drafts = ref<ArticleVO[] | null>(null)
const counts = ref<ArticleCounts | null>(null)
const links = ref<FriendLinkStats | null>(null)
const comments = ref<CommentAdmin[] | null>(null)
const today = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())
const hasError = computed(
  () =>
    !loading.value &&
    [recent.value, drafts.value, counts.value, links.value, comments.value].some((v) => v === null),
)
const stats = computed(() => [
  { label: '全部文章', value: counts.value?.total, tab: 'all' },
  { label: '已发布', value: counts.value?.published, tab: 'published' },
  { label: '未发布', value: counts.value?.draft, tab: 'draft' },
])
let requestId = 0

async function loadDashboard() {
  const id = ++requestId
  recent.value = null
  drafts.value = null
  counts.value = null
  links.value = null
  comments.value = null
  if (!isAdmin.value) {
    loading.value = false
    return
  }
  loading.value = true
  // 每区独立取数，一项失败不把其他区域伪装为空数据。
  const results = await Promise.allSettled([
    getMyArticles({ pageNum: 1, pageSize: 8, sortField: 'UPDATE_TIME' }),
    getMyArticles({ pageNum: 1, pageSize: 3, status: 'DRAFT', sortField: 'UPDATE_TIME' }),
    getMyArticleStats(),
    getAdminLinkStats(),
    getAdminCommentPage({ pageNum: 1, pageSize: 3 }),
  ])
  if (id !== requestId) return
  const [recentResult, draftResult, countResult, linkResult, commentResult] = results
  if (recentResult.status === 'fulfilled') recent.value = recentResult.value.content
  if (draftResult.status === 'fulfilled') drafts.value = draftResult.value.content
  if (countResult.status === 'fulfilled') counts.value = countResult.value
  if (linkResult.status === 'fulfilled') links.value = linkResult.value
  if (commentResult.status === 'fulfilled') comments.value = commentResult.value.content
  loading.value = false
}
watch([isAdmin, () => userInfo.value?.userId], loadDashboard, { immediate: true })
onBeforeUnmount(() => {
  requestId++
})
</script>

<template>
  <div class="dashboard">
    <header class="workspace-page-heading dashboard-heading">
      <h1>工作台</h1>
      <time class="workspace-date">{{ today }}</time>
    </header>

    <template v-if="isAdmin">
      <div class="workspace-summary" :aria-busy="loading">
        <RouterLink
          v-for="stat in stats"
          :key="stat.tab"
          :to="{ path: '/admin/articles', query: { tab: stat.tab } }"
          class="summary-item"
        >
          <span class="summary-value">{{ loading ? '—' : (stat.value ?? '—') }}</span>
          <span class="summary-label">{{ stat.label }}</span>
        </RouterLink>
        <button
          class="refresh-btn"
          :disabled="loading"
          aria-label="刷新工作台"
          @click="loadDashboard"
        >
          <AdminIcon name="reset" :class="{ spinning: loading }" /><span>{{
            loading ? '加载中' : '刷新'
          }}</span>
        </button>
      </div>
      <p v-if="hasError" class="workspace-error" role="alert">
        部分数据未能加载。<button @click="loadDashboard">重新加载</button>
      </p>

      <div class="workspace-grid">
        <section class="recent-section" aria-labelledby="recent-heading" :aria-busy="loading">
          <div class="section-heading">
            <h2 id="recent-heading">最近更新</h2>
            <RouterLink to="/admin/articles">全部文章 <span aria-hidden="true">↗</span></RouterLink>
          </div>
          <p v-if="loading" class="section-state">正在读取文章…</p>
          <p v-else-if="recent === null" class="section-state">文章加载失败，请重新加载。</p>
          <div v-else-if="!recent.length" class="first-article">
            <AdminIcon name="article" />
            <h3>从第一篇开始</h3>
            <p>草稿可以随时保存，准备好了再发布。</p>
            <RouterLink to="/admin/write" class="primary-btn"
              >写文章 <span aria-hidden="true">↗</span></RouterLink
            >
          </div>
          <ol v-else class="recent-list">
            <li v-for="(article, index) in recent" :key="article.id" class="recent-item">
              <span class="article-index" aria-hidden="true">{{
                String(index + 1).padStart(2, '0')
              }}</span>
              <div class="recent-content">
                <RouterLink :to="'/admin/write/' + article.id" class="recent-title">{{
                  article.title || '无标题'
                }}</RouterLink>
                <div class="recent-meta">
                  <span
                    class="article-state"
                    :class="{ 'article-state--live': article.publishedVersionId !== null }"
                    >{{ articleStateLabel(article) }}</span
                  >
                  <span v-if="hasUnpublishedChanges(article)" class="unpublished-note"
                    >有未发布修改</span
                  >
                  <span v-if="article.category">{{ article.category.name }}</span>
                  <span>{{ formatDate(article.updateTime) }} 更新</span>
                </div>
              </div>
              <RouterLink
                :to="'/admin/write/' + article.id"
                class="continue-link"
                :aria-label="'编辑：' + article.title"
                ><span>编辑</span><span aria-hidden="true">↗</span></RouterLink
              >
            </li>
          </ol>
        </section>

        <aside class="workspace-side">
          <section class="draft-section" aria-labelledby="draft-heading">
            <div class="section-heading">
              <h2 id="draft-heading">继续草稿</h2>
              <RouterLink to="/admin/articles?tab=draft" aria-label="查看全部未发布文章"
                >↗</RouterLink
              >
            </div>
            <p v-if="loading" class="side-state">正在读取草稿…</p>
            <p v-else-if="drafts === null" class="side-state">草稿加载失败。</p>
            <p v-else-if="!drafts.length" class="side-state">
              没有未发布的文章。<RouterLink to="/admin/write">写一篇新的 ↗</RouterLink>
            </p>
            <ul v-else class="draft-list">
              <li v-for="article in drafts" :key="article.id">
                <RouterLink :to="'/admin/write/' + article.id">{{
                  article.title || '无标题'
                }}</RouterLink>
                <span
                  >{{ articleStateLabel(article) }} ·
                  {{ formatDate(article.updateTime) }} 更新</span
                >
              </li>
            </ul>
          </section>

          <RouterLink to="/admin/links?tab=pending" class="pending-links">
            <span class="pending-label"><AdminIcon name="link" />友链待审核</span>
            <span class="pending-value"
              >{{ loading ? '—' : (links?.pending ?? '—') }} <span aria-hidden="true">↗</span></span
            >
          </RouterLink>

          <section class="comments-section" aria-labelledby="comments-heading">
            <div class="section-heading">
              <h2 id="comments-heading">最新评论</h2>
              <RouterLink to="/admin/comments" aria-label="查看全部评论">↗</RouterLink>
            </div>
            <p v-if="loading" class="side-state">正在读取评论…</p>
            <p v-else-if="comments === null" class="side-state">评论加载失败。</p>
            <p v-else-if="!comments.length" class="side-state">还没有评论。</p>
            <ul v-else class="comment-list">
              <li v-for="comment in comments" :key="comment.id">
                <div class="comment-byline">
                  <span>{{ comment.user?.username || '已注销用户' }}</span
                  ><time>{{ formatDate(comment.createTime) }}</time>
                </div>
                <p>{{ comment.content }}</p>
                <span class="comment-source"
                  >{{ comment.status === 2 ? '已隐藏 · ' : ''
                  }}{{ comment.articleTitle || '文章已删除' }}</span
                >
              </li>
            </ul>
          </section>
        </aside>
      </div>
    </template>

    <div v-else class="personal-workspace">
      <h2>{{ userInfo?.username || '我的账号' }}</h2>
      <p>管理个人资料，或查看与你有关的回复。</p>
      <RouterLink to="/admin/profile" class="primary-btn">个人资料</RouterLink>
      <RouterLink to="/notifications" class="ghost-btn">查看通知</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.dashboard-heading {
  margin-bottom: 28px;
}
.workspace-date {
  font-size: 12px;
  color: var(--admin-text-muted);
}
.workspace-summary {
  display: flex;
  align-items: center;
  gap: 36px;
  padding-bottom: 28px;
  margin-bottom: 36px;
  border-bottom: 1px solid var(--admin-border);
}
.summary-item {
  display: flex;
  align-items: baseline;
  gap: 10px;
  text-decoration: none;
  color: var(--admin-text-primary);
}
.summary-value {
  font-family: var(--font-editorial);
  font-size: 32px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.summary-label {
  font-size: 12px;
  color: var(--admin-text-muted);
}
.summary-item:hover .summary-label {
  color: var(--admin-accent);
}
.refresh-btn {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  color: var(--admin-text-muted);
  border: 0;
  background: none;
  cursor: pointer;
  padding: 8px;
}
.refresh-btn svg {
  width: 14px;
  height: 14px;
}
.refresh-btn:disabled {
  cursor: wait;
}
.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 44px;
}
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}
.section-heading h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}
.section-heading a {
  font-size: 11px;
  color: var(--admin-text-muted);
  text-decoration: none;
}
.section-heading a:hover {
  color: var(--admin-accent);
}
.section-heading a span {
  margin-left: 10px;
}
.recent-list,
.draft-list,
.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.recent-item {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 24px 0;
  border-top: 1px solid var(--admin-border-soft);
}
.article-index {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--admin-text-muted);
  padding-top: 6px;
}
.recent-content {
  min-width: 0;
  flex: 1;
}
.recent-title {
  display: block;
  color: var(--admin-text-primary);
  font-size: 17px;
  font-weight: 500;
  line-height: 1.6;
  text-decoration: none;
  overflow-wrap: anywhere;
}
.recent-title:hover {
  color: var(--admin-accent);
}
.recent-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px 14px;
  margin-top: 10px;
  color: var(--admin-text-muted);
  font-size: 11px;
}
.article-state::before {
  content: '';
  display: inline-block;
  width: 5px;
  height: 5px;
  border: 1px solid currentColor;
  border-radius: 50%;
  margin: 0 6px 1px 0;
}
.article-state--live::before {
  background: var(--admin-status-ok);
  border-color: var(--admin-status-ok);
}
.unpublished-note {
  color: var(--admin-warning);
}
.continue-link {
  display: flex;
  gap: 8px;
  padding: 5px 0 5px 8px;
  font-size: 11px;
  color: var(--admin-text-muted);
  text-decoration: none;
  white-space: nowrap;
}
.continue-link:hover {
  color: var(--admin-accent);
}
.workspace-side {
  border-left: 1px solid var(--admin-border);
  padding-left: 28px;
}
.draft-section {
  padding-bottom: 24px;
}
.draft-list li + li {
  margin-top: 20px;
}
.draft-list a {
  display: block;
  font-size: 13px;
  line-height: 1.7;
  color: var(--admin-text-primary);
  text-decoration: none;
  overflow-wrap: anywhere;
}
.draft-list a:hover {
  color: var(--admin-accent);
}
.draft-list li > span {
  display: block;
  font-size: 10px;
  color: var(--admin-text-muted);
  margin-top: 5px;
}
.pending-links {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  margin: 4px 0 30px;
  border: 1px solid var(--admin-accent-border);
  border-radius: 6px;
  background: var(--admin-accent-bg-subtle);
  color: var(--admin-accent-dark);
  text-decoration: none;
}
.pending-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
.pending-label svg {
  width: 15px;
  height: 15px;
}
.pending-value {
  font-family: var(--font-mono);
  font-size: 19px;
  white-space: nowrap;
}
.pending-value span {
  font-size: 12px;
  margin-left: 10px;
}
.comment-list li + li {
  border-top: 1px solid var(--admin-border-soft);
  margin-top: 20px;
  padding-top: 20px;
}
.comment-byline {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 10px;
  color: var(--admin-text-muted);
}
.comment-byline > span {
  overflow-wrap: anywhere;
}
.comment-byline time {
  flex-shrink: 0;
}
.comment-list p {
  font-size: 12px;
  line-height: 1.8;
  margin: 8px 0 5px;
  color: var(--admin-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
}
.comment-source {
  display: block;
  font-size: 10px;
  color: var(--admin-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.section-state,
.side-state {
  font-size: 12px;
  color: var(--admin-text-muted);
  padding: 20px 0;
  margin: 0;
}
.side-state a {
  display: block;
  margin-top: 12px;
  color: var(--admin-accent-dark);
}
.workspace-error {
  margin: -12px 0 24px;
  font-size: 12px;
  color: var(--admin-danger);
}
.workspace-error button {
  color: inherit;
  font: inherit;
  border: 0;
  background: none;
  text-decoration: underline;
  cursor: pointer;
  padding: 4px;
}
.first-article {
  padding: 56px 20px;
  border-top: 1px solid var(--admin-border-soft);
}
.first-article > svg {
  width: 28px;
  height: 28px;
  color: var(--admin-text-muted);
}
.first-article h3,
.personal-workspace h2 {
  font-family: var(--font-display);
  font-size: 26px;
  margin: 20px 0 12px;
}
.first-article p,
.personal-workspace p {
  font-size: 13px;
  color: var(--admin-text-muted);
  margin-bottom: 24px;
}
.first-article .primary-btn {
  width: fit-content;
  gap: 24px;
  text-decoration: none;
}
.personal-workspace .primary-btn,
.personal-workspace .ghost-btn {
  display: inline-flex;
  margin-right: 12px;
  text-decoration: none;
}
.spinning {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 1100px) {
  .workspace-grid {
    grid-template-columns: minmax(0, 1fr) 240px;
    gap: 28px;
  }
  .workspace-side {
    padding-left: 22px;
  }
  .recent-item {
    gap: 12px;
  }
  .continue-link > span:first-child {
    display: none;
  }
}
@media (max-width: 900px) {
  .workspace-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .workspace-side {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;
    border-left: 0;
    padding: 28px 0 0;
    border-top: 1px solid var(--admin-border);
  }
  .pending-links {
    grid-column: 1;
    margin: 0;
    align-self: start;
  }
  .comments-section {
    grid-column: 2;
    grid-row: 1 / span 2;
  }
  .draft-section {
    padding: 0;
  }
}
@media (max-width: 540px) {
  .workspace-summary {
    gap: 20px;
    margin-bottom: 28px;
    padding-bottom: 24px;
  }
  .summary-item {
    flex-direction: column;
    gap: 8px;
  }
  .summary-value {
    font-size: 30px;
  }
  .summary-label {
    font-size: 11px;
  }
  .refresh-btn span {
    display: none;
  }
  .workspace-date {
    font-size: 10px;
  }
  .recent-item {
    padding: 20px 0;
    gap: 12px;
  }
  .recent-title {
    font-size: 16px;
  }
  .recent-meta {
    gap: 6px 10px;
  }
  .workspace-side {
    display: block;
  }
  .pending-links {
    margin: 24px 0 28px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .spinning {
    animation: none;
  }
}
</style>
