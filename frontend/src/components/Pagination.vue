<template>
  <div v-if="total > 0" class="pagination-wrap">
    <el-pagination
      background
      :total="total"
      :current-page="page"
      :page-size="size"
      :page-sizes="pageSizes"
      :layout="layout"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
/**
 * 分页组件：与后端 PageResult（records/total/page/size/pages）配合使用
 *
 * 用法：<Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadData" />
 */
const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] },
  layout: { type: String, default: 'total, sizes, prev, pager, next, jumper' }
})

const emit = defineEmits(['update:page', 'update:size', 'change'])

const handlePageChange = (value) => {
  emit('update:page', value)
  emit('change')
}

const handleSizeChange = (value) => {
  emit('update:size', value)
  emit('update:page', 1)
  emit('change')
}
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}
</style>
