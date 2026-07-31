<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import {
  getCommentPage,
  saveComment,
  deleteComment,
  type CommentVO,
  type CommentUserVO,
} from '@/api/comment'
import { useUserStore } from '@/stores/useUserStore'
import { useModalStore } from '@/stores/useModalStore'
import { useConfirm } from '@/composables/useConfirm'
import { ApiError } from '@/utils/request'

const props = defineProps<{ articleId: number }>()

const PAGE_SIZE = 10
const MAX_LENGTH = 1000

const userStore = useUserStore()
const { isLoggedIn, userInfo } = storeToRefs(userStore)
const modalStore = useModalStore()
const confirm = useConfirm()

const list = ref<CommentVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const hasNext = ref(false)
const loading = ref(true)
const loadingMore = ref(false)
const failed = ref(false)

const draft = ref('')
const replyDraft = ref('')
const replyingTo = ref<number | null>(null)
const submitting = ref(false)
const feedback = ref('')

const currentUserId = computed(() => userInfo.value?.userId ?? null)
const remaining = computed(() => MAX_LENGTH - draft.value.length)

function initial(user: CommentUserVO | null) {
  return (user?.username ?? '').charAt(0).toUpperCase() || '?'
}

function formatTime(iso: string) {
  return iso ? iso.slice(0, 16) : ''
}

function isOwn(user: CommentUserVO | null) {
  return !!user && !!currentUserId.value && user.userId === currentUserId.value
}

