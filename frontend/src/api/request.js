import axios from 'axios'
import { ElMessage } from 'element-plus'

/** 买家端 Token 存储键 */
export const BUYER_TOKEN_KEY = 'mall_buyer_token'
/** 管理端 Token 存储键 */
export const ADMIN_TOKEN_KEY = 'mall_admin_token'
/** 买家端用户信息存储键 */
export const BUYER_INFO_KEY = 'mall_buyer_info'
/** 管理端用户信息存储键 */
export const ADMIN_INFO_KEY = 'mall_admin_info'

/** 跳转登录页（401 时） */
function redirectToLogin(isAdmin) {
  const loginPath = isAdmin ? '/admin/login' : '/login'
  const current = window.location.pathname + window.location.search
  if (window.location.pathname === loginPath) {
    return
  }
  window.location.href = `${loginPath}?redirect=${encodeURIComponent(current)}`
}

/**
 * 创建 axios 实例
 * @param {string} tokenKey Token 在 localStorage 中的键
 * @param {boolean} isAdmin 是否为管理端实例
 */
function createService(tokenKey, isAdmin) {
  const service = axios.create({
    baseURL: '/api',
    timeout: 30000
  })

  service.interceptors.request.use(
    (config) => {
      const token = localStorage.getItem(tokenKey)
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    (error) => Promise.reject(error)
  )

  service.interceptors.response.use(
    (response) => {
      // 文件下载：直接返回完整响应，交由 downloadBlob 处理
      if (response.config.responseType === 'blob') {
        const contentType = response.headers['content-type'] || ''
        if (contentType.includes('application/json')) {
          return response.data.text().then((text) => {
            let message = '导出失败'
            try {
              message = JSON.parse(text).message || message
            } catch (e) {
              /* ignore */
            }
            ElMessage.error(message)
            return Promise.reject(new Error(message))
          })
        }
        return response
      }

      const res = response.data
      if (res && res.code === 200) {
        return res
      }
      const message = (res && res.message) || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    },
    (error) => {
      const status = error.response && error.response.status
      const data = error.response && error.response.data
      const message = (data && data.message) || error.message || '网络异常，请稍后重试'

      if (status === 401) {
        localStorage.removeItem(tokenKey)
        localStorage.removeItem(isAdmin ? ADMIN_INFO_KEY : BUYER_INFO_KEY)
        ElMessage.error(message || '登录已过期，请重新登录')
        redirectToLogin(isAdmin)
      } else if (status === 403) {
        ElMessage.error((data && data.message) || '抱歉，您没有该操作权限')
      } else if (status === 404) {
        ElMessage.error('请求的接口不存在')
      } else {
        ElMessage.error(message)
      }
      return Promise.reject(error)
    }
  )

  return service
}

/** 买家端请求实例 */
export const request = createService(BUYER_TOKEN_KEY, false)

/** 管理端请求实例 */
export const adminRequest = createService(ADMIN_TOKEN_KEY, true)

/**
 * 下载后端返回的文件流
 * @param {object} response axios 响应（responseType=blob）
 * @param {string} defaultName 兜底文件名
 */
export function downloadBlob(response, defaultName = 'download.xlsx') {
  const blob = new Blob([response.data])
  const disposition = response.headers['content-disposition'] || ''
  let filename = defaultName
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  const plainMatch = /filename="?([^";]+)"?/i.exec(disposition)
  if (utf8Match) {
    filename = decodeURIComponent(utf8Match[1])
  } else if (plainMatch) {
    filename = decodeURIComponent(plainMatch[1])
  }
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export default request
