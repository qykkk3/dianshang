<template>
  <div class="page-card">
    <div class="admin-page-title mb-16">我的评价</div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="待评价" name="pending" />
      <el-tab-pane label="已评价" name="done" />
    </el-tabs>

    <el-skeleton v-if="loading" :rows="4" animated />

    <template v-else>
      <!-- 待评价 -->
      <template v-if="activeTab === 'pending'">
        <div v-if="pendingList.length" class="pending-list">
          <div v-for="item in pendingList" :key="item.orderItemId" class="pending-row">
            <el-image :src="item.productImage" fit="cover" class="row-image">
              <template #error><div class="image-fallback">暂无图</div></template>
            </el-image>
            <div class="row-info">
              <div class="row-name text-ellipsis-2">{{ item.productName }}</div>
              <div class="text-muted row-spec">{{ item.specInfo }} × {{ item.quantity }}</div>
              <div class="text-muted row-time">订单号：{{ item.orderNo }} ｜ 完成时间：{{ formatDateTime(item.finishTime) }}</div>
            </div>
            <el-button type="primary" @click="openCommentDialog(item)">发表评价</el-button>
          </div>
        </div>
        <el-empty v-else description="暂无待评价的订单" />
      </template>

      <!-- 已评价 -->
      <template v-else>
        <div v-if="list.length" class="comment-list">
          <div v-for="item in list" :key="item.id" class="comment-row">
            <el-image :src="item.productImage" fit="cover" class="row-image">
              <template #error><div class="image-fallback">暂无图</div></template>
            </el-image>
            <div class="row-info">
              <div class="row-name text-ellipsis-2" @click="$router.push(`/product/${item.productId}`)">
                {{ item.productName }}
              </div>
              <div class="flex gap-12" style="align-items: center; margin: 6px 0">
                <el-rate :model-value="item.star" disabled size="small" />
                <span class="text-muted" style="font-size: 12px">{{ formatDateTime(item.createTime) }}</span>
              </div>
              <div class="comment-content">{{ item.content }}</div>
              <div v-if="item.replyContent" class="reply-block">
                <span class="reply-label">商家回复：</span>{{ item.replyContent }}
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="还没有发表过评价" />
      </template>
    </template>

    <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />

    <!-- 发表评价弹窗 -->
    <el-dialog v-model="dialogVisible" title="发表评价" width="520px" append-to-body>
      <div v-if="current" class="dialog-product">
        <el-image :src="current.productImage" fit="cover" class="dialog-image">
          <template #error><div class="image-fallback">暂无图</div></template>
        </el-image>
        <div>
          <div class="row-name">{{ current.productName }}</div>
          <div class="text-muted" style="font-size: 12px">{{ current.specInfo }}</div>
        </div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="mt-16">
        <el-form-item label="评分" prop="star">
          <el-rate v-model="form.star" show-text :texts="['很差', '较差', '一般', '满意', '非常满意']" />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说说这次的购物体验，帮助其他买家做参考～"
          />
        </el-form-item>
        <el-form-item label="匿名评价">
          <el-switch v-model="form.isAnonymous" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitComment">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import { addComment, getMyCommentPage, getPendingCommentPage } from '@/api/user'
import { formatDateTime } from '@/utils/format'

const activeTab = ref('pending')
const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const pendingList = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 5 })

const dialogVisible = ref(false)
const formRef = ref()
const current = ref(null)
const form = reactive({ star: 5, content: '', isAnonymous: 0 })

const rules = {
  star: [{ required: true, message: '请选择评分', trigger: 'change' }],
  content: [
    { required: true, message: '请填写评价内容', trigger: 'blur' },
    { min: 5, message: '评价内容至少 5 个字', trigger: 'blur' }
  ]
}

const loadData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'pending') {
      const res = await getPendingCommentPage({ ...query })
      pendingList.value = res.data.records || []
      total.value = Number(res.data.total || 0)
    } else {
      const res = await getMyCommentPage({ ...query })
      list.value = res.data.records || []
      total.value = Number(res.data.total || 0)
    }
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  query.page = 1
  loadData()
}

const openCommentDialog = (item) => {
  current.value = item
  form.star = 5
  form.content = ''
  form.isAnonymous = 0
  dialogVisible.value = true
}

const submitComment = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await addComment({
      orderItemId: current.value.orderItemId,
      star: form.star,
      content: form.content,
      images: [],
      isAnonymous: form.isAnonymous
    })
    ElMessage.success('评价成功，感谢您的反馈！')
    dialogVisible.value = false
    query.page = 1
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.pending-list,
.comment-list {
  display: flex;
  flex-direction: column;
}

.pending-row,
.comment-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 4px;
  border-bottom: 1px dashed #ebeef5;
}

.row-image {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  flex-shrink: 0;
}

.image-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 12px;
}

.row-info {
  flex: 1;
  min-width: 0;
}

.row-name {
  font-size: 14px;
  color: #303133;
  cursor: pointer;
}

.row-name:hover {
  color: #ff5000;
}

.row-spec {
  font-size: 12px;
  margin: 6px 0;
}

.row-time {
  font-size: 12px;
}

.comment-content {
  font-size: 13px;
  color: #606266;
  line-height: 20px;
  margin-top: 4px;
}

.reply-block {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 12px;
  color: #606266;
}

.reply-label {
  color: #ff5000;
  font-weight: 600;
}

.dialog-product {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
}

.dialog-image {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  flex-shrink: 0;
}
</style>
