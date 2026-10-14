<template>
  <div class="page-container mt-16">
    <div class="page-card">
      <el-page-header @back="$router.push('/orders')">
        <template #content>
          <span class="page-title">订单详情</span>
        </template>
      </el-page-header>

      <el-skeleton v-if="loading" :rows="8" animated class="mt-20" />

      <template v-else-if="order">
        <!-- 状态步骤条 -->
        <div class="status-steps">
          <el-steps :active="activeStep" align-center finish-status="success">
            <el-step title="提交订单" :description="formatDateTime(order.createTime)" />
            <el-step title="付款成功" :description="formatDateTime(order.payTime)" />
            <el-step title="商品发货" :description="formatDateTime(order.shipTime)" />
            <el-step title="确认收货" :description="formatDateTime(order.receiveTime)" />
            <el-step title="交易完成" :description="formatDateTime(order.finishTime)" />
          </el-steps>
          <el-alert
            v-if="order.status === 4"
            title="该订单已取消"
            type="info"
            :closable="false"
            class="mt-16"
            :description="`取消时间：${formatDateTime(order.closeTime)}`"
          />
          <el-alert
            v-else-if="order.status === 5"
            title="该订单正在售后处理中"
            type="warning"
            :closable="false"
            class="mt-16"
          />
        </div>

        <!-- 操作栏 -->
        <div class="action-bar">
          <el-tag :type="orderStatusType(order.status)" size="large" effect="dark">
            {{ order.statusName || orderStatusLabel(order.status) }}
          </el-tag>
          <div class="flex gap-8">
            <template v-if="order.status === 0">
              <el-button type="danger" @click="handlePay">立即支付</el-button>
              <el-button @click="handleCancel">取消订单</el-button>
            </template>
            <template v-if="order.status === 2">
              <el-button type="success" @click="handleConfirm">确认收货</el-button>
            </template>
            <template v-if="order.status === 3">
              <el-button @click="openRefundDialog">申请售后</el-button>
              <el-button type="warning" @click="$router.push('/user/comment')">去评价</el-button>
            </template>
            <template v-if="order.status === 3 || order.status === 4">
              <el-button link type="danger" @click="handleDelete">删除订单</el-button>
            </template>
          </div>
        </div>

        <!-- 收货信息 -->
        <el-descriptions title="收货信息" :column="2" border class="mb-20">
          <el-descriptions-item label="收货人">{{ order.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ order.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ order.receiverAddress }}</el-descriptions-item>
          <el-descriptions-item label="买家备注">{{ order.remark || '无' }}</el-descriptions-item>
          <el-descriptions-item label="商家备注">{{ order.adminRemark || '无' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 订单信息 -->
        <el-descriptions title="订单信息" :column="2" border class="mb-20">
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatDateTime(order.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ order.payTime ? '模拟支付' : '未支付' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ formatDateTime(order.payTime) }}</el-descriptions-item>
          <el-descriptions-item label="快递公司">{{ order.expressCompany || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="快递单号">{{ order.expressNo || '暂无' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 商品明细 -->
        <div class="section-title">商品明细</div>
        <el-table :data="order.items || []" border>
          <el-table-column label="商品" min-width="300">
            <template #default="{ row }">
              <div class="flex gap-12" style="align-items: center">
                <el-image :src="row.productImage" fit="cover" class="table-image">
                  <template #error><div class="image-fallback">暂无图</div></template>
                </el-image>
                <div>
                  <div class="goods-name">{{ row.productName }}</div>
                  <div class="text-muted" style="font-size: 12px">{{ row.specInfo }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120" align="right">
            <template #default="{ row }">{{ formatPrice(row.price) }}</template>
          </el-table-column>
          <el-table-column label="数量" width="90" align="center" prop="quantity" />
          <el-table-column label="小计" width="140" align="right">
            <template #default="{ row }">
              <span class="text-price">{{ formatPrice(row.totalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="评价状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isCommented ? 'success' : 'info'" size="small" effect="plain">
                {{ row.isCommented ? '已评价' : '未评价' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 金额明细 -->
        <div class="amount-panel">
          <div class="amount-row">
            <span>商品总额</span><span>{{ formatPrice(order.totalAmount) }}</span>
          </div>
          <div class="amount-row">
            <span>运费</span><span>{{ formatPrice(order.freightAmount) }}</span>
          </div>
          <div class="amount-row">
            <span>优惠抵扣</span><span class="text-success">-{{ formatPrice(order.discountAmount) }}</span>
          </div>
          <el-divider class="amount-divider" />
          <div class="amount-row total">
            <span>实付金额</span><span class="text-price">{{ formatPrice(order.payAmount) }}</span>
          </div>
        </div>

        <!-- 物流轨迹 -->
        <template v-if="order.tracks && order.tracks.length">
          <div class="section-title">物流轨迹</div>
          <el-timeline class="track-timeline">
            <el-timeline-item
              v-for="track in order.tracks"
              :key="track.id"
              :timestamp="formatDateTime(track.acceptTime)"
              :type="track.status === '已签收' ? 'success' : 'primary'"
              placement="top"
            >
              <div class="track-status">{{ track.status }}</div>
              <div class="track-context">{{ track.context }}</div>
              <div class="text-muted" style="font-size: 12px">{{ track.location }}</div>
            </el-timeline-item>
          </el-timeline>
        </template>

        <!-- 售后记录 -->
        <template v-if="order.refunds && order.refunds.length">
          <div class="section-title">售后记录</div>
          <el-table :data="order.refunds" border>
            <el-table-column label="售后单号" prop="refundNo" min-width="180" />
            <el-table-column label="类型" width="110">
              <template #default="{ row }">{{ refundTypeName(row.type) }}</template>
            </el-table-column>
            <el-table-column label="退款金额" width="120" align="right">
              <template #default="{ row }">{{ formatPrice(row.refundAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="refundStatusType(row.status)">
                  {{ row.statusName || refundStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核意见" prop="auditRemark" min-width="180" show-overflow-tooltip />
            <el-table-column label="申请时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </template>

      <el-empty v-else description="订单不存在" />
    </div>

    <!-- 申请售后弹窗 -->
    <el-dialog v-model="refundVisible" title="申请售后" width="560px" append-to-body>
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="90px">
        <el-form-item label="售后商品">
          <el-select v-model="refundForm.orderItemId" placeholder="整单售后可不选" clearable style="width: 100%">
            <el-option
              v-for="item in order ? order.items || [] : []"
              :key="item.id"
              :label="`${item.productName}（${item.specInfo || '默认'}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="售后类型" prop="type">
          <el-radio-group v-model="refundForm.type">
            <el-radio :value="1">仅退款</el-radio>
            <el-radio :value="2">退货退款</el-radio>
            <el-radio :value="3">换货</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="申请原因" prop="reason">
          <el-select v-model="refundForm.reason" placeholder="请选择原因" style="width: 100%">
            <el-option v-for="reason in refundReasons" :key="reason" :label="reason" :value="reason" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input-number v-model="refundForm.refundAmount" :min="0.01" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="refundForm.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  applyRefund,
  cancelOrder,
  confirmOrder,
  deleteOrder,
  getOrderDetail,
  payOrder
} from '@/api/order'
import {
  formatDateTime,
  formatPrice,
  orderStatusLabel,
  orderStatusType,
  refundStatusLabel
} from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const order = ref(null)
const refundVisible = ref(false)
const submitting = ref(false)
const refundFormRef = ref()

const refundForm = reactive({ orderItemId: null, type: 1, reason: '', description: '', refundAmount: 0 })
const refundRules = {
  type: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
  reason: [{ required: true, message: '请选择申请原因', trigger: 'change' }]
}
const refundReasons = ['不想要了', '商品与描述不符', '尺码不合适', '质量问题', '发错货了', '快递太慢', '其它原因']

/** 步骤条进度 */
const activeStep = computed(() => {
  if (!order.value) return 0
  const status = order.value.status
  if (status === 4) return Math.min(order.value.payTime ? 2 : 1, 5)
  if (status === 0) return 1
  if (status === 1 || status === 5) return 2
  if (status === 2) return 3
  if (status === 3) return 5
  return 0
})

const refundTypeName = (type) => ({ 1: '仅退款', 2: '退货退款', 3: '换货' }[type] || '其它')
const refundStatusType = (status) =>
  ({ 0: 'warning', 1: 'primary', 2: 'danger', 3: 'warning', 4: 'success', 5: 'info' }[status] || 'info')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.orderNo)
    order.value = res.data
  } catch (e) {
    order.value = null
  } finally {
    loading.value = false
  }
}

const handlePay = async () => {
  await ElMessageBox.confirm(
    `需支付 ${formatPrice(order.value.payAmount)}，确认立即支付吗？（演示环境为模拟支付）`,
    '模拟支付',
    { type: 'warning', confirmButtonText: '立即支付' }
  )
  await payOrder(order.value.orderNo)
  ElMessage.success('支付成功')
  loadData()
}

const handleCancel = async () => {
  await ElMessageBox.confirm('确定要取消该订单吗？取消后库存将自动回滚。', '取消订单', { type: 'warning' })
  await cancelOrder(order.value.orderNo)
  ElMessage.success('订单已取消')
  loadData()
}

const handleConfirm = async () => {
  await ElMessageBox.confirm('确认已收到商品吗？', '确认收货', { type: 'warning' })
  await confirmOrder(order.value.orderNo)
  ElMessage.success('已确认收货')
  loadData()
}

const handleDelete = async () => {
  await ElMessageBox.confirm('删除后订单将不在列表中显示，确定删除吗？', '删除订单', { type: 'warning' })
  await deleteOrder(order.value.orderNo)
  ElMessage.success('订单已删除')
  router.push('/orders')
}

const openRefundDialog = () => {
  refundForm.orderItemId = order.value.items && order.value.items.length === 1 ? order.value.items[0].id : null
  refundForm.type = 1
  refundForm.reason = ''
  refundForm.description = ''
  refundForm.refundAmount = Number(order.value.payAmount || 0)
  refundVisible.value = true
}

const submitRefund = async () => {
  const valid = await refundFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await applyRefund({
      orderId: order.value.id,
      orderItemId: refundForm.orderItemId,
      type: refundForm.type,
      reason: refundForm.reason,
      description: refundForm.description,
      refundAmount: refundForm.refundAmount
    })
    ElMessage.success('售后申请已提交')
    refundVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-title {
  font-size: 16px;
  font-weight: 600;
}

.status-steps {
  padding: 26px 20px 10px;
  background: #fafcff;
  border-radius: 8px;
  margin-bottom: 16px;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: #fff8f5;
  border: 1px solid #ffe4d4;
  border-radius: 8px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 24px 0 12px;
  padding-left: 10px;
  border-left: 3px solid #ff5000;
}

.table-image {
  width: 52px;
  height: 52px;
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

.goods-name {
  font-size: 14px;
  color: #303133;
}

.amount-panel {
  width: 320px;
  margin-left: auto;
  margin-top: 18px;
  background: #fafafa;
  border-radius: 8px;
  padding: 16px 18px;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
  margin-bottom: 10px;
}

.amount-row.total {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 0;
}

.amount-divider {
  margin: 6px 0 12px;
}

.track-timeline {
  padding-left: 6px;
}

.track-status {
  font-weight: 600;
  color: #303133;
}

.track-context {
  font-size: 13px;
  color: #606266;
  margin: 4px 0;
}
</style>
