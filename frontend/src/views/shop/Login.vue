<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-brand">
        <span class="logo-mark">星</span>
        <div>
          <div class="brand-title">星辰商城</div>
          <div class="brand-sub">会员登录</div>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable>
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>

      <div class="auth-links">
        <span>还没有账号？<router-link to="/register" class="link">免费注册</router-link></span>
        <router-link to="/" class="link">返回首页</router-link>
      </div>

      <el-divider>演示账号（点击填入）</el-divider>
      <div class="demo-accounts">
        <el-tag
          v-for="item in demoAccounts"
          :key="item.username"
          class="demo-tag"
          type="info"
          effect="plain"
          @click="fillDemo(item)"
        >
          {{ item.username }} / {{ item.password }}
        </el-tag>
      </div>

      <div class="admin-entry">
        <router-link to="/admin/login" class="link">管理员登录入口 →</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBuyerStore } from '@/store/buyer'
import { useCartStore } from '@/store/cart'

const route = useRoute()
const router = useRouter()
const buyerStore = useBuyerStore()
const cartStore = useCartStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = [
  { username: 'zhangsan', password: '123456' },
  { username: 'lisi', password: '123456' },
  { username: 'wangwu', password: '123456' }
]

const fillDemo = (item) => {
  form.username = item.username
  form.password = item.password
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await buyerStore.login({ username: form.username, password: form.password })
    await cartStore.refresh()
    ElMessage.success('登录成功，欢迎回来！')
    const redirect = route.query.redirect
    router.push(redirect ? decodeURIComponent(redirect) : '/')
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
  background: linear-gradient(135deg, #ff7a45 0%, #ff5000 45%, #d93600 100%);
}

.auth-card {
  width: 420px;
  background: #fff;
  border-radius: 14px;
  padding: 34px 36px 26px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.22);
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
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
  letter-spacing: 4px;
}

.auth-links {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.link {
  color: #ff5000;
}

.link:hover {
  text-decoration: underline;
}

.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-tag {
  cursor: pointer;
}

.admin-entry {
  text-align: center;
  margin-top: 18px;
  font-size: 13px;
}
</style>
