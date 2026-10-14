<template>
  <div class="page-container checkout-page" v-loading="loading">
    <!-- 收货地址 -->
    <div class="page-card">
      <div class="block-head">
        <span class="block-title">收货地址</span>
        <el-button type="primary" text icon="Plus" @click="openAddressDialog()">新增地址</el-button>
      </div>
      <div v-if="addressList.length" class="address-list">
        <div
          v-for="item in addressList"
          :key="item.id"
          class="address-card"
          :class="{ active: addressId === item.id }"
          @click="handleSelectAddress(item)"
        >
          <div class="address-top">
            <span class="address-name">{{ item.receiver }}</span>
            <span class="address-phone">{{ item.phone }}</span>
            <el-tag v-if="item.isDefault === 1" type="danger" size="small" effect="plain">默认</el-tag>
          </div>
          <div class="address-detail">{{ item.fullAddress || `${item.province}${item.city}${item.district}${item.detail}` }}</div>
          <div class="address-op">
            <el-button text type="primary" size="small" @click.stop="openAddressDialog(item)">编辑</el-button>
            <el-button text type="danger" size="small" @click.stop="handleDeleteAddress(item)">删除</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="还没有收货地址，请先添加" :image-size="80">
        <el-button type="danger" @click="openAddressDialog()">新增地址</el-button>
      </el-empty>
    </div>

    <!-- 商品清单 -->
    <div class="page-card mt-16">
      <div class="block-head">
        <span class="block-title">商品清单</span>
        <span class="block-tip">共 {{ preview.items.length }} 种商品</span>
      </div>
      <el-table :data="preview.items" border stripe>
        <el-table-column label="商品" min-width="320">
          <template #default="{ row }">
            <div class="item-cell">
              <el-image class="item-image" :src="row.productImage" fit="cover">
                <template #error>
                  <div class="image-fallback">无图</div>
                </template>
              </el-image>
              <div class="item-text">
                <div class="item-name text-ellipsis-2">{{ row.productName }}</div>
                <div class="item-spec">{{ row.specInfo || '默认规格' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="130" align="center">
          <template #default="{ row }">{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="100" align="center">
          <template #default="{ row }">× {{ row.quantity }}</template>
        </el-table-column>
        <el-table-column label="小计" width="140" align="center">
          <template #default="{ row }">
            <span class="text-price">{{ formatPrice(row.totalAmount || row.price * row.quantity) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 优惠券 -->
    <div class="page-card mt-16">
      <div class="block-head">
        <span class="block-title">优惠券</span>
        <span class="block-tip">共 {{ preview.availableCoupons.length }} 张可用</span>
      </div>
      <div v-if="preview.availableCoupons.length" class="coupon-list">
        <div class="coupon-item" :class="{ active: !userCouponId }" @click="handleSelectCoupon(null)">
          <div class="coupon-name">不使用优惠券</div>
        </div>
        <div
          v-for="item in preview.availableCoupons"
          :key="item.id"
          class="coupon-item"
          :class="{ active: userCouponId === item.id }"
          @click="handleSelectCoupon(item.id)"
        >
          <div class="coupon-value">{{ couponValue(item) }}</div>
          <div class="coupon-name text-ellipsis">{{ item.couponName || item.name }}</div>
          <div class="coupon-threshold">满 {{ formatAmount(item.minAmount) }} 元可用</div>
        </div>
      </div>
      <div v-else class="text-muted">当前订单没有可用的优惠券</div>
    </div>

    <!-- 备注 + 金额 -->
    <div class="page-card mt-16">
      <div class="block-head">
        <span class="block-title">买家备注</span>
      </div>
      <el-input
        v-model="remark"
        type="textarea"
        :rows="2"
        maxlength="200"
        show-word-limit
        placeholder="选填，如：工作日白天送货、需要发票等"
        class="remark-input"
      />

      <div class="amount-box">
        <div class="amount-row">
          <span>商品总额</span>
          <span>{{ formatPrice(preview.totalAmount) }}</span>
        </div>
        <div class="amount-row">
          <span>运费</span>
          <span>{{ formatPrice(preview.freightAmount) }}</span>
        </div>
        <div class="amount-row">
          <span>优惠金额</span>
          <span class="text-danger">- {{ formatPrice(preview.discountAmount) }}</span>
        </div>
        <div class="amount-row total">
          <span>应付金额</span>
          <span class="text-price">{{ formatPrice(preview.payAmount) }}</span>
        </div>
      </div>

      <div class="submit-row">
        <el-button size="large" @click="router.back()">返回</el-button>
        <el-button type="danger" size="large" :loading="submitting" @click="handleSubmit">提交订单</el-button>
      </div>
    </div>

    <!-- 地址弹窗 -->
    <el-dialog v-model="addressDialogVisible" :title="addressForm.id ? '编辑收货地址' : '新增收货地址'" width="520px">
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="90px">
        <el-form-item label="收货人" prop="receiver">
          <el-input v-model="addressForm.receiver" placeholder="请输入收货人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="addressForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="addressForm.province" placeholder="如：江苏省" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="addressForm.city" placeholder="如：南京市" />
        </el-form-item>
        <el-form-item label="区/县" prop="district">
          <el-input v-model="addressForm.district" placeholder="如：鼓楼区" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="addressForm.detail" type="textarea" :rows="2" placeholder="街道、门牌号等" />
        </el-form-item>
        <el-form-item label="邮编">
          <el-input v-model="addressForm.postCode" placeholder="选填" maxlength="6" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addressSaving" @click="handleSaveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addAddress, deleteAddress, getAddressList, updateAddress } from '@/api/cart'
import { payOrder, previewOrder, submitOrder } from '@/api/order'
import { useCartStore } from '@/store/cart'
import { formatAmount, formatPrice } from '@/utils/format'

/**
 * 确认订单：地址选择 / 优惠券试算 / 提交订单 / 模拟支付
 */
const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

/** 结算来源 */
const cartIds = ref([])
const buyNow = ref(null)

const loading = ref(false)
const submitting = ref(false)
const addressList = ref([])
const addressId = ref(null)
const userCouponId = ref(null)
const remark = ref('')

const preview = reactive({
  items: [],
  address: null,
  totalAmount: 0,
  freightAmount: 0,
  discountAmount: 0,
  payAmount: 0,
  availableCoupons: []
})

/* ------------------------------ 地址弹窗 ------------------------------ */
const addressDialogVisible = ref(false)
const addressSaving = ref(false)
const addressFormRef = ref(null)
const addressForm = reactive({
  id: null,
  receiver: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  postCode: '',
  isDefault: 0
})

const addressRules = {
  receiver: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区/县', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const couponValue = (item) => {
  if (item.type === 2) {
    return `${Number(item.discount || 0)} 折`
  }
  return `￥${formatAmount(item.amount)}`
}

/** 解析结算来源参数 */
const parseQuery = () => {
  const ids = route.query.cartIds
  if (ids) {
    cartIds.value = String(ids)
      .split(',')
      .map((item) => Number(item.trim()))
      .filter((item) => !Number.isNaN(item) && item > 0)
  } else {
    cartIds.value = []
  }
  if (route.query.skuId) {
    buyNow.value = {
      skuId: Number(route.query.skuId),
      quantity: Number(route.query.quantity || 1)
    }
  } else {
    buyNow.value = null
  }
}

/** 组装预览/提交参数 */
const buildParams = () => {
  const params = { userCouponId: userCouponId.value || null }
  if (cartIds.value.length) {
    params.cartIds = cartIds.value
  } else if (buyNow.value) {
    params.buyNow = buyNow.value
  }
  return params
}

const loadPreview = async () => {
  loading.value = true
  try {
    const res = await previewOrder(buildParams())
    const data = res.data || {}
    preview.items = data.items || []
    preview.address = data.address || null
    preview.totalAmount = data.totalAmount || 0
    preview.freightAmount = data.freightAmount || 0
    preview.discountAmount = data.discountAmount || 0
    preview.payAmount = data.payAmount || 0
    preview.availableCoupons = data.availableCoupons || []
    if (data.userCouponId) {
      userCouponId.value = data.userCouponId
    }
  } catch (e) {
    preview.items = []
  } finally {
    loading.value = false
  }
}

const loadAddress = async () => {
  try {
    const res = await getAddressList()
    addressList.value = res.data || []
    if (addressList.value.length) {
      const current = addressList.value.find((item) => item.id === addressId.value)
      if (!current) {
        const defaultOne = addressList.value.find((item) => item.isDefault === 1)
        addressId.value = (defaultOne || addressList.value[0]).id
      }
    } else {
      addressId.value = null
    }
  } catch (e) {
    addressList.value = []
  }
}

const handleSelectAddress = (item) => {
  addressId.value = item.id
}

const handleSelectCoupon = async (id) => {
  userCouponId.value = id
  await loadPreview()
}

const openAddressDialog = (item) => {
  if (item) {
    Object.assign(addressForm, {
      id: item.id,
      receiver: item.receiver,
      phone: item.phone,
      province: item.province,
      city: item.city,
      district: item.district,
      detail: item.detail,
      postCode: item.postCode || '',
      isDefault: item.isDefault || 0
    })
  } else {
    Object.assign(addressForm, {
      id: null,
      receiver: '',
      phone: '',
      province: '',
      city: '',
      district: '',
      detail: '',
      postCode: '',
      isDefault: 0
    })
  }
  addressDialogVisible.value = true
}

const handleSaveAddress = async () => {
  await addressFormRef.value.validate()
  addressSaving.value = true
  try {
    const payload = { ...addressForm }
    delete payload.id
    if (addressForm.id) {
      await updateAddress(addressForm.id, payload)
      ElMessage.success('地址修改成功')
    } else {
      await addAddress(payload)
      ElMessage.success('地址添加成功')
    }
    addressDialogVisible.value = false
    await loadAddress()
  } finally {
    addressSaving.value = false
  }
}

const handleDeleteAddress = async (item) => {
  await ElMessageBox.confirm('确定要删除该收货地址吗？', '提示', { type: 'warning' })
  await deleteAddress(item.id)
  ElMessage.success('删除成功')
  if (addressId.value === item.id) {
    addressId.value = null
  }
  await loadAddress()
}

/** 提交订单 → 模拟支付 */
const handleSubmit = async () => {
  if (!addressId.value) {
    ElMessage.warning('请先选择收货地址')
    return
  }
  if (!preview.items.length) {
    ElMessage.warning('没有可结算的商品')
    return
  }
  submitting.value = true
  try {
    const payload = {
      addressId: addressId.value,
      userCouponId: userCouponId.value || null,
      remark: remark.value
    }
    if (cartIds.value.length) {
      payload.cartIds = cartIds.value
    } else if (buyNow.value) {
      payload.buyNow = buyNow.value
    }
    const res = await submitOrder(payload)
    const orderNo = res.data.orderNo
    cartStore.refresh()
    try {
      await ElMessageBox.confirm('订单提交成功，是否立即支付？', '支付确认', {
        confirmButtonText: '立即支付',
        cancelButtonText: '稍后支付',
        type: 'success'
      })
      await payOrder(orderNo)
      ElMessage.success('支付成功')
    } catch (e) {
      /* 取消支付：仍跳转订单详情（待付款） */
    }
    router.replace(`/order/${orderNo}`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  parseQuery()
  if (!cartIds.value.length && !buyNow.value) {
    ElMessage.warning('缺少结算参数，请重新选择商品')
    router.replace('/cart')
    return
  }
  await loadAddress()
  await loadPreview()
})
</script>

<style scoped>
.checkout-page {
  padding-top: 16px;
}

.block-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f2f6fc;
  margin-bottom: 14px;
}

.block-title {
  font-size: 16px;
  font-weight: 700;
}

.block-tip {
  font-size: 13px;
  color: #909399;
}

.block-head .el-button {
  margin-left: auto;
}

.address-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.address-card {
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.address-card:hover {
  border-color: #ffb08a;
}

.address-card.active {
  border-color: #ff5000;
  background: #fff7f3;
  box-shadow: 0 2px 10px rgba(255, 80, 0, 0.1);
}

.address-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.address-name {
  font-size: 15px;
  font-weight: 600;
}

.address-phone {
  color: #606266;
  font-size: 13px;
}

.address-detail {
  font-size: 13px;
  color: #606266;
  margin-top: 8px;
  line-height: 20px;
  min-height: 40px;
}

.address-op {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 6px;
}

.item-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-image {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
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
}

.item-spec {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.coupon-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.coupon-item {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.coupon-item:hover {
  border-color: #ffb08a;
}

.coupon-item.active {
  border-color: #ff5000;
  background: #fff7f3;
}

.coupon-value {
  color: #ff5000;
  font-size: 18px;
  font-weight: 700;
}

.coupon-name {
  font-size: 13px;
  margin-top: 4px;
}

.coupon-threshold {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.remark-input {
  max-width: 720px;
}

.amount-box {
  margin-top: 18px;
  margin-left: auto;
  width: 320px;
  font-size: 14px;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  color: #606266;
}

.amount-row.total {
  border-top: 1px solid #f2f6fc;
  margin-top: 6px;
  padding-top: 10px;
  font-size: 15px;
  font-weight: 600;
}

.amount-row.total .text-price {
  font-size: 22px;
}

.submit-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
}
</style>
