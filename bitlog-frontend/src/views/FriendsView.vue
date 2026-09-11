<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useHead, useSeoMeta } from '@unhead/vue'
import { useConfirm } from '@/composables/useConfirm'
import { useModalStore } from '@/stores/useModalStore'
import { useUserStore } from '@/stores/useUserStore'
import { ApiError } from '@/utils/request'
import {
  applyFriendLink,
  deleteMyFriendLink,
  getFriendLinks,
  getMyFriendLink,
  updateMyFriendLink,
  LINK_STATUS,
  type FriendLinkVO,
  type LinkStatus,
  type MyFriendLinkVO,
} from '@/api/link'

const pageTitle = '友链 | BitLog'
const pageDescription = '一些博客，推荐给你。'

useHead({
  title: pageTitle,
  link: [{ rel: 'canonical', href: 'https://bitlog.harrylei.top/friends' }],
})
useSeoMeta({
  description: pageDescription,
  ogTitle: pageTitle,
  ogDescription: pageDescription,
  ogUrl: 'https://bitlog.harrylei.top/friends',
})

const userStore = useUserStore()
const modalStore = useModalStore()
const confirm = useConfirm()

const links = ref<FriendLinkVO[]>([])
const myLink = ref<MyFriendLinkVO | null>(null)
const loading = ref(true)
const linksError = ref(false)
const mineLoading = ref(true)
const mineError = ref(false)
const submitting = ref(false)
const removing = ref(false)
const formOpen = ref(false)
const feedback = ref('')
let pageActive = true
let accountVersion = 0
let mineRequestId = 0

const form = reactive({
  name: '',
  url: '',
  avatar: '',
  description: '',
  applyMessage: '',
})

const statusLabel: Record<LinkStatus, string> = {
  [LINK_STATUS.PENDING]: '待审核',
  [LINK_STATUS.APPROVED]: '展示中',
  [LINK_STATUS.REJECTED]: '未通过',
}

const panel = computed(() => {
  if (!userStore.isLoggedIn) return 'guest'
  if (formOpen.value) return 'form'
  // 没申请过也只给一行入口，不默认摊开表单——多数访客并不需要申请
  return myLink.value ? 'mine' : 'entry'
})

// ── 取数 ────────────────────────────────────────────────────────────────────

async function loadLinks() {
  const result = await getFriendLinks()
  if (!pageActive) return
  links.value = result
  linksError.value = false
}

async function refreshLinks() {
  loading.value = true
  linksError.value = false
  try {
    await loadLinks()
  } catch {
    linksError.value = true
  } finally {
    loading.value = false
  }
}

async function refreshMine() {
  if (!userStore.sessionInitialized || !userStore.isLoggedIn || !userStore.userInfo?.userId) return
  const version = accountVersion
  const requestId = ++mineRequestId
  const isCurrent = () => pageActive && version === accountVersion && requestId === mineRequestId
  mineLoading.value = true
  mineError.value = false
  try {
    const result = await getMyFriendLink()
    if (isCurrent()) myLink.value = result
  } catch {
    if (isCurrent()) mineError.value = true
  } finally {
    if (isCurrent()) mineLoading.value = false
  }
}

// 静默恢复、弹窗登录和账号切换共用一条状态同步路径，不依赖整页刷新。
watch(
  [
    () => userStore.sessionInitialized,
    () => userStore.isLoggedIn,
    () => userStore.userInfo?.userId,
  ],
  ([ready, loggedIn, userId]) => {
    accountVersion++
    mineRequestId++
    myLink.value = null
    formOpen.value = false
    resetForm()
    feedback.value = ''
    mineError.value = false
    submitting.value = false
    removing.value = false
    mineLoading.value = !ready || (loggedIn && !userId)
    if (ready && loggedIn && userId) void refreshMine()
  },
  { immediate: true },
)

onMounted(() => void refreshLinks())
onUnmounted(() => {
  pageActive = false
  accountVersion++
  mineRequestId++
})

// ── 头像 ────────────────────────────────────────────────────────────────────

/** 头像是外链，对方换域名或清 CDN 都会破图，挂了就退回站名首字 */
const brokenAvatars = ref(new Set<number>())

