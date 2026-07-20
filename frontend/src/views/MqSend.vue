<template>
  <div class="mq-send-container">
    <div class="main-content">
      <!-- 配置面板 -->
      <div class="config-panel">
        <!-- 环境和类型 -->
        <div class="config-row">
          <div class="config-item">
            <label>环境</label>
            <div class="switch-group">
              <el-button size="small" :type="currentEnv === 'dev' ? 'primary' : ''" @click="switchEnv('dev')">DEV</el-button>
              <el-button size="small" :type="currentEnv === 'test' ? 'primary' : ''" @click="switchEnv('test')">TEST</el-button>
            </div>
          </div>
          <div class="config-item">
            <label>类型</label>
            <div class="switch-group">
              <el-button size="small" :type="currentType === 'INBD' ? 'success' : ''" @click="switchType('INBD')">🚛 入场</el-button>
              <el-button size="small" :type="currentType === 'OUTBD' ? 'warning' : ''" @click="switchType('OUTBD')">🚚 出场</el-button>
            </div>
          </div>
        </div>

        <!-- 请求地址 -->
        <div class="url-bar">
          <span class="url-label">POST</span>
          <span class="url-value">{{ currentUrl }}</span>
          <el-tag :type="currentEnv === 'dev' ? 'success' : 'warning'" size="small">
            {{ currentEnv.toUpperCase() }}
          </el-tag>
        </div>

        <!-- 表单 -->
        <div class="form-grid">
          <div class="form-item">
            <label><span class="required">*</span>车牌号</label>
            <div class="input-group">
              <el-input size="small" v-model="licencePlate" placeholder="粤B1834C" @input="updatePreview" />
              <el-button size="small" @click="licencePlate = ''; updatePreview()">清空</el-button>
            </div>
          </div>
          <div class="form-item">
            <label><span class="required">*</span>总重量(kg)</label>
            <div class="input-group">
              <el-input-number size="small" v-model="totalWeight" :min="0" @change="updatePreview" />
              <el-button size="small" @click="totalWeight = 0; updatePreview()">清空</el-button>
            </div>
          </div>
          <div class="form-item">
            <label>称重时间</label>
            <el-input size="small" v-model="weightDate" readonly />
          </div>
          <div class="form-item">
            <label>仓库编码</label>
            <el-input size="small" v-model="warehouseCode" readonly />
          </div>
          <div class="form-item">
            <label>Topic</label>
            <el-input size="small" v-model="topic" readonly />
          </div>
          <div class="form-item">
            <label>Tag</label>
            <el-input size="small" v-model="tag" readonly />
          </div>
          <div class="form-item full-width">
            <label><span class="required">*</span>{{ tokenLabel }}</label>
            <div class="input-group">
              <el-input size="small" v-model="token" placeholder="从RocketMQ管理后台登录后获取" @input="updatePreview" />
              <el-button size="small" @click="token = ''; updatePreview()">清空</el-button>
              <el-button size="small" @click="restoreDefaults()">默认值</el-button>
              <el-button size="small" type="success" :loading="isSending" @click="handleSend">
                {{ isSending ? '发送中...' : '发 送' }}
              </el-button>
            </div>
          </div>
        </div>

        <div class="footer-hint">
          注意事项：① Token需要问开发索要；② 多个车牌号用英文逗号隔开，如：粤B1834C,粤B5678D
        </div>
      </div>

      <!-- 请求预览 -->
      <div class="preview-panel">
        <div class="panel-title">请求预览</div>
        <div class="preview-content">
          <div class="headers-preview">
            <div v-for="(value, key) in proxyHeaders" :key="key" class="header-line">
              <span class="header-key">{{ key }}:</span>
              <span class="header-val">{{ value }}</span>
            </div>
          </div>
          <div class="json-preview">
            <div v-if="plateList.length > 1" class="preview-nav">
              <el-button size="small" :disabled="currentPlateIndex === 0" @click="navigatePlate(-1)">◀</el-button>
              <span>{{ currentPlateIndex + 1 }} / {{ plateList.length }} [{{ plateList[currentPlateIndex] }}]</span>
              <el-button size="small" :disabled="currentPlateIndex === plateList.length - 1" @click="navigatePlate(1)">▶</el-button>
            </div>
            <pre class="json-box">{{ formatJson(currentPayload) }}</pre>
          </div>
        </div>
      </div>

      <!-- 响应结果 -->
      <div class="result-panel" :class="{ success: hasSuccess, fail: hasFail }">
        <div class="panel-title">响应结果</div>
        <div class="result-list">
          <div v-if="resultList.length === 0" class="empty-hint">暂无响应</div>
          <div v-for="(item, index) in resultList" :key="index" class="result-item" :class="{ success: item.success, fail: !item.success }" @click="item.expanded = !item.expanded">
            <div class="result-item-header">
              <span class="result-item-icon">{{ item.success ? '✅' : '❌' }}</span>
              <span class="result-item-plate">{{ item.plate }}</span>
              <span class="result-item-summary">{{ item.summary }}</span>
              <span class="result-item-toggle" :class="{ expanded: item.expanded }">▶</span>
            </div>
            <div v-if="item.expanded" class="result-item-body">
              <pre class="json-box">{{ formatJson(item.data) }}</pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 环境配置
