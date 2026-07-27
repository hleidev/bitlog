<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@/admin/composables/useToast'
import { createUser, type CreateUserResult } from '@/api/admin/user'

const router = useRouter()
const toast = useToast()

const form = reactive({
  username: '',
  email: '',
  userRole: 0 as 0 | 1,
  position: '',
  company: '',
  profile: '',
})

const submitting = ref(false)

const successVisible = ref(false)
const createResult = ref<CreateUserResult | null>(null)

async function handleSubmit() {
  if (!form.username.trim()) {
    toast.warning('请输入用户名')
    return
  }
  if (!/^[a-zA-Z0-9_-]{4,16}$/.test(form.username)) {
    toast.warning('用户名为 4-16 位字母、数字、下划线或连字符')
    return
  }
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    toast.warning('邮箱格式不正确')
    return
  }
  if (form.email && form.email.length > 128) {
    toast.warning('邮箱最长 128 个字符')
    return
  }
  if (form.position.length > 64) {
    toast.warning('职位最长 64 个字符')
    return
  }
  if (form.company.length > 64) {
    toast.warning('公司最长 64 个字符')
    return
  }
  if (form.profile.length > 500) {
    toast.warning('个人简介最长 500 个字符')
    return
  }

  submitting.value = true
  try {
    const result = await createUser({
      username: form.username,
      email: form.email || undefined,
      userRole: form.userRole,
      position: form.position || undefined,
      company: form.company || undefined,
      profile: form.profile || undefined,
    })
    createResult.value = result
    successVisible.value = true
    copyCredentials(result)
  } catch (err: any) {
    if (err?.code === 42001) toast.error('用户名已存在')
    else if (err?.code === 40001) toast.error('参数校验失败，请检查用户名或邮箱格式')
    else toast.error('创建失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

async function copyCredentials(result = createResult.value) {
  if (!result) return
  try {
    await navigator.clipboard.writeText(`账号：${result.username}\n密码：${result.initialPassword}`)
    toast.success('账号和密码已复制到剪贴板')
  } catch {
    toast.error('复制失败，请手动复制')
  }
}

function handleSuccessClose() {
  successVisible.value = false
  router.push('/admin/users')
}
</script>

<template>
  <div class="add-user-page">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <button class="back-btn" @click="router.back()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
        返回
      </button>
      <button class="primary-btn" :disabled="submitting" @click="handleSubmit">
        <span v-if="submitting" class="btn-spinner" />
        保存
      </button>
    </div>

    <div class="form-wrap">
      <!-- 基本信息 -->
      <section class="form-card">
        <div class="card-head">
          <span class="section-label">基本信息</span>
          <div class="section-rule" />
        </div>
        <div class="card-body">
          <div class="field-row">
            <div class="field">
              <label class="field-label">用户名 <span class="required">*</span></label>
              <input
                v-model="form.username"
                class="field-input"
                placeholder="请输入用户名"
                maxlength="16"
              />
            </div>
            <div class="field">
              <label class="field-label">邮箱</label>
              <input
                v-model="form.email"
                class="field-input"
                placeholder="user@example.com"
                maxlength="128"
              />
            </div>
          </div>
          <div class="field field--half">
            <label class="field-label">角色</label>
            <select v-model="form.userRole" class="field-select">
              <option :value="0">普通用户</option>
              <option :value="1">管理员</option>
            </select>
          </div>
        </div>
      </section>

      <!-- 个人资料 -->
      <section class="form-card">
        <div class="card-head">
          <span class="section-label">个人资料</span>
          <span class="card-hint">选填</span>
          <div class="section-rule" />
        </div>
        <div class="card-body">
          <div class="field-row">
            <div class="field">
              <label class="field-label">职位</label>
              <input
                v-model="form.position"
                class="field-input"
                placeholder="如：前端工程师"
                maxlength="64"
              />
            </div>
            <div class="field">
              <label class="field-label">公司</label>
              <input
                v-model="form.company"
                class="field-input"
                placeholder="如：Acme Inc."
                maxlength="64"
              />
            </div>
          </div>
          <div class="field">
            <label class="field-label">
              个人简介
              <span class="field-hint">{{ form.profile.length }} / 500</span>
            </label>
            <textarea
              v-model="form.profile"
              class="field-textarea"
              rows="3"
              placeholder="简单介绍一下这个用户…"
              maxlength="500"
            />
          </div>
        </div>
      </section>
    </div>
  </div>

  <!-- 创建成功弹窗 -->
  <Teleport to="body">
    <div v-if="successVisible" class="dialog-mask">
      <div class="dialog">
        <div v-if="createResult" class="dialog-inner">
          <div class="dialog-check">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <p class="dialog-title">用户已创建</p>
          <p class="dialog-sub">
            账号 <b>{{ createResult.username }}</b> · 初始密码
          </p>
          <div class="dialog-password">{{ createResult.initialPassword }}</div>
          <p class="dialog-copied">账号和密码已自动复制到剪贴板</p>
        </div>
        <div class="dialog-footer">
          <button class="primary-btn" @click="handleSuccessClose">完成</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.add-user-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ── Top bar ── */

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 14px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  background: transparent;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text);
  cursor: pointer;
  transition: background 0.15s;
}

