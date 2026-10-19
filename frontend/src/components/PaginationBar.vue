<template>
  <div class="pagination-bar">
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      :page-sizes="pageSizes"
      :layout="layout"
      :small="small"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    currentPage: number
    pageSize: number
    total: number
    pageSizes?: number[]
    layout?: string
    small?: boolean
  }>(),
  {
    pageSizes: () => [10, 20, 50, 100],
    layout: 'total, sizes, prev, pager, next, jumper',
    small: false
  }
)

// 事件名与 el-pagination 保持一致，接入页面时既有处理函数无需改动
const emit = defineEmits<{
  (e: 'update:current-page', page: number): void
  (e: 'update:page-size', size: number): void
  (e: 'current-change', page: number): void
  (e: 'size-change', size: number): void
}>()

const handleCurrentChange = (page: number) => {
  emit('update:current-page', page)
  emit('current-change', page)
}

const handleSizeChange = (size: number) => {
  emit('update:page-size', size)
  emit('size-change', size)
}
</script>

<style scoped>
.pagination-bar {
  display: flex;
  justify-content: flex-end;
}
</style>
