<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import HeroSection from '@/components/home/HeroSection.vue'
import ArticleCard from '@/components/home/ArticleCard.vue'
import HomeSidebar from '@/components/home/HomeSidebar.vue'
import { getArticlePage, type ArticleItemVO } from '@/api/article'

const articles = ref<ArticleItemVO[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getArticlePage({ pageNum: 1, pageSize: 5 })
    articles.value = res.content
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main>
    <HeroSection />

    <div id="content-area" class="home-content">
      <div class="home-content__inner container">
        <section class="home-content__main">
          <div class="section-header">
            <span class="section-title">最新文章</span>
            <RouterLink to="/articles" class="section-more">查看全部 →</RouterLink>
          </div>
          <div class="article-list">
            <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
          </div>
        </section>

        <HomeSidebar />
      </div>
    </div>
  </main>
</template>

<style scoped>
.home-content {
  background: var(--color-bg);
  border-radius: 28px 28px 0 0;
  position: relative;
  z-index: 10;
  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.12);
  padding: 80px 0 80px;
}

[data-theme='dark'] .home-content {
  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.40);
}

.home-content__inner {
  display: flex;
  gap: 48px;
  align-items: flex-start;
}

.home-content__main {
  flex: 1;
  min-width: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: 2px;
}

.section-more {
  font-size: 13px;
  color: var(--color-text-muted);
  transition: color var(--transition-base);
}

.section-more:hover {
  color: var(--color-accent);
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

@media (max-width: 900px) {
  .home-content__inner {
    flex-direction: column;
  }
}
</style>
