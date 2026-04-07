<script setup lang="ts">
import HeroSection from '@/components/home/HeroSection.vue'
import ArticleCard from '@/components/home/ArticleCard.vue'
import HomeSidebar from '@/components/home/HomeSidebar.vue'

const articles = [
  {
    id: 1,
    title: '深入理解 Java 虚拟机：从字节码到 JIT 编译器',
    summary: '本文从字节码层面出发，系统分析 JVM 的类加载机制、内存模型、垃圾回收算法，以及 JIT 编译器的工作原理和热点代码优化策略。',
    cover: 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=480&q=80',
    category: 'Java',
    date: '2024-12-01',
    views: 3842,
    likes: 126,
  },
  {
    id: 2,
    title: 'Vue 3 响应式原理深度解析：Proxy 与依赖追踪',
    summary: '深入探讨 Vue 3 响应式系统的实现原理，包括 Proxy 的工作机制、effect 函数的依赖收集与触发、以及与 Vue 2 的对比分析。',
    cover: 'https://images.unsplash.com/photo-1555099962-4199c345e5dd?w=480&q=80',
    category: '前端开发',
    date: '2024-11-28',
    views: 2917,
    likes: 98,
    readTime: 10,
  },
  {
    id: 3,
    title: 'MySQL 索引优化实战：从执行计划到查询调优',
    summary: '结合实际案例分析 MySQL 索引的工作机制，通过 EXPLAIN 解读执行计划，掌握覆盖索引、最左前缀原则等优化技巧。',
    cover: 'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=480&q=80',
    category: '后端技术',
    date: '2024-11-20',
    views: 4201,
    likes: 155,
    readTime: 15,
  },
  {
    id: 4,
    title: 'Docker 容器化实践：从开发到生产的完整流程',
    summary: '分享在实际项目中使用 Docker 进行容器化改造的完整经验，涵盖多阶段构建、Compose 编排、以及生产环境下的最佳实践。',
    cover: 'https://images.unsplash.com/photo-1605745341112-85968b19335b?w=480&q=80',
    category: '工具效率',
    date: '2024-11-15',
    views: 2156,
    likes: 84,
    readTime: 8,
  },
  {
    id: 5,
    title: 'Redis 分布式锁：设计原理与实现细节',
    summary: '从分布式场景下的并发问题出发，详细介绍基于 Redis 的分布式锁实现方案，分析 SETNX、Lua 脚本、Redisson 等方案的优缺点。',
    cover: 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=480&q=80',
    category: '后端技术',
    date: '2024-11-10',
    views: 3105,
    likes: 112,
    readTime: 11,
  },
]
</script>

<template>
  <main>
    <HeroSection />

    <div id="content-area" class="home-content">
      <div class="home-content__inner container">
        <section class="home-content__main">
          <div class="section-header">
            <span class="section-title">最新文章</span>
            <a href="/articles" class="section-more">查看全部 →</a>
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
