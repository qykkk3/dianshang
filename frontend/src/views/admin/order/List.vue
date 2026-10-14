<template>
  <div class="order-list-page">
    <div class="admin-page-header">
      <span class="admin-page-title">订单列表</span>
      <el-button
        v-perm="'order:export'"
        type="success"
        :icon="'Download'"
        :loading="exporting"
        @click="handleExport"
      >
        导出订单
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <el-form class="search-bar" :model="query" inline @submit.prevent>
      <el-form-item label="订单号">
        <el-input v-model="query.orderNo" placeholder="请输入订单号" clearable style="width: 190px" />
      </el-form-item>
      <el-form-item label="订单状态">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="(item, key) in ORDER_STATUS" :key="key" :label="item.label" :value="Number(key)" />
        </el-select>
      </el-form-item>
      <el-form-item label="买家ID">
        <el-input v-model="query.userId" placeholder="请输入买家ID" clearable style="width: 130px" />
      </el-form-item>
      <el-form-item label="下单时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          unlink-panels
          style="width: 250px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 订单表格 -->
    <div class="page-card">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="190" show-overflow-tooltip />
        <el-table-column prop="buyerNickname" label="买家昵称" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.buyerNickname || '-' }}</template>
        </el-table-column>

        <!-- 商品：首个商品图 + 等 N 件商品 -->
        <el-table-column label="商品" min-width="240">
          <template #default="{ row }">
            <div class="goods-cell">
              <el-image :src="firstItem(row).productImage" fit="cover" class="goods-thumb">
                <template #error>
                  <div class="img-error"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
              <div class="goods-info">
                <div class="text-ellipsis">{{ firstItem(row).productName || '商品信息缺失' }}</div>
                <div class="text-muted">等 {{ row.itemCount || (row.items || []).length }} 件商品</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="商品总额" width="110" align="right">
          <template #default="{ row }">{{ formatPrice(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="实付金额" width="110" align="right">
          <template #default="{ row }">
            <span class="text-price">{{ formatPrice(row.payAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="快递信息" width="180">
          <template #default="{ row }">
            <div v-if="row.expressNo">
              <div>{{ row.expressCompany || '快递' }}</div>
              <div class="text-muted">{{ row.expressNo }}</div>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="goDetail(row)">详情</el-button>
              <el-button v-perm="'order:remark'" link type="primary" @click="openRemark(row)">备注</el-button>
              <el-button
                v-if="row.status === 0"
                v-perm="'order:price'"
                link
                type="primary"
                @click="openPrice(row)"
              >
                改价
              </el-button>
              <el-button
                v-if="row.status === 0"
                v-perm="'order:close'"
                link
                type="danger"
                @click="handleClose(row)"
              >
                关闭
              </el-button>
              <el-button
                v-if="row.status === 1"
                v-perm="'order:doShip'"
                link
                type="success"
                @click="openShip(row)"
              >
                发货
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <!-- 备注弹窗 -->
    <el-dialog v-model="remarkVisible" title="订单备注" width="480px">
      <el-form label-width="90px">
        <el-form-item label="订单号">
          <span>{{ currentRow.orderNo }}</span>
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
      <el-form label-width="100px">
        <el-form-item label="订单号">
          <span>{{ currentRow.orderNo }}</span>
        </el-form-item>
        <el-form-item label="原实付金额">
          <span class="text-price">{{ formatPrice(currentRow.payAmount) }}</span>
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
          <span>{{ currentRow.orderNo }}</span>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { closeOrder, exportOrders, getAdminOrderPage, priceOrder, remarkOrder, shipOrder } from '@/api/admin/order'
import Pagination from '@/components/Pagination.vue'
import { ORDER_STATUS, formatDateTime, formatPrice, orderStatusLabel, orderStatusType } from '@/utils/format'

const router = useRouter()

/** 快递公司可选值（支持自定义输入） */
const EXPRESS_COMPANIES = ['顺丰速运', '中通快递', '圆通速递', '京东物流', '韵达快递']

/** 查询条件 */
const query = reactive({
  orderNo: '',
  status: '',
  userId: '',
  startTime: '',
  endTime: '',
  page: 1,
  size: 10
})
const dateRange = ref([])

const loading = ref(false)
const exporting = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)

/** 过滤空值，避免后端把空字符串解析为非法数值 */
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

/** 组装查询参数 */
const buildParams = () => {
  const params = { ...query }
  if (dateRange.value && dateRange.value.length === 2) {
    params.startTime = `${dateRange.value[0]} 00:00:00`
    params.endTime = `${dateRange.value[1]} 23:59:59`
  }
  return cleanParams(params)
}

/** 加载订单分页 */
const loadData = async () => {
  loading.value = true
  try {
    const res = await getAdminOrderPage(buildParams())
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
  query.orderNo = ''
  query.status = ''
  query.userId = ''
  query.startTime = ''
  query.endTime = ''
  dateRange.value = []
  query.page = 1
  loadData()
}

/** 首个订单明细（列表接口已带 items） */
const firstItem = (row) => (row.items && row.items.length ? row.items[0] : {})

const goDetail = (row) => router.push(`/admin/order/detail/${row.id}`)

/** 导出订单 */
const handleExport = async () => {
  exporting.value = true
  try {
    await exportOrders(buildParams())
    ElMessage.success('订单导出成功')
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    exporting.value = false
  }
}

/* ------------------------------ 备注 ------------------------------ */
const remarkVisible = ref(false)
const remarkForm = reactive({ adminRemark: '' })
const currentRow = ref({})

const openRemark = (row) => {
  currentRow.value = row
  remarkForm.adminRemark = row.adminRemark || ''
  remarkVisible.value = true
}

const submitRemark = async () => {
  submitting.value = true
  try {
    await remarkOrder(currentRow.value.id, remarkForm.adminRemark)
    ElMessage.success('备注已保存')
    remarkVisible.value = false
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

/* ------------------------------ 改价 ------------------------------ */
const priceVisible = ref(false)
const priceForm = reactive({ payAmount: 0 })

const openPrice = (row) => {
  currentRow.value = row
  priceForm.payAmount = Number(row.payAmount || 0)
  priceVisible.value = true
}

const submitPrice = async () => {
  if (!priceForm.payAmount || priceForm.payAmount <= 0) {
    ElMessage.warning('请输入正确的实付金额')
    return
  }
  submitting.value = true
  try {
    await priceOrder(currentRow.value.id, priceForm.payAmount)
    ElMessage.success('改价成功')
    priceVisible.value = false
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

/* ------------------------------ 关闭订单 ------------------------------ */
const handleClose = async (row) => {
  try {
    await ElMessageBox.confirm(`确定关闭订单 ${row.orderNo} 吗？关闭后买家将无法继续支付。`, '关闭订单', {
      type: 'warning',
      confirmButtonText: '确定关闭',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await closeOrder(row.id)
    ElMessage.success('订单已关闭')
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

/* ------------------------------ 发货 ------------------------------ */
const shipVisible = ref(false)
const shipFormRef = ref()
const shipForm = reactive({ expressCompany: '顺丰速运', expressNo: '' })
const shipRules = {
  expressCompany: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  expressNo: [{ required: true, message: '请填写快递单号', trigger: 'blur' }]
}

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
  line-height: 1.6;
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
</style>
