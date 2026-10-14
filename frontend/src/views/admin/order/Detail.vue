<template>
  <div class="order-detail-page" v-loading="loading">
    <div class="admin-page-header">
      <div class="flex gap-12" style="align-items: center">
        <el-button :icon="'ArrowLeft'" @click="goBack">返回</el-button>
        <span class="admin-page-title">订单详情</span>
        <el-tag v-if="order.orderNo" :type="orderStatusType(order.status)" size="small">
          {{ orderStatusLabel(order.status) }}
        </el-tag>
      </div>
      <div class="table-actions">
        <el-button v-perm="'order:remark'" :icon="'EditPen'" @click="openRemark">备注</el-button>
        <el-button v-if="order.status === 0" v-perm="'order:price'" :icon="'Money'" @click="openPrice">改价</el-button>
        <el-button v-if="order.status === 0" v-perm="'order:close'" type="danger" plain :icon="'CircleClose'" @click="handleClose">
          关闭订单
        </el-button>
        <el-button v-if="order.status === 1" v-perm="'order:doShip'" type="success" :icon="'Van'" @click="openShip">
          发货
        </el-button>
      </div>
    </div>

    <!-- 订单状态步骤条 -->
    <div class="page-card mb-16">
      <el-steps :active="stepActive" :process-status="stepStatus" finish-status="success" align-center>
        <el-step title="提交订单" :description="formatDateTime(order.createTime)" />
        <el-step title="买家付款" :description="formatDateTime(order.payTime)" />
        <el-step title="商家发货" :description="formatDateTime(order.shipTime)" />
        <el-step title="交易完成" :description="formatDateTime(order.finishTime || order.receiveTime)" />
      </el-steps>
      <el-alert v-if="order.status === 4" type="info" :closable="false" show-icon class="mt-16" title="该订单已关闭 / 取消" />
      <el-alert
        v-else-if="order.status === 5"
        type="error"
        :closable="false"
        show-icon
        class="mt-16"
        title="该订单正在售后处理中，可在「售后工单」中查看与审核"
      />
    </div>

    <!-- 基础信息 -->
    <div class="page-card mb-16">
      <div class="block-title mb-16">订单基础信息</div>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="订单号">{{ order.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="orderStatusType(order.status)" size="small">{{ orderStatusLabel(order.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ order.payType ? '模拟支付' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatDateTime(order.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatDateTime(order.payTime) }}</el-descriptions-item>
        <el-descriptions-item label="发货时间">{{ formatDateTime(order.shipTime) }}</el-descriptions-item>
        <el-descriptions-item label="收货时间">{{ formatDateTime(order.receiveTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDateTime(order.finishTime) }}</el-descriptions-item>
        <el-descriptions-item label="关闭时间">{{ formatDateTime(order.closeTime) }}</el-descriptions-item>
        <el-descriptions-item label="买家">
          {{ order.buyerNickname || '-' }} <span class="text-muted">(ID: {{ order.userId }})</span>
        </el-descriptions-item>
        <el-descriptions-item label="买家备注">{{ order.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商家备注">{{ order.adminRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 收货信息 -->
    <div class="page-card mb-16">
      <div class="block-title mb-16">收货信息</div>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="收货人">{{ order.receiverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ order.receiverPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ order.receiverAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快递公司">{{ order.expressCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快递单号">{{ order.expressNo || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 商品明细 -->
    <div class="page-card mb-16">
      <div class="block-title mb-16">商品明细</div>
      <el-table :data="order.items || []" border size="small">
        <el-table-column label="商品图" width="80">
          <template #default="{ row }">
            <el-image :src="row.productImage" fit="cover" class="item-thumb">
              <template #error>
                <div class="img-error"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="240" show-overflow-tooltip />
        <el-table-column label="规格" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.specInfo || '默认规格' }}</template>
        </el-table-column>
        <el-table-column label="单价" width="110" align="right">
          <template #default="{ row }">{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column label="小计" width="120" align="right">
          <template #default="{ row }">
            <span class="text-price">{{ formatPrice(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="是否已评价" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isCommented ? 'success' : 'info'" size="small">
              {{ row.isCommented ? '已评价' : '未评价' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 金额明细 -->
      <div class="amount-box mt-16">
        <div class="amount-item">
          <span class="text-muted">商品总额：</span>{{ formatPrice(order.totalAmount) }}
        </div>
        <div class="amount-item">
          <span class="text-muted">运费：</span>{{ formatPrice(order.freightAmount) }}
        </div>
        <div class="amount-item">
          <span class="text-muted">优惠：</span>-{{ formatPrice(order.discountAmount) }}
        </div>
        <div class="amount-item">
          <span class="text-muted">实付金额：</span>
          <span class="text-price">{{ formatPrice(order.payAmount) }}</span>
        </div>
      </div>
    </div>

    <!-- 物流轨迹 -->
    <div class="page-card mb-16">
      <div class="flex-between mb-16">
        <span class="block-title">物流轨迹</span>
        <el-button v-if="order.expressNo" v-perm="'order:track'" link type="primary" @click="openAddTrack">
          追加轨迹
        </el-button>
      </div>
      <el-timeline v-if="tracks.length">
        <el-timeline-item
          v-for="item in tracks"
          :key="item.id"
          :timestamp="formatDateTime(item.acceptTime)"
          placement="top"
          :type="item.status === '已签收' ? 'success' : 'primary'"
        >
          <div class="track-status">{{ item.status }}</div>
          <div>{{ item.context }}</div>
          <div v-if="item.location" class="text-muted">{{ item.location }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无物流轨迹" :image-size="80" />
    </div>

    <!-- 售后记录 -->
    <div class="page-card">
      <div class="block-title mb-16">售后记录</div>
      <el-table :data="refunds" border size="small">
        <el-table-column prop="refundNo" label="售后单号" width="190" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">{{ REFUND_TYPE[row.type] || '未知' }}</template>
        </el-table-column>
        <el-table-column label="退款金额" width="110" align="right">
          <template #default="{ row }">{{ formatPrice(row.refundAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="refundStatusType(row.status)" size="small">{{ refundStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申请原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="auditUser" label="审核人" width="110">
          <template #default="{ row }">{{ row.auditUser || '-' }}</template>
        </el-table-column>
        <el-table-column label="审核时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.auditTime) }}</template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 备注弹窗 -->
    <el-dialog v-model="remarkVisible" title="订单备注" width="480px">
      <el-form label-width="90px">
        <el-form-item label="订单号">
          <span>{{ order.orderNo }}</span>
        </el-form-item>
        <el-form-item label="商家备注">
          <el-input v-model="remarkForm.adminRemark" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="请输入商家备注，仅后台可见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remarkVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRemark">确定</el-button>
      </template>
    </el-dialog>

    <!-- 改价弹窗 -->
    <el-dialog v-model="priceVisible" title="订单改价" width="460px">
      <el-form label-width="110px">
        <el-form-item label="订单号">
          <span>{{ order.orderNo }}</span>
        </el-form-item>
        <el-form-item label="原实付金额">
          <span class="text-price">{{ formatPrice(order.payAmount) }}</span>
        </el-form-item>
        <el-form-item label="新实付金额">
          <el-input-number v-model="priceForm.payAmount" :min="0.01" :precision="2" :step="1" style="width: 200px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priceVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPrice">确定改价</el-button>
      </template>
    </el-dialog>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipVisible" title="订单发货" width="500px">
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ order.orderNo }}</span>
        </el-form-item>
        <el-form-item label="快递公司" prop="expressCompany">
          <el-select v-model="shipForm.expressCompany" placeholder="请选择或输入快递公司" filterable allow-create style="width: 100%">
            <el-option v-for="item in EXPRESS_COMPANIES" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号" prop="expressNo">
          <div class="flex gap-8" style="width: 100%">
            <el-input v-model="shipForm.expressNo" placeholder="请输入快递单号" style="flex: 1" />
            <el-button :icon="'MagicStick'" @click="generateExpressNo">自动生成</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 追加轨迹弹窗 -->
    <el-dialog v-model="addTrackVisible" title="追加物流轨迹" width="500px">
      <el-form ref="trackFormRef" :model="trackForm" :rules="trackRules" label-width="90px">
        <el-form-item label="节点状态" prop="status">
          <el-select v-model="trackForm.status" placeholder="请选择节点状态" style="width: 100%">
            <el-option v-for="item in TRACK_STATUS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="轨迹描述" prop="context">
          <el-input v-model="trackForm.context" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="如：快件已到达【杭州转运中心】" />
        </el-form-item>
        <el-form-item label="所在城市">
          <el-input v-model="trackForm.location" placeholder="如：杭州市" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addTrackVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAddTrack">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addOrderTrack, closeOrder, getAdminOrderDetail, priceOrder, remarkOrder, shipOrder } from '@/api/admin/order'
import {
  REFUND_STATUS,
  REFUND_TYPE,
  formatDateTime,
  formatPrice,
  orderStatusLabel,
  orderStatusType,
  refundStatusLabel
} from '@/utils/format'

const route = useRoute()
const router = useRouter()

/** 快递公司可选值（支持自定义输入） */
const EXPRESS_COMPANIES = ['顺丰速运', '中通快递', '圆通速递', '京东物流', '韵达快递']
/** 物流节点状态 */
const TRACK_STATUS = ['已揽收', '运输中', '派送中', '已签收']

const loading = ref(false)
const submitting = ref(false)
const order = ref({})
const tracks = ref([])
const refunds = ref([])

/** 售后状态标签类型 */
const refundStatusType = (status) => (REFUND_STATUS[status] || {}).type || 'info'

/** 步骤条当前进度：下单 -> 付款 -> 发货 -> 完成 */
const stepActive = computed(() => {
  const status = order.value.status
  if (status === 0) return 1
  if (status === 1) return 2
  if (status === 2) return 3
  if (status === 3) return 4
  if (status === 4) return Number(order.value.payTime ? 2 : 1)
  return 3
})

/** 步骤条状态：已取消显示 error */
const stepStatus = computed(() => (order.value.status === 4 ? 'error' : 'process'))

/** 加载订单详情 */
const loadData = async () => {
  const id = route.params.id
  if (!id) {
    ElMessage.error('缺少订单ID')
    return
  }
  loading.value = true
  try {
    const res = await getAdminOrderDetail(id)
    const data = res.data || {}
    order.value = data
    tracks.value = data.tracks || []
    refunds.value = data.refunds || []
  } catch (e) {
    order.value = {}
    tracks.value = []
    refunds.value = []
  } finally {
    loading.value = false
  }
}

const goBack = () => router.push('/admin/order/list')

/* ============================ 备注 ============================ */
const remarkVisible = ref(false)
const remarkForm = reactive({ adminRemark: '' })

const openRemark = () => {
  remarkForm.adminRemark = order.value.adminRemark || ''
  remarkVisible.value = true
}

const submitRemark = async () => {
  submitting.value = true
  try {
    await remarkOrder(order.value.id, remarkForm.adminRemark)
    ElMessage.success('备注已保存')
    remarkVisible.value = false
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

/* ============================ 改价 ============================ */
const priceVisible = ref(false)
const priceForm = reactive({ payAmount: 0 })

const openPrice = () => {
  priceForm.payAmount = Number(order.value.payAmount || 0)
  priceVisible.value = true
}

const submitPrice = async () => {
  if (!priceForm.payAmount || priceForm.payAmount <= 0) {
    ElMessage.warning('请输入正确的实付金额')
    return
  }
  submitting.value = true
  try {
    await priceOrder(order.value.id, priceForm.payAmount)
    ElMessage.success('改价成功')
    priceVisible.value = false
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

/* ============================ 关闭订单 ============================ */
const handleClose = async () => {
  try {
    await ElMessageBox.confirm(`确定关闭订单 ${order.value.orderNo} 吗？关闭后买家将无法继续支付。`, '关闭订单', {
      type: 'warning',
      confirmButtonText: '确定关闭',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await closeOrder(order.value.id)
    ElMessage.success('订单已关闭')
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

/* ============================ 发货 ============================ */
const shipVisible = ref(false)
const shipFormRef = ref()
const shipForm = reactive({ expressCompany: '顺丰速运', expressNo: '' })
const shipRules = {
  expressCompany: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  expressNo: [{ required: true, message: '请填写快递单号', trigger: 'blur' }]
}

const openShip = () => {
  shipForm.expressCompany = order.value.expressCompany || '顺丰速运'
  shipForm.expressNo = order.value.expressNo || ''
  shipVisible.value = true
}

/** 自动生成快递单号：SF + 12 位随机数字 */
const generateExpressNo = () => {
  let random = ''
  for (let i = 0; i < 12; i += 1) {
    random += Math.floor(Math.random() * 10)
  }
  shipForm.expressNo = `SF${random}`
}

const submitShip = async () => {
  if (!shipFormRef.value) return
  try {
    await shipFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await shipOrder(order.value.id, { expressCompany: shipForm.expressCompany, expressNo: shipForm.expressNo })
    ElMessage.success('发货成功')
    shipVisible.value = false
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

/* ============================ 追加轨迹 ============================ */
const addTrackVisible = ref(false)
const trackFormRef = ref()
const trackForm = reactive({ status: '运输中', context: '', location: '' })
const trackRules = {
  status: [{ required: true, message: '请选择节点状态', trigger: 'change' }],
  context: [{ required: true, message: '请填写轨迹描述', trigger: 'blur' }]
}

const openAddTrack = () => {
  trackForm.status = '运输中'
  trackForm.context = ''
  trackForm.location = ''
  addTrackVisible.value = true
}

const submitAddTrack = async () => {
  if (!trackFormRef.value) return
  try {
    await trackFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await addOrderTrack(order.value.id, { ...trackForm })
    ElMessage.success('物流轨迹已追加')
    addTrackVisible.value = false
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.block-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.item-thumb {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
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

.amount-box {
  display: flex;
  justify-content: flex-end;
  gap: 32px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
  font-size: 14px;
}

.track-status {
  font-weight: 600;
  color: #303133;
}
</style>
