<template>
  <div>
    <div class="search-bar">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="用户名 / 昵称 / 手机号" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="admin-page-header">
        <div class="admin-page-title">会员列表</div>
        <span class="text-muted">共 {{ total }} 位会员</span>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatar">{{ (row.nickname || 'U').substring(0, 1) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="nickname" label="昵称" width="130" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="性别" width="80" align="center">
          <template #default="{ row }">{{ ['未知', '男', '女'][row.gender] || '未知' }}</template>
        </el-table-column>
        <el-table-column label="订单数" width="90" align="center">
          <template #default="{ row }">{{ row.orderCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="消费总额" width="130" align="right">
          <template #default="{ row }">
            <span class="text-price">{{ formatPrice(row.totalConsumption) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="!canEdit"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <el-drawer v-model="drawerVisible" title="会员详情" size="640px">
      <template v-if="detail">
        <div class="member-head">
          <el-avatar :size="58" :src="detail.avatar">{{ (detail.nickname || 'U').substring(0, 1) }}</el-avatar>
          <div>
            <div class="member-name">{{ detail.nickname || detail.username }}</div>
            <div class="text-muted">账号：{{ detail.username }} ｜ 手机号：{{ detail.phone || '未填写' }}</div>
          </div>
        </div>

        <el-row :gutter="12" class="mb-16">
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ detail.orderCount || 0 }}</div>
              <div class="stat-label">订单数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value text-price">{{ formatAmount(detail.totalConsumption) }}</div>
              <div class="stat-label">消费总额</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ detail.addressCount || 0 }}</div>
              <div class="stat-label">收货地址</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ detail.favoriteCount || 0 }}</div>
              <div class="stat-label">收藏 / 评价 {{ detail.commentCount || 0 }}</div>
            </div>
          </el-col>
        </el-row>

        <el-descriptions :column="2" border class="mb-16">
          <el-descriptions-item label="邮箱">{{ detail.email || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="生日">{{ detail.birthday || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="detail.status === 1 ? 'success' : 'danger'">
              {{ detail.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="admin-page-title mb-16">最近订单</div>
        <el-table :data="detail.recentOrders || []" border size="small">
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }">{{ formatPrice(row.payAmount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="orderStatusType(row.status)">
                {{ row.statusName || orderStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import { getMemberDetail, getMemberPage, updateMemberStatus } from '@/api/admin/member'
import { formatAmount, formatDateTime, formatPrice, orderStatusLabel, orderStatusType } from '@/utils/format'
import { useAdminStore } from '@/store/admin'

const adminStore = useAdminStore()
const canEdit = computed(() => adminStore.hasPermission('member:status'))

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: null, page: 1, size: 10 })

const drawerVisible = ref(false)
const detail = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMemberPage({ ...query })
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.page = 1
  loadData()
}

const handleReset = () => {
  Object.assign(query, { keyword: '', status: null, page: 1 })
  loadData()
}

const handleStatusChange = async (row, val) => {
  const action = val === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(
    val === 0 ? `禁用后会员「${row.nickname || row.username}」将被强制下线并无法登录，确定吗？` : `确定${action}该会员吗？`,
    `${action}会员`,
    { type: 'warning' }
  )
  await updateMemberStatus(row.id, val)
  row.status = val
  ElMessage.success(`已${action}`)
}

const openDetail = async (row) => {
  const res = await getMemberDetail(row.id)
  detail.value = res.data
  drawerVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.member-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.member-name {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 6px;
}

.stat-card {
  background: #f7f8fa;
  border-radius: 8px;
  padding: 12px 8px;
  text-align: center;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
