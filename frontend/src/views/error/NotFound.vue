<template>
  <div class="not-found">
    <el-result icon="warning" title="404" sub-title="抱歉，您访问的页面不存在或已被移除">
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">返回商城首页</el-button>
        <el-button @click="$router.push('/admin/dashboard')">进入管理后台</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
// 404 页面
</script>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f5f7fa;
}
</style>
