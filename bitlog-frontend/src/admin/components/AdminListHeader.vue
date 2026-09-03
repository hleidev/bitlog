<script setup lang="ts">
import AdminIcon from './AdminIcon.vue'

/**
 * 列表页头部：左 tabs、右「搜索 + 重置 + 主操作」，同一行。
 *
 * 六个页面原本各写一份，分出三种形态（带计数 tabs / 不带计数 tabs / 「共 N 个」纯文字），
 * 且圆箭头按钮在两页是「重置筛选」、在四页是「刷新」——同一个图标两种语义。
 * 这里统一为重置：重置必然重新请求，能力上覆盖刷新。
 */
interface ListTab {
  key: string
  label: string
  /** 恒显示，含 0。缺数据时传 undefined 才隐藏 */
  count?: number
}

const props = defineProps<{
  tabs: ListTab[]
  searchPlaceholder: string
  /** 主操作按钮文案，不传则不渲染 */
  actionLabel?: string
}>()

const activeTab = defineModel<string>('activeTab', { required: true })
const keyword = defineModel<string>('keyword', { required: true })

const emit = defineEmits<{
  search: []
  reset: []
  action: []
}>()
</script>

<template>
  <div class="card-header">
    <div class="view-tabs">
      <button
        v-for="tab in props.tabs"
        :key="tab.key"
        class="view-tab"
        :class="{ 'view-tab--active': activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
        <span v-if="tab.count !== undefined" class="tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <div class="header-actions">
      <div class="search-wrap">
        <AdminIcon name="search" class="search-icon" />
        <input
          v-model="keyword"
          class="search-input"
          :placeholder="props.searchPlaceholder"
          @keyup.enter="emit('search')"
        />
        <button v-if="keyword" class="search-clear" title="清除" @click="keyword = ''">
          <AdminIcon name="close" />
        </button>
      </div>
      <button class="icon-btn" title="重置筛选" @click="emit('reset')">
        <AdminIcon name="reset" />
      </button>
      <button v-if="props.actionLabel" class="primary-btn" @click="emit('action')">
        <AdminIcon name="plus" />
        {{ props.actionLabel }}
      </button>
    </div>
  </div>
</template>