function markBroken(id: number) {
  brokenAvatars.value = new Set(brokenAvatars.value).add(id)
}

/**
 * 地址一律过协议白名单再进 :src / :href。
 * Vue 对这两个绑定不做净化，javascript: 开头的地址会变成公开页上一个可点的执行入口。
 */
const SAFE_PROTOCOLS = ['http:', 'https:']

function safeUrl(url?: string): string | undefined {
  if (!url) return undefined
  try {
    return SAFE_PROTOCOLS.includes(new URL(url).protocol) ? url : undefined
  } catch {
    return undefined
  }
}

function avatarSrc(link: FriendLinkVO) {
  return brokenAvatars.value.has(link.id) ? undefined : safeUrl(link.avatar)
}

function initial(name: string) {
  return name.trim().charAt(0) || '·'
}

// ── 操作 ────────────────────────────────────────────────────────────────────

function openLogin() {
  modalStore.open('login')
}

function resetForm() {
  form.name = ''
  form.url = ''
  form.avatar = ''
  form.description = ''
  form.applyMessage = ''
}

function startApply() {
  resetForm()
  feedback.value = ''
  formOpen.value = true
}

function startEdit() {
  if (myLink.value) {
    form.name = myLink.value.name
    form.url = myLink.value.url
    form.avatar = myLink.value.avatar ?? ''
    form.description = myLink.value.description ?? ''
    // 表单是整体替换提交，漏回填的字段会被提交的空值覆盖掉
    form.applyMessage = myLink.value.applyMessage ?? ''
  }
  feedback.value = ''
  formOpen.value = true
}

function closeForm() {
  feedback.value = ''
  formOpen.value = false
}

async function submit() {
  if (submitting.value) return
  const version = accountVersion
  submitting.value = true
  feedback.value = ''
  try {
    if (myLink.value) {
      await updateMyFriendLink({ ...form })
    } else {
      await applyFriendLink({ ...form })
    }
    if (!pageActive || version !== accountVersion) return
    formOpen.value = false
    await Promise.all([refreshMine(), refreshLinks()])
  } catch (err) {
    if (pageActive && version === accountVersion)
      feedback.value = err instanceof ApiError ? err.message : '提交失败，请稍后重试'
  } finally {
    if (version === accountVersion) submitting.value = false
  }
}

async function removeMine() {
  if (!myLink.value || removing.value) return
  const version = accountVersion
  const pending = myLink.value.status === LINK_STATUS.PENDING
  removing.value = true
  try {
    await confirm(
      pending ? '撤回这条友链申请？' : '删除后需要重新申请。',
      pending ? '撤回申请' : '删除友链',
      { confirmText: pending ? '撤回' : '删除', danger: true },
    )
  } catch {
    if (version === accountVersion) removing.value = false
    return
  }
  if (!pageActive || version !== accountVersion) return
  feedback.value = ''
  try {
    await deleteMyFriendLink()
    if (!pageActive || version !== accountVersion) return
    await Promise.all([refreshMine(), refreshLinks()])
  } catch (err) {
    if (pageActive && version === accountVersion)
      feedback.value = err instanceof ApiError ? err.message : '删除失败，请稍后重试'
  } finally {
    if (version === accountVersion) removing.value = false
  }
}
</script>

