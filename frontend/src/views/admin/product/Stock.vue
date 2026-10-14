<template>
  <div>
    <el-alert type="warning" :closable="false" class="mb-16">
      <template #title>
        库存预警规则：SKU 库存 ≤ 预警阈值（当前阈值：<b>{{ threshold }}</b>）时进入本列表，请及时补货。
        <el-button link type="primary" v-perm="'stock:edit'" @click="openConfigDialog">修改阈值</el-button>
      </template>
    </el-alert>

    <div class="search-bar">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="商品名称 / SKU编码" clearable style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column label="商品图" width="90" align="center">
          <template #default="{ row }">
            <el-image :src="row.mainImage" fit="cover" class="table-image" :preview-src-list="[row.mainImage]" preview-teleported>
              <template #error><div class="image-fallback">暂无图</div></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="specInfo" label="规格" min-width="150" show-overflow-tooltip />
        <el-table-column label="当前库存" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.stock <= 0" type="danger" effect="dark">已售罄</el-tag>
            <el-tag v-else type="warning" effect="plain">{{ row.stock }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warnStock" label="预警阈值" width="100" align="center" />
        <el-table-column label="商品状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.productStatus === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ row.productStatus === 1 ? '上架中' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button link type="primary" v-perm="'stock:edit'" @click="openStockDialog(row)">调整库存</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <!-- 调整库存 -->
    <el-dialog v-model="stockDialogVisible" title="调整库存" width="460px" append-to-body>
      <el-descriptions v-if="current" :column="1" border class="mb-16">
        <el-descriptions-item label="商品">{{ current.productName }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ current.specInfo || '默认' }}</el-descriptions-item>
        <el-descriptions-item label="SKU编码">{{ current.skuCode }}</el-descriptions-item>
        <el-descriptions-item label="当前库存">{{ current.stock }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="90px">
        <el-form-item label="新库存">
          <el-input-number v-model="newStock" :min="0" :max="999999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleUpdateStock">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改阈值 -->
    <el-dialog v-model="configDialogVisible" title="修改库存预警阈值" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="阈值">
          <el-input-number v-model="thresholdInput" :min="0" :max="9999" />
          <div class="text-muted" style="font-size: 12px; margin-top: 6px">
            SKU 库存小于等于该值时进入预警列表
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleUpdateConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import { getStockConfig, getStockWarningPage, updateSkuStock, updateStockConfig } from '@/api/admin/product'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const threshold = ref(10)
const thresholdInput = ref(10)
const query = reactive({ keyword: '', page: 1, size: 10 })

const stockDialogVisible = ref(false)
const configDialogVisible = ref(false)
const current = ref(null)
const newStock = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getStockWarningPage({ ...query })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

const loadConfig = async () => {
  try {
    const res = await getStockConfig()
    threshold.value = res.data.threshold
    thresholdInput.value = res.data.threshold
  } catch (e) {
    /* 忽略 */
  }
}

const handleSearch = () => {
  query.page = 1
  loadData()
}

const handleReset = () => {
  query.keyword = ''
  query.page = 1
  loadData()
}

const openStockDialog = (row) => {
  current.value = row
  newStock.value = row.stock
  stockDialogVisible.value = true
}

const handleUpdateStock = async () => {
  saving.value = true
  try {
    await updateSkuStock(current.value.skuId, newStock.value)
    ElMessage.success('库存已更新')
    stockDialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const openConfigDialog = () => {
  thresholdInput.value = threshold.value
  configDialogVisible.value = true
}

const handleUpdateConfig = async () => {
  saving.value = true
  try {
    await updateStockConfig(thresholdInput.value)
    ElMessage.success('预警阈值已更新')
    configDialogVisible.value = false
    await loadConfig()
    loadData()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
  loadData()
})
</script>

<style scoped>
.table-image {
  width: 52px;
  height: 52px;
  border-radius: 4px;
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
