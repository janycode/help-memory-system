<template>
  <div class="rocketmq-container">
    <div class="main-content">
      <div class="config-panel">
        <div class="url-config-header" @click="urlConfigExpanded = !urlConfigExpanded">
          <span class="url-config-title">请求地址配置</span>
          <el-link type="primary" :underline="false">
            {{ urlConfigExpanded ? '收起 ▲' : '展开 ▼' }}
          </el-link>
        </div>
        <div v-if="urlConfigExpanded" class="url-config-body">
          <div class="url-config-env">
            <span class="url-config-env-label">DEV:</span>
            <el-input v-model="envConfig.dev.url" size="small" placeholder="发送地址" />
            <el-input v-model="envConfig.dev.topicsUrl" size="small" placeholder="Topic列表地址" />
          </div>
          <div class="url-config-env">
            <span class="url-config-env-label">TEST:</span>
            <el-input v-model="envConfig.test.url" size="small" placeholder="发送地址" />
            <el-input v-model="envConfig.test.topicsUrl" size="small" placeholder="Topic列表地址" />
          </div>
          <div class="url-config-actions">
            <el-button size="small" type="primary" @click="handleSaveUrls">保存</el-button>
            <el-button size="small" @click="handleResetUrls">重置</el-button>
          </div>
        </div>

        <div class="config-row">
          <div class="config-item">
            <label>环境</label>
            <div class="switch-group">
              <el-button size="small" :type="currentEnv === 'dev' ? 'primary' : ''" @click="switchEnv('dev')">DEV</el-button>
              <el-button size="small" :type="currentEnv === 'test' ? 'primary' : ''" @click="switchEnv('test')">TEST</el-button>
            </div>
          </div>
        </div>

        <div class="url-bar">
          <span class="url-label">POST</span>
          <span class="url-value">{{ currentUrl }}</span>
          <el-tag :type="currentEnv === 'dev' ? 'success' : 'warning'" size="small">
            {{ currentEnv.toUpperCase() }}
          </el-tag>
          <span class="version-tag">基于{{ currentVersion }}</span>
        </div>

        <div class="form-grid">
          <div class="form-item">
            <label><span class="required">*</span>{{ currentTokenLabel }}</label>
            <div class="input-group">
              <el-input size="small" v-model="token" :placeholder="currentTokenPlaceholder" :spellcheck="false" @input="refreshHeaders" @blur="onTokenBlur" />
              <el-button size="small" @click="token = ''; refreshHeaders()">清空</el-button>
            </div>
          </div>

          <div class="form-item span-2">
            <label><span class="required">*</span>Topic</label>
            <div class="input-group">
              <el-select size="small" v-model="topic" placeholder="请选择Topic" filterable clearable>
                <el-option v-for="t in topicList" :key="t" :label="t" :value="t" />
              </el-select>
              <el-button size="small" type="primary" :loading="isLoadingTopics" @click="loadTopics">刷新</el-button>
            </div>
          </div>

          <div class="form-item full-width">
            <label><span class="required">*</span>Java文件</label>
            <div class="input-group">
              <el-input
                size="small"
                v-model="javaFilePath"
                placeholder="选择文件或粘贴服务器绝对路径"
                :spellcheck="false"
                @input="onJavaPathInput"
              >
                <template #prefix>
                  <el-tooltip content="点击选择本地文件（自动上传）" placement="top">
                    <span class="file-picker-hint" @click="selectJavaFile">选择文件</span>
                  </el-tooltip>
                </template>
              </el-input>
              <el-button size="small" type="primary" :loading="isParsing" @click="parseJavaFile">解析</el-button>
              <el-button size="small" type="success" :loading="isPreviewingJava" @click="previewJavaFileContent">预览</el-button>
            </div>
            <input ref="fileInputRef" type="file" accept=".java" class="hidden-file-input" @change="onJavaFileSelected" />
          </div>

          <div class="form-item">
            <label><span class="required">*</span>Tag</label>
            <el-input size="small" v-model="tag" placeholder="自动解析或手动输入" :spellcheck="false" />
          </div>

          <div class="form-item key-field">
            <label><span class="required">*</span>Key</label>
            <el-input size="small" v-model="key" placeholder="消息唯一标识" :spellcheck="false" />
          </div>

          <div class="form-item">
            <label>消息轨迹</label>
            <el-switch v-model="traceEnabled" size="small" :active-value="true" :inactive-value="false" />
          </div>
        </div>

        <div class="footer-hint">
          注意事项：① Token需要从RocketMQ管理后台登录后获取<span class="highlight-red"> Cookie </span>中的值；② 点击输入框左侧【选择文件】选择本地<span class="highlight-red"> .java 文件 </span>（解析时自动上传，内存解析不落盘），或直接粘贴服务器可访问的<span class="highlight-red"> 绝对路径 </span>；③ 消息体JSON可手动编辑，只需编辑所需<span class="highlight-red"> Value </span>值
        </div>
      </div>

      <div class="panels-row">
        <div class="preview-panel featured-panel">
          <div class="panel-title">
            消息体
            <div class="body-toolbar">
              <el-button size="small" @click="formatMessageBody">格式化</el-button>
              <el-button size="small" type="warning" @click="validateMessageBody">校验</el-button>
              <el-button size="small" type="primary" @click="switchBodyMode">
                {{ bodyEditMode ? '预览' : '编辑' }}
              </el-button>
              <el-button size="small" class="btn-danger" @click="clearMessageBody">清空</el-button>
            </div>
          </div>
          <div class="preview-content">
            <div class="json-editor">
              <div v-if="bodyEditMode" class="body-editor-wrapper">
                <div class="line-numbers" ref="lineNumbersRef">
                  <div v-for="n in lineCount" :key="n" class="line-number">{{ n }}</div>
                </div>
                <textarea
                  ref="textareaRef"
                  v-model="messageBodyJson"
                  placeholder="消息体JSON"
                  class="body-editor-textarea"
                  :spellcheck="false"
                  :autocapitalize="'off'"
                  :autocorrect="'off'"
                  @scroll="onTextareaScroll"
                ></textarea>
              </div>
              <pre v-else class="json-preview body-preview" v-html="highlightJson(parseMessageBody(messageBodyJson))"></pre>
            </div>
          </div>
        </div>

        <div class="send-panel">
          <div class="panel-title">请求预览</div>
          <div class="preview-content">
            <div class="headers-preview">
              <div v-for="(value, key) in proxyHeaders" :key="key" class="header-line">
                <span class="header-key">{{ key }}:</span>
                <span class="header-val">{{ value }}</span>
              </div>
            </div>
            <div class="json-preview">
              <el-button class="popup-preview-btn" size="small" type="primary" plain @click="requestPreviewVisible = true">弹窗预览</el-button>
              <pre class="json-box" v-html="highlightJson(buildPayloadForDisplay())"></pre>
            </div>
          </div>
          <div class="send-action">
            <el-button @click="historyDialogVisible = true">历史消息</el-button>
            <el-button type="success" :loading="isSending" @click="handleSend" :disabled="!canSend">
              {{ isSending ? '发送中...' : '发送消息' }}
            </el-button>
            <el-button type="primary" @click="resetForm">重置</el-button>
          </div>
        </div>
      </div>

    </div>

    <div class="result-panel" :class="{ success: hasSuccess, fail: hasFail }">
      <div class="panel-title">响应结果</div>
      <div class="result-list">
        <div v-if="resultList.length === 0" class="empty-hint">暂无响应</div>
        <div v-for="(item, index) in resultList" :key="index" class="result-item" :class="{ success: item.success, fail: !item.success }" @click="item.expanded = !item.expanded">
          <div class="result-item-header">
            <span class="result-item-icon">{{ item.success ? '✅' : '❌' }}</span>
            <span class="result-item-summary">{{ item.summary }}</span>
            <span class="result-item-toggle" :class="{ expanded: item.expanded }">▶</span>
          </div>
          <div v-if="item.expanded" class="result-item-body">
            <pre class="json-box json-box-full">{{ formatJson(item.data) }}</pre>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="requestPreviewVisible" title="请求体完整预览" width="90%" :close-on-click-modal="false">
      <pre class="json-box request-preview-body" v-html="highlightJson(buildPayloadForDisplay())"></pre>
    </el-dialog>

    <el-dialog v-model="historyDialogVisible" title="历史消息" width="80%" :close-on-click-modal="false">
      <div class="history-toolbar">
        <span class="history-count">共 {{ filteredHistoryList.length }}/{{ MAX_HISTORY }} 条记录</span>
        <el-button size="small" type="danger" @click="clearHistory" :disabled="filteredHistoryList.length === 0">清空历史</el-button>
      </div>
      <div class="history-list">
        <div v-if="filteredHistoryList.length === 0" class="empty-hint">暂无历史记录</div>
        <div v-for="record in filteredHistoryList" :key="record.id" class="history-item" :class="{ success: record.success, fail: !record.success }">
          <div class="history-item-header">
            <div class="history-item-header-left" @click="record.expanded = !record.expanded">
              <span class="history-status" :class="record.success ? 'status-success' : 'status-fail'">
                {{ record.success ? '成功' : '失败' }}
              </span>
              <span class="history-time">{{ record.timestamp }}</span>
              <span class="history-summary">Topic: {{ record.request.topic }} | Tag: {{ record.request.tag }} | Key: {{ record.request.key }}</span>
              <span class="history-toggle" :class="{ expanded: record.expanded }">▶</span>
            </div>
            <div class="history-item-actions">
              <el-button size="small" type="primary" @click.stop="editFromHistory(record.id)">编辑</el-button>
              <el-button size="small" type="success" @click.stop="resendFromHistory(record.id)">发送</el-button>
              <el-button size="small" type="danger" @click.stop="removeFromHistory(record.id)">删除</el-button>
            </div>
          </div>
          <div v-if="record.expanded" class="history-item-body">
            <div class="history-section">
              <div class="history-section-title">请求信息</div>
              <div class="history-section-content">
                <pre class="json-box">{{ formatJson({ targetUrl: record.request.targetUrl, topic: record.request.topic, tag: record.request.tag, key: record.request.key, traceEnabled: record.request.traceEnabled }) }}</pre>
              </div>
              <div class="history-section-title">消息体</div>
              <div class="history-section-content">
                <pre class="json-box">{{ formatJson(parseMessageBody(record.request.messageBody)) }}</pre>
              </div>
              <div class="history-section-title">请求头</div>
              <div class="history-section-content">
                <pre class="json-box">{{ formatJson(record.request.proxyHeaders) }}</pre>
              </div>
            </div>
            <div class="history-section">
              <div class="history-section-title">响应信息</div>
              <div class="history-section-content history-section-content-full">
                <pre class="json-box json-box-full">{{ formatJson(record.response) }}</pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="javaPreviewVisible" title="Java文件预览" width="80%" :close-on-click-modal="false">
      <div class="java-preview-header">
        <span class="java-preview-path">{{ javaPreviewFilePath }}</span>
      </div>
      <div class="java-preview-wrapper">
        <pre class="java-preview-code"><code v-html="javaHighlightedContent" class="language-java"></code></pre>
      </div>
      <template #footer>
        <el-button @click="javaPreviewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { rocketmqApi } from '@/api/rocketmq'