<template>
  <main class="friends-page">
    <div class="friends-main">
      <header class="journal-page-head">
        <div>
          <span class="journal-kicker">THE BLOGROLL</span>
          <h1>友链<span class="page-title-dot">.</span></h1>
        </div>
        <p>{{ pageDescription }}</p>
      </header>

      <p v-if="loading" class="empty">加载中…</p>

      <div v-else-if="linksError" class="empty" role="alert">
        <p>友链暂时没能加载出来。</p>
        <button class="journal-link" type="button" @click="refreshLinks">重新加载</button>
      </div>

      <p v-else-if="links.length === 0" class="empty">还没有友链。</p>

      <div v-else class="link-grid">
        <a
          v-for="(link, index) in links"
          :key="link.id"
          class="link-card"
          :href="safeUrl(link.url)"
          target="_blank"
          rel="noopener noreferrer"
        >
          <span class="link-card__index" aria-hidden="true">{{
            String(index + 1).padStart(2, '0')
          }}</span>
          <span class="link-card__arrow" aria-hidden="true">↗</span>
          <img
            v-if="avatarSrc(link)"
            class="avatar"
            :src="avatarSrc(link)"
            :alt="link.name"
            loading="lazy"
            @error="markBroken(link.id)"
          />
          <span v-else class="avatar avatar--fallback" aria-hidden="true">
            {{ initial(link.name) }}
          </span>

          <span class="link-name">{{ link.name }}</span>
          <span v-if="link.description" class="link-desc">{{ link.description }}</span>
        </a>
      </div>

      <!-- ── 底部申请区 ── -->
      <section class="apply">
        <p v-if="feedback" class="feedback" role="alert">{{ feedback }}</p>
        <p v-if="mineLoading" role="status">正在加载申请状态…</p>
        <div v-else-if="mineError" role="alert">
          <p>你的友链申请状态暂时无法读取。</p>
          <button class="journal-link" type="button" @click="refreshMine">重新加载</button>
        </div>

        <!-- 未登录、以及已登录但还没申请：都只给一行安静的入口 -->
        <div v-else-if="panel === 'guest' || panel === 'entry'" class="apply-entry">
          <div>
            <h2 class="apply-invitation">交换友链</h2>
            <p class="apply-note">如果你也在写博客，欢迎交换链接。</p>
          </div>
          <button
            class="more-link"
            type="button"
            @click="panel === 'guest' ? openLogin() : startApply()"
          >
            申请友链 <span class="more-arrow">→</span>
          </button>
        </div>

        <!-- 申请 / 修改表单 -->
        <template v-else-if="panel === 'form'">
          <div class="apply-head">
            <h2 class="apply-title">{{ myLink ? '修改友链' : '申请友链' }}</h2>
          </div>

          <form class="form" @submit.prevent="submit">
            <label class="field">
              <span class="field-label">站点名称</span>
              <input v-model="form.name" class="input" type="text" required maxlength="64" />
            </label>

            <label class="field">
              <span class="field-label">站点地址</span>
              <input
                v-model="form.url"
                class="input"
                type="url"
                required
                maxlength="512"
                pattern="https?://.+"
                placeholder="https://"
              />
            </label>

            <label class="field">
              <span class="field-label">头像地址 <span class="opt">选填</span></span>
              <input
                v-model="form.avatar"
                class="input"
                type="url"
                maxlength="512"
                pattern="https?://.+"
                placeholder="https://your-site.com/favicon.ico"
              />
            </label>

            <label class="field">
              <span class="field-label">简介 <span class="opt">选填</span></span>
              <input
                v-model="form.description"
                class="input"
                type="text"
                maxlength="255"
                placeholder="写点前端和摄影"
              />
            </label>

            <label class="field field--full">
              <span class="field-label">留言 <span class="opt">选填</span></span>
              <textarea
                v-model="form.applyMessage"
                class="textarea"
                maxlength="500"
                placeholder="已在 https://your-site.com/links 添加贵站"
              ></textarea>
            </label>

            <div class="form-actions">
              <button class="btn" type="submit" :disabled="submitting">
                {{ submitting ? '提交中…' : myLink ? '保存' : '提交申请' }}
              </button>
              <button
                class="btn btn--ghost"
                type="button"
                :disabled="submitting"
                @click="closeForm"
              >
                取消
              </button>
            </div>
          </form>
        </template>

        <!-- 我的友链 -->
        <template v-else-if="myLink">
          <div class="apply-head">
            <h2 class="apply-title">我的友链</h2>
            <span
              class="badge"
              :class="{
                'badge--rejected': myLink.status === LINK_STATUS.REJECTED,
                'badge--live': myLink.status === LINK_STATUS.APPROVED,
              }"
            >
              {{ statusLabel[myLink.status] }}
            </span>
          </div>

          <div class="mine">
            <div class="mine-top">
              <img
                v-if="avatarSrc(myLink)"
                class="avatar"
                :src="avatarSrc(myLink)"
                :alt="myLink.name"
                @error="markBroken(myLink.id)"
              />
              <span v-else class="avatar avatar--fallback" aria-hidden="true">
                {{ initial(myLink.name) }}
              </span>
              <span class="mine-meta">
                <span class="mine-name">{{ myLink.name }}</span>
                <span class="mine-url">{{ myLink.url }}</span>
              </span>
            </div>

            <p
              v-if="myLink.status === LINK_STATUS.REJECTED && myLink.rejectReason"
              class="mine-reason"
            >
              {{ myLink.rejectReason }}
            </p>

            <span v-if="myLink.status === LINK_STATUS.PENDING" class="mine-time">
              提交于 {{ myLink.createTime }}
            </span>

            <div class="mine-actions">
              <button class="link-inline" type="button" :disabled="removing" @click="startEdit">
                修改
              </button>
              <button class="link-inline" type="button" :disabled="removing" @click="removeMine">
                {{ removing ? '处理中…' : myLink.status === LINK_STATUS.PENDING ? '撤回' : '删除' }}
              </button>
            </div>
          </div>
        </template>
      </section>
    </div>
  </main>
