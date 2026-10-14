<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="adminStore.sidebarCollapsed ? '64px' : '220px'" class="admin-aside">
      <div class="aside-logo">
        <span class="logo-mark">星</span>
        <span v-show="!adminStore.sidebarCollapsed" class="logo-text">星辰商城后台</span>
      </div>
      <el-scrollbar class="aside-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="adminStore.sidebarCollapsed"
          :collapse-transition="false"
          background-color="#1f2d3d"
          text-color="#c0c4cc"
          active-text-color="#ffd04b"
          router
          unique-opened
        >
          <admin-menu-item v-for="menu in adminStore.menus" :key="menu.id" :item="menu" />
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <!-- 顶部 -->
      <el-header class="admin-header">
        <div class="flex gap-16" style="align-items: center">
          <el-icon class="collapse-btn" @click="adminStore.toggleSidebar()">
            <component :is="adminStore.sidebarCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">{{ item.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="flex gap-16" style="align-items: center">
          <el-tag type="warning" effect="plain" size="small">{{ adminStore.roleName || '管理员' }}</el-tag>
          <el-link type="primary" :underline="false" href="/" target="_blank">前台商城</el-link>
          <el-dropdown @command="handleCommand">
            <span class="user-entry">
              <el-avatar :size="28" :src="adminStore.avatar">
                {{ adminStore.nickname.substring(0, 1) }}
              </el-avatar>
              <span class="user-name">{{ adminStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="refresh">
                  <el-icon><Refresh /></el-icon>刷新权限菜单
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="admin-main">
        <router-view v-slot="{ Component }">
          <keep-alive :max="8">
            <component :is="Component" :key="$route.fullPath" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminMenuItem from '@/components/AdminMenuItem.vue'
import { useAdminStore } from '@/store/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

/** 当前高亮菜单 */
const activeMenu = computed(() => route.path)

/** 面包屑（取路由 meta.title） */
const breadcrumbs = computed(() =>
  route.matched.filter((item) => item.meta && item.meta.title).map((item) => ({ path: item.path, title: item.meta.title }))
)

const handleCommand = async (command) => {
  if (command === 'refresh') {
    await adminStore.fetchInfo()
    ElMessage.success('权限菜单已刷新')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    await adminStore.logout()
    ElMessage.success('已退出登录')
    router.push('/admin/login')
  }
}

onMounted(async () => {
  // 刷新页面后重新拉取权限与菜单，保证角色变更即时生效
  try {
    await adminStore.fetchInfo()
  } catch (e) {
    /* 拦截器已处理 401 */
  }
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.admin-aside {
  background-color: #1f2d3d;
  transition: width 0.25s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.aside-logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  color: #fff;
  background-color: #16222f;
  white-space: nowrap;
}

.logo-mark {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff7a45, #ff5000);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
}

.aside-scroll {
  flex: 1;
}

.admin-aside :deep(.el-menu) {
  border-right: none;
}

.admin-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.admin-main {
  background: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}
</style>
