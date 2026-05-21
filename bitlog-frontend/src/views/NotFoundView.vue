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
  font-family: var(--font-serif);
  font-size: 96px;
  font-weight: 400;
  color: var(--color-border);
  line-height: 1;
}

.not-found__msg {
  font-size: 16px;
  color: var(--color-text-muted);
}

.not-found__link {
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  text-decoration: none;
  background-image: linear-gradient(var(--color-accent), var(--color-accent));
  background-repeat: no-repeat;
  background-size: 0% 1px;
  background-position: left bottom;
  padding-bottom: 1px;
  transition: color var(--transition-base), background-size var(--transition-sweep);
}

.not-found__link:hover {
  color: var(--color-text-secondary);
  background-size: 100% 1px;
}
</style>
