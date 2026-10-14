<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-brand">
        <span class="logo-mark">星</span>
        <div>
          <div class="brand-title">星辰商城</div>
          <div class="brand-sub">新用户注册</div>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="4-20 位字母、数字或下划线" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-20 位" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="选填，默认与用户名相同" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填，11 位手机号" maxlength="11" clearable />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">立即注册</el-button>
        </el-form-item>
      </el-form>

      <div class="auth-links">
        <span>已有账号？<router-link to="/login" class="link">直接登录</router-link></span>
        <router-link to="/" class="link">返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: ''
})

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '长度需为 4-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度需为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || form.username,
      phone: form.phone,
      email: form.email
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  background: linear-gradient(135deg, #ff7a45 0%, #ff5000 45%, #d93600 100%);
}

.auth-card {
  width: 460px;
  background: #fff;
  border-radius: 14px;
  padding: 32px 36px 24px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.22);
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
}

.logo-mark {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff7a45, #ff5000);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.brand-sub {
  font-size: 13px;
  color: #909399;
}

.submit-btn {
  width: 100%;
  font-size: 16px;
  letter-spacing: 2px;
}

.auth-links {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #909399;
}

.link {
  color: #ff5000;
}

.link:hover {
  text-decoration: underline;
}

:deep(.el-form-item__label) {
  padding-bottom: 2px;
  font-size: 13px;
  color: #606266;
}
</style>
