<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="hero container" aria-labelledby="hero-title">
    <div class="hero__copy">
      <p class="journal-kicker">HARRY'S PERSONAL JOURNAL</p>
      <h1 id="hero-title">
        边折腾<span class="hero__comma">，</span><br />边<span class="hero__emphasis">记录。</span>
      </h1>
      <div class="hero__intro">
        <span class="hero__intro-rule" aria-hidden="true"></span>
        <p>我是 Harry，一名后端工程师。<br />爱折腾，也在奔波里记录生活的边角料。</p>
      </div>
      <a href="#latest" class="journal-link hero__read"
        >从最近一篇读起 <span aria-hidden="true">↓</span></a
      >
    </div>

    <RouterLink to="/articles" class="hero__edition" aria-label="打开 BitLog 全部文章">
      <div class="hero__edition-top">
        <span>BITLOG / <span class="hero__edition-name">随写随记</span></span
        ><span aria-hidden="true">↗</span>
      </div>
      <div class="hero__letter" aria-hidden="true">b<span>.</span></div>
      <div class="hero__edition-bottom">
        <span>Bits of code.<br />Notes on life.</span>
        <span class="hero__edition-index" aria-hidden="true">[ B—L ]</span>
      </div>
    </RouterLink>
  </section>
</template>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.66fr);
  align-items: start;
  gap: 72px;
  padding-top: calc(var(--spacing-header-height) + var(--spacing-page-head-top));
  padding-bottom: 64px;
}
.hero__copy {
  min-width: 0;
}
h1 {
  font-family: var(--font-display);
  font-size: clamp(44px, 5.6vw, 76px);
  font-weight: 600;
  line-height: 1.32;
  letter-spacing: -0.045em;
  margin: var(--spacing-page-title-gap) 0 26px;
}
.hero__comma {
  margin-right: -0.5em;
}
.hero__emphasis {
  color: var(--color-accent);
}
.hero__intro {
  display: flex;
  align-items: stretch;
  gap: 16px;
  color: var(--color-text-secondary);
  font-size: 15px;
  line-height: 1.9;
}
.hero__intro-rule {
  width: 1px;
  background: var(--color-border-strong);
  flex: none;
}
.hero__read {
  margin-top: 28px;
  min-width: 192px;
}
.hero__edition {
  background: var(--journal-blue);
  color: var(--journal-blue-ink);
  aspect-ratio: 1 / 1.03;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  position: relative;
  isolation: isolate;
  overflow: hidden;
  transition: background 0.3s;
}
.hero__edition::after {
  content: '';
  position: absolute;
  inset: 18px;
  border: 1px solid rgb(255 255 255 / 18%);
  pointer-events: none;
  z-index: -1;
}
.hero__edition-top,
.hero__edition-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-family: var(--font-mono);
  font-size: 12px;
  letter-spacing: 0.035em;
  z-index: 1;
}
.hero__edition-top > :last-child {
  font: 26px var(--font-sans);
}
.hero__letter {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: var(--font-editorial);
  font-style: italic;
  font-size: clamp(180px, 24vw, 320px);
  line-height: 0.85;
  letter-spacing: -0.12em;
  padding-right: 0.1em;
  padding-bottom: 0.08em;
  transition: transform 0.6s var(--ease-out-expo);
}
.hero__letter span {
  color: #c7ff8b;
}
.hero__edition-bottom {
  align-items: flex-end;
  line-height: 1.7;
}
.hero__edition-index {
  font-size: 12px;
}
.hero__edition:hover .hero__letter {
  transform: rotate(-5deg) scale(1.04);
}
.hero__edition:hover {
  background: #2342c4;
}
@media (min-width: 1600px) {
  .hero {
    gap: 100px;
  }
}
@media (max-width: 960px) {
  .hero {
    gap: 40px;
    grid-template-columns: minmax(0, 1.15fr) minmax(230px, 0.85fr);
  }
  h1 {
    font-size: clamp(40px, 5.4vw, 64px);
  }
  .hero__edition {
    padding: 24px;
  }
}
@media (max-width: 640px) {
  .hero {
    grid-template-columns: minmax(0, 1fr);
    padding-bottom: 40px;
    gap: 32px;
  }
  h1 {
    font-size: clamp(44px, 10vw, 62px);
    margin: var(--spacing-page-title-gap) 0 22px;
  }
  .hero__intro {
    font-size: 14px;
  }
  .hero__read {
    margin-top: 20px;
  }
  .hero__edition {
    aspect-ratio: auto;
    min-height: 190px;
    padding: 22px 26px;
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    grid-template-areas: 'label letter' 'note letter';
    gap: 24px 8px;
  }
  .hero__edition-top {
    grid-area: label;
    align-self: start;
  }
  .hero__edition-top > :last-child {
    display: none;
  }
  .hero__edition-name {
    display: block;
  }
  .hero__edition-bottom {
    grid-area: note;
    align-self: end;
  }
  .hero__letter {
    grid-area: letter;
    min-width: 0;
    font-size: clamp(140px, 40vw, 220px);
    padding: 0;
  }
  .hero__edition-index {
    display: none;
  }
}
</style>
