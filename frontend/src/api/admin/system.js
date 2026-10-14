import { adminRequest } from '../request'

/* ============================ 管理员 ============================ */

/** 管理员分页：{ keyword, status, roleId, page, size } */
export function getSysUserPage(params) {
  return adminRequest.get('/admin/sys/users', { params })
}

/** 管理员详情 */
export function getSysUser(id) {
  return adminRequest.get(`/admin/sys/users/${id}`)
}

/** 新增管理员 */
export function addSysUser(data) {
  return adminRequest.post('/admin/sys/users', data)
}

/** 修改管理员 */
export function updateSysUser(id, data) {
  return adminRequest.put(`/admin/sys/users/${id}`, data)
}

/** 删除管理员 */
export function deleteSysUser(id) {
  return adminRequest.delete(`/admin/sys/users/${id}`)
}

/** 启用/禁用管理员 */
export function updateSysUserStatus(id, status) {
  return adminRequest.put(`/admin/sys/users/${id}/status`, null, { params: { status } })
}

/** 重置管理员密码 */
export function resetSysUserPassword(id, password) {
  return adminRequest.put(`/admin/sys/users/${id}/password`, { password })
}

/* ============================ 角色权限 ============================ */

/** 角色列表（含 permissionIds） */
export function getRoleList() {
  return adminRequest.get('/admin/sys/roles')
}

/** 角色详情 */
export function getRole(id) {
  return adminRequest.get(`/admin/sys/roles/${id}`)
}

/** 新增角色 */
export function addRole(data) {
  return adminRequest.post('/admin/sys/roles', data)
}

/** 修改角色 */
export function updateRole(id, data) {
  return adminRequest.put(`/admin/sys/roles/${id}`, data)
}

/** 删除角色 */
export function deleteRole(id) {
  return adminRequest.delete(`/admin/sys/roles/${id}`)
}

/** 分配权限 */
export function assignRolePermissions(id, permissionIds) {
  return adminRequest.put(`/admin/sys/roles/${id}/permissions`, { permissionIds })
}

/** 启用/禁用角色 */
export function updateRoleStatus(id, status) {
  return adminRequest.put(`/admin/sys/roles/${id}/status`, null, { params: { status } })
}

/** 完整权限树 */
export function getPermissionTree() {
  return adminRequest.get('/admin/sys/permissions')
}

/* ============================ 系统配置 ============================ */

/** 配置列表 */
export function getConfigList() {
  return adminRequest.get('/admin/sys/configs')
}

/** 批量保存配置 */
export function updateConfigs(list) {
  return adminRequest.put('/admin/sys/configs', list)
}

/* ============================ 操作日志 ============================ */

/** 日志分页：{ keyword, module, status, startTime, endTime, page, size } */
export function getLogPage(params) {
  return adminRequest.get('/admin/sys/logs', { params })
}

/** 清理日志：days=0 表示清空全部 */
export function cleanLogs(days = 30) {
  return adminRequest.delete('/admin/sys/logs', { params: { days } })
}

/** 批量删除日志 */
export function deleteLogs(ids) {
  return adminRequest.delete('/admin/sys/logs/batch', { data: ids })
}
