<template>
  <div class="page-container detail-page" v-loading="loading">
    <template v-if="product">
      <!-- 面包屑 -->
      <div class="crumb page-card">
        <router-link to="/">首页</router-link>
        <span class="crumb-sep">/</span>
        <router-link to="/products">全部商品</router-link>
        <span class="crumb-sep">/</span>
        <span class="crumb-current text-ellipsis">{{ product.name }}</span>
      </div>

      <!-- 主图 + 信息 -->
      <div class="main-card page-card">
        <div class="gallery">
          <el-carousel v-if="galleryImages.length > 1" height="380px" :autoplay="false" indicator-position="none" arrow="hover">
            <el-carousel-item v-for="(img, index) in galleryImages" :key="index">
              <el-image class="big-image" :src="img" fit="contain">
                <template #error>
                  <div class="image-fallback">暂无图片</div>
                </template>
              </el-image>
            </el-carousel-item>
          </el-carousel>
          <div v-else class="single-image">
            <el-image class="big-image" :src="galleryImages[0]" fit="contain">
              <template #error>
                <div class="image-fallback">暂无图片</div>
              </template>
            </el-image>
          </div>
          <div v-if="galleryImages.length > 1" class="thumb-list">
            <div
              v-for="(img, index) in galleryImages"
              :key="index"
              class="thumb-item"
              :class="{ active: currentImage === img }"
              @click="currentImage = img"
            >
              <el-image :src="img" fit="cover">
                <template #error>
                  <div class="image-fallback small">无图</div>
                </template>
              </el-image>
            </div>
          </div>
        </div>

        <div class="info">
          <h1 class="product-name">{{ product.name }}</h1>
          <p v-if="product.subtitle" class="product-subtitle">{{ product.subtitle }}</p>

          <div class="price-box">
            <div class="price-row">
              <span class="price-label">价格</span>
              <span class="price-now">{{ formatPrice(currentPrice) }}</span>
              <span v-if="currentOriginalPrice && Number(currentOriginalPrice) > Number(currentPrice)" class="price-old">
                {{ formatPrice(currentOriginalPrice) }}
              </span>
            </div>
            <div class="meta-row">
              <span>销量 <b>{{ product.sales || 0 }}</b></span>
              <span>品牌 <b>{{ product.brand || '-' }}</b></span>
              <span>单位 <b>{{ product.unit || '件' }}</b></span>
              <span>
                库存
                <b :class="{ 'text-danger': currentStock <= 0 }">{{ currentStock }}</b>
              </span>
            </div>
          </div>

          <!-- 规格选择 -->
          <div v-for="spec in specs" :key="spec.id" class="spec-row">
            <span class="spec-label">{{ spec.specName }}</span>
            <div class="spec-values">
              <span
                v-for="value in spec.values"
                :key="value"
                class="spec-value"
                :class="{ active: selectedSpecs[spec.specName] === value }"
                @click="selectSpec(spec.specName, value)"
              >
                {{ value }}
              </span>
            </div>
          </div>

          <!-- 数量 -->
          <div class="spec-row">
            <span class="spec-label">数量</span>
            <div class="spec-values">
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="currentStock > 0 ? currentStock : 1"
                :disabled="currentStock <= 0"
              />
              <span class="stock-tip">
                {{ currentStock > 0 ? `库存 ${currentStock} 件` : '该规格暂时缺货' }}
              </span>
            </div>
          </div>

          <div v-if="!currentSku && specs.length" class="sku-tip">请选择完整规格后加入购物车</div>

          <div class="action-row">
            <el-button type="danger" size="large" icon="ShoppingCart" :disabled="!canBuy" @click="handleAddCart">
              加入购物车
            </el-button>
            <el-button type="warning" size="large" :disabled="!canBuy" @click="handleBuyNow">立即购买</el-button>
            <el-button size="large" :icon="favorited ? 'StarFilled' : 'Star'" @click="handleFavorite">
              {{ favorited ? '已收藏' : '收藏' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 详情与评价 -->
      <div class="page-card mt-16">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="商品详情" name="detail">
            <div v-if="product.detail" class="detail-html" v-html="product.detail" />
            <el-empty v-else description="暂无图文详情" />
          </el-tab-pane>
          <el-tab-pane :label="`商品评价(${commentTotal})`" name="comment">
            <div class="comment-wrap">
              <!-- 评价统计 -->
              <div class="stats-card">
                <div class="stats-left">
                  <div class="stats-score">{{ stats.avgStar || 0 }}</div>
                  <el-rate :model-value="Number(stats.avgStar || 0)" disabled show-score score-template="{value} 分" />
                  <div class="stats-total">共 {{ stats.total || 0 }} 条评价</div>
                </div>
                <div class="stats-right">
                  <div v-for="star in [5, 4, 3, 2, 1]" :key="star" class="stats-row">
                    <span class="stats-star">{{ star }} 星</span>
                    <el-progress
                      class="stats-progress"
                      :percentage="starPercent(star)"
                      :stroke-width="10"
                      :show-text="false"
                    />
                    <span class="stats-count">{{ starCount(star) }}</span>
                  </div>
                </div>
              </div>

              <!-- 星级筛选 -->
              <div class="comment-filter">
                <span class="filter-label">筛选</span>
                <el-radio-group v-model="commentStar" @change="handleStarChange">
                  <el-radio-button :value="0">全部</el-radio-button>
                  <el-radio-button v-for="star in [5, 4, 3, 2, 1]" :key="star" :value="star">
                    {{ star }} 星
                  </el-radio-button>
                </el-radio-group>
              </div>

              <!-- 评价列表 -->
              <div v-if="comments.length" class="comment-list">
                <div v-for="item in comments" :key="item.id" class="comment-item">
                  <el-avatar :size="40" :src="item.userAvatar || ''">
                    {{ (item.userNickname || '匿')[0] }}
                  </el-avatar>
                  <div class="comment-main">
                    <div class="comment-head">
                      <span class="comment-user">{{ item.userNickname || '匿名用户' }}</span>
                      <el-rate :model-value="item.star" disabled size="small" />
                      <span class="comment-time">{{ formatDateTime(item.createTime) }}</span>
                    </div>
                    <div class="comment-content">{{ item.content }}</div>
                    <div v-if="item.images && item.images.length" class="comment-images">
                      <el-image
                        v-for="(img, index) in item.images"
                        :key="index"
                        class="comment-image"
                        :src="img"
                        fit="cover"
                        :preview-src-list="item.images"
                        :initial-index="index"
                        preview-teleported
                      >
                        <template #error>
                          <div class="image-fallback small">无图</div>
                        </template>
                      </el-image>
                    </div>
                    <div v-if="item.specInfo" class="comment-spec">规格：{{ item.specInfo }}</div>
                    <div v-if="item.replyContent" class="merchant-reply">
                      <span class="reply-tag">商家回复</span>
                      <span>{{ item.replyContent }}</span>
                    </div>
                  </div>
                </div>
                <Pagination v-model:page="commentQuery.page" v-model:size="commentQuery.size" :total="commentTotal" @change="loadComments" />
              </div>
              <el-empty v-else description="暂无评价" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>
    <el-empty v-else-if="!loading" description="商品不存在或已下架" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addCart } from '@/api/cart'
import { getCommentStats, getProductComments, getProductDetail } from '@/api/shop'
import { recordHistory, toggleFavorite } from '@/api/user'
import { useBuyerStore } from '@/store/buyer'
import { useCartStore } from '@/store/cart'
import { formatDateTime, formatPrice, splitImages } from '@/utils/format'
import Pagination from '@/components/Pagination.vue'

/**
 * 商品详情：主图切换 / 规格联动 SKU / 加购 / 收藏 / 图文详情 / 评价
 */
const route = useRoute()
const router = useRouter()
const buyerStore = useBuyerStore()
const cartStore = useCartStore()

const loading = ref(false)
const product = ref(null)
const specs = ref([])
const skus = ref([])
const selectedSpecs = reactive({})
const quantity = ref(1)
const currentImage = ref('')
const favorited = ref(false)
const activeTab = ref('detail')

const stats = ref({ avgStar: 0, total: 0, starCount: {} })
const comments = ref([])
const commentStar = ref(0)
const commentQuery = reactive({ page: 1, size: 5 })

/** 图集：轮播图优先，为空时使用主图 */
const galleryImages = computed(() => {
  if (!product.value) return []
  const images = splitImages(product.value.images)
  const list = images.length ? images : [product.value.mainImage].filter(Boolean)
  return list
})

/** 当前选中的 SKU */
const currentSku = computed(() => {
  if (!specs.value.length) return null
  const keys = specs.value.map((item) => item.specName)
  const picked = keys.map((key) => selectedSpecs[key])
  if (picked.some((value) => !value)) return null
  return (
    skus.value.find((sku) => {
      const data = parseSpecData(sku.specData)
      return keys.every((key) => data[key] === selectedSpecs[key])
    }) || null
  )
})

const currentPrice = computed(() => {
  if (currentSku.value) return currentSku.value.price
  return product.value ? product.value.price : 0
})

const currentOriginalPrice = computed(() => {
  if (currentSku.value) return currentSku.value.originalPrice
  return product.value ? product.value.originalPrice : 0
})

const currentStock = computed(() => {
  if (currentSku.value) return Number(currentSku.value.stock || 0)
  if (!specs.value.length) return Number((product.value && product.value.stock) || 0)
  return 0
})

/** 是否可以购买：单规格商品直接可买，多规格需匹配到 SKU 且有库存 */
const canBuy = computed(() => {
  if (!product.value) return false
  if (!specs.value.length) return Number(product.value.stock || 0) > 0
  return !!currentSku.value && Number(currentSku.value.stock || 0) > 0
})

/** 解析 SKU 的 specData（JSON 字符串） */
function parseSpecData(specData) {
  if (!specData) return {}
  try {
    const data = JSON.parse(specData)
    return typeof data === 'object' && data !== null ? data : {}
  } catch (e) {
    return {}
  }
}

/** 规格值解析：兼容数组与逗号分隔字符串 */
const parseSpecValues = (values) => {
  if (Array.isArray(values)) return values
  if (!values) return []
  return String(values)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

const selectSpec = (specName, value) => {
  if (selectedSpecs[specName] === value) {
    delete selectedSpecs[specName]
  } else {
    selectedSpecs[specName] = value
  }
  quantity.value = 1
  const sku = currentSku.value
  if (sku && sku.image) {
    currentImage.value = sku.image
  }
}

const starCount = (star) => {
  const map = stats.value.starCount || {}
  return Number(map[star] || map[String(star)] || 0)
}

const starPercent = (star) => {
  const total = Number(stats.value.total || 0)
  if (!total) return 0
  return Math.round((starCount(star) / total) * 100)
}

const commentTotal = computed(() => Number(stats.value.total || 0))

/** 载入商品详情 */
const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getProductDetail(route.params.id)
    const data = res.data || null
    product.value = data
    specs.value = ((data && data.specs) || []).map((item) => ({
      id: item.id,
      specName: item.specName,
      values: parseSpecValues(item.specValues)
    }))
    skus.value = (data && data.skus) || []
    Object.keys(selectedSpecs).forEach((key) => delete selectedSpecs[key])
    quantity.value = 1
    currentImage.value = galleryImages.value[0] || ''
    favorited.value = !!(data && data.favorited)
    stats.value = { avgStar: 0, total: 0, starCount: {} }
    comments.value = []
    commentStar.value = 0
    commentQuery.page = 1
    loadStats()
    loadComments()
    recordBrowse()
  } catch (e) {
    product.value = null
  } finally {
    loading.value = false
  }
}

