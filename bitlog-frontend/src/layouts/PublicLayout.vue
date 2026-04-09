<script setup lang="ts">
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import LoginModal from '@/components/common/LoginModal.vue'
</script>

<template>
  <AppHeader />
  <RouterView />
  <AppFooter />
  <LoginModal />
</template>
