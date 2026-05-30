<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getArticleDraft } from '@/api/admin/article'
import type { ArticleDetailVO } from '@/api/admin/article'
import ProseContent from '@/components/ProseContent.vue'
import TocSidebar from '@/components/TocSidebar.vue'
import { useToc } from '@/composables/useToc'

const route = useRoute()
const article = ref<ArticleDetailVO | null>(null)
const loading = ref(true)
const error = ref(false)

const { toc, activeSection, visibleTocItems, buildToc, scrollToSection } = useToc()

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    article.value = await getArticleDraft(id)
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
  if (!article.value) return
  await nextTick()
  buildToc()
})
</script>

<template>
  <div class="preview-page">
    <div class="preview-bar">
      <span class="preview-badge">草稿预览</span>
      <span class="preview-bar-title">{{ article?.title ?? '' }}</span>
      <a href="/admin/articles" class="preview-back">返回后台</a>
    </div>

    <div v-if="loading" class="preview-state">加载中…</div>
    <div v-else-if="error" class="preview-state preview-state--error">文章加载失败</div>
    <div v-else class="preview-layout">
      <div class="preview-content">
        <h1 class="preview-title">{{ article?.title }}</h1>
        <div v-if="article?.tags?.length" class="preview-tags">
          <span v-for="tag in article.tags" :key="tag" class="preview-tag">{{ tag }}</span>
        </div>
        <ProseContent v-if="article" :content="article.content" />
      </div>

      <aside v-if="toc.length" class="toc-sidebar">
        <TocSidebar
          :items="visibleTocItems"
          :active-section="activeSection"
          @scroll-to="scrollToSection"
        />
      </aside>
    </div>
  </div>
</template>

<style scoped>
.preview-page {
  min-height: 100vh;
  background: var(--color-bg);
  font-family: var(--font-sans);
}

.preview-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 24px;
  height: 48px;
  background: #1f1f1f;
  border-bottom: 1px solid #333;
}

.preview-badge {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #febc2e;
  background: rgba(254, 188, 46, 0.12);
  border: 1px solid rgba(254, 188, 46, 0.3);
  padding: 2px 8px;
  border-radius: 3px;
}

.preview-bar-title {
  flex: 1;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-back {
  flex-shrink: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  text-decoration: none;
  transition: color 0.15s;
}

.preview-back:hover { color: rgba(255, 255, 255, 0.85); }

.preview-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 48px);
  font-size: 15px;
  color: var(--color-text-muted);
}

.preview-state--error { color: #e53e3e; }

.preview-layout {
  display: grid;
  grid-template-columns: 1fr 200px;
  gap: 48px;
  max-width: 1100px;
  margin: 0 auto;
  padding: 56px 24px 80px;
}

.preview-content { min-width: 0; }

.preview-title {
  font-family: var(--font-serif);
  font-size: 36px;
  font-weight: 500;
  color: var(--color-text-primary);
  line-height: 1.3;
  margin: 0 0 20px;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 36px;
}

.preview-tag {
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  border: 1px solid var(--color-border);
  padding: 2px 10px;
  border-radius: 2px;
}

.toc-sidebar {
  position: sticky;
  top: calc(48px + 24px);
  max-height: calc(100vh - 48px - 48px);
  align-self: start;
}

@media (max-width: 900px) {
  .preview-layout {
    grid-template-columns: 1fr;
    padding: 32px 16px 60px;
  }
  .toc-sidebar { display: none; }
  .preview-title { font-size: 26px; }
}
</style>