import type { RocketMqEnvironment } from '@/types/rocketmq'
import { normalizeDomain } from '@/utils/domain'
import { useEnvConfig } from '@/composables/useEnvConfig'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const { load: loadEnvConfig, save: saveEnvConfig, reset: resetEnvConfig } = useEnvConfig()
const envConfig = ref(loadEnvConfig())
const urlConfigExpanded = ref(false)

const getEnvConfig = () => envConfig.value

const handleSaveUrls = () => {
  saveEnvConfig(envConfig.value)
  ElMessage.success('URL 配置已保存')
}

const handleResetUrls = () => {
  envConfig.value = resetEnvConfig()
  ElMessage.info('已重置为默认 URL')
}

const currentEnv = ref<RocketMqEnvironment>('test')

const TOKEN_STORAGE_KEY = 'rocketmq_token'

const loadToken = (env: RocketMqEnvironment) => {
  return localStorage.getItem(`${TOKEN_STORAGE_KEY}_${env}`) || ''
}

const saveToken = (env: RocketMqEnvironment, value: string) => {
  localStorage.setItem(`${TOKEN_STORAGE_KEY}_${env}`, value)
}

const token = ref(loadToken(currentEnv.value))
const javaFilePath = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedJavaFile = ref<File | null>(null)
const pendingJavaAction = ref<'parse' | 'preview' | null>(null)
const topic = ref('')
const tag = ref('')
const key = ref('')
const traceEnabled = ref(false)
const messageBodyObj = ref<Record<string, any>>({})
const messageBodyJson = ref('{}')

