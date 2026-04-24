<template>
  <div class="debug-environment">
    <h2>环境管理调试页面</h2>

    <div class="debug-info">
      <h3>环境数据调试</h3>
      <el-button @click="loadEnvironments">加载环境数据</el-button>

      <div v-if="environmentStore.isLoading">加载中...</div>

      <div v-if="environmentStore.environments.length > 0">
        <h4>环境列表 (共 {{ environmentStore.environments.length }} 个)</h4>

        <div v-for="env in environmentStore.environments" :key="env.id" class="env-item">
          <h5>环境 {{ env.id }}: {{ env.name }}</h5>
          <pre>完整数据: {{ JSON.stringify(env, null, 2) }}</pre>

          <div class="field-debug">
            <p>URL字段: "{{ env.url }}"</p>
            <p>URL类型: {{ typeof env.url }}</p>
            <p>URL是否为真值: {{ !!env.url }}</p>
            <p>URL长度: {{ env.url ? env.url.length : 'N/A' }}</p>
          </div>

          <div class="url-display-test">
            <h6>URL显示测试:</h6>
            <UrlLink v-if="env.url" :url="env.url" />
            <span v-else>❌ 没有显示URL (url字段为空或假值)</span>
          </div>
        </div>
      </div>

      <div v-else>
        <p>没有环境数据</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useEnvironmentStore } from '@/stores/environment'
import UrlLink from '@/components/UrlLink.vue'

const environmentStore = useEnvironmentStore()

const loadEnvironments = () => {
  environmentStore.fetchEnvironments({
    page: 0,
    size: 10
  })
}

onMounted(() => {
  loadEnvironments()
})
</script>

<style scoped>
.debug-environment {
  padding: 20px;
}

.debug-info {
  margin-top: 20px;
}

.env-item {
  border: 1px solid #ddd;
  padding: 15px;
  margin: 10px 0;
  border-radius: 4px;
}

.field-debug {
  background-color: #f5f5f5;
  padding: 10px;
  margin: 10px 0;
  border-radius: 4px;
}

.url-display-test {
  background-color: #e8f4fd;
  padding: 10px;
  margin: 10px 0;
  border-radius: 4px;
}
</style>