<script setup lang="ts">
import { computed, ref, nextTick, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useDropdown } from '@/composables/useDropdown'
import { useTheme } from '@/composables/useTheme'
import { useModalStore } from '@/stores/useModalStore'
import { useNotificationStore } from '@/stores/useNotificationStore'
import { useUserStore } from '@/stores/useUserStore'
import UserDropdown from '@/components/common/UserDropdown.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'

const { isDark, setTheme } = useTheme()
const modalStore = useModalStore()
const notificationStore = useNotificationStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { userInfo, isLoggedIn, sessionInitialized } = storeToRefs(userStore)
const { unreadCount } = storeToRefs(notificationStore)
const avatarFailed = ref(false)
watch(
  () => userInfo.value?.avatar,
  () => {
    avatarFailed.value = false
  },
)

const mobileMenuOpen = ref(false)
const menuTriggerRef = ref<HTMLButtonElement | null>(null)

function closeMobileMenu() {
  if (!mobileMenuOpen.value) return
  mobileMenuOpen.value = false
  menuTriggerRef.value?.focus({ preventScroll: true })
}

watch(
  () => route.fullPath,
  () => {
    mobileMenuOpen.value = false
    closeSearch()
  },
)
const searchOpen = ref(false)
const searchKeyword = ref('')
const searchInputRef = ref<HTMLInputElement | null>(null)
const searchTriggerRef = ref<HTMLButtonElement | null>(null)
const {
  isOpen: dropdownOpen,
  containerRef: dropdownRef,
  triggerRef: dropdownTriggerRef,
  close: closeDropdown,
  toggle: toggleDropdown,
} = useDropdown(route)
const mobileUnreadLabel = computed(() =>
  unreadCount.value > 99 ? '99+' : String(unreadCount.value),
)

const openSearch = async () => {
  mobileMenuOpen.value = false
  closeDropdown()
  searchOpen.value = true
  await nextTick()
  searchInputRef.value?.focus()
}

const closeSearch = () => {
  searchOpen.value = false
  searchKeyword.value = ''
}

const dismissSearch = async () => {
  closeSearch()
  await nextTick()
  searchTriggerRef.value?.focus({ preventScroll: true })
}

const handleEscape = () => {
  if (searchOpen.value) void dismissSearch()
  else closeMobileMenu()
}

const doSearch = () => {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  router.push({ path: '/articles', query: { keyword: kw } })
  closeSearch()
}

const handleMobileLogin = async () => {
  closeMobileMenu()
  await nextTick()
  modalStore.open('login')
}
</script>

<template>
  <header
    class="header header--scrolled"
    :class="{ 'header--search': searchOpen }"
    @keydown.esc="handleEscape"
  >
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
          placeholder="搜索文章…"
          aria-label="搜索文章"
          @keyup.enter="doSearch"
        />
        <button class="header__search-close" aria-label="关闭搜索" @click="dismissSearch">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <!-- Normal mode -->
      <template v-else>
        <RouterLink to="/" class="header__logo">
          <span class="header__logo-icon" aria-hidden="true">b.</span>
          <span class="header__logo-text"
            >BitLog<span class="header__logo-caption">随写随记</span></span
          >
        </RouterLink>

        <nav class="header__nav" aria-label="主导航">
          <RouterLink to="/" class="header__nav-link">首页</RouterLink>
          <RouterLink to="/articles" class="header__nav-link">文章</RouterLink>
          <RouterLink to="/friends" class="header__nav-link">友链</RouterLink>
        </nav>

        <div class="header__actions">
          <button
            ref="searchTriggerRef"
            class="header__search-btn"
            aria-label="搜索"
            @click="openSearch"
          >
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
            <div v-else ref="dropdownRef" class="header__user">
              <button
                ref="dropdownTriggerRef"
                class="header__avatar-btn"
                :aria-label="unreadCount > 0 ? '用户菜单，有未读通知' : '用户菜单'"
                aria-haspopup="menu"
                :aria-expanded="dropdownOpen"
                :aria-controls="dropdownOpen ? 'site-account-menu' : undefined"
                @click="toggleDropdown"
              >
                <img
                  v-if="userInfo?.avatar && !avatarFailed"
                  :src="userInfo.avatar"
                  class="header__avatar"
                  :alt="userInfo?.username"
                  @error="avatarFailed = true"
                />
                <span v-else class="header__avatar header__avatar--placeholder">
                  {{ userInfo?.username?.[0]?.toUpperCase() ?? '?' }}
                </span>
                <span v-if="unreadCount > 0" class="header__unread-dot" aria-hidden="true"></span>
              </button>
              <Transition name="dropdown">
                <UserDropdown
                  v-if="dropdownOpen"
                  id="site-account-menu"
                  show-admin-links
                  class="header__dropdown"
                  @close="closeDropdown"
                />
              </Transition>
            </div>
          </div>
        </div>

        <!-- Mobile hamburger -->
        <button
          ref="menuTriggerRef"
          class="header__hamburger"
          aria-label="菜单"
          aria-controls="mobile-navigation"
          :aria-expanded="mobileMenuOpen"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <span></span><span></span><span></span>
        </button>
      </template>
    </div>

    <!-- Mobile drawer -->
    <div
      id="mobile-navigation"
      class="mobile-drawer"
      :class="{ 'mobile-drawer--open': mobileMenuOpen }"
      :inert="!mobileMenuOpen"
    >
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
        <RouterLink
          v-if="isLoggedIn"
          to="/notifications"
          class="mobile-drawer__link"
          @click="mobileMenuOpen = false"
        >
          通知
          <span v-if="unreadCount > 0" class="mobile-drawer__unread">{{ mobileUnreadLabel }}</span>
        </RouterLink>
        <RouterLink
          v-if="isLoggedIn"
          to="/admin/profile"
          class="mobile-drawer__link"
          @click="mobileMenuOpen = false"
          >个人资料</RouterLink
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
    <div v-if="mobileMenuOpen" class="mobile-overlay" @click="closeMobileMenu"></div>
  </header>
</template>

