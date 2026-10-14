<template>
  <div>
    <div class="search-bar">
      <el-form inline>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 150px">
            <el-option label="平台公告" :value="1" />
            <el-option label="促销活动" :value="2" />
            <el-option label="帮助中心" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="已发布" :value="1" />
            <el-option label="草稿" :value="0" />
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
        <div class="admin-page-title">公告管理</div>
        <el-button type="primary" icon="Plus" v-perm="'notice:add'" @click="openDialog()">新增公告</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.type)" effect="plain">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-perm="'notice:edit'" @click="openDialog(row)">编辑</el-button>
            <el-button link type="success" v-perm="'notice:publish'" @click="handleTogglePublish(row)">
              {{ row.status === 1 ? '下线' : '发布' }}
            </el-button>
            <el-button link type="danger" v-perm="'notice:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '新增公告'" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">平台公告</el-radio>
            <el-radio :value="2">促销活动</el-radio>
            <el-radio :value="3">帮助中心</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="支持 HTML 片段，例如：<p>活动时间：10 月 20 日 - 11 月 11 日</p>"
          />
          <div class="preview-toggle">
            <el-checkbox v-model="showPreview">实时预览</el-checkbox>
          </div>
          <div v-if="showPreview" class="notice-preview" v-html="form.content"></div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">已发布</el-radio>
            <el-radio :value="0">存为草稿</el-radio>
          </el-radio-group>
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
import { addNotice, deleteNotice, getNoticePage, publishNotice, updateNotice } from '@/api/admin/marketing'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ type: null, status: null, page: 1, size: 10 })

const dialogVisible = ref(false)
const formRef = ref()
const showPreview = ref(false)
const form = reactive({ id: null, title: '', content: '', type: 1, status: 1 })

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }]
}

const typeLabel = (type) => ({ 1: '平台公告', 2: '促销活动', 3: '帮助中心' }[type] || '其它')
const typeTag = (type) => ({ 1: 'primary', 2: 'danger', 3: 'info' }[type] || 'info')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getNoticePage({ ...query })
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
  Object.assign(query, { type: null, status: null, page: 1 })
  loadData()
}

const openDialog = (row) => {
  showPreview.value = false
  if (row) {
    Object.assign(form, { id: row.id, title: row.title, content: row.content, type: row.type, status: row.status })
  } else {
    Object.assign(form, { id: null, title: '', content: '', type: 1, status: 1 })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { title: form.title, content: form.content, type: form.type, status: form.status }
    if (form.id) {
      await updateNotice(form.id, payload)
    } else {
      await addNotice(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleTogglePublish = async (row) => {
  const next = row.status === 1 ? 0 : 1
  await publishNotice(row.id, next)
  ElMessage.success(next === 1 ? '公告已发布' : '公告已下线')
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除公告', { type: 'warning' })
  await deleteNotice(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.preview-toggle {
  margin-top: 6px;
}

.notice-preview {
  width: 100%;
  margin-top: 8px;
  padding: 12px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  background: #fafafa;
  font-size: 13px;
  line-height: 1.8;
  max-height: 200px;
  overflow-y: auto;
}
</style>
