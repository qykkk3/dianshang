<template>
  <div>
    <div class="page-card">
      <div class="admin-page-header">
        <div class="admin-page-title">角色权限</div>
        <el-button type="primary" icon="Plus" v-perm="'sys:role:add'" @click="openDialog()">新增角色</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="roleCode" label="角色编码" width="150">
          <template #default="{ row }">
            <el-tag size="small" :type="roleTag(row.roleCode)" effect="plain">{{ row.roleCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
        <el-table-column label="管理员数" width="100" align="center">
          <template #default="{ row }">{{ row.userCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="权限数" width="100" align="center">
          <template #default="{ row }">{{ (row.permissionIds || []).length }}</template>
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
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-perm="'sys:role:edit'" @click="openDialog(row)">编辑</el-button>
            <el-button link type="warning" v-perm="'sys:role:assign'" @click="openAssign(row)">分配权限</el-button>
            <el-button link type="danger" v-perm="'sys:role:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑角色 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model="form.roleCode"
            :disabled="isBuiltin"
            placeholder="大写字母/下划线，如 CUSTOMER_SERVICE"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如：运营/客服" maxlength="20" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
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

    <!-- 分配权限 -->
    <el-drawer v-model="assignVisible" :title="`分配权限 - ${currentRole ? currentRole.roleName : ''}`" size="520px">
      <el-alert
        title="保存后该角色下的管理员将被强制下线，重新登录后权限生效"
        type="warning"
        :closable="false"
        class="mb-16"
      />
      <div class="flex-between mb-12">
        <span class="text-muted">勾选该角色可访问的菜单与可操作按钮</span>
        <div>
          <el-button size="small" @click="checkAll(true)">全选</el-button>
          <el-button size="small" @click="checkAll(false)">全不选</el-button>
        </div>
      </div>
      <el-tree
        ref="permTreeRef"
        :data="permissionTree"
        show-checkbox
        node-key="id"
        default-expand-all
        :props="{ label: 'name', children: 'children' }"
        class="perm-tree"
      >
        <template #default="{ data }">
          <span>
            {{ data.name }}
            <el-tag v-if="data.type === 3" size="small" type="info" effect="plain" style="margin-left: 6px">按钮</el-tag>
            <span v-if="data.perms" class="text-muted" style="margin-left: 6px; font-size: 12px">{{ data.perms }}</span>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleAssign">保存权限</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addRole,
  assignRolePermissions,
  deleteRole,
  getPermissionTree,
  getRoleList,
  updateRole,
  updateRoleStatus
} from '@/api/admin/system'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const permissionTree = ref([])

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, roleCode: '', roleName: '', description: '', status: 1 })

const assignVisible = ref(false)
const permTreeRef = ref()
const currentRole = ref(null)

const isBuiltin = computed(() => ['ADMIN', 'OPERATOR', 'WAREHOUSE'].includes(form.roleCode))

const roleTag = (code) => ({ ADMIN: 'danger', OPERATOR: 'warning', WAREHOUSE: 'success' }[code] || 'info')

const rules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z_]*$/, message: '仅支持大写字母与下划线，且以字母开头', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRoleList()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadPermissions = async () => {
  if (permissionTree.value.length) return
  const res = await getPermissionTree()
  permissionTree.value = res.data || []
}

const openDialog = (row) => {
  if (row) {
    Object.assign(form, {
      id: row.id,
      roleCode: row.roleCode,
      roleName: row.roleName,
      description: row.description,
      status: row.status
    })
  } else {
    Object.assign(form, { id: null, roleCode: '', roleName: '', description: '', status: 1 })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      roleCode: form.roleCode,
      roleName: form.roleName,
      description: form.description,
      status: form.status
    }
    if (form.id) {
      await updateRole(form.id, payload)
    } else {
      await addRole(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleStatusChange = async (row, val) => {
  await updateRoleStatus(row.id, val)
  row.status = val
  ElMessage.success(val === 1 ? '角色已启用' : '角色已停用')
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(
    `确定删除角色「${row.roleName}」吗？内置角色或仍有管理员使用的角色无法删除。`,
    '删除角色',
    { type: 'warning' }
  )
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const openAssign = async (row) => {
  await loadPermissions()
  currentRole.value = row
  assignVisible.value = true
  // 仅勾选叶子节点，父节点交由 el-tree 自动计算半选状态
  const leafIds = collectLeafIds(permissionTree.value)
  const checked = (row.permissionIds || []).filter((id) => leafIds.includes(id))
  permTreeRef.value.setCheckedKeys(checked)
}

/** 收集所有叶子节点 id */
const collectLeafIds = (nodes) => {
  const ids = []
  const walk = (list) => {
    list.forEach((node) => {
      if (node.children && node.children.length) {
        walk(node.children)
      } else {
        ids.push(node.id)
      }
    })
  }
  walk(nodes)
  return ids
}

const checkAll = (checked) => {
  if (checked) {
    permTreeRef.value.setCheckedKeys(collectLeafIds(permissionTree.value))
  } else {
    permTreeRef.value.setCheckedKeys([])
  }
}

const handleAssign = async () => {
  const checked = permTreeRef.value.getCheckedKeys()
  const halfChecked = permTreeRef.value.getHalfCheckedKeys()
  saving.value = true
  try {
    await assignRolePermissions(currentRole.value.id, [...checked, ...halfChecked])
    ElMessage.success('权限分配成功，该角色下的管理员需重新登录')
    assignVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.perm-tree {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px;
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}
</style>
