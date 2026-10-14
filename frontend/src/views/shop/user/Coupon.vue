<template>
  <div class="page-card">
    <div class="admin-page-header">
      <div class="admin-page-title">我的优惠券</div>
      <el-radio-group v-model="query.status" @change="handleStatusChange">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">未使用</el-radio-button>
        <el-radio-button :value="1">已使用</el-radio-button>
        <el-radio-button :value="2">已过期</el-radio-button>
      </el-radio-group>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />

    <template v-else>
      <div v-if="list.length" class="coupon-list">
        <div
          v-for="item in list"
          :key="item.id"
          class="coupon-card"
          :class="{ disabled: item.status !== 0, used: item.status === 1 }"
        >
          <div class="coupon-left">
            <template v-if="item.type === 1">
              <div class="coupon-amount">
                <span class="symbol">￥</span>{{ formatAmount(item.amount) }}
              </div>
              <div class="coupon-cond">满 {{ formatAmount(item.minAmount) }} 元可用</div>
            </template>
            <template v-else>
              <div class="coupon-amount">
                {{ formatAmount(item.discount) }}<span class="symbol">折</span>
              </div>
              <div class="coupon-cond">满 {{ formatAmount(item.minAmount) }} 元可用</div>
            </template>
          </div>
          <div class="coupon-right">
            <div class="coupon-name text-ellipsis">{{ item.couponName }}</div>
            <div class="coupon-desc text-ellipsis">{{ item.description || '全场通用' }}</div>
            <div class="coupon-time">
              {{ formatDate(item.receiveTime) }} 至 {{ formatDate(item.expireTime) }}
            </div>
          </div>
          <div class="coupon-status">
            <el-tag :type="statusTag(item.status)" effect="dark" size="small">{{ statusLabel(item.status) }}</el-tag>
            <el-button v-if="item.status === 0" type="primary" size="small" class="use-btn" @click="goUse">
              去使用
            </el-button>
            <div v-if="item.status === 1" class="text-muted used-time">已使用</div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无优惠券">
        <el-button type="primary" @click="$router.push('/')">去领券中心</el-button>
      </el-empty>
    </template>

    <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Pagination from '@/components/Pagination.vue'
import { getMyCouponPage } from '@/api/user'
import { COUPON_STATUS, formatAmount, formatDate } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ status: null, page: 1, size: 6 })

const statusLabel = (status) => (COUPON_STATUS[status] || {}).label || '未知'
const statusTag = (status) => (COUPON_STATUS[status] || {}).type || 'info'

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyCouponPage({ ...query })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  query.page = 1
  loadData()
}

const goUse = () => router.push('/products')

onMounted(loadData)
</script>

<style scoped>
.coupon-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.coupon-card {
  display: flex;
  align-items: stretch;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ffe0d1;
  background: #fff;
  position: relative;
}

.coupon-card.disabled {
  border-color: #ebeef5;
  filter: grayscale(0.35);
  opacity: 0.85;
}

.coupon-left {
  width: 150px;
  background: linear-gradient(135deg, #ff7a45, #ff5000);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px 8px;
  flex-shrink: 0;
}

.coupon-card.disabled .coupon-left {
  background: linear-gradient(135deg, #b1b3b8, #909399);
}

.coupon-amount {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
}

.coupon-amount .symbol {
  font-size: 16px;
  font-weight: 500;
}

.coupon-cond {
  font-size: 12px;
  margin-top: 6px;
  opacity: 0.9;
}

.coupon-right {
  flex: 1;
  min-width: 0;
  padding: 14px 16px;
}

.coupon-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.coupon-desc {
  font-size: 12px;
  color: #909399;
  margin: 8px 0;
}

.coupon-time {
  font-size: 12px;
  color: #a8abb2;
}

.coupon-status {
  width: 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-left: 1px dashed #ebeef5;
  flex-shrink: 0;
}

.used-time {
  font-size: 12px;
}
</style>
