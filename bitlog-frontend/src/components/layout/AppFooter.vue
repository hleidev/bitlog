<script setup lang="ts">
const START_YEAR = 2026
const currentYear = new Date().getFullYear()
const copyrightYear = currentYear > START_YEAR ? `${START_YEAR}–${currentYear}` : `${START_YEAR}`
</script>

<template>
  <footer class="footer">
    <div class="footer__inner">
      <span class="footer__rule" aria-hidden="true"></span>
      <p class="footer__slogan">落笔有痕，代码留迹。</p>
      <p class="footer__copy">© {{ copyrightYear }} BitLog · Harry</p>
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
  padding: 48px var(--spacing-page-padding);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 13px;
}

.footer__rule {
  width: 32px;
  height: 1px;
  background: var(--color-border);
}

.footer__slogan {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 13.5px;
  color: var(--color-text-muted);
  letter-spacing: 0.06em;
}

.footer__copy {
  font-size: 11.5px;
  /* 版权是正文信息而非装饰：faint(#c4c4c4) 在白底上仅 1.74，远不达 AA。 */
  color: var(--color-text-muted);
  letter-spacing: 0.06em;
}

@media (max-width: 768px) {
  .footer__inner {
    padding: 40px 20px;
  }
}
</style>
