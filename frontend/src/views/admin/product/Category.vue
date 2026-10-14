<template>
  <div>
    <div class="search-bar">
      <el-form inline>
        <el-form-item label="分类名称">
          <el-input v-model="keyword" placeholder="输入分类名称过滤" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Plus" v-perm="'category:add'" @click="openDialog(null, 0)">新增一级分类</el-button>
          <el-button icon="Refresh" @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="分类名称" min-width="220" />
        <el-table-column label="图标" width="90" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.icon" :size="18"><component :is="row.icon" /></el-icon>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="层级" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.level === 1 ? 'primary' : 'success'" effect="plain">
              {{ row.level === 1 ? '一级' : '二级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="商品数" width="90" align="center">
          <template #default="{ row }">{{ row.productCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center">
          <template #default="{ row }">
            <el-button v-if="row.level === 1" link type="primary" v-perm="'category:add'" @click="openDialog(null, row.id)">
              新增子分类
            </el-button>
            <el-button link type="primary" v-perm="'category:edit'" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" v-perm="'category:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" placeholder="选择上级分类" style="width: 100%">
            <el-option label="顶级分类" :value="0" />
            <el-option v-for="item in topCategories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：手机通讯" maxlength="20" />
        </el-form-item>
        <el-form-item label="图标名称">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 Monitor、Goods" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
          <span class="text-muted" style="margin-left: 8px; font-size: 12px">数字越小越靠前</span>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addCategory,
  deleteCategory,
  getAdminCategoryTree,
  updateCategory,
  updateCategoryStatus
} from '@/api/admin/product'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const keyword = ref('')
const dialogVisible = ref(false)
const formRef = ref()

const form = reactive({ id: null, parentId: 0, name: '', icon: '', sort: 0, status: 1 })

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const topCategories = computed(() => list.value.filter((item) => item.level === 1))

/** 关键词过滤（保留匹配节点的父级，便于树形展示） */
const tableData = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return list.value
  const result = []
  list.value.forEach((top) => {
    const children = (top.children || []).filter((child) => child.name.includes(kw))
    if (top.name.includes(kw) || children.length) {
      result.push({ ...top, children })
    }
  })
  return result
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAdminCategoryTree()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openDialog = (row, parentId) => {
  if (row) {
    Object.assign(form, {
      id: row.id,
      parentId: row.parentId,
      name: row.name,
      icon: row.icon,
      sort: row.sort,
      status: row.status
    })
  } else {
    Object.assign(form, {
      id: null,
      parentId: parentId || 0,
      name: '',
      icon: '',
      sort: 0,
      status: 1
    })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      parentId: form.parentId,
      name: form.name,
      icon: form.icon,
      sort: form.sort,
      status: form.status
    }
    if (form.id) {
      await updateCategory(form.id, payload)
    } else {
      await addCategory(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleStatusChange = async (row, val) => {
  await updateCategoryStatus(row.id, val)
  row.status = val
  ElMessage.success(val === 1 ? '分类已启用' : '分类已停用')
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？存在子分类或商品时无法删除。`, '删除分类', {
    type: 'warning'
  })
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
