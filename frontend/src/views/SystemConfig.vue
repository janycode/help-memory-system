<template>
  <div class="system-config">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">系统配置</h1>
        <p class="page-description">管理系统全局配置项</p>
      </div>
      
      <div class="config-list">
        <el-card v-for="config in configs" :key="config.id" class="config-card">
          <template #header>
            <div class="config-header">
              <div>
                <span class="config-key">{{ getConfigName(config.configKey) }}</span>
                <span class="config-subtitle">{{ getConfigLocation(config.configKey) }}</span>
              </div>
              <span class="config-description">{{ config.description }}</span>
            </div>
          </template>
          <div class="config-body">
            <el-input 
              v-model="editingConfigs[config.id]" 
              placeholder="请输入配置值"
              class="config-input"
            />
            <div class="config-tip">
              <el-tag size="small" :type="getConfigEffectType(config.configKey)">{{ getConfigEffect(config.configKey) }}</el-tag>
            </div>
            <div class="config-actions">
              <el-button 
                type="primary" 
                size="small" 
                @click="updateConfig(config.id)"
                :loading="loading[config.id]"
              >
                保存
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { systemApi } from '@/api/system'
import { ElMessage, ElCard, ElInput, ElButton } from 'element-plus'

interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  description: string
}

const configs = ref<SystemConfig[]>([])
const editingConfigs = reactive<Record<number, string>>({})
const loading = reactive<Record<number, boolean>>({})

const fetchConfigs = async () => {
  try {
    const response = await systemApi.getConfigs()
    if (response.data) {
      // 只显示system.name配置项
      configs.value = response.data.filter((config: SystemConfig) => config.configKey === 'system.name')
      // 初始化编辑值
      configs.value.forEach(config => {
        editingConfigs[config.id] = config.configValue
      })
    }
  } catch (error) {
    console.error('获取配置失败:', error)
    ElMessage.error('获取配置失败')
  }
}

const updateConfig = async (id: number) => {
  try {
    loading[id] = true
    const value = editingConfigs[id]
    await systemApi.updateConfig(id, value)
    ElMessage.success('配置更新成功')
    // 重新获取配置
    await fetchConfigs()
  } catch (error) {
    console.error('更新配置失败:', error)
    ElMessage.error('更新配置失败')
  } finally {
    loading[id] = false
  }
}

// 获取配置项中文名称
const getConfigName = (key: string): string => {
  const configNames: Record<string, string> = {
    'system.name': '系统名称',
    'system.version': '系统版本',
    'security.password.min.length': '密码最小长度',
    'file.storage.max.size': '文件最大上传大小'
  }
  return configNames[key] || key
}

// 获取配置项生效位置
const getConfigLocation = (key: string): string => {
  const configLocations: Record<string, string> = {
    'system.name': '登录页、首页、欢迎语',
    'system.version': '系统信息',
    'security.password.min.length': '用户注册、密码修改',
    'file.storage.max.size': '文件上传功能'
  }
  return configLocations[key] || '系统全局'
}

// 获取配置项生效方式
const getConfigEffect = (key: string): string => {
  const configEffects: Record<string, string> = {
    'system.name': '实时生效',
    'system.version': '实时生效',
    'security.password.min.length': '实时生效',
    'file.storage.max.size': '实时生效'
  }
  return configEffects[key] || '实时生效'
}

// 获取配置项生效方式对应的标签类型
const getConfigEffectType = (key: string): string => {
  const effectTypes: Record<string, string> = {
    '实时生效': 'success',
    '重新登录生效': 'warning',
    '重启服务生效': 'info'
  }
  const effect = getConfigEffect(key)
  return effectTypes[effect] || 'success'
}

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped>
.system-config {
  min-height: 100vh;
  padding: 20px;
  background-color: #f5f7fa;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.page-description {
  font-size: 14px;
  color: #606266;
  margin: 0;
}

.config-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.config-card {
  transition: all 0.3s;
}

.config-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.config-key {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  display: block;
  margin-bottom: 4px;
}

.config-subtitle {
  font-size: 12px;
  color: #606266;
  display: block;
}

.config-description {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.config-body {
  margin-top: 15px;
}

.config-input {
  margin-bottom: 10px;
}

.config-tip {
  margin-bottom: 15px;
}

.config-actions {
  display: flex;
  justify-content: flex-end;
}
</style>