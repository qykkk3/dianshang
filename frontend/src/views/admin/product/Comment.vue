<template>
  <div>
    <div class="search-bar">
      <el-form inline>
        <el-form-item label="商品ID">
          <el-input v-model="query.productId" placeholder="商品ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="星级">
          <el-select v-model="query.star" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="n in 5" :key="n" :label="`${n} 星`" :value="n" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="显示" :value="1" />
            <el-option label="隐藏" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="评价内容" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="flex gap-8" style="align-items: center">
              <el-image :src="row.productImage" fit="cover" class="table-image">
                <template #error><div class="image-fallback">暂无图</div></template>
              </el-image>
              <div style="min-width: 0">
                <div class="text-ellipsis" style="max-width: 150px">{{ row.productName }}</div>
                <div class="text-muted" style="font-size: 12px">{{ row.specInfo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="130">
          <template #default="{ row }">
            {{ row.userNickname }}
            <el-tag v-if="row.isAnonymous === 1" size="small" type="info" effect="plain">匿名</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="150" align="center">
          <template #default="{ row }">
            <el-rate :model-value="row.star" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="240">
          <template #default="{ row }">
            <div class="comment-text">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column label="商家回复" min-width="180">
          <template #default="{ row }">
            <span v-if="row.replyContent" class="reply-text">{{ row.replyContent }}</span>
            <span v-else class="text-muted">未回复</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评价时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-perm="'comment:reply'" @click="openReplyDialog(row)">回复</el-button>
            <el-button link type="primary" v-perm="'comment:status'" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '隐藏' : '显示' }}
            </el-button>
            <el-button link type="danger" v-perm="'comment:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <el-dialog v-model="replyVisible" title="回复评价" width="520px" append-to-body>
      <div v-if="current" class="origin-comment">
        <div class="flex gap-8" style="align-items: center; margin-bottom: 8px">
          <el-rate :model-value="current.star" disabled size="small" />
          <span class="text-muted" style="font-size: 12px">{{ current.userNickname }}</span>
        </div>
        <div>{{ current.content }}</div>
      </div>
      <el-form class="mt-16">
        <el-form-item label="回复内容">
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入回复内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import { deleteComment, getCommentPage, replyComment, updateCommentStatus } from '@/api/admin/marketing'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ productId: '', star: null, status: null, keyword: '', page: 1, size: 10 })

const replyVisible = ref(false)
const current = ref(null)
const replyContent = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCommentPage({ ...query })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.page = 1
  loadData()
}

const handleReset = () => {
  Object.assign(query, { productId: '', star: null, status: null, keyword: '', page: 1 })
  loadData()
}

const openReplyDialog = (row) => {
  current.value = row
  replyContent.value = row.replyContent || ''
  replyVisible.value = true
}

const handleReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  saving.value = true
  try {
    await replyComment(current.value.id, replyContent.value)
    ElMessage.success('回复成功')
    replyVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleToggleStatus = async (row) => {
  const next = row.status === 1 ? 0 : 1
  await updateCommentStatus(row.id, next)
  row.status = next
  ElMessage.success(next === 1 ? '评价已显示' : '评价已隐藏')
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('删除后不可恢复，确定删除该评价吗？', '删除评价', { type: 'warning' })
  await deleteComment(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.table-image {
  width: 48px;
  height: 48px;
  border-radius: 4px;
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

.comment-text {
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  font-size: 13px;
  color: #606266;
}

.reply-text {
  font-size: 13px;
  color: #ff5000;
}

.origin-comment {
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  font-size: 13px;
  color: #606266;
}
</style>
