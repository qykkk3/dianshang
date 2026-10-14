<template>
  <div class="page-card">
    <div class="admin-page-title mb-16">个人资料</div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 520px">
      <el-form-item label="头像">
        <el-avatar :size="56" :src="form.avatar">{{ (form.nickname || 'U').substring(0, 1) }}</el-avatar>
        <span class="text-muted ml-12">演示环境暂不支持修改头像</span>
      </el-form-item>
      <el-form-item label="用户名">
        <el-input :model-value="form.username" disabled />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.gender">
          <el-radio :value="0">保密</el-radio>
          <el-radio :value="1">男</el-radio>
          <el-radio :value="2">女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="生日">
        <el-date-picker
          v-model="form.birthday"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择生日"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
        <el-button @click="loadProfile">重置</el-button>
      </el-form-item>
    </el-form>

    <el-divider />

    <div class="admin-page-title mb-16">修改密码</div>
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px" style="max-width: 520px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-20 位" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="changing" @click="handleChangePassword">确认修改</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updatePassword, updateProfile } from '@/api/user'
import { useBuyerStore } from '@/store/buyer'

const buyerStore = useBuyerStore()

const formRef = ref()
const pwdFormRef = ref()
const saving = ref(false)
const changing = ref(false)

const form = reactive({
  id: null,
  username: '',
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
  gender: 0,
  birthday: null
})

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度需为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }]
}

const loadProfile = async () => {
  const res = await getProfile()
  const data = res.data || {}
  Object.assign(form, {
    id: data.id,
    username: data.username,
    nickname: data.nickname,
    phone: data.phone,
    email: data.email,
    avatar: data.avatar,
    gender: data.gender ?? 0,
    birthday: data.birthday || null
  })
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname,
      phone: form.phone,
      email: form.email,
      gender: form.gender,
      birthday: form.birthday,
      avatar: form.avatar
    })
    ElMessage.success('资料已更新')
    await buyerStore.fetchInfo()
    loadProfile()
  } finally {
    saving.value = false
  }
}

const handleChangePassword = async () => {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  changing.value = true
  try {
    await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请牢记新密码')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } finally {
    changing.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.ml-12 {
  margin-left: 12px;
  font-size: 12px;
}
</style>
