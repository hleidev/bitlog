<script setup lang="ts">
import { onMounted, onUnmounted, watch } from 'vue'

const props = defineProps<{
  open: boolean
  src?: string
  alt?: string
}>()

const emit = defineEmits<{ close: [] }>()

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.open) emit('close')
}

function lockScroll(lock: boolean) {
  document.body.style.overflow = lock ? 'hidden' : ''
}

watch(() => props.open, (open) => lockScroll(open))

onMounted(() => window.addEventListener('keydown', handleKeydown))
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  lockScroll(false)
})
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="img-lightbox" role="dialog" aria-modal="true" @click="emit('close')">
      <button class="img-lightbox__close" aria-label="关闭" @click.stop="emit('close')">×</button>
      <div class="img-lightbox__stage">
        <img v-if="src" :src="src" :alt="alt ?? ''" class="img-lightbox__img" @click.stop />
        <div v-else class="img-lightbox__html"><slot /></div>
      </div>
    </div>
  </Teleport>
</template>

<style>
.img-lightbox {
  position: fixed;
  inset: 0;
  z-index: 10000;
  padding: 48px 64px;
  background: rgba(15, 12, 10, 0.9);
  cursor: zoom-out;
  animation: img-lightbox-fade 0.15s ease-out;
}

.img-lightbox__close {
  position: fixed;
  top: 20px;
  right: 24px;
  width: 40px;
  height: 40px;
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: #f0ede8;
  border-radius: 50%;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
  font-family: var(--font-sans);
  transition: background 0.15s, transform 0.15s;
}

.img-lightbox__close:hover {
  background: rgba(255, 255, 255, 0.18);
  transform: scale(1.05);
}

.img-lightbox__stage {
  position: relative;
  width: 100%;
  height: 100%;
}

.img-lightbox__img {
  display: block;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 4px;
}

.img-lightbox__html {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.img-lightbox__html > * {
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
  max-height: 100%;
  width: 100%;
  height: 100%;
}

.img-lightbox__html svg,
.img-lightbox__html > * svg {
  display: block;
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
}

@keyframes img-lightbox-fade {
  from { opacity: 0; }
  to   { opacity: 1; }
}
</style>
