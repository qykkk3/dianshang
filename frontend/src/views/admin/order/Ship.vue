<template>
  <div class="ship-page">
    <div class="admin-page-header">
      <span class="admin-page-title">发货管理</span>
    </div>

    <!-- 待发货提示 -->
    <el-alert type="warning" :closable="false" show-icon class="mb-16">
      <template #title>
        待发货订单 <strong>{{ pendingTotal }}</strong> 笔，请及时处理，避免买家等待过久。
      </template>
    </el-alert>

    <!-- ======================= 待发货订单 ======================= -->
    <div class="page-card mb-16">
      <div class="flex-between mb-16">
        <span class="section-title">待发货订单</span>
        <el-button :icon="'Refresh'" @click="loadPending">刷新</el-button>
      </div>

      <el-table v-loading="pendingLoading" :data="pendingList" border stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <el-table :data="row.items || []" size="small" border>
                <el-table-column label="商品图" width="80">
                  <template #default="scope">
                    <el-image :src="scope.row.productImage" fit="cover" class="item-thumb">
                      <template #error>
                        <div class="img-error"><el-icon><Picture /></el-icon></div>
                      </template>
                    </el-image>
                  </template>
                </el-table-column>
                <el-table-column prop="productName" label="商品名称" min-width="220" show-overflow-tooltip />
                <el-table-column prop="specInfo" label="规格" width="160" show-overflow-tooltip>
                  <template #default="scope">{{ scope.row.specInfo || '默认规格' }}</template>
                </el-table-column>
                <el-table-column label="单价" width="100" align="right">
                  <template #default="scope">{{ formatPrice(scope.row.price) }}</template>
                </el-table-column>
                <el-table-column prop="quantity" label="数量" width="80" align="center" />
                <el-table-column label="小计" width="110" align="right">
                  <template #default="scope">
                    <span class="text-price">{{ formatPrice(scope.row.totalAmount) }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" width="190" show-overflow-tooltip />
        <el-table-column label="买家" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.buyerNickname || `ID:${row.userId}` }}</template>
        </el-table-column>
        <el-table-column label="收货人 / 电话 / 地址" min-width="280">
          <template #default="{ row }">
            <div>{{ row.receiverName }} {{ row.receiverPhone }}</div>
            <div class="text-muted text-ellipsis">{{ row.receiverAddress }}</div>
          </template>
        </el-table-column>
        <el-table-column label="商品明细" width="110" align="center">
          <template #default="{ row }">{{ row.itemCount || (row.items || []).length }} 件</template>
        </el-table-column>
        <el-table-column label="实付金额" width="110" align="right">
          <template #default="{ row }">
            <span class="text-price">{{ formatPrice(row.payAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="下单 / 支付时间" width="180">
          <template #default="{ row }">
            <div>{{ formatDateTime(row.createTime) }}</div>
            <div class="text-muted">{{ formatDateTime(row.payTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'order:doShip'" link type="success" @click="openShip(row)">发货</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="pendingQuery.page" v-model:size="pendingQuery.size" :total="pendingTotal" @change="loadPending" />
    </div>

    <!-- ======================= 已发货订单 ======================= -->
    <div class="page-card">
      <div class="flex-between mb-16">
        <span class="section-title">已发货订单（可维护物流轨迹）</span>
        <el-button :icon="'Refresh'" @click="loadShipped">刷新</el-button>
      </div>

      <el-table v-loading="shippedLoading" :data="shippedList" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="190" show-overflow-tooltip />
        <el-table-column label="买家" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.buyerNickname || `ID:${row.userId}` }}</template>
        </el-table-column>
        <el-table-column label="快递公司" width="130">
          <template #default="{ row }">{{ row.expressCompany || '-' }}</template>
        </el-table-column>
        <el-table-column label="快递单号" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.expressNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="收货人 / 地址" min-width="260">
          <template #default="{ row }">
            <div>{{ row.receiverName }} {{ row.receiverPhone }}</div>
            <div class="text-muted text-ellipsis">{{ row.receiverAddress }}</div>
          </template>
        </el-table-column>
        <el-table-column label="发货时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.shipTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openTracks(row)">物流轨迹</el-button>
              <el-button v-perm="'order:track'" link type="primary" @click="openAddTrack(row)">追加轨迹</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="shippedQuery.page" v-model:size="shippedQuery.size" :total="shippedTotal" @change="loadShipped" />
    </div>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipVisible" title="订单发货" width="520px">
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ currentRow.orderNo }}</span>
        </el-form-item>
        <el-form-item label="收货信息">
          <span>{{ currentRow.receiverName }} {{ currentRow.receiverPhone }}</span>
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

    <!-- 物流轨迹抽屉 -->
    <el-drawer v-model="trackVisible" title="物流轨迹" size="480px">
      <div v-loading="trackLoading">
        <el-descriptions :column="1" border size="small" class="mb-16">
          <el-descriptions-item label="订单号">{{ currentRow.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="快递公司">{{ currentRow.expressCompany || '-' }}</el-descriptions-item>
          <el-descriptions-item label="快递单号">{{ currentRow.expressNo || '-' }}</el-descriptions-item>
        </el-descriptions>

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
        <el-empty v-else description="暂无物流轨迹" />
      </div>
    </el-drawer>

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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { addOrderTrack, getAdminOrderDetail, getAdminOrderPage, shipOrder } from '@/api/admin/order'
import Pagination from '@/components/Pagination.vue'
import { formatDateTime, formatPrice, orderStatusLabel, orderStatusType } from '@/utils/format'

/** 快递公司可选值（支持自定义输入） */
const EXPRESS_COMPANIES = ['顺丰速运', '中通快递', '圆通速递', '京东物流', '韵达快递']
/** 物流节点状态 */
const TRACK_STATUS = ['已揽收', '运输中', '派送中', '已签收']

/* ============================ 待发货 ============================ */
const pendingQuery = reactive({ status: 1, page: 1, size: 10 })
const pendingList = ref([])
const pendingTotal = ref(0)
const pendingLoading = ref(false)

const loadPending = async () => {
  pendingLoading.value = true
  try {
    const res = await getAdminOrderPage({ ...pendingQuery })
    const data = res.data || {}
    pendingList.value = data.records || []
    pendingTotal.value = data.total || 0
  } catch (e) {
    pendingList.value = []
    pendingTotal.value = 0
  } finally {
    pendingLoading.value = false
  }
}

/* ============================ 已发货 ============================ */
const shippedQuery = reactive({ status: 2, page: 1, size: 10 })
const shippedList = ref([])
const shippedTotal = ref(0)
const shippedLoading = ref(false)

const loadShipped = async () => {
  shippedLoading.value = true
  try {
    const res = await getAdminOrderPage({ ...shippedQuery })
    const data = res.data || {}
    shippedList.value = data.records || []
    shippedTotal.value = data.total || 0
  } catch (e) {
    shippedList.value = []
    shippedTotal.value = 0
  } finally {
    shippedLoading.value = false
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
const currentRow = ref({})
const submitting = ref(false)

const openShip = (row) => {
  currentRow.value = row
  shipForm.expressCompany = row.expressCompany || '顺丰速运'
  shipForm.expressNo = row.expressNo || ''
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
    await shipOrder(currentRow.value.id, { expressCompany: shipForm.expressCompany, expressNo: shipForm.expressNo })
    ElMessage.success('发货成功')
    shipVisible.value = false
    loadPending()
    loadShipped()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

/* ============================ 物流轨迹 ============================ */
const trackVisible = ref(false)
const trackLoading = ref(false)
const tracks = ref([])

const openTracks = async (row) => {
  currentRow.value = row
  tracks.value = []
  trackVisible.value = true
  trackLoading.value = true
  try {
    const res = await getAdminOrderDetail(row.id)
    tracks.value = (res.data && res.data.tracks) || []
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    trackLoading.value = false
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

const openAddTrack = (row) => {
  currentRow.value = row
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
    await addOrderTrack(currentRow.value.id, { ...trackForm })
    ElMessage.success('物流轨迹已追加')
    addTrackVisible.value = false
    if (trackVisible.value) {
      openTracks(currentRow.value)
    }
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPending()
  loadShipped()
})
</script>

<style scoped>
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.expand-box {
  padding: 8px 40px;
  background: #fafafa;
}

.item-thumb {
  width: 44px;
  height: 44px;
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

.track-status {
  font-weight: 600;
  color: #303133;
}
</style>
