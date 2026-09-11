<script setup lang="ts">
/**
 * 后台列表页图标集。六个页面原本各画一份：搜索图标 r=8 和 r=7 两版，
 * 清除/新建/重置 各有 stroke 与 fill 两种画法，同一个按钮在不同页面长得不一样。
 * 这里统一为 stroke 画法、24 视窗。
 */
type IconName =
  | 'search'
  | 'close'
  | 'reset'
  | 'plus'
  | 'more'
  | 'article'
  | 'user'
  | 'comment'
  | 'category'
  | 'tag'
  | 'link'
  | 'workspace'
  | 'profile'
  | 'external'

const props = defineProps<{ name: IconName }>()

const PATHS: Record<Exclude<IconName, 'more'>, string> = {
  workspace: 'M3 3h18v18H3zM3 9h18M9 9v12',
  profile: 'M9 3H4v18h16V3h-5M9 2h6v4H9zM8 17h8M9 12h6',
  external: 'M14 3h7v7M21 3l-11 11M10 3H3v18h18v-7',
  search: 'M11 3a8 8 0 1 0 0 16 8 8 0 0 0 0-16M21 21l-4.35-4.35',
  close: 'M18 6 6 18M6 6l12 12',
  reset: 'M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8M3 3v5h5',
  plus: 'M12 5v14M5 12h14',
  article: 'M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9zM14 3v6h6',
  user: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8',
  comment: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z',
  category: 'M4 4h6l2 2h8a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2',
  tag: 'M20.6 13.4 12 4.8V2H4v8h2.8l8.6 8.6a2 2 0 0 0 2.8 0l2.4-2.4a2 2 0 0 0 0-2.8M7 7h.01',
  link: 'M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.7 1.7M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7',
}
</script>

<template>
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <template v-if="props.name === 'more'">
      <circle cx="12" cy="5" r="1.4" fill="currentColor" stroke="none" />
      <circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none" />
      <circle cx="12" cy="19" r="1.4" fill="currentColor" stroke="none" />
    </template>
    <path v-else :d="PATHS[props.name]" />
  </svg>
</template>
