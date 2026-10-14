<template>
  <div class="page-card">
    <div class="admin-page-header">
      <div class="admin-page-title">收货地址</div>
      <el-button type="primary" icon="Plus" @click="openDialog()">新增地址</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />

    <template v-else>
      <div v-if="list.length" class="address-list">
        <div v-for="item in list" :key="item.id" class="address-card" :class="{ default: item.isDefault === 1 }">
          <div class="address-head">
            <span class="receiver">{{ item.receiver }}</span>
            <span class="phone">{{ item.phone }}</span>
            <el-tag v-if="item.isDefault === 1" type="danger" size="small" effect="dark">默认</el-tag>
          </div>
          <div class="address-detail">{{ item.fullAddress || `${item.province}${item.city}${item.district}${item.detail}` }}</div>
          <div class="address-actions">
            <el-button v-if="item.isDefault !== 1" link type="primary" @click="handleSetDefault(item)">设为默认</el-button>
            <el-button link type="primary" @click="openDialog(item)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(item)">删除</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="还没有收货地址，快去添加一个吧" />
    </template>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑地址' : '新增地址'" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="收货人" prop="receiver">
          <el-input v-model="form.receiver" placeholder="请输入收货人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="form.province" placeholder="如：广东省" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="如：深圳市" />
        </el-form-item>
        <el-form-item label="区/县" prop="district">
          <el-input v-model="form.district" placeholder="如：南山区" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="form.detail" type="textarea" :rows="2" maxlength="100" placeholder="街道、门牌号等" />
        </el-form-item>
        <el-form-item label="邮编">
          <el-input v-model="form.postCode" placeholder="选填" maxlength="6" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addAddress, deleteAddress, getAddressList, setDefaultAddress, updateAddress } from '@/api/cart'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const form = reactive({
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

const rules = {
  receiver: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区/县', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAddressList()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openDialog = (item) => {
  if (item) {
    Object.assign(form, item)
  } else {
    Object.assign(form, {
      id: null,
      receiver: '',
      phone: '',
      province: '',
      city: '',
      district: '',
      detail: '',
      postCode: '',
      isDefault: list.value.length === 0 ? 1 : 0
    })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      await updateAddress(form.id, payload)
    } else {
      await addAddress(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleSetDefault = async (item) => {
  await setDefaultAddress(item.id)
  ElMessage.success('已设为默认地址')
  loadData()
}

const handleDelete = async (item) => {
  await ElMessageBox.confirm(`确定删除「${item.receiver}」的收货地址吗？`, '删除地址', { type: 'warning' })
  await deleteAddress(item.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.address-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.address-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 16px;
  transition: all 0.2s;
}

.address-card:hover {
  border-color: #ffb08a;
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.08);
}

.address-card.default {
  border-color: #ff5000;
  background: #fffaf7;
}

.address-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.receiver {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.phone {
  color: #606266;
}

.address-detail {
  margin: 10px 0 12px;
  font-size: 13px;
  color: #606266;
  line-height: 20px;
  min-height: 40px;
}

.address-actions {
  display: flex;
  justify-content: flex-end;
  border-top: 1px dashed #ebeef5;
  padding-top: 8px;
}
</style>
