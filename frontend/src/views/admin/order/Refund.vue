<template>
  <div class="refund-page">
    <div class="admin-page-header">
      <span class="admin-page-title">售后工单</span>
    </div>

    <!-- 搜索栏 -->
    <el-form class="search-bar" :model="query" inline @submit.prevent>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="退款单号 / 订单号" clearable style="width: 220px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="(item, key) in REFUND_STATUS" :key="key" :label="item.label" :value="Number(key)" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 售后表格 -->
    <div class="page-card">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="refundNo" label="售后单号" width="190" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="订单号" width="190" show-overflow-tooltip />
        <el-table-column label="买家" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.buyerNickname || `ID:${row.userId}` }}</template>
        </el-table-column>
        <el-table-column label="商品" min-width="230">
          <template #default="{ row }">
            <div class="goods-cell">
              <el-image :src="row.productImage" fit="cover" class="goods-thumb">
                <template #error>
                  <div class="img-error"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
              <div class="goods-info text-ellipsis">{{ row.productName || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ REFUND_TYPE[row.type] || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退款金额" width="110" align="right">
          <template #default="{ row }">
            <span class="text-price">{{ formatPrice(row.refundAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="refundStatusType(row.status)" size="small">{{ refundStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="审核人" width="110">
          <template #default="{ row }">{{ row.auditUser || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 0" v-perm="'refund:audit'" link type="primary" @click="openAudit(row)">
                审核
              </el-button>
              <el-button
                v-if="row.status === 3"
                v-perm="'refund:audit'"
                link
                type="success"
                @click="handleFinish(row)"
              >
                退款完成
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <!-- 售后详情抽屉 -->
    <el-drawer v-model="detailVisible" title="售后详情" size="620px">
      <div v-loading="detailLoading">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="售后单号">{{ detail.refundNo }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="买家">{{ detail.buyerNickname || `ID:${detail.userId}` }}</el-descriptions-item>
          <el-descriptions-item label="售后类型">{{ REFUND_TYPE[detail.type] || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="售后状态">
            <el-tag :type="refundStatusType(detail.status)" size="small">{{ refundStatusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="退款金额">
            <span class="text-price">{{ formatPrice(detail.refundAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="申请原因">{{ detail.reason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="问题描述">{{ detail.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ detail.auditUser || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ formatDateTime(detail.auditTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核意见">{{ detail.auditRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatDateTime(detail.finishTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="mt-16">
          <div class="block-title mb-12">关联商品</div>
          <div class="goods-cell">
            <el-image :src="detail.productImage" fit="cover" class="goods-thumb" :preview-src-list="detailImages" preview-teleported>
              <template #error>
                <div class="img-error"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <div>{{ detail.productName || '-' }}</div>
          </div>
        </div>

        <div class="mt-16">
          <div class="block-title mb-12">凭证图片</div>
          <div v-if="detailImages.length" class="proof-list">
            <el-image
              v-for="(url, index) in detailImages"
              :key="index"
              :src="url"
              fit="cover"
              class="proof-thumb"
              :preview-src-list="detailImages"
              :initial-index="index"
              preview-teleported
            >
              <template #error>
                <div class="img-error"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
          </div>
          <el-empty v-else description="买家未上传凭证" :image-size="70" />
        </div>
      </div>
    </el-drawer>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="售后审核" width="520px">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="100px">
        <el-form-item label="售后单号">
          <span>{{ currentRow.refundNo }}</span>
        </el-form-item>
        <el-form-item label="申请退款">
          <span class="text-price">{{ formatPrice(currentRow.refundAmount) }}</span>
        </el-form-item>
        <el-form-item label="处理结果" prop="status">
          <el-radio-group v-model="auditForm.status">
            <el-radio :value="1">同意</el-radio>
            <el-radio :value="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input-number v-model="auditForm.refundAmount" :min="0" :precision="2" :step="1" style="width: 200px" />
          <div class="form-tip">可在申请金额基础上调整；退货退款同意后需等待买家寄回。</div>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="请填写审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAudit">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditRefund, getAdminRefundDetail, getAdminRefundPage } from '@/api/admin/order'
import Pagination from '@/components/Pagination.vue'
import { REFUND_STATUS, REFUND_TYPE, formatDateTime, formatPrice, refundStatusLabel, splitImages } from '@/utils/format'

/** 售后状态标签类型（REFUND_STATUS 中已包含 type 字段） */
const refundStatusType = (status) => (REFUND_STATUS[status] || {}).type || 'info'

/** 查询条件 */
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })
const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)

const cleanParams = (source) => {
  const result = {}
  Object.keys(source).forEach((key) => {
    const value = source[key]
    if (value !== '' && value !== null && value !== undefined) {
      result[key] = value
    }
  })
  return result
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAdminRefundPage(cleanParams({ ...query }))
    const data = res.data || {}
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.page = 1
  loadData()
}

const handleReset = () => {
  query.keyword = ''
  query.status = ''
  query.page = 1
  loadData()
}

/* ============================ 详情 ============================ */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})
const currentRow = ref({})

const detailImages = computed(() => splitImages(detail.value.images))

const openDetail = async (row) => {
  currentRow.value = row
  detail.value = {}
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getAdminRefundDetail(row.id)
    detail.value = res.data || {}
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    detailLoading.value = false
  }
}

/* ============================ 审核 ============================ */
const auditVisible = ref(false)
const auditFormRef = ref()
const auditForm = reactive({ status: 1, auditRemark: '', refundAmount: 0 })
const auditRules = {
  status: [{ required: true, message: '请选择处理结果', trigger: 'change' }]
}

const openAudit = (row) => {
  currentRow.value = row
  auditForm.status = 1
  auditForm.auditRemark = ''
  auditForm.refundAmount = Number(row.refundAmount || 0)
  auditVisible.value = true
}

const submitAudit = async () => {
  if (!auditFormRef.value) return
  try {
    await auditFormRef.value.validate()
  } catch (e) {
    return
  }
  if (auditForm.status === 1 && auditForm.refundAmount <= 0) {
    ElMessage.warning('同意退款时退款金额必须大于 0')
    return
  }
  submitting.value = true
  try {
    await auditRefund(currentRow.value.id, {
      status: auditForm.status,
      auditRemark: auditForm.auditRemark,
      refundAmount: auditForm.refundAmount
    })
    ElMessage.success(auditForm.status === 1 ? '已同意该售后申请' : '已拒绝该售后申请')
    auditVisible.value = false
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

/* ============================ 退款完成 ============================ */
const handleFinish = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认买家已寄回商品并完成为其退款 ${formatPrice(row.refundAmount)} 吗？`,
      '退款完成',
      { type: 'warning', confirmButtonText: '确认完成', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await auditRefund(row.id, { status: 4, auditRemark: '商家确认收到退货，退款完成' })
    ElMessage.success('该售后单已退款完成')
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

onMounted(loadData)
</script>

<style scoped>
.goods-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.goods-thumb {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  flex-shrink: 0;
  border: 1px solid #ebeef5;
}

.goods-info {
  min-width: 0;
  flex: 1;
  font-size: 13px;
}

.img-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
}

.block-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.proof-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.proof-thumb {
  width: 96px;
  height: 96px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin-top: 4px;
}
</style>
