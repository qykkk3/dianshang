<template>
  <div class="page-container mt-16">
    <div class="page-card">
      <div class="admin-page-title mb-16">我的订单</div>

      <!-- 状态标签 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane v-for="tab in tabs" :key="tab.name" :name="tab.name">
          <template #label>
            <span>
              {{ tab.label }}
              <el-badge
                v-if="tab.countKey && counts[tab.countKey] > 0"
                :value="counts[tab.countKey]"
                class="tab-badge"
                type="danger"
              />
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <el-skeleton v-if="loading" :rows="6" animated />

      <template v-else>
        <div v-if="list.length" class="order-list">
          <div v-for="order in list" :key="order.id" class="order-card">
            <div class="order-head">
              <div class="flex gap-16">
                <span class="order-no">订单号：{{ order.orderNo }}</span>
                <span class="text-muted">{{ formatDateTime(order.createTime) }}</span>
              </div>
              <el-tag :type="orderStatusType(order.status)" effect="light">
                {{ order.statusName || orderStatusLabel(order.status) }}
              </el-tag>
            </div>

            <div class="order-body">
              <div class="order-goods">
                <div
                  v-for="item in order.items || []"
                  :key="item.id"
                  class="goods-row"
                  @click="$router.push(`/order/${order.orderNo}`)"
                >
                  <el-image :src="item.productImage" fit="cover" class="goods-image">
                    <template #error><div class="image-fallback">暂无图</div></template>
                  </el-image>
                  <div class="goods-info">
                    <div class="goods-name text-ellipsis-2">{{ item.productName }}</div>
                    <div class="goods-spec text-muted">{{ item.specInfo }}</div>
                  </div>
                  <div class="goods-price">
                    {{ formatPrice(item.price) }} × {{ item.quantity }}
                  </div>
                  <div class="goods-total">{{ formatPrice(item.totalAmount) }}</div>
                </div>
              </div>

              <div class="order-side">
                <div class="side-amount">
                  <div class="amount-label">实付金额</div>
                  <div class="amount-value text-price">{{ formatPrice(order.payAmount) }}</div>
                  <div class="text-muted side-count">共 {{ order.itemCount }} 件商品</div>
                </div>
              </div>
            </div>

            <div class="order-foot">
              <el-button link type="primary" @click="$router.push(`/order/${order.orderNo}`)">订单详情</el-button>

              <template v-if="order.status === 0">
                <el-button type="danger" @click="handlePay(order)">去支付</el-button>
                <el-button @click="handleCancel(order)">取消订单</el-button>
              </template>

              <template v-if="order.status === 2">
                <el-button type="primary" @click="$router.push(`/order/${order.orderNo}`)">查看物流</el-button>
                <el-button type="success" @click="handleConfirm(order)">确认收货</el-button>
              </template>

              <template v-if="order.status === 3 || order.status === 5">
                <el-button v-if="order.status === 3" @click="openRefundDialog(order)">申请售后</el-button>
                <el-button v-if="order.status === 3" type="warning" @click="$router.push('/user/comment')">
                  去评价
                </el-button>
              </template>

              <template v-if="order.status === 3 || order.status === 4">
                <el-button link type="danger" @click="handleDelete(order)">删除订单</el-button>
              </template>
            </div>
          </div>
        </div>

        <el-empty v-else description="暂无相关订单">
          <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
        </el-empty>
      </template>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <!-- 申请售后弹窗 -->
    <el-dialog v-model="refundVisible" title="申请售后" width="560px" append-to-body>
      <el-alert
        v-if="refundOrder"
        :title="`订单号：${refundOrder.orderNo} ｜ 实付 ${formatPrice(refundOrder.payAmount)}`"
        type="info"
        :closable="false"
        class="mb-16"
      />
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="90px">
        <el-form-item label="售后商品">
          <el-select v-model="refundForm.orderItemId" placeholder="请选择商品" clearable style="width: 100%">
            <el-option
              v-for="item in refundOrder ? refundOrder.items || [] : []"
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
          <span class="text-muted ml-8">默认按订单实付金额</span>
        </el-form-item>
        <el-form-item label="问题描述" prop="description">
          <el-input
            v-model="refundForm.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请描述遇到的问题，便于客服快速处理"
          />
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
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import {
  cancelOrder,
  confirmOrder,
  deleteOrder,
  getOrderCounts,
  getOrderPage,
  payOrder,
  applyRefund
} from '@/api/order'
import { formatDateTime, formatPrice, orderStatusLabel, orderStatusType } from '@/utils/format'

const route = useRoute()

