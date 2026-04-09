<template>
  <div class="not-found">
    <div class="not-found__code">404</div>
    <p class="not-found__msg">你访问的页面不存在</p>
    <a href="/" class="not-found__link">返回首页</a>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 80px 20px;
}

.not-found__code {
  font-size: 96px;
  font-weight: 800;
  color: var(--color-border);
  line-height: 1;
}

.not-found__msg {
  font-size: 16px;
  color: var(--color-text-muted);
}

.not-found__link {
  font-size: 14px;
  color: var(--color-accent);
  text-decoration: none;
  padding: 8px 20px;
  border: 1px solid var(--color-accent);
  border-radius: 20px;
  transition: all 0.2s;
}

.not-found__link:hover {
  background: var(--color-accent);
  color: #fff;
}
</style>
