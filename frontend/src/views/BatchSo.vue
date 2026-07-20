<template>
  <div class="batch-so-container">
    <div class="page-header">
      <div class="header-actions">
        <el-button size="small" @click="showHistory = true; hasNewHistory = false" class="history-btn">
          创建历史
          <el-badge v-if="hasNewHistory" :value="1" class="history-badge" />
        </el-button>
      </div>
    </div>

    <div class="main-content">
      <div class="left-panel">
        <!-- 环境切换 -->
        <div class="form-section">
          <div class="form-row">
            <label>环境</label>
            <div class="env-switch">
              <el-button size="small" :type="currentEnv === 'dev' ? 'primary' : ''" @click="switchEnv('dev')">dev</el-button>
              <el-button size="small" :type="currentEnv === 'test' ? 'primary' : ''" @click="switchEnv('test')">test</el-button>
            </div>
          </div>
          <div class="form-row">
            <label>请求地址</label>
            <el-input size="small" v-model="apiUrl" placeholder="请输入请求地址" />
          </div>
        </div>

        <!-- 基本配置 -->
        <div class="form-section">
          <div class="form-row">
            <label><span class="required">*</span>前缀</label>
            <el-input size="small" v-model="prefix" maxlength="4" style="width: 60px" @input="updatePreview" />
            <label>批量</label>
            <el-input-number size="small" v-model="batchCount" :min="1" :max="20" @change="updatePreview" @wheel.prevent="handleBatchWheel" />
            <label>起始</label>
            <el-input-number size="small" v-model="startNumber" :min="1" :max="99" disabled />
          </div>
          <div class="form-row">
            <el-checkbox size="small" v-model="satellite">卫星仓</el-checkbox>
            <span class="satellite-info" :class="{ active: satellite }">
              ✓ 广州白云仓
            </span>
          </div>
        </div>

        <!-- Authorization -->
        <div class="form-section">
          <div class="form-row">
            <label><span class="required">*</span>Auth</label>
            <el-input size="small" v-model="authorization" placeholder="Authorization值" />
            <el-button size="small" type="primary" :loading="isSubmitting" @click="handleSubmit">
              {{ isSubmitting ? '创建中...' : '开始创建' }}
            </el-button>
            <el-button size="small" type="success" disabled>
              开始接单
            </el-button>
          </div>
        </div>

        <!-- 字段配置 -->
        <div class="form-section">
          <div class="section-title">必填默认字段</div>
          <div class="field-config">
            <div class="config-hint">值可点击修改</div>
            <div v-for="(value, key) in modifiedFields" :key="key" class="config-row">
              <span class="config-label">* {{ getFieldLabel(key) }}</span>
              <el-input
                v-if="editingField === key"
                v-model="modifiedFields[key]"
                size="small"
                @blur="editingField = null"
                @keyup.enter="editingField = null"
              />
              <span v-else class="config-value" @click="editingField = key">{{ value }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="right-panel">
        <!-- 单号预览 -->
        <div class="preview-section">
          <div class="section-title">进仓单号预览</div>
          <div class="preview-list">
            <el-tag v-for="no in previewBookingNos" :key="no" size="small" class="preview-item" :type="getPreviewTagType(no)">
              {{ no }}
            </el-tag>
            <div v-if="previewBookingNos.length === 0" class="empty-hint">请选择批量数量和起始编号查看预览</div>
          </div>
        </div>

        <!-- 结果面板 -->
        <div class="result-section" :class="{ success: resultSuccess, error: resultError }">
          <div class="result-header">
            <span class="result-icon">{{ resultSuccess ? '✓' : resultError ? '✗' : '...' }}</span>
            <span>{{ resultMessage }}</span>
          </div>
          <div class="result-stats">
            <span>总数：<strong>{{ totalCount }}</strong></span>
            <span>成功：<strong>{{ successCount }}</strong></span>
            <span>失败：<strong>{{ errorCount }}</strong></span>
          </div>
          <div class="result-list">
            <div v-for="(item, index) in resultList" :key="index" class="result-item" :class="item.status">
              <div class="result-item-header" @click="toggleExpand(index)">
                <span class="expand-icon">
                  <el-icon v-if="expandedResults[index]"><ArrowDown /></el-icon>
                  <el-icon v-else><ArrowRight /></el-icon>
                </span>
                <span class="booking-no">{{ item.bookingNo }}</span>
                <el-tag :type="item.status === 'success' ? 'success' : 'danger'" size="small">
                  {{ item.status === 'success' ? '成功' : '失败' }}
                </el-tag>
              </div>
              <div v-if="expandedResults[index]" class="result-detail">
                <div class="detail-section">
                  <div class="detail-label">请求体：</div>
                  <pre class="detail-json">{{ JSON.stringify(item.requestBody, null, 2) }}</pre>
                </div>
                <div class="detail-section">
                  <div class="detail-label">响应值：</div>
                  <pre class="detail-json">{{ JSON.stringify(item.responseData, null, 2) }}</pre>
                </div>
              </div>
            </div>
            <div v-if="resultList.length === 0 && !isSubmitting && resultMessage" class="empty-hint">暂无结果</div>
            <div v-if="resultList.length === 0 && !isSubmitting && !resultMessage" class="empty-hint">点击【开始创建】后此处显示请求列表</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 历史记录弹窗 -->
    <el-dialog v-model="showHistory" title="创建历史" width="600px">
      <div class="history-content">
        <div v-if="historyList.length === 0" class="empty-hint">暂无创建历史</div>
        <div v-for="(group, time) in groupedHistory" :key="time" class="history-group">
          <div class="history-time">{{ time }}</div>
          <div class="history-items">
            <el-tag v-for="item in group" :key="item.no" size="small" class="history-item">
              {{ item.no }}
              <el-icon class="copy-icon" @click="copyToClipboard(item.no)"><CopyDocument /></el-icon>
            </el-tag>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, ArrowDown, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import axios from 'axios'

const userStore = useUserStore()

// 状态
const currentEnv = ref('dev')
const apiUrl = ref('')
const prefix = ref('TEST')
const batchCount = ref(1)
const startNumber = ref(1)
const satellite = ref(false)
const authorization = ref('')
const isSubmitting = ref(false)
const isReceiving = ref(false)
const editingField = ref<string | null>(null)
const expandedResults = ref<Record<number, boolean>>({})
const showHistory = ref(false)
const hasNewHistory = ref(false)

// 字段配置
const modifiedFields = ref<Record<string, string>>({
  forwarderCode: 'KN',
  consigneeCode: 'KN-SEX',
  payerCode: 'KNSZ',
  warehouseCode: 'PLA',
  freightForwarder: '张三',
  packageQty: '100',
  packageType: 'CTN',
  volume: '2',
  grossWeight: '500',
  destinationCountry: 'ABW',
  markCode: 'TEST',
  customerId: '1'
})

// 结果状态
const previewBookingNos = ref<string[]>([])
const resultList = ref<Array<{ bookingNo: string; status: string; statusText: string; requestBody?: any; responseData?: any; statusCode?: number }>>([])
const totalCount = ref(0)
const successCount = ref(0)
const errorCount = ref(0)
const resultSuccess = ref(false)
const resultError = ref(false)
const resultMessage = ref('')

// 历史记录
const historyList = ref<Array<{ no: string; time: string }>>([])

// 计算属性
const groupedHistory = computed(() => {
  const grouped: Record<string, Array<{ no: string; time: string }>> = {}
  historyList.value.forEach(item => {
    // 按日期+小时:分钟分组，如 "2026-07-17 19:20"
    const timeKey = item.time ? item.time.substring(0, 16) : '未知时间'
    if (!grouped[timeKey]) {
      grouped[timeKey] = []
    }
    grouped[timeKey].push(item)
  })
  return grouped
})

// API配置（根据当前用户动态生成）
const getApiUrls = () => {
  const brand = userStore.currentUser?.username || 'example'
  return {
    dev: `http://dev.${brand}.org/api/om/v1/shipping-order/submit`,
    test: `https://uat.${brand}.com/api/om/v1/shipping-order/submit`
  }
}

// 方法
const switchEnv = (env: string) => {
  currentEnv.value = env
  apiUrl.value = getApiUrls()[env as keyof ReturnType<typeof getApiUrls>]
  const savedAuth = localStorage.getItem(`shippingOrderAuthorization_${env}`)
  authorization.value = savedAuth || ''
  updatePreview()
}

const updatePreview = () => {
  const today = new Date()
  const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '')
  previewBookingNos.value = []

  for (let i = 0; i < batchCount.value; i++) {
    const num = startNumber.value + i
    previewBookingNos.value.push(`${prefix.value}${dateStr}${String(num).padStart(2, '0')}`)
  }
}

