<template>
  <div class="page-card">
    <div class="admin-page-header">
      <div class="admin-page-title">退款/售后</div>
      <el-radio-group v-model="query.status" @change="handleStatusChange">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">待审核</el-radio-button>
        <el-radio-button :value="1">已同意</el-radio-button>
        <el-radio-button :value="3">待退货</el-radio-button>
        <el-radio-button :value="4">退款完成</el-radio-button>
        <el-radio-button :value="2">已拒绝</el-radio-button>
      </el-radio-group>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />

    <template v-else>
      <el-table v-if="list.length" :data="list" border>
        <el-table-column label="售后单号" prop="refundNo" min-width="180" />
        <el-table-column label="订单号" prop="orderNo" min-width="180" />
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="flex gap-8" style="align-items: center">
              <el-image :src="row.productImage" fit="cover" class="table-image">
                <template #error><div class="image-fallback">暂无图</div></template>
              </el-image>
              <span class="text-ellipsis" style="max-width: 140px">{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ refundTypeName(row.type) }}</template>
        </el-table-column>
        <el-table-column label="退款金额" width="110" align="right">
          <template #default="{ row }">
            <span class="text-price">{{ formatPrice(row.refundAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusName || refundStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" link type="danger" @click="handleCancel(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无售后记录" />
    </template>

    <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />

    <el-drawer v-model="drawerVisible" title="售后详情" size="480px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="售后单号">{{ detail.refundNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="售后类型">{{ refundTypeName(detail.type) }}</el-descriptions-item>
        <el-descriptions-item label="申请原因">{{ detail.reason }}</el-descriptions-item>
        <el-descriptions-item label="问题描述">{{ detail.description || '无' }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">
          <span class="text-price">{{ formatPrice(detail.refundAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="statusTag(detail.status)" size="small">
            {{ detail.statusName || refundStatusLabel(detail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detail.auditUser || '待审核' }}</el-descriptions-item>
        <el-descriptions-item label="审核意见">{{ detail.auditRemark || '无' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ formatDateTime(detail.auditTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDateTime(detail.finishTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import { cancelRefund, getRefundDetail, getRefundPage } from '@/api/order'
import { REFUND_TYPE, formatDateTime, formatPrice, refundStatusLabel } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ status: null, page: 1, size: 10 })

const drawerVisible = ref(false)
const detail = ref(null)

const refundTypeName = (type) => REFUND_TYPE[type] || '其它'
const statusTag = (status) =>
  ({ 0: 'warning', 1: 'primary', 2: 'danger', 3: 'warning', 4: 'success', 5: 'info' }[status] || 'info')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRefundPage({ ...query })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  query.page = 1
  loadData()
}

const openDetail = async (row) => {
  const res = await getRefundDetail(row.id)
  detail.value = res.data
  drawerVisible.value = true
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确定撤销该售后申请吗？撤销后订单将恢复到申请前的状态。', '撤销售后', { type: 'warning' })
  await cancelRefund(row.id)
  ElMessage.success('已撤销售后申请')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.table-image {
  width: 44px;
  height: 44px;
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
</style>
