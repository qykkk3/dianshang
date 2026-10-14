<template>
  <div class="dashboard-page">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="12" :md="8" :lg="6" :xl="4">
        <el-card class="stat-card" shadow="hover" v-loading="overviewLoading">
          <div class="stat-body">
            <div class="stat-icon" :style="{ background: card.color }">
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 销售趋势 -->
    <el-card class="mt-16" shadow="never">
      <template #header>
        <div class="flex-between">
          <span class="chart-title">销售趋势</span>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div v-loading="trendLoading" class="chart-box">
        <div ref="trendChartRef" class="chart-canvas"></div>
        <el-empty v-if="!trendLoading && !trendList.length" class="chart-empty" description="暂无销售趋势数据" />
      </div>
    </el-card>

    <el-row :gutter="16" class="mt-16">
      <!-- 分类销售占比 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header><span class="chart-title">分类销售占比</span></template>
          <div v-loading="categoryLoading" class="chart-box">
            <div ref="categoryChartRef" class="chart-canvas"></div>
            <el-empty v-if="!categoryLoading && !categoryList.length" class="chart-empty" description="暂无分类销售数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 订单状态分布 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header><span class="chart-title">订单状态分布</span></template>
          <div v-loading="statusLoading" class="chart-box">
            <div ref="statusChartRef" class="chart-canvas"></div>
            <el-empty v-if="!statusLoading && !statusList.length" class="chart-empty" description="暂无订单数据" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-16">
      <!-- 热销商品 TOP10 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header><span class="chart-title">热销商品 TOP10</span></template>
          <div v-loading="topLoading" class="chart-box">
            <div ref="topChartRef" class="chart-canvas"></div>
            <el-empty v-if="!topLoading && !topList.length" class="chart-empty" description="暂无热销商品数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 热销商品明细表 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header><span class="chart-title">热销商品明细</span></template>
          <el-table :data="topList" v-loading="topLoading" size="small" height="320" empty-text="暂无数据">
            <el-table-column type="index" label="排名" width="60" align="center" />
            <el-table-column prop="productName" label="商品名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="saleCount" label="销量" width="80" align="center" />
            <el-table-column label="销售额" width="110" align="right">
              <template #default="{ row }">￥{{ formatAmount(row.totalAmount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 管理后台数据看板
 *  - 统计卡片：getOverview()
 *  - 销售趋势：getSalesTrend(days)（支持 7/30 天切换）
 *  - 分类销售占比：getCategorySales()
 *  - 热销商品排行：getTopProducts(10)
 *  - 订单状态分布：getOrderStatusStat()
 */
import { computed, nextTick, onActivated, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getCategorySales, getOrderStatusStat, getOverview, getSalesTrend, getTopProducts } from '@/api/admin/dashboard'
import { formatAmount, orderStatusLabel } from '@/utils/format'

/* ============================ 状态数据 ============================ */
const overview = ref({})
const overviewLoading = ref(false)

const trendDays = ref(7)
const trendList = ref([])
const trendLoading = ref(false)

const categoryList = ref([])
const categoryLoading = ref(false)

const topList = ref([])
const topLoading = ref(false)

const statusList = ref([])
const statusLoading = ref(false)

/* ============================ 统计卡片 ============================ */
const statCards = computed(() => {
  const data = overview.value || {}
  return [
    { key: 'todaySales', label: '今日销售额', value: `￥${formatAmount(data.todaySales)}`, icon: 'Money', color: '#ff5000' },
    { key: 'todayOrders', label: '今日订单数', value: data.todayOrders || 0, icon: 'List', color: '#409eff' },
    { key: 'todayNewUsers', label: '今日新增用户', value: data.todayNewUsers || 0, icon: 'UserFilled', color: '#67c23a' },
    { key: 'totalSales', label: '累计销售额', value: `￥${formatAmount(data.totalSales)}`, icon: 'TrendCharts', color: '#e6a23c' },
    { key: 'pendingShipOrders', label: '待发货订单', value: data.pendingShipOrders || 0, icon: 'Van', color: '#909399' },
    { key: 'pendingRefunds', label: '待处理售后', value: data.pendingRefunds || 0, icon: 'RefreshLeft', color: '#f56c6c' },
    { key: 'stockWarnings', label: '库存预警', value: data.stockWarnings || 0, icon: 'Warning', color: '#f56c6c' },
    { key: 'totalUsers', label: '会员总数', value: data.totalUsers || 0, icon: 'User', color: '#409eff' },
    { key: 'totalProducts', label: '在售商品数', value: data.totalProducts || 0, icon: 'Goods', color: '#67c23a' }
  ]
})

