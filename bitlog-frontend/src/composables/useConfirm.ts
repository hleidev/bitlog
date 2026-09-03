import { ref } from 'vue'

/**
 * 确认框的三个出口：确认 / 第三选项 / 取消。
 * 取消走 reject（沿用旧行为），另两个走 resolve 并带上判别值 —— 只读 await 不看返回值的
 * 旧调用点因此完全不受影响。
 */
export type ConfirmResult = 'confirm' | 'extra'

export interface ConfirmOptions {
  confirmText?: string
  cancelText?: string
  /** 第三个出口的文案（如「直接离开」）；不传则不渲染该按钮 */
  extraText?: string
  danger?: boolean
}

export interface PendingConfirm {
  message: string
  title: string
  options: {
    confirmText: string
    cancelText: string
    extraText: string | null
    danger: boolean
  }
  resolve: (result: ConfirmResult) => void
  reject: () => void
}

const pending = ref<PendingConfirm | null>(null)

export function useConfirm() {
  return function confirm(
    message: string,
    title = '确认',
    options: ConfirmOptions = {},
  ): Promise<ConfirmResult> {
    return new Promise((resolve, reject) => {
      pending.value = {
        message,
        title,
        options: {
          confirmText: options.confirmText ?? '确认',
          cancelText: options.cancelText ?? '取消',
          extraText: options.extraText ?? null,
          danger: options.danger ?? false,
        },
        resolve,
        reject,
      }
    })
  }
}

export { pending }
