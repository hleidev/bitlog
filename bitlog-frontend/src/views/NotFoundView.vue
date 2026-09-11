<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useHead } from '@unhead/vue'

useHead({ title: '页面不存在 | BitLog', meta: [{ name: 'robots', content: 'noindex' }] })
</script>

<template>
  <main class="not-found container">
    <RouterLink to="/" class="not-found__brand">BitLog<span>.</span></RouterLink>
    <div class="not-found__body">
      <p class="journal-kicker">A PAGE NOT FOUND</p>
      <div class="not-found__code" aria-hidden="true">4<span>0</span>4</div>
      <h1>页面不存在</h1>
      <p class="not-found__msg">链接可能有误，或页面已被移除。</p>
      <RouterLink to="/articles" class="journal-link"
        >浏览文章 <span aria-hidden="true">↗</span></RouterLink
      >
    </div>
  </main>
</template>

<style scoped>
.not-found {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  padding-top: 28px;
  padding-bottom: 28px;
}
.not-found__brand {
  font: 600 28px var(--font-editorial);
  letter-spacing: -0.04em;
  width: fit-content;
}
.not-found__brand span {
  color: var(--color-accent);
}
.not-found__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  text-align: center;
}
.not-found__code {
  font: italic clamp(130px, 23vw, 280px)/1.1 var(--font-editorial);
  letter-spacing: -0.07em;
  color: var(--color-text-primary);
  margin: 24px 0;
}
.not-found__code > span {
  color: var(--color-accent);
}
h1 {
  font: 500 clamp(24px, 4vw, 36px) var(--font-display);
}
.not-found__msg {
  color: var(--color-text-muted);
  font-size: 14px;
  margin-top: 16px;
}
.journal-link {
  margin-top: 32px;
  min-width: 180px;
}
</style>
