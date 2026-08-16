<script setup lang="ts">
/**
 * 批量操作条。结构固定为「已选 N X ── 业务动作 ── 取消选择」。
 *
 * 原本各页各摆各的：评论页把「取消选择」当成第一个 ghost-btn 塞进动作区，
 * 分类/标签页还在动作区里放了「全选」——表头 checkbox 已经能全选，是重复入口。
 * 业务动作走默认插槽，取消选择由本组件固定放在最右。
 */
const props = defineProps<{
  count: number
  /** 每页锁死一个量词：篇 / 人 / 条 / 个 */
  unit: string
}>()

const emit = defineEmits<{ clear: [] }>()
</script>

<template>
  <Transition name="sel-bar">
    <div v-if="props.count > 0" class="selection-bar">
      <span class="sel-count">
        已选 <b>{{ props.count }}</b> {{ props.unit }}
      </span>
      <div class="sel-actions">
        <slot />
      </div>
      <button class="cancel-btn" @click="emit('clear')">取消选择</button>
    </div>
  </Transition>
</template>