/* ============================ 图表实例 ============================ */
const trendChartRef = ref(null)
const categoryChartRef = ref(null)
const statusChartRef = ref(null)
const topChartRef = ref(null)
const charts = {
  trend: null,
  category: null,
  status: null,
  top: null
}

/** 安全初始化图表（容器不存在或已销毁时跳过） */
const initChart = (key, el) => {
  if (!el) return null
  if (charts[key]) {
    charts[key].dispose()
    charts[key] = null
  }
  charts[key] = echarts.init(el)
  return charts[key]
}

/** 统一渲染销售趋势 */
const renderTrend = () => {
  const chart = charts.trend
  if (!chart) return
  const dates = trendList.value.map((item) => item.saleDate)
  const amounts = trendList.value.map((item) => Number(item.totalAmount || 0))
  const orders = trendList.value.map((item) => Number(item.orderCount || 0))
  chart.setOption(
    {
      tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
      legend: { data: ['销售额', '订单数'], right: 10, top: 0 },
      grid: { left: 60, right: 60, top: 44, bottom: 40 },
      xAxis: { type: 'category', boundaryGap: false, data: dates, axisLabel: { color: '#909399' } },
      yAxis: [
        { type: 'value', name: '销售额(元)', axisLabel: { color: '#909399' }, splitLine: { lineStyle: { type: 'dashed' } } },
        { type: 'value', name: '订单数', axisLabel: { color: '#909399' }, splitLine: { show: false } }
      ],
      series: [
        {
          name: '销售额',
          type: 'line',
          smooth: true,
          symbolSize: 6,
          data: amounts,
          itemStyle: { color: '#ff5000' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(255,80,0,0.28)' },
              { offset: 1, color: 'rgba(255,80,0,0.02)' }
            ])
          }
        },
        { name: '订单数', type: 'bar', yAxisIndex: 1, barWidth: 14, data: orders, itemStyle: { color: '#409eff' } }
      ]
    },
    true
  )
}

/** 统一渲染分类销售占比 */
const renderCategory = () => {
  const chart = charts.category
  if (!chart) return
  chart.setOption(
    {
      tooltip: {
        trigger: 'item',
        formatter: (params) => `${params.name}<br/>销售额：￥${formatAmount(params.value)}（${params.percent}%）`
      },
      legend: { type: 'scroll', orient: 'vertical', right: 10, top: 'center' },
      color: ['#ff5000', '#409eff', '#67c23a', '#e6a23c', '#909399', '#9254de', '#36cfc9', '#f759ab'],
      series: [
        {
          name: '分类销售额',
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['38%', '52%'],
          avoidLabelOverlap: true,
          label: { formatter: '{b}\n{d}%', color: '#606266' },
          data: categoryList.value.map((item) => ({ name: item.categoryName, value: Number(item.totalAmount || 0) }))
        }
      ]
    },
    true
  )
}

/** 统一渲染订单状态分布 */
const renderStatus = () => {
  const chart = charts.status
  if (!chart) return
  chart.setOption(
    {
      tooltip: { trigger: 'item', formatter: '{b}<br/>订单数：{c}（{d}%）' },
      legend: { bottom: 0, type: 'scroll' },
      color: ['#e6a23c', '#409eff', '#36cfc9', '#67c23a', '#909399', '#f56c6c'],
      series: [
        {
          name: '订单状态',
          type: 'pie',
          radius: ['38%', '62%'],
          center: ['50%', '45%'],
          label: { formatter: '{b}: {c}', color: '#606266' },
          data: statusList.value.map((item) => ({
            name: item.statusName || orderStatusLabel(item.status),
            value: Number(item.orderCount || 0)
          }))
        }
      ]
    },
    true
  )
}

