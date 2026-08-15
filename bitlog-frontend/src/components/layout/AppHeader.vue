<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useHeaderScroll } from '@/composables/useHeaderScroll'
import { useTheme } from '@/composables/useTheme'
import { useModalStore } from '@/stores/useModalStore'
import { useUserStore } from '@/stores/useUserStore'
import UserDropdown from '@/components/common/UserDropdown.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'

const { isScrolled } = useHeaderScroll()
const { isDark, setTheme } = useTheme()
const modalStore = useModalStore()
const userStore = useUserStore()
const router = useRouter()
const { userInfo, isLoggedIn, sessionInitialized } = storeToRefs(userStore)

const mobileMenuOpen = ref(false)
const dropdownOpen = ref(false)
const searchOpen = ref(false)
const searchKeyword = ref('')
const searchInputRef = ref<HTMLInputElement | null>(null)

const openSearch = async () => {
  searchOpen.value = true
  await nextTick()
  searchInputRef.value?.focus()
}

const closeSearch = () => {
  searchOpen.value = false
  searchKeyword.value = ''
}

const doSearch = () => {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  router.push({ path: '/articles', query: { keyword: kw } })
  closeSearch()
}

const handleMobileLogin = () => {
  modalStore.open('login')
  mobileMenuOpen.value = false
}
</script>

<template>
  <header class="header" :class="{ 'header--scrolled': isScrolled, 'header--search': searchOpen }">
    <div class="header__inner">
      <!-- Search mode -->
      <div v-if="searchOpen" class="header__search-mode">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          ref="searchInputRef"
          v-model="searchKeyword"
          class="header__search-input"
          placeholder="搜索文章..."
          @keyup.enter="doSearch"
          @keyup.escape="closeSearch"
        />
        <button class="header__search-close" aria-label="关闭搜索" @click="closeSearch">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <!-- Normal mode -->
      <template v-else>
        <RouterLink to="/" class="header__logo">
          <img src="@/assets/images/logo.png" class="header__logo-icon" alt="Bitlog Logo" />
          <span class="header__logo-text">BitLog</span>
        </RouterLink>

        <nav class="header__nav">
          <RouterLink to="/" class="header__nav-link">首页</RouterLink>
          <RouterLink to="/articles" class="header__nav-link">文章</RouterLink>
          <RouterLink to="/friends" class="header__nav-link">友链</RouterLink>
        </nav>

        <div class="header__actions">
          <button class="header__search-btn" aria-label="搜索" @click="openSearch">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </button>
          <!-- 主题切换 -->
          <ThemeToggle />
          <!-- 会话恢复前保留固定空间，避免静态首屏的登录按钮再跳成头像。 -->
          <div class="header__auth-slot">
            <span
              v-if="!sessionInitialized || (isLoggedIn && !userInfo)"
              class="header__auth-placeholder"
              aria-hidden="true"
            ></span>
            <!-- 登录按钮 / 用户头像 -->
            <button
              v-else-if="!isLoggedIn"
              class="header__login-btn"
              @click="modalStore.open('login')"
            >
              登录
            </button>
            <div
              v-else
              class="header__user"
              @mouseenter="dropdownOpen = true"
              @mouseleave="dropdownOpen = false"
            >
              <button class="header__avatar-btn" aria-label="用户菜单">
                <img
                  v-if="userInfo?.avatar"
                  :src="userInfo.avatar"
                  class="header__avatar"
                  :alt="userInfo?.userName"
                />
                <span v-else class="header__avatar header__avatar--placeholder">
                  {{ userInfo?.username?.[0]?.toUpperCase() ?? '?' }}
                </span>
              </button>
              <Transition name="dropdown">
                <UserDropdown v-if="dropdownOpen" show-admin-links class="header__dropdown" />
              </Transition>
            </div>
          </div>
        </div>

        <!-- Mobile hamburger -->
        <button
          class="header__hamburger"
          aria-label="菜单"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <span></span><span></span><span></span>
        </button>
      </template>
    </div>

    <!-- Mobile drawer -->
    <div class="mobile-drawer" :class="{ 'mobile-drawer--open': mobileMenuOpen }">
      <nav class="mobile-drawer__nav">
        <RouterLink to="/" class="mobile-drawer__link" @click="mobileMenuOpen = false"
          >首页</RouterLink
        >
        <RouterLink to="/articles" class="mobile-drawer__link" @click="mobileMenuOpen = false"
          >文章</RouterLink
        >
        <RouterLink to="/friends" class="mobile-drawer__link" @click="mobileMenuOpen = false"
          >友链</RouterLink
        >
        <div class="mobile-theme-row">
          <button
            class="mobile-theme-btn"
            :class="{ 'mobile-theme-btn--active': isDark }"
            @click="setTheme(isDark ? 'light' : 'dark')"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path v-if="isDark" d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
              <g v-else>
                <circle cx="12" cy="12" r="5" />
                <path
                  d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"
                />
              </g>
            </svg>
            {{ isDark ? '深色' : '浅色' }}
          </button>
        </div>
      </nav>
      <div class="mobile-drawer__actions">
        <button v-if="!isLoggedIn" class="mobile-login-btn" @click="handleMobileLogin">登录</button>
      </div>
    </div>
    <div v-if="mobileMenuOpen" class="mobile-overlay" @click="mobileMenuOpen = false"></div>
  </header>
</template>

<style scoped>
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: var(--scrollbar-width, 0px);
  height: var(--spacing-header-height);
  z-index: 1000;
  transition:
    background var(--transition-header),
    border-color var(--transition-header);
  border-bottom: 1px solid transparent;
}

.header--scrolled {
  background: rgba(var(--color-bg-rgb), 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom-color: var(--color-border);
}

.header__inner {
  height: 100%;
  max-width: var(--spacing-container);
  margin: 0 auto;
  display: flex;
  align-items: center;
  position: relative;
  padding: 0 48px;
}

.header__logo {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.header__logo-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  flex-shrink: 0;
}

.header__logo-text {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-on-dark);
  transition: color var(--transition-header);
}

.header--scrolled .header__logo-text {
  color: var(--color-text-primary);
}

.header__nav {
  display: flex;
  align-items: center;
  gap: 36px;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.header__nav-link {
  font-size: 13px;
  letter-spacing: 0.04em;
  color: rgba(var(--color-on-dark-rgb), 0.7);
  padding-bottom: 1px;
  background-image: linear-gradient(var(--color-accent), var(--color-accent));
  background-repeat: no-repeat;
  background-size: 0% 1px;
  background-position: left bottom;
  transition:
    color var(--transition-header),
    background-size var(--transition-sweep);
}

.header--scrolled .header__nav-link {
  color: var(--color-text-secondary);
}

.header__nav-link:hover {
  background-size: 100% 1px;
}

.header__nav-link.router-link-exact-active {
  color: var(--color-text-on-dark);
  background-size: 100% 1px;
}

.header--scrolled .header__nav-link.router-link-exact-active {
  color: var(--color-text-primary);
}

.header__actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-left: auto;
}

.header__search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: rgba(var(--color-on-dark-rgb), 0.7);
  transition:
    color var(--transition-header),
    opacity var(--transition-base);
  flex-shrink: 0;
}

.header__search-btn svg {
  width: 17px;
  height: 17px;
}

.header__search-btn:hover {
  opacity: 0.65;
}

.header--scrolled .header__search-btn {
  color: var(--color-text-secondary);
}

/* Search mode */
.header--search {
  background: rgba(var(--color-bg-rgb), 0.96) !important;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom-color: var(--color-border) !important;
}

.header__search-mode {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
}

.header__search-mode svg {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
  color: var(--color-text-muted);
}

.header__search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 15px;
  color: var(--color-text-primary);
  font-family: var(--font-sans);
}

.header__search-input::placeholder {
  color: var(--color-text-faint);
}

.header__search-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: var(--color-text-muted);
  flex-shrink: 0;
  transition: color var(--transition-base);
}

.header__search-close:hover {
  color: var(--color-text-primary);
}

.header__search-close svg {
  width: 15px;
  height: 15px;
}

.header__login-btn {
  font-size: 13px;
  letter-spacing: 0.04em;
  white-space: nowrap;
  flex-shrink: 0;
  padding: 5px 14px;
  border-radius: 4px;
  border: 1px solid rgba(var(--color-on-dark-rgb), 0.35);
  background: transparent;
  color: rgba(var(--color-on-dark-rgb), 0.85);
  transition: all var(--transition-base);
  font-family: var(--font-sans);
  cursor: pointer;
}

.header__auth-slot {
  min-width: 60px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

/* 上次登录过：直接按头像尺寸留位，登录后头像左侧不会多出空当 */
[data-session='restoring'] .header__auth-slot {
  min-width: 0;
}

.header__auth-placeholder {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(var(--color-on-dark-rgb), 0.14);
  animation:
    auth-placeholder-in 160ms ease-out 200ms both,
    auth-placeholder-pulse 1.4s ease-in-out 360ms infinite;
}

.header--scrolled .header__auth-placeholder {
  background: var(--color-bg-hover);
}

@keyframes auth-placeholder-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes auth-placeholder-pulse {
  50% {
    opacity: 0.55;
  }
}

.header__login-btn:hover {
  background: rgba(255, 255, 255, 0.12);
}

.header--scrolled .header__login-btn {
  border-color: var(--color-border);
  color: var(--color-text-secondary);
}

.header--scrolled .header__login-btn:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-text-muted);
  color: var(--color-text-primary);
}

.header__user {
  position: relative;
}

.header__avatar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: opacity var(--transition-base);
}

.header__avatar-btn:hover {
  opacity: 0.85;
}

.header__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.header__avatar--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-accent);
  color: var(--color-text-on-accent);
  font-size: 12px;
  font-weight: 600;
}

.header__dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 1001;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* Hamburger */
.header__hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  width: 36px;
  height: 36px;
  margin-left: auto;
  padding: 4px;
}

.header__hamburger span {
  display: block;
  width: 22px;
  height: 1.5px;
  background: rgba(var(--color-on-dark-rgb), 0.85);
  transition: all 0.3s ease;
  transform-origin: center;
}

.header--scrolled .header__hamburger span {
  background: var(--color-text-primary);
}

/* Mobile Drawer */
.mobile-drawer {
  position: fixed;
  top: 0;
  right: 0;
  width: 260px;
  height: 100vh;
  background: var(--color-bg-card);
  border-left: 1px solid var(--color-border);
  padding: 80px 24px 40px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transform: translateX(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 999;
}

.mobile-drawer--open {
  transform: translateX(0);
}

.mobile-drawer__nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mobile-drawer__link {
  font-size: 16px;
  color: var(--color-text-primary);
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  transition: color var(--transition-base);
}

.mobile-drawer__link:hover {
  color: var(--color-accent);
}

.mobile-drawer__actions {
  margin-top: auto;
}

.mobile-login-btn {
  display: block;
  width: 100%;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid var(--color-border);
  font-size: 14px;
  color: var(--color-text-secondary);
  text-align: center;
  background: transparent;
  cursor: pointer;
  font-family: var(--font-sans);
  transition: all var(--transition-base);
}

.mobile-login-btn:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-text-muted);
  color: var(--color-text-primary);
}

/* ── Mobile theme options ── */
.mobile-theme-row {
  display: flex;
  flex-direction: column;
  border-bottom: 1px solid var(--color-border);
}

.mobile-theme-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  font-size: 15px;
  font-family: var(--font-sans);
  color: var(--color-text-muted);
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: color var(--transition-base);
}

.mobile-theme-btn svg {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
}

.mobile-theme-btn:hover {
  color: var(--color-text-primary);
}
.mobile-theme-btn--active {
  color: var(--color-accent);
}

/* 同上：.mobile-theme-btn:hover(0,2,0) 特异性更高，不覆盖会让选中项失去高亮 */
.mobile-theme-btn--active:hover {
  color: var(--color-accent-dark);
}

.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 998;
}

@media (max-width: 768px) {
  .header {
    height: 56px;
  }
  .header__inner {
    padding: 0 20px;
  }
  .header__nav,
  .header__actions {
    display: none;
  }
  .header__hamburger {
    display: flex;
  }
}
</style>
