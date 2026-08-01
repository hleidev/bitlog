<script setup lang="ts">
defineProps<{ disabled?: boolean }>()
defineEmits<{ click: [] }>()
</script>

<template>
  <button
    type="button"
    class="google-btn"
    :disabled="disabled"
    aria-label="使用 Google 账号登录"
    title="使用 Google 账号登录"
    @click="$emit('click')"
  >
    <!-- Google 品牌规范要求 G 标使用未经修改的官方四色版本，不得改色或替换为单色 -->
    <svg class="google-btn__logo" viewBox="0 0 48 48" aria-hidden="true">
      <path
        fill="#EA4335"
        d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
      />
      <path
        fill="#4285F4"
        d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"
      />
      <path
        fill="#FBBC05"
        d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"
      />
      <path
        fill="#34A853"
        d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
      />
    </svg>
  </button>
</template>

<style scoped>
/*
 * 固定白底不跟随暗色主题：Google 品牌规范只允许官方三套配色，
 * 套用站点的 terracotta 或深色反转都属于违规改造
 */
.google-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  background: #ffffff;
  transition: background 0.15s ease;
}

.google-btn:hover:not(:disabled) {
  background: #f7f8f8;
}

.google-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.google-btn__logo {
  width: 20px;
  height: 20px;
}
</style>