const tabs = [
  { name: 'all', label: '全部订单', countKey: null },
  { name: '0', label: '待付款', countKey: 'unpaid' },
  { name: '1', label: '待发货', countKey: 'paid' },
  { name: '2', label: '待收货', countKey: 'shipped' },
  { name: '3', label: '已完成', countKey: 'finished' }
]

const loading = ref(false)
const list = ref([])
const total = ref(0)
const activeTab = ref(String(route.query.status || 'all'))
const counts = ref({})
const query = reactive({ status: null, page: 1, size: 5 })

const refundVisible = ref(false)
const submitting = ref(false)
const refundFormRef = ref()
const refundOrder = ref(null)
const refundForm = reactive({ orderItemId: null, type: 1, reason: '', description: '', refundAmount: 0 })
const refundRules = {
  type: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
  reason: [{ required: true, message: '请选择申请原因', trigger: 'change' }]
}
const refundReasons = ['不想要了', '商品与描述不符', '尺码不合适', '质量问题', '发错货了', '快递太慢', '其它原因']

const syncStatus = () => {
  query.status = activeTab.value === 'all' ? null : Number(activeTab.value)
}

const loadData = async () => {
  syncStatus()
  loading.value = true
  try {
    const res = await getOrderPage({ status: query.status, page: query.page, size: query.size })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
  loadCounts()
}

const loadCounts = async () => {
  try {
    const res = await getOrderCounts()
    counts.value = res.data || {}
  } catch (e) {
    counts.value = {}
  }
}

const handleTabChange = () => {
  query.page = 1
  loadData()
}

const handlePay = async (order) => {
  await ElMessageBox.confirm(
    `订单 ${order.orderNo} 需支付 ${formatPrice(order.payAmount)}，确认立即支付吗？（演示环境为模拟支付）`,
    '模拟支付',
    { type: 'warning', confirmButtonText: '立即支付' }
  )
  await payOrder(order.orderNo)
  ElMessage.success('支付成功')
  loadData()
}

const handleCancel = async (order) => {
  await ElMessageBox.confirm('确定要取消该订单吗？取消后库存将自动回滚。', '取消订单', { type: 'warning' })
  await cancelOrder(order.orderNo)
  ElMessage.success('订单已取消')
  loadData()
}

const handleConfirm = async (order) => {
  await ElMessageBox.confirm('确认已收到商品吗？', '确认收货', { type: 'warning' })
  await confirmOrder(order.orderNo)
  ElMessage.success('已确认收货，感谢您的购买')
  loadData()
}

const handleDelete = async (order) => {
  await ElMessageBox.confirm('删除后订单将不在列表中显示，确定删除吗？', '删除订单', { type: 'warning' })
  await deleteOrder(order.orderNo)
  ElMessage.success('订单已删除')
  loadData()
}

const openRefundDialog = (order) => {
  refundOrder.value = order
  refundForm.orderItemId = order.items && order.items.length === 1 ? order.items[0].id : null
  refundForm.type = 1
  refundForm.reason = ''
  refundForm.description = ''
  refundForm.refundAmount = Number(order.payAmount || 0)
  refundVisible.value = true
}

const submitRefund = async () => {
  const valid = await refundFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!refundOrder.value) return
  submitting.value = true
  try {
    await applyRefund({
      orderId: refundOrder.value.id,
      orderItemId: refundForm.orderItemId,
      type: refundForm.type,
      reason: refundForm.reason,
      description: refundForm.description,
      refundAmount: refundForm.refundAmount
    })
    ElMessage.success('售后申请已提交，请等待客服审核')
    refundVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.tab-badge {
  margin-left: 4px;
  vertical-align: middle;
}

.order-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fafafa;
  font-size: 13px;
}

.order-no {
  font-weight: 600;
  color: #303133;
}

.order-body {
  display: flex;
  border-bottom: 1px solid #f2f6fc;
}

.order-goods {
  flex: 1;
  padding: 6px 0;
}

.goods-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
}

.goods-row:hover {
  background: #fffaf7;
}

.goods-image {
  width: 64px;
  height: 64px;
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

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-name {
  font-size: 14px;
  color: #303133;
}

.goods-spec {
  font-size: 12px;
  margin-top: 4px;
}

.goods-price,
.goods-total {
  width: 120px;
  text-align: right;
  font-size: 13px;
  color: #606266;
}

.goods-total {
  color: #303133;
  font-weight: 600;
}

.order-side {
  width: 200px;
  border-left: 1px solid #f2f6fc;
  display: flex;
  align-items: center;
  justify-content: center;
}

.side-amount {
  text-align: center;
}

.amount-label {
  font-size: 12px;
  color: #909399;
}

.amount-value {
  font-size: 18px;
  margin: 4px 0;
}

.side-count {
  font-size: 12px;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;
}

.ml-8 {
  margin-left: 8px;
}
</style>
