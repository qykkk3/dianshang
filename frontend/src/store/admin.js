import { defineStore } from 'pinia'
import { adminLogin, adminLogout, getAdminInfo } from '@/api/auth'
import { ADMIN_TOKEN_KEY, ADMIN_INFO_KEY } from '@/api/request'

/**
 * 管理端登录状态与权限
 */
export const useAdminStore = defineStore('admin', {
  state: () => ({
    token: localStorage.getItem(ADMIN_TOKEN_KEY) || '',
    info: JSON.parse(localStorage.getItem(ADMIN_INFO_KEY) || 'null'),
    permissions: [],
    menus: [],
    sidebarCollapsed: false
  }),
  getters: {
    isLogin: (state) => !!state.token,
    nickname: (state) => (state.info && (state.info.realName || state.info.username)) || '管理员',
    avatar: (state) => (state.info && state.info.avatar) || '',
    roleName: (state) => (state.info && state.info.roleName) || '',
    isSuperAdmin: (state) => state.info && state.info.roleCode === 'ADMIN'
  },
  actions: {
    /** 登录 */
    async login(form) {
      const res = await adminLogin(form)
      const data = res.data || {}
      this.token = data.token
      this.info = data.userInfo
      this.permissions = data.permissions || []
      this.menus = data.menus || []
      localStorage.setItem(ADMIN_TOKEN_KEY, this.token)
      localStorage.setItem(ADMIN_INFO_KEY, JSON.stringify(this.info))
      return data
    },
    /** 拉取当前管理员信息（含权限与菜单，角色变更后即时生效） */
    async fetchInfo() {
      if (!this.token) return null
      const res = await getAdminInfo()
      const data = res.data || {}
      this.info = data.userInfo
      this.permissions = data.permissions || []
      this.menus = data.menus || []
      localStorage.setItem(ADMIN_INFO_KEY, JSON.stringify(this.info))
      return data
    },
    /** 是否拥有某权限标识 */
    /**
     * 是否拥有某个权限标识
     *
     * 判定规则与后端完全一致（后端 @PreAuthorize 按权限标识逐项校验），
     * 因此不以「超级管理员」身份短路，避免出现「按钮可见但点击 403」。
     */
    hasPermission(code) {
      if (!code) return true
      return this.permissions.includes(code)
    },
    /** 侧边栏折叠 */
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    /** 退出登录 */
    async logout() {
      try {
        await adminLogout()
      } catch (e) {
        /* 忽略 */
      }
      this.clear()
    },
    clear() {
      this.token = ''
      this.info = null
      this.permissions = []
      this.menus = []
      localStorage.removeItem(ADMIN_TOKEN_KEY)
      localStorage.removeItem(ADMIN_INFO_KEY)
    }
  }
})
