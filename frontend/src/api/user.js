import { request } from './request'

/* ======================= 个人中心（需买家登录） ======================= */

/** 个人资料 */
export function getProfile() {
  return request.get('/user/profile')
}

/** 修改个人资料 */
export function updateProfile(data) {
  return request.put('/user/profile', data)
}

/** 修改密码 */
export function updatePassword(data) {
  return request.put('/user/password', data)
}

/* ------------------------------ 收藏 ------------------------------ */

/** 我的收藏分页 */
export function getFavoritePage(params) {
  return request.get('/user/favorites', { params })
}

/** 收藏/取消收藏（返回布尔值表示收藏后的状态） */
export function toggleFavorite(productId) {
  return request.post(`/user/favorites/${productId}`)
}

/** 取消收藏 */
export function deleteFavorite(productId) {
  return request.delete(`/user/favorites/${productId}`)
}

/* ---------------------------- 浏览记录 ---------------------------- */

/** 浏览记录分页 */
export function getHistoryPage(params) {
  return request.get('/user/history', { params })
}

/** 记录一次浏览 */
export function recordHistory(productId) {
  return request.post(`/user/history/${productId}`)
}

/** 清空浏览记录 */
export function clearHistory() {
  return request.delete('/user/history')
}

/* ---------------------------- 我的优惠券 --------------------------- */

/** 我的优惠券分页：{ status, page, size } */
export function getMyCouponPage(params) {
  return request.get('/user/coupons', { params })
}

/** 领取优惠券 */
export function receiveCoupon(couponId) {
  return request.post(`/user/coupons/${couponId}/receive`)
}

/* ------------------------------ 评价 ------------------------------ */

/** 发表评价：{ orderItemId, star, content, images, isAnonymous } */
export function addComment(data) {
  return request.post('/comment', data)
}

/** 我的评价分页 */
export function getMyCommentPage(params) {
  return request.get('/comment/my', { params })
}

/** 待评价订单项分页 */
export function getPendingCommentPage(params) {
  return request.get('/comment/pending', { params })
}
