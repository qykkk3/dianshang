<template>
  <div class="product-edit-page">
    <div class="admin-page-header">
      <span class="admin-page-title">{{ isEdit ? '编辑商品' : '新增商品' }}</span>
      <div class="table-actions">
        <el-button :icon="'Back'" @click="goBack">返回列表</el-button>
      </div>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" v-loading="pageLoading">
      <!-- 基本信息 -->
      <el-card class="mb-16" shadow="never">
        <template #header><span class="card-title">基本信息</span></template>
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="200" show-word-limit clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="副标题">
              <el-input v-model="form.subtitle" placeholder="卖点描述，如：轻薄便携旗舰" maxlength="255" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="品牌">
              <el-input v-model="form.brand" placeholder="请输入品牌" maxlength="50" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="单位">
              <el-input v-model="form.unit" placeholder="如：件 / 台 / 盒" maxlength="20" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="商品分类" prop="categoryId">
              <el-cascader
                ref="categoryCascaderRef"
                v-model="categoryPath"
                :options="categoryOptions"
                :props="cascaderProps"
                placeholder="请选择商品分类"
                clearable
                style="width: 100%"
                @change="handleCategoryChange"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :max="99999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="预警库存">
              <el-input-number
                v-model="form.warnStock"
                :min="0"
                :max="999999"
                controls-position="right"
                placeholder="留空则使用系统配置阈值"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="商品状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品标签">
              <div class="flex gap-16" style="align-items: center; flex-wrap: wrap">
                <el-switch v-model="form.isRecommend" :active-value="1" :inactive-value="0" active-text="推荐" />
                <el-switch v-model="form.isNew" :active-value="1" :inactive-value="0" active-text="新品" />
                <el-switch v-model="form.isHot" :active-value="1" :inactive-value="0" active-text="热销" />
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 商品图片 -->
      <el-card class="mb-16" shadow="never">
        <template #header><span class="card-title">商品图片</span></template>
        <el-form-item label="商品主图">
          <ImageUpload v-model="form.mainImage" type="product" tip="建议尺寸 800x800，支持 jpg/png，大小不超过 10MB" />
        </el-form-item>
        <el-form-item label="轮播图">
          <ImageUpload v-model="form.images" type="product" multiple :limit="5" tip="最多 5 张，用于商品详情页轮播展示" />
        </el-form-item>
      </el-card>

      <!-- 图文详情 -->
      <el-card class="mb-16" shadow="never">
        <template #header>
          <div class="flex-between">
            <span class="card-title">图文详情</span>
            <el-radio-group v-model="detailMode" size="small">
              <el-radio-button value="edit">编辑 HTML</el-radio-button>
              <el-radio-button value="preview">效果预览</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <el-form-item label="详情内容" label-width="110px">
          <el-input
            v-if="detailMode === 'edit'"
            v-model="form.detail"
            type="textarea"
            :rows="10"
            placeholder="可直接粘贴 HTML 片段，例如：&lt;p&gt;商品介绍&lt;/p&gt;&lt;img src=&quot;/api/files/xxx.png&quot; /&gt;"
          />
          <div v-else class="detail-preview">
            <div v-if="form.detail" v-html="form.detail"></div>
            <el-empty v-else description="暂无详情内容" />
          </div>
        </el-form-item>
      </el-card>

      <!-- 规格定义 -->
      <el-card class="mb-16" shadow="never">
        <template #header>
          <div class="flex-between">
            <span class="card-title">规格定义</span>
            <div class="table-actions">
              <el-button :icon="'Plus'" @click="addSpec">添加规格</el-button>
              <el-button type="primary" :icon="'MagicStick'" @click="generateSkuTable">生成 SKU 表</el-button>
            </div>
          </div>
        </template>

        <el-alert
          class="mb-16"
          type="info"
          :closable="false"
          show-icon
          title="规格值请用英文逗号或中文逗号分隔，例如「曜石黑,月光银」；点击「生成 SKU 表」将按规格值组合自动生成 SKU 行，已存在的同规格 SKU 会保留原价格与库存。"
        />

        <el-table :data="specRows" border size="small" empty-text="暂无规格，可直接点击「生成 SKU 表」创建单规格商品">
          <el-table-column label="规格名" width="220">
            <template #default="{ row }">
              <el-input v-model="row.specName" placeholder="如：颜色" />
            </template>
          </el-table-column>
          <el-table-column label="规格值（逗号分隔）" min-width="320">
            <template #default="{ row }">
              <el-input v-model="row.specValuesText" placeholder="如：曜石黑,月光银" />
            </template>
          </el-table-column>
          <el-table-column label="规格值预览" min-width="200">
            <template #default="{ row }">
              <el-tag v-for="value in parseSpecValues(row)" :key="value" class="spec-tag" type="info" size="small">
                {{ value }}
              </el-tag>
              <span v-if="!parseSpecValues(row).length" class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeSpec($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- SKU 表 -->
      <el-card class="mb-16" shadow="never">
        <template #header>
          <div class="flex-between">
            <span class="card-title">SKU 明细（共 {{ skuRows.length }} 条）</span>
            <div class="flex gap-8" style="align-items: center; flex-wrap: wrap">
              <span class="text-muted">批量填充：</span>
              <el-input-number v-model="batchFill.price" :min="0" :precision="2" :controls="false" placeholder="售价" style="width: 110px" />
              <el-input-number v-model="batchFill.originalPrice" :min="0" :precision="2" :controls="false" placeholder="原价" style="width: 110px" />
              <el-input-number v-model="batchFill.stock" :min="0" :controls="false" placeholder="库存" style="width: 110px" />
              <el-button :icon="'EditPen'" @click="applyBatchFill">应用到全部</el-button>
              <el-button :icon="'Plus'" @click="addSkuRow">新增 SKU</el-button>
            </div>
          </div>
        </template>

        <el-table :data="skuRows" border size="small" empty-text="暂无 SKU，请先定义规格后点击「生成 SKU 表」">
          <el-table-column label="SKU 编码" width="180">
            <template #default="{ row }">
              <el-input v-model="row.skuCode" placeholder="自动生成，可修改" />
            </template>
          </el-table-column>
          <el-table-column label="规格展示" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.specInfo" placeholder="如：曜石黑 / M" />
            </template>
          </el-table-column>
          <el-table-column label="售价(元)" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" placeholder="必填" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="原价(元)" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.originalPrice" :min="0" :precision="2" :controls="false" placeholder="选填" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="库存" width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.stock" :min="0" :controls="false" placeholder="必填" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="SKU 图片" width="120" align="center">
            <template #default="{ row }">
              <el-popover placement="left" :width="260" trigger="click">
                <template #reference>
                  <div class="sku-image-cell">
                    <el-image v-if="row.image" :src="row.image" fit="cover" class="sku-thumb">
                      <template #error><div class="sku-thumb-empty">失效</div></template>
                    </el-image>
                    <div v-else class="sku-thumb-empty">未设置</div>
                  </div>
                </template>
                <ImageUpload v-model="row.image" type="product" tip="支持单张 SKU 图片，不设置则使用商品主图" />
              </el-popover>
              <el-button v-if="row.image" link type="danger" size="small" @click="row.image = ''">清除</el-button>
              <el-button v-else link type="primary" size="small" @click="row.image = form.mainImage">用主图</el-button>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.status" :active-value="1" :inactive-value="0" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeSkuRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 提交 -->
      <div class="page-card flex-center submit-bar">
        <el-button size="large" @click="goBack">取消</el-button>
        <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '立即创建' }}
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
/**
 * 商品新增 / 编辑（含多规格 SKU 编辑器）
 *  - 新增：/admin/product/create
 *  - 编辑：/admin/product/edit/:id（进入时回填详情）
 *  - 提交流程：规格行 -> 笛卡尔积 -> SKU 行 -> specs/skus 组装后提交
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ImageUpload from '@/components/ImageUpload.vue'
import { addProduct, getAdminCategoryTree, getAdminProductDetail, updateProduct } from '@/api/admin/product'
import { splitImages } from '@/utils/format'

const route = useRoute()
const router = useRouter()

/** 是否为编辑模式 */
const isEdit = computed(() => !!route.params.id)

