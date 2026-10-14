<template>
  <div>
    <div class="search-bar">
      <el-form inline>
        <el-form-item label="位置">
          <el-select v-model="query.position" placeholder="全部" clearable style="width: 140px">
            <el-option label="商城首页" :value="1" />
            <el-option label="分类页" :value="2" />
          </el-select>
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
        <div class="admin-page-title">轮播图管理</div>
        <el-button type="primary" icon="Plus" v-perm="'banner:add'" @click="openDialog()">新增轮播图</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="预览图" width="200">
          <template #default="{ row }">
            <el-image :src="row.image" fit="cover" class="banner-image" :preview-src-list="[row.image]" preview-teleported>
              <template #error><div class="image-fallback">暂无图</div></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="linkUrl" label="跳转链接" min-width="180" show-overflow-tooltip />
        <el-table-column label="位置" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.position === 1 ? '商城首页' : '分类页' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
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
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-perm="'banner:edit'" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" v-perm="'banner:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑轮播图' : '新增轮播图'" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="如：星辰 X9 Pro 新品首发" maxlength="50" />
        </el-form-item>
        <el-form-item label="图片" prop="image">
          <ImageUpload v-model="form.image" type="banner" :limit="1" tip="建议尺寸 1200×420，支持 jpg/png/svg" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="如：/shop/product/1 或外部链接" />
        </el-form-item>
        <el-form-item label="展示位置">
          <el-radio-group v-model="form.position">
            <el-radio :value="1">商城首页</el-radio>
            <el-radio :value="2">分类页</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { addBanner, deleteBanner, getBannerPage, updateBanner, updateBannerStatus } from '@/api/admin/marketing'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ position: null, status: null, page: 1, size: 10 })

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, title: '', image: '', linkUrl: '', position: 1, sort: 0, status: 1 })

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  image: [{ required: true, message: '请上传图片', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getBannerPage({ ...query })
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
  Object.assign(query, { position: null, status: null, page: 1 })
  loadData()
}

const openDialog = (row) => {
  if (row) {
    Object.assign(form, {
      id: row.id,
      title: row.title,
      image: row.image,
      linkUrl: row.linkUrl,
      position: row.position,
      sort: row.sort,
      status: row.status
    })
  } else {
    Object.assign(form, { id: null, title: '', image: '', linkUrl: '', position: 1, sort: 0, status: 1 })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      title: form.title,
      image: form.image,
      linkUrl: form.linkUrl,
      position: form.position,
      sort: form.sort,
      status: form.status
    }
    if (form.id) {
      await updateBanner(form.id, payload)
    } else {
      await addBanner(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleStatusChange = async (row, val) => {
  await updateBannerStatus(row.id, val)
  row.status = val
  ElMessage.success(val === 1 ? '已启用' : '已停用')
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除轮播图「${row.title}」吗？`, '删除轮播图', { type: 'warning' })
  await deleteBanner(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.banner-image {
  width: 180px;
  height: 64px;
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