const ENV_CONFIG = {
  dev: {
    url: 'http://192.168.33.10:9880/topic/sendTopicMessage.do',
    topic: 'dev%edi',
    tokenType: 'XSRF-TOKEN'
  },
  test: {
    url: 'https://devops.example.com/rocketmq/topic/sendTopicMessage.do',
    topic: 'staging%edi',
    tokenType: 'JSESSIONID'
  }
}

const TYPE_DEFAULTS: Record<string, number> = {
  INBD: 5500,
  OUTBD: 1500
}

// 状态
const currentEnv = ref('test')
const currentType = ref('INBD')
const licencePlate = ref('粤B1834C')
const totalWeight = ref(5500)
const weightDate = ref('')
const warehouseCode = ref('QY')
const topic = ref('staging%edi')
const tag = ref('weighbridge')
const token = ref('37DEF86CD498103D218962626EB2A25F')
const isSending = ref(false)
const currentPlateIndex = ref(0)
const plateList = ref<string[]>([])
const resultList = ref<Array<{
  plate: string
  success: boolean
  summary: string
  data: any
  expanded: boolean
}>>([])

// 计算属性
const currentUrl = computed(() => ENV_CONFIG[currentEnv.value as keyof typeof ENV_CONFIG].url)
const tokenLabel = computed(() => ENV_CONFIG[currentEnv.value as keyof typeof ENV_CONFIG].tokenType)

const proxyHeaders = computed(() => {
  const cfg = ENV_CONFIG[currentEnv.value as keyof typeof ENV_CONFIG]
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Cookie': getCookie(),
    'User-Agent': 'Apifox/1.0.0 (https://apifox.com)',
    'Accept': '*/*',
    'Host': new URL(cfg.url).host,
    'Connection': 'keep-alive'
  }
  if (cfg.tokenType === 'XSRF-TOKEN') {
    headers['x-xsrf-token'] = token.value
  }
  return headers
})

const currentPayload = computed(() => {
  if (plateList.value.length === 0) return null
  if (currentPlateIndex.value >= plateList.value.length) {
    currentPlateIndex.value = plateList.value.length - 1
  }
  return buildPayload(plateList.value[currentPlateIndex.value])
})

const hasSuccess = computed(() => resultList.value.some(r => r.success))
const hasFail = computed(() => resultList.value.some(r => !r.success))

// 方法
const getCookie = () => {
  const cfg = ENV_CONFIG[currentEnv.value as keyof typeof ENV_CONFIG]
  if (cfg.tokenType === 'XSRF-TOKEN') {
    return `XSRF-TOKEN=${token.value}`
  } else {
    return `NG_TRANSLATE_LANG_KEY=%22zh%22;JSESSIONID=${token.value}`
  }
}

const switchEnv = (env: string) => {
  currentEnv.value = env
  const cfg = ENV_CONFIG[env as keyof typeof ENV_CONFIG]
  topic.value = cfg.topic
  updatePreview()
}

const switchType = (type: string) => {
  currentType.value = type
  totalWeight.value = TYPE_DEFAULTS[type]
  refreshWeightDate()
  updatePreview()
}