const topicList = ref<string[]>([])
const isLoadingTopics = ref(false)
const isParsing = ref(false)
const isPreviewingJava = ref(false)
const javaPreviewVisible = ref(false)
const javaPreviewContent = ref('')
const javaHighlightedContent = computed(() => {
  if (!javaPreviewContent.value) return ''
  try {
    return hljs.highlight(javaPreviewContent.value, { language: 'java' }).value
  } catch {
    return javaPreviewContent.value
  }
})
const javaPreviewFilePath = ref('')
const isSending = ref(false)
const requestPreviewVisible = ref(false)
const bodyEditMode = ref(false)
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const lineNumbersRef = ref<HTMLElement | null>(null)

const lineCount = computed(() => {
  if (!messageBodyJson.value) return 1
  return messageBodyJson.value.split('\n').length
})

const onTextareaScroll = () => {
  if (textareaRef.value && lineNumbersRef.value) {
    lineNumbersRef.value.scrollTop = textareaRef.value.scrollTop
  }
}

const resultList = ref<Array<{
  success: boolean
  summary: string
  data: any
  expanded: boolean
}>>([])

const currentUrl = computed(() => (getEnvConfig() as Record<string, any>)[currentEnv.value].url)
const currentVersion = computed(() => (getEnvConfig() as Record<string, any>)[currentEnv.value].version)
const currentTokenLabel = computed(() => (getEnvConfig() as Record<string, any>)[currentEnv.value].tokenType)
const currentTokenPlaceholder = computed(() => {
  const cfg = (getEnvConfig() as Record<string, any>)[currentEnv.value]
  if (cfg.tokenType === 'XSRF-TOKEN') {
    return 'Cookie中XSRF-TOKEN的值'
  } else {
    return 'Cookie中JSESSIONID的值'
  }
})

