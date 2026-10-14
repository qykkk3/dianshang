import { adminRequest } from '../request'

/* ======================== 会员（买家）管理 ======================== */

/** 会员分页：{ keyword, status, page, size } */
export function getMemberPage(params) {
  return adminRequest.get('/admin/users', { params })
}

/** 会员详情（含下单数、消费总额、地址数等） */
export function getMemberDetail(id) {
  return adminRequest.get(`/admin/users/${id}`)
}

/** 启用/禁用会员 */
export function updateMemberStatus(id, status) {
  return adminRequest.put(`/admin/users/${id}/status`, null, { params: { status } })
}
