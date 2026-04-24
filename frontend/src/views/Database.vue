<template>
  <div class="database-container">
    <div class="page-header">
      <h2>本地数据库</h2>
      <div class="db-info">
        <el-tag type="info">数据库文件路径: {{ databasePath }}</el-tag>
      </div>
    </div>
    
    <div class="h2-console-wrapper">
      <iframe
        src="/h2-console"
        frameborder="0"
        width="100%"
        height="100%"
        class="h2-console-iframe"
      ></iframe>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { systemApi } from '@/api/system'

const databasePath = ref('')

const fetchDatabasePath = async () => {
  try {
    const response = await systemApi.getDatabasePath()
    databasePath.value = response.data.path
  } catch (error) {
    console.error('获取数据库路径失败:', error)
    // 失败时使用默认路径
    databasePath.value = '数据库路径获取失败'
  }
}

onMounted(() => {
  fetchDatabasePath()
})
</script>

<style scoped>
.database-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #dcdfe6;
}

.page-header h2 {
  margin: 0;
  color: #303133;
}

.db-info {
  display: flex;
  align-items: center;
}

.h2-console-wrapper {
  width: 100%;
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.h2-console-iframe {
  border: none;
}
</style>