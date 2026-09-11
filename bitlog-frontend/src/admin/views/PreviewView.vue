<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useHead } from '@unhead/vue'
import { useRoute } from 'vue-router'
import { getArticleDraft } from '@/api/admin/article'
import { readPreviewHandoff } from '@/admin/composables/usePreviewHandoff'
import ArticleContent from '@/components/ArticleContent.vue'

interface PreviewData {
  title: string
  content: string
  category: { id: number; name: string } | null
  tags: { id: number; name: string }[]
}

const route = useRoute()
const article = ref<PreviewData | null>(null)
const loading = ref(true)
const error = ref(false)
const editorPath = computed(() =>
  route.params.id === 'new' ? '/admin/write' : `/admin/write/${route.params.id}`,
)
useHead({
  title: computed(() => `${article.value?.title || '文章'} · 预览 | BitLog`),
  meta: [{ name: 'robots', content: 'noindex, nofollow' }],
})

onMounted(async () => {
  const raw = String(route.params.id)

  // 新文章还没有 id，正文只能来自交接；没有交接就无从预览
  if (raw === 'new') {
    const handoff = readPreviewHandoff('new')
    if (handoff) {
      article.value = { title: handoff.title, content: handoff.content, category: null, tags: [] }
    } else {
      error.value = true
    }
    loading.value = false
    return
  }

  const id = Number(raw)
  const handoff = readPreviewHandoff(id)
  try {
    const data = await getArticleDraft(id)
    // 分类/标签只在服务器上，正文以编辑器交接的为准（含未保存的改动）
    article.value = {
      title: handoff ? handoff.title : data.title,
      content: handoff ? handoff.content : data.content,
      category: data.category,
      tags: data.tags,
    }
  } catch {
    // 预览本身不写任何东西，服务器拿不到时退化成只渲染交接内容（缺分类/标签），
    // 比整页报错有用 —— 用户要看的就是编辑器里那份
    if (handoff) {
      article.value = { title: handoff.title, content: handoff.content, category: null, tags: [] }
    } else {
      error.value = true
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="preview-page">
    <header class="preview-notice">
      <span>文章预览<span class="preview-note"> · 不会发布或保存修改</span></span
      ><RouterLink :to="editorPath">返回编辑 ↗</RouterLink>
    </header>
    <div v-if="loading" class="page-state">
      <div class="skeleton-body">
        <div class="skeleton-line w-20" />
        <div class="skeleton-line w-70" />
        <div class="skeleton-line w-50" />
        <div class="skeleton-line w-100" />
        <div class="skeleton-line w-80" />
      </div>
    </div>

    <div v-else-if="error" class="page-state page-state--error">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="8" x2="12" y2="12" />
        <line x1="12.01" y1="16" x2="12.01" y2="16" />
      </svg>
      <p>文章加载失败</p>
    </div>

    <div v-else-if="article" class="article-layout">
      <article class="article-body">
        <!-- Category -->
        <span v-if="article.category" class="article-category">{{ article.category.name }}</span>

        <!-- Title -->
        <h1 class="article-title">{{ article.title }}</h1>

        <div class="article-divider" />

        <ArticleContent :content="article.content" />

        <!-- Footer tags -->
        <div v-if="article.tags.length" class="article-footer">
          <div class="article-footer__tags">
            <span v-for="tag in article.tags" :key="tag.id" class="footer-tag">{{ tag.name }}</span>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.preview-notice {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  min-height: 52px;
  padding: 12px 32px;
  border-bottom: 1px solid var(--color-border);
  font-size: 12px;
  color: var(--color-text-muted);
}
.preview-notice a {
  color: var(--color-accent);
  text-decoration: none;
  white-space: nowrap;
}
@media (max-width: 480px) {
  .preview-notice {
    padding-inline: 20px;
  }
  .preview-note {
    display: none;
  }
}
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

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: var(--spacing-prose);
  width: 100%;
  padding: 64px 48px 0;
  margin: 0 auto;
}

.skeleton-line {
  height: 14px;
  border-radius: var(--admin-radius);
  background: linear-gradient(
    90deg,
    var(--color-bg-hover) 25%,
    var(--color-border) 50%,
    var(--color-bg-hover) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-line.w-20 {
  width: 20%;
  height: 10px;
}
.skeleton-line.w-50 {
  width: 50%;
}
.skeleton-line.w-70 {
  width: 70%;
}
.skeleton-line.w-80 {
  width: 80%;
}
.skeleton-line.w-100 {
  width: 100%;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.article-layout {
  padding: 64px 48px 100px;
}

.article-body {
  max-width: var(--spacing-prose);
  margin: 0 auto;
  width: 100%;
}

.article-category {
  display: inline-block;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--color-accent);
  margin-bottom: 20px;
}

.article-title {
  font-family: var(--font-serif);
  font-size: clamp(28px, 4vw, 48px);
  font-weight: 400;
  line-height: 1.28;
  color: var(--color-text-primary);
  letter-spacing: 0.01em;
  margin-bottom: 32px;
}

.article-divider {
  height: 1px;
  background: var(--color-border);
  margin-bottom: 36px;
}

.article-footer {
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
}

.article-footer__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.footer-tag {
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  padding: 4px 12px;
  border-radius: var(--radius-tag);
  border: 1px solid var(--color-border);
}

@media (max-width: 768px) {
  .article-layout {
    padding: 40px 20px 80px;
  }
}
</style>
