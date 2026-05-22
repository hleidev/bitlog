<template>
  <section class="hero" id="hero">
    <div class="hero__after"></div>
    <div class="hero__inner">

      <div class="hero__left">
        <h1 class="hero__title">BitLog<span class="hero__dot">.</span></h1>
        <p class="hero__subtitle">落笔有痕，代码留迹。</p>
      </div>

      <div class="hero__right">
        <div class="hero__author-name">Harry</div>
        <div class="hero__author-bio">后端工程师，偶尔写前端。<br>记录技术思考，也记录那些值得留下来的字。</div>
        <div class="hero__author-links">
          <a href="#" class="hero__link">GitHub</a>
          <span class="hero__sep">·</span>
          <RouterLink to="/about" class="hero__link">关于我</RouterLink>
        </div>
      </div>

    </div>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<style scoped>
.hero {
  background: var(--color-hero-bg);
  min-height: 72vh;
  display: flex;
  align-items: flex-end;
  position: relative;
  border-bottom: 1px solid var(--color-border);
}

.hero__after {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, transparent 60%, rgba(255, 255, 255, 0.03) 100%);
  pointer-events: none;
}

.hero__inner {
  max-width: var(--spacing-container);
  margin: 0 auto;
  width: 100%;
  padding: 120px var(--spacing-page-padding) 80px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 60px;
  position: relative;
  z-index: 1;
}

/* ── Left ── */

.hero__left {
  flex: 1;
  min-width: 0;
}

.hero__title {
  font-family: var(--font-serif);
  font-size: clamp(80px, 13vw, 200px);
  font-weight: 500;
  line-height: 1;
  color: var(--color-text-on-dark);
  letter-spacing: 0.03em;
  margin-bottom: 24px;
  opacity: 0;
  animation: fadeUp 0.9s cubic-bezier(0.16, 1, 0.3, 1) 0.1s forwards;
}

.hero__dot {
  color: var(--color-accent);
}

.hero__subtitle {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 15px;
  color: rgba(245, 243, 239, 0.32);
  letter-spacing: 0.1em;
  opacity: 0;
  animation: fadeUp 0.9s cubic-bezier(0.16, 1, 0.3, 1) 0.35s forwards;
}

/* ── Right ── */

.hero__right {
  flex-shrink: 0;
  width: 260px;
  padding-bottom: 4px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  opacity: 0;
  animation: fadeUp 0.9s cubic-bezier(0.16, 1, 0.3, 1) 0.5s forwards;
}

.hero__author-name {
  font-family: var(--font-serif);
  font-size: 14px;
  font-weight: 500;
  color: rgba(245, 243, 239, 0.75);
  letter-spacing: 0.04em;
}

.hero__author-bio {
  font-size: 13px;
  line-height: 1.75;
  color: rgba(245, 243, 239, 0.3);
  letter-spacing: 0.01em;
}

.hero__author-links {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.hero__link {
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: rgba(245, 243, 239, 0.35);
  transition: color var(--transition-base);
}

.hero__link:hover {
  color: rgba(245, 243, 239, 0.8);
}

.hero__sep {
  font-size: 10px;
  color: rgba(245, 243, 239, 0.18);
}

/* ── Animation ── */

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ── Responsive ── */

@media (max-width: 960px) {
  .hero__right {
    width: 200px;
  }
}

@media (max-width: 768px) {
  .hero__inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 40px;
    padding: 100px 24px 60px;
  }

  .hero__right {
    width: 100%;
  }
}
</style>