.back-btn svg {
  width: 14px;
  height: 14px;
}
.back-btn:hover {
  background: var(--admin-sidebar-hover);
}

/* ── Form wrap ── */

.form-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── Form card ── */

.form-card {
  background: var(--admin-header-bg);
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  overflow: hidden;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--admin-sidebar-border);
}

.section-label {
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: 13.5px;
  font-weight: 600;
  color: var(--admin-text-primary);
  white-space: nowrap;
}

.section-rule {
  flex: 1;
  height: 1px;
  background: var(--admin-sidebar-border);
}

.card-hint {
  font-size: 12px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  white-space: nowrap;
}

.card-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ── Fields ── */

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.field--half {
  max-width: calc(50% - 6px);
}

.field-row {
  display: flex;
  gap: 12px;
}

.field-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  color: var(--admin-text-primary);
}

.field-hint {
  font-size: 11px;
  font-weight: 400;
  color: var(--admin-sidebar-text-muted);
}

.required {
  color: var(--admin-required);
  margin-left: 2px;
}

.field-input,
.field-select {
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  background: var(--admin-surface-input);
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-text-primary);
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
  width: 100%;
}

.field-input:focus,
.field-select:focus {
  border-color: var(--admin-accent);
}

.field-input::placeholder {
  color: var(--admin-sidebar-text-muted);
}

.field-select {
  cursor: pointer;
  appearance: auto;
}

.field-textarea {
  padding: 8px 10px;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  background: var(--admin-surface-input);
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-text-primary);
  outline: none;
  transition: border-color 0.15s;
  resize: vertical;
  box-sizing: border-box;
  width: 100%;
  min-height: 80px;
}

.field-textarea:focus {
  border-color: var(--admin-accent);
}
.field-textarea::placeholder {
  color: var(--admin-sidebar-text-muted);
}

/* ── Buttons ── */

.primary-btn {
  height: 34px;
  padding: 0 18px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--admin-accent);
  color: var(--admin-text-on-accent);
  border: none;
  border-radius: 4px;
  font-size: 13px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.primary-btn:hover:not(:disabled) {
  background: var(--admin-accent-dark);
}
.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: var(--admin-text-on-accent);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── Success dialog ── */

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  width: min(360px, calc(100vw - 32px));
  background: var(--admin-header-bg);
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 4px;
  overflow: hidden;
}

.dialog-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px 24px 20px;
  text-align: center;
}

.dialog-check {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--admin-accent-bg-strong);
  color: var(--admin-accent-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.dialog-check svg {
  width: 20px;
  height: 20px;
}

.dialog-title {
  font-family: var(--font-serif, 'Lora', serif);
  font-size: 16px;
  font-weight: 600;
  color: var(--admin-text-primary);
  margin: 0;
}

.dialog-sub {
  font-size: 12px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  margin: 2px 0 0;
}

.dialog-password {
  font-family: 'ui-monospace', 'Menlo', monospace;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: var(--admin-accent-dark);
  background: var(--admin-accent-bg-soft);
  border: 1px solid var(--admin-accent-border);
  border-radius: 4px;
  padding: 10px 20px;
  margin: 4px 0;
}

.dialog-copied {
  font-size: 12px;
  font-family: var(--font-sans, 'Inter', sans-serif);
  color: var(--admin-sidebar-text-muted);
  margin: 0;
}

.dialog-footer {
  padding: 0 24px 24px;
  display: flex;
  justify-content: center;
}

/* ── Responsive ── */

@media (max-width: 768px) {
  .field-row {
    flex-direction: column;
  }
  .field--half {
    max-width: 100%;
  }
  .card-body {
    padding: 16px;
  }
}
</style>