const refreshWeightDate = () => {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  weightDate.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const getPlates = () => {
  const raw = licencePlate.value.trim()
  if (!raw) return []
  return [...new Set(raw.split(',').map(s => s.trim()).filter(Boolean))]
}

const buildPayload = (plate: string) => {
  return {
    topic: topic.value,
    key: plate,
    tag: tag.value,
    messageBody: JSON.stringify({
      weighbridgeType: currentType.value,
      totalWeight: totalWeight.value,
      licencePlate: plate,
      weightDate: weightDate.value || new Date().toISOString(),
      warehouseCode: warehouseCode.value
    }),
    traceEnabled: false
  }
}

const updatePreview = () => {
  plateList.value = getPlates()
  if (currentPlateIndex.value >= plateList.value.length) {
    currentPlateIndex.value = Math.max(0, plateList.value.length - 1)
  }
}

const navigatePlate = (delta: number) => {
  currentPlateIndex.value += delta
}

const formatJson = (obj: any) => {
  if (!obj) return ''
  return JSON.stringify(obj, null, 2)
}

const restoreDefaults = () => {
  licencePlate.value = '粤B1834C'
  totalWeight.value = TYPE_DEFAULTS[currentType.value]
  token.value = '37DEF86CD498103D218962626EB2A25F'
  refreshWeightDate()
  updatePreview()
}

const handleSend = async () => {
  refreshWeightDate()
  updatePreview()

  plateList.value = getPlates()
  if (plateList.value.length === 0) {
    ElMessage.error('请输入车牌号')
    return
  }

  if (!token.value) {
    ElMessage.error(`请输入 ${tokenLabel.value}`)
    return
  }

  isSending.value = true
  resultList.value = []

  for (const plate of plateList.value) {
    const payload = buildPayload(plate)

    const resultItem = {
      plate,
      success: false,
      summary: '发送中...',
      data: null,
      expanded: false
    }
    resultList.value.push(resultItem)

    try {
      const token = userStore.token
      const headers: Record<string, string> = { 'Content-Type': 'application/json' }
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      const response = await fetch('/api/tools/mq-send', {
        method: 'POST',
        headers,
        body: JSON.stringify({
          env: currentEnv.value,
          proxyHeaders: proxyHeaders.value,
          ...payload
        })
      })

      const data = await response.json()
      const isSuccess = data.data?.data?.sendStatus === 'SEND_OK'

      resultItem.success = isSuccess
      resultItem.summary = isSuccess ? '请求成功' : '请求失败'
      resultItem.data = data
    } catch (err: any) {
      resultItem.success = false
      resultItem.summary = '请求失败'
      resultItem.data = { error: err.message }
    }
  }

  isSending.value = false
}

// 初始化
onMounted(() => {
  refreshWeightDate()
  updatePreview()
})
</script>

<style scoped>
.mq-send-container {
  padding: 10px 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  overflow-y: auto;
}

.config-panel,
.preview-panel,
.result-panel {
  background: #fff;
  border-radius: 6px;
  padding: 10px 12px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.08);
}

.config-row {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.config-item label {
  font-size: 11px;
  color: #909399;
}

.switch-group {
  display: flex;
  gap: 4px;
}

.url-bar {
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 5px 8px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.url-label {
  font-weight: 600;
  color: #303133;
}

.url-value {
  flex: 1;
  word-break: break-all;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 12px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.form-item.full-width {
  grid-column: 1 / -1;
}

.form-item label {
  font-size: 11px;
  font-weight: 500;
  color: #606266;
}

.required {
  color: #f56c6c;
}

.input-group {
  display: flex;
  gap: 4px;
  align-items: center;
}

.footer-hint {
  font-size: 10px;
  color: #909399;
  margin-top: 6px;
}

.panel-title {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid #ebeef5;
}

.preview-content {
  display: flex;
  gap: 10px;
}

.headers-preview {
  flex: 0 0 300px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 10px;
  line-height: 1.5;
}

.header-line {
  margin-bottom: 2px;
}

.header-key {
  color: #c678dd;
  font-weight: 600;
}

.header-val {
  color: #98c379;
  word-break: break-all;
}

.json-preview {
  flex: 1;
  min-width: 0;
}

.preview-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 11px;
  color: #909399;
}

.json-box {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 10px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  max-height: 250px;
  overflow-y: auto;
}

.result-panel {
  border-left: 3px solid #dcdfe6;
}

.result-panel.success {
  border-left-color: #67c23a;
}

.result-panel.fail {
  border-left-color: #f56c6c;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 250px;
  overflow-y: auto;
}

.result-item {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 6px 8px;
  cursor: pointer;
  transition: background 0.15s;
  font-size: 12px;
  border-left: 3px solid transparent;
}

.result-item.success {
  border-left-color: #67c23a;
}

.result-item.fail {
  border-left-color: #f56c6c;
}

.result-item:hover {
  background: #f5f7fa;
}

.result-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-item-icon {
  font-size: 12px;
}

.result-item-plate {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
}

.result-item-summary {
  font-size: 11px;
  color: #909399;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-item-toggle {
  font-size: 9px;
  color: #409eff;
  transition: transform 0.2s;
}

.result-item-toggle.expanded {
  transform: rotate(90deg);
}

.result-item-body {
  margin-top: 6px;
}

.empty-hint {
  text-align: center;
  color: #909399;
  font-size: 11px;
  padding: 10px 0;
}
</style>