const getFieldLabel = (key: string) => {
  const labels: Record<string, string> = {
    forwarderCode: '货代',
    consigneeCode: '收货方',
    payerCode: '分公司',
    warehouseCode: '目标仓库',
    type: '业务类型',
    freightForwarder: '货代联系人',
    packageQty: '包装数量',
    packageType: '包装类型',
    volume: '预计体积',
    grossWeight: '预计重量',
    destinationCountry: '目的国',
    markCode: '唛头',
    batteryGoods: '纯电池',
    customerId: 'CustomerId'
  }
  return labels[key] || key
}

const getPreviewTagType = (no: string) => {
  const item = resultList.value.find(r => r.bookingNo === no)
  if (!item) return ''
  return item.status === 'success' ? 'success' : item.status === 'error' ? 'danger' : 'info'
}

const handleSubmit = async () => {
  if (!apiUrl.value) {
    ElMessage.error('请输入请求地址')
    return
  }
  if (!authorization.value) {
    ElMessage.error('请输入Authorization值')
    return
  }

  isSubmitting.value = true
  resultList.value = []
  expandedResults.value = {}
  totalCount.value = batchCount.value
  successCount.value = 0
  errorCount.value = 0
  resultSuccess.value = false
  resultError.value = false
  resultMessage.value = '正在创建...'

  try {
    const token = userStore.token
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch('/api/tools/batch-so/submit', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        apiUrl: apiUrl.value,
        batchCount: batchCount.value,
        startNumber: startNumber.value,
        prefix: prefix.value,
        satellite: satellite.value,
        authorization: authorization.value,
        modifiedFields: modifiedFields.value
      })
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`请求失败 (${response.status}): ${errorText}`)
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader!.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      // 兼容 \r\n 换行
      buffer = buffer.replace(/\r\n/g, '\n')
      const messages = buffer.split('\n\n')
      buffer = messages.pop() || ''

      for (const message of messages) {
        if (!message.trim()) continue

        const lines = message.split('\n')
        let eventType = ''
        let eventData: any = null

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventType = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            const dataStr = line.substring(5).trim()
            if (dataStr) {
              try {
                eventData = JSON.parse(dataStr)
              } catch (e) {
                eventData = { raw: dataStr }
              }
            }
          }
        }

        if (eventType && eventData) {
          if (eventType === 'start') {
            // 开始事件
          } else if (eventType === 'success') {
            resultList.value = [...resultList.value, {
              bookingNo: eventData.bookingNo,
              status: 'success',
              statusText: '成功',
              requestBody: eventData.body,
              responseData: eventData.response
            }]
            successCount.value++
            resultMessage.value = `已完成 ${successCount.value + errorCount.value}/${totalCount.value}`
          } else if (eventType === 'error') {
            resultList.value = [...resultList.value, {
              bookingNo: eventData.bookingNo,
              status: 'error',
              statusText: eventData.error?.message || eventData.error || '失败',
              requestBody: eventData.body,
              responseData: eventData.response || eventData.error
            }]
            errorCount.value++
            resultMessage.value = `已完成 ${successCount.value + errorCount.value}/${totalCount.value}`
          } else if (eventType === 'complete') {
            resultSuccess.value = eventData.success
            resultError.value = !eventData.success
            resultMessage.value = eventData.success ? '批量创建成功！' : '部分创建失败'
            hasNewHistory.value = true
            loadHistory()
          }
        }
      }
    }

    if (resultMessage.value === '正在创建...') {
      resultMessage.value = '批量创建完成'
      hasNewHistory.value = true
      loadHistory()
    }
  } catch (error: any) {
    ElMessage.error('请求失败: ' + error.message)
    resultMessage.value = '请求失败: ' + error.message
  } finally {
    isSubmitting.value = false
  }
}

