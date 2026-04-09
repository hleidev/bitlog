<template>
  <div class="not-found">
    <el-result icon="warning" title="404" sub-title="你访问的页面不存在">
      <template #extra>
        <el-button type="primary" @click="$router.push('/admin/dashboard')">返回仪表盘</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
}
</style>
