<template>
  <div>
    <el-alert type="info" :closable="false" class="mb-16">
      <template #title>
        以下配置会影响前台展示与交易规则：包邮门槛、默认运费、库存预警阈值、未支付订单自动取消时间等。修改后立即生效。
      </template>
    </el-alert>

    <div class="page-card">
      <div class="admin-page-header">
        <div class="admin-page-title">系统配置</div>
        <div>
          <el-button v-if="!editing" type="primary" icon="Edit" v-perm="'sys:config:edit'" @click="startEdit">
            编辑配置
          </el-button>
          <template v-else>
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" icon="Check" :loading="saving" @click="handleSave">保存全部</el-button>
          </template>
        </div>
      </div>

      <el-skeleton v-if="loading" :rows="6" animated />

      <el-form v-else label-width="180px" class="config-form">
        <el-form-item v-for="item in list" :key="item.configKey" :label="item.configName">
          <div class="config-row">
            <el-input
              v-if="editing"
              v-model="item.configValue"
              :placeholder="item.configKey"
              style="max-width: 320px"
            />
            <span v-else class="config-value">{{ item.configValue }}</span>
            <span class="text-muted config-key">{{ item.configKey }}</span>
          </div>
          <div class="text-muted config-remark">{{ item.remark }}</div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigList, updateConfigs } from '@/api/admin/system'

const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const list = ref([])
let backup = []

/** 数字类配置项，保存前做非负校验 */
const NUMBER_KEYS = [
  'stock_warn_threshold',
  'freight_fee',
  'free_freight_amount',
  'order_auto_cancel_minutes',
  'order_auto_finish_days',
  'after_sale_days'
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await getConfigList()
    list.value = res.data || []
    backup = JSON.parse(JSON.stringify(list.value))
  } finally {
    loading.value = false
  }
}

const startEdit = () => {
  backup = JSON.parse(JSON.stringify(list.value))
  editing.value = true
}

const cancelEdit = () => {
  list.value = JSON.parse(JSON.stringify(backup))
  editing.value = false
}

const validate = () => {
  for (const item of list.value) {
    if (!item.configValue || !String(item.configValue).trim()) {
      ElMessage.warning(`配置「${item.configName}」不能为空`)
      return false
    }
    if (NUMBER_KEYS.includes(item.configKey)) {
      const num = Number(item.configValue)
      if (Number.isNaN(num) || num < 0) {
        ElMessage.warning(`配置「${item.configName}」必须为非负数字`)
        return false
      }
    }
  }
  return true
}

const handleSave = async () => {
  if (!validate()) return
  saving.value = true
  try {
    await updateConfigs(
      list.value.map((item) => ({
        configKey: item.configKey,
        configName: item.configName,
        configValue: String(item.configValue),
        remark: item.remark
      }))
    )
    ElMessage.success('配置已保存')
    editing.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.config-form :deep(.el-form-item) {
  margin-bottom: 22px;
  align-items: flex-start;
}

.config-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.config-value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.config-key {
  font-size: 12px;
  font-family: Consolas, Monaco, monospace;
}

.config-remark {
  font-size: 12px;
  line-height: 18px;
  margin-top: 4px;
}
</style>
