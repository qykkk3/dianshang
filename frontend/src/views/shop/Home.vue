<template>
  <div class="home">
    <!-- 轮播图 + 公告 -->
    <div class="page-container top-section">
      <div class="banner-wrap">
        <el-carousel v-if="banners.length" height="360px" :interval="4500" arrow="hover">
          <el-carousel-item v-for="item in banners" :key="item.id" @click="goBanner(item)">
            <el-image class="banner-image" :src="item.image" fit="cover">
              <template #error>
                <div class="image-fallback">暂无图片</div>
              </template>
            </el-image>
            <div v-if="item.title" class="banner-title">{{ item.title }}</div>
          </el-carousel-item>
        </el-carousel>
        <div v-else class="banner-empty flex-center">暂无轮播图</div>
      </div>

      <div class="notice-panel">
        <div class="notice-head">
          <el-icon><Bell /></el-icon>
          <span>平台公告</span>
          <router-link to="/notices" class="notice-more">更多</router-link>
        </div>
        <ul v-if="notices.length" class="notice-list">
          <li v-for="item in notices" :key="item.id" class="notice-item" @click="goNotice(item.id)">
            <span class="notice-title text-ellipsis">{{ item.title }}</span>
            <span class="notice-time">{{ formatDate(item.publishTime || item.createTime) }}</span>
          </li>
        </ul>
        <div v-else class="notice-empty">暂无公告</div>
      </div>
    </div>

    <!-- 分类快捷入口 -->
    <div class="page-container">
      <div class="page-card section-card">
        <div class="section-head">
          <span class="section-title">全部分类</span>
        </div>
        <div v-if="categories.length" class="category-grid">
          <div v-for="item in categories" :key="item.id" class="category-item" @click="goCategory(item.id)">
            <div class="category-icon">
              <el-icon :size="24"><Menu /></el-icon>
            </div>
            <div class="category-name text-ellipsis">{{ item.name }}</div>
            <div class="category-count">{{ item.productCount || 0 }} 件</div>
          </div>
        </div>
        <el-empty v-else description="暂无分类" :image-size="80" />
      </div>
    </div>

    <!-- 领券专区 -->
    <div class="page-container mt-16">
      <div class="page-card section-card">
        <div class="section-head">
          <span class="section-title">领券专区</span>
          <router-link to="/user/coupon" class="section-more">我的优惠券</router-link>
        </div>
        <div v-if="coupons.length" class="coupon-grid">
          <div v-for="item in coupons" :key="item.id" class="coupon-card">
            <div class="coupon-left">
              <div class="coupon-value">{{ couponValue(item) }}</div>
              <div class="coupon-threshold">满 {{ formatAmount(item.minAmount) }} 元可用</div>
            </div>
            <div class="coupon-right">
              <div class="coupon-name text-ellipsis">{{ item.name }}</div>
              <div class="coupon-desc text-ellipsis">{{ item.description || item.scopeNames || '全平台通用' }}</div>
              <div class="coupon-date">
                {{ formatDate(item.startTime) }} 至 {{ formatDate(item.endTime) }}
              </div>
            </div>
            <el-button
              class="coupon-btn"
              :type="item.received ? 'info' : 'danger'"
              size="small"
              round
              :disabled="!!item.received"
              @click.stop="handleReceive(item)"
            >
              {{ item.received ? '已领取' : '立即领取' }}
            </el-button>
          </div>
        </div>
        <el-empty v-else description="暂无可领取的优惠券" :image-size="80" />
      </div>
    </div>

    <!-- 商品楼层 -->
    <div v-for="floor in floors" :key="floor.key" class="page-container mt-16">
      <div class="page-card section-card">
        <div class="section-head">
          <span class="section-title">{{ floor.title }}</span>
          <span class="section-sub">{{ floor.subtitle }}</span>
          <router-link to="/products" class="section-more">查看更多</router-link>
        </div>
        <div v-if="floor.list.length" class="product-grid">
          <ShopProductCard v-for="product in floor.list" :key="product.id" :product="product" />
        </div>
        <el-empty v-else description="暂无商品" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getBanners,
  getCategories,
  getAvailableCoupons,
  getHotProducts,
  getNewProducts,
  getNoticePage,
  getRecommendProducts
} from '@/api/shop'
import { receiveCoupon } from '@/api/user'
import { useBuyerStore } from '@/store/buyer'
import { formatAmount, formatDate } from '@/utils/format'
import ShopProductCard from '@/components/ShopProductCard.vue'

/**
 * 商城首页：轮播 / 公告 / 分类 / 领券 / 三个商品楼层
 */
const router = useRouter()
const buyerStore = useBuyerStore()

const banners = ref([])
const notices = ref([])
const categories = ref([])
const coupons = ref([])
const floors = ref([
  { key: 'recommend', title: '为你推荐', subtitle: '猜你喜欢的好物', list: [] },
  { key: 'hot', title: '热销榜单', subtitle: '按销量排序', list: [] },
  { key: 'new', title: '新品上市', subtitle: '新鲜上架', list: [] }
])

