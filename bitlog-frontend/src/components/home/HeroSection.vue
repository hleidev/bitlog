<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import heroImage from '@/assets/images/hero.png'

const heroRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
let animationId: number

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  opacity: number
}

const scrollToContent = () => {
  const contentEl = document.getElementById('content-area')
  if (!contentEl) return
  const top = contentEl.getBoundingClientRect().top + window.scrollY
  window.scrollTo({ top, behavior: 'smooth' })
}

const resize = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  resize()
  window.addEventListener('resize', resize)

  const particles: Particle[] = Array.from({ length: 60 }, () => ({
    x: Math.random() * canvas.width,
    y: Math.random() * canvas.height,
    vx: (Math.random() - 0.5) * 0.4,
    vy: (Math.random() - 0.5) * 0.4,
    size: Math.random() * 2 + 0.5,
    opacity: Math.random() * 0.4 + 0.1,
  }))

  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    particles.forEach(p => {
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > canvas.width) p.vx *= -1
      if (p.y < 0 || p.y > canvas.height) p.vy *= -1
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(255,255,255,${p.opacity})`
      ctx.fill()
    })
    animationId = requestAnimationFrame(animate)
  }
  animate()
})

onUnmounted(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', resize)
})
</script>

<template>
  <section class="hero" id="hero" ref="heroRef">
    <img
      class="hero__bg"
      :src="heroImage"
      alt=""
      aria-hidden="true"
    />
    <div class="hero__overlay"></div>
    <canvas ref="canvasRef" class="hero__canvas"></canvas>

    <div class="hero__content">
      <h1 class="hero__title">BitLog</h1>
      <p class="hero__slogan">落笔有痕，代码留迹。</p>
      <p class="hero__notice">内容迁移中，敬请期待</p>
      <div class="hero__socials">
        <a href="#" aria-label="GitHub">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/></svg>
        </a>
        <a href="#" aria-label="X (Twitter)">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
        </a>
        <a href="#" aria-label="RSS">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 11a9 9 0 0 1 9 9"/><path d="M4 4a16 16 0 0 1 16 16"/><circle cx="5" cy="19" r="1" fill="currentColor" stroke="none"/></svg>
        </a>
      </div>
    </div>

    <div class="hero__scroll" @click="scrollToContent">
      <span class="hero__scroll-text">Scroll</span>
      <div class="hero__scroll-line"></div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  position: relative;
  height: 100vh;
  overflow: hidden;
}

.hero__bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transform-origin: center center;
  animation: kenBurns 20s ease-in-out infinite alternate;
}

@keyframes kenBurns {
  from { transform: scale(1); }
  to { transform: scale(1.08); }
}

.hero__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(10, 25, 50, 0.35) 0%,
    rgba(15, 35, 65, 0.45) 60%,
    rgba(20, 40, 75, 0.60) 100%
  );
}

.hero__canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 2;
}

.hero__content {
  position: relative;
  z-index: 3;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
}

@keyframes heroReveal {
  from {
    opacity: 0;
    transform: translateY(40px);
    filter: blur(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

.hero__title {
  font-family: var(--font-serif);
  font-size: 72px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 10px;
  animation: heroReveal 1.4s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both;
}

.hero__slogan {
  font-family: var(--font-serif);
  font-size: 16px;
  color: rgba(255, 255, 255, 0.75);
  font-style: italic;
  letter-spacing: 5px;
  margin-top: 24px;
  animation: heroReveal 1.4s cubic-bezier(0.16, 1, 0.3, 1) 0.7s both;
}

.hero__notice {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 2px;
  margin-top: 12px;
  animation: heroReveal 1.4s cubic-bezier(0.16, 1, 0.3, 1) 0.9s both;
}

.hero__socials {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 28px;
  animation: heroReveal 1.4s cubic-bezier(0.16, 1, 0.3, 1) 1.0s both;
}

.hero__socials a {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0.85;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.hero__socials a:hover {
  transform: scale(1.2);
  opacity: 1;
}

.hero__socials svg {
  width: 20px;
  height: 20px;
}

.hero__scroll {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 16px 32px;
}

.hero__scroll-text {
  font-size: 12px;
  letter-spacing: 5px;
  color: rgba(255, 255, 255, 0.75);
  text-transform: uppercase;
}

.hero__scroll-line {
  width: 1.5px;
  height: 64px;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.8), transparent);
  position: relative;
  overflow: hidden;
}

.hero__scroll-line::after {
  content: '';
  position: absolute;
  top: -100%;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to bottom, transparent, #fff, transparent);
  animation: lineFlow 1.8s ease-in-out infinite;
}

@keyframes lineFlow {
  0% { top: -100%; }
  100% { top: 100%; }
}

@media (max-width: 768px) {
  .hero__title {
    font-size: 40px;
    letter-spacing: 4px;
  }
  .hero__slogan {
    font-size: 14px;
    letter-spacing: 2px;
  }
}
</style>
