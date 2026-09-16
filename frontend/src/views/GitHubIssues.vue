<template>
  <div class="github-issues">
    <div class="page-header">
      <div>
        <h2 class="page-title">GitHub 指派任务</h2>
        <p class="page-desc">
          展示指派给当前 GitHub 账号的 Issue 与 Bug，生成可直接复制给 AI Agent 的提示词（每 5 分钟自动刷新）
        </p>
      </div>
      <div class="header-actions">
        <span v-if="lastRefreshAt" class="refresh-time">最近刷新：{{ lastRefreshAt }}</span>
        <el-select v-model="stateFilter" class="state-select" aria-label="状态筛选">
          <el-option label="全部" value="all" />
          <el-option label="进行中" value="open" />
          <el-option label="已关闭" value="closed" />
        </el-select>
        <el-button :loading="loading" @click="refresh()">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button @click="openConfig">
          <el-icon><Setting /></el-icon>
          配置
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-tabs v-model="activeTab" class="issue-tabs">
        <el-tab-pane :label="`全部 ${stateFilteredIssues.length}`" name="all" />
        <el-tab-pane :label="`Issue ${featureIssues.length}`" name="feature" />
        <el-tab-pane :label="`Bug ${bugIssues.length}`" name="bug" />
      </el-tabs>

      <el-table :data="filteredIssues" v-loading="loading" stripe>
        <el-table-column label="编号" width="90" align="center">
          <template #default="{ row }">
            <a class="issue-link" :href="row.htmlUrl" target="_blank" rel="noopener">#{{ row.number }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="issue-title" @click="openDetail(row)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isBug" type="danger" size="small" effect="plain">Bug</el-tag>
            <el-tag v-else type="primary" size="small" effect="plain">Issue</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.state === 'open' ? 'success' : 'info'" size="small">
              {{ row.state === 'open' ? '进行中' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="label in row.labels" :key="label" size="small" type="info" effect="plain" class="label-tag">
              {{ label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="150">
          <template #default="{ row }">
            <span class="text-muted">{{ formatTime(row.updatedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button link type="success" size="small" @click="openPrompt(row)">生成提示词</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty v-if="!loading" :description="emptyText">
            <el-button v-if="!configLoaded || !config.tokenConfigured" type="primary" @click="openConfig">
              前往配置
            </el-button>
          </el-empty>
        </template>
      </el-table>
    </el-card>

    <!-- 配置抽屉 -->
    <el-drawer v-model="configVisible" title="GitHub 配置" size="480px">
      <el-form label-width="110px">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="config-alert"
          title="Token 为 GitHub Classic PAT，需授权目标仓库的 Issues 和 Bugs 的只读权限"
        />
        <el-form-item label="组织">
          <el-input v-model="configForm.owner" placeholder="如 LeaderrunTeam" />
        </el-form-item>
        <el-form-item label="仓库">
          <el-input v-model="configForm.repo" placeholder="如 pm" />
        </el-form-item>
        <el-form-item label="Token">
          <el-input
            v-model="configForm.token"
            type="password"
            show-password
            :placeholder="configForm.tokenConfigured ? '留空表示不修改（当前已配置）' : '填写 Classic PAT'"
          />
        </el-form-item>
        <el-form-item label="API 基地址">
          <el-input v-model="configForm.baseUrl" placeholder="https://api.github.com" />
        </el-form-item>
        <el-form-item label="代理">
          <div class="proxy-row">
            <el-input v-model="configForm.proxyHost" placeholder="主机（可选）" />
            <el-input v-model="configForm.proxyPort" placeholder="端口" style="width: 110px" />
          </div>
        </el-form-item>
        <el-form-item label="Bug 标签">
          <el-input v-model="configForm.bugLabels" placeholder="bug（多个用英文逗号分隔）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :loading="testing" @click="handleTestConnection">测试连接</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveConfig">保存</el-button>
      </template>
    </el-drawer>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" size="720px" class="detail-drawer">
      <div v-if="detail" v-loading="detailLoading" class="detail-body">
        <div class="detail-head">
          <div class="detail-title-row">
            <span class="detail-number">#{{ detail.number }}</span>
            <h3 class="detail-title">{{ detail.title }}</h3>
          </div>
          <div class="detail-meta">
            <el-tag :type="detail.isBug ? 'danger' : 'primary'" size="small" effect="plain">
              {{ detail.isBug ? 'Bug' : 'Issue' }}
            </el-tag>
            <el-tag :type="detail.state === 'open' ? 'success' : 'info'" size="small">
              {{ detail.state === 'open' ? '进行中' : '已关闭' }}
            </el-tag>
            <el-tag v-for="label in detail.labels" :key="label" size="small" type="info" effect="plain">
              {{ label }}
            </el-tag>
            <span class="text-muted meta-time">更新于 {{ formatTime(detail.updatedAt) }}</span>
            <a class="issue-link" :href="detail.htmlUrl" target="_blank" rel="noopener">在 GitHub 打开</a>
          </div>
        </div>

        <el-alert
          v-for="(warning, i) in detailWarnings"
          :key="i"
          type="warning"
          :title="warning"
          :closable="false"
          show-icon
          class="detail-alert"
        />

        <section class="detail-section">
          <h4 class="section-title">正文</h4>
          <div class="markdown-body" v-html="renderMarkdown(detail.body || '（无正文）')" />
        </section>

        <section v-if="detail.images.length" class="detail-section">
          <h4 class="section-title">图片（{{ detail.images.length }}）</h4>
          <div v-for="(img, i) in detail.images" :key="i" class="image-block">
            <el-image
              v-if="img.downloaded && imageBlobUrls[img.previewUrl]"
              :src="imageBlobUrls[img.previewUrl]"
              :preview-src-list="previewList"
              :initial-index="previewIndex(img)"
              :preview-teleported="true"
              fit="contain"
              class="detail-image"
            />
            <div v-else class="image-miss">
              <span class="text-muted">{{ img.note || '图片加载中…' }}</span>
              <a :href="img.sourceUrl" target="_blank" rel="noopener">查看原图</a>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <h4 class="section-title">评论（{{ detail.comments.length }}）</h4>
          <el-empty v-if="!detail.comments.length" description="无评论" :image-size="56" />
          <div v-for="(comment, i) in detail.comments" :key="i" class="comment-item">
            <div class="comment-head">
              <span class="comment-author">{{ comment.author }}</span>
              <span class="text-muted">{{ formatTime(comment.createdAt) }}</span>
            </div>
            <div class="markdown-body" v-html="renderMarkdown(comment.body)" />
          </div>
        </section>
      </div>
    </el-drawer>

    <!-- 提示词弹窗 -->
    <el-dialog
      v-model="promptVisible"
      :title="promptResult ? `#${promptResult.number} AI 提示词` : '生成 AI 提示词'"
      class="prompt-dialog"
      width="min(1180px, 94vw)"
      top="3vh"
      :close-on-click-modal="false"
    >
      <div v-if="promptResult" v-loading="promptLoading" class="prompt-body">
        <el-alert
          v-for="(warning, i) in promptResult.warnings"
          :key="i"
          type="warning"
          :title="warning"
          :closable="false"
          show-icon
        />
        <div class="prompt-actions">
          <el-button type="success" :loading="promptSaving" @click="savePrompt">
            <el-icon><Check /></el-icon>
            保存<el-tag v-if="promptDirty" size="small" type="warning" effect="light" class="dirty-tag">未保存</el-tag>
          </el-button>
          <el-button type="primary" @click="copyPrompt">
            <el-icon><CopyDocument /></el-icon>
            复制
          </el-button>
          <el-button @click="downloadPrompt">
            <el-icon><Download /></el-icon>
            下载 .md
          </el-button>
          <el-button :loading="promptLoading" @click="regeneratePrompt">重新生成</el-button>
        </div>
        <el-input
          v-model="editablePrompt"
          type="textarea"
          class="prompt-textarea"
          @input="onPromptInput"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import MarkdownIt from 'markdown-it'
import { useUserStore } from '@/stores/user'
import {
  getGitHubConfig,
  saveGitHubConfig,
  testGitHubConnection,
  getGitHubIssues,
  getGitHubIssueDetail,
  buildGitHubPrompt,
  getCustomPrompt,
  saveCustomPrompt
} from '@/api/github'
import type {
  GitHubConfig,
  GitHubImage,
  GitHubIssue,
  GitHubIssueDetail,
  GitHubPromptResult
} from '@/types/github'

const AUTO_REFRESH_MS = 5 * 60 * 1000

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

// ===== 列表 =====
const issues = ref<GitHubIssue[]>([])
const loading = ref(false)
const activeTab = ref('all')
const stateFilter = ref('open')
const lastRefreshAt = ref('')
const config = ref<GitHubConfig>({})
const configLoaded = ref(false)

// 先按状态过滤，再按类型 Tab 过滤
const stateFilteredIssues = computed(() =>
  stateFilter.value === 'all' ? issues.value : issues.value.filter((i) => i.state === stateFilter.value)
)
const bugIssues = computed(() => stateFilteredIssues.value.filter((i) => i.isBug))
const featureIssues = computed(() => stateFilteredIssues.value.filter((i) => !i.isBug))
const filteredIssues = computed(() => {
  if (activeTab.value === 'bug') return bugIssues.value
  if (activeTab.value === 'feature') return featureIssues.value
  return stateFilteredIssues.value
})
const emptyText = computed(() => {
  if (configLoaded.value && !config.value.tokenConfigured) return '尚未配置 GitHub Token，请先完成配置'
  if (stateFilter.value !== 'all') return '当前状态下暂无任务，可切换为「全部」查看'
  return '暂无指派给你的 Issue'
})

async function refresh(silent = false) {
  loading.value = true
  try {
    const resp: any = await getGitHubIssues()
    issues.value = resp.data || []
    lastRefreshAt.value = dayjs().format('HH:mm:ss')
  } catch (e) {
    if (!silent) return
    console.warn('GitHub 指派任务自动刷新失败：', e)
  } finally {
    loading.value = false
  }
}

async function loadConfig() {
  try {
    const resp: any = await getGitHubConfig()
    config.value = resp.data || {}
    configLoaded.value = true
  } catch {
    configLoaded.value = true
  }
}

// ===== 配置 =====
const configVisible = ref(false)
const configForm = ref<GitHubConfig>({})
const saving = ref(false)
const testing = ref(false)

function openConfig() {
  configForm.value = { ...config.value, token: '' }
  configVisible.value = true
}

async function handleSaveConfig() {
  saving.value = true
  try {
    const resp: any = await saveGitHubConfig({ ...configForm.value })
    config.value = resp.data || {}
    configLoaded.value = true
    ElMessage.success('配置保存成功')
    configVisible.value = false
  } catch {
    // 错误消息由拦截器统一提示
  } finally {
    saving.value = false
  }
}

async function handleTestConnection() {
  testing.value = true
  try {
    const resp: any = await testGitHubConnection()
    ElMessage.success(`连接成功，当前 GitHub 账号：${resp.data?.login || '-'}`)
  } catch {
    // 错误消息由拦截器统一提示
  } finally {
    testing.value = false
  }
}

// ===== 详情 =====
const detailVisible = ref(false)
const detail = ref<GitHubIssueDetail | null>(null)
const detailLoading = ref(false)
const imageBlobUrls = ref<Record<string, string>>({})

// 详情抽屉同时展示截断类与图片类警告（提示词只带前者）
const detailWarnings = computed(() => [
  ...(detail.value?.warnings || []),
  ...(detail.value?.imageWarnings || [])
])

async function openDetail(row: GitHubIssue) {
  detailVisible.value = true
  detail.value = null
  detailLoading.value = true
  try {
    const resp: any = await getGitHubIssueDetail(row.number)
    detail.value = resp.data
    loadImageBlobs(resp.data?.images || [])
  } catch {
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function loadImageBlobs(images: GitHubImage[]) {
  // 用原生 fetch 直连静态映射路径，避免 axios 的 /api 前缀干扰
  const token = useUserStore().token
  await Promise.all(
    images.map(async (img) => {
      if (!img.downloaded || !img.previewUrl || imageBlobUrls.value[img.previewUrl]) return
      try {
        const resp = await fetch(img.previewUrl, {
          headers: { Authorization: `Bearer ${token}` }
        })
        if (!resp.ok) return
        imageBlobUrls.value[img.previewUrl] = URL.createObjectURL(await resp.blob())
      } catch {
        // 单张图片加载失败不阻断整体展示
      }
    })
  )
}

const previewList = computed(() =>
  (detail.value?.images || [])
    .filter((img) => img.downloaded && imageBlobUrls.value[img.previewUrl])
    .map((img) => imageBlobUrls.value[img.previewUrl])
)

function previewIndex(img: GitHubImage) {
  return Math.max(0, previewList.value.indexOf(imageBlobUrls.value[img.previewUrl]))
}

// ===== 提示词 =====
const promptVisible = ref(false)
const promptResult = ref<GitHubPromptResult | null>(null)
const promptLoading = ref(false)
const promptSaving = ref(false)
const editablePrompt = ref('')
const promptDirty = ref(false)
let promptNumber = 0

async function openPrompt(row: GitHubIssue) {
  promptNumber = row.number
  promptResult.value = null
  editablePrompt.value = ''
  promptDirty.value = false
  promptVisible.value = true
  // 优先加载自定义版本，没有再走生成
  try {
    const custom: any = await getCustomPrompt(row.number)
    if (custom?.data?.prompt) {
      editablePrompt.value = custom.data.prompt
      promptResult.value = { number: row.number, title: row.title, htmlUrl: '', prompt: custom.data.prompt, warnings: [] }
      return
    }
  } catch {
    // 读取失败忽略，继续生成
  }
  await generatePrompt(false)
}

async function regeneratePrompt() {
  await generatePrompt(true)
}

async function generatePrompt(reload: boolean) {
  promptLoading.value = true
  try {
    const resp: any = await buildGitHubPrompt(promptNumber)
    promptResult.value = resp.data
    editablePrompt.value = resp.data.prompt
    promptDirty.value = false
    if (reload) {
      ElMessage.success('提示词已重新生成（未保存的编辑将被覆盖）')
    }
  } catch {
    if (!promptResult.value) {
      promptVisible.value = false
    }
  } finally {
    promptLoading.value = false
  }
}

function onPromptInput() {
  promptDirty.value = true
}

async function savePrompt() {
  if (!editablePrompt.value || !promptNumber) return
  promptSaving.value = true
  try {
    await saveCustomPrompt(promptNumber, editablePrompt.value)
    promptDirty.value = false
    ElMessage.success('提示词已保存')
  } finally {
    promptSaving.value = false
  }
}

async function copyPrompt() {
  const text = editablePrompt.value
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('提示词已复制到剪贴板')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('提示词已复制到剪贴板')
  }
}

function downloadPrompt() {
  const content = editablePrompt.value
  if (!content) return
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `#${promptNumber}-AI提示词.md`
  link.click()
  URL.revokeObjectURL(url)
}

// Ctrl/Cmd + S 保存
function onKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's' && promptVisible.value) {
    e.preventDefault()
    savePrompt()
  }
}

onMounted(async () => {
  await loadConfig()
  refresh()
  autoTimer = window.setInterval(() => refresh(true), AUTO_REFRESH_MS)
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  if (autoTimer) {
    clearInterval(autoTimer)
  }
  window.removeEventListener('keydown', onKeydown)
  Object.values(imageBlobUrls.value).forEach((url) => URL.revokeObjectURL(url))
})

function renderMarkdown(text: string) {
  return md.render(text || '')
}

function formatTime(value: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '—'
}

// ===== 5 分钟自动刷新 =====
let autoTimer: number | undefined

onMounted(async () => {
  await loadConfig()
  refresh()
  autoTimer = window.setInterval(() => refresh(true), AUTO_REFRESH_MS)
})

onUnmounted(() => {
  if (autoTimer) {
    clearInterval(autoTimer)
  }
  Object.values(imageBlobUrls.value).forEach((url) => URL.revokeObjectURL(url))
})
</script>

<style scoped>
.github-issues {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 6px;
}

.page-desc {
  color: #86909c;
  font-size: 13px;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.refresh-time {
  color: #86909c;
  font-size: 12px;
}

.state-select {
  width: 100px;
}

.issue-link {
  color: #409eff;
  text-decoration: none;
  font-weight: 600;
}

.issue-title {
  cursor: pointer;
  color: #409eff;
}

.issue-title:hover {
  color: #66b1ff;
  text-decoration: underline;
}

.label-tag {
  margin-right: 4px;
}

.text-muted {
  color: #86909c;
  font-size: 12px;
}

.config-alert {
  margin-bottom: 16px;
}

.proxy-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.detail-drawer :deep(.el-drawer__body) {
  padding: 20px 24px;
}

.detail-body {
  display: flex;
  flex-direction: column;
}

.detail-head {
  margin-bottom: 16px;
}

.detail-title-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
}

.detail-number {
  color: #909399;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.detail-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.5;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-time {
  margin-left: 2px;
}

.detail-alert {
  margin-bottom: 12px;
}

.detail-section {
  border-top: 1px solid #ebeef5;
  padding: 16px 0 4px;
  margin-bottom: 16px;
}

html.dark .detail-section {
  border-top-color: #3a3a3d;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 12px;
  color: #303133;
}

html.dark .section-title {
  color: #e5eaf3;
}

/* ===== Markdown 排版 ===== */
.markdown-body {
  line-height: 1.75;
  font-size: 14px;
  word-break: break-word;
  color: #303133;
}

html.dark .markdown-body {
  color: #d5d8de;
}

.markdown-body :deep(p) {
  margin: 8px 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 16px 0 8px;
  font-weight: 600;
  line-height: 1.4;
}

.markdown-body :deep(h1) {
  font-size: 20px;
}

.markdown-body :deep(h2) {
  font-size: 17px;
}

.markdown-body :deep(h3) {
  font-size: 15px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 22px;
  margin: 8px 0;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

.markdown-body :deep(blockquote) {
  margin: 10px 0;
  padding: 6px 14px;
  border-left: 3px solid #409eff;
  background: rgba(64, 158, 255, 0.06);
  color: #606266;
  border-radius: 0 6px 6px 0;
}

html.dark .markdown-body :deep(blockquote) {
  color: #a8abb2;
}

.markdown-body :deep(code) {
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 13px;
}

html.dark .markdown-body :deep(code) {
  background: rgba(255, 255, 255, 0.1);
}

.markdown-body :deep(pre) {
  background: #f6f8fa;
  padding: 12px 14px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 10px 0;
}

html.dark .markdown-body :deep(pre) {
  background: #1f1f22;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  margin: 10px 0;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #e5e7eb;
  padding: 6px 12px;
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 14px 0;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 6px;
}

/* ===== 图片 ===== */
.image-block {
  margin-bottom: 14px;
}

.detail-image {
  width: 100%;
  max-height: 320px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  background-color: #f5f7fa;
}

html.dark .detail-image {
  border-color: #3a3a3d;
  background-color: #1f1f22;
}

.image-miss {
  padding: 14px 16px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

html.dark .image-miss {
  border-color: #3a3a3d;
}

/* ===== 评论 ===== */
.comment-item {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
}

html.dark .comment-item {
  border-color: #3a3a3d;
}

.comment-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.comment-author {
  font-weight: 600;
  font-size: 13px;
}

.dirty-tag {
  margin-left: 4px;
}
</style>

<!--
  提示词弹窗：整体占满一屏高度，内容区撑满剩余空间
  弹窗内部元素由子组件经 teleport 渲染，不带本组件作用域属性，scoped 无法命中，故用全局样式并收敛在 .prompt-dialog 下
-->
<style>
.prompt-dialog {
  display: flex;
  flex-direction: column;
  height: 92vh;
  margin-bottom: 0 !important;
}

.prompt-dialog .el-dialog__header {
  flex: 0 0 auto;
  padding: 14px 20px 8px;
  margin: 0;
}

.prompt-dialog .el-dialog__body {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  padding: 12px 20px 16px;
  overflow: hidden;
}

.prompt-dialog .prompt-body {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  gap: 10px;
}

/* 警告不参与高度分配，避免被压缩 */
.prompt-dialog .prompt-body > .el-alert {
  flex: 0 0 auto;
}

.prompt-dialog .prompt-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 0 0 auto;
}

/* 内容区撑满弹窗剩余高度，内部滚动条仅在超长时出现 */
.prompt-dialog .prompt-textarea {
  position: relative;
  flex: 1 1 auto;
  min-height: 200px;
}

.prompt-dialog .prompt-textarea .el-textarea__inner {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  resize: none;
}
</style>