const handleReceive = async () => {
  if (!apiUrl.value) {
    ElMessage.error('请输入请求地址')
    return
  }
  if (!authorization.value) {
    ElMessage.error('请输入Authorization值')
    return
  }
  if (previewBookingNos.value.length === 0) {
    ElMessage.error('请先创建进仓单或选择单号预览')
    return
  }

  isReceiving.value = true
  resultList.value = []
  totalCount.value = previewBookingNos.value.length
  successCount.value = 0
  errorCount.value = 0
  resultSuccess.value = false
  resultError.value = false
  resultMessage.value = '正在接单...'

  try {
    const token = userStore.token
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch('/api/tools/batch-so/receive', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        apiUrl: apiUrl.value,
        bookingNos: previewBookingNos.value,
        satellite: satellite.value,
        authorization: authorization.value
      })
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`请求失败 (${response.status}): ${errorText}`)
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader!.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const messages = buffer.split('\n\n')
      buffer = messages.pop() || ''

      for (const message of messages) {
        if (!message.trim()) continue

        const lines = message.split('\n')
        let eventType = ''
        let eventData: any = null

        for (const line of lines) {
          if (line.startsWith('event: ')) {
            eventType = line.substring(7).trim()
          } else if (line.startsWith('data: ')) {
            const dataStr = line.substring(6).trim()
            if (dataStr) {
              eventData = JSON.parse(dataStr)
            }
          }
        }

        if (eventType && eventData) {
          if (eventType === 'receive-success') {
            resultList.value.push({
              bookingNo: eventData.bookingNo,
              status: 'success',
              statusText: '接单成功'
            })
            successCount.value++
          } else if (eventType === 'receive-error') {
            resultList.value.push({
              bookingNo: eventData.bookingNo,
              status: 'error',
              statusText: '接单失败'
            })
            errorCount.value++
          } else if (eventType === 'receive-complete') {
            resultSuccess.value = eventData.success
            resultError.value = !eventData.success
            resultMessage.value = eventData.success ? '批量接单成功！' : '部分接单失败'
          }
        }
      }
    }
  } catch (error: any) {
    ElMessage.error('接单请求失败: ' + error.message)
  } finally {
    isReceiving.value = false
  }
}

