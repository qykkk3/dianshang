import { adminRequest, downloadBlob } from '../request'

/* ============================ 商品管理 ============================ */

/** 商品分页 */
export function getAdminProductPage(params) {
  return adminRequest.get('/admin/products', { params })
}

/** 商品详情（含规格与 SKU） */
export function getAdminProductDetail(id) {
  return adminRequest.get(`/admin/products/${id}`)
}

/** 新增商品 */
export function addProduct(data) {
  return adminRequest.post('/admin/products', data)
}

/** 修改商品 */
export function updateProduct(id, data) {
  return adminRequest.put(`/admin/products/${id}`, data)
}

/** 删除商品 */
export function deleteProduct(id) {
  return adminRequest.delete(`/admin/products/${id}`)
}

/** 上下架 */
export function updateProductStatus(id, status) {
  return adminRequest.put(`/admin/products/${id}/status`, null, { params: { status } })
}

/** 推荐开关 */
export function updateProductRecommend(id, isRecommend) {
  return adminRequest.put(`/admin/products/${id}/recommend`, null, { params: { isRecommend } })
}

/** 导出商品 Excel */
export async function exportProducts(params) {
  const response = await adminRequest.get('/admin/products/export', { params, responseType: 'blob' })
  downloadBlob(response, '商品列表.xlsx')
}

/** 下载导入模板 */
export async function downloadProductTemplate() {
  const response = await adminRequest.get('/admin/products/template', { responseType: 'blob' })
  downloadBlob(response, '商品导入模板.xlsx')
}

/** 批量导入商品 */
export function importProducts(file) {
  const formData = new FormData()
  formData.append('file', file)
  return adminRequest.post('/admin/products/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/* ============================ 分类管理 ============================ */

/** 分类树（含停用） */
export function getAdminCategoryTree() {
  return adminRequest.get('/admin/categories')
}

/** 新增分类 */
export function addCategory(data) {
  return adminRequest.post('/admin/categories', data)
}

/** 修改分类 */
export function updateCategory(id, data) {
  return adminRequest.put(`/admin/categories/${id}`, data)
}

/** 删除分类 */
export function deleteCategory(id) {
  return adminRequest.delete(`/admin/categories/${id}`)
}

/** 分类启用/停用 */
export function updateCategoryStatus(id, status) {
  return adminRequest.put(`/admin/categories/${id}/status`, null, { params: { status } })
}

/* ============================ 库存预警 ============================ */

/** 库存预警分页 */
export function getStockWarningPage(params) {
  return adminRequest.get('/admin/stock/warnings', { params })
}

/** 调整 SKU 库存 */
export function updateSkuStock(skuId, stock) {
  return adminRequest.put(`/admin/stock/${skuId}`, { stock })
}

/** 获取预警阈值 */
export function getStockConfig() {
  return adminRequest.get('/admin/stock/config')
}

/** 修改预警阈值 */
export function updateStockConfig(threshold) {
  return adminRequest.put('/admin/stock/config', { threshold })
}

/* ============================ 文件上传 ============================ */

/** 上传图片：type 为 product / banner / avatar */
export function uploadFile(file, type = 'product') {
  const formData = new FormData()
  formData.append('file', file)
  return adminRequest.post('/admin/upload', formData, {
    params: { type },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 删除已上传的图片 */
export function deleteUploadedFile(url) {
  return adminRequest.delete('/admin/upload', { params: { url } })
}
