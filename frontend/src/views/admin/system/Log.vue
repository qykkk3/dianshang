<template>
  <div>
    <div class="search-bar">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="操作描述 / 操作人 / 请求地址" clearable style="width: 240px" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="query.module" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="admin-page-header">
        <div class="admin-page-title">操作日志</div>
        <div>
          <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="!selectedIds.length"
            v-perm="'sys:log:delete'"
            @click="handleBatchDelete"
          >
            批量删除（{{ selectedIds.length }}）
          </el-button>
          <el-button type="danger" icon="Delete" v-perm="'sys:log:delete'" @click="openCleanDialog">
            清理日志
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="module" label="模块" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作描述" width="140" />
        <el-table-column prop="method" label="请求方法" min-width="200" show-overflow-tooltip />
        <el-table-column prop="requestUri" label="请求地址" min-width="200" show-overflow-tooltip />
        <el-table-column label="耗时" width="90" align="center">
          <template #default="{ row }">{{ row.costTime }} ms</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
    </div>

    <el-drawer v-model="detailVisible" title="日志详情" size="620px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="操作人">{{ detail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模块 / 操作">{{ detail.module }} / {{ detail.operation }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="请求地址">{{ detail.requestUri }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.ip }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costTime }} ms</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="detail.status === 1 ? 'success' : 'danger'">
            {{ detail.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="json-block">{{ prettyJson(detail.params) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="异常信息">
          <pre class="json-block error">{{ detail.errorMsg }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="cleanVisible" title="清理操作日志" width="440px" append-to-body>
      <el-form label-width="120px">
        <el-form-item label="清理范围">
          <el-radio-group v-model="cleanMode">
            <el-radio value="30">清理 30 天前</el-radio>
            <el-radio value="7">清理 7 天前</el-radio>
            <el-radio value="0">清空全部</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <el-alert
        v-if="cleanMode === '0'"
        title="将删除全部操作日志，且不可恢复"
        type="error"
        :closable="false"
      />
      <template #footer>
        <el-button @click="cleanVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="handleClean">确认清理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import { cleanLogs, deleteLogs, getLogPage } from '@/api/admin/system'
import { formatDateTime } from '@/utils/format'

const modules = ['商品管理', '库存管理', '订单管理', '售后管理', '营销管理', '评价管理', '会员管理', '系统管理']

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const timeRange = ref([])
const selectedIds = ref([])
const query = reactive({ keyword: '', module: null, status: null, startTime: null, endTime: null, page: 1, size: 10 })

const detailVisible = ref(false)
const detail = ref(null)
const cleanVisible = ref(false)
const cleanMode = ref('30')

const loadData = async () => {
  loading.value = true
  try {
    query.startTime = timeRange.value && timeRange.value[0] ? `${timeRange.value[0]} 00:00:00` : null
    query.endTime = timeRange.value && timeRange.value[1] ? `${timeRange.value[1]} 23:59:59` : null
    const res = await getLogPage({ ...query })
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

const handleResetQuery = () => {
  Object.assign(query, { keyword: '', module: null, status: null, page: 1 })
  timeRange.value = []
  loadData()
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((row) => row.id)
}

const openDetail = (row) => {
  detail.value = row
  detailVisible.value = true
}

const prettyJson = (text) => {
  if (!text) return '无'
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch (e) {
    return text
  }
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条日志吗？`, '批量删除', { type: 'warning' })
  await deleteLogs(selectedIds.value)
  ElMessage.success('删除成功')
  loadData()
}

const openCleanDialog = () => {
  cleanMode.value = '30'
  cleanVisible.value = true
}

const handleClean = async () => {
  saving.value = true
  try {
    await cleanLogs(Number(cleanMode.value))
    ElMessage.success('日志清理完成')
    cleanVisible.value = false
    query.page = 1
    loadData()
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.json-block {
  margin: 0;
  max-height: 260px;
  overflow: auto;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: Consolas, Monaco, monospace;
}

.json-block.error {
  color: #f56c6c;
}
</style>
