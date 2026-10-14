<template>
  <div class="page-container product-list-page">
    <div class="list-body">
      <!-- 左侧筛选栏 -->
      <aside class="side-bar page-card">
        <div class="side-title">商品分类</div>
        <div class="side-all" :class="{ active: !currentCategoryId }" @click="goAllProducts">全部商品</div>
        <ul class="cate-tree">
          <li v-for="first in categories" :key="first.id">
            <div
              class="cate-first"
              :class="{ active: Number(currentCategoryId) === Number(first.id) }"
              @click="goCategory(first.id)"
            >
              <span class="text-ellipsis">{{ first.name }}</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
            <ul v-if="first.children && first.children.length" class="cate-children">
              <li
                v-for="second in first.children"
                :key="second.id"
                class="cate-second text-ellipsis"
                :class="{ active: Number(currentCategoryId) === Number(second.id) }"
                @click="goCategory(second.id)"
              >
                {{ second.name }}
              </li>
            </ul>
          </li>
        </ul>
        <el-empty v-if="!categories.length" description="暂无分类" :image-size="60" />
      </aside>

      <!-- 右侧商品区 -->
      <section class="main-col">
        <!-- 条件说明 -->
        <div class="page-card cond-bar">
          <span class="cond-text">{{ conditionText }}</span>
          <span class="cond-total">共 <b>{{ total }}</b> 件商品</span>
          <el-button v-if="currentCategoryId || keyword" text type="primary" @click="goAllProducts">清除筛选</el-button>
        </div>

        <!-- 筛选条 -->
        <div class="page-card filter-bar">
          <div class="filter-row">
            <span class="filter-label">关键词</span>
            <el-input
              v-model="keywordInput"
              class="keyword-input"
              placeholder="商品名称 / 品牌"
              clearable
              @keyup.enter="handleKeywordSearch"
            />
            <el-button type="primary" icon="Search" @click="handleKeywordSearch">搜索</el-button>
          </div>

          <div class="filter-row">
            <span class="filter-label">价格区间</span>
            <el-input-number v-model="minPrice" :min="0" :controls="false" placeholder="最低价" class="price-input" />
            <span class="price-sep">-</span>
            <el-input-number v-model="maxPrice" :min="0" :controls="false" placeholder="最高价" class="price-input" />
            <el-button @click="handlePriceFilter">确定</el-button>
            <el-button text @click="resetPrice">重置</el-button>
          </div>

          <div class="filter-row">
            <span class="filter-label">排序</span>
            <el-radio-group v-model="sort" @change="handleSortChange">
              <el-radio-button v-for="item in sortOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 商品网格 -->
        <div class="page-card mt-16">
          <div v-if="loading" class="loading-block" v-loading="true" element-loading-text="加载中..." />
          <template v-else>
            <div v-if="list.length" class="product-grid">
              <ShopProductCard v-for="product in list" :key="product.id" :product="product" />
            </div>
            <el-empty v-else description="没有找到符合条件的商品" />
            <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadProducts" />
          </template>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategories, getProductPage } from '@/api/shop'
import ShopProductCard from '@/components/ShopProductCard.vue'
import Pagination from '@/components/Pagination.vue'

/**
 * 商品列表：同一组件服务 /products、/category/:id、/search?keyword=xxx
 */
const route = useRoute()
const router = useRouter()

const categories = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)
const currentCategoryId = ref('')
const keyword = ref('')
const keywordInput = ref('')

const query = reactive({ page: 1, size: 12 })
const sort = ref('default')
const minPrice = ref(undefined)
const maxPrice = ref(undefined)

const sortOptions = [
  { label: '综合', value: 'default' },
  { label: '销量', value: 'sales' },
  { label: '价格升序', value: 'price_asc' },
  { label: '价格降序', value: 'price_desc' },
  { label: '新品', value: 'new' }
]

/** 当前条件说明 */
const conditionText = computed(() => {
  if (keyword.value) {
    return `搜索：${keyword.value}`
  }
  if (currentCategoryId.value) {
    const name = findCategoryName(categories.value, currentCategoryId.value)
    return `分类：${name || currentCategoryId.value}`
  }
  return '全部商品'
})

const findCategoryName = (tree, id) => {
  for (const first of tree) {
    if (String(first.id) === String(id)) return first.name
    if (first.children && first.children.length) {
      const hit = first.children.find((second) => String(second.id) === String(id))
      if (hit) return hit.name
    }
  }
  return ''
}

