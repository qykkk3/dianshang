import { request } from './request'

/* ============================ 购物车 ============================ */

/** 购物车列表 */
export function getCartList() {
  return request.get('/cart/list')
}

/** 购物车条目数（角标） */
export function getCartCount() {
  return request.get('/cart/count')
}

/** 加入购物车 */
export function addCart(data) {
  return request.post('/cart', data)
}

/** 修改数量/选中状态 */
export function updateCart(id, data) {
  return request.put(`/cart/${id}`, data)
}

/** 批量选中/取消选中：ids 为空表示全部 */
export function updateCartChecked(data) {
  return request.put('/cart/checked', data)
}

/** 删除购物车条目 */
export function deleteCart(id) {
  return request.delete(`/cart/${id}`)
}

/** 批量删除 */
export function deleteCartBatch(ids) {
  return request.delete('/cart/batch', { data: { ids } })
}

/* ============================ 收货地址 ============================ */

/** 地址列表 */
export function getAddressList() {
  return request.get('/address/list')
}

/** 地址详情 */
export function getAddress(id) {
  return request.get(`/address/${id}`)
}

/** 新增地址 */
export function addAddress(data) {
  return request.post('/address', data)
}

/** 修改地址 */
export function updateAddress(id, data) {
  return request.put(`/address/${id}`, data)
}

/** 删除地址 */
export function deleteAddress(id) {
  return request.delete(`/address/${id}`)
}

/** 设为默认地址 */
export function setDefaultAddress(id) {
  return request.put(`/address/${id}/default`)
}