/** 记录浏览足迹（失败静默） */
const recordBrowse = async () => {
  if (!buyerStore.isLogin) return
  try {
    await recordHistory(route.params.id)
  } catch (e) {
    /* 浏览记录失败不打扰用户 */
  }
}

const loadStats = async () => {
  try {
    const res = await getCommentStats(route.params.id)
    stats.value = res.data || { avgStar: 0, total: 0, starCount: {} }
  } catch (e) {
    stats.value = { avgStar: 0, total: 0, starCount: {} }
  }
}

const loadComments = async () => {
  try {
    const params = { page: commentQuery.page, size: commentQuery.size }
    if (commentStar.value) {
      params.star = commentStar.value
    }
    const res = await getProductComments(route.params.id, params)
    comments.value = res.data.records || []
  } catch (e) {
    comments.value = []
  }
}

const handleStarChange = () => {
  commentQuery.page = 1
  loadComments()
}

/** 未登录统一跳登录页 */
const requireLogin = () => {
  if (buyerStore.isLogin) return true
  ElMessage.warning('请先登录后再操作')
  router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
  return false
}

const resolveSkuId = () => {
  if (currentSku.value) return currentSku.value.id
  if (!specs.value.length && skus.value.length === 1) return skus.value[0].id
  return null
}

const handleAddCart = async () => {
  if (!requireLogin()) return
  const skuId = resolveSkuId()
  if (!skuId) {
    ElMessage.warning('请选择完整的商品规格')
    return
  }
  await addCart({ productId: product.value.id, skuId, quantity: quantity.value })
  ElMessage.success('已加入购物车')
  cartStore.refresh()
}

