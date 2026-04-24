<template>
  <div class="search-page">
    <div class="page-header">
      <h2>全局搜索</h2>
    </div>

    <!-- 搜索栏 -->
    <div class="search-container">
      <el-input
        v-model="keyword"
        placeholder="输入关键字搜索..."
        size="large"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button type="primary" @click="handleSearch" :loading="searchStore.isSearching">
            搜索
          </el-button>
        </template>
      </el-input>
    </div>

    <!-- 搜索历史 -->
    <div v-if="searchStore.searchHistory.length" class="search-history">
      <h4>搜索历史</h4>
      <div class="history-tags">
        <el-tag
          v-for="(item, index) in searchStore.searchHistory.slice(0, 10)"
          :key="index"
          @click="keyword = item; handleSearch()"
          class="history-tag"
        >
          {{ item }}
        </el-tag>
      </div>
      <el-button type="danger" link @click="searchStore.clearHistory">清空历史</el-button>
    </div>

    <!-- 搜索结果 -->
    <div v-if="searchStore.searchResult" class="search-result">
      <div class="result-header">
        <span>找到 {{ searchStore.searchResult.total }} 条结果</span>
      </div>

      <!-- 环境结果 -->
      <div v-if="searchStore.searchResult.environments.length" class="result-section">
        <h3><el-icon><Monitor /></el-icon> 环境 ({{ searchStore.searchResult.environments.length }})</h3>
        <el-card v-for="item in searchStore.searchResult.environments" :key="item.id" class="result-item">
          <div class="item-title" @click="showEnvironmentDetail(item)">{{ item.name }}</div>
          <div class="item-desc">{{ item.description }}</div>
          <div class="item-details">
            <el-button type="primary" link @click="copyEnvironmentInfo(item)">文本复制</el-button>
            <el-tag size="small" :type="item.status ? 'success' : 'danger'">
              {{ item.status ? '启用' : '禁用' }}
            </el-tag>
            <el-tag size="small" class="ml-2">{{ item.type }}</el-tag>
            <span class="item-meta">创建时间: {{ formatDate(item.createdAt) }}</span>
          </div>
        </el-card>
      </div>

      <!-- 组件结果 -->
      <div v-if="searchStore.searchResult.components.length" class="result-section">
        <h3><el-icon><Connection /></el-icon> 技术组件 ({{ searchStore.searchResult.components.length }})</h3>
        <el-card v-for="item in searchStore.searchResult.components" :key="item.id" class="result-item">
          <div class="item-title" @click="showComponentDetail(item)">{{ item.name }}</div>
          <div class="item-desc">{{ item.description }}</div>
          <div class="item-details">
            <el-button type="primary" link @click="copyComponentInfo(item)">文本复制</el-button>
            <el-tag size="small" :type="item.status ? 'success' : 'danger'">
              {{ item.status ? '启用' : '禁用' }}
            </el-tag>
            <el-tag size="small" class="ml-2">{{ item.category }}</el-tag>
            <span class="item-meta">创建时间: {{ formatDate(item.createdAt) }}</span>
          </div>
        </el-card>
      </div>

      <!-- 流程结果 -->
      <div v-if="searchStore.searchResult.processes.length" class="result-section">
        <h3><el-icon><Document /></el-icon> 业务流程 ({{ searchStore.searchResult.processes.length }})</h3>
        <el-card v-for="item in searchStore.searchResult.processes" :key="item.id" class="result-item">
          <div class="item-title" @click="showProcessDetail(item)">{{ item.name }}</div>
          <div class="item-desc">{{ item.description }}</div>
          <div class="item-details">
            <el-button type="primary" link @click="copyProcessInfo(item)">文本复制</el-button>
            <el-tag size="small" :type="item.status ? 'success' : 'danger'">
              {{ item.status ? '启用' : '禁用' }}
            </el-tag>
            <el-tag size="small" class="ml-2">{{ item.category }}</el-tag>
            <el-tag size="small" class="ml-2" :type="getPriorityType(item.priority)">{{ getPriorityText(item.priority) }}</el-tag>
            <span class="item-meta">创建时间: {{ formatDate(item.createdAt) }}</span>
          </div>
        </el-card>
      </div>

      <!-- 代码片段结果 -->
      <div v-if="searchStore.searchResult.snippets && searchStore.searchResult.snippets.length" class="result-section">
        <h3><el-icon><Document /></el-icon> 代码片段 ({{ searchStore.searchResult.snippets.length }})</h3>
        <el-card v-for="item in searchStore.searchResult.snippets" :key="item.id" class="result-item">
          <div class="snippet-header">
            <div class="item-title" @click="showSnippetDetail(item)">{{ item.title }}</div>
            <el-button type="primary" link @click="copySnippetCode(item.code)">复制代码</el-button>
          </div>
          <div class="item-desc">{{ item.description || '无描述' }}</div>
          <!-- 显示代码片段内容预览 -->
          <div v-if="item.code" class="code-preview">
            <pre class="code-snippet">{{ getCodePreview(item.code) }}</pre>
          </div>
          <div class="item-details">
            <el-button type="primary" link @click="copySnippetInfo(item)">文本复制</el-button>
            <el-tag size="small" :type="getLanguageTagType(item.language)">
              {{ getLanguageLabel(item.language) }}
            </el-tag>
            <el-tag size="small" class="ml-2" :type="(item.status ?? true) ? 'success' : 'danger'">
              {{ (item.status ?? true) ? '启用' : '禁用' }}
            </el-tag>
            <span class="item-meta">创建时间: {{ formatDate(item.createdAt) }}</span>
          </div>
        </el-card>
      </div>

      <!-- 代码仓库结果 -->
      <div v-if="searchStore.searchResult.projects.length" class="result-section">
        <h3><el-icon><Folder /></el-icon> 代码仓库 ({{ searchStore.searchResult.projects.length }})</h3>
        <el-card v-for="item in searchStore.searchResult.projects" :key="item.id" class="result-item">
          <div class="item-title" @click="showProjectDetail(item)">{{ item.name }}</div>
          <div class="item-desc">{{ item.description }}</div>
          <div class="item-details">
            <el-button type="primary" link @click="copyProjectInfo(item)">文本复制</el-button>
            <el-tag size="small" :type="item.status ? 'success' : 'danger'">
              {{ item.status ? '启用' : '禁用' }}
            </el-tag>
            <el-tag size="small" class="ml-2">{{ item.fullName }}</el-tag>
            <span class="item-meta">创建时间: {{ formatDate(item.createdAt) }}</span>
          </div>
        </el-card>
      </div>

      <!-- 无结果 -->
      <el-empty v-if="searchStore.searchResult.total === 0" description="未找到相关结果" />
    </div>

    <!-- 环境详情对话框 -->
    <el-dialog
      v-model="environmentDialogVisible"
      title="环境详情"
      width="600px"
      destroy-on-close
    >
      <el-form :model="selectedEnvironment" label-width="100px">
        <el-form-item label="环境名称">
          <el-input v-model="selectedEnvironment.name" disabled />
        </el-form-item>
        <el-form-item label="环境类型">
          <el-input v-model="selectedEnvironment.type" disabled />
        </el-form-item>
        <el-form-item label="访问地址">
          <el-input v-model="selectedEnvironment.url" disabled />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="selectedEnvironment.username" disabled />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="selectedEnvironment.password" disabled type="password" show-password />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="selectedEnvironment.description" type="textarea" :rows="3" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="environmentDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 代码片段详情对话框 -->
    <el-dialog
      v-model="snippetDialogVisible"
      title="代码片段详情"
      width="800px"
      destroy-on-close
    >
      <el-form :model="selectedSnippet" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="selectedSnippet.title" disabled />
        </el-form-item>
        <el-form-item label="编程语言">
          <el-input v-model="selectedSnippet.language" disabled />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="selectedSnippet.tags" disabled />
        </el-form-item>
        <el-form-item label="代码">
          <div class="code-preview">
            <div class="code-header">
              <span class="language-tag">{{ getLanguageLabel(selectedSnippet.language) }}</span>
              <el-button type="primary" link @click="copySnippetCode(selectedSnippet.code)">复制代码</el-button>
            </div>
            <pre><code :class="`language-${selectedSnippet.language}`" v-html="getHighlightedCode(selectedSnippet.code, selectedSnippet.language)"></code></pre>
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="selectedSnippet.description" type="textarea" :rows="2" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="snippetDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 组件详情对话框 -->
    <el-dialog
      v-model="componentDialogVisible"
      title="技术组件详情"
      width="600px"
      destroy-on-close
    >
      <el-form :model="selectedComponent" label-width="100px">
        <el-form-item label="组件名称">
          <el-input v-model="selectedComponent.name" disabled />
        </el-form-item>
        <el-form-item label="组件类型">
          <el-input v-model="selectedComponent.category" disabled />
        </el-form-item>
        <el-form-item label="访问地址">
          <el-input v-model="selectedComponent.url" disabled />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="selectedComponent.username" disabled />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="selectedComponent.password" disabled type="password" show-password />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="selectedComponent.version" disabled />
        </el-form-item>
        <el-form-item label="配置信息">
          <el-input v-model="selectedComponent.configuration" type="textarea" :rows="3" disabled />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="selectedComponent.notes" type="textarea" :rows="2" disabled />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="selectedComponent.description" type="textarea" :rows="2" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="componentDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 流程详情对话框 -->
    <el-dialog
      v-model="processDialogVisible"
      title="业务流程详情"
      width="700px"
      destroy-on-close
    >
      <el-form :model="selectedProcess" label-width="100px">
        <el-form-item label="流程名称">
          <el-input v-model="selectedProcess.name" disabled />
        </el-form-item>
        <el-form-item label="流程类型">
          <el-input v-model="selectedProcess.category" disabled />
        </el-form-item>
        <el-form-item label="优先级">
          <el-tag :type="getPriorityType(selectedProcess.priority)">{{ getPriorityText(selectedProcess.priority) }}</el-tag>
        </el-form-item>
        <el-form-item label="流程步骤">
          <el-input v-model="selectedProcess.steps" type="textarea" :rows="4" disabled />
        </el-form-item>
        <el-form-item label="注意事项">
          <el-input v-model="selectedProcess.precautions" type="textarea" :rows="3" disabled />
        </el-form-item>
        <el-form-item label="检查清单">
          <el-input v-model="selectedProcess.checklist" type="textarea" :rows="3" disabled />
        </el-form-item>
        <el-form-item label="相关文档">
          <el-input v-model="selectedProcess.relatedDocuments" type="textarea" :rows="2" disabled />
        </el-form-item>
        <el-form-item label="流程说明">
          <el-input v-model="selectedProcess.description" type="textarea" :rows="2" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 项目详情对话框 -->
    <el-dialog
      v-model="projectDialogVisible"
      title="代码仓库详情"
      width="600px"
      destroy-on-close
    >
      <el-form :model="selectedProject" label-width="100px">
        <el-form-item label="项目名称">
          <el-input v-model="selectedProject.name" disabled />
        </el-form-item>
        <el-form-item label="项目全称">
          <el-input v-model="selectedProject.projectFullName" disabled />
        </el-form-item>
        <el-form-item label="代码仓库">
          <el-input v-model="selectedProject.codeRepository" disabled />
        </el-form-item>
        <el-form-item label="文档路径">
          <el-input v-model="selectedProject.documentPath" disabled />
        </el-form-item>
        <el-form-item label="部署路径">
          <el-input v-model="selectedProject.deploymentPath" disabled />
        </el-form-item>
        <el-form-item label="端口">
          <el-input v-model="selectedProject.port" disabled />
        </el-form-item>
        <el-form-item label="团队成员">
          <el-input v-model="selectedProject.teamMembers" type="textarea" :rows="3" disabled />
        </el-form-item>
        <el-form-item label="项目状态">
          <el-input v-model="selectedProject.status" disabled />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="selectedProject.notes" type="textarea" :rows="2" disabled />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="selectedProject.description" type="textarea" :rows="2" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="projectDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Monitor, Connection, Document, Folder } from '@element-plus/icons-vue'