const loadHistory = () => {
  const token = userStore.token
  const headers: Record<string, string> = {}
  if (token) headers['Authorization'] = `Bearer ${token}`

  axios.get(`/api/tools/so-history?env=${currentEnv.value}`, { headers })
    .then(res => {
      historyList.value = res.data.data || []
      calcStartNumberFromHistory()
    })
    .catch(() => {
      historyList.value = []
    })
}

const calcStartNumberFromHistory = () => {
  if (historyList.value.length === 0) {
    startNumber.value = 1
    return
  }

  // 今天日期字符串 YYYYMMDD
  const today = new Date()
  const todayStr = today.getFullYear().toString() +
    String(today.getMonth() + 1).padStart(2, '0') +
    String(today.getDate()).padStart(2, '0')

  // 筛选今天日期的 SO，取最大尾号
  let maxNum = 0
  historyList.value.forEach(item => {
    const match = item.no.match(/(\d{8})(\d{2})$/)
    if (match && match[1] === todayStr) {
      const num = parseInt(match[2], 10)
      if (num > maxNum) maxNum = num
    }
  })

  startNumber.value = maxNum + 1
}

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

const handleBatchWheel = (e: WheelEvent) => {
  e.preventDefault()
  const delta = e.deltaY < 0 ? 1 : -1
  const newVal = batchCount.value + delta
  if (newVal >= 1 && newVal <= 20) {
    batchCount.value = newVal
    updatePreview()
  }
}

const toggleExpand = (index: number) => {
  expandedResults.value[index] = !expandedResults.value[index]
}

const fetchRecords = async () => {
  try {
    const token = userStore.token
    const headers: Record<string, string> = {}
    if (token) headers['Authorization'] = `Bearer ${token}`

    const res = await axios.get(`/api/tools/so-records?env=${currentEnv.value}`, { headers })
    const records = res.data.data || []
    resultList.value = records.map((r: any) => ({
      bookingNo: r.bookingNo,
      status: r.status,
      statusText: r.status === 'success' ? '成功' : '失败',
      requestBody: r.requestBody,
      responseData: r.response
    }))
    successCount.value = records.filter((r: any) => r.status === 'success').length
    errorCount.value = records.filter((r: any) => r.status === 'error').length
    totalCount.value = records.length
  } catch (error: any) {
    ElMessage.error('获取记录失败: ' + (error.message || error))
  }
}

// 监听环境变化
watch(currentEnv, () => {
  loadHistory()
})

