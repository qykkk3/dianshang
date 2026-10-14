<template>
  <div class="page-container cart-page" v-loading="loading">
    <div class="page-card">
      <div class="cart-head">
        <span class="head-title">我的购物车</span>
        <span class="head-tip">共 {{ allItems.length }} 种商品，其中 {{ invalidItems.length }} 种已失效</span>
      </div>

      <template v-if="allItems.length">
        <!-- 表头 -->
        <div class="cart-row cart-header">
          <div class="col-check">
            <el-checkbox :model-value="validAllChecked" :indeterminate="validIndeterminate" @change="handleCheckAll">
              全选
            </el-checkbox>
          </div>
          <div class="col-info">商品信息</div>
          <div class="col-price">单价</div>
          <div class="col-qty">数量</div>
          <div class="col-sum">小计</div>
          <div class="col-op">操作</div>
        </div>

        <!-- 有效商品 -->
        <div v-if="validItems.length" class="group-title">有效商品（{{ validItems.length }}）</div>
        <div v-for="item in validItems" :key="item.id" class="cart-row">
          <div class="col-check">
            <el-checkbox
              :model-value="item.checked === 1"
              @change="(value) => handleCheckOne(item, value)"
            />
          </div>
          <div class="col-info">
            <el-image class="item-image" :src="item.productImage" fit="cover" @click="goProduct(item.productId)">
              <template #error>
                <div class="image-fallback">无图</div>
              </template>
            </el-image>
            <div class="item-text">
              <div class="item-name text-ellipsis-2" @click="goProduct(item.productId)">{{ item.productName }}</div>
              <div class="item-spec">{{ item.specInfo || '默认规格' }}</div>
            </div>
          </div>
          <div class="col-price">{{ formatPrice(item.price) }}</div>
          <div class="col-qty">
            <el-input-number
              v-model="item.quantity"
              :min="1"
              :max="item.stock > 0 ? item.stock : 1"
              size="small"
              @change="(value) => handleQuantityChange(item, value)"
            />
            <div v-if="item.stock <= 5" class="stock-warn">仅剩 {{ item.stock }} 件</div>
          </div>
          <div class="col-sum text-price">{{ formatPrice(item.price * item.quantity) }}</div>
          <div class="col-op">
            <el-button text type="danger" @click="handleDelete(item)">删除</el-button>
          </div>
        </div>

        <!-- 失效商品 -->
        <template v-if="invalidItems.length">
          <div class="group-title invalid">失效商品（{{ invalidItems.length }}）</div>
          <div v-for="item in invalidItems" :key="item.id" class="cart-row invalid-row">
            <div class="col-check">
              <el-tag type="info" size="small">失效</el-tag>
            </div>
            <div class="col-info">
              <el-image class="item-image" :src="item.productImage" fit="cover">
                <template #error>
                  <div class="image-fallback">无图</div>
                </template>
              </el-image>
              <div class="item-text">
                <div class="item-name text-ellipsis-2">{{ item.productName }}</div>
                <div class="item-spec">商品已下架或库存不足</div>
              </div>
            </div>
            <div class="col-price text-muted">{{ formatPrice(item.price) }}</div>
            <div class="col-qty text-muted">× {{ item.quantity }}</div>
            <div class="col-sum text-muted">{{ formatPrice(item.price * item.quantity) }}</div>
            <div class="col-op">
              <el-button text type="danger" @click="handleDelete(item)">删除</el-button>
            </div>
          </div>
        </template>
      </template>

      <el-empty v-else description="购物车还是空的，快去挑选商品吧">
        <el-button type="danger" @click="router.push('/products')">去逛逛</el-button>
      </el-empty>
    </div>

    <!-- 底部结算栏 -->
    <div v-if="allItems.length" class="settle-bar">
      <div class="page-container settle-inner">
        <el-checkbox :model-value="validAllChecked" :indeterminate="validIndeterminate" @change="handleCheckAll">
          全选
        </el-checkbox>
        <el-button text type="danger" :disabled="!selectedValidItems.length" @click="handleBatchDelete">
          删除选中
        </el-button>
        <div class="settle-right">
          <span class="settle-count">
            已选 <b>{{ selectedValidItems.length }}</b> 件
          </span>
          <span class="settle-amount">
            合计：<b>{{ formatPrice(totalAmount) }}</b>
          </span>
          <el-button type="danger" size="large" :disabled="!selectedValidItems.length" @click="handleCheckout">
            去结算
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteCart, deleteCartBatch, getCartList, updateCart, updateCartChecked } from '@/api/cart'
import { useCartStore } from '@/store/cart'
import { formatPrice } from '@/utils/format'

/**
 * 购物车：全选/单选、数量变更、删除、结算
 */
const router = useRouter()
const cartStore = useCartStore()

const loading = ref(false)
const list = ref([])

const allItems = computed(() => list.value)
const validItems = computed(() => list.value.filter((item) => item.status === 1))
const invalidItems = computed(() => list.value.filter((item) => item.status !== 1))
const selectedValidItems = computed(() => validItems.value.filter((item) => item.checked === 1))
const validAllChecked = computed(() => validItems.value.length > 0 && selectedValidItems.value.length === validItems.value.length)
const validIndeterminate = computed(
  () => selectedValidItems.value.length > 0 && selectedValidItems.value.length < validItems.value.length
)

/** 合计金额：只统计已选中且有效的商品 */
const totalAmount = computed(() =>
  selectedValidItems.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0)
)

const loadCart = async () => {
  loading.value = true
  try {
    const res = await getCartList()
    list.value = res.data || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

const goProduct = (productId) => {
  if (productId) router.push(`/product/${productId}`)
}

/** 全选 / 取消全选：ids 为空数组表示全部 */
const handleCheckAll = async (value) => {
  const checked = value ? 1 : 0
  await updateCartChecked({ ids: [], checked })
  await loadCart()
  cartStore.refresh()
}

/** 单选切换 */
const handleCheckOne = async (item, value) => {
  const checked = value ? 1 : 0
  item.checked = checked
  await updateCartChecked({ ids: [item.id], checked })
  cartStore.refresh()
}

/** 数量变更（el-input-number 在失焦/点击步进时触发，天然防抖） */
const handleQuantityChange = async (item, value) => {
  const quantity = Number(value || 1)
  if (!quantity || quantity < 1) return
  await updateCart(item.id, { quantity })
  ElMessage.success('数量已更新')
  await loadCart()
  cartStore.refresh()
}

const handleDelete = async (item) => {
  await ElMessageBox.confirm(`确定要删除「${item.productName}」吗？`, '删除确认', { type: 'warning' })
  await deleteCart(item.id)
  ElMessage.success('删除成功')
  await loadCart()
  cartStore.refresh()
}

const handleBatchDelete = async () => {
  const ids = selectedValidItems.value.map((item) => item.id)
  if (!ids.length) return
  await ElMessageBox.confirm(`确定要删除选中的 ${ids.length} 件商品吗？`, '批量删除', { type: 'warning' })
  await deleteCartBatch(ids)
  ElMessage.success('删除成功')
  await loadCart()
  cartStore.refresh()
}

const handleCheckout = () => {
  const ids = selectedValidItems.value.map((item) => item.id)
  if (!ids.length) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }
  router.push({ path: '/checkout', query: { cartIds: ids.join(',') } })
}

onMounted(() => {
  loadCart()
})
</script>

<style scoped>
.cart-page {
  padding-top: 16px;
}

.cart-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f2f6fc;
}

.head-title {
  font-size: 18px;
  font-weight: 700;
}

.head-tip {
  font-size: 13px;
  color: #909399;
}

.cart-row {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f2f6fc;
}

.cart-header {
  font-size: 13px;
  color: #909399;
  padding: 12px 0;
  background: #fafbfc;
}

.col-check {
  width: 90px;
  flex-shrink: 0;
}

.col-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.col-price {
  width: 110px;
  flex-shrink: 0;
  text-align: center;
}

.col-qty {
  width: 150px;
  flex-shrink: 0;
  text-align: center;
}

.col-sum {
  width: 120px;
  flex-shrink: 0;
  text-align: center;
  font-weight: 600;
}

.col-op {
  width: 80px;
  flex-shrink: 0;
  text-align: center;
}

.item-image {
  width: 78px;
  height: 78px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  background: #f5f7fa;
}

.image-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #c0c4cc;
}

.item-text {
  min-width: 0;
}

.item-name {
  font-size: 14px;
  cursor: pointer;
}

.item-name:hover {
  color: #ff5000;
}

.item-spec {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.stock-warn {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
}

.group-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding: 14px 0 4px;
}

.group-title.invalid {
  color: #909399;
}

.invalid-row {
  background: #fafafa;
  color: #a8abb2;
}

.invalid-row .item-name {
  color: #a8abb2;
  cursor: default;
}

.settle-bar {
  position: sticky;
  bottom: 0;
  background: #fff;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
  margin-top: 16px;
  border-radius: 8px;
}

.settle-inner {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 64px;
}

.settle-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 20px;
}

.settle-count {
  font-size: 14px;
  color: #606266;
}

.settle-count b {
  color: #ff5000;
}

.settle-amount {
  font-size: 14px;
  color: #606266;
}

.settle-amount b {
  color: #ff5000;
  font-size: 22px;
}
</style>
