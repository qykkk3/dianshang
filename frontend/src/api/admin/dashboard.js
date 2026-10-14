import { adminRequest } from '../request'

/* ============================ 数据看板 ============================ */

/** 总览指标 */
export function getOverview() {
  return adminRequest.get('/admin/dashboard/overview')
}

/** 销售趋势（天） */
export function getSalesTrend(days = 7) {
  return adminRequest.get('/admin/dashboard/sales-trend', { params: { days } })
}

/** 分类销售占比 */
export function getCategorySales() {
  return adminRequest.get('/admin/dashboard/category-sales')
}

/** 热销商品排行 */
export function getTopProducts(limit = 10) {
  return adminRequest.get('/admin/dashboard/top-products', { params: { limit } })
}

/** 订单状态分布 */
export function getOrderStatusStat() {
  return adminRequest.get('/admin/dashboard/order-status')
}