/** 优惠券面额展示：满减显示金额，折扣显示折扣 */
const couponValue = (item) => {
  if (item.type === 2) {
    return `${Number(item.discount || 0)} 折`
  }
  return `￥${formatAmount(item.amount)}`
}

const goBanner = (item) => {
  if (item.linkUrl) {
    router.push(item.linkUrl)
  }
}

const goNotice = (id) => {
  router.push(`/notice/${id}`)
}

const goCategory = (id) => {
  router.push(`/category/${id}`)
}

/** 领取优惠券 */
const handleReceive = async (item) => {
  if (!buyerStore.isLogin) {
    ElMessage.warning('请先登录后再领取优惠券')
    router.push(`/login?redirect=${encodeURIComponent('/')}`)
    return
  }
  const res = await receiveCoupon(item.id)
  // 后端返回领取后的状态；兼容返回布尔值或用户券对象
  const received = typeof res.data === 'boolean' ? res.data : true
  item.received = received
  if (received) {
    ElMessage.success('领取成功，可在「我的优惠券」查看')
  }
}

const loadBanners = async () => {
  try {
    const res = await getBanners(1)
    banners.value = res.data || []
  } catch (e) {
    banners.value = []
  }
}

const loadNotices = async () => {
  try {
    const res = await getNoticePage({ page: 1, size: 5 })
    notices.value = res.data.records || []
  } catch (e) {
    notices.value = []
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (e) {
    categories.value = []
  }
}

const loadCoupons = async () => {
  try {
    const res = await getAvailableCoupons()
    coupons.value = res.data || []
  } catch (e) {
    coupons.value = []
  }
}

const loadProducts = async () => {
  const handlers = [
    { key: 'recommend', fn: getRecommendProducts },
    { key: 'hot', fn: getHotProducts },
    { key: 'new', fn: getNewProducts }
  ]
  await Promise.all(
    handlers.map(async (handler) => {
      try {
        const res = await handler.fn(8)
        const floor = floors.value.find((item) => item.key === handler.key)
        if (floor) {
          floor.list = res.data || []
        }
      } catch (e) {
        /* 单个楼层失败不影响其它楼层 */
      }
    })
  )
}

onMounted(async () => {
  loadBanners()
  loadNotices()
  loadCategories()
  loadCoupons()
  await loadProducts()
  if (buyerStore.isLogin) {
    // 已登录时接口会返回「是否已领取」，需要重新拉一次券列表
    loadCoupons()
  }
})
</script>

<style scoped>
.home {
  padding-top: 16px;
}

.top-section {
  display: flex;
  gap: 16px;
}

.banner-wrap {
  flex: 1;
  min-width: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.banner-image {
  width: 100%;
  height: 100%;
}

.banner-title {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 24px 20px 14px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.55));
}

.banner-empty {
  height: 360px;
  color: #c0c4cc;
  font-size: 14px;
}

.image-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f5f7fa;
}

.notice-panel {
  width: 300px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.notice-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #303133;
  padding-bottom: 12px;
  border-bottom: 1px solid #f2f6fc;
}

.notice-more {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

.notice-more:hover {
  color: #ff5000;
}

.notice-list {
  flex: 1;
  overflow: hidden;
}

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px dashed #f2f6fc;
  cursor: pointer;
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-item:hover .notice-title {
  color: #ff5000;
}

.notice-title {
  font-size: 13px;
  max-width: 190px;
}

.notice-time {
  font-size: 12px;
  color: #c0c4cc;
  white-space: nowrap;
}

.notice-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 13px;
}

.section-card {
  padding: 16px 20px 20px;
}

.section-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f2f6fc;
  padding-bottom: 12px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.section-sub {
  font-size: 13px;
  color: #909399;
}

.section-more {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
}

.section-more:hover {
  color: #ff5000;
}

/* 分类 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafbfc;
}

.category-item:hover {
  background: #fff7f3;
  color: #ff5000;
}

.category-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd9c9, #ffece4);
  color: #ff5000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.category-name {
  font-size: 14px;
  font-weight: 500;
  max-width: 100%;
}

.category-count {
  font-size: 12px;
  color: #a8abb2;
}

/* 优惠券 */
.coupon-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.coupon-card {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #ffd9c9;
  border-radius: 8px;
  padding: 12px;
  background: #fff9f5;
}

.coupon-left {
  width: 96px;
  flex-shrink: 0;
  text-align: center;
  border-right: 1px dashed #ffc9a8;
  padding-right: 8px;
}

.coupon-value {
  color: #ff5000;
  font-size: 20px;
  font-weight: 700;
}

.coupon-threshold {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.coupon-right {
  flex: 1;
  min-width: 0;
}

.coupon-name {
  font-size: 14px;
  font-weight: 600;
}

.coupon-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.coupon-date {
  font-size: 12px;
  color: #a8abb2;
  margin-top: 4px;
}

.coupon-btn {
  flex-shrink: 0;
}

/* 商品网格：每行 4 个 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
</style>
