<template>
  <div>
    <div class="search-bar">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="账号 / 姓名 / 手机号" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="admin-page-header">
        <div class="admin-page-title">管理员管理</div>
        <el-button type="primary" icon="Plus" v-perm="'sys:user:add'" @click="openDialog()">新增管理员</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="34" :src="row.avatar">{{ (row.realName || row.username).substring(0, 1) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="realName" label="姓名" width="130" />
        <el-table-column label="角色" width="140" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="roleTag(row.roleCode)" effect="plain">
              {{ row.roleName || row.roleCode }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column prop="lastLoginIp" label="登录IP" width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-perm="'sys:user:edit'" @click="openDialog(row)">编辑</el-button>
            <el-button link type="warning" v-perm="'sys:user:reset'" @click="openResetDialog(row)">重置密码</el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
              <el-button link type="primary">更多<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="status" v-if="adminStore.hasPermission('sys:user:edit')">
                    {{ row.status === 1 ? '禁用账号' : '启用账号' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided v-if="adminStore.hasPermission('sys:user:delete')">
                    删除管理员
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑管理员' : '新增管理员'" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="4-20 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-20 位" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id">
              <span>{{ role.roleName }}</span>
              <span class="text-muted" style="float: right; font-size: 12px">{{ role.roleCode }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="选填" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="选填" />
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

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="440px" append-to-body>
      <el-alert
        title="重置后该管理员将被强制下线，需要使用新密码重新登录"
        type="warning"
        :closable="false"
        class="mb-16"
      />
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="90px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetForm.password" type="password" show-password placeholder="6-20 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import {
  addSysUser,
  deleteSysUser,
  getRoleList,
  getSysUserPage,
  resetSysUserPassword,
  updateSysUser,
  updateSysUserStatus
} from '@/api/admin/system'
import { formatDateTime } from '@/utils/format'
import { useAdminStore } from '@/store/admin'

const adminStore = useAdminStore()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const roles = ref([])
const total = ref(0)
const query = reactive({ keyword: '', roleId: null, status: null, page: 1, size: 10 })

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, username: '', password: '', realName: '', roleId: null, phone: '', email: '', status: 1 })

const resetVisible = ref(false)
const resetFormRef = ref()
const resetForm = reactive({ id: null, password: '' })

const roleTag = (code) => ({ ADMIN: 'danger', OPERATOR: 'warning', WAREHOUSE: 'success' }[code] || 'info')

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '长度需为 4-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度需为 6-20 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const resetRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度需为 6-20 位', trigger: 'blur' }
  ]
}

const loadRoles = async () => {
  const res = await getRoleList()
  roles.value = res.data || []
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSysUserPage({ ...query })
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

const handleResetQuery = () => {
  Object.assign(query, { keyword: '', roleId: null, status: null, page: 1 })
  loadData()
}

const openDialog = (row) => {
  if (row) {
    Object.assign(form, {
      id: row.id,
      username: row.username,
      password: '',
      realName: row.realName,
      roleId: row.roleId,
      phone: row.phone,
      email: row.email,
      status: row.status
    })
  } else {
    Object.assign(form, {
      id: null,
      username: '',
      password: '',
      realName: '',
      roleId: null,
      phone: '',
      email: '',
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
      username: form.username,
      password: form.password,
      realName: form.realName,
      roleId: form.roleId,
      phone: form.phone,
      email: form.email,
      status: form.status
    }
    if (form.id) {
      await updateSysUser(form.id, payload)
    } else {
      await addSysUser(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const openResetDialog = (row) => {
  resetForm.id = row.id
  resetForm.password = ''
  resetVisible.value = true
}

const handleResetPassword = async () => {
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await resetSysUserPassword(resetForm.id, resetForm.password)
    ElMessage.success('密码已重置，该管理员需重新登录')
    resetVisible.value = false
  } finally {
    saving.value = false
  }
}

const handleCommand = async (command, row) => {
  if (command === 'status') {
    const next = row.status === 1 ? 0 : 1
    await ElMessageBox.confirm(
      next === 0 ? `禁用后「${row.realName || row.username}」将被强制下线，确定吗？` : '确定启用该管理员吗？',
      '提示',
      { type: 'warning' }
    )
    await updateSysUserStatus(row.id, next)
    ElMessage.success(next === 1 ? '已启用' : '已禁用')
    loadData()
  } else if (command === 'delete') {
    await ElMessageBox.confirm('删除后不可恢复（超级管理员与自己不可删除），确定删除吗？', '删除管理员', {
      type: 'warning'
    })
    await deleteSysUser(row.id)
    ElMessage.success('删除成功')
    loadData()
  }
}

onMounted(() => {
  loadRoles()
  loadData()
})
</script>
