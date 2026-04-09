<script setup lang="ts">
import { Document, User, ChatLineRound, TrendCharts } from '@element-plus/icons-vue'

const stats = [
  { label: '文章总数', value: '—', sub: '篇', icon: Document, color: '#4a8db7' },
  { label: '注册用户', value: '—', sub: '人', icon: User, color: '#52c41a' },
  { label: '评论总数', value: '—', sub: '条', icon: ChatLineRound, color: '#faad14' },
  { label: '总访问量', value: '—', sub: '次', icon: TrendCharts, color: '#eb2f96' },
]
</script>

<template>
  <div class="dashboard">
    <div class="stat-grid">
      <el-card v-for="item in stats" :key="item.label" class="stat-card" shadow="never">
        <div class="stat-content">
          <div class="stat-icon" :style="{ backgroundColor: item.color + '1a', color: item.color }">
            <el-icon :size="24"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">
              {{ item.value }}
              <span class="stat-unit">{{ item.sub }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <el-card shadow="never" class="placeholder-card">
      <template #header>
        <span class="card-title">最近文章</span>
      </template>
      <el-empty description="暂无数据" :image-size="80" />
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #262626;
  line-height: 1;
}

.stat-unit {
  font-size: 13px;
  font-weight: 400;
  color: #8c8c8c;
  margin-left: 2px;
}

.placeholder-card {
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
