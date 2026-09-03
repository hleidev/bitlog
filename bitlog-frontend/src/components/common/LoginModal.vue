<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import BaseModal from './BaseModal.vue'
import AuthForm from '@/components/auth/AuthForm.vue'
import { useModalStore } from '@/stores/useModalStore'

const modalStore = useModalStore()

const visible = computed(() => modalStore.visible && modalStore.activeModal === 'login')

// 关闭动画期间 slot 仍挂载，重开会复用旧实例，靠 key 强制重建以清空表单
const openCount = ref(0)
watch(visible, (val) => {
  if (val) openCount.value += 1
})
</script>

<template>
  <BaseModal :visible="visible" @close="modalStore.close()">
    <div class="auth-modal">
      <button class="auth-modal__close" aria-label="关闭" @click="modalStore.close()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M18 6 6 18M6 6l12 12" />
        </svg>
      </button>

      <AuthForm :key="openCount" @success="modalStore.close()" />
    </div>
  </BaseModal>
</template>

<style scoped>
.auth-modal {
  padding: 40px;
  position: relative;
}

.auth-modal__close {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: var(--color-text-faint);
  transition: all var(--transition-base);
}

.auth-modal__close svg {
  width: 16px;
  height: 16px;
}

.auth-modal__close:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}
</style>
