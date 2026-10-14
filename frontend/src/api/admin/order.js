import { adminRequest, downloadBlob } from '../request'

/* ============================ 订单管理 ============================ */

/** 订单分页：{ orderNo, status, userId, startTime, endTime, page, size } */
export function getAdminOrderPage(params) {
  return adminRequest.get('/admin/orders', { params })
}

/** 订单详情（按主键 id） */
export function getAdminOrderDetail(id) {
  return adminRequest.get(`/admin/orders/${id}`)
}

/** 订单发货：{ expressCompany, expressNo } */
export function shipOrder(id, data) {
  return adminRequest.put(`/admin/orders/${id}/ship`, data)
}

/** 追加物流轨迹：{ status, context, location } */
export function addOrderTrack(id, data) {
  return adminRequest.post(`/admin/orders/${id}/track`, data)
}

/** 商家备注 */
export function remarkOrder(id, adminRemark) {
  return adminRequest.put(`/admin/orders/${id}/remark`, { adminRemark })
}

/** 订单改价：{ payAmount } */
export function priceOrder(id, payAmount) {
  return adminRequest.put(`/admin/orders/${id}/price`, { payAmount })
}

/** 关闭订单 */
export function closeOrder(id) {
  return adminRequest.put(`/admin/orders/${id}/close`)
}

/** 各状态订单数量 */
export function getOrderStatistics() {
  return adminRequest.get('/admin/orders/statistics')
}

/** 导出订单 Excel */
export async function exportOrders(params) {
  const response = await adminRequest.get('/admin/orders/export', { params, responseType: 'blob' })
  downloadBlob(response, '订单列表.xlsx')
}

/* ============================ 售后管理 ============================ */

/** 售后分页：{ keyword, status, page, size } */
export function getAdminRefundPage(params) {
  return adminRequest.get('/admin/refunds', { params })
}

/** 售后详情 */
export function getAdminRefundDetail(id) {
  return adminRequest.get(`/admin/refunds/${id}`)
}

/** 售后审核：{ status, auditRemark, refundAmount } */
export function auditRefund(id, data) {
  return adminRequest.put(`/admin/refunds/${id}/audit`, data)
}
