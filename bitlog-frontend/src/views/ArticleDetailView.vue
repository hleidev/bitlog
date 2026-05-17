<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getArticleDetail, type ArticleDetailVO } from '@/api/article'
import { formatDate } from '@/utils/format'

const router = useRouter()
const route = useRoute()

const article = ref<ArticleDetailVO | null>(null)
const loading = ref(true)
const error = ref(false)

const toc = ref<{ id: string; level: number; text: string }[]>([])

const activeSection = ref('')
const scrollProgress = ref(0)

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    article.value = await getArticleDetail(id)
    await nextTick()
    const headings = document.querySelectorAll<HTMLElement>('.prose h2, .prose h3')
    toc.value = Array.from(headings).map((el, i) => {
      if (!el.id) el.id = `heading-${i}`
      return { id: el.id, level: el.tagName === 'H2' ? 2 : 3, text: el.textContent ?? '' }
    })
    if (toc.value.length) activeSection.value = toc.value[0].id
    window.addEventListener('scroll', onScroll, { passive: true })
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
})

const onScroll = () => {
  const el = document.documentElement
  const scrolled = el.scrollTop
  const total = el.scrollHeight - el.clientHeight
  scrollProgress.value = total > 0 ? (scrolled / total) * 100 : 0

  for (let i = toc.value.length - 1; i >= 0; i--) {
    const section = document.getElementById(toc.value[i].id)
    if (section && section.getBoundingClientRect().top <= 100) {
      activeSection.value = toc.value[i].id
      break
    }
  }
}

const scrollToSection = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <!-- Reading progress bar -->
  <div class="progress-bar" :style="{ width: scrollProgress + '%' }" />

  <!-- Loading -->
  <div v-if="loading" class="page-state">
    <div class="skeleton-hero" />
    <div class="skeleton-body container">
      <div class="skeleton-line w-60" />
      <div class="skeleton-line w-100" />
      <div class="skeleton-line w-80" />
      <div class="skeleton-line w-100" />
      <div class="skeleton-line w-70" />
    </div>
  </div>

  <!-- Error -->
  <div v-else-if="error" class="page-state page-state--error">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
      <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
    </svg>
    <p>文章加载失败</p>
    <button @click="router.back()">返回上一页</button>
  </div>

  <div v-else-if="article" class="article-detail">
    <!-- Hero cover -->
    <div class="article-hero">
      <img class="article-hero__img" :src="article.cover ?? ''" :alt="article.title" />
      <div class="article-hero__overlay" />
      <div class="article-hero__content container">
        <button class="back-btn" @click="router.back()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 5l-7 7 7 7" />
          </svg>
          返回
        </button>
        <div class="hero-meta">
          <span class="hero-category">{{ article.categoryName }}</span>
          <span class="hero-date">{{ formatDate(article.publishTime) }}</span>
        </div>
        <h1 class="article-hero__title">{{ article.title }}</h1>
        <p class="article-hero__summary">{{ article.summary }}</p>
        <div class="hero-stats">
          <span class="stat-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" />
            </svg>
            {{ article.readCount.toLocaleString() }} 阅读
          </span>
          <span class="stat-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
            </svg>
            {{ article.commentCount }} 评论
          </span>
        </div>
      </div>
    </div>

    <!-- Article layout -->
    <div class="article-layout container">
      <!-- Main content -->
      <article class="article-body">
        <div class="article-tags">
          <a v-for="tag in article.tags" :key="tag" :href="`/tag/${tag}`" class="article-tag">
            # {{ tag }}
          </a>
        </div>

        <div class="prose" v-html="article.content" />

        <!-- Tags footer -->
        <div class="article-footer">
          <div class="article-footer__tags">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="tag-icon">
              <path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z" /><line x1="7" y1="7" x2="7.01" y2="7" />
            </svg>
            <a v-for="tag in article.tags" :key="tag" :href="`/tag/${tag}`" class="footer-tag">
              {{ tag }}
            </a>
          </div>
          <div class="article-footer__actions">
            <button class="action-btn">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
              </svg>
              分享
            </button>
          </div>
        </div>

        <!-- Comment section -->
        <div class="comment-section">
          <h3 class="comment-section__title">评论 <span>({{ article.commentCount }})</span></h3>
          <div class="comment-compose comment-compose--disabled">
            <div class="comment-avatar comment-avatar--muted">我</div>
            <div class="compose-right">
              <textarea
                class="compose-input"
                placeholder="评论功能即将上线…"
                rows="3"
                disabled
              />
              <div class="compose-footer">
                <span class="compose-count">评论功能开发中</span>
                <button class="compose-submit" disabled>发表评论</button>
              </div>
            </div>
          </div>
        </div>
      </article>

      <!-- TOC sidebar -->
      <aside class="toc-sidebar">
        <div class="toc-card">
          <h4 class="toc-title">目录</h4>
          <nav class="toc-nav">
            <a
              v-for="item in toc"
              :key="item.id"
              class="toc-item"
              :class="[`toc-item--h${item.level}`, { 'toc-item--active': activeSection === item.id }]"
              @click.prevent="scrollToSection(item.id)"
              href="#"
            >
              {{ item.text }}
            </a>
          </nav>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