const getCookie = () => {
  const cfg = (getEnvConfig() as Record<string, any>)[currentEnv.value]
  if (cfg.tokenType === 'XSRF-TOKEN') {
    return `XSRF-TOKEN=${token.value}`
  } else {
    return `NG_TRANSLATE_LANG_KEY=%22zh%22;JSESSIONID=${token.value}`
  }
}

const proxyHeaders = computed(() => {
  const cfg = (getEnvConfig() as Record<string, any>)[currentEnv.value]
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Cookie': getCookie(),
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',
    'Accept': '*/*',
    'Host': new URL(cfg.url).host,
    'Connection': 'keep-alive'
  }
  if (cfg.tokenType === 'XSRF-TOKEN') {
    headers['x-xsrf-token'] = token.value
  }
  return headers
})

const hasSuccess = computed(() => resultList.value.some(r => r.success))
const hasFail = computed(() => resultList.value.some(r => !r.success))

const canSend = computed(() => {
  return token.value && topic.value && tag.value && key.value && messageBodyJson.value && !isSending.value
})

const switchEnv = (env: RocketMqEnvironment) => {
  saveToken(currentEnv.value, token.value)
  currentEnv.value = env
  token.value = loadToken(env)
  refreshHeaders()
  topic.value = ''
  topicList.value = []
  if (token.value) {
    loadTopics()
  }
}

const refreshHeaders = () => {
}

watch(token, (val) => {
  saveToken(currentEnv.value, val)
})

const onTokenBlur = () => {
  if (token.value) {
    loadTopics()
  }
}

const loadTopics = async () => {
  if (!token.value) {
    ElMessage.error('请先输入 Token')
    return
  }
  isLoadingTopics.value = true
  const topicsUrl = (getEnvConfig() as Record<string, any>)[currentEnv.value].topicsUrl
  try {
    const response = await rocketmqApi.getTopics(topicsUrl, proxyHeaders.value, currentEnv.value)
    topicList.value = response.data?.topics || []
    if (topicList.value.length === 0) {
      ElMessage.warning('未获取到Topic列表，请检查Token是否有效')
    }
  } catch (error: any) {
    ElMessage.error('获取Topic列表失败: ' + (error.message || '未知错误'))
  } finally {
    isLoadingTopics.value = false
  }
}

const selectJavaFile = () => {
  fileInputRef.value?.click()
}

const onJavaFileSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  selectedJavaFile.value = file
  javaFilePath.value = file.name
  const action = pendingJavaAction.value
  pendingJavaAction.value = null
  if (action === 'parse') {
    await parseJavaFile()
  } else if (action === 'preview') {
    await previewJavaFileContent()
  }
}

const onJavaPathInput = () => {
  // 用户手动输入/粘贴路径时，清除已选文件，切换到路径解析模式
  selectedJavaFile.value = null
}

const ensureJavaSource = (action: 'parse' | 'preview'): boolean => {
  if (selectedJavaFile.value || (javaFilePath.value && javaFilePath.value.trim())) {
    return true
  }
  pendingJavaAction.value = action
  ElMessage.info('请选择Java文件或输入绝对路径')
  selectJavaFile()
  return false
}

const parseJavaFile = async () => {
  if (!ensureJavaSource('parse')) {
    return
  }
  isParsing.value = true
  try {
    const response = selectedJavaFile.value
      ? await rocketmqApi.parseJavaFileUpload(selectedJavaFile.value)
      : await rocketmqApi.parseJavaFile(javaFilePath.value.trim())
    tag.value = response.data?.tag || ''
    messageBodyObj.value = response.data?.messageBody || {}
    messageBodyJson.value = JSON.stringify(messageBodyObj.value)
    ElMessage.success('解析成功')
  } catch (error: any) {
    ElMessage.error('解析失败: ' + (error.message || '未知错误'))
  } finally {
    isParsing.value = false
  }
}

const previewJavaFileContent = async () => {
  if (!ensureJavaSource('preview')) {
    return
  }
  isPreviewingJava.value = true
  try {
    const response = selectedJavaFile.value
      ? await rocketmqApi.previewJavaFileUpload(selectedJavaFile.value)
      : await rocketmqApi.previewJavaFile(javaFilePath.value.trim())
    javaPreviewContent.value = response.data?.content || ''
    javaPreviewFilePath.value = response.data?.fileName || response.data?.filePath || javaFilePath.value
    javaPreviewVisible.value = true
  } catch (error: any) {
    ElMessage.error('预览失败: ' + (error.message || '未知错误'))
  } finally {
    isPreviewingJava.value = false
  }
}

const buildPayload = () => {
  return {
    topic: topic.value,
    key: key.value,
    tag: tag.value,
    messageBody: messageBodyJson.value,
    traceEnabled: traceEnabled.value
  }
}

const parseMessageBody = (body: any) => {
  if (typeof body === 'string') {
    try {
      return JSON.parse(body)
    } catch {
      return body
    }
  }
  return body
}

const buildPayloadForDisplay = () => {
  let bodyObj: any = messageBodyJson.value
  try {
    bodyObj = JSON.parse(messageBodyJson.value)
  } catch {
    bodyObj = messageBodyJson.value
  }
  return {
    topic: topic.value,
    key: key.value,
    tag: tag.value,
    messageBody: bodyObj,
    traceEnabled: traceEnabled.value
  }
}

const switchBodyMode = () => {
  if (bodyEditMode.value) {
    // 编辑 → 预览：校验 JSON，非法则阻止切换，避免展示不一致内容
    try {
      JSON.parse(messageBodyJson.value)
      bodyEditMode.value = false
    } catch (e: any) {
      ElMessage.error('JSON格式错误，无法切换预览: ' + e.message)
    }
  } else {
    bodyEditMode.value = true
  }
}

const formatMessageBody = () => {
  try {
    const obj = JSON.parse(messageBodyJson.value)
    messageBodyJson.value = JSON.stringify(obj, null, 2)
    ElMessage.success('格式化成功')
  } catch {
    ElMessage.error('JSON格式错误，无法格式化')
  }
}

const validateMessageBody = () => {
  try {
    JSON.parse(messageBodyJson.value)
    ElMessage.success('JSON格式正确')
  } catch (e: any) {
    ElMessage.error('JSON格式错误: ' + e.message)
  }
}

const clearMessageBody = () => {
  messageBodyObj.value = {}
  messageBodyJson.value = '{}'
  ElMessage.success('消息体已清空')
}

const formatJson = (obj: any) => {
  if (!obj) return ''
  return JSON.stringify(obj, null, 2)
}

const highlightJson = (obj: any) => {
  const text = formatJson(obj)
  if (!text) return ''
  try {
    return hljs.highlight(text, { language: 'json', ignoreIllegals: true }).value
  } catch {
    return text
  }
}

const resetForm = () => {
  javaFilePath.value = ''
  selectedJavaFile.value = null
  pendingJavaAction.value = null
  topic.value = ''
  tag.value = ''
  key.value = ''
  traceEnabled.value = false
  messageBodyObj.value = {}
  messageBodyJson.value = '{}'
  resultList.value = []
}

const MAX_HISTORY = 20

interface HistoryRecord {
  id: string
  timestamp: string
  success: boolean
  env: RocketMqEnvironment
  request: {
    targetUrl: string
    proxyHeaders: Record<string, string>
    topic: string
    key: string
    tag: string
    messageBody: string
    traceEnabled: boolean
  }
  response: any
  expanded: boolean
}

const historyList = ref<HistoryRecord[]>([])
const historyDialogVisible = ref(false)

const filteredHistoryList = computed(() => {
  return historyList.value.filter(h => h.env === currentEnv.value)
})

const formatTimestamp = (date: Date) => {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const addToHistory = (success: boolean, request: any, response: any) => {
  const record: HistoryRecord = {
    id: `${Date.now()}${Math.random().toString(36).slice(2, 6)}`,
    timestamp: formatTimestamp(new Date()),
    success,
    env: currentEnv.value,
    request: { ...request },
    response,
    expanded: false
  }
  historyList.value.unshift(record)
  const envRecords = historyList.value.filter(h => h.env === currentEnv.value)
  while (envRecords.length > MAX_HISTORY) {
    const lastIdx = historyList.value.lastIndexOf(envRecords[envRecords.length - 1])
    historyList.value.splice(lastIdx, 1)
  }
}

const removeFromHistory = (id: string) => {
  historyList.value = historyList.value.filter(h => h.id !== id)
}

const editFromHistory = (id: string) => {
  const record = historyList.value.find(h => h.id === id)
  if (!record) return
  currentEnv.value = (Object.keys(getEnvConfig()) as RocketMqEnvironment[]).find(
    k => (getEnvConfig() as Record<string, any>)[k].url === record.request.targetUrl
  ) || 'test'
  token.value = record.request.proxyHeaders.Cookie?.replace(/^(XSRF-TOKEN=|.*JSESSIONID=)/, '') || ''
  topic.value = record.request.topic
  tag.value = record.request.tag
  key.value = record.request.key
  traceEnabled.value = record.request.traceEnabled
  messageBodyJson.value = record.request.messageBody
  try {
    messageBodyObj.value = JSON.parse(record.request.messageBody)
  } catch {
    messageBodyObj.value = {}
  }
  historyDialogVisible.value = false
  ElMessage.success('已加载到表单，可编辑后发送')
}

const resendFromHistory = async (id: string) => {
  const record = historyList.value.find(h => h.id === id)
  if (!record) return
  historyDialogVisible.value = false
  isSending.value = true
  resultList.value = []
  const resultItem = {
    success: false,
    summary: '历史消息发送中...',
    data: null,
    expanded: false
  }
  resultList.value.push(resultItem)
  try {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    const response = await fetch('/api/tools/mq-send', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        targetUrl: record.request.targetUrl,
        proxyHeaders: record.request.proxyHeaders,
        topic: record.request.topic,
        key: record.request.key,
        tag: record.request.tag,
        messageBody: record.request.messageBody,
        traceEnabled: record.request.traceEnabled
      })
    })
    const data = await response.json()
    const isSuccess = data.data?.data?.sendStatus === 'SEND_OK'
    resultItem.success = isSuccess
    resultItem.summary = isSuccess ? '历史消息发送成功' : '历史消息发送失败'
    resultItem.data = data
    resultItem.expanded = true
    addToHistory(isSuccess, record.request, data)
    if (isSuccess) {
      ElMessage.success('历史消息发送成功')
    } else {
      ElMessage.error('历史消息发送失败: ' + (data.data?.errMsg || '未知错误'))
    }
  } catch (err: any) {
    resultItem.success = false
    resultItem.summary = '历史消息请求失败'
    resultItem.data = { error: err.message }
    resultItem.expanded = true
    addToHistory(false, record.request, { error: err.message })
    ElMessage.error('历史消息请求失败: ' + err.message)
  } finally {
    isSending.value = false
  }
}

