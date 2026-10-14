import { useAdminStore } from '@/store/admin'
import { useBuyerStore } from '@/store/buyer'

/**
 * 按钮级权限指令
 *
 * 用法：
 *   <el-button v-perm="'product:add'">新增</el-button>
 *   <el-button v-perm="['product:add', 'product:edit']">新增或编辑</el-button>
 *   <el-button v-perm:buyer="'login'">买家端权限（可选）</el-button>
 */
export const permissionDirective = {
  mounted(el, binding) {
    const { value } = binding
    if (!value) return

    const codes = Array.isArray(value) ? value : [value]
    const adminStore = useAdminStore()
    const buyerStore = useBuyerStore()

    const allowed = codes.some((code) => {
      if (code.startsWith('buyer:')) {
        return buyerStore.isLogin
      }
      return adminStore.hasPermission(code)
    })

    if (!allowed) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}

export default permissionDirective
