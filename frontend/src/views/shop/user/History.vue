<template>
  <div class="page-card">
    <div class="admin-page-header">
      <div class="admin-page-title">浏览记录</div>
      <el-button v-if="list.length" type="danger" plain icon="Delete" @click="handleClear">清空记录</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />

    <template v-else>
      <div v-if="list.length" class="history-list">
        <div v-for="item in list" :key="item.id" class="history-row">
          <el-image :src="item.productImage" fit="cover" class="history-image" @click="goDetail(item.productId)">
            <template #error><div class="image-fallback">暂无图</div></template>
          </el-image>
          <div class="history-info">
            <div class="history-name text-ellipsis-2" @click="goDetail(item.productId)">{{ item.productName }}</div>
            <div class="history-meta">
              <span class="text-price">{{ formatPrice(item.price) }}</span>
              <span class="text-muted">已售 {{ item.sales || 0 }}</span>
              <el-tag v-if="item.status === 0" type="info" size="small">已下架</el-tag>
            </div>
            <div class="text-muted history-time">浏览时间：{{ formatDateTime(item.browseTime) }}</div>
          </div>
          <div class="history-actions">
            <el-button size="small" type="primary" :disabled="item.status === 0" @click="handleAddCart(item)">
              加入购物车
            </el-button>
            <el-button size="small" @click="goDetail(item.productId)">查看详情</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无浏览记录">
        <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
      </el-empty>
    </template>

    <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import { clearHistory, getHistoryPage } from '@/api/user'
import { addCart } from '@/api/cart'
import { getProductDetail } from '@/api/shop'
import { formatDateTime, formatPrice } from '@/utils/format'
import { useCartStore } from '@/store/cart'

const router = useRouter()
const cartStore = useCartStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 8 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getHistoryPage({ ...query })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

const goDetail = (productId) => router.push(`/product/${productId}`)

const handleAddCart = async (item) => {
  try {
    const res = await getProductDetail(item.productId)
    const skus = (res.data && res.data.skus) || []
    if (skus.length !== 1) {
      ElMessage.info('该商品有多个规格，请到详情页选择规格后加入购物车')
      router.push(`/product/${item.productId}`)
      return
    }
    await addCart({ productId: item.productId, skuId: skus[0].id, quantity: 1 })
    ElMessage.success('已加入购物车')
    cartStore.refresh()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

const handleClear = async () => {
  await ElMessageBox.confirm('确定清空全部浏览记录吗？', '清空记录', { type: 'warning' })
  await clearHistory()
  ElMessage.success('已清空')
  query.page = 1
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.history-list {
  display: flex;
  flex-direction: column;
}

.history-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 4px;
  border-bottom: 1px dashed #ebeef5;
}

.history-image {
  width: 84px;
  height: 84px;
  border-radius: 6px;
  flex-shrink: 0;
  cursor: pointer;
}

.image-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 12px;
}

.history-info {
  flex: 1;
  min-width: 0;
}

.history-name {
  font-size: 14px;
  line-height: 20px;
  cursor: pointer;
}

.history-name:hover {
  color: #ff5000;
}

.history-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 6px 0;
  font-size: 13px;
}

.history-time {
  font-size: 12px;
}

.history-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>
