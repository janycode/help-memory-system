<template>
  <el-dialog
    v-model="visible"
    title="连接信息"
    width="650px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div v-if="!component" class="empty-hint">请选择一个组件</div>
    <div v-else class="connection-strings">
      <div class="component-info">
        <span class="component-name">{{ component.name }}</span>
        <el-tag size="small">{{ component.category }}</el-tag>
      </div>
      <el-divider />
      <div v-if="strings.length === 0" class="empty-hint">
        该组件暂无连接信息可生成
      </div>
      <div v-for="(item, idx) in strings" :key="idx" class="connection-item">
        <div class="connection-label">
          <span>{{ item.label }}</span>
          <el-button type="primary" link @click="copy(item.command)">
            <el-icon><CopyDocument /></el-icon>
            复制
          </el-button>
        </div>
        <div class="connection-command">
          <code>{{ item.command }}</code>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { generateConnectionStrings } from '@/utils/connectionStrings'
import type { TechnicalComponent } from '@/types/component'

const visible = ref(false)
const component = ref<TechnicalComponent | null>(null)

const strings = computed(() => {
  if (!component.value) return []
  return generateConnectionStrings(
    component.value.category,
    component.value.url || '',
    component.value.username,
    component.value.password
  )
})

const open = (comp: TechnicalComponent) => {
  component.value = comp
  visible.value = true
}

const copy = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

defineExpose({ open })
</script>

<style scoped>
.empty-hint {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  font-size: 14px;
}

.component-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.component-name {
  font-size: 16px;
  font-weight: 600;
}

.connection-item {
  margin-bottom: 16px;
}

.connection-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
  color: #606266;
}

.connection-command code {
  display: block;
  padding: 10px 14px;
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-all;
  white-space: pre-wrap;
}

html.dark .connection-command code {
  background-color: #1d1e1f;
  border-color: #333;
  color: #e0e0e0;
}
</style>
