import { adminRequest } from '../request'

/* ============================ 优惠券 ============================ */

/** 优惠券分页：{ keyword, status, page, size } */
export function getCouponPage(params) {
  return adminRequest.get('/admin/coupons', { params })
}

/** 新增优惠券 */
export function addCoupon(data) {
  return adminRequest.post('/admin/coupons', data)
}

/** 修改优惠券 */
export function updateCoupon(id, data) {
  return adminRequest.put(`/admin/coupons/${id}`, data)
}

/** 删除优惠券 */
export function deleteCoupon(id) {
  return adminRequest.delete(`/admin/coupons/${id}`)
}

/** 启用/停用 */
export function updateCouponStatus(id, status) {
  return adminRequest.put(`/admin/coupons/${id}/status`, null, { params: { status } })
}

/** 领取记录 */
export function getCouponRecords(id, params) {
  return adminRequest.get(`/admin/coupons/${id}/records`, { params })
}

/* ============================ 轮播图 ============================ */

/** 轮播图分页：{ position, status, page, size } */
export function getBannerPage(params) {
  return adminRequest.get('/admin/banners', { params })
}

/** 新增轮播图 */
export function addBanner(data) {
  return adminRequest.post('/admin/banners', data)
}

/** 修改轮播图 */
export function updateBanner(id, data) {
  return adminRequest.put(`/admin/banners/${id}`, data)
}

/** 删除轮播图 */
export function deleteBanner(id) {
  return adminRequest.delete(`/admin/banners/${id}`)
}

/** 启用/停用 */
export function updateBannerStatus(id, status) {
  return adminRequest.put(`/admin/banners/${id}/status`, null, { params: { status } })
}

/* ============================ 公告 ============================ */

/** 公告分页：{ type, status, page, size } */
export function getNoticePage(params) {
  return adminRequest.get('/admin/notices', { params })
}

/** 新增公告 */
export function addNotice(data) {
  return adminRequest.post('/admin/notices', data)
}

/** 修改公告 */
export function updateNotice(id, data) {
  return adminRequest.put(`/admin/notices/${id}`, data)
}

/** 删除公告 */
export function deleteNotice(id) {
  return adminRequest.delete(`/admin/notices/${id}`)
}

/** 发布/下线公告 */
export function publishNotice(id, status) {
  return adminRequest.put(`/admin/notices/${id}/publish`, null, { params: { status } })
}

/* ============================ 商品评价 ============================ */

/** 评价分页：{ productId, userId, star, status, keyword, page, size } */
export function getCommentPage(params) {
  return adminRequest.get('/admin/comments', { params })
}

/** 回复评价 */
export function replyComment(id, replyContent) {
  return adminRequest.put(`/admin/comments/${id}/reply`, { replyContent })
}

/** 显示/隐藏评价 */
export function updateCommentStatus(id, status) {
  return adminRequest.put(`/admin/comments/${id}/status`, { status })
}

/** 删除评价 */
export function deleteComment(id) {
  return adminRequest.delete(`/admin/comments/${id}`)
}