/* ── Loading / Error states ── */
.page-state {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--color-text-muted);
}

.page-state--error svg {
  width: 40px;
  height: 40px;
  color: var(--color-text-faint);
}

.page-state--error p {
  font-size: 15px;
}

.page-state--error button {
  font-size: 13px;
  color: var(--color-accent);
  background: none;
  border: 1px solid var(--color-accent);
  border-radius: 20px;
  padding: 6px 18px;
  cursor: pointer;
  margin-top: 4px;
  transition: all var(--transition-base);
}

.page-state--error button:hover {
  background: rgba(74, 141, 183, 0.08);
}

.skeleton-hero {
  width: 100%;
  height: 420px;
  background: linear-gradient(90deg, var(--color-bg-hover) 25%, var(--color-border) 50%, var(--color-bg-hover) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 40px;
}

.skeleton-line {
  height: 14px;
  border-radius: 6px;
  background: linear-gradient(90deg, var(--color-bg-hover) 25%, var(--color-border) 50%, var(--color-bg-hover) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-line.w-60 { width: 60%; }
.skeleton-line.w-70 { width: 70%; }
.skeleton-line.w-80 { width: 80%; }
.skeleton-line.w-100 { width: 100%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── Progress bar ── */
.progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  background: var(--color-accent);
  z-index: 1000;
  transition: width 0.1s linear;
  border-radius: 0 2px 2px 0;
}

/* ── Hero ── */
.article-hero {
  position: relative;
  height: 520px;
  overflow: hidden;
}

.article-hero__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-hero__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.15) 0%,
    rgba(0, 0, 0, 0.55) 60%,
    rgba(0, 0, 0, 0.82) 100%
  );
}

.article-hero__content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding-bottom: 48px;
  color: #fff;
}

.back-btn {
  position: absolute;
  top: calc(var(--spacing-header-height) + 20px);
  left: 24px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  padding: 6px 14px;
  cursor: pointer;
  transition: all var(--transition-base);
  backdrop-filter: blur(4px);
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
}

.back-btn svg {
  width: 14px;
  height: 14px;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.hero-category {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  background: var(--color-accent);
  color: #fff;
  padding: 3px 12px;
  border-radius: 20px;
}

.hero-date {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.article-hero__title {
  font-size: clamp(22px, 4vw, 36px);
  font-weight: 700;
  line-height: 1.35;
  color: #fff;
  max-width: 760px;
  margin-bottom: 12px;
}

.article-hero__summary {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.72);
  line-height: 1.65;
  max-width: 660px;
  margin-bottom: 20px;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.stat-item svg {
  width: 14px;
  height: 14px;
}

/* ── Layout ── */
.article-layout {
  display: flex;
  align-items: flex-start;
  gap: 40px;
  padding-top: 48px;
  padding-bottom: 80px;
}

/* ── Article body ── */
.article-body {
  flex: 1;
  min-width: 0;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 32px;
}

.article-tag {
  font-size: 12px;
  color: var(--color-accent);
  background: rgba(74, 141, 183, 0.08);
  border: 1px solid rgba(74, 141, 183, 0.25);
  padding: 4px 12px;
  border-radius: var(--radius-tag);
  transition: all var(--transition-base);
}

.article-tag:hover {
  background: rgba(74, 141, 183, 0.15);
}

/* ── Prose typography ── */
.prose {
  font-size: 16px;
  line-height: 1.85;
  color: var(--color-text-secondary);
}

.prose h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 40px 0 16px;
  padding-top: 8px;
  border-bottom: 2px solid var(--color-border);
  padding-bottom: 10px;
}

.prose h2:first-child {
  margin-top: 0;
}

.prose h3 {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 28px 0 12px;
}

.prose p {
  margin-bottom: 18px;
}

.prose strong {
  font-weight: 600;
  color: var(--color-text-primary);
}

.prose code {
  font-family: var(--font-mono);
  font-size: 0.875em;
  background: var(--color-bg-hover);
  color: #e07b4f;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid var(--color-border);
}

.prose pre {
  background: #1e2436;
  border-radius: 12px;
  padding: 20px 24px;
  overflow-x: auto;
  margin: 24px 0;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.prose pre code {
  font-family: var(--font-mono);
  font-size: 13.5px;
  background: none;
  color: #a8c4d8;
  padding: 0;
  border: none;
  border-radius: 0;
  line-height: 1.7;
  white-space: pre;
}

.prose blockquote {
  border-left: 3px solid var(--color-accent);
  margin: 24px 0;
  padding: 12px 20px;
  background: rgba(74, 141, 183, 0.05);
  border-radius: 0 8px 8px 0;
}

.prose blockquote p {
  margin: 0;
  color: var(--color-text-muted);
  font-style: italic;
}

.prose ul,
.prose ol {
  padding-left: 24px;
  margin-bottom: 18px;
}

.prose li {
  margin-bottom: 8px;
}

.prose ul li {
  list-style: disc;
}

.prose ol li {
  list-style: decimal;
}

/* ── Article footer ── */
.article-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
}

.article-footer__tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-icon {
  width: 15px;
  height: 15px;
  color: var(--color-text-faint);
  flex-shrink: 0;
}

.footer-tag {
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  padding: 4px 12px;
  border-radius: var(--radius-tag);
  border: 1px solid var(--color-border);
  transition: all var(--transition-base);
}

.footer-tag:hover {
  color: var(--color-accent);
  border-color: var(--color-accent);
}

.article-footer__actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-muted);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 20px;
  padding: 7px 16px;
  cursor: pointer;
  transition: all var(--transition-base);
}

