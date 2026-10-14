<template>
  <template v-if="item.children && item.children.length">
    <el-sub-menu :index="item.path || String(item.id)">
      <template #title>
        <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
        <span>{{ item.name }}</span>
      </template>
      <admin-menu-item v-for="child in item.children" :key="child.id" :item="child" />
    </el-sub-menu>
  </template>
  <el-menu-item v-else :index="item.path">
    <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
    <template #title>{{ item.name }}</template>
  </el-menu-item>
</template>

<script setup>
// 递归渲染后台侧边菜单（由后端 /api/admin/auth/menus 返回的权限菜单树驱动）
defineOptions({ name: 'AdminMenuItem' })

defineProps({
  item: {
    type: Object,
    required: true
  }
})
</script>
