<template>
  <div class="product-card" @click="goDetail">
    <div class="card-image">
      <el-image :src="product.mainImage" fit="cover" lazy>
        <template #error>
          <div class="image-fallback">暂无图片</div>
        </template>
      </el-image>
      <el-tag v-if="product.isNew === 1" class="corner-tag new" size="small" effect="dark">新品</el-tag>
      <el-tag v-else-if="product.isHot === 1" class="corner-tag hot" size="small" effect="dark">热销</el-tag>
    </div>
    <div class="card-body">
      <div class="card-name text-ellipsis-2">{{ product.name }}</div>
      <div class="card-subtitle text-ellipsis">{{ product.subtitle || product.brand || '' }}</div>
      <div class="card-footer">
        <span class="price">{{ formatPrice(product.price) }}</span>
        <span class="sales">已售 {{ product.sales || 0 }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { formatPrice } from '@/utils/format'

/**
 * 商城商品卡片
 */
const props = defineProps({
  product: { type: Object, required: true }
})

const router = useRouter()

const goDetail = () => {
  if (props.product && props.product.id) {
    router.push(`/product/${props.product.id}`)
  }
}
</script>

<style scoped>
.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s;
  border: 1px solid transparent;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  border-color: #ffd9c9;
}

.card-image {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f5f7fa;
}

.card-image :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.image-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 13px;
}

.corner-tag {
  position: absolute;
  top: 8px;
  left: 8px;
}

.corner-tag.hot {
  background-color: #ff5000;
  border-color: #ff5000;
}

.card-body {
  padding: 10px 12px 14px;
}

.card-name {
  font-size: 14px;
  line-height: 20px;
  height: 40px;
  color: #303133;
}

.card-subtitle {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  height: 18px;
}

.card-footer {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 8px;
}

.price {
  color: #ff5000;
  font-size: 18px;
  font-weight: 700;
}

.sales {
  font-size: 12px;
  color: #a8abb2;
}
</style>
