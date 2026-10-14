<template>
  <div class="page-container mt-16">
    <div class="page-card">
      <el-page-header @back="$router.back()">
        <template #content>
          <span class="page-title">公告详情</span>
        </template>
      </el-page-header>

      <el-skeleton v-if="loading" :rows="6" animated class="mt-20" />

      <template v-else-if="notice">
        <h1 class="notice-title">{{ notice.title }}</h1>
        <div class="notice-meta">
          <el-tag :type="typeTag(notice.type)" size="small" effect="plain">{{ notice.typeName }}</el-tag>
          <span class="text-muted">发布时间：{{ formatDateTime(notice.publishTime || notice.createTime) }}</span>
        </div>
        <el-divider />
        <div class="notice-content" v-html="notice.content"></div>
      </template>

      <el-empty v-else description="公告不存在或已下线" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getNoticeDetail } from '@/api/shop'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const loading = ref(false)
const notice = ref(null)

const typeTag = (type) => ({ 1: 'primary', 2: 'danger', 3: 'info' }[type] || 'info')

onMounted(async () => {
  loading.value = true
  try {
    const res = await getNoticeDetail(route.params.id)
    notice.value = res.data
  } catch (e) {
    notice.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-title {
  font-size: 16px;
  font-weight: 600;
}

.notice-title {
  margin: 20px 0 12px;
  font-size: 24px;
  line-height: 34px;
  color: #303133;
}

.notice-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 13px;
}

.notice-content {
  font-size: 15px;
  line-height: 1.9;
  color: #303133;
}

.notice-content :deep(img) {
  max-width: 100%;
}

.notice-content :deep(h3) {
  margin: 18px 0 10px;
}
</style>