const pageLoading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const detailMode = ref('edit')

/** 表单数据：字段与后端 ProductSaveDTO 对齐 */
const form = reactive({
  id: null,
  name: '',
  subtitle: '',
  brand: '',
  unit: '件',
  categoryId: null,
  mainImage: '',
  images: [],
  detail: '',
  status: 1,
  isRecommend: 0,
  isNew: 0,
  isHot: 0,
  sort: 0,
  warnStock: null
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }]
}

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

const handleCategoryChange = (value) => {
  if (!value || !value.length) {
    form.categoryId = null
    return
  }
  const node = categoryCascaderRef.value && categoryCascaderRef.value.getCheckedNodes(true)
  form.categoryId = node && node.length ? node[0].value : value[value.length - 1]
}

/**
 * 编辑时把分类ID转换为级联路径（支持二级分类）
 */
const buildCategoryPath = (categoryId, options) => {
  for (const item of options) {
    if (String(item.id) === String(categoryId)) return [item.id]
    if (item.children && item.children.length) {
      const child = item.children.find((sub) => String(sub.id) === String(categoryId))
      if (child) return [item.id, child.id]
    }
  }
  return []
}

/* ============================ 规格与 SKU ============================ */
/** 规格行：{ id?, specName, specValuesText } */
const specRows = ref([])
/** SKU 行：{ id?, skuCode, specData, specInfo, price, originalPrice, stock, image, status } */
const skuRows = ref([])

const batchFill = reactive({ price: null, originalPrice: null, stock: null })

/** 解析规格值文本为数组（兼容中英文逗号、顿号与分号） */
const parseSpecValues = (row) => {
  const text = row.specValuesText || ''
  return text
    .split(/[,，、;；]/)
    .map((item) => String(item).trim())
    .filter(Boolean)
}

const addSpec = (specName = '', specValuesText = '') => {
  specRows.value.push({ id: null, specName, specValuesText })
}

const removeSpec = (index) => {
  specRows.value.splice(index, 1)
}

/** 规格键值对 -> 展示文本，如 {"颜色":"红","尺码":"M"} => 红 / M */
const buildSpecInfo = (specData) => Object.values(specData).join(' / ')

/** 展示文本 -> 规格键值对 */
const parseSpecInfoToData = (specInfo) => {
  const data = {}
  String(specInfo || '')
    .split('/')
    .map((item) => item.trim())
    .filter(Boolean)
    .forEach((value, index) => {
      data[`规格${index + 1}`] = value
    })
  return data
}

/** 解析已有的 specData JSON 字符串 */
const parseSpecData = (specData) => {
  if (!specData) return {}
  if (typeof specData === 'object') return specData
  try {
    const parsed = JSON.parse(specData)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (e) {
    return {}
  }
}

/** 规格值笛卡尔积 */
const cartesian = (lists) => lists.reduce((acc, list) => acc.flatMap((prev) => list.map((value) => [...prev, value])), [[]])

/**
 * 生成 SKU 表：按规格值笛卡尔积生成组合；
 * 已存在相同 specData 的 SKU 保留其 id/编码/价格/库存/图片/状态
 */
const generateSkuTable = () => {
  const validSpecs = specRows.value
    .map((row) => ({ specName: (row.specName || '').trim(), specValues: parseSpecValues(row) }))
    .filter((row) => row.specName && row.specValues.length)

  if (specRows.value.length && !validSpecs.length) {
    ElMessage.warning('请先填写完整的规格名与规格值（逗号分隔）')
    return
  }

  // 无有效规格：生成单条默认 SKU
  if (!validSpecs.length) {
    if (skuRows.value.length) {
      ElMessage.info('未定义规格，已保留现有 SKU；如需重置请先删除规格行')
      return
    }
    skuRows.value = [
      {
        id: null,
        skuCode: form.id ? `SPU${form.id}-1` : '',
        specData: '{}',
        specInfo: '默认规格',
        price: null,
        originalPrice: null,
        stock: null,
        image: form.mainImage || '',
        status: 1
      }
    ]
    ElMessage.success('已生成 1 条默认 SKU')
    return
  }

  // 现有 SKU 以 specData 为键建索引
  const existingMap = new Map()
  skuRows.value.forEach((row) => {
    const key = JSON.stringify(parseSpecData(row.specData))
    if (!existingMap.has(key)) existingMap.set(key, row)
  })

  const combinations = cartesian(validSpecs.map((spec) => spec.specValues))
  const rows = combinations.map((values, index) => {
    const specData = {}
    validSpecs.forEach((spec, specIndex) => {
      specData[spec.specName] = values[specIndex]
    })
    const key = JSON.stringify(specData)
    const exist = existingMap.get(key)
    if (exist) {
      return { ...exist, specData: key, specInfo: exist.specInfo || buildSpecInfo(specData) }
    }
    return {
      id: null,
      skuCode: form.id ? `SPU${form.id}-${index + 1}` : '',
      specData: key,
      specInfo: buildSpecInfo(specData),
      price: null,
      originalPrice: null,
      stock: null,
      image: '',
      status: 1
    }
  })

  skuRows.value = rows
  ElMessage.success(`已生成 ${rows.length} 条 SKU`)
}

const addSkuRow = () => {
  skuRows.value.push({
    id: null,
    skuCode: '',
    specData: '{}',
    specInfo: '',
    price: null,
    originalPrice: null,
    stock: null,
    image: '',
    status: 1
  })
}

const removeSkuRow = (index) => {
  skuRows.value.splice(index, 1)
}

/** 批量填充价格与库存 */
const applyBatchFill = () => {
  if (!skuRows.value.length) {
    ElMessage.warning('请先生成或新增 SKU 行')
    return
  }
  if (batchFill.price === null && batchFill.originalPrice === null && batchFill.stock === null) {
    ElMessage.warning('请至少填写一项批量填充内容')
    return
  }
  skuRows.value.forEach((row) => {
    if (batchFill.price !== null && batchFill.price !== undefined) row.price = batchFill.price
    if (batchFill.originalPrice !== null && batchFill.originalPrice !== undefined) row.originalPrice = batchFill.originalPrice
    if (batchFill.stock !== null && batchFill.stock !== undefined) row.stock = batchFill.stock
  })
  ElMessage.success(`已填充 ${skuRows.value.length} 条 SKU`)
}

/* ============================ 详情回填 ============================ */
const loadDetail = async (id) => {
  pageLoading.value = true
  try {
    const res = await getAdminProductDetail(id)
    const data = res.data || {}
    form.id = data.id
    form.name = data.name || ''
    form.subtitle = data.subtitle || ''
    form.brand = data.brand || ''
    form.unit = data.unit || '件'
    form.categoryId = data.categoryId != null ? data.categoryId : null
    form.mainImage = data.mainImage || ''
    form.images = splitImages(data.images)
    form.detail = data.detail || ''
    form.status = data.status == null ? 1 : data.status
    form.isRecommend = data.isRecommend == null ? 0 : data.isRecommend
    form.isNew = data.isNew == null ? 0 : data.isNew
    form.isHot = data.isHot == null ? 0 : data.isHot
    form.sort = data.sort == null ? 0 : data.sort
    form.warnStock = data.warnStock == null ? null : data.warnStock
    categoryPath.value = buildCategoryPath(form.categoryId, categoryOptions.value)

    // 规格回填
    specRows.value = (data.specs || []).map((spec) => ({
      id: spec.id || null,
      specName: spec.specName || '',
      specValuesText: (spec.specValues || []).join(',')
    }))

    // SKU 回填
    skuRows.value = (data.skus || []).map((sku) => {
      const specData = parseSpecData(sku.specData)
      const specInfo = sku.specInfo || (Object.keys(specData).length ? buildSpecInfo(specData) : '')
      return {
        id: sku.id || null,
        skuCode: sku.skuCode || '',
        specData: Object.keys(specData).length ? JSON.stringify(specData) : '{}',
        specInfo,
        price: sku.price == null ? null : Number(sku.price),
        originalPrice: sku.originalPrice == null ? null : Number(sku.originalPrice),
        stock: sku.stock == null ? null : Number(sku.stock),
        image: sku.image || '',
        status: sku.status == null ? 1 : sku.status
      }
    })
  } catch (e) {
    ElMessage.error('商品详情加载失败')
  } finally {
    pageLoading.value = false
  }
}

/* ============================ 提交 ============================ */
/** 组装提交参数 */
const buildPayload = () => ({
  id: form.id || undefined,
  categoryId: form.categoryId,
  name: form.name.trim(),
  subtitle: form.subtitle,
  brand: form.brand,
  unit: form.unit,
  mainImage: form.mainImage,
  images: form.images || [],
  detail: form.detail,
  status: form.status,
  isRecommend: form.isRecommend,
  isNew: form.isNew,
  isHot: form.isHot,
  sort: form.sort || 0,
  warnStock: form.warnStock,
  specs: specRows.value
    .filter((row) => (row.specName || '').trim() && parseSpecValues(row).length)
    .map((row) => ({
      id: row.id || undefined,
      specName: row.specName.trim(),
      specValues: parseSpecValues(row)
    })),
  skus: skuRows.value.map((row) => {
    const specData = parseSpecData(row.specData)
    const specDataObj = Object.keys(specData).length ? specData : parseSpecInfoToData(row.specInfo)
    const specInfo = (row.specInfo || '').trim() || buildSpecInfo(specDataObj)
    return {
      id: row.id || undefined,
      skuCode: row.skuCode,
      specData: JSON.stringify(specDataObj),
      specInfo,
      price: row.price,
      originalPrice: row.originalPrice == null ? row.price : row.originalPrice,
      stock: row.stock == null ? 0 : row.stock,
      image: row.image,
      status: row.status == null ? 1 : row.status
    }
  })
})

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (!skuRows.value.length) {
    ElMessage.warning('请至少添加一个 SKU（可定义规格后点击「生成 SKU 表」）')
    return
  }
  const invalidSku = skuRows.value.some((row) => row.price === null || row.price === undefined || row.price === '')
  if (invalidSku) {
    ElMessage.warning('请填写全部 SKU 的售价')
    return
  }
  const invalidStock = skuRows.value.some((row) => row.stock === null || row.stock === undefined || row.stock === '')
  if (invalidStock) {
    ElMessage.warning('请填写全部 SKU 的库存')
    return
  }

  const payload = buildPayload()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProduct(form.id, payload)
      ElMessage.success('商品保存成功')
    } else {
      await addProduct(payload)
      ElMessage.success('商品创建成功')
    }
    router.push('/admin/product/list')
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.push('/admin/product/list')
}

onMounted(async () => {
  await loadCategoryOptions()
  if (isEdit.value) {
    await loadDetail(route.params.id)
  } else {
    addSpec('颜色', '')
  }
})
</script>

<style scoped>
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.detail-preview {
  width: 100%;
  min-height: 200px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  overflow-x: auto;
}

.spec-tag {
  margin: 0 6px 4px 0;
}

.sku-image-cell {
  cursor: pointer;
}

.sku-thumb {
  width: 44px;
  height: 44px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.sku-thumb-empty {
  width: 44px;
  height: 44px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #c0c4cc;
  background: #f5f7fa;
  border-radius: 4px;
}

.submit-bar {
  gap: 16px;
  padding: 18px;
}
</style>
