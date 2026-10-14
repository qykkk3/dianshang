import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getBuyerInfo } from '@/api/auth'
import { BUYER_TOKEN_KEY, BUYER_INFO_KEY } from '@/api/request'

/**
 * 买家端登录状态
 */
export const useBuyerStore = defineStore('buyer', {
  state: () => ({
    token: localStorage.getItem(BUYER_TOKEN_KEY) || '',
    info: JSON.parse(localStorage.getItem(BUYER_INFO_KEY) || 'null')
  }),
  getters: {
    isLogin: (state) => !!state.token,
    nickname: (state) => (state.info && (state.info.nickname || state.info.username)) || '未登录',
    avatar: (state) => (state.info && state.info.avatar) || ''
  },
  actions: {
    /** 登录 */
    async login(form) {
      const res = await loginApi(form)
      this.token = res.data.token
      this.info = res.data.userInfo
      localStorage.setItem(BUYER_TOKEN_KEY, this.token)
      localStorage.setItem(BUYER_INFO_KEY, JSON.stringify(this.info))
      return res.data
    },
    /** 拉取最新用户信息 */
    async fetchInfo() {
      if (!this.token) return null
      const res = await getBuyerInfo()
      this.info = res.data
      localStorage.setItem(BUYER_INFO_KEY, JSON.stringify(this.info))
      return this.info
    },
    /** 更新本地缓存的信息（修改资料后调用） */
    setInfo(info) {
      this.info = info
      localStorage.setItem(BUYER_INFO_KEY, JSON.stringify(info))
    },
    /** 退出登录 */
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        /* 忽略退出接口异常，本地状态照常清理 */
      }
      this.clear()
    },
    clear() {
      this.token = ''
      this.info = null
      localStorage.removeItem(BUYER_TOKEN_KEY)
      localStorage.removeItem(BUYER_INFO_KEY)
    }
  }
})
