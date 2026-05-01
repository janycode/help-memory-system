<template>
  <div class="dashboard">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">首页</h1>
        <p class="page-description">欢迎回来！这里是您的{{ welcomeText }}</p>
      </div>

      <el-card class="global-search-card">
        <div class="search-layout">
          <!-- 搜索框 -->
          <div class="search-box-section">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索环境、组件、流程、代码仓库、代码片段..."
              clearable
              @keyup.enter="handleGlobalSearch"
              class="search-input"
            >
              <template #prefix>
                <el-icon class="search-icon"><Search /></el-icon>
              </template>
              <template #append>
                <el-button type="primary" @click="handleGlobalSearch" class="search-button" size="large">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
              </template>
            </el-input>
          </div>

          <!-- 搜索历史 -->
          <div v-if="searchHistory.length > 0" class="search-history-section">
            <div class="history-content">
              <div class="history-header">
                <span class="history-title">搜索历史</span>
                <el-button type="text" @click="clearSearchHistory" size="small">清除</el-button>
              </div>
              <div class="history-list">
                <el-tag
                  v-for="(item, index) in searchHistory.slice(0, 10)"
                  :key="index"
                  class="history-tag"
                  @click="selectSearchHistory(item)"
                  closable
                  @close.stop="removeSearchHistory(index)"
                >
                  {{ item }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <div class="stats-row">
        <el-card class="stat-card" @click="navigateTo('/environments')">
          <div class="stat-content">
            <div class="stat-icon environment">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.environments }}</div>
              <div class="stat-label">环境</div>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" @click="navigateTo('/components')">
          <div class="stat-content">
            <div class="stat-icon component">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.components }}</div>
              <div class="stat-label">组件</div>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" @click="navigateTo('/processes')">
          <div class="stat-content">
            <div class="stat-icon process">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.processes }}</div>
              <div class="stat-label">流程</div>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" @click="navigateTo('/repositories')">
          <div class="stat-content">
            <div class="stat-icon project">
              <el-icon><Folder /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.projects }}</div>
              <div class="stat-label">仓库</div>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" @click="navigateTo('/snippets')">
          <div class="stat-content">
            <div class="stat-icon snippet">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.snippets }}</div>
              <div class="stat-label">代码片段</div>
            </div>
          </div>
        </el-card>
      </div>

      <el-card class="quick-actions">
        <div class="card-header">
          <h3>快速操作</h3>
        </div>
        <div class="action-buttons">
          <el-button type="primary" class="action-button" @click="showAddEnvironmentDialog">
            <el-icon><Plus /></el-icon>
            添加环境信息
          </el-button>
          <el-button type="success" class="action-button" @click="showAddComponentDialog">
            <el-icon><Plus /></el-icon>
            添加技术组件
          </el-button>
          <el-button type="warning" class="action-button" @click="showAddProcessDialog">
            <el-icon><Plus /></el-icon>
            添加业务流程
          </el-button>
          <el-button type="info" class="action-button" @click="showAddProjectDialog">
            <el-icon><Plus /></el-icon>
            添加代码仓库
          </el-button>
          <el-button type="danger" class="action-button" @click="showAddSnippetDialog">
            <el-icon><Plus /></el-icon>
            添加代码片段
          </el-button>
        </div>
      </el-card>

      <!-- 环境新增弹窗 -->
      <el-dialog
        v-model="environmentDialog.visible"
        title="新增环境"
        width="600px"
        destroy-on-close
      >
        <el-form :model="environmentDialog.formData" :rules="environmentDialog.rules" ref="environmentDialog.formRef" label-width="100px">
          <el-form-item label="环境名称" prop="name">
            <el-input v-model="environmentDialog.formData.name" placeholder="请输入环境名称" />
          </el-form-item>
          <el-form-item label="环境类型" prop="type">
            <el-select v-model="environmentDialog.formData.type" placeholder="请选择环境类型">
              <el-option
                v-for="item in environmentDialog.typeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="访问地址" prop="url">
            <el-input v-model="environmentDialog.formData.url" placeholder="请输入访问地址" />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="environmentDialog.formData.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="environmentDialog.formData.password"
              type="password"
              placeholder="请输入密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="environmentDialog.formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="environmentDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="handleEnvironmentSubmit" :loading="environmentDialog.submitting">确定</el-button>
        </template>
      </el-dialog>

      <!-- 组件新增弹窗 -->
      <el-dialog
        v-model="componentDialog.visible"
        title="新增组件"
        width="600px"
        destroy-on-close
      >
        <el-form :model="componentDialog.formData" :rules="componentDialog.rules" ref="componentDialog.formRef" label-width="100px">
          <el-form-item label="组件名称" prop="name">
            <el-input v-model="componentDialog.formData.name" placeholder="请输入组件名称" />
          </el-form-item>
          <el-form-item label="组件分类" prop="category">
            <el-select v-model="componentDialog.formData.category" placeholder="请选择组件分类">
              <el-option
                v-for="item in componentDialog.categoryOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="环境" prop="environmentId">
            <el-select v-model="componentDialog.formData.environmentId" placeholder="请选择环境" clearable>
              <el-option
                v-for="env in componentDialog.environments"
                :key="env.id"
                :label="getEnvironmentDisplayLabel(env)"
                :value="env.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="版本" prop="version">
            <el-input v-model="componentDialog.formData.version" placeholder="请输入版本号" />
          </el-form-item>
          <el-form-item label="访问地址" prop="url">
            <el-input v-model="componentDialog.formData.url" placeholder="请输入访问地址" />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="componentDialog.formData.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="componentDialog.formData.password"
              type="password"
              placeholder="请输入密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="componentDialog.formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="componentDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="handleComponentSubmit" :loading="componentDialog.submitting">确定</el-button>
        </template>
      </el-dialog>

      <!-- 流程新增弹窗 -->
      <el-dialog
        v-model="processDialog.visible"
        title="新增流程"
        width="700px"
        destroy-on-close
      >
        <el-form :model="processDialog.formData" :rules="processDialog.rules" ref="processDialog.formRef" label-width="100px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="流程名称" prop="name">
                <el-input v-model="processDialog.formData.name" placeholder="请输入流程名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="流程分类" prop="category">
                <el-select v-model="processDialog.formData.category" placeholder="请选择流程分类">
                  <el-option
                    v-for="item in processDialog.categoryOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="环境" prop="environmentId">
                <el-select v-model="processDialog.formData.environmentId" placeholder="请选择环境" clearable>
                  <el-option label="不区分环境" :value="null" />
                  <el-option
                    v-for="env in processDialog.environments"
                    :key="env.id"
                    :label="getEnvironmentDisplayLabel(env)"
                    :value="env.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="优先级" prop="priority">
                <el-input-number v-model="processDialog.formData.priority" :min="0" :max="100" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="描述" prop="description">
            <el-input v-model="processDialog.formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
          </el-form-item>
          <el-form-item label="流程步骤" prop="processFlow">
            <el-input v-model="processDialog.formData.processFlow" type="textarea" :rows="5" placeholder="请输入流程步骤说明" />
          </el-form-item>
          <el-form-item label="注意事项" prop="precautions">
            <el-input v-model="processDialog.formData.precautions" type="textarea" :rows="3" placeholder="请输入注意事项" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="processDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="handleProcessSubmit" :loading="processDialog.submitting">确定</el-button>
        </template>
      </el-dialog>

      <!-- 项目新增弹窗 -->
      <el-dialog
        v-model="projectDialog.visible"
        title="新增代码仓库"
        width="700px"
        destroy-on-close
      >
        <el-form :model="projectDialog.formData" :rules="projectDialog.rules" ref="projectDialog.formRef" label-width="100px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="代码仓库名称" prop="name">
                <el-input v-model="projectDialog.formData.name" placeholder="请输入代码仓库名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="代码仓库状态" prop="status">
                <el-select v-model="projectDialog.formData.status" placeholder="请选择代码仓库状态">
                  <el-option
                    v-for="item in projectDialog.statusOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目全称" prop="projectFullName">
                <el-input v-model="projectDialog.formData.projectFullName" placeholder="请输入项目全称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="端口号" prop="port">
                <el-input v-model="projectDialog.formData.port" placeholder="请输入端口号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="代码仓库" prop="codeRepository">
            <el-input v-model="projectDialog.formData.codeRepository" placeholder="请输入代码仓库地址" />
          </el-form-item>
          <el-form-item label="文档位置" prop="documentPath">
            <el-input v-model="projectDialog.formData.documentPath" placeholder="请输入文档位置" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="projectDialog.formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="projectDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="handleProjectSubmit" :loading="projectDialog.submitting">确定</el-button>
        </template>
      </el-dialog>

      <!-- 代码片段新增弹窗 -->
      <el-dialog
        v-model="snippetDialog.visible"
        title="新增代码片段"
        width="800px"
        destroy-on-close
      >
        <el-form :model="snippetDialog.formData" :rules="snippetDialog.rules" ref="snippetDialog.formRef" label-width="100px">
          <el-form-item label="标题" prop="title">
            <el-input v-model="snippetDialog.formData.title" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item label="编程语言" prop="language">
            <el-select v-model="snippetDialog.formData.language" placeholder="请选择编程语言" class="full-width">
              <el-option label="Java" value="java" />
              <el-option label="Vue" value="vue" />
              <el-option label="JavaScript" value="javascript" />
              <el-option label="SQL" value="sql" />
            </el-select>
          </el-form-item>
          <el-form-item label="标签" prop="tags">
            <el-input v-model="snippetDialog.formData.tags" placeholder="请输入标签，多个标签用逗号分隔" />
          </el-form-item>
          <el-form-item label="代码" prop="code">
            <el-input
              v-model="snippetDialog.formData.code"
              type="textarea"
              :rows="15"
              placeholder="请输入代码"
              class="code-textarea"
            />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="snippetDialog.formData.description" type="textarea" :rows="2" placeholder="请输入描述" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="snippetDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="handleSnippetSubmit" :loading="snippetDialog.submitting">确定</el-button>
        </template>
      </el-dialog>

      <el-card class="recent-activities">
        <div class="card-header">
          <h3>最近活动</h3>
        </div>
        <el-timeline v-if="recentActivities.length > 0">
          <el-timeline-item
            v-for="activity in recentActivities"
            :key="activity.id"
            :timestamp="formatDate(activity.createdAt)"
            :type="getActivityType(activity.operation)"
            placement="top"
          >
            <div class="activity-item">
              <el-tag :type="getActivityTagType(activity.operation)" size="small">
                {{ activity.operation }}
              </el-tag>
              <span class="activity-user">{{ activity.username }}</span>
              <span class="activity-module">[{{ activity.module }}]</span>
              <span class="activity-desc">{{ activity.description }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无活动记录" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'
import { activityApi, type ActivityLog } from '@/api/activity'
import { Search, Monitor, Cpu, Document, Folder, Plus } from '@element-plus/icons-vue'
import { systemApi } from '@/api/system'
import { ElMessage } from 'element-plus'
import { useEnvironmentStore } from '@/stores/environment'
import { useComponentStore } from '@/stores/component'
import { useProcessStore } from '@/stores/process'
import { useProjectStore } from '@/stores/project'
import { useDictStore } from '@/stores/dict'

const router = useRouter()
const searchKeyword = ref('')
const welcomeText = ref('助记单中心')
const searchHistory = ref<string[]>([])

const stats = ref({
  environments: 0,
  components: 0,
  processes: 0,
  projects: 0,
  snippets: 0
})

const recentActivities = ref<ActivityLog[]>([])

// Store 实例
const environmentStore = useEnvironmentStore()
const componentStore = useComponentStore()
const processStore = useProcessStore()
const projectStore = useProjectStore()
const dictStore = useDictStore()

// 环境弹窗配置
const environmentDialog = reactive({
  visible: false,
  submitting: false,
  formRef: null as any,
  formData: {
    name: '',
    type: '',
    url: '',
    username: '',
    password: '',
    description: ''
  },
  rules: {
    name: [{ required: true, message: '请输入环境名称', trigger: 'blur' }],
    type: [{ required: true, message: '请选择环境类型', trigger: 'change' }]
  },
  typeOptions: computed(() => dictStore.getDictOptions('environment_type'))
})

// 组件弹窗配置
const componentDialog = reactive({
  visible: false,
  submitting: false,
  formRef: null as any,
  formData: {
    name: '',
    category: '',
    environmentId: undefined,
    version: '',
    url: '',
    username: '',
    password: '',
    description: ''
  },
  rules: {
    name: [{ required: true, message: '请输入组件名称', trigger: 'blur' }],
    category: [{ required: true, message: '请选择组件分类', trigger: 'change' }]
  },
  categoryOptions: computed(() => dictStore.getDictOptions('component_category')),
  environments: computed(() => environmentStore.environments)
})

// 流程弹窗配置
const processDialog = reactive({
  visible: false,
  submitting: false,
  formRef: null as any,
  formData: {
    name: '',
    category: '',
    environmentId: undefined,
    priority: 0,
    description: '',
    processFlow: '',
    precautions: ''
  },
  rules: {
    name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
    category: [{ required: true, message: '请选择流程分类', trigger: 'change' }]
  },
  categoryOptions: computed(() => dictStore.getDictOptions('process_category')),
  environments: computed(() => environmentStore.environments)
})

// 项目弹窗配置
const projectDialog = reactive({
  visible: false,
  submitting: false,
  formRef: null as any,
  formData: {
    name: '',
    status: '',
    projectFullName: '',
    port: '',
    codeRepository: '',
    documentPath: '',
    description: ''
  },
  rules: {
    name: [{ required: true, message: '请输入代码仓库名称', trigger: 'blur' }],
    status: [{ required: true, message: '请选择代码仓库状态', trigger: 'change' }]
  },
  statusOptions: computed(() => dictStore.getDictOptions('repository_status'))
})

// 代码片段弹窗配置
const snippetDialog = reactive({
  visible: false,
  submitting: false,
  formRef: null as any,
  formData: {
    title: '',
    language: '',
    description: '',
    code: '',
    tags: ''
  },
  rules: {
    title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
    language: [{ required: true, message: '请选择编程语言', trigger: 'change' }],
    code: [{ required: true, message: '请输入代码', trigger: 'blur' }]
  }
})

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getActivityType = (operation: string) => {
  const map: Record<string, string> = {
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    LOGIN: 'primary',
    LOGOUT: 'info'
  }
  return map[operation] || 'info'
}

const getActivityTagType = (operation: string) => {
  const map: Record<string, string> = {
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    LOGIN: '',
    LOGOUT: 'info'
  }
  return map[operation] || 'info'
}

const loadRecentActivities = async () => {
  try {
    const response = await activityApi.getRecentActivities(10)
    if (response && response.data) {
      recentActivities.value = response.data
    }
  } catch (error) {
    console.error('加载最近活动失败:', error)
  }
}

const navigateTo = (path: string) => {
  router.push(path)
}

// 环境相关函数
const showAddEnvironmentDialog = () => {
  Object.assign(environmentDialog.formData, {
    name: '',
    type: '',
    url: '',
    username: '',
    password: '',
    description: ''
  })
  environmentDialog.visible = true
}

const handleEnvironmentSubmit = async () => {
  await environmentDialog.formRef.validate()
  environmentDialog.submitting = true

  try {
    await environmentStore.addEnvironment(environmentDialog.formData)
    ElMessage.success('创建成功')
    environmentDialog.visible = false
    loadStats()
  } catch (error) {
    console.error('创建环境失败:', error)
    ElMessage.error('创建失败')
  } finally {
    environmentDialog.submitting = false
  }
}

// 组件相关函数
const showAddComponentDialog = () => {
  Object.assign(componentDialog.formData, {
    name: '',
    category: '',
    environmentId: undefined,
    version: '',
    url: '',
    username: '',
    password: '',
    description: ''
  })
  componentDialog.visible = true
}

const handleComponentSubmit = async () => {
  await componentDialog.formRef.validate()
  componentDialog.submitting = true

  try {
    await componentStore.addComponent(componentDialog.formData)
    ElMessage.success('创建成功')
    componentDialog.visible = false
    loadStats()
  } catch (error) {
    console.error('创建组件失败:', error)
    ElMessage.error('创建失败')
  } finally {
    componentDialog.submitting = false
  }
}

// 流程相关函数
const showAddProcessDialog = () => {
  Object.assign(processDialog.formData, {
    name: '',
    category: '',
    environmentId: undefined,
    priority: 0,
    description: '',
    processFlow: '',
    precautions: ''
  })
  processDialog.visible = true
}

const handleProcessSubmit = async () => {
  await processDialog.formRef.validate()
  processDialog.submitting = true

  try {
    await processStore.addProcess(processDialog.formData)
    ElMessage.success('创建成功')
    processDialog.visible = false
    loadStats()
  } catch (error) {
    console.error('创建流程失败:', error)
    ElMessage.error('创建失败')
  } finally {
    processDialog.submitting = false
  }
}

// 项目相关函数
const showAddProjectDialog = () => {
  Object.assign(projectDialog.formData, {
    name: '',
    status: '',
    projectFullName: '',
    port: '',
    codeRepository: '',
    documentPath: '',
    description: ''
  })
  projectDialog.visible = true
}

const handleProjectSubmit = async () => {
  await projectDialog.formRef.validate()
  projectDialog.submitting = true

  try {
    await projectStore.addProject(projectDialog.formData)
    ElMessage.success('创建成功')
    projectDialog.visible = false
    loadStats()
  } catch (error) {
    console.error('创建项目失败:', error)
    ElMessage.error('创建失败')
  } finally {
    projectDialog.submitting = false
  }
}

// 代码片段相关函数
const showAddSnippetDialog = () => {
  Object.assign(snippetDialog.formData, {
    title: '',
    language: '',
    description: '',
    code: '',
    tags: ''
  })
  snippetDialog.visible = true
}

const handleSnippetSubmit = async () => {
  await snippetDialog.formRef.validate()
  snippetDialog.submitting = true

  try {
    await request.post('/snippets', snippetDialog.formData)
    ElMessage.success('创建成功')
    snippetDialog.visible = false
    loadStats()
  } catch (error) {
    console.error('创建代码片段失败:', error)
    ElMessage.error('创建失败')
  } finally {
    snippetDialog.submitting = false
  }
}

// 工具函数
const getEnvironmentDisplayLabel = (env: any) => {
  const environmentTypeOptions = dictStore.getDictOptions('environment_type')
  const option = environmentTypeOptions.find(o => o.value === env.type)
  const typeLabel = option?.label || env.type
  return `${env.name} (${typeLabel})`
}

const handleGlobalSearch = () => {
  if (searchKeyword.value.trim()) {
    // 保存搜索历史
    saveSearchHistory(searchKeyword.value.trim())
    router.push(`/search?q=${encodeURIComponent(searchKeyword.value.trim())}`)
  }
}

const loadSearchHistory = () => {
  const history = localStorage.getItem('searchHistory')
  if (history) {
    searchHistory.value = JSON.parse(history)
  }
}

const saveSearchHistory = (keyword: string) => {
  // 移除重复的关键词
  const index = searchHistory.value.indexOf(keyword)
  if (index > -1) {
    searchHistory.value.splice(index, 1)
  }
  // 添加到历史记录的开头
  searchHistory.value.unshift(keyword)
  // 限制历史记录数量为10条
  if (searchHistory.value.length > 10) {
    searchHistory.value = searchHistory.value.slice(0, 10)
  }
  // 保存到localStorage
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
}

const clearSearchHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

const selectSearchHistory = (keyword: string) => {
  searchKeyword.value = keyword
  handleGlobalSearch()
}

const removeSearchHistory = (index: number) => {
  searchHistory.value.splice(index, 1)
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
}

const loadStats = async () => {
  try {
    const response = await request.get('/dashboard/stats')
    if (response && response.data) {
      stats.value = response.data
    }

    // 加载代码片段数量
    try {
      const snippetResponse = await request.get('/snippets/list')
      if (snippetResponse && snippetResponse.data) {
        stats.value.snippets = snippetResponse.data.length || 0
      }
    } catch (snippetError) {
      console.error('加载代码片段数量失败:', snippetError)
      stats.value.snippets = 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const fetchSystemConfig = async () => {
  try {
    const response = await systemApi.getConfigByKey('system.name')
    if (response.data && response.data.configValue) {
      welcomeText.value = response.data.configValue + '中心'
    } else {
      // 默认值
      welcomeText.value = '新人筑基丹中心'
    }
  } catch (error) {
    console.error('获取系统配置失败:', error)
    // 出错时使用默认值
    welcomeText.value = '新人筑基丹中心'
  }
}

// 页面获得焦点时刷新配置
const handlePageFocus = () => {
  fetchSystemConfig()
}

onMounted(() => {
  // 加载字典数据
  dictStore.fetchDictDataByType('environment_type')
  dictStore.fetchDictDataByType('component_category')
  dictStore.fetchDictDataByType('process_category')
  dictStore.fetchDictDataByType('repository_status')

  // 加载环境数据（用于组件和流程的环境选择）
  environmentStore.fetchEnvironments({ page: 0, size: 100 })

  loadStats()
  loadRecentActivities()
  fetchSystemConfig()
  loadSearchHistory()

  // 监听页面获得焦点事件
  window.addEventListener('focus', handlePageFocus)
})

onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('focus', handlePageFocus)
})
</script>

<style scoped>
.dashboard {
  min-height: 100%;
}

.stats-row {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 24px;
}

.stats-row .stat-card {
  flex: 1;
  max-width: 200px;
  min-width: 140px;
}

.global-search-card {
  margin-bottom: 24px;
}

.search-container {
  max-width: 800px;
  margin: 0 auto;
  position: relative;
}

.search-input {
  height: 48px;
  font-size: 16px;
}

.search-icon {
  font-size: 20px;
  color: #909399;
}

.search-button {
  height: 48px;
  font-size: 16px;
}

.search-layout {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-box-section {
  width: 100%;
}

.search-input {
  height: 48px;
  font-size: 16px;
}

.search-history-section {
  width: 100%;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  margin-top: 16px;
}

.history-content {
  width: 100%;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.history-title {
  font-size: 16px;
  font-weight: 600;
  color: #495057;
}

.history-list {
  display: flex;
  flex-wrap: nowrap;
  gap: 10px;
  overflow-x: auto;
  max-height: 40px;
  padding-bottom: 5px;
}

.history-list::-webkit-scrollbar {
  height: 4px;
}

.history-list::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 2px;
}

.history-tag {
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  padding: 6px 12px;
}

.history-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.no-history {
  padding: 20px 0;
  text-align: center;
}

:deep(.el-empty__description) {
  color: #adb5bd;
  font-size: 14px;
}

.stat-card {
  height: 120px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
  color: white;
}

.stat-icon.environment {
  background: linear-gradient(135deg, #409eff, #53a8ff);
}

.stat-icon.component {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.stat-icon.process {
  background: linear-gradient(135deg, #e6a23c, #eebe77);
}

.stat-icon.project {
  background: linear-gradient(135deg, #909399, #a6a9ad);
}

.stat-icon.snippet {
  background: linear-gradient(135deg, #f56c6c, #f89898);
}

.stat-number {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-top: 4px;
}

.card-header {
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.quick-actions {
  margin-bottom: 24px;
}

.quick-actions .action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.action-button {
  min-width: 140px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 15px;
}

.recent-activities {
  margin-bottom: 24px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.activity-user {
  font-weight: 500;
  color: #409eff;
}

.activity-module {
  color: #909399;
}

.activity-desc {
  color: #606266;
}

.full-width {
  width: 100%;
}

.code-textarea {
  font-family: 'Fira Code', Consolas, 'Courier New', monospace;
  font-size: 14px;
}

@media (max-width: 768px) {
  .stats-row {
    flex-wrap: wrap;
  }
  .stats-row .stat-card {
    flex: 1 1 calc(33% - 16px);
    max-width: none;
  }
  .quick-actions .action-buttons {
    flex-wrap: wrap;
  }
  .action-button {
    flex: 1 1 calc(33% - 16px);
  }
}
</style>
