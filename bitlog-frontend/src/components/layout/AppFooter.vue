<script setup lang="ts">
const START_YEAR = 2026
const currentYear = new Date().getFullYear()
const copyrightYear = currentYear > START_YEAR ? `${START_YEAR}–${currentYear}` : `${START_YEAR}`
</script>

<template>
  <footer class="footer">
    <div class="footer__inner container">
      <div class="footer__top">
        <div>
          <p class="journal-kicker">UNTIL THE NEXT NOTE</p>
          <p class="footer__slogan">文字有尽，<span>折腾不止。</span></p>
        </div>
        <a
          class="journal-link"
          href="https://github.com/hleidev"
          target="_blank"
          rel="noopener noreferrer"
          >在 GitHub 找到我 <span aria-hidden="true">↗</span></a
        >
      </div>
      <div class="footer__bottom">
        <a href="/" class="footer__wordmark" aria-label="BitLog 首页">BitLog<span>.</span></a>
        <p>© {{ copyrightYear }} BitLog · Harry</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  border-top: 1px solid var(--color-border-strong);
}
.footer__inner {
  padding-top: 48px;
  padding-bottom: 24px;
}
.footer__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 32px;
  padding-bottom: 48px;
}
.footer__slogan {
  font: 500 clamp(26px, 3vw, 38px)/1.5 var(--font-display);
  margin-top: 20px;
  letter-spacing: -0.035em;
}
.footer__slogan span {
  color: var(--color-text-muted);
}
.footer__bottom {
  display: flex;
  align-items: center;
  gap: 32px;
  border-top: 1px solid var(--color-border);
  padding-top: 22px;
  color: var(--color-text-muted);
  font-size: 12px;
}
.footer__wordmark {
  font: 600 26px var(--font-editorial);
  color: var(--color-text-primary);
  letter-spacing: -0.06em;
  margin-right: auto;
}
.footer__wordmark span {
  color: var(--color-accent);
}
@media (max-width: 640px) {
  .footer__inner {
    padding-top: 32px;
  }
  .footer__top {
    align-items: flex-start;
    flex-direction: column;
    gap: 24px;
    padding-bottom: 32px;
  }
  .footer__bottom {
    gap: 16px;
    flex-wrap: wrap;
  }
}
</style>