</template>

<style scoped>
.friends-page {
  padding-top: var(--spacing-header-height);
  min-height: 80vh;
}
.friends-main {
  max-width: var(--spacing-container);
  margin: 0 auto;
  padding: 0 var(--spacing-page-padding) 100px;
}
.page-title-dot {
  color: var(--color-accent);
}
.apply-invitation {
  font: 500 24px var(--font-display);
}
.apply-note {
  font-size: 14px;
  color: var(--color-text-muted);
  margin-top: 10px;
}

.empty {
  margin: 0;
  padding: 48px 0;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 13.5px;
}

/* ── 友链卡片网格 ── */

/* auto-fit + 上限宽度：友链只有几条时整组居中，不会被拉宽也不会靠左堆着 */
.link-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  border-top: 1px solid var(--color-border-strong);
  padding-top: 32px;
}
.link-card {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 24px;
  align-content: start;
  align-items: center;
  gap: 20px 14px;
  padding: 26px;
  border: 1px solid var(--color-border);
  background: var(--color-bg-card);
  min-height: 224px;
  transition:
    border-color 0.2s,
    transform 0.2s;
}
.link-card:hover {
  border-color: var(--color-accent);
  transform: translateY(-3px);
}
.link-card__index {
  grid-column: 1 / 3;
  font: 12px var(--font-mono);
  color: var(--color-text-muted);
}
.link-card__arrow {
  grid-column: 3;
  font-size: 24px;
  color: var(--color-text-muted);
}
.link-card:hover .link-card__arrow {
  color: var(--color-accent);
}
.link-card .avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  flex-shrink: 0;
  object-fit: cover;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
}

.avatar--fallback {
  display: grid;
  place-items: center;
  font-family: var(--font-serif);
  font-size: 20px;
  color: var(--color-text-secondary);
  user-select: none;
}

/* 截断：站名可以长到 64 字，不夹住会把整行网格撑变形 */
.link-name {
  grid-column: 2 / -1;
  font-family: var(--font-display);
  font-size: 21px;
  font-weight: 500;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background-image: linear-gradient(currentColor, currentColor);
  background-repeat: no-repeat;
  background-size: 0% 1px;
  background-position: left bottom;
  transition: background-size var(--transition-sweep);
}

.link-card:hover .link-name {
  background-size: 100% 1px;
}

