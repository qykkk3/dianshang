import { request, adminRequest } from './request'

/* ============================ 买家端认证 ============================ */

/** 买家注册 */
export function register(data) {
  return request.post('/auth/register', data)
}

/** 买家登录 */
export function login(data) {
  return request.post('/auth/login', data)
}

/** 买家退出登录 */
export function logout() {
  return request.post('/auth/logout')
}

/** 获取当前买家信息 */
export function getBuyerInfo() {
  return request.get('/auth/me')
}

/* ============================ 管理端认证 ============================ */

/** 管理员登录 */
export function adminLogin(data) {
  return adminRequest.post('/admin/auth/login', data)
}

/** 管理员退出登录 */
export function adminLogout() {
  return adminRequest.post('/admin/auth/logout')
}

/** 获取当前管理员信息（含权限与菜单） */
export function getAdminInfo() {
  return adminRequest.get('/admin/auth/me')
}

/** 获取当前管理员菜单树 */
export function getAdminMenus() {
  return adminRequest.get('/admin/auth/menus')
}
