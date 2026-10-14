<template>
  <div>
    <div class="search-bar">
      <el-form inline>
        <el-form-item label="券名称">
          <el-input v-model="query.keyword" placeholder="券名称关键词" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="admin-page-header">
        <div class="admin-page-title">优惠券列表</div>
        <el-button type="primary" icon="Plus" v-perm="'coupon:add'" @click="openDialog()">新增优惠券</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="券名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 1 ? 'danger' : 'warning'" effect="plain">
              {{ row.type === 1 ? '满减' : '折扣' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠力度" width="150">
          <template #default="{ row }">
            <span class="text-price">
              {{ row.type === 1 ? `满 ${row.minAmount} 减 ${row.amount}` : `${row.discount} 折（满 ${row.minAmount}）` }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="发行/领取/使用" width="200">
          <template #default="{ row }">
            <div class="text-muted" style="font-size: 12px">
              {{ row.totalCount }} / {{ row.receivedCount }} / {{ row.usedCount }}
            </div>
            <el-progress
              :percentage="row.totalCount ? Math.min(100, Math.round((row.receivedCount / row.totalCount) * 100)) : 0"
              :stroke-width="6"
              :show-text="false"
            />
          </template>
        </el-table-column>
        <el-table-column prop="perLimit" label="每人限领" width="90" align="center" />
        <el-table-column label="适用范围" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.scopeNames || scopeLabel(row.scope) }}</template>
        </el-table-column>
        <el-table-column label="有效期" width="210">
          <template #default="{ row }">
            <div style="font-size: 12px">{{ formatDate(row.startTime) }} ~ {{ formatDate(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-perm="'coupon:edit'" @click="openDialog(row)">编辑</el-button>
            <el-button link type="primary" @click="openRecords(row)">领取记录</el-button>
            <el-button link type="danger" v-perm="'coupon:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑优惠券' : '新增优惠券'" width="620px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="券名称" prop="name">
          <el-input v-model="form.name" placeholder="如：新人专享 满100减20" maxlength="50" />
        </el-form-item>
        <el-form-item label="优惠类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="满减金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="10" />
          <span class="text-muted ml-8">元</span>
        </el-form-item>
        <el-form-item v-else label="折扣率" prop="discount">
          <el-input-number v-model="form.discount" :min="0.1" :max="9.9" :precision="1" :step="0.5" />
          <span class="text-muted ml-8">折（如 8.5 表示 8.5 折）</span>
        </el-form-item>
        <el-form-item label="使用门槛" prop="minAmount">
          <el-input-number v-model="form.minAmount" :min="0" :precision="2" :step="10" />
          <span class="text-muted ml-8">元，0 表示无门槛</span>
        </el-form-item>
        <el-form-item label="发行总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" :max="1000000" />
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perLimit" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="适用范围">
          <el-radio-group v-model="form.scope">
            <el-radio :value="1">全场通用</el-radio>
            <el-radio :value="2">指定分类</el-radio>
            <el-radio :value="3">指定商品</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scope === 2" label="选择分类">
          <el-tree
            ref="categoryTreeRef"
            :data="categoryTree"
            show-checkbox
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            :default-checked-keys="form.scopeIds"
            class="scope-tree"
          />
        </el-form-item>
        <el-form-item v-if="form.scope === 3" label="商品ID">
          <el-input v-model="scopeIdsText" placeholder="多个商品ID用英文逗号分隔，如：1,5,9" />
        </el-form-item>
        <el-form-item label="有效期" prop="timeRange">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="生效时间"
            end-placeholder="失效时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="使用说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 领取记录 -->
    <el-drawer v-model="recordsVisible" title="领取记录" size="620px">
      <el-table v-loading="recordsLoading" :data="records" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="couponName" label="券名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="(COUPON_STATUS[row.status] || {}).type">
              {{ (COUPON_STATUS[row.status] || {}).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="领取时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.receiveTime) }}</template>
        </el-table-column>
        <el-table-column label="使用时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.useTime) }}</template>
        </el-table-column>
      </el-table>
      <Pagination v-model:page="recordQuery.page" v-model:size="recordQuery.size" :total="recordTotal" @change="loadRecords" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import {
  addCoupon,
  deleteCoupon,
  getCouponPage,
  getCouponRecords,
  updateCoupon,
  updateCouponStatus
} from '@/api/admin/marketing'
import { getAdminCategoryTree } from '@/api/admin/product'
import { COUPON_STATUS, formatDate, formatDateTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: null, page: 1, size: 10 })

const dialogVisible = ref(false)
const formRef = ref()
const categoryTreeRef = ref()
const categoryTree = ref([])
const timeRange = ref([])
const scopeIdsText = ref('')

const form = reactive({
  id: null,
  name: '',
  type: 1,
  amount: 10,
  discount: 9,
  minAmount: 100,
  totalCount: 1000,
  perLimit: 1,
  scope: 1,
  scopeIds: [],
  status: 1,
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入券名称', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入满减金额', trigger: 'blur' }],
  discount: [{ required: true, message: '请输入折扣率', trigger: 'blur' }],
  minAmount: [{ required: true, message: '请输入使用门槛', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入发行总量', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请选择有效期', trigger: 'change' }]
}

const recordsVisible = ref(false)
const recordsLoading = ref(false)
const records = ref([])
const recordTotal = ref(0)
const recordQuery = reactive({ page: 1, size: 10 })
const currentCouponId = ref(null)

const scopeLabel = (scope) => ({ 1: '全场通用', 2: '指定分类', 3: '指定商品' }[scope] || '全场通用')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCouponPage({ ...query })
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
  Object.assign(query, { keyword: '', status: null, page: 1 })
  loadData()
}

const loadCategoryTree = async () => {
  if (categoryTree.value.length) return
  const res = await getAdminCategoryTree()
  categoryTree.value = res.data || []
}

const openDialog = async (row) => {
  await loadCategoryTree()
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      type: row.type,
      amount: Number(row.amount),
      discount: Number(row.discount),
      minAmount: Number(row.minAmount),
      totalCount: row.totalCount,
      perLimit: row.perLimit,
      scope: row.scope,
      scopeIds: [],
      status: row.status,
      description: row.description
    })
    timeRange.value = [row.startTime, row.endTime]
    scopeIdsText.value = row.scope === 3 ? row.scopeIds || '' : ''
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      type: 1,
      amount: 10,
      discount: 9,
      minAmount: 100,
      totalCount: 1000,
      perLimit: 1,
      scope: 1,
      scopeIds: [],
      status: 1,
      description: ''
    })
    timeRange.value = []
    scopeIdsText.value = ''
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!timeRange.value || timeRange.value.length !== 2) {
    ElMessage.warning('请选择有效期')
    return
  }
  if (form.scope === 2 && categoryTreeRef.value) {
    form.scopeIds = categoryTreeRef.value.getCheckedKeys()
    if (!form.scopeIds.length) {
      ElMessage.warning('请至少选择一个分类')
      return
    }
  }
  if (form.scope === 3 && !scopeIdsText.value.trim()) {
    ElMessage.warning('请填写指定商品ID')
    return
  }

  saving.value = true
  try {
    const payload = {
      name: form.name,
      type: form.type,
      amount: form.type === 1 ? form.amount : 0,
      discount: form.type === 2 ? form.discount : 10,
      minAmount: form.minAmount,
      totalCount: form.totalCount,
      perLimit: form.perLimit,
      scope: form.scope,
      scopeIds: form.scope === 2 ? form.scopeIds : form.scope === 3 ? scopeIdsText.value.split(',').map((v) => Number(v.trim())).filter(Boolean) : [],
      startTime: timeRange.value[0],
      endTime: timeRange.value[1],
      status: form.status,
      description: form.description
    }
    if (form.id) {
      await updateCoupon(form.id, payload)
    } else {
      await addCoupon(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleStatusChange = async (row, val) => {
  await updateCouponStatus(row.id, val)
  row.status = val
  ElMessage.success(val === 1 ? '优惠券已启用' : '优惠券已停用')
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除优惠券「${row.name}」吗？已被领取的券无法删除。`, '删除优惠券', {
    type: 'warning'
  })
  await deleteCoupon(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const openRecords = (row) => {
  currentCouponId.value = row.id
  recordQuery.page = 1
  recordsVisible.value = true
  loadRecords()
}

const loadRecords = async () => {
  recordsLoading.value = true
  try {
    const res = await getCouponRecords(currentCouponId.value, { ...recordQuery })
    records.value = res.data.records || []
    recordTotal.value = Number(res.data.total || 0)
  } finally {
    recordsLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.ml-8 {
  margin-left: 8px;
  font-size: 12px;
}

.scope-tree {
  width: 100%;
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px;
}
</style>
