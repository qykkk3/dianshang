import { request } from './request'

/* ============================ 订单 ============================ */

/** 结算页预览：{ cartIds, buyNow:{skuId,quantity}, userCouponId } */
export function previewOrder(data) {
  return request.post('/order/preview', data)
}

/** 提交订单：{ addressId, cartIds, buyNow, userCouponId, remark } */
export function submitOrder(data) {
  return request.post('/order/submit', data)
}

/** 我的订单分页：{ status, orderNo, page, size } */
export function getOrderPage(params) {
  return request.get('/order/list', { params })
}

/** 我的各状态订单数量（个人中心角标） */
export function getOrderCounts() {
  return request.get('/order/counts')
}

/** 订单详情 */
export function getOrderDetail(orderNo) {
  return request.get(`/order/${orderNo}`)
}

/** 模拟支付 */
export function payOrder(orderNo) {
  return request.post(`/order/${orderNo}/pay`)
}

/** 取消订单 */
export function cancelOrder(orderNo) {
  return request.post(`/order/${orderNo}/cancel`)
}

/** 确认收货 */
export function confirmOrder(orderNo) {
  return request.post(`/order/${orderNo}/confirm`)
}

/** 删除订单 */
export function deleteOrder(orderNo) {
  return request.delete(`/order/${orderNo}`)
}

/** 物流轨迹 */
export function getOrderTrack(orderNo) {
  return request.get(`/order/${orderNo}/track`)
}

/* ============================ 售后 ============================ */

/** 申请售后 */
export function applyRefund(data) {
  return request.post('/refund/apply', data)
}

/** 我的售后列表 */
export function getRefundPage(params) {
  return request.get('/refund/list', { params })
}

/** 售后详情 */
export function getRefundDetail(id) {
  return request.get(`/refund/${id}`)
}

/** 撤销售后申请 */
export function cancelRefund(id) {
  return request.post(`/refund/${id}/cancel`)
}
