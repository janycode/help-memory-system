<template>
  <div class="test-stats">
    <h2>统计数据测试页面</h2>
    <el-button @click="testApiCall" type="primary">测试API调用</el-button>
    <div v-if="loading">加载中...</div>
    <div v-else>
      <p>环境数量: {{ stats.environments }}</p>
      <p>技术组件: {{ stats.components }}</p>
      <p>业务流程: {{ stats.processes }}</p>
      <p>项目数量: {{ stats.projects }}</p>
    </div>
    <div v-if="error" class="error">
      错误: {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import request from '@/api/request'

const loading = ref(false)
const error = ref('')
const stats = ref({
  environments: 0,
  components: 0,
  processes: 0,
  projects: 0
})

const testApiCall = async () => {
  loading.value = true
  error.value = ''

  try {
    console.log('发送API请求...')
    const response = await request.get('/dashboard/stats')
    console.log('收到响应:', response)

    // 尝试不同的数据提取方式
    if (response.data && response.data.data) {
      stats.value = response.data.data
    } else if (response.data) {
      stats.value = response.data
    } else {
      stats.value = response
    }

    console.log('最终统计数据:', stats.value)
  } catch (err) {
    console.error('API调用失败:', err)
    error.value = err.message || '未知错误'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.test-stats {
  padding: 20px;
}
.error {
  color: red;
  margin-top: 10px;
}
</style>