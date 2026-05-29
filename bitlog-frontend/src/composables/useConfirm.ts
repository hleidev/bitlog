import { ref } from 'vue'

export interface ConfirmOptions {
  confirmText?: string
  cancelText?:  string
  danger?:      boolean
}

export interface PendingConfirm {
  message: string
  title:   string
  options: Required<ConfirmOptions>
  resolve: () => void
  reject:  () => void
}

const pending = ref<PendingConfirm | null>(null)

export function useConfirm() {
  return function confirm(
    message: string,
    title = '确认',
    options: ConfirmOptions = {},
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      pending.value = {
        message,
        title,
        options: {
          confirmText: options.confirmText ?? '确认',
          cancelText:  options.cancelText  ?? '取消',
          danger:      options.danger      ?? false,
        },
        resolve,
        reject,
      }
    })
  }
}

export { pending }