/** 载入商品分页 */
const loadProducts = async () => {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size,
      sort: sort.value
    }
    if (currentCategoryId.value) {
      params.categoryId = currentCategoryId.value
    }
    if (keyword.value) {
      params.keyword = keyword.value
    }
    if (minPrice.value !== undefined && minPrice.value !== null && minPrice.value !== '') {
      params.minPrice = minPrice.value
    }
    if (maxPrice.value !== undefined && maxPrice.value !== null && maxPrice.value !== '') {
      params.maxPrice = maxPrice.value
    }
    const res = await getProductPage(params)
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 从路由同步筛选条件 */
const syncFromRoute = () => {
  if (route.name === 'CategoryProducts') {
    currentCategoryId.value = route.params.id ? String(route.params.id) : ''
    keyword.value = ''
  } else if (route.name === 'SearchProducts') {
    keyword.value = (route.query.keyword || '').toString()
    currentCategoryId.value = ''
  } else {
    currentCategoryId.value = ''
    keyword.value = ''
  }
  keywordInput.value = keyword.value
  query.page = 1
}

const loadCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (e) {
    categories.value = []
  }
}

const goCategory = (id) => {
  router.push(`/category/${id}`)
}

const goAllProducts = () => {
  if (route.name === 'ProductList') {
    currentCategoryId.value = ''
    keyword.value = ''
    keywordInput.value = ''
    query.page = 1
    loadProducts()
  } else {
    router.push('/products')
  }
}

const handleKeywordSearch = () => {
  const value = keywordInput.value.trim()
  if (route.name === 'SearchProducts') {
    if (value) {
      router.push({ path: '/search', query: { keyword: value } })
    } else {
      router.push('/search')
    }
    return
  }
  // 其它路由下切换为搜索路由
  router.push({ path: '/search', query: value ? { keyword: value } : {} })
}

const handlePriceFilter = () => {
  const min = minPrice.value
  const max = maxPrice.value
  if (min !== undefined && min !== null && max !== undefined && max !== null && Number(min) > Number(max)) {
    const temp = minPrice.value
    minPrice.value = maxPrice.value
    maxPrice.value = temp
  }
  query.page = 1
  loadProducts()
}

const resetPrice = () => {
  minPrice.value = undefined
  maxPrice.value = undefined
  query.page = 1
  loadProducts()
}

const handleSortChange = () => {
  query.page = 1
  loadProducts()
}

// 路由变化（分类切换 / 搜索词变化）重新加载
watch(
  () => route.fullPath,
  () => {
    syncFromRoute()
    loadProducts()
  }
)

onMounted(async () => {
  await loadCategories()
  syncFromRoute()
  loadProducts()
})
</script>

<style scoped>
.product-list-page {
  padding-top: 16px;
}

.list-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.side-bar {
  width: 200px;
  flex-shrink: 0;
}

.side-title {
  font-size: 15px;
  font-weight: 600;
  padding-bottom: 10px;
  border-bottom: 1px solid #f2f6fc;
  margin-bottom: 8px;
}

.side-all {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
}

.side-all:hover,
.side-all.active {
  background: #fff7f3;
  color: #ff5000;
  font-weight: 600;
}

.cate-first {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.cate-first:hover,
.cate-first.active {
  background: #fff7f3;
  color: #ff5000;
}

.cate-children {
  padding-left: 10px;
  margin-bottom: 4px;
}

.cate-second {
  padding: 6px 10px;
  font-size: 13px;
  color: #606266;
  border-radius: 6px;
  cursor: pointer;
}

.cate-second:hover,
.cate-second.active {
  color: #ff5000;
  background: #fff7f3;
}

.main-col {
  flex: 1;
  min-width: 0;
}

.cond-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
}

.cond-text {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.cond-total {
  font-size: 13px;
  color: #909399;
}

.cond-total b {
  color: #ff5000;
}

.filter-bar {
  margin-top: 12px;
  padding: 14px 20px 4px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.filter-label {
  width: 60px;
  flex-shrink: 0;
  font-size: 13px;
  color: #909399;
}

.keyword-input {
  width: 280px;
}

.price-input {
  width: 110px;
}

.price-sep {
  color: #c0c4cc;
}

.loading-block {
  height: 240px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
</style>