<style scoped>
.header {
  position: fixed;
  inset: 0 var(--scrollbar-width, 0px) auto 0;
  height: var(--spacing-header-height);
  z-index: 1000;
  background: rgba(var(--color-bg-rgb), 0.96);
  border-bottom: 1px solid var(--color-border);
  backdrop-filter: blur(16px);
}
.header__inner {
  height: 100%;
  max-width: var(--spacing-container);
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 0 var(--spacing-page-padding);
}
.header__logo {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: none;
}
.header__logo-icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  background: var(--journal-blue);
  color: white;
  font: italic 30px/1 var(--font-editorial);
  padding: 0 5px 5px 0;
}
.header__logo-text {
  font: 600 23px/1 var(--font-editorial);
  letter-spacing: -0.04em;
  display: flex;
  align-items: center;
  gap: 18px;
}
.header__logo-caption {
  font: 12px var(--font-sans);
  letter-spacing: 0.14em;
  color: var(--color-text-muted);
  padding-left: 18px;
  border-left: 1px solid var(--color-border-strong);
}
.header__nav {
  display: flex;
  align-items: center;
  gap: 32px;
  margin-left: auto;
  height: 100%;
}
.header__nav-link {
  position: relative;
  display: flex;
  align-items: center;
  height: 100%;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.header__nav-link::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--color-accent);
  transform: scaleX(0);
  transition: transform 0.2s;
}
.header__nav-link:hover,
.header__nav-link.router-link-exact-active {
  color: var(--color-accent);
}
.header__nav-link.router-link-exact-active::after {
  transform: scaleX(1);
}
.header__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-left: 24px;
  border-left: 1px solid var(--color-border);
}
.header__search-btn,
.header__search-close {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  color: var(--color-text-secondary);
  flex: none;
}
.header__search-btn svg,
.header__search-close svg {
  width: 18px;
  height: 18px;
}
.header__search-btn:hover,
.header__search-close:hover {
  color: var(--color-accent);
  background: var(--color-bg-hover);
}
.header__search-mode {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
}
.header__search-mode > svg {
  width: 20px;
  height: 20px;
  color: var(--color-accent);
  flex: none;
}
.header__search-input {
  background: transparent;
  border: none;
  flex: 1;
  min-width: 0;
  color: var(--color-text-primary);
  font: 16px var(--font-sans);
}
.header__search-input::placeholder {
  color: var(--color-text-muted);
}
.header__login-btn {
  border: 1px solid var(--color-border-strong);
  padding: 6px 16px;
  min-height: 36px;
  font-size: 13px;
  color: var(--color-text-primary);
}
.header__login-btn:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.header__auth-slot {
  min-width: 60px;
  display: flex;
  justify-content: flex-end;
}
.header__auth-placeholder {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-bg-hover);
}
.header__user,
.header__avatar-btn {
  position: relative;
}
.header__avatar-btn {
  display: block;
  padding: 4px;
  border-radius: 50%;
}
.header__avatar-btn:hover,
.header__avatar-btn[aria-expanded='true'] {
  background: var(--color-bg-hover);
}
.header__avatar-btn:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}
.header__avatar {
  width: 32px;
  height: 32px;
  object-fit: cover;
  border-radius: 50%;
}
.header__avatar--placeholder {
  display: grid;
  place-items: center;
  background: var(--color-accent);
  color: var(--color-text-on-accent);
  font-size: 14px;
}
.header__unread-dot {
  position: absolute;
  top: -1px;
  right: -1px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-accent);
  box-shadow: 0 0 0 2px var(--color-bg);
}
.header__dropdown {
  position: absolute;
  top: calc(100% + 16px);
  right: 0;
  z-index: 1001;
}
.header__hamburger {
  display: none;
  width: 40px;
  height: 40px;
  padding: 9px;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
}
.header__hamburger span {
  display: block;
  width: 22px;
  height: 1.5px;
  background: var(--color-text-primary);
  transition: transform 0.2s;
}
.header__hamburger[aria-expanded='true'] span:first-child {
  transform: translateY(6.5px) rotate(45deg);
}
.header__hamburger[aria-expanded='true'] span:nth-child(2) {
  opacity: 0;
}
.header__hamburger[aria-expanded='true'] span:last-child {
  transform: translateY(-6.5px) rotate(-45deg);
}
.mobile-drawer {
  position: fixed;
  top: var(--spacing-header-height);
  right: 0;
  width: min(360px, 100vw);
  max-height: calc(100dvh - var(--spacing-header-height));
  overflow-y: auto;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-top: none;
  padding: 24px;
  z-index: 1001;
  visibility: hidden;
  transform: translateX(100%);
  transition:
    transform 0.25s,
    visibility 0.25s;
}
.mobile-drawer--open {
  visibility: visible;
  transform: none;
}
.mobile-drawer__nav {
  display: flex;
  flex-direction: column;
}
.mobile-drawer__link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border);
  font-size: 18px;
}
.mobile-drawer__link.router-link-exact-active {
  color: var(--color-accent);
}
.mobile-drawer__unread {
  padding: 2px 8px;
  background: var(--color-accent);
  color: var(--color-text-on-accent);
  font-size: 12px;
  border-radius: 20px;
}
.mobile-theme-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--color-text-secondary);
  font-size: 14px;
  padding: 18px 0;
}
.mobile-theme-btn svg {
  width: 18px;
  height: 18px;
}
.mobile-login-btn {
  width: 100%;
  background: var(--color-text-primary);
  color: var(--color-bg);
  padding: 12px;
  font-size: 14px;
}
.mobile-overlay {
  position: fixed;
  inset: var(--spacing-header-height) 0 0;
  height: 100dvh;
  background: rgb(0 0 0 / 24%);
  z-index: 1000;
}
.dropdown-enter-active,
.dropdown-leave-active {
  transition:
    opacity 0.15s,
    transform 0.15s;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
@media (max-width: 960px) {
  .header__logo-caption {
    display: none;
  }
  .header__inner {
    gap: 24px;
  }
}
@media (max-width: 768px) {
  .header__inner {
    gap: 8px;
  }
  .header__nav,
  .header__auth-slot {
    display: none;
  }
  .header__actions {
    margin-left: auto;
    border: none;
    padding: 0;
    gap: 4px;
  }
  .header__hamburger {
    display: flex;
    flex: none;
  }
  .header__logo-text {
    font-size: 22px;
  }
}
@media (min-width: 769px) {
  .mobile-drawer,
  .mobile-overlay {
    display: none;
  }
}
</style>