.action-btn svg {
  width: 14px;
  height: 14px;
}

.action-btn:hover,
.like-btn:hover {
  color: var(--color-accent);
  border-color: var(--color-accent);
  background: rgba(74, 141, 183, 0.06);
}

/* ── Comment section ── */
.comment-section {
  margin-top: 48px;
}

.comment-section__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.comment-section__title span {
  font-size: 14px;
  font-weight: 400;
  color: var(--color-text-faint);
  margin-left: 6px;
}

.comment-placeholder {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.comment-item {
  display: flex;
  gap: 14px;
}

.comment-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-accent), #2d6a9f);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.comment-author {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.comment-time {
  font-size: 12px;
  color: var(--color-text-faint);
}

.comment-text {
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

/* ── TOC sidebar ── */
.toc-sidebar {
  width: 240px;
  flex-shrink: 0;
  position: sticky;
  top: calc(var(--spacing-header-height) + 24px);
  max-height: calc(100vh - var(--spacing-header-height) - 48px);
  overflow-y: auto;
}

.toc-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: 20px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);
}

.toc-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--color-text-faint);
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border);
}

.toc-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toc-item {
  display: block;
  font-size: 13px;
  line-height: 1.45;
  color: var(--color-text-muted);
  padding: 5px 8px;
  border-radius: 6px;
  transition: all var(--transition-base);
  border-left: 2px solid transparent;
  text-decoration: none;
}

.toc-item--h3 {
  padding-left: 20px;
  font-size: 12px;
}

.toc-item:hover {
  color: var(--color-accent);
  background: rgba(74, 141, 183, 0.06);
}

.toc-item--active {
  color: var(--color-accent);
  border-left-color: var(--color-accent);
  background: rgba(74, 141, 183, 0.06);
  font-weight: 500;
}

/* ── Responsive ── */
@media (max-width: 900px) {
  .toc-sidebar {
    display: none;
  }

  .article-hero {
    height: 380px;
  }

  .article-hero__title {
    font-size: 22px;
  }

  .article-layout {
    padding-top: 32px;
  }

  .prose {
    font-size: 15px;
  }

  .prose h2 {
    font-size: 19px;
  }
}

@media (max-width: 600px) {
  .article-hero {
    height: 320px;
  }

  .article-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* ── Comment compose ── */
.comment-compose {
  display: flex;
  gap: 14px;
  margin-bottom: 32px;
  padding-bottom: 32px;
  border-bottom: 1px solid var(--color-border);
}

.compose-right {
  flex: 1;
  min-width: 0;
}

.compose-input {
  width: 100%;
  resize: vertical;
  min-height: 88px;
  padding: 12px 14px;
  font-size: 14px;
  font-family: var(--font-sans);
  line-height: 1.65;
  color: var(--color-text-primary);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  outline: none;
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
  box-sizing: border-box;
}

.compose-input:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(74, 141, 183, 0.12);
}

.compose-input::placeholder {
  color: var(--color-text-faint);
}

.compose-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.compose-count {
  font-size: 12px;
  color: var(--color-text-faint);
}

.compose-submit {
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  background: var(--color-accent);
  border: none;
  border-radius: 20px;
  padding: 7px 20px;
  cursor: pointer;
  transition: all var(--transition-base);
}

.compose-submit:hover:not(:disabled) {
  background: var(--color-accent-dark);
}

.compose-submit:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.comment-compose--disabled {
  opacity: 0.5;
  pointer-events: none;
}

.comment-avatar--muted {
  background: var(--color-bg-hover);
  color: var(--color-text-faint);
}
</style>
