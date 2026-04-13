<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { createUser, type CreateUserResult } from '@/api/admin/user'

const router = useRouter()

const form = reactive({
  userName: '',
  email: '',
  userRole: 0 as 0 | 1,
  position: '',
  company: '',
  profile: '',
})

const submitting = ref(false)

// 成功弹窗
const successVisible = ref(false)
const createResult = ref<CreateUserResult | null>(null)

async function handleSubmit() {
  if (!form.userName.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!/^[a-zA-Z0-9_-]{4,16}$/.test(form.userName)) {
    ElMessage.warning('用户名为 4-16 位字母、数字、下划线或连字符')
    return
  }
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  if (form.email && form.email.length > 128) {
    ElMessage.warning('邮箱最长 128 个字符')
    return
  }
  if (form.position.length > 64) {
    ElMessage.warning('职位最长 64 个字符')
    return
  }
  if (form.company.length > 64) {
    ElMessage.warning('公司最长 64 个字符')
    return
  }
  if (form.profile.length > 500) {
    ElMessage.warning('个人简介最长 500 个字符')
    return
  }

  submitting.value = true
  try {
    const result = await createUser({
      userName: form.userName,
      email: form.email || undefined,
      userRole: form.userRole,
      position: form.position || undefined,
      company: form.company || undefined,
      profile: form.profile || undefined,
    })
    createResult.value = result
    successVisible.value = true
    // 弹窗打开后自动复制账号和密码
    copyCredentials(result)
  } catch (err: any) {
    if (err?.code === 42001) {
      ElMessage.error('用户名已存在')
    } else if (err?.code === 40001) {
      ElMessage.error('参数校验失败，请检查用户名或邮箱格式')
    } else {
      ElMessage.error('创建失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

async function copyCredentials(result = createResult.value) {
  if (!result) return
  try {
    await navigator.clipboard.writeText(`账号：${result.userName}\n密码：${result.initialPassword}`)
    ElMessage.success('账号和密码已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
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
        <el-icon><ArrowLeft /></el-icon>
        返回
      </button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
    </div>

    <div class="form-wrap">
      <!-- 基本信息 -->
      <section class="card">
        <div class="card-head">
          <span class="card-title">基本信息</span>
        </div>
        <div class="card-body">
          <div class="field-row">
            <div class="field">
              <label class="field-label">用户名 <span class="required">*</span></label>
              <el-input v-model="form.userName" placeholder="请输入用户名" :maxlength="16" />
            </div>
            <div class="field">
              <label class="field-label">邮箱</label>
              <el-input v-model="form.email" placeholder="user@example.com" :maxlength="128" />
            </div>
          </div>
          <div class="field" style="width: calc(50% - 6px)">
            <label class="field-label">角色</label>
            <el-select v-model="form.userRole" style="width: 100%">
              <el-option label="普通用户" :value="0" />
              <el-option label="管理员" :value="1" />
            </el-select>
          </div>
        </div>
      </section>

      <!-- 个人资料 -->
      <section class="card">
        <div class="card-head">
          <span class="card-title">个人资料</span>
          <span class="card-hint">选填</span>
        </div>
        <div class="card-body">
          <div class="field-row">
            <div class="field">
              <label class="field-label">职位</label>
              <el-input v-model="form.position" placeholder="如：前端工程师" :maxlength="64" />
            </div>
            <div class="field">
              <label class="field-label">公司</label>
              <el-input v-model="form.company" placeholder="如：Acme Inc." :maxlength="64" />
            </div>
          </div>
          <div class="field">
            <label class="field-label">个人简介</label>
            <el-input
              v-model="form.profile"
              type="textarea"
              :rows="3"
              placeholder="简单介绍一下这个用户…"
              :maxlength="500"
              show-word-limit
            />
          </div>
        </div>
      </section>
    </div>
  </div>

  <!-- 创建成功弹窗 -->
  <el-dialog v-model="successVisible" width="min(360px, 92vw)" :show-close="false" :close-on-click-modal="false" align-center>
    <template #header><span /></template>
    <div v-if="createResult" class="pwd-dialog-inner">
      <div class="pwd-dialog-icon">✓</div>
      <p class="pwd-dialog-title">用户已创建</p>
      <p class="pwd-dialog-sub">账号 <b>{{ createResult.userName }}</b> · 初始密码</p>
      <div class="pwd-dialog-value">{{ createResult.initialPassword }}</div>
      <p class="pwd-dialog-copied">账号和密码已自动复制到剪贴板</p>
    </div>
    <template #footer>
      <el-button type="primary" @click="handleSuccessClose">完成</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.add-user-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
}

/* Top bar */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-size: 13px;
  color: #6b7280;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
}

.back-btn:hover {
  color: #374151;
  border-color: #d1d5db;
}

/* Form wrap */
.form-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Card */
.card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #f3f4f6;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.card-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-left: 4px;
}

.card-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Fields */
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.field-row {
  display: flex;
  gap: 12px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.required {
  color: #ef4444;
  margin-left: 2px;
}

@media (max-width: 768px) {
  .field-row {
    flex-direction: column;
  }

  /* 角色 select 在移动端占满宽度 */
  .card-body > .field[style] {
    width: 100% !important;
  }

  .card-body {
    padding: 16px;
  }
}

/* Success dialog */
.pwd-dialog-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 0 4px;
  text-align: center;
}

.pwd-dialog-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #dcfce7;
  color: #16a34a;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.pwd-dialog-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.pwd-dialog-sub {
  font-size: 12px;
  color: #9ca3af;
  margin: 4px 0 0;
}

.pwd-dialog-value {
  font-family: ui-monospace, monospace;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #1d4ed8;
  background: #eff6ff;
  border-radius: 8px;
  padding: 10px 20px;
  margin: 2px 0;
}

.pwd-dialog-copied {
  font-size: 12px;
  color: #16a34a;
  margin: 0;
}
</style>
