<template>
  <div class="dashboard">
    <div class="container">
      <!-- 搜索区域 -->
      <el-card class="global-search-card" :shadow="'never'">
        <div class="search-layout">
          <div class="search-box-section">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索项目、组件、流程、技术资料..."
              clearable
              @keyup.enter="handleGlobalSearch"
              class="search-input"
            >
              <template #prefix>
                <el-icon class="search-icon"><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleGlobalSearch" class="search-btn">
              搜索
            </el-button>
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

      <!-- 统计卡片 -->
      <div class="stats-row">
        <div class="stat-card" @click="navigateTo('/environments')">
          <div class="stat-icon environment">
            <el-icon :size="24"><Monitor /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ stats.environments }}</div>
            <div class="stat-label">环境管理</div>
          </div>
        </div>

        <div class="stat-card" @click="navigateTo('/components')">
          <div class="stat-icon component">
            <el-icon :size="24"><Tickets /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ stats.components }}</div>
            <div class="stat-label">技术组件</div>
          </div>
        </div>

        <div class="stat-card" @click="navigateTo('/processes')">
          <div class="stat-icon process">
            <el-icon :size="24"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ stats.processes }}</div>
            <div class="stat-label">业务流程</div>
          </div>
        </div>

        <div class="stat-card" @click="navigateTo('/repositories')">
          <div class="stat-icon project">
            <el-icon :size="24"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ stats.projects }}</div>
            <div class="stat-label">代码仓库</div>
          </div>
        </div>

        <div class="stat-card" @click="navigateTo('/iterations')">
          <div class="stat-icon iteration">
            <el-icon :size="24"><List /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ stats.iterations }}</div>
            <div class="stat-label">迭代管理</div>
          </div>
        </div>

        <div class="stat-card" @click="navigateTo('/snippets')">
          <div class="stat-icon snippet">
            <el-icon :size="24"><Bell /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ stats.snippets }}</div>
            <div class="stat-label">代码片段</div>
          </div>
        </div>
      </div>

      <!-- 快速操作 -->
      <div class="quick-actions">
        <h3 class="section-title">快速操作</h3>
        <div class="action-buttons">
          <el-button type="primary" class="action-btn blue" @click="showAddEnvironmentDialog">
            创建环境信息
          </el-button>
          <el-button type="success" class="action-btn green" @click="showAddComponentDialog">
            创建技术组件
          </el-button>
          <el-button type="warning" class="action-btn orange" @click="showAddProcessDialog">
            创建业务流程
          </el-button>
          <el-button type="info" class="action-btn gray" @click="showAddProjectDialog">
            创建代码仓库
          </el-button>
          <el-button class="action-btn purple" @click="showAddIterationDialog">
            创建迭代任务
          </el-button>
          <el-button type="danger" class="action-btn red" @click="showAddSnippetDialog">
            创建代码片段
          </el-button>
        </div>
      </div>

      <!-- 最近活动 -->
      <div class="recent-activities">
        <h3 class="section-title">最近活动</h3>
        <el-table :data="recentActivities" stripe style="width: 100%" v-if="recentActivities.length > 0">
          <el-table-column prop="createdAt" label="时间" min-width="170">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="用户" width="100">
            <template #default="{ row }">
              <el-tag size="small" type="primary">{{ row.username || 'admin' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="module" label="操作" min-width="120" />
          <el-table-column prop="description" label="模块" min-width="150" />
        </el-table>
        <el-empty v-else description="暂无活动记录" />
      </div>
    </div>

    <!-- 环境新增弹窗 -->
    <el-dialog v-model="environmentDialog.visible" title="新增环境" width="600px" destroy-on-close>
      <el-form :model="environmentDialog.formData" :rules="environmentDialog.rules" ref="environmentDialog.formRef" label-width="100px">
        <el-form-item label="环境名称" prop="name">
          <el-input v-model="environmentDialog.formData.name" placeholder="请输入环境名称" />
        </el-form-item>
        <el-form-item label="环境类型" prop="type">
          <el-select v-model="environmentDialog.formData.type" placeholder="请选择环境类型">
            <el-option v-for="item in environmentDialog.typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问地址" prop="url">
          <el-input v-model="environmentDialog.formData.url" placeholder="请输入访问地址" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="environmentDialog.formData.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="environmentDialog.formData.password" type="password" placeholder="请输入密码" show-password />
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
    <el-dialog v-model="componentDialog.visible" title="新增组件" width="600px" destroy-on-close>
      <el-form :model="componentDialog.formData" :rules="componentDialog.rules" ref="componentDialog.formRef" label-width="100px">
        <el-form-item label="组件名称" prop="name">
          <el-input v-model="componentDialog.formData.name" placeholder="请输入组件名称" />
        </el-form-item>
        <el-form-item label="组件分类" prop="category">
          <el-select v-model="componentDialog.formData.category" placeholder="请选择组件分类">
            <el-option v-for="item in componentDialog.categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="环境" prop="environmentId">
          <el-select v-model="componentDialog.formData.environmentId" placeholder="请选择环境" clearable>
            <el-option v-for="env in componentDialog.environments" :key="env.id" :label="getEnvironmentDisplayLabel(env)" :value="env.id" />
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
          <el-input v-model="componentDialog.formData.password" type="password" placeholder="请输入密码" show-password />
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
    <el-dialog v-model="processDialog.visible" title="新增流程" width="700px" destroy-on-close>
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
                <el-option v-for="item in processDialog.categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="环境" prop="environmentId">
              <el-select v-model="processDialog.formData.environmentId" placeholder="请选择环境" clearable>
                <el-option label="不区分环境" :value="null" />
                <el-option v-for="env in processDialog.environments" :key="env.id" :label="getEnvironmentDisplayLabel(env)" :value="env.id" />
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
    <el-dialog v-model="projectDialog.visible" title="新增代码仓库" width="700px" destroy-on-close>
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
                <el-option v-for="item in projectDialog.statusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
    <el-dialog v-model="snippetDialog.visible" title="新增代码片段" width="800px" destroy-on-close>
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
          <el-input v-model="snippetDialog.formData.code" type="textarea" :rows="15" placeholder="请输入代码" class="code-textarea" />
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

    <!-- 迭代新增弹窗 -->
    <el-dialog v-model="iterationDialog.visible" title="新增迭代任务" width="600px" destroy-on-close>
      <el-form :model="iterationDialog.formData" :rules="iterationDialog.rules" ref="iterationDialog.formRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Issue编号" prop="issueNumber">
              <el-input v-model="iterationDialog.formData.issueNumber" placeholder="如：ISSUE-123" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目简称" prop="projectCode">
              <el-input v-model="iterationDialog.formData.projectCode" placeholder="如：mynewwork" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="需求概要" prop="title">
          <el-input v-model="iterationDialog.formData.title" placeholder="需求概要描述" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="iterationDialog.formData.status" placeholder="请选择状态">
                <el-option label="需求提出" value="pending" />
                <el-option label="开发中" value="developing" />
                <el-option label="测试中" value="testing" />
                <el-option label="已上线" value="released" />
                <el-option label="已关闭" value="closed" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="iterationDialog.formData.priority" placeholder="请选择优先级">
                <el-option label="高" value="high" />
                <el-option label="中" value="medium" />
                <el-option label="低" value="low" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="iterationDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="handleIterationSubmit" :loading="iterationDialog.submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'
import { activityApi, type ActivityLog } from '@/api/activity'
import { Search, Monitor, Cpu, Document, Folder, Plus, Tickets, UserFilled, Bell, List } from '@element-plus/icons-vue'
import { systemApi } from '@/api/system'
import { ElMessage } from 'element-plus'
import { useEnvironmentStore } from '@/stores/environment'
import { useComponentStore } from '@/stores/component'
import { useProcessStore } from '@/stores/process'
import { useProjectStore } from '@/stores/project'
import { useIterationStore } from '@/stores/iteration'
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
  iterations: 0,
  snippets: 0
})