const clearHistory = () => {
  historyList.value = historyList.value.filter(h => h.env !== currentEnv.value)
}

const handleSend = async () => {
  if (!token.value) {
    ElMessage.error('请输入 Token')
    return
  }
  if (!topic.value) {
    ElMessage.error('请选择Topic')
    return
  }
  if (!tag.value) {
    ElMessage.error('请输入Tag')
    return
  }
  if (!key.value) {
    ElMessage.error('请输入Key')
    return
  }
  if (!messageBodyJson.value) {
    ElMessage.error('请输入消息体')
    return
  }
  try {
    JSON.parse(messageBodyJson.value)
  } catch (e: any) {
    ElMessage.error('消息体JSON格式错误，请先修正后再发送: ' + e.message)
    return
  }

  isSending.value = true
  resultList.value = []

  const payload = buildPayload()
  const finalUrl = normalizeDomain(currentUrl.value)
  const requestData = {
    targetUrl: finalUrl,
    proxyHeaders: proxyHeaders.value,
    ...payload
  }

  const resultItem = {
    success: false,
    summary: '发送中...',
    data: null,
    expanded: false
  }
  resultList.value.push(resultItem)

  try {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }

    const response = await fetch('/api/tools/mq-send', {
      method: 'POST',
      headers,
      body: JSON.stringify(requestData)
    })

    const data = await response.json()
    const isSuccess = data.data?.data?.sendStatus === 'SEND_OK'

    resultItem.success = isSuccess
    resultItem.summary = isSuccess ? '发送成功' : '发送失败'
    resultItem.data = data
    resultItem.expanded = true

    addToHistory(isSuccess, requestData, data)

    if (isSuccess) {
      ElMessage.success('消息发送成功')
    } else {
      ElMessage.error('消息发送失败: ' + (data.data?.errMsg || '未知错误'))
    }
  } catch (err: any) {
    resultItem.success = false
    resultItem.summary = '请求失败'
    resultItem.data = { error: err.message }
    resultItem.expanded = true

    addToHistory(false, requestData, { error: err.message })

    ElMessage.error('请求失败: ' + err.message)
  } finally {
    isSending.value = false
  }
}

watch(messageBodyJson, (val) => {
  try {
    if (val) {
      JSON.parse(val)
    }
  } catch {
    // 编辑过程中的中间状态允许 JSON 不完整，此处仅校验不报错
  }
})
</script>

<style scoped>
.rocketmq-container {
  padding: .625rem 1rem;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: .5rem;
  flex: 1;
  overflow-y: auto;
}

.panels-row {
  display: flex;
  gap: .5rem;
  flex: 1;
  min-height: 0;
}

