<template>
  <footer class="footer">
    <div class="footer__inner">
      <span class="footer__copy">© 2026 BitLog · Harry</span>
      <span class="footer__slogan">落笔有痕，代码留迹。</span>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  border-top: 1px solid var(--color-border);
}

.footer__inner {
  max-width: var(--spacing-container);
  margin: 0 auto;
  padding: 36px var(--spacing-page-padding);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.footer__copy {
  font-size: 12px;
  color: var(--color-text-faint);
  letter-spacing: 0.04em;
}

.footer__slogan {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 13px;
  color: var(--color-text-faint);
}

@media (max-width: 768px) {
  .footer__inner {
    flex-direction: column;
    gap: 8px;
    text-align: center;
    padding: 28px 20px;
  }
}
</style>
