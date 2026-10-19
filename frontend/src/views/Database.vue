<template>
  <PageContainer class="database-page">
    <PageHeader title="本地数据库">
      <template #actions>
        <el-tag type="info">数据库文件路径: {{ databasePath }}</el-tag>
      </template>
    </PageHeader>    
    <div class="h2-console-wrapper">
      <iframe
        src="/h2-console"
        frameborder="0"
        width="100%"
        height="100%"
        class="h2-console-iframe"
      ></iframe>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { systemApi } from '@/api/system'
import PageContainer from '@/components/PageContainer.vue'
import PageHeader from '@/components/PageHeader.vue'

const databasePath = ref('')

const fetchDatabasePath = async () => {
  try {
    const response = await systemApi.getDatabasePath()
    databasePath.value = response.data.path
  } catch (error) {
    console.error('Get db path failed:', error)
    // 失败时使用默认路径
    databasePath.value = '数据库路径获取失败'
  }
}

onMounted(() => {
  fetchDatabasePath()
})
</script>

<style scoped>
.database-page :deep(.page-container__inner) {
  display: flex;
  flex-direction: column;
  gap: var(--app-space-4);
  height: 100%;
}

.h2-console-wrapper {
  width: 100%;
  flex: 1;
  border: 1px solid var(--app-border-base);
  border-radius: var(--app-radius-card);
  overflow: hidden;
  box-shadow: var(--app-shadow-card);
}

.h2-console-iframe {
  border: none;
}
</style>