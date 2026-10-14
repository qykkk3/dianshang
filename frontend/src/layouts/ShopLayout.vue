<template>
  <div class="shop-layout">
    <!-- 顶部通栏 -->
    <div class="top-bar">
      <div class="page-container flex-between">
        <span class="tip">欢迎来到星辰商城！满 99 元包邮 · 七天无理由退换货</span>
        <div class="flex gap-16 top-links">
          <template v-if="buyerStore.isLogin">
            <router-link to="/user/profile">{{ buyerStore.nickname }}</router-link>
            <router-link to="/orders">我的订单</router-link>
            <router-link to="/user/favorite">我的收藏</router-link>
            <a @click="handleLogout">退出登录</a>
          </template>
          <template v-else>
            <router-link to="/login">登录</router-link>
            <router-link to="/register">免费注册</router-link>
          </template>
          <router-link to="/admin/login" class="admin-entry">管理后台</router-link>
        </div>
      </div>
    </div>

    <!-- 头部：Logo + 搜索 + 购物车 -->
    <div class="header">
      <div class="page-container header-inner">
        <router-link to="/" class="logo">
          <span class="logo-mark">星</span>
          <span class="logo-text">星辰商城</span>
        </router-link>

        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索商品名称、品牌，如：智能手机"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button icon="Search" @click="handleSearch">搜索</el-button>
          </template>
        </el-input>

        <router-link to="/cart" class="cart-entry">
          <el-badge :value="cartStore.count" :hidden="!cartStore.count" :max="99">
            <el-button icon="ShoppingCart" round>购物车</el-button>
          </el-badge>
        </router-link>
      </div>
    </div>

    <!-- 分类导航 -->
    <div class="nav-bar">
      <div class="page-container nav-inner">
        <router-link to="/" class="nav-item" :class="{ active: isActive('/') }">首页</router-link>
        <router-link to="/products" class="nav-item" :class="{ active: isActive('/products') }">全部商品</router-link>
        <router-link
          v-for="item in categories"
          :key="item.id"
          :to="`/category/${item.id}`"
          class="nav-item"
          :class="{ active: isActive(`/category/${item.id}`) }"
        >
          {{ item.name }}
        </router-link>
        <router-link to="/notices" class="nav-item" :class="{ active: isActive('/notices') }">公告中心</router-link>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="main-area">
      <router-view />
    </div>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="page-container footer-inner">
        <div class="footer-brand">
          <div class="footer-title">星辰商城</div>
          <p class="text-muted">基于 Spring Boot 3 + Vue 3 + Element Plus 的 B2C 电商管理系统演示项目</p>
        </div>
        <div class="footer-links">
          <div class="footer-col">
            <div class="footer-col-title">购物指南</div>
            <a>购物流程</a>
            <a>会员介绍</a>
            <a>常见问题</a>
          </div>
          <div class="footer-col">
            <div class="footer-col-title">配送方式</div>
            <a>满 99 元包邮</a>
            <a>配送范围</a>
            <a>物流跟踪</a>
          </div>
          <div class="footer-col">
            <div class="footer-col-title">售后服务</div>
            <a>七天无理由</a>
            <a>退款说明</a>
            <a>退换货流程</a>
          </div>
        </div>
      </div>
      <div class="copyright">© 2026 星辰商城 · 仅用于教学演示</div>
    </footer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategories } from '@/api/shop'
import { useBuyerStore } from '@/store/buyer'
import { useCartStore } from '@/store/cart'

const route = useRoute()
const router = useRouter()
const buyerStore = useBuyerStore()
const cartStore = useCartStore()

const keyword = ref('')
const categories = ref([])

const isActive = (path) => route.path === path

const handleSearch = () => {
  const value = keyword.value.trim()
  router.push({ path: '/search', query: value ? { keyword: value } : {} })
}

const handleLogout = async () => {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  await buyerStore.logout()
  cartStore.clear()
  ElMessage.success('已退出登录')
  router.push('/')
}

onMounted(async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (e) {
    categories.value = []
  }
  cartStore.refresh()
})
</script>

<style scoped>
.shop-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.top-bar {
  background: #303133;
  color: #c0c4cc;
  font-size: 12px;
  line-height: 34px;
}

.top-bar a {
  color: #c0c4cc;
  cursor: pointer;
}

.top-bar a:hover {
  color: #fff;
}

.admin-entry {
  color: #ffd04b !important;
}

.header {
  background: #fff;
  padding: 18px 0;
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 40px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 26px;
  font-weight: 700;
  color: #ff5000;
  white-space: nowrap;
}

.logo-mark {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ff7a45, #ff5000);
  color: #fff;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-input {
  max-width: 520px;
}

.cart-entry {
  margin-left: auto;
}

.nav-bar {
  background: #fff;
  border-top: 2px solid #ff5000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.nav-inner {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 46px;
  overflow-x: auto;
}

.nav-item {
  padding: 0 16px;
  line-height: 46px;
  font-size: 15px;
  color: #303133;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
}

.nav-item:hover,
.nav-item.active {
  color: #ff5000;
  font-weight: 600;
  border-bottom-color: #ff5000;
}

.main-area {
  flex: 1;
  padding-bottom: 30px;
}

.footer {
  background: #fff;
  border-top: 1px solid #ebeef5;
}

.footer-inner {
  display: flex;
  justify-content: space-between;
  padding: 30px 0 20px;
}

.footer-title {
  font-size: 20px;
  font-weight: 700;
  color: #ff5000;
  margin-bottom: 8px;
}

.footer-links {
  display: flex;
  gap: 60px;
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}

.footer-col-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.copyright {
  text-align: center;
  font-size: 12px;
  color: #a8abb2;
  padding: 12px 0;
  border-top: 1px solid #f2f6fc;
}
</style>