const recentActivities = ref<ActivityLog[]>([])

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
  formData: { name: '', type: '', url: '', username: '', password: '', description: '' },
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
  formData: { name: '', category: '', environmentId: undefined as number | undefined, version: '', url: '', username: '', password: '', description: '' },
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
  formData: { name: '', category: '', environmentId: undefined as number | undefined, priority: 0, description: '', processFlow: '', precautions: '' },
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
  formData: { name: '', status: '', projectFullName: '', port: '', codeRepository: '', documentPath: '', description: '' },
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
  formData: { title: '', language: '', description: '', code: '', tags: '' },
  rules: {
    title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
    language: [{ required: true, message: '请选择编程语言', trigger: 'change' }],
    code: [{ required: true, message: '请输入代码', trigger: 'blur' }]
  }
})

// 迭代弹窗配置
const iterationDialog = reactive({
  visible: false,
  submitting: false,
  formRef: null as any,
  formData: { issueNumber: '', projectCode: '', title: '', status: 'pending', priority: 'medium' },
  rules: {
    issueNumber: [{ required: true, message: '请输入Issue编号', trigger: 'blur' }],
    projectCode: [{ required: true, message: '请输入项目简称', trigger: 'blur' }],
    title: [{ required: true, message: '请输入需求概要', trigger: 'blur' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }],
    priority: [{ required: true, message: '请选择优先级', trigger: 'change' }]
  }
})