.panels-row .preview-panel.featured-panel {
  flex: 0 0 35%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.panels-row .send-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.config-panel,
.preview-panel,
.send-panel,
.result-panel {
  background: #fff;
  border-radius: .375rem;
  padding: .625rem .75rem;
  box-shadow: 0 .0625rem .375rem 0 rgba(0, 0, 0, 0.08);
}

.preview-panel.featured-panel {
  border: .125rem solid #409eff;
  box-shadow: 0 .25rem .75rem 0 rgba(64, 158, 255, 0.15);
}

.preview-panel.featured-panel .panel-title {
  background: linear-gradient(to right, #409eff, #66b1ff);
  color: #fff;
  margin: -.625rem -.75rem .375rem -.75rem;
  padding: .5rem .75rem;
  border-radius: .375rem .375rem 0 0;
  border-bottom: none;
}

.preview-panel.featured-panel .panel-title .el-button {
  background: #fff;
  color: #409eff;
  border-color: #fff;
}

.preview-panel.featured-panel .panel-title .el-button:hover {
  background: #ecf5ff;
  color: #409eff;
  border-color: #ecf5ff;
}

.preview-panel.featured-panel .panel-title .btn-danger {
  color: #f56c6c;
}

.preview-panel.featured-panel .panel-title .btn-danger:hover {
  background: #fef0f0;
  color: #f56c6c;
  border-color: #fef0f0;
}

.config-row {
  display: flex;
  gap: 1rem;
  margin-bottom: .5rem;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: .25rem;
}

.config-item label {
  font-size: .6875rem;
  color: #909399;
}

.switch-group {
  display: flex;
  gap: .25rem;
}

.url-bar {
  background: #f5f7fa;
  border: .0625rem solid #dcdfe6;
  border-radius: .25rem;
  padding: .3125rem .5rem;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: .6875rem;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: .375rem;
  margin-bottom: .5rem;
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
  grid-template-columns: 1fr 1fr 1fr;
  gap: .375rem .75rem;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: .1875rem;
}

.form-item.full-width {
  grid-column: 1 / -1;
}

.form-item.span-2 {
  grid-column: span 2;
}

.form-item label {
  font-size: .6875rem;
  font-weight: 500;
  color: #606266;
}

.form-item.key-field {
  background: #ecf5ff;
  border: .0625rem solid #d9ecff;
  border-radius: .25rem;
  padding: .375rem .5rem;
  border-left: .1875rem solid #409eff;
  box-shadow: 0 .125rem .25rem 0 rgba(64, 158, 255, 0.1);
}

.form-item.key-field label {
  color: #409eff;
  font-weight: 600;
}

.required {
  color: #f56c6c;
}

.input-group {
  display: flex;
  gap: .25rem;
  align-items: center;
}

.footer-hint {
  font-size: .625rem;
  color: #909399;
  margin-top: .375rem;
}

.highlight-red {
  color: #f56c6c;
  font-weight: 600;
}

.panel-title {
  font-size: .75rem;
  font-weight: 600;
  color: #303133;
  margin-bottom: .375rem;
  padding-bottom: .25rem;
  border-bottom: .0625rem solid #ebeef5;
}

.preview-content {
  display: flex;
  gap: .625rem;
}

.panels-row .send-panel .preview-content {
  display: flex;
  gap: .5rem;
  flex: 1;
  min-height: 0;
}

.panels-row .preview-panel.featured-panel .preview-content {
  display: block;
  flex: 1;
  min-height: 0;
}

.headers-preview {
  flex: 0 0 auto;
  min-width: 15rem;
  max-width: 18rem;
  background: #fafafa;
  border: .0625rem solid #ebeef5;
  border-radius: .25rem;
  padding: .5rem;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: .75rem;
  line-height: 1.5;
  height: 100%;
  overflow-y: auto;
}

.header-line {
  margin-bottom: .125rem;
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
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: .75rem;
  line-height: 1.5;
  height: 100%;
  position: relative;
}

.popup-preview-btn {
  position: absolute;
  top: .25rem;
  right: .25rem;
  z-index: 1;
}

.request-preview-body {
  max-height: none;
  height: auto;
  overflow: visible;
}

.body-preview {
  margin: 0;
  height: 100%;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.json-box {
  background: #fafafa;
  border: .0625rem solid #ebeef5;
  border-radius: .25rem;
  padding: .5rem;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: .625rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  max-height: 15.625rem;
  overflow-y: auto;
}

/* 请求预览面板内覆盖：放大字号并撑满高度（不影响响应结果/历史弹窗） */
.panels-row .json-box {
  font-size: .75rem;
  max-height: none;
  height: 100%;
  box-sizing: border-box;
}

.json-editor {
  flex: 1;
  min-width: 0;
  height: 100%;
}

.json-textarea {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: .6875rem;
  line-height: 1.5;
  resize: none;
}

.send-action {
  display: flex;
  gap: .5rem;
  justify-content: flex-end;
  margin-top: .5rem;
  flex-shrink: 0;
}

.result-panel {
  border-left: .1875rem solid #dcdfe6;
  flex-shrink: 0;
  margin-top: .5rem;
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
  gap: .25rem;
  max-height: 15.625rem;
  overflow-y: auto;
}

.result-item {
  background: #fafafa;
  border: .0625rem solid #ebeef5;
  border-radius: .25rem;
  padding: .375rem .5rem;
  cursor: pointer;
  transition: background 0.15s;
  font-size: .75rem;
  border-left: .1875rem solid transparent;
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
  gap: .5rem;
}

.result-item-icon {
  font-size: .75rem;
}

.result-item-summary {
  font-size: .6875rem;
  color: #909399;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-item-toggle {
  font-size: .5625rem;
  color: #409eff;
  transition: transform 0.2s;
}

.result-item-toggle.expanded {
  transform: rotate(90deg);
}

.result-item-body {
  margin-top: .375rem;
}

.empty-hint {
  text-align: center;
  color: #909399;
  font-size: .6875rem;
  padding: .625rem 0;
}

.body-toolbar {
  display: flex;
  gap: .25rem;
  align-items: center;
}

.body-editor-wrapper {
  display: flex;
  border: .0625rem solid #dcdfe6;
  border-radius: .25rem;
  overflow: hidden;
  height: 100%;
}

.line-numbers {
  width: 2.5rem;
  background: #f5f7fa;
  border-right: .0625rem solid #dcdfe6;
  text-align: center;
  padding: .5rem .25rem;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: .625rem;
  line-height: 1.5;
  color: #909399;
  overflow: hidden;
  user-select: none;
}

.line-number {
  height: .9375rem;
}

.body-editor-textarea {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: .625rem;
  line-height: 1.5;
  resize: none;
  flex: 1;
  height: 100%;
  padding: .5rem;
  border: none;
  outline: none;
  box-sizing: border-box;
  background: #fff;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.version-tag {
  margin-left: auto;
  font-size: .625rem;
  color: #909399;
  background: #f0f2f5;
  padding: .125rem .375rem;
  border-radius: .25rem;
  font-weight: 500;
}

.history-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: .5rem;
}

.history-count {
  font-size: .75rem;
  color: #606266;
  font-weight: 500;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: .5rem;
  max-height: 31.25rem;
  overflow-y: auto;
}

.history-item {
  background: #fafafa;
  border: .0625rem solid #ebeef5;
  border-radius: .375rem;
  padding: .5rem;
  border-left: .1875rem solid transparent;
}

.history-item.success {
  border-left-color: #67c23a;
}

.history-item.fail {
  border-left-color: #f56c6c;
}

.history-item-header {
  display: flex;
  align-items: center;
  gap: .5rem;
}

.history-item-header-left {
  display: flex;
  align-items: center;
  gap: .5rem;
  flex: 1;
  min-width: 0;
  cursor: pointer;
  user-select: none;
}

.history-status {
  font-size: .6875rem;
  font-weight: 600;
  padding: .125rem .375rem;
  border-radius: .25rem;
  color: #fff;
}

.history-status.status-success {
  background: #67c23a;
}

.history-status.status-fail {
  background: #f56c6c;
}

.history-time {
  font-size: .6875rem;
  color: #606266;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.history-summary {
  font-size: .6875rem;
  color: #909399;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-toggle {
  font-size: .5625rem;
  color: #409eff;
  transition: transform 0.2s;
}

.history-toggle.expanded {
  transform: rotate(90deg);
}

.history-item-body {
  margin-top: .5rem;
  display: flex;
  flex-direction: column;
  gap: .5rem;
}

.history-section {
  background: #fff;
  border: .0625rem solid #ebeef5;
  border-radius: .25rem;
  padding: .375rem .5rem;
}

.history-section-title {
  font-size: .625rem;
  color: #606266;
  font-weight: 600;
  margin-bottom: .25rem;
}

.history-section-content {
  max-height: none;
  overflow: visible;
}

.history-section-content .json-box {
  max-height: none;
  overflow: visible;
}

.history-section-content-full {
  max-height: none;
  overflow: visible;
}

.history-item-actions {
  display: flex;
  justify-content: flex-end;
  gap: .375rem;
  flex-shrink: 0;
}

.json-box-full {
  max-height: none;
  overflow: visible;
}

.java-preview-header {
  background: #f5f7fa;
  border: .0625rem solid #dcdfe6;
  border-radius: .25rem;
  padding: .375rem .5rem;
  margin-bottom: .5rem;
}

.java-preview-path {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: .6875rem;
  color: #606266;
  word-break: break-all;
}

.java-preview-wrapper {
  border: .0625rem solid #dcdfe6;
  border-radius: .25rem;
  overflow: auto;
  max-height: 34.375rem;
}

.java-preview-code {
  margin: 0;
  padding: .75rem;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: .75rem;
  line-height: 1.6;
  background: #fff;
  white-space: pre;
  overflow-x: auto;
}

.java-preview-code code {
  background: transparent !important;
  padding: 0;
  font-family: inherit;
}

.url-config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  cursor: pointer;
  user-select: none;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 8px;
}

.url-config-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.url-config-body {
  padding: 8px 0 12px;
}

.url-config-env {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.url-config-env-label {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  min-width: 40px;
}

.url-config-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.hidden-file-input {
  display: none;
}

.file-picker-hint {
  color: #409eff;
  font-size: 12px;
  font-weight: 500;
  user-select: none;
  cursor: pointer;
}

.file-picker-hint:hover {
  color: #66b1ff;
  text-decoration: underline;
}
</style>