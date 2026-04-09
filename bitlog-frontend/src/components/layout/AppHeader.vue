<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useHeaderScroll } from '@/composables/useHeaderScroll'
import { useModalStore } from '@/stores/useModalStore'
import { useUserStore } from '@/stores/useUserStore'
import UserDropdown from '@/components/common/UserDropdown.vue'

const { isScrolled } = useHeaderScroll()
const modalStore = useModalStore()
const userStore = useUserStore()
const { userInfo, isLoggedIn } = storeToRefs(userStore)

const isDark = ref(false)
const mobileMenuOpen = ref(false)
const dropdownOpen = ref(false)

const closeDropdown = () => {
  dropdownOpen.value = false
}

const toggleTheme = () => {
  isDark.value = !isDark.value
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
}
</script>

<template>
  <header class="header" :class="{ 'header--scrolled': isScrolled }">
    <div class="header__inner">
      <RouterLink to="/" class="header__logo">
        <img src="@/assets/images/logo.jpeg" class="header__logo-icon" alt="Bitlog Logo" />
        <span class="header__logo-text">BitLog</span>
      </RouterLink>

      <nav class="header__nav">
        <RouterLink to="/" class="header__nav-link">首页</RouterLink>
        <RouterLink to="/about" class="header__nav-link">关于</RouterLink>
        <RouterLink to="/friends" class="header__nav-link">友链</RouterLink>
      </nav>

      <div class="header__actions">
        <svg class="header__search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <!-- 主题切换 -->
        <button class="header__theme-btn" @click="toggleTheme" :aria-label="isDark ? '切换亮色' : '切换暗色'">
          <svg v-if="!isDark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
        </button>
        <!-- 登录按钮 / 用户头像 -->
        <button v-if="!isLoggedIn" class="header__login-btn" @click="modalStore.open('login')">登录</button>
        <template v-else>
          <div class="header__user" @mouseenter="dropdownOpen = true" @mouseleave="dropdownOpen = false">
            <button class="header__avatar-btn" aria-label="用户菜单">
              <img
                v-if="userInfo?.avatar"
                :src="userInfo.avatar"
                class="header__avatar"
                :alt="userInfo?.userName"
              />
              <span v-else class="header__avatar header__avatar--placeholder">
                {{ userInfo?.userName?.[0]?.toUpperCase() ?? '?' }}
              </span>
            </button>
            <Transition name="dropdown">
              <UserDropdown
                v-if="dropdownOpen"
                show-admin-links
                class="header__dropdown"
              />
            </Transition>
          </div>
        </template>
      </div>

      <!-- Mobile hamburger -->
      <button class="header__hamburger" @click="mobileMenuOpen = !mobileMenuOpen" aria-label="菜单">
        <span></span><span></span><span></span>
      </button>
    </div>

    <!-- Mobile drawer -->
    <div class="mobile-drawer" :class="{ 'mobile-drawer--open': mobileMenuOpen }">
      <nav class="mobile-drawer__nav">
        <RouterLink to="/" class="mobile-drawer__link" @click="mobileMenuOpen = false">首页</RouterLink>
        <RouterLink to="/about" class="mobile-drawer__link" @click="mobileMenuOpen = false">关于</RouterLink>
        <RouterLink to="/friends" class="mobile-drawer__link" @click="mobileMenuOpen = false">友链</RouterLink>
      </nav>
      <div class="mobile-drawer__actions">
        <button v-if="!isLoggedIn" class="mobile-login-btn" @click="modalStore.open('login'); mobileMenuOpen = false">登录</button>
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
  transition: background var(--transition-header), box-shadow var(--transition-header), border-color var(--transition-header);
  border-bottom: 1px solid transparent;
}

.header--scrolled {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom-color: var(--color-border);
  box-shadow: 0 1px 20px rgba(0, 0, 0, 0.06);
}

[data-theme='dark'] .header--scrolled {
  background: rgba(15, 17, 23, 0.90);
  box-shadow: 0 1px 20px rgba(0, 0, 0, 0.25);
}

.header__inner {
  height: 100%;
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
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  flex-shrink: 0;
}

.header__logo-text {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  transition: color var(--transition-header);
}

.header--scrolled .header__logo-text {
  color: #1a1a1a;
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
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  position: relative;
  padding: 4px 0;
  transition: color var(--transition-header);
}

.header--scrolled .header__nav-link {
  color: #374151;
}

.header__nav-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 1.5px;
  background: currentColor;
  transition: width 0.3s ease;
}

.header__nav-link:hover::after {
  width: 100%;
}

.header__actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-left: auto;
}

.header__search-icon {
  width: 20px;
  height: 20px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.9);
  transition: color var(--transition-header);
  flex-shrink: 0;
}

.header--scrolled .header__search-icon {
  color: #374151;
}

.header__theme-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: rgba(255, 255, 255, 0.9);
  transition: color var(--transition-header), transform var(--transition-base);
  flex-shrink: 0;
}

.header__theme-btn svg {
  width: 18px;
  height: 18px;
}

.header__theme-btn:hover {
  transform: scale(1.15);
}

.header--scrolled .header__theme-btn {
  color: #374151;
}

.header__login-btn {
  font-size: 13px;
  padding: 5px 18px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
  font-family: inherit;
  cursor: pointer;
}

.header__login-btn:hover {
  background: rgba(255, 255, 255, 0.9);
  color: #1a1a1a;
}

.header--scrolled .header__login-btn {
  border-color: #374151;
  color: #374151;
}

.header--scrolled .header__login-btn:hover {
  background: #374151;
  color: #fff;
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
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.header__avatar--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4a8db7, #2d6a9f);
  color: #fff;
  font-size: 13px;
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
  transition: opacity 0.15s ease, transform 0.15s ease;
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
  background: #fff;
  border-radius: 2px;
  transition: all 0.3s ease;
  transform-origin: center;
}

.header--scrolled .header__hamburger span {
  background: #1a1a1a;
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
  font-size: 18px;
  color: #1a1a1a;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
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
  border-radius: 10px;
  border: 1px solid #d1d9e6;
  font-size: 14px;
  color: #374151;
  text-align: center;
  transition: all var(--transition-base);
}

.mobile-login-btn:hover {
  background: #f9f8f5;
  border-color: var(--color-accent);
  color: var(--color-accent);
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