async function load(page = 1, append = false) {
  if (append) loadingMore.value = true
  else loading.value = true
  try {
    const res = await getCommentPage(props.articleId, { pageNum: page, pageSize: PAGE_SIZE })
    list.value = append ? [...list.value, ...res.content] : res.content
    total.value = res.totalElements
    hasNext.value = res.hasNext
    pageNum.value = page
    failed.value = false
  } catch {
    if (!append) failed.value = true
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 仅在客户端取数：评论不参与 SSG 预渲染，静态产物里始终是骨架
onMounted(() => load())

function requireLogin() {
  modalStore.open('login')
}

function toggleReply(rootId: number) {
  replyingTo.value = replyingTo.value === rootId ? null : rootId
  replyDraft.value = ''
  feedback.value = ''
}

async function submit(content: string, parentId?: number) {
  const text = content.trim()
  if (!text || submitting.value) return false
  submitting.value = true
  feedback.value = ''
  try {
    await saveComment(props.articleId, parentId ? { content: text, parentId } : { content: text })
    await load(1)
    return true
  } catch (err) {
    feedback.value = err instanceof ApiError ? err.message : '发表失败，请稍后重试'
    return false
  } finally {
    submitting.value = false
  }
}

async function submitRoot() {
  if (await submit(draft.value)) draft.value = ''
}

async function submitReply(parentId: number) {
  if (await submit(replyDraft.value, parentId)) {
    replyDraft.value = ''
    replyingTo.value = null
  }
}

async function remove(commentId: number) {
  try {
    await confirm('删除后不可恢复，确认删除这条评论？', '删除评论', {
      confirmText: '删除',
      danger: true,
    })
  } catch {
    return
  }
  try {
    await deleteComment(props.articleId, commentId)
    await load(1)
  } catch (err) {
    feedback.value = err instanceof ApiError ? err.message : '删除失败，请稍后重试'
  }
}
</script>

<template>
  <div class="comments">
    <!-- 发表框 -->
    <div v-if="isLoggedIn" class="composer">
      <textarea
        v-model="draft"
        class="composer-input"
        :maxlength="MAX_LENGTH"
        rows="3"
        placeholder="写下你的想法…"
      />
      <div class="composer-foot">
        <span class="composer-count" :class="{ 'is-low': remaining < 50 }">{{ remaining }}</span>
        <button class="btn-primary" :disabled="!draft.trim() || submitting" @click="submitRoot">
          {{ submitting ? '发表中…' : '发表' }}
        </button>
      </div>
    </div>
    <div v-else class="login-hint">
      <span>登录后参与讨论</span>
      <button class="btn-text" @click="requireLogin">登录</button>
    </div>

    <p v-if="feedback" class="feedback">{{ feedback }}</p>

    <!-- 骨架 -->
    <div v-if="loading" class="sk-list" aria-hidden="true">
      <div v-for="i in 3" :key="i" class="sk-item">
        <div class="sk sk-avatar" />
        <div class="sk-lines">
          <div class="sk sk-name" />
          <div class="sk sk-text" />
        </div>
      </div>
    </div>

    <p v-else-if="failed" class="state-text">评论加载失败，请刷新页面重试。</p>
    <p v-else-if="!list.length" class="state-text">还没有评论，来说点什么。</p>

    <!-- 列表 -->
    <ul v-else class="comment-list">
      <li v-for="root in list" :key="root.id" class="comment-item">
        <div class="comment" :class="{ 'is-removed': root.removed }">
          <template v-if="root.removed">
            <span class="tombstone-avatar" aria-hidden="true">—</span>
            <div class="comment-main">
              <p class="tombstone-text">该评论已删除</p>
            </div>
          </template>
          <template v-else>
            <img
              v-if="root.user?.avatar"
              :src="root.user.avatar"
              class="avatar avatar--img"
              :alt="root.user.username"
            />
            <span v-else class="avatar avatar--placeholder">{{ initial(root.user) }}</span>
            <div class="comment-main">
              <div class="comment-head">
                <span class="comment-name">{{ root.user?.username }}</span>
                <span class="comment-time">{{ formatTime(root.createTime) }}</span>
              </div>
              <p class="comment-body">{{ root.content }}</p>
              <div class="comment-actions">
                <button v-if="isLoggedIn" class="btn-text" @click="toggleReply(root.id)">
                  {{ replyingTo === root.id ? '取消' : '回复' }}
                </button>
                <button
                  v-if="isOwn(root.user)"
                  class="btn-text btn-text--danger"
                  @click="remove(root.id)"
                >
                  删除
                </button>
              </div>
            </div>
          </template>
        </div>

        <!-- 楼中楼 -->
        <ul v-if="root.replies.length" class="reply-list">
          <li v-for="reply in root.replies" :key="reply.id" class="reply">
            <img
              v-if="reply.user?.avatar"
              :src="reply.user.avatar"
              class="avatar avatar--sm avatar--img"
              :alt="reply.user.username"
            />
            <span v-else class="avatar avatar--sm avatar--placeholder">{{
              initial(reply.user)
            }}</span>
            <div class="comment-main">
              <div class="comment-head">
                <span class="comment-name">{{ reply.user?.username }}</span>
                <span v-if="reply.replyToUser" class="reply-to"
                  >回复 @{{ reply.replyToUser.username }}</span
                >
                <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
              </div>
              <p class="comment-body">{{ reply.content }}</p>
              <div class="comment-actions">
                <button v-if="isLoggedIn" class="btn-text" @click="toggleReply(reply.id)">
                  {{ replyingTo === reply.id ? '取消' : '回复' }}
                </button>
                <button
                  v-if="isOwn(reply.user)"
                  class="btn-text btn-text--danger"
                  @click="remove(reply.id)"
                >
                  删除
                </button>
              </div>
            </div>
          </li>
        </ul>

        <!-- 回复框 -->
        <div
          v-if="replyingTo === root.id || root.replies.some((r) => r.id === replyingTo)"
          class="composer composer--reply"
        >
          <textarea
            v-model="replyDraft"
            class="composer-input"
            :maxlength="MAX_LENGTH"
            rows="2"
            placeholder="回复…"
          />
          <div class="composer-foot">
            <button
              class="btn-primary"
              :disabled="!replyDraft.trim() || submitting"
              @click="submitReply(replyingTo!)"
            >
              {{ submitting ? '发表中…' : '回复' }}
            </button>
          </div>
        </div>
      </li>
    </ul>

    <button
      v-if="hasNext && !loading"
      class="btn-more"
      :disabled="loadingMore"
      @click="load(pageNum + 1, true)"
    >
      {{ loadingMore ? '加载中…' : '加载更多' }}
    </button>
  </div>
</template>

<style scoped>
.comments {
  font-size: 14px;
}

/* --- 发表框 --- */
.composer {
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  margin-bottom: 32px;
}

.composer--reply {
  margin: 12px 0 4px 48px;
}

.composer-input {
  display: block;
  width: 100%;
  padding: 14px 16px;
  border: none;
  background: transparent;
  color: var(--color-text-primary);
  font-family: var(--font-sans);
  font-size: 14px;
  line-height: 1.7;
  resize: vertical;
}

.composer-input:focus {
  outline: none;
}

.composer-input::placeholder {
  color: var(--color-text-faint);
}

.composer-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  padding: 8px 12px;
  border-top: 1px solid var(--color-border-light);
}

.composer-count {
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-faint);
}

