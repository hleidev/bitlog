<template>
  <section id="hero" class="hero">
    <div class="hero__after"></div>
    <div class="hero__inner">
      <div class="hero__left">
        <h1 class="hero__title">
          <span class="hero__char" style="animation-delay: 0.08s">B</span>
          <span class="hero__char" style="animation-delay: 0.14s">i</span>
          <span class="hero__char" style="animation-delay: 0.2s">t</span>
          <span class="hero__char" style="animation-delay: 0.26s">L</span>
          <span class="hero__char" style="animation-delay: 0.32s">o</span>
          <span class="hero__char" style="animation-delay: 0.38s">g</span>
          <span class="hero__char hero__dot" style="animation-delay: 0.46s">.</span>
        </h1>
        <p class="hero__subtitle">落笔有痕，代码留迹。</p>
      </div>

      <div class="hero__right">
        <div class="hero__author-header">
          <div class="hero__accent-bar"></div>
          <div class="hero__author-name">Harry</div>
        </div>
        <div class="hero__author-bio">后端工程师，<br />在奔波里记录生活的边角料。</div>
        <div class="hero__author-links">
          <a
            href="https://github.com/hleidev"
            target="_blank"
            rel="noopener noreferrer"
            class="hero__link hero__link--icon"
            aria-label="GitHub"
          >
            <svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <path
                d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"
              />
            </svg>
          </a>
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
  min-height: 52vh;
  display: flex;
  align-items: flex-end;
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid var(--color-border);
}

/* Noise grain texture */
.hero::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='280' height='280'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.78' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='280' height='280' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 280px 280px;
  opacity: 0.045;
  mix-blend-mode: overlay;
}

.hero__after {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 55% 70% at 5% 110%, rgba(255, 255, 255, 0.05) 0%, transparent 60%),
    linear-gradient(to bottom, transparent 55%, rgba(255, 255, 255, 0.08) 100%);
  pointer-events: none;
}

[data-theme='dark'] .hero__after {
  background: linear-gradient(to bottom, transparent 55%, rgba(0, 0, 0, 0.3) 100%);
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
  font-size: clamp(100px, 16vw, 260px);
  font-weight: 500;
  line-height: 1;
  color: var(--color-text-on-dark);
  letter-spacing: 0.03em;
  margin-bottom: 24px;
}

.hero__char {
  display: inline-block;
  opacity: 0;
  animation: fadeUp 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.hero__dot {
  color: var(--color-accent-on-dark);
}

.hero__subtitle {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 15px;
  /* 站点标语是正文内容：0.32 合成后只有 2.73；0.52 为 5.45。 */
  color: rgba(var(--color-on-dark-rgb), 0.52);
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

.hero__author-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero__accent-bar {
  width: 2.5px;
  height: 28px;
  background: var(--color-accent-on-dark);
  border-radius: var(--radius-tag);
  flex-shrink: 0;
  opacity: 0.8;
}

.hero__author-name {
  font-family: var(--font-serif);
  font-size: 14px;
  font-weight: 500;
  color: rgba(var(--color-on-dark-rgb), 0.75);
  letter-spacing: 0.04em;
}

.hero__author-bio {
  font-size: 13px;
  line-height: 1.75;
  /* 0.3 合成到 hero 深底上只有 2.53，不达 AA；0.5 为 5.15，仍明显弱于作者名。 */
  color: rgba(var(--color-on-dark-rgb), 0.5);
  letter-spacing: 0.01em;
  padding-left: 13px;
}

.hero__author-links {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
  padding-left: 13px;
}

.hero__link {
  font-size: 11.5px;
  letter-spacing: 0.08em;
  /* 这两个是真链接，0.35 只有 3.05；0.6 为 7.0，且仍低于 hover 的 0.8。 */
  color: rgba(var(--color-on-dark-rgb), 0.6);
  transition: color var(--transition-base);
}

.hero__link:hover {
  color: rgba(var(--color-on-dark-rgb), 0.8);
}

.hero__link--icon {
  display: inline-flex;
  align-items: center;
}

.hero__link--icon svg {
  width: 15px;
  height: 15px;
  display: block;
}

.hero__sep {
  font-size: 10px;
  color: rgba(var(--color-on-dark-rgb), 0.18);
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