const handleBuyNow = () => {
  if (!requireLogin()) return
  const skuId = resolveSkuId()
  if (!skuId) {
    ElMessage.warning('请选择完整的商品规格')
    return
  }
  router.push({ path: '/checkout', query: { skuId, quantity: quantity.value } })
}

const handleFavorite = async () => {
  if (!requireLogin()) return
  const res = await toggleFavorite(product.value.id)
  favorited.value = typeof res.data === 'boolean' ? res.data : !favorited.value
  ElMessage.success(favorited.value ? '收藏成功' : '已取消收藏')
}

watch(
  () => route.params.id,
  (value) => {
    if (value) loadDetail()
  }
)

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.detail-page {
  padding-top: 16px;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.crumb a:hover {
  color: #ff5000;
}

.crumb-sep {
  color: #dcdfe6;
}

.crumb-current {
  color: #303133;
  max-width: 500px;
}

.main-card {
  display: flex;
  gap: 30px;
}

.gallery {
  width: 420px;
  flex-shrink: 0;
}

.big-image {
  width: 100%;
  height: 380px;
  background: #fafbfc;
}

.image-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f5f7fa;
  font-size: 13px;
}

.image-fallback.small {
  font-size: 12px;
}

.single-image {
  height: 380px;
}

.thumb-list {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.thumb-item {
  width: 64px;
  height: 64px;
  border: 2px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}

.thumb-item.active {
  border-color: #ff5000;
}

.thumb-item :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.info {
  flex: 1;
  min-width: 0;
}

.product-name {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 8px;
  line-height: 30px;
}

.product-subtitle {
  color: #ff5000;
  font-size: 14px;
  margin: 0 0 12px;
}

.price-box {
  background: #fff7f3;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.price-label {
  font-size: 13px;
  color: #909399;
}

.price-now {
  color: #ff5000;
  font-size: 28px;
  font-weight: 700;
}

.price-old {
  color: #a8abb2;
  text-decoration: line-through;
  font-size: 14px;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
}

.meta-row b {
  color: #303133;
}

.spec-row {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
  align-items: flex-start;
}

.spec-label {
  width: 60px;
  flex-shrink: 0;
  font-size: 13px;
  color: #909399;
  line-height: 32px;
}

.spec-values {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.spec-value {
  padding: 6px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.spec-value:hover {
  border-color: #ff5000;
  color: #ff5000;
}

.spec-value.active {
  border-color: #ff5000;
  color: #ff5000;
  background: #fff7f3;
  font-weight: 600;
}

.stock-tip {
  font-size: 12px;
  color: #909399;
}

.sku-tip {
  color: #e6a23c;
  font-size: 13px;
  margin-bottom: 10px;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.detail-html {
  line-height: 1.8;
  color: #303133;
}

.detail-html :deep(img) {
  max-width: 100%;
}

.comment-wrap {
  padding-top: 6px;
}

.stats-card {
  display: flex;
  gap: 40px;
  background: #fafbfc;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.stats-left {
  width: 180px;
  text-align: center;
  border-right: 1px solid #ebeef5;
}

.stats-score {
  font-size: 34px;
  font-weight: 700;
  color: #ff5000;
  line-height: 1.2;
}

.stats-total {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.stats-right {
  flex: 1;
}

.stats-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.stats-star {
  width: 40px;
  font-size: 12px;
  color: #909399;
}

.stats-progress {
  flex: 1;
}

.stats-count {
  width: 46px;
  text-align: right;
  font-size: 12px;
  color: #909399;
}

.comment-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.filter-label {
  font-size: 13px;
  color: #909399;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f2f6fc;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.comment-user {
  font-weight: 600;
  font-size: 14px;
}

.comment-time {
  margin-left: auto;
  font-size: 12px;
  color: #a8abb2;
}

.comment-content {
  margin-top: 8px;
  font-size: 14px;
  line-height: 22px;
  color: #303133;
  word-break: break-word;
}

.comment-images {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.comment-image {
  width: 76px;
  height: 76px;
  border-radius: 6px;
  overflow: hidden;
}

.comment-spec {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.merchant-reply {
  margin-top: 10px;
  background: #f7f8fa;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  color: #606266;
}

.reply-tag {
  color: #ff5000;
  font-weight: 600;
  margin-right: 6px;
}
</style>
