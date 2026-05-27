<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import LoginModal from '@/components/common/LoginModal.vue'

const showBackTop = ref(false)

const onScroll = () => {
  showBackTop.value = window.scrollY > 400
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <AppHeader />
  <RouterView />
  <AppFooter />
  <LoginModal />
  <Transition name="back-top">
    <button v-if="showBackTop" class="back-top" @click="scrollToTop" aria-label="回到顶部">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/>
      </svg>
    </button>
  </Transition>
</template>

<style scoped>
.back-top {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface, #f4f2ef);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  color: var(--color-text-muted);
  cursor: pointer;
  z-index: 200;
  transition: color var(--transition-base), border-color var(--transition-base), background var(--transition-base);
}

.back-top svg {
  width: 15px;
  height: 15px;
  display: block;
}

.back-top:hover {
  color: var(--color-accent);
  border-color: var(--color-accent);
}

.back-top-enter-active,
.back-top-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.back-top-enter-from,
.back-top-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 768px) {
  .back-top {
    bottom: 24px;
    right: 20px;
  }
}
</style>
