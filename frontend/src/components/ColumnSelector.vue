<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <el-button size="small">
      <el-icon><Grid /></el-icon>
      列设置
      <el-icon><ArrowDown /></el-icon>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu class="column-selector-menu">
        <el-dropdown-item
          v-for="col in columns"
          :key="col.key"
          :command="col.key"
          :divided="col.key === 'divider'"
        >
          <el-checkbox
            :model-value="col.visible"
            :label="col.label"
            size="small"
            @click.stop
            @change="() => toggle(col.key)"
          />
        </el-dropdown-item>
        <el-dropdown-item divided command="reset">
          <span style="color: #409eff;">恢复默认</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { Grid, ArrowDown } from '@element-plus/icons-vue'
import type { ColumnDef } from '@/composables/useColumnVisibility'

const props = defineProps<{
  columns: ColumnDef[]
}>()

const emit = defineEmits<{
  toggle: [key: string]
  reset: []
}>()

const handleCommand = (key: string) => {
  if (key === 'reset') {
    emit('reset')
  } else {
    emit('toggle', key)
  }
}

const toggle = (key: string) => {
  emit('toggle', key)
}
</script>

<style scoped>
.column-selector-menu {
  max-height: 300px;
  overflow-y: auto;
}

.column-selector-menu :deep(.el-dropdown-menu__item) {
  padding: 4px 16px;
}

.column-selector-menu :deep(.el-checkbox) {
  margin-right: 0;
}
</style>
