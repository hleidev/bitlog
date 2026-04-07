<template>
  <footer class="footer">
    <div class="footer__inner">
      <p class="footer__name">BitLog</p>
      <p class="footer__copy">© 2024 BitLog · 用心记录，用代码表达</p>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  background: var(--color-bg);
  border-top: 1px solid var(--color-border);
  padding: 40px 24px;
  text-align: center;
}

.footer__name {
  font-family: var(--font-serif);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.footer__copy {
  font-size: 13px;
  color: var(--color-text-faint);
}
</style>
