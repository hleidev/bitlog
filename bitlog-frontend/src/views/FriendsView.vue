<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
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

const userStore = useUserStore()
const modalStore = useModalStore()
const confirm = useConfirm()

const links = ref<FriendLinkVO[]>([])
const myLink = ref<MyFriendLinkVO | null>(null)
const loading = ref(true)
const submitting = ref(false)
const formOpen = ref(false)
const feedback = ref('')

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
  links.value = await getFriendLinks()
}

async function loadMine() {
  myLink.value = userStore.isLoggedIn ? await getMyFriendLink() : null
}

onMounted(async () => {
  try {
    // 登录态要等静默续期完成再判断，否则刷新页面时永远走到未登录分支
    await userStore.waitForSession()
    await Promise.all([loadLinks(), loadMine()])
  } catch {
    feedback.value = '加载失败，请刷新重试'
  } finally {
    loading.value = false
  }
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

function startApply() {
  form.name = ''
  form.url = ''
  form.avatar = ''
  form.description = ''
  form.applyMessage = ''
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
  submitting.value = true
  feedback.value = ''
  try {
    if (myLink.value) {
      await updateMyFriendLink({ ...form })
    } else {
      await applyFriendLink({ ...form })
    }
    formOpen.value = false
    await Promise.all([loadMine(), loadLinks()])
  } catch (err) {
    feedback.value = err instanceof ApiError ? err.message : '提交失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

async function removeMine() {
  if (!myLink.value) return
  const pending = myLink.value.status === LINK_STATUS.PENDING
  try {
    await confirm(
      pending ? '撤回这条友链申请？' : '删除后需要重新申请。',
      pending ? '撤回申请' : '删除友链',
      { confirmText: pending ? '撤回' : '删除', danger: true },
    )
  } catch {
    return
  }
  feedback.value = ''
  try {
    await deleteMyFriendLink()
    await Promise.all([loadMine(), loadLinks()])
  } catch (err) {
    feedback.value = err instanceof ApiError ? err.message : '删除失败，请稍后重试'
  }
}
</script>

<template>
  <div class="friends-page">
    <div class="friends-main">
      <header class="page-head">
        <h1 class="page-title">友链</h1>
        <p class="page-desc">一些我常逛的博客。</p>
      </header>

      <p v-if="loading" class="empty">加载中…</p>

      <p v-else-if="links.length === 0" class="empty">还没有友链。</p>

      <div v-else class="link-grid">
        <a
          v-for="link in links"
          :key="link.id"
          class="link-card"
          :href="safeUrl(link.url)"
          target="_blank"
          rel="noopener noreferrer"
        >
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
      <section v-if="!loading" class="apply">
        <p v-if="feedback" class="feedback">{{ feedback }}</p>

        <!-- 未登录、以及已登录但还没申请：都只给一行安静的入口 -->
        <div v-if="panel === 'guest' || panel === 'entry'" class="apply-entry">
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
              <button class="btn btn--ghost" type="button" @click="closeForm">取消</button>
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
              <button class="link-inline" type="button" @click="startEdit">修改</button>
              <button class="link-inline" type="button" @click="removeMine">
                {{ myLink.status === LINK_STATUS.PENDING ? '撤回' : '删除' }}
              </button>
            </div>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.friends-main {
  max-width: var(--spacing-container);
  margin: 0 auto;
  padding: 72px var(--spacing-page-padding) 120px;
}

/* ── 页头 ── */

.page-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 48px;
  text-align: center;
}

.page-title {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 500;
  letter-spacing: -0.01em;
}

.page-desc {
  margin: 0;
  max-width: 54ch;
  color: var(--color-text-secondary);
  font-size: 14px;
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
  grid-template-columns: repeat(auto-fit, minmax(200px, 232px));
  justify-content: center;
  gap: 16px;
}

.link-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 28px 20px;
  border: 1px solid var(--color-border);
  color: inherit;
  text-decoration: none;
  text-align: center;
  transition:
    background var(--transition-base),
    border-color var(--transition-base);
}

.link-card:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-border-strong);
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
  font-size: 15px;
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
  font-size: 12.5px;
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
  justify-content: center;
}

.more-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-family: inherit;
  font-size: 12.5px;
  letter-spacing: 0.07em;
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

.btn:disabled {
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
  font-size: 11.5px;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
}

.opt {
  color: var(--color-text-faint);
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
  color: var(--color-text-faint);
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
  color: var(--color-text-faint);
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

@media (max-width: 768px) {
  .friends-main {
    padding: 48px 20px 80px;
  }

  .link-grid {
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 12px;
  }

  .link-card {
    padding: 22px 14px;
  }

  .form {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
