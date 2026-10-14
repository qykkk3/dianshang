import { defineStore } from 'pinia'
import { getCartCount } from '@/api/cart'
import { useBuyerStore } from './buyer'

/**
 * 购物车角标状态
 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    count: 0
  }),
  actions: {
    /** 刷新购物车数量（未登录时清零） */
    async refresh() {
      const buyerStore = useBuyerStore()
      if (!buyerStore.isLogin) {
        this.count = 0
        return
      }
      try {
        const res = await getCartCount()
        this.count = Number(res.data || 0)
      } catch (e) {
        this.count = 0
      }
    },
    setCount(count) {
      this.count = Number(count || 0)
    },
    clear() {
      this.count = 0
    }
  }
})
