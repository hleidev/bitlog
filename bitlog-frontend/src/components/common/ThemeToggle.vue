<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useTheme, type ThemeMode } from '@/composables/useTheme'

const { themeMode, isDark, setTheme } = useTheme()
const open = ref(false)
const wrapRef = ref<HTMLElement | null>(null)

function handleClickOutside(e: MouseEvent) {
  if (!wrapRef.value) return
  if (!wrapRef.value.contains(e.target as Node)) open.value = false
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))

function pick(mode: ThemeMode) {
  setTheme(mode)
  open.value = false
}
</script>

<template>
  <div ref="wrapRef" class="theme-toggle">
    <button
      class="theme-toggle__btn"
      :class="{ 'theme-toggle__btn--active': open }"
      :aria-label="isDark ? '切换为浅色主题' : '切换为深色主题'"
      :aria-expanded="open"
      @click="open = !open"
    >
      <svg v-if="themeMode === 'system'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/>
      </svg>
      <svg v-else-if="!isDark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
      </svg>
    </button>

    <Transition name="theme-menu">
      <div v-if="open" class="theme-toggle__menu" role="menu">
        <button class="theme-toggle__item" :class="{ 'is-active': themeMode === 'system' }" role="menuitem" @click="pick('system')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/>
          </svg>
          跟随系统
        </button>
        <button class="theme-toggle__item" :class="{ 'is-active': themeMode === 'light' }" role="menuitem" @click="pick('light')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
          </svg>
          浅色
        </button>
        <button class="theme-toggle__item" :class="{ 'is-active': themeMode === 'dark' }" role="menuitem" @click="pick('dark')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
          深色
        </button>
      </div>
    </Transition>
  </div>
</template>

<style>
/* Public-site uses surface tokens; admin overrides locally */
.theme-toggle { position: relative; }

.theme-toggle__btn {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
  background: transparent; border: none; cursor: pointer;
  color: var(--color-text-secondary, #5a5248);
  border-radius: 4px;
  transition: color 0.15s, background 0.15s;
}
.theme-toggle__btn svg { width: 17px; height: 17px; display: block; }
.theme-toggle__btn:hover,
.theme-toggle__btn--active {
  color: var(--color-accent, #b85c38);
  background: var(--color-bg-hover, #f0ede9);
}

.theme-toggle__menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 140px;
  background: var(--color-bg-card, #f4f2ef);
  border: 1px solid var(--color-border, #e8e4de);
  border-radius: 4px;
  padding: 4px;
  z-index: 1100;
  display: flex; flex-direction: column; gap: 2px;
}

.theme-toggle__item {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 10px;
  font-size: 13px; font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--color-text-primary, #1a1610);
  background: transparent; border: none; cursor: pointer; text-align: left;
  border-radius: var(--admin-radius);
  transition: background 0.12s, color 0.12s;
}
.theme-toggle__item svg { width: 15px; height: 15px; flex-shrink: 0; }
.theme-toggle__item:hover { background: var(--color-bg-hover, #f0ede9); }
.theme-toggle__item.is-active { color: var(--color-accent, #b85c38); font-weight: 500; }

.theme-menu-enter-active,
.theme-menu-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.theme-menu-enter-from,
.theme-menu-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