// 监听弹窗打开时刷新历史
watch(showHistory, (val) => {
  if (val) {
    loadHistory()
    hasNewHistory.value = false
  }
})

// 监听 Auth 变化自动保存
watch(authorization, (val) => {
  localStorage.setItem(`shippingOrderAuthorization_${currentEnv.value}`, val || '')
})

// 初始化
onMounted(() => {
  switchEnv('dev')
  const savedAuth = localStorage.getItem(`shippingOrderAuthorization_${currentEnv.value}`)
  if (savedAuth) {
    authorization.value = savedAuth
  }
  updatePreview()
  loadHistory()
})
</script>

<style scoped>
.batch-so-container {
  padding: 10px 16px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 8px;
}

.main-content {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.left-panel {
  flex: 0 0 40%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.right-panel {
  flex: 0 0 60%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.preview-section {
  flex: 0 0 auto;
  max-height: 20%;
  overflow-y: auto;
  background: #fff;
  border-radius: 6px;
  padding: 10px 12px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.08);
}

.result-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-left: 3px solid #dcdfe6;
  background: #fff;
  border-radius: 6px;
  padding: 10px 12px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.08);
}

.form-section {
  background: #fff;
  border-radius: 6px;
  padding: 10px 12px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.08);
}

.form-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.form-row:last-child {
  margin-bottom: 0;
}

.form-row label {
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
  font-size: 12px;
}

.required {
  color: #f56c6c;
}

.env-switch {
  display: flex;
  gap: 4px;
}

.satellite-info {
  font-size: 11px;
  color: #909399;
}

.satellite-info.active {
  color: #67c23a;
}

.section-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  font-size: 13px;
}

.field-config {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 8px;
}

.config-hint {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
  padding-bottom: 4px;
  border-bottom: 1px dashed #dcdfe6;
}

.config-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 3px 6px;
  border-radius: 3px;
  margin-bottom: 2px;
}

.config-row:hover {
  background: #ecf5ff;
}

.config-label {
  font-size: 11px;
  color: #606266;
  font-weight: 500;
}

.config-value {
  font-size: 11px;
  color: #303133;
  cursor: pointer;
  padding: 1px 6px;
  border-radius: 3px;
  transition: background 0.2s;
}

.config-value:hover {
  background: #e3f2fd;
}

.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  min-height: 40px;
  align-items: flex-start;
  align-content: flex-start;
}

.preview-item {
  font-family: 'Consolas', monospace;
}

.result-section.success {
  border-left-color: #67c23a;
  background: #f0f9eb;
}

.result-section.error {
  border-left-color: #f56c6c;
  background: #fef0f0;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  margin-bottom: 6px;
  font-size: 13px;
}

.result-icon {
  font-size: 14px;
}

.result-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #606266;
}

.result-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.result-item {
  display: flex;
  flex-direction: column;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 3px;
  margin-bottom: 3px;
  font-size: 12px;
}

.result-item-header {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.expand-icon {
  display: flex;
  align-items: center;
  font-size: 10px;
  color: #909399;
}

.result-item.success {
  border-left: 2px solid #67c23a;
}

.result-item.error {
  border-left: 2px solid #f56c6c;
}

.result-detail {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed #dcdfe6;
}

.detail-section {
  margin-bottom: 6px;
}

.detail-label {
  font-weight: 500;
  color: #606266;
  margin-bottom: 3px;
  font-size: 11px;
}

.detail-json {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 3px;
  padding: 6px 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  line-height: 1.4;
  color: #303133;
  overflow-x: auto;
  max-height: 200px;
  overflow-y: auto;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.booking-no {
  font-family: 'Consolas', monospace;
  font-weight: 500;
  font-size: 12px;
}

.empty-hint {
  text-align: center;
  color: #909399;
  font-size: 12px;
  padding: 12px 0;
}

.history-btn {
  position: relative;
}

.history-badge {
  position: absolute;
  top: -6px;
  right: -6px;
}

.history-content {
  max-height: 400px;
  overflow-y: auto;
}

.history-group {
  margin-bottom: 12px;
}

.history-time {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.history-items {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.history-item {
  cursor: pointer;
}

.copy-icon {
  margin-left: 4px;
  cursor: pointer;
}

.copy-icon:hover {
  color: #409eff;
}
</style>