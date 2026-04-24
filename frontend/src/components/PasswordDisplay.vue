<template>
  <div class="password-display">
    <span class="password-text" :class="{ masked: !showPassword }">
      {{ displayPassword }}
    </span>
    <el-button
      type="text"
      class="toggle-btn"
      @click="togglePassword"
      :title="showPassword ? '隐藏密码' : '显示密码'"
    >
      <el-icon>
        <View />
      </el-icon>
    </el-button>
    <el-button
      type="text"
      class="copy-btn"
      @click="copyPassword"
      title="复制密码"
    >
      <el-icon>
        <CopyDocument />
      </el-icon>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, View } from '@element-plus/icons-vue'

const props = defineProps<{
  password: string
}>()

const showPassword = ref(false)

const displayPassword = computed(() => {
  if (showPassword.value) {
    return props.password
  }
  // 根据密码实际长度显示相应数量的星号
  // return '*'.repeat(props.password.length)
  return '*'.repeat(6)
})

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const copyPassword = async () => {
  try {
    await navigator.clipboard.writeText(props.password)
    ElMessage.success('密码已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.password-text {
  font-size: 14px;
  color: #606266;
  min-width: 100px;
}

.password-text.masked {
  color: #909399;
  letter-spacing: 2px;
}

.toggle-btn,
.copy-btn {
  font-size: 16px;
}

.toggle-btn:hover,
.copy-btn:hover {
  color: #409eff;
}
</style>