/** 统一渲染热销商品 TOP10 */
const renderTop = () => {
  const chart = charts.top
  if (!chart) return
  // 条形图从下往上展示，因此数据倒序
  const list = [...topList.value].reverse()
  chart.setOption(
    {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params) => {
          const item = params[0]
          return `${item.name}<br/>销量：${item.value}`
        }
      },
      grid: { left: 10, right: 40, top: 16, bottom: 10, containLabel: true },
      xAxis: { type: 'value', axisLabel: { color: '#909399' }, splitLine: { lineStyle: { type: 'dashed' } } },
      yAxis: {
        type: 'category',
        data: list.map((item) => item.productName),
        axisLabel: { color: '#606266', width: 130, overflow: 'truncate' }
      },
      series: [
        {
          name: '销量',
          type: 'bar',
          barWidth: 14,
          data: list.map((item) => Number(item.saleCount || 0)),
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#ffa940' },
              { offset: 1, color: '#ff5000' }
            ])
          },
          label: { show: true, position: 'right', color: '#909399' }
        }
      ]
    },
    true
  )
}

/* ============================ 数据加载 ============================ */
const loadOverview = async () => {
  overviewLoading.value = true
  try {
    const res = await getOverview()
    overview.value = res.data || {}
  } catch (e) {
    overview.value = {}
  } finally {
    overviewLoading.value = false
  }
}

const loadTrend = async () => {
  trendLoading.value = true
  try {
    const res = await getSalesTrend(trendDays.value)
    trendList.value = res.data || []
  } catch (e) {
    trendList.value = []
  } finally {
    trendLoading.value = false
    await nextTick()
    renderTrend()
  }
}

const loadCategory = async () => {
  categoryLoading.value = true
  try {
    const res = await getCategorySales()
    categoryList.value = res.data || []
  } catch (e) {
    categoryList.value = []
  } finally {
    categoryLoading.value = false
    await nextTick()
    renderCategory()
  }
}

const loadTop = async () => {
  topLoading.value = true
  try {
    const res = await getTopProducts(10)
    topList.value = res.data || []
  } catch (e) {
    topList.value = []
  } finally {
    topLoading.value = false
    await nextTick()
    renderTop()
  }
}

const loadStatus = async () => {
  statusLoading.value = true
  try {
    const res = await getOrderStatusStat()
    statusList.value = res.data || []
  } catch (e) {
    statusList.value = []
  } finally {
    statusLoading.value = false
    await nextTick()
    renderStatus()
  }
}

/** 加载全部看板数据 */
const loadAll = () => {
  loadOverview()
  loadTrend()
  loadCategory()
  loadTop()
  loadStatus()
}

/* ============================ 生命周期 ============================ */
const handleResize = () => {
  Object.values(charts).forEach((chart) => chart && chart.resize())
}

onMounted(async () => {
  await nextTick()
  // 先建实例，保证 el-empty 遮罩出现时图表实例仍然存在（重新有数据时可直接 setOption）
  initChart('trend', trendChartRef.value)
  initChart('category', categoryChartRef.value)
  initChart('status', statusChartRef.value)
  initChart('top', topChartRef.value)
  window.addEventListener('resize', handleResize)
  loadAll()
})

// 首次挂载时 onActivated 会紧随 onMounted 触发，跳过以免重复请求
let activatedOnce = false

onActivated(() => {
  if (!activatedOnce) {
    activatedOnce = true
    return
  }
  // 被 keep-alive 缓存后再次进入时重新拉取，保证数据新鲜
  loadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  Object.keys(charts).forEach((key) => {
    if (charts[key]) {
      charts[key].dispose()
      charts[key] = null
    }
  })
})
</script>

<style scoped>
.stat-card {
  margin-bottom: 16px;
  border: none;
}

.stat-body {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-info {
  overflow: hidden;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-label {
  margin-top: 2px;
  font-size: 13px;
  color: #909399;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chart-box {
  position: relative;
  height: 320px;
}

.chart-canvas {
  width: 100%;
  height: 320px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
}
</style>
