import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBuyerStore } from '@/store/buyer'
import { useAdminStore } from '@/store/admin'

/**
 * 路由表
 *  - /            买家商城（ShopLayout）
 *  - /admin       管理后台（AdminLayout，需管理端登录）
 */
const routes = [
  // ============================== 买家商城 ==============================
  {
    path: '/',
    component: () => import('@/layouts/ShopLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/shop/Home.vue'), meta: { title: '首页' } },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('@/views/shop/ProductList.vue'),
        meta: { title: '全部商品' }
      },
      {
        path: 'category/:id',
        name: 'CategoryProducts',
        component: () => import('@/views/shop/ProductList.vue'),
        meta: { title: '分类商品' }
      },
      {
        path: 'search',
        name: 'SearchProducts',
        component: () => import('@/views/shop/ProductList.vue'),
        meta: { title: '搜索商品' }
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('@/views/shop/ProductDetail.vue'),
        meta: { title: '商品详情' }
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/shop/Cart.vue'),
        meta: { title: '购物车', requiresAuth: true }
      },
      {
        path: 'checkout',
        name: 'Checkout',
        component: () => import('@/views/shop/Checkout.vue'),
        meta: { title: '确认订单', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/shop/OrderList.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'order/:orderNo',
        name: 'OrderDetail',
        component: () => import('@/views/shop/OrderDetail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'notices',
        name: 'NoticeList',
        component: () => import('@/views/shop/NoticeList.vue'),
        meta: { title: '公告中心' }
      },
      {
        path: 'notice/:id',
        name: 'NoticeDetail',
        component: () => import('@/views/shop/NoticeDetail.vue'),
        meta: { title: '公告详情' }
      },
      {
        path: 'user',
        component: () => import('@/views/shop/user/UserCenter.vue'),
        meta: { requiresAuth: true },
        children: [
          { path: '', redirect: '/user/profile' },
          {
            path: 'profile',
            name: 'UserProfile',
            component: () => import('@/views/shop/user/Profile.vue'),
            meta: { title: '个人资料', requiresAuth: true }
          },
          {
            path: 'address',
            name: 'UserAddress',
            component: () => import('@/views/shop/user/Address.vue'),
            meta: { title: '收货地址', requiresAuth: true }
          },
          {
            path: 'favorite',
            name: 'UserFavorite',
            component: () => import('@/views/shop/user/Favorite.vue'),
            meta: { title: '我的收藏', requiresAuth: true }
          },
          {
            path: 'history',
            name: 'UserHistory',
            component: () => import('@/views/shop/user/History.vue'),
            meta: { title: '浏览记录', requiresAuth: true }
          },
          {
            path: 'coupon',
            name: 'UserCoupon',
            component: () => import('@/views/shop/user/Coupon.vue'),
            meta: { title: '我的优惠券', requiresAuth: true }
          },
          {
            path: 'comment',
            name: 'UserComment',
            component: () => import('@/views/shop/user/Comment.vue'),
            meta: { title: '我的评价', requiresAuth: true }
          },
          {
            path: 'refund',
            name: 'UserRefund',
            component: () => import('@/views/shop/user/Refund.vue'),
            meta: { title: '退款/售后', requiresAuth: true }
          }
        ]
      }
    ]
  },

  // ============================== 登录注册 ==============================
  { path: '/login', name: 'Login', component: () => import('@/views/shop/Login.vue'), meta: { title: '会员登录' } },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/shop/Register.vue'),
    meta: { title: '会员注册' }
  },

  // ============================== 管理后台 ==============================
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/Login.vue'),
    meta: { title: '管理后台登录' }
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据看板', requiresAdmin: true, perm: 'dashboard:view' }
      },
      {
        path: 'product/list',
        name: 'AdminProductList',
        component: () => import('@/views/admin/product/List.vue'),
        meta: { title: '商品列表', requiresAdmin: true, perm: 'product:list' }
      },
      {
        path: 'product/create',
        name: 'AdminProductCreate',
        component: () => import('@/views/admin/product/Edit.vue'),
        meta: { title: '新增商品', requiresAdmin: true, perm: 'product:add' }
      },
      {
        path: 'product/edit/:id',
        name: 'AdminProductEdit',
        component: () => import('@/views/admin/product/Edit.vue'),
        meta: { title: '编辑商品', requiresAdmin: true, perm: 'product:edit' }
      },
      {
        path: 'product/category',
        name: 'AdminCategory',
        component: () => import('@/views/admin/product/Category.vue'),
        meta: { title: '商品分类', requiresAdmin: true, perm: 'category:list' }
      },
      {
        path: 'product/stock',
        name: 'AdminStock',
        component: () => import('@/views/admin/product/Stock.vue'),
        meta: { title: '库存预警', requiresAdmin: true, perm: 'stock:list' }
      },
      {
        path: 'product/comment',
        name: 'AdminComment',
        component: () => import('@/views/admin/product/Comment.vue'),
        meta: { title: '商品评价', requiresAdmin: true, perm: 'comment:list' }
      },
      {
        path: 'order/list',
        name: 'AdminOrderList',
        component: () => import('@/views/admin/order/List.vue'),
        meta: { title: '订单列表', requiresAdmin: true, perm: 'order:list' }
      },
      {
        path: 'order/ship',
        name: 'AdminOrderShip',
        component: () => import('@/views/admin/order/Ship.vue'),
        meta: { title: '发货管理', requiresAdmin: true, perm: 'order:ship' }
      },
      {
        path: 'order/refund',
        name: 'AdminRefund',
        component: () => import('@/views/admin/order/Refund.vue'),
        meta: { title: '售后工单', requiresAdmin: true, perm: 'refund:list' }
      },
      {
        path: 'order/detail/:id',
        name: 'AdminOrderDetail',
        component: () => import('@/views/admin/order/Detail.vue'),
        meta: { title: '订单详情', requiresAdmin: true, perm: 'order:list' }
      },
      {
        path: 'marketing/coupon',
        name: 'AdminCoupon',
        component: () => import('@/views/admin/marketing/Coupon.vue'),
        meta: { title: '优惠券管理', requiresAdmin: true, perm: 'coupon:list' }
      },
      {
        path: 'marketing/banner',
        name: 'AdminBanner',
        component: () => import('@/views/admin/marketing/Banner.vue'),
        meta: { title: '轮播图管理', requiresAdmin: true, perm: 'banner:list' }
      },
      {
        path: 'marketing/notice',
        name: 'AdminNotice',
        component: () => import('@/views/admin/marketing/Notice.vue'),
        meta: { title: '公告管理', requiresAdmin: true, perm: 'notice:list' }
      },
      {
        path: 'user/list',
        name: 'AdminMemberList',
        component: () => import('@/views/admin/user/List.vue'),
        meta: { title: '会员列表', requiresAdmin: true, perm: 'member:list' }
      },
      {
        path: 'system/user',
        name: 'AdminSysUser',
        component: () => import('@/views/admin/system/User.vue'),
        meta: { title: '管理员管理', requiresAdmin: true, perm: 'sys:user:list' }
      },
      {
        path: 'system/role',
        name: 'AdminSysRole',
        component: () => import('@/views/admin/system/Role.vue'),
        meta: { title: '角色权限', requiresAdmin: true, perm: 'sys:role:list' }
      },
      {
        path: 'system/config',
        name: 'AdminSysConfig',
        component: () => import('@/views/admin/system/Config.vue'),
        meta: { title: '系统配置', requiresAdmin: true, perm: 'sys:config:list' }
      },
      {
        path: 'system/log',
        name: 'AdminSysLog',
        component: () => import('@/views/admin/system/Log.vue'),
        meta: { title: '操作日志', requiresAdmin: true, perm: 'sys:log:list' }
      }
    ]
  },

  // ============================== 404 ==============================
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/** 全局前置守卫：登录态与权限校验 */
router.beforeEach((to, from, next) => {
  const buyerStore = useBuyerStore()
  const adminStore = useAdminStore()

  document.title = to.meta.title ? `${to.meta.title} - 星辰商城` : '星辰商城 - 电商管理系统'

  // 管理端
  if (to.meta.requiresAdmin) {
    if (!adminStore.token) {
      return next(`/admin/login?redirect=${encodeURIComponent(to.fullPath)}`)
    }
    if (to.meta.perm && !adminStore.hasPermission(to.meta.perm)) {
      ElMessage.error('抱歉，您没有该页面的访问权限')
      return next('/admin/dashboard')
    }
    return next()
  }

  // 买家端需登录页面
  if (to.meta.requiresAuth && !buyerStore.token) {
    ElMessage.warning('请先登录后再操作')
    return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }

  next()
})

export default router
