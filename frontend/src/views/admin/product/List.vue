<template>
  <div class="product-list-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="商品名称 / 副标题 / 品牌" clearable style="width: 220px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="商品分类">
          <el-cascader
            ref="categoryCascaderRef"
            v-model="categoryPath"
            :options="categoryOptions"
            :props="cascaderProps"
            placeholder="全部分类"
            clearable
            style="width: 220px"
            @change="handleCategoryChange"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="推荐">
          <el-select v-model="query.isRecommend" placeholder="全部" clearable style="width: 120px">
            <el-option label="是" :value="1" />
            <el-option label="否" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 列表 -->
    <div class="page-card">
      <div class="admin-page-header">
        <span class="admin-page-title">商品列表</span>
        <div class="table-actions">
          <el-button v-perm="'product:add'" type="primary" :icon="'Plus'" @click="goCreate">新增商品</el-button>
          <el-button v-perm="'product:export'" :icon="'Download'" @click="handleExport">导出 Excel</el-button>
          <el-button :icon="'DocumentCopy'" @click="handleDownloadTemplate">下载导入模板</el-button>
          <el-button v-perm="'product:import'" :icon="'Upload'" @click="importVisible = true">批量导入</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border stripe row-key="id" :row-class-name="rowClassName">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="主图" width="84" align="center">
          <template #default="{ row }">
            <el-image
              class="thumb"
              :src="row.mainImage"
              :preview-src-list="row.mainImage ? [row.mainImage] : []"
              preview-teleported
              fit="cover"
            >
              <template #error>
                <div class="thumb-error">暂无图</div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column label="商品名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="goEdit(row.id)">{{ row.name }}</el-link>
            <div v-if="row.subtitle" class="sub-text text-ellipsis">{{ row.subtitle }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120" show-overflow-tooltip />
        <el-table-column prop="brand" label="品牌" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.brand || '-' }}</template>
        </el-table-column>
        <el-table-column label="价格" width="110" align="right">
          <template #default="{ row }">
            <span class="text-price">{{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="100" align="center">
          <template #default="{ row }">
            <span :class="stockClass(row)">{{ row.stock == null ? 0 : row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-if="canStatus"
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :loading="row.statusLoading"
              @change="handleStatusChange(row)"
            />
            <el-tag v-else :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="推荐" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-if="canEdit"
              v-model="row.isRecommend"
              :active-value="1"
              :inactive-value="0"
              :loading="row.recommendLoading"
              @change="handleRecommendChange(row)"
            />
            <el-tag v-else :type="row.isRecommend === 1 ? 'warning' : 'info'" size="small">
              {{ row.isRecommend === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-perm="'product:edit'" link type="primary" @click="goEdit(row.id)">编辑</el-button>
              <el-button v-perm="'product:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="importVisible" title="批量导入商品" width="480px" @closed="resetImport">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx"
        :on-change="handleFileChange"
        :on-exceed="handleExceed"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 .xlsx 文件拖到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">
            仅支持 .xlsx 格式，请先下载导入模板并按格式填写（单次建议不超过 200 条）
          </div>
        </template>
      </el-upload>
      <div class="mt-16">
        <el-button type="primary" :icon="'DocumentCopy'" @click="handleDownloadTemplate">下载导入模板</el-button>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="handleImport">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 导入结果弹窗 -->
    <el-dialog v-model="resultVisible" title="导入结果" width="560px">
      <el-alert
        :title="`导入完成：成功 ${importResult.successCount} 条，失败 ${importResult.failCount} 条`"
        :type="importResult.failCount > 0 ? 'warning' : 'success'"
        :closable="false"
        show-icon
      />
      <div v-if="importResult.errors && importResult.errors.length" class="mt-16">
        <div class="mb-12 text-muted">失败明细（最多展示 100 条）：</div>
        <el-scrollbar max-height="260px">
          <ul class="error-list">
            <li v-for="(item, index) in importResult.errors" :key="index">{{ item }}</li>
          </ul>
        </el-scrollbar>
      </div>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 商品列表：搜索、导入导出、上下架/推荐开关、编辑删除
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import {
  deleteProduct,
  downloadProductTemplate,
  exportProducts,
  getAdminCategoryTree,
  getAdminProductPage,
  importProducts,
  updateProductRecommend,
  updateProductStatus
} from '@/api/admin/product'
import { useAdminStore } from '@/store/admin'
import { formatDateTime, formatPrice } from '@/utils/format'

const router = useRouter()
const adminStore = useAdminStore()

/** 按钮级权限（开关类控件不便使用 v-perm，改为手动判断；无权限时降级为标签展示） */
const canStatus = adminStore.hasPermission('product:status')
const canEdit = adminStore.hasPermission('product:edit')

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  categoryId: null,
  status: null,
  isRecommend: null,
  page: 1,
  size: 10
})

/* ============================ 分类级联 ============================ */
const categoryCascaderRef = ref(null)
const categoryOptions = ref([])
const categoryPath = ref([])
const cascaderProps = { value: 'id', label: 'name', children: 'children', checkStrictly: true, emitPath: true }

const loadCategoryOptions = async () => {
  try {
    const res = await getAdminCategoryTree()
    categoryOptions.value = res.data || []
  } catch (e) {
    categoryOptions.value = []
  }
}

/** 级联选择：取选中节点自身的分类ID */
const handleCategoryChange = (value) => {
  if (!value || !value.length) {
    query.categoryId = null
    return
  }
  const node = categoryCascaderRef.value && categoryCascaderRef.value.getCheckedNodes(true)
  query.categoryId = node && node.length ? node[0].value : value[value.length - 1]
}

/* ============================ 列表加载 ============================ */
const loadData = async () => {
  loading.value = true
  try {
    const res = await getAdminProductPage({ ...query })
    const data = res.data || {}
    list.value = (data.records || []).map((item) => ({
      ...item,
      statusLoading: false,
      recommendLoading: false
    }))
    total.value = data.total || 0
  } catch (e) {
    list.value = []
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
  query.categoryId = null
  query.status = null
  query.isRecommend = null
  query.page = 1
  categoryPath.value = []
  loadData()
}

/* ============================ 行内操作 ============================ */
/** 库存显示样式：0 标红、低于等于预警阈值标橙 */
const stockClass = (row) => {
  const stock = Number(row.stock || 0)
  const warn = Number(row.warnStock || 0)
  if (stock <= 0) return 'stock-danger'
  if (warn > 0 && stock <= warn) return 'stock-warn'
  return ''
}

const rowClassName = ({ row }) => {
  const stock = Number(row.stock || 0)
  const warn = Number(row.warnStock || 0)
  return warn > 0 && stock > 0 && stock <= warn ? 'row-warn' : ''
}

/** 上下架切换 */
const handleStatusChange = async (row) => {
  row.statusLoading = true
  const target = row.status
  try {
    await updateProductStatus(row.id, target)
    ElMessage.success(target === 1 ? '商品已上架' : '商品已下架')
  } catch (e) {
    row.status = target === 1 ? 0 : 1
  } finally {
    row.statusLoading = false
  }
}

/** 推荐切换 */
const handleRecommendChange = async (row) => {
  row.recommendLoading = true
  const target = row.isRecommend
  try {
    await updateProductRecommend(row.id, target)
    ElMessage.success(target === 1 ? '已设为推荐商品' : '已取消推荐')
  } catch (e) {
    row.isRecommend = target === 1 ? 0 : 1
  } finally {
    row.recommendLoading = false
  }
}

/** 删除商品 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除商品「${row.name}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

/* ============================ 跳转 ============================ */
const goCreate = () => {
  router.push('/admin/product/create')
}

const goEdit = (id) => {
  router.push(`/admin/product/edit/${id}`)
}

/* ============================ 导出 / 导入 ============================ */
const handleExport = async () => {
  try {
    await exportProducts({ ...query, page: undefined, size: undefined })
    ElMessage.success('导出任务已完成，请查看下载文件')
  } catch (e) {
    /* 拦截器已提示 */
  }
}

const handleDownloadTemplate = async () => {
  try {
    await downloadProductTemplate()
    ElMessage.success('模板下载已开始')
  } catch (e) {
    /* 拦截器已提示 */
  }
}

const importVisible = ref(false)
const importing = ref(false)
const importFile = ref(null)
const uploadRef = ref(null)
const resultVisible = ref(false)
const importResult = ref({ successCount: 0, failCount: 0, errors: [] })

const handleFileChange = (file) => {
  const raw = file.raw || file
  const isXlsx = /\.xlsx$/i.test(raw.name || '')
  if (!isXlsx) {
    ElMessage.error('请选择 .xlsx 格式的文件')
    importFile.value = null
    if (uploadRef.value) uploadRef.value.clearFiles()
    return
  }
  importFile.value = raw
}

const handleExceed = (files) => {
  if (uploadRef.value) uploadRef.value.clearFiles()
  handleFileChange({ raw: files[0] })
}

const resetImport = () => {
  importFile.value = null
  importing.value = false
  if (uploadRef.value) uploadRef.value.clearFiles()
}

const handleImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择要导入的 Excel 文件')
    return
  }
  importing.value = true
  try {
    const res = await importProducts(importFile.value)
    const data = res.data || {}
    importResult.value = {
      successCount: data.successCount || 0,
      failCount: data.failCount || 0,
      errors: data.errors || []
    }
    importVisible.value = false
    resultVisible.value = true
    ElMessage.success('导入处理完成')
    query.page = 1
    loadData()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadCategoryOptions()
  loadData()
})
</script>

<style scoped>
.thumb {
  width: 54px;
  height: 54px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  display: block;
}

.thumb-error {
  width: 54px;
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #c0c4cc;
  background: #f5f7fa;
}

.sub-text {
  font-size: 12px;
  color: #909399;
}

.stock-danger {
  color: #f56c6c;
  font-weight: 600;
}

.stock-warn {
  color: #e6a23c;
  font-weight: 600;
}

:deep(.row-warn) {
  --el-table-tr-bg-color: #fffaf0;
}

.upload-icon {
  font-size: 42px;
  color: #c0c4cc;
  margin-bottom: 8px;
}

.error-list li {
  padding: 4px 0;
  font-size: 13px;
  color: #f56c6c;
  border-bottom: 1px dashed #f2f6fc;
}
</style>
