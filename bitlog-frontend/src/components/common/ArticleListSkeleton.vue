<script setup lang="ts">
withDefaults(defineProps<{ rows?: number }>(), { rows: 6 })
</script>

<template>
  <div class="skeleton-list" aria-hidden="true">
    <div v-for="i in rows" :key="i" class="skeleton-row">
      <div class="sk sk-date" />
      <div class="sk-body">
        <div class="sk sk-title" />
        <div class="sk sk-excerpt" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-list {
  border-top: 1px solid var(--color-border);
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 28px 0;
  border-bottom: 1px solid var(--color-border);
}

.sk {
  border-radius: var(--radius-tag);
  background: linear-gradient(
    90deg,
    var(--color-bg-hover) 25%,
    var(--color-border) 50%,
    var(--color-bg-hover) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.sk-date {
  width: 80px;
  height: 11px;
  flex-shrink: 0;
}

.sk-body {
  flex: 1;
  max-width: 680px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sk-title {
  width: 60%;
  height: 18px;
}

.sk-excerpt {
  width: 90%;
  height: 13px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 640px) {
  .sk-date { display: none; }
}
</style>