const iterationStore = useIterationStore()

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
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
  Object.assign(environmentDialog.formData, { name: '', type: '', url: '', username: '', password: '', description: '' })
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
  Object.assign(componentDialog.formData, { name: '', category: '', environmentId: undefined, version: '', url: '', username: '', password: '', description: '' })
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
  Object.assign(processDialog.formData, { name: '', category: '', environmentId: undefined, priority: 0, description: '', processFlow: '', precautions: '' })
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
  Object.assign(projectDialog.formData, { name: '', status: '', projectFullName: '', port: '', codeRepository: '', documentPath: '', description: '' })
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
  Object.assign(snippetDialog.formData, { title: '', language: '', description: '', code: '', tags: '' })
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

// 迭代相关函数
const showAddIterationDialog = () => {
  Object.assign(iterationDialog.formData, { issueNumber: '', projectCode: '', title: '', status: 'TODO', priority: 'MEDIUM' })
  iterationDialog.visible = true
}

const handleIterationSubmit = async () => {
  await iterationDialog.formRef.validate()
  iterationDialog.submitting = true
  try {
    await iterationStore.addIteration(iterationDialog.formData as any)
    ElMessage.success('创建成功')
    iterationDialog.visible = false
    loadStats()
  } catch (error) {
    console.error('创建迭代失败:', error)
    ElMessage.error('创建失败')
  } finally {
    iterationDialog.submitting = false
  }
}

const getEnvironmentDisplayLabel = (env: any) => {
  const environmentTypeOptions = dictStore.getDictOptions('environment_type')
  const option = environmentTypeOptions.find(o => o.value === env.type)
  const typeLabel = option?.label || env.type
  return `${env.name} (${typeLabel})`
}

const handleGlobalSearch = () => {
  if (searchKeyword.value.trim()) {
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
  const index = searchHistory.value.indexOf(keyword)
  if (index > -1) searchHistory.value.splice(index, 1)
  searchHistory.value.unshift(keyword)
  if (searchHistory.value.length > 10) searchHistory.value = searchHistory.value.slice(0, 10)
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
    try {
      const snippetResponse = await request.get('/snippets/list')
      if (snippetResponse && snippetResponse.data) {
        stats.value.snippets = snippetResponse.data.length || 0
      }
    } catch (e) {
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
      welcomeText.value = '新人筑基丹中心'
    }
  } catch (error) {
    welcomeText.value = '新人筑基丹中心'
  }
}

const handlePageFocus = () => {
  fetchSystemConfig()
}

onMounted(() => {
  dictStore.fetchDictDataByType('environment_type')
  dictStore.fetchDictDataByType('component_category')
  dictStore.fetchDictDataByType('process_category')
  dictStore.fetchDictDataByType('repository_status')
  environmentStore.fetchEnvironments({ page: 0, size: 100 })
  loadStats()
  loadRecentActivities()
  fetchSystemConfig()
  loadSearchHistory()
  window.addEventListener('focus', handlePageFocus)
})

onUnmounted(() => {
  window.removeEventListener('focus', handlePageFocus)
})
</script>

<style scoped>
.dashboard {
  min-height: 100%;
}

/* ===== 页面标题 ===== */
.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 22px;
  font-weight: 600;
  color: #1d2129;
  margin: 0;
}

.page-description {
  color: #86909c;
  font-size: 14px;
  margin-top: 4px;
}

/* ===== 全局搜索卡片 ===== */
.global-search-card {
  margin-bottom: 24px !important;
  border-radius: var(--card-border-radius) !important;
  border: none !important;
  box-shadow: var(--card-shadow) !important;
}