/* 夹到两行，卡片高度才齐 */
.link-desc {
  grid-column: 1 / -1;
  font-size: 14px;
  line-height: 1.5;
  color: var(--color-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ── 底部申请区 ── */

.apply {
  margin-top: 56px;
  padding-top: 32px;
  border-top: 1px solid var(--color-border);
}

/* 与首页底部「全部文章 →」同一套写法，见 HomeView.vue */
.apply-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.more-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-family: inherit;
  font-size: 14px;
  min-height: 44px;
  letter-spacing: 0.03em;
  color: var(--color-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  background-image: linear-gradient(currentColor, currentColor);
  background-repeat: no-repeat;
  background-size: 0% 1px;
  background-position: left bottom;
  padding: 0 0 1px;
  transition:
    color var(--transition-base),
    background-size 0.28s ease;
}

.more-link:hover {
  color: var(--color-text-secondary);
  background-size: 100% 1px;
}

.more-arrow {
  display: inline-block;
  transition: transform var(--transition-base);
}

.more-link:hover .more-arrow {
  transform: translateX(4px);
}

.apply-head {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.apply-title {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
}

.feedback {
  margin: 0 auto 18px;
  max-width: 640px;
  font-size: 13px;
  color: var(--color-danger-on-soft);
  background: var(--color-danger-bg);
  border-left: 2px solid var(--color-danger);
  padding: 9px 12px;
}

.btn:disabled,
.link-inline:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.badge {
  font-size: 11px;
  letter-spacing: 0.08em;
  padding: 2px 8px;
  border-radius: var(--radius-badge);
  border: 1px solid var(--color-border-strong);
  color: var(--color-text-muted);
  white-space: nowrap;
}

.badge--live {
  color: var(--color-text-secondary);
}

.badge--rejected {
  border-color: var(--color-danger);
  color: var(--color-danger-on-soft);
  background: var(--color-danger-bg);
}

/* ── 表单 ── */

.form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 24px;
  max-width: 640px;
  margin: 0 auto;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field--full {
  grid-column: 1 / -1;
}

.field-label {
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
}

.opt {
  color: var(--color-text-muted);
}

.input,
.textarea {
  font-family: inherit;
  font-size: 13.5px;
  color: var(--color-text-primary);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 0;
  padding: 9px 11px;
  width: 100%;
  transition: border-color var(--transition-base);
}

.textarea {
  resize: vertical;
  min-height: 68px;
}

.input:focus,
.textarea:focus {
  outline: none;
  border-color: var(--color-accent);
}

.input::placeholder,
.textarea::placeholder {
  color: var(--color-text-muted);
}

.form-actions {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 4px;
}

.btn {
  font-family: inherit;
  font-size: 13px;
  letter-spacing: 0.04em;
  padding: 9px 22px;
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: var(--color-text-on-accent);
  border-radius: 0;
  cursor: pointer;
  transition:
    background var(--transition-base),
    border-color var(--transition-base);
}

.btn:hover {
  background: var(--color-accent-dark);
  border-color: var(--color-accent-dark);
}

.btn--ghost {
  background: none;
  color: var(--color-text-secondary);
  border-color: var(--color-border-strong);
}

.btn--ghost:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
  border-color: var(--color-border-strong);
}

/* ── 我的友链 ── */

.mine {
  border: 1px solid var(--color-border);
  background: var(--color-bg-card);
  padding: 20px 22px;
  max-width: 640px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mine-top {
  display: flex;
  align-items: center;
  gap: 14px;
}

.mine-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.mine-name {
  font-size: 14.5px;
  font-weight: 500;
}

.mine-url {
  font-family: var(--font-mono);
  font-size: 11.5px;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
}

.mine-reason {
  margin: 0;
  font-size: 13px;
  color: var(--color-danger-on-soft);
  background: var(--color-danger-bg);
  border-left: 2px solid var(--color-danger);
  padding: 9px 12px;
}

.mine-time {
  font-size: 11.5px;
  color: var(--color-text-muted);
}

.mine-actions {
  display: flex;
  align-items: center;
  gap: 18px;
  padding-top: 2px;
}

.link-inline {
  font-family: inherit;
  font-size: 13px;
  color: var(--color-accent);
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  background-image: linear-gradient(currentColor, currentColor);
  background-repeat: no-repeat;
  background-size: 0% 1px;
  background-position: left bottom;
  transition: background-size var(--transition-sweep);
}

.link-inline:hover {
  background-size: 100% 1px;
}

/* ── Mobile ── */

@media (max-width: 960px) {
  .link-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 640px) {
  .friends-main {
    padding-bottom: 64px;
  }
  .link-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
    padding-top: 24px;
  }
  .link-card {
    min-height: 190px;
    padding: 22px;
  }
  .apply-entry {
    flex-direction: column;
    align-items: flex-start;
  }
  .apply {
    margin-top: 32px;
  }
  .form {
    grid-template-columns: minmax(0, 1fr);
  }
  .apply-invitation {
    font-size: 22px;
  }
}
</style>
