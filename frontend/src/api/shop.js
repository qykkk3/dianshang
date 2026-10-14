import { request } from './request'

/* ==================== 商城公开接口（无需登录） ==================== */

/** 分类树 */
export function getCategories() {
  return request.get('/shop/categories')
}

/** 商品分页查询：{ keyword, categoryId, minPrice, maxPrice, sort, page, size } */
export function getProductPage(params) {
  return request.get('/shop/products', { params })
}

/** 商品详情 */
export function getProductDetail(id) {
  return request.get(`/shop/products/${id}`)
}

/** 热销商品 */
export function getHotProducts(limit = 8) {
  return request.get('/shop/products/hot', { params: { limit } })
}

/** 新品 */
export function getNewProducts(limit = 8) {
  return request.get('/shop/products/new', { params: { limit } })
}

/** 推荐商品 */
export function getRecommendProducts(limit = 8) {
  return request.get('/shop/products/recommend', { params: { limit } })
}

/** 商品评价分页 */
export function getProductComments(productId, params) {
  return request.get(`/shop/products/${productId}/comments`, { params })
}

/** 商品评价统计 */
export function getCommentStats(productId) {
  return request.get(`/shop/products/${productId}/comment-stats`)
}

/** 轮播图：position 1首页 2分类页 */
export function getBanners(position = 1) {
  return request.get('/shop/banners', { params: { position } })
}

/** 公告分页（仅已发布） */
export function getNoticePage(params) {
  return request.get('/shop/notices', { params })
}

/** 公告详情 */
export function getNoticeDetail(id) {
  return request.get(`/shop/notices/${id}`)
}

/** 可领取优惠券列表 */
export function getAvailableCoupons() {
  return request.get('/shop/coupons')
}
