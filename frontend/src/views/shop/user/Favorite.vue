<template>
  <div class="page-card">
    <div class="admin-page-header">
      <div class="admin-page-title">我的收藏</div>
      <span class="text-muted">共 {{ total }} 件商品</span>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />

    <template v-else>
      <div v-if="list.length" class="fav-list">
        <div v-for="item in list" :key="item.id" class="fav-card">
          <el-image :src="item.productImage" fit="cover" class="fav-image" @click="goDetail(item.productId)">
            <template #error><div class="image-fallback">暂无图</div></template>
          </el-image>
          <div class="fav-info">
            <div class="fav-name text-ellipsis-2" @click="goDetail(item.productId)">{{ item.productName }}</div>
            <div class="fav-meta">
              <span class="text-price">{{ formatPrice(item.price) }}</span>
              <span class="text-muted">已售 {{ item.sales || 0 }}</span>
              <el-tag v-if="item.status === 0" type="info" size="small">已下架</el-tag>
            </div>
            <div class="fav-actions">
              <el-button size="small" type="primary" :disabled="item.status === 0" @click="handleAddCart(item)">
                加入购物车
              </el-button>
              <el-button size="small" @click="goDetail(item.productId)">查看详情</el-button>
              <el-button size="small" type="danger" plain @click="handleRemove(item)">取消收藏</el-button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="还没有收藏任何商品">
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
import { deleteFavorite, getFavoritePage } from '@/api/user'
import { addCart } from '@/api/cart'
import { getProductDetail } from '@/api/shop'
import { formatPrice } from '@/utils/format'
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
    const res = await getFavoritePage({ ...query })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

const goDetail = (productId) => router.push(`/product/${productId}`)

/** 只有单 SKU 时直接加购，多规格需到详情页选择 */
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

const handleRemove = async (item) => {
  await ElMessageBox.confirm('确定取消收藏该商品吗？', '提示', { type: 'warning' })
  await deleteFavorite(item.productId)
  ElMessage.success('已取消收藏')
  if (list.value.length === 1 && query.page > 1) {
    query.page -= 1
  }
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.fav-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.fav-card {
  display: flex;
  gap: 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  transition: all 0.2s;
}

.fav-card:hover {
  border-color: #ffb08a;
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.08);
}

.fav-image {
  width: 110px;
  height: 110px;
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

.fav-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.fav-name {
  font-size: 14px;
  line-height: 20px;
  height: 40px;
  cursor: pointer;
}

.fav-name:hover {
  color: #ff5000;
}

.fav-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 8px 0 auto;
  font-size: 13px;
}

.fav-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}
</style>
