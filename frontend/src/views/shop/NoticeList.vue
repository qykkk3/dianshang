<template>
  <div class="page-container mt-16">
    <div class="page-card">
      <div class="flex-between mb-16">
        <div class="admin-page-title">公告中心</div>
        <el-radio-group v-model="query.type" size="default" @change="handleTypeChange">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="1">平台公告</el-radio-button>
          <el-radio-button :value="2">促销活动</el-radio-button>
          <el-radio-button :value="3">帮助中心</el-radio-button>
        </el-radio-group>
      </div>

      <el-skeleton v-if="loading" :rows="5" animated />

      <template v-else>
        <div v-if="list.length" class="notice-list">
          <div v-for="item in list" :key="item.id" class="notice-item" @click="goDetail(item.id)">
            <div class="notice-main">
              <div class="notice-title">
                <el-tag :type="typeTag(item.type)" size="small" effect="plain">{{ item.typeName }}</el-tag>
                <span class="title-text">{{ item.title }}</span>
              </div>
              <div class="notice-summary text-ellipsis">{{ plainText(item.content) }}</div>
            </div>
            <div class="notice-time">{{ formatDateTime(item.publishTime || item.createTime) }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无公告" />
      </template>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Pagination from '@/components/Pagination.vue'
import { getNoticePage } from '@/api/shop'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ type: null, page: 1, size: 10 })

const typeTag = (type) => ({ 1: 'primary', 2: 'danger', 3: 'info' }[type] || 'info')

/** 去掉 HTML 标签，用于列表摘要 */
const plainText = (html) => String(html || '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()

const loadData = async () => {
  loading.value = true
  try {
    const res = await getNoticePage({ ...query })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

const handleTypeChange = () => {
  query.page = 1
  loadData()
}

const goDetail = (id) => router.push(`/notice/${id}`)

onMounted(loadData)
</script>

<style scoped>
.notice-list {
  border-top: 1px solid #ebeef5;
}

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 6px;
  border-bottom: 1px dashed #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
}

.notice-item:hover {
  background: #fff8f5;
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.notice-item:hover .title-text {
  color: #ff5000;
}

.notice-summary {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.notice-time {
  font-size: 13px;
  color: #a8abb2;
  white-space: nowrap;
}
</style>
