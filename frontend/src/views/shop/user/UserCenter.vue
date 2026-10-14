<template>
  <div class="page-container mt-16">
    <!-- 用户信息卡 -->
    <div class="page-card user-header">
      <div class="flex gap-16" style="align-items: center">
        <el-avatar :size="64" :src="buyerStore.avatar">
          {{ buyerStore.nickname.substring(0, 1) }}
        </el-avatar>
        <div>
          <div class="user-name">{{ buyerStore.nickname }}</div>
          <div class="text-muted">账号：{{ buyerStore.info?.username }} ｜ 会员中心</div>
        </div>
      </div>

      <div class="order-shortcuts">
        <div v-for="item in shortcuts" :key="item.label" class="shortcut" @click="goShortcut(item)">
          <div class="shortcut-count">{{ counts[item.countKey] || 0 }}</div>
          <div class="shortcut-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <div class="user-body">
      <!-- 左侧菜单 -->
      <div class="page-card user-aside">
        <el-menu :default-active="$route.path" router>
          <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 右侧内容 -->
      <div class="user-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderCounts } from '@/api/order'
import { useBuyerStore } from '@/store/buyer'

const router = useRouter()
const buyerStore = useBuyerStore()

const counts = ref({})

const menus = [
  { path: '/user/profile', label: '个人资料', icon: 'User' },
  { path: '/user/address', label: '收货地址', icon: 'Location' },
  { path: '/user/favorite', label: '我的收藏', icon: 'Star' },
  { path: '/user/history', label: '浏览记录', icon: 'Clock' },
  { path: '/user/coupon', label: '我的优惠券', icon: 'Discount' },
  { path: '/user/comment', label: '我的评价', icon: 'ChatDotSquare' },
  { path: '/user/refund', label: '退款/售后', icon: 'RefreshLeft' }
]

const shortcuts = [
  { label: '待付款', countKey: 'unpaid', status: 0 },
  { label: '待发货', countKey: 'paid', status: 1 },
  { label: '待收货', countKey: 'shipped', status: 2 },
  { label: '待评价', countKey: 'toComment', path: '/user/comment' }
]

const goShortcut = (item) => {
  if (item.path) {
    router.push(item.path)
  } else {
    router.push({ path: '/orders', query: { status: item.status } })
  }
}

onMounted(async () => {
  try {
    const res = await getOrderCounts()
    counts.value = res.data || {}
  } catch (e) {
    counts.value = {}
  }
})
</script>

<style scoped>
.user-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.order-shortcuts {
  display: flex;
  gap: 10px;
}

.shortcut {
  width: 86px;
  text-align: center;
  padding: 10px 0;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.shortcut:hover {
  background: #fff5f0;
}

.shortcut-count {
  font-size: 20px;
  font-weight: 700;
  color: #ff5000;
}

.shortcut-label {
  font-size: 13px;
  color: #606266;
  margin-top: 4px;
}

.user-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.user-aside {
  width: 200px;
  padding: 6px 0;
  flex-shrink: 0;
}

.user-aside :deep(.el-menu) {
  border-right: none;
}

.user-content {
  flex: 1;
  min-width: 0;
}
</style>
