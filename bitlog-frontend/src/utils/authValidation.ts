// 规则与后端 bitlog-user 的 LoginParam / RegisterParam / PasswordResetParam 一一对应，改动需同步
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const USERNAME_RE = /^[a-zA-Z0-9_-]{4,16}$/
const PASSWORD_RE = /^[a-zA-Z0-9_@#%&!$*-]{8,20}$/
const CODE_RE = /^\d{6}$/

export const EMAIL_MAX_LENGTH = 128
export const CODE_LENGTH = 6

export function isEmailFormat(value: string): boolean {
  const val = value.trim()
  return val.length <= EMAIL_MAX_LENGTH && EMAIL_RE.test(val)
}

// 以下校验函数统一返回可直接展示的中文提示，空串表示通过
export function validateEmail(value: string): string {
  const val = value.trim()
  if (!val) return '请输入邮箱'
  if (val.length > EMAIL_MAX_LENGTH) return `邮箱不能超过 ${EMAIL_MAX_LENGTH} 个字符`
  if (!EMAIL_RE.test(val)) return '邮箱格式不正确'
  return ''
}

export function validateUsername(value: string): string {
  const val = value.trim()
  if (!val) return '请输入用户名'
  if (!USERNAME_RE.test(val)) return '用户名为 4~16 位，只能含字母、数字、_ 或 -'
  return ''
}

export function validatePassword(value: string, label = '密码'): string {
  if (!value) return `请输入${label}`
  if (!PASSWORD_RE.test(value)) return `${label}为 8~20 位，可含字母、数字及常用符号`
  return ''
}

export function validateConfirmPassword(value: string, password: string): string {
  if (!value) return '请再次输入密码'
  if (value !== password) return '两次密码输入不一致'
  return ''
}

export function validateCode(value: string): string {
  if (!value) return '请输入验证码'
  if (!CODE_RE.test(value)) return `验证码为 ${CODE_LENGTH} 位数字`
  return ''
}