import { useSearchStore } from '@/stores/search'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { ElMessage } from 'element-plus'
import type { Environment } from '@/types/environment'
import type { CodeSnippet } from '@/types/snippet'
import type { TechnicalComponent } from '@/types/component'
import type { BusinessProcess } from '@/types/process'
import type { Repository } from '@/types/project'

const route = useRoute()
const searchStore = useSearchStore()
const keyword = ref('')

// 对话框控制
const environmentDialogVisible = ref(false)
const snippetDialogVisible = ref(false)
const componentDialogVisible = ref(false)
const processDialogVisible = ref(false)
const projectDialogVisible = ref(false)
const selectedEnvironment = ref<Environment>({} as Environment)
const selectedSnippet = ref<CodeSnippet>({} as CodeSnippet)
const selectedComponent = ref<TechnicalComponent>({} as TechnicalComponent)
const selectedProcess = ref<BusinessProcess>({} as BusinessProcess)
const selectedProject = ref<Repository>({} as Repository)

const handleSearch = () => {
  if (keyword.value.trim()) {
    searchStore.search(keyword.value.trim())
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getPriorityType = (priority: number) => {
  const typeMap = {
    1: 'danger',
    2: 'warning',
    3: 'success'
  }
  return typeMap[priority] || 'info'
}

const getPriorityText = (priority: number) => {
  const textMap = {
    1: '高',
    2: '中',
    3: '低'
  }
  return `优先级: ${textMap[priority] || '未知'}`
}

const getLanguageTagType = (language: string) => {
  const map: Record<string, string> = {
    java: 'danger',
    vue: 'success',
    javascript: 'warning',
    sql: 'info'
  }
  return map[language] || ''
}

const getLanguageLabel = (language: string) => {
  const map: Record<string, string> = {
    java: 'Java',
    vue: 'Vue',
    javascript: 'JavaScript',
    sql: 'SQL'
  }
  return map[language] || language
}

const getCodePreview = (code: string) => {
  if (!code) return ''
  // 取代码的前100个字符作为预览
  const preview = code.substring(0, 100)
  return preview.length < code.length ? preview + '...' : preview
}

const getHighlightedCode = (code: string, language: string) => {
  if (!code || !language) return code

  try {
    const lang = language === 'javascript' ? 'javascript' :
                language === 'vue' ? 'xml' :
                language

    return hljs.highlight(code, { language: lang }).value
  } catch (error) {
    return code
  }
}

const copySnippetCode = async (code: string) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('代码已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
}

const showEnvironmentDetail = (env: Environment) => {
  selectedEnvironment.value = { ...env }
  environmentDialogVisible.value = true
}

const showSnippetDetail = (snippet: CodeSnippet) => {
  selectedSnippet.value = { ...snippet }
  snippetDialogVisible.value = true
}

const showComponentDetail = (component: TechnicalComponent) => {
  selectedComponent.value = { ...component }
  componentDialogVisible.value = true
}

const showProcessDetail = (process: BusinessProcess) => {
  selectedProcess.value = { ...process }
  processDialogVisible.value = true
}

const showProjectDetail = (project: Repository) => {
  selectedProject.value = { ...project }
  projectDialogVisible.value = true
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
}

const copyEnvironmentInfo = async (env: Environment) => {
  const info = `
${env.name}-${env.type}
地址: \`${env.url || '-'}\`
账号: ${env.username || '-'}
密码: ${env.password || '-'}
  `.trim()
  await copyToClipboard(info)
}

const copySnippetInfo = async (snippet: CodeSnippet) => {
  const info = `
${snippet.title}
语言: ${getLanguageLabel(snippet.language)}
描述: ${snippet.description || '-'}
代码: ${snippet.code}
标签: ${snippet.tags || '-'}
  `.trim()
  await copyToClipboard(info)
}

const copyComponentInfo = async (component: TechnicalComponent) => {
  const info = `
${component.name}-${component.category}
地址: \`${component.url || '-'}\`
账号: ${component.username || '-'}
密码: ${component.password || '-'}
版本: ${component.version || '-'}
配置: ${component.configuration || '-'}
备注: ${component.notes || '-'}
  `.trim()
  await copyToClipboard(info)
}

const copyProcessInfo = async (process: BusinessProcess) => {
  const info = `
${process.name}-${process.category}
优先级: ${getPriorityText(process.priority)}
描述: ${process.description || '-'}
步骤: ${process.steps || '-'}
注意事项: ${process.precautions || '-'}
检查清单: ${process.checklist || '-'}
相关文档: ${process.relatedDocuments || '-'}
  `.trim()
  await copyToClipboard(info)
}

const copyProjectInfo = async (project: Repository) => {
  const info = `
${project.name}
项目全称: ${project.projectFullName || '-'}
代码仓库: \`${project.codeRepository || '-'}\`
文档路径: \`${project.documentPath || '-'}\`
部署路径: \`${project.deploymentPath || '-'}\`
端口: ${project.port || '-'}
团队成员: ${project.teamMembers || '-'}
状态: ${project.status || '-'}
备注: ${project.notes || '-'}
  `.trim()
  await copyToClipboard(info)
}

// 从URL参数中获取搜索关键词
onMounted(() => {
  const urlKeyword = route.query.q as string
  if (urlKeyword) {
    keyword.value = urlKeyword
    searchStore.search(urlKeyword)
  }
})
</script>

<style scoped>
.search-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.search-container {
  max-width: 600px;
  margin: 0 auto 30px;
}

.search-history {
  margin-bottom: 30px;
}

.search-history h4 {
  margin-bottom: 10px;
  color: #666;
}

.history-tags {
  margin-bottom: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 40px;
  overflow: hidden;
}

.history-tag {
  cursor: pointer;
  white-space: nowrap;
}

.history-tag:hover {
  opacity: 0.8;
}

.search-result {
  margin-top: 20px;
}

.result-header {
  margin-bottom: 20px;
  color: #666;
}

.result-section {
  margin-bottom: 30px;
}

.result-section h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  color: #333;
}

.result-item {
  margin-bottom: 10px;
  cursor: pointer;
}

.result-item:hover {
  border-color: var(--el-color-primary);
}

.snippet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.item-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  cursor: pointer;
  flex: 1;
}

.item-title:hover {
  color: var(--el-color-primary);
}

.item-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-preview {
  margin: 10px 0;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

.code-snippet {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #333;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 60px;
  overflow: hidden;
}

.code-preview {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #f8f9fa;
  max-height: 400px;
  overflow: auto;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background-color: #f1f3f4;
  border-bottom: 1px solid #dcdfe6;
}

.language-tag {
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  background-color: #e9ecef;
  padding: 2px 8px;
  border-radius: 4px;
}

.code-preview pre {
  margin: 0;
  padding: 16px;
  background: transparent;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
}

.code-preview code {
  font-family: inherit;
}

.item-details {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.item-meta {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.ml-2 {
  margin-left: 8px;
}
</style>
