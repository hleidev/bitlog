<script setup lang="ts">
import { computed } from 'vue'
import BaseModal from './BaseModal.vue'
import AuthForm from '@/components/auth/AuthForm.vue'
import { useModalStore } from '@/stores/useModalStore'

const modalStore = useModalStore()

const visible = computed(() => modalStore.visible && modalStore.activeModal === 'login')
</script>

<template>
  <BaseModal :visible="visible" aria-label="登录或注册" @close="modalStore.close()">
    <div class="auth-modal">
      <button class="auth-modal__close" aria-label="关闭" @click="modalStore.close()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M18 6 6 18M6 6l12 12" />
        </svg>
      </button>

      <AuthForm @success="modalStore.close()" />
    </div>
  </BaseModal>
</template>

<style scoped>
.auth-modal {
  padding: 28px 32px;
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
  color: var(--color-text-muted);
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