.search-layout {
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-box-section {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-box-section .search-input {
  flex: 1;
}

.search-box-section .search-input :deep(.el-input__wrapper) {
  height: 44px;
  padding: 0 16px;
  border-radius: 8px;
}

.search-box-section .search-input :deep(.el-input__inner) {
  font-size: 15px;
}

.search-icon {
  font-size: 18px;
  color: #c0c4cc;
}

.search-btn {
  height: 44px;
  padding: 0 28px;
  font-size: 15px;
  border-radius: 8px;
  white-space: nowrap;
}

/* 搜索历史 */
.search-history-section {
  background: #f7f8fa;
  border-radius: 8px;
  padding: 16px 18px;
  border: 1px solid #f0f0f0;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.history-title {
  font-size: 13px;
  font-weight: 600;
  color: #86909c;
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  border-radius: 4px;
}

.history-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

/* ===== 统计卡片行 ===== */
.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px 24px;
  background: #fff;
  border-radius: var(--card-border-radius);
  box-shadow: var(--card-shadow);
  cursor: pointer;
  transition: all 0.25s ease;
  border: none;
}

html.dark .stat-card {
  background: #1d1e1f;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: white;
}

.stat-icon.environment {
  background: linear-gradient(135deg, #409eff, #337ecc);
}

.stat-icon.component {
  background: linear-gradient(135deg, #67c23a, #529b2e);
}

.stat-icon.process {
  background: linear-gradient(135deg, #e6a23c, #b88230);
}

.stat-icon.project {
  background: linear-gradient(135deg, #909399, #73767a);
}

.stat-icon.snippet {
  background: linear-gradient(135deg, #f56c6c, #c45656);
}

.stat-icon.iteration {
  background: linear-gradient(135deg, #9c88ff, #8a70ff);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 26px;
  font-weight: 700;
  color: #1d2129;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #86909c;
  margin-top: 2px;
}

/* ===== 快速操作 ===== */
.quick-actions {
  background: #fff;
  border-radius: var(--card-border-radius);
  box-shadow: var(--card-shadow);
  padding: 20px 24px;
  margin-bottom: 24px;
}

html.dark .quick-actions {
  background: #1d1e1f;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  margin: 0 0 16px 0;
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.action-btn {
  height: 36px;
  padding: 0 18px;
  font-size: 13px;
  border-radius: 6px;
  font-weight: 500;
  white-space: nowrap;
}

.action-btn.blue {
  --el-button-bg-color: #409eff;
  --el-button-border-color: #409eff;
}

.action-btn.green {
  --el-button-bg-color: #67c23a;
  --el-button-border-color: #67c23a;
}

.action-btn.orange {
  --el-button-bg-color: #e6a23c;
  --el-button-border-color: #e6a23c;
}

.action-btn.gray {
  --el-button-bg-color: #909399;
  --el-button-border-color: #909399;
}

.action-btn.red {
  --el-button-bg-color: #f56c6c;
  --el-button-border-color: #f56c6c;
}

.action-btn.purple {
  --el-button-bg-color: #9c88ff;
  --el-button-border-color: #9c88ff;
}

/* ===== 最近活动 ===== */
.recent-activities {
  background: #fff;
  border-radius: var(--card-border-radius);
  box-shadow: var(--card-shadow);
  padding: 20px 24px;
  margin-bottom: 24px;
}

html.dark .recent-activities {
  background: #1d1e1f;
}

.full-width {
  width: 100%;
}

.code-textarea {
  font-family: 'Fira Code', Consolas, 'Courier New', monospace;
  font-size: 14px;
}

/* ===== 响应式 ===== */
@media (max-width: 992px) {
  .stats-row {
    flex-wrap: wrap;
  }

  .stat-card {
    flex: 1 1 calc(33% - 11px);
    min-width: 160px;
  }

  .action-buttons {
    flex-wrap: wrap;
  }

  .action-btn {
    flex: 1 1 calc(50% - 5px);
  }
}

@media (max-width: 576px) {
  .stats-row {
    flex-direction: column;
  }

  .stat-card {
    flex: none;
  }

  .search-box-section {
    flex-direction: column;
  }

  .search-btn {
    width: 100%;
  }
}
</style>
