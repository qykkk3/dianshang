/**
 * 通用格式化与常量工具
 */

/** 金额格式化：12.5 -> ￥12.50 */
export function formatPrice(value) {
  const num = Number(value || 0)
  return `￥${num.toFixed(2)}`
}

/** 金额格式化（不带符号） */
export function formatAmount(value) {
  return Number(value || 0).toFixed(2)
}

/** 日期时间格式化，兼容 "2026-10-13 10:00:00" 与时间戳 */
export function formatDateTime(value) {
  if (!value) return '-'
  if (typeof value === 'string' && value.length >= 19) {
    return value.substring(0, 19)
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(
    date.getMinutes()
  )}:${pad(date.getSeconds())}`
}

/** 仅日期 */
export function formatDate(value) {
  if (!value) return '-'
  return String(value).substring(0, 10)
}

/** 逗号分隔字符串 -> 数组 */
export function splitImages(value) {
  if (!value) return []
  if (Array.isArray(value)) return value
  return String(value)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

/** 订单状态映射 */
export const ORDER_STATUS = {
  0: { label: '待付款', type: 'danger' },
  1: { label: '待发货', type: 'warning' },
  2: { label: '待收货', type: 'primary' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已取消', type: 'info' },
  5: { label: '售后中', type: 'danger' }
}

/** 售后状态映射 */
export const REFUND_STATUS = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已同意', type: 'primary' },
  2: { label: '已拒绝', type: 'danger' },
  3: { label: '待买家退货', type: 'warning' },
  4: { label: '退款完成', type: 'success' },
  5: { label: '已撤销', type: 'info' }
}

/** 售后类型映射 */
export const REFUND_TYPE = {
  1: '仅退款',
  2: '退货退款',
  3: '换货'
}

/** 优惠券状态映射 */
export const COUPON_STATUS = {
  0: { label: '未使用', type: 'success' },
  1: { label: '已使用', type: 'info' },
  2: { label: '已过期', type: 'info' }
}

/** 订单状态标签类型 */
export function orderStatusType(status) {
  return (ORDER_STATUS[status] || {}).type || 'info'
}

/** 订单状态名称 */
export function orderStatusLabel(status) {
  return (ORDER_STATUS[status] || {}).label || '未知'
}

/** 售后状态名称 */
export function refundStatusLabel(status) {
  return (REFUND_STATUS[status] || {}).label || '未知'
}

/** 图片兜底：后端不可用时展示占位图 */
export function fallbackImage(event) {
  const target = event.target
  target.src =
    'data:image/svg+xml;charset=utf-8,' +
    encodeURIComponent(
      '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect width="200" height="200" fill="#f0f2f5"/><text x="100" y="105" font-size="16" fill="#c0c4cc" text-anchor="middle">暂无图片</text></svg>'
    )
}
