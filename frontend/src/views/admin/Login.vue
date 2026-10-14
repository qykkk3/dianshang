<template>
  <div class="admin-login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-inner">
        <div class="brand-logo">
          <span class="logo-mark">星</span>
          <span class="logo-text">星辰商城</span>
        </div>
        <h1 class="brand-title">星辰商城管理后台</h1>
        <p class="brand-desc">商品、订单、营销、会员与系统权限一体化管理，让经营数据一目了然。</p>
        <ul class="brand-feature">
          <li><el-icon><DataLine /></el-icon>实时数据看板，销售趋势随时掌握</li>
          <li><el-icon><Goods /></el-icon>多规格 SKU 商品管理，库存预警更省心</li>
          <li><el-icon><UserFilled /></el-icon>角色权限精细控制，操作安全可追溯</li>
        </ul>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="form-panel">
      <div class="login-card">
        <h2 class="login-title">账号登录</h2>
        <p class="login-subtitle">请使用管理员账号登录后台</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable>
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password clearable>
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
          </el-form-item>
        </el-form>

        <!-- 演示账号 -->
        <el-divider content-position="center">演示账号（点击一键填入）</el-divider>
        <div class="demo-list">
          <div v-for="item in demoAccounts" :key="item.username" class="demo-item" @click="fillAccount(item)">
            <div class="demo-role">{{ item.role }}</div>
            <div class="demo-account">{{ item.username }} / {{ item.password }}</div>
            <div class="demo-desc">{{ item.desc }}</div>
          </div>
        </div>

        <div class="login-footer">
          <el-link type="primary" :underline="false" @click="goShop">
            <el-icon><Back /></el-icon>
            返回商城首页
          </el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 管理后台登录页（独立页面，不套 AdminLayout）
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '@/store/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/** 演示账号 */
const demoAccounts = [
  { role: '超级管理员', username: 'admin', password: '123456', desc: '拥有全部菜单与操作权限' },
  { role: '运营专员', username: 'operator', password: '123456', desc: '负责商品、营销，无系统管理权限' },
  { role: '仓储专员', username: 'warehouse', password: '123456', desc: '负责库存与发货' }
]

/** 一键填入演示账号 */
const fillAccount = (item) => {
  form.username = item.username
  form.password = item.password
  if (formRef.value) {
    formRef.value.clearValidate()
  }
  ElMessage.success(`已填入${item.role}账号`)
}

/** 登录 */
const handleLogin = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await adminStore.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功，欢迎回来')
    const redirect = route.query.redirect ? decodeURIComponent(String(route.query.redirect)) : '/admin/dashboard'
    router.replace(redirect)
  } catch (e) {
    /* 失败提示由 request 拦截器统一处理 */
  } finally {
    loading.value = false
  }
}

/** 返回商城首页 */
const goShop = () => {
  router.push('/')
}
</script>

<style scoped>
.admin-login-page {
  display: flex;
  min-height: 100vh;
  background: #f0f2f5;
}

/* 左侧品牌区 */
.brand-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #fff;
  background: linear-gradient(135deg, #1f2d3d 0%, #2b4a6f 55%, #ff5000 140%);
}

.brand-inner {
  max-width: 420px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
}

.logo-mark {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ff7a45, #ff5000);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
}

.brand-title {
  margin: 0 0 14px;
  font-size: 30px;
  line-height: 1.3;
}

.brand-desc {
  margin: 0 0 26px;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.78);
}

.brand-feature li {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

/* 右侧表单区 */
.form-panel {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: #fff;
}

.login-card {
  width: 100%;
  max-width: 360px;
}

.login-title {
  margin: 0 0 6px;
  font-size: 22px;
  color: #303133;
}

.login-subtitle {
  margin: 0 0 24px;
  font-size: 13px;
  color: #909399;
}

.login-btn {
  width: 100%;
  letter-spacing: 4px;
}

/* 演示账号 */
.demo-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.demo-item {
  padding: 8px 12px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.demo-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.demo-role {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.demo-account {
  font-size: 13px;
  color: #ff5000;
}

.demo-desc {
  font-size: 12px;
  color: #909399;
}

.login-footer {
  margin-top: 20px;
  text-align: center;
}

@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }

  .form-panel {
    width: 100%;
  }
}
</style>