.composer-count.is-low {
  color: var(--color-danger);
}

.btn-primary {
  padding: 6px 18px;
  border: 1px solid var(--color-text-primary);
  background: var(--color-text-primary);
  color: var(--color-bg);
  font-family: var(--font-sans);
  font-size: 12px;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: opacity 0.15s ease;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.82;
}

.btn-primary:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.login-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  margin-bottom: 32px;
  border: 1px dashed var(--color-border);
  font-size: 13px;
  color: var(--color-text-muted);
}

.btn-text {
  padding: 0;
  border: none;
  background: none;
  color: var(--color-text-secondary);
  font-family: var(--font-sans);
  font-size: 12px;
  cursor: pointer;
  transition: color 0.15s ease;
}

.btn-text:hover {
  color: var(--color-text-primary);
}

.btn-text--danger:hover {
  color: var(--color-danger);
}

.feedback {
  margin: -20px 0 24px;
  font-size: 12px;
  color: var(--color-danger);
}

.state-text {
  padding: 8px 0 4px;
  font-size: 13px;
  color: var(--color-text-muted);
  letter-spacing: 0.02em;
}

/* --- 列表 --- */
.comment-list,
.reply-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.comment-item {
  padding: 20px 0;
  border-top: 1px solid var(--color-border-light);
}

.comment-item:first-child {
  border-top: none;
  padding-top: 0;
}

.comment,
.reply {
  display: flex;
  gap: 12px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
}

.avatar--sm {
  width: 28px;
  height: 28px;
}

.avatar--img {
  object-fit: cover;
}

.avatar--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-hover);
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 500;
}

.avatar--sm.avatar--placeholder {
  font-size: 11px;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.reply-to {
  font-size: 12px;
  color: var(--color-text-muted);
}

.comment-time {
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-faint);
}

.comment-body {
  font-size: 14px;
  line-height: 1.75;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

/* --- 墓碑 --- */
.tombstone-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px dashed var(--color-border);
  color: var(--color-text-faint);
  font-size: 12px;
}

.tombstone-text {
  padding-top: 8px;
  font-size: 13px;
  font-style: italic;
  color: var(--color-text-faint);
}

/* --- 回复 --- */
.reply-list {
  margin: 16px 0 0 48px;
}

.reply {
  padding: 12px 0;
  border-top: 1px solid var(--color-border-light);
}

/* --- 加载更多 --- */
.btn-more {
  display: block;
  width: 100%;
  margin-top: 24px;
  padding: 10px;
  border: 1px solid var(--color-border);
  background: none;
  color: var(--color-text-secondary);
  font-family: var(--font-sans);
  font-size: 12px;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: border-color 0.15s ease;
}

.btn-more:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  color: var(--color-text-primary);
}

.btn-more:disabled {
  opacity: 0.5;
  cursor: default;
}

/* --- 骨架 --- */
.sk-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sk-item {
  display: flex;
  gap: 12px;
}

.sk {
  background: var(--color-bg-hover);
  animation: sk-pulse 1.5s ease-in-out infinite;
}

.sk-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
}

.sk-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 4px;
}

.sk-name {
  width: 90px;
  height: 11px;
}

.sk-text {
  width: 100%;
  height: 11px;
}

@keyframes sk-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.45;
  }
}

@media (max-width: 768px) {
  .reply-list,
  .composer--reply {
    margin-left: 24px;
  }
}
</style>
