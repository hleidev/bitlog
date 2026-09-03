import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ModalType = 'login'

export const useModalStore = defineStore('modal', () => {
  const visible = ref(false)
  const activeModal = ref<ModalType | null>(null)

  function open(type: ModalType) {
    activeModal.value = type
    visible.value = true
  }

  function close() {
    visible.value = false
    activeModal.value = null
  }

  return { visible, activeModal, open, close }
})
