import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'warning'

export interface ToastItem {
  id: number
  type: ToastType
  message: string
}

const toasts = ref<ToastItem[]>([])
let nextId = 0

function add(type: ToastType, message: string) {
  const id = ++nextId
  toasts.value.push({ id, type, message })
  setTimeout(() => remove(id), 3000)
}

function remove(id: number) {
  const idx = toasts.value.findIndex(t => t.id === id)
  if (idx !== -1) toasts.value.splice(idx, 1)
}

export function useToast() {
  return {
    success: (msg: string) => add('success', msg),
    error:   (msg: string) => add('error', msg),
    warning: (msg: string) => add('warning', msg),
    remove,
  }
}

export { toasts }
