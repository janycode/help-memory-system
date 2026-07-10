<template>
  <div class="iteration-board">
    <!-- 左侧面板：需求列表 -->
    <div class="board-left">
      <div class="left-header">
        <h3 class="board-title">需求看板</h3>
        <div class="header-actions">
          <el-tooltip content="刷新目录" placement="top">
            <el-button size="small" @click="handleRefreshDirs" :loading="refreshing">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
          <el-button size="small" @click="showImportDialog">
            <el-icon><Upload /></el-icon>
            导入
          </el-button>
          <el-button type="primary" size="small" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新建
          </el-button>
        </div>
      </div>

      <!-- 搜索和筛选 -->
      <div class="left-filters">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索编号、项目、标题..."
          clearable
          size="small"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div class="filter-row">
          <el-select v-model="filterStatus" placeholder="状态" clearable size="small" @change="handleFilter">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-select v-model="filterPriority" placeholder="优先级" clearable size="small" @change="handleFilter">
            <el-option
              v-for="item in priorityOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
      </div>

      <!-- 需求列表 -->
      <div class="issue-list" v-loading="iterationStore.isLoading">
        <div
          v-for="item in iterationStore.iterations"
          :key="item.id"
          class="issue-card"
          :class="{ active: selectedId === item.id }"
          @click="selectIteration(item)"
        >
          <div class="issue-card-header">
            <span class="issue-number">#{{ item.issueNumber }}</span>
            <span class="issue-project">{{ item.projectCode }}</span>
            <el-tag v-if="item.hasTodos" type="warning" size="small" effect="dark" class="todos-tag">
              有待办
            </el-tag>
          </div>
          <div class="issue-card-title" :class="{ 'status-done': item.status === 'DONE' }">{{ item.title }}</div>
          <div class="issue-card-footer">
            <el-tag
              size="small"
              effect="light"
              :style="{ color: getStatusColor(item.status), borderColor: getStatusColor(item.status), backgroundColor: getStatusColor(item.status) + '15' }"
            >
              {{ getStatusLabel(item.status) }}
            </el-tag>
            <el-tag
              :type="getPriorityTagType(item.priority)"
              size="small"
              effect="plain"
            >
              {{ getPriorityLabel(item.priority) }}
            </el-tag>
            <span class="issue-time" v-if="item.completedAt" :title="'完成于 ' + formatDateTime(item.completedAt)">
              {{ formatDateTime(item.completedAt) }}
            </span>
            <span class="issue-time" v-else>{{ formatDateTime(item.createdAt) }}</span>
          </div>
        </div>

        <el-empty v-if="!iterationStore.isLoading && iterationStore.iterations.length === 0" description="暂无需求" />
      </div>

      <!-- 分页 -->
      <div class="left-pagination" v-if="iterationStore.pagination?.totalElements">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="iterationStore.pagination?.totalElements || 0"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          small
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 右侧面板：需求详情 -->
    <div class="board-right">
      <IterationDetail
        :iteration="selectedIteration"
        @edit="handleEdit"
        @delete="handleDelete"
        @update-status="handleUpdateStatus"
        @update-priority="handleUpdatePriority"
        @auto-save="handleAutoSave"
      />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑需求' : '新建需求'"
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="编号" prop="issueNumber">
          <el-input v-model="formData.issueNumber" placeholder="如Issue编号" />
        </el-form-item>
        <el-form-item label="项目简称" prop="projectCode">
          <el-input v-model="formData.projectCode" placeholder="如：order-service" />
        </el-form-item>
        <el-form-item label="需求标题" prop="title">
          <el-input v-model="formData.title" placeholder="需求概要描述" />
        </el-form-item>
        <el-form-item label="需求文档URL">
          <el-input v-model="formData.issueUrl" placeholder="如GitHub Issue URL" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择状态" style="width: 100%">
                <el-option
                  v-for="item in statusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="formData.priority" placeholder="请选择优先级" style="width: 100%">
                <el-option
                  v-for="item in priorityOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存' : '创建需求' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog
      v-model="importDialogVisible"
      title="从目录导入需求"
      width="500px"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="目录路径">
          <el-input v-model="importDirPath" placeholder="如：D:\work\07_需求" />
        </el-form-item>
        <el-form-item>
          <el-text type="info" size="small">
            文件夹格式：#编号-项目简称-需求标题<br/>
            文件夹内 .txt 文件将作为开发笔记，.md 文件将作为发布文档
          </el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing">
          导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Upload, Refresh } from '@element-plus/icons-vue'
import { useIterationStore } from '@/stores/iteration'
import type { Iteration } from '@/types/iteration'
import { IterationStatusMap, IterationPriorityMap } from '@/types/iteration'
import { getImportConfig, importFromDirectory, autoSync, checkNewFolders, refreshFileLists } from '@/api/iteration'
import IterationDetail from '@/components/IterationDetail.vue'

const iterationStore = useIterationStore()

const searchKeyword = ref('')
const filterStatus = ref('')
const filterPriority = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const selectedId = ref<number | null>(null)

const importDialogVisible = ref(false)
const importDirPath = ref('')
const importing = ref(false)
const refreshing = ref(false)

const autoSyncInterval: ReturnType<typeof setInterval> | null = null
const folderCheckInterval: ReturnType<typeof setInterval> | null = null

const statusOptions = Object.values(IterationStatusMap).map(item => ({
  value: item.value,
  label: item.label
}))

const priorityOptions = Object.values(IterationPriorityMap).map(item => ({
  value: item.value,
  label: item.label
}))

const formData = reactive({
  id: 0,
  issueNumber: '',
  projectCode: '',
  title: '',
  issueUrl: '',
  status: 'TODO' as string,
  priority: 'MEDIUM' as string
})

const rules = {
  issueNumber: [{ required: true, message: '请输入Issue编号', trigger: 'blur' }],
  projectCode: [{ required: true, message: '请输入项目简称', trigger: 'blur' }],
  title: [{ required: true, message: '请输入需求标题', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }]
}

const selectedIteration = computed(() => {
  if (!selectedId.value) return null
  // 优先使用 currentIteration（包含完整数据）
  if (iterationStore.currentIteration && iterationStore.currentIteration.id === selectedId.value) {
    return iterationStore.currentIteration
  }
  return iterationStore.iterations.find(i => i.id === selectedId.value) || null
})

const getStatusTagType = (status: string) => {
  const map: Record<string, string> = {
    TODO: 'info',
    IN_PROGRESS: '',
    TESTING: 'warning',
    DONE: 'success'
  }
  return map[status] || 'info'
}

const getStatusColor = (status: string) => {
  const map: Record<string, string> = {
    TODO: '#909399',
    IN_PROGRESS: '#409EFF',
    TESTING: '#E6A23C',
    DONE: '#67C23A'
  }
  return map[status] || '#909399'
}

const getStatusLabel = (status: string) => {
  return IterationStatusMap[status as keyof typeof IterationStatusMap]?.label || status || '未设置'
}

const getPriorityTagType = (priority: string) => {
  const map: Record<string, string> = {
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'success'
  }
  return map[priority] || 'info'
}

const getPriorityLabel = (priority: string) => {
  return IterationPriorityMap[priority as keyof typeof IterationPriorityMap]?.label || priority || '未设置'
}

const formatDateTime = (dateStr: string | null) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const fetchList = () => {
  iterationStore.fetchIterations({
    page: currentPage.value - 1,
    size: pageSize.value,
    status: filterStatus.value || undefined,
    priority: filterPriority.value || undefined,
    sortBy: 'issueNumber',
    sortDir: 'desc'
  })
}

const handleSearch = () => {
  if (searchKeyword.value) {
    iterationStore.searchIterationList(searchKeyword.value)
  } else {
    fetchList()
  }
}

const handleFilter = () => {
  currentPage.value = 1
  fetchList()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  fetchList()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchList()
}

const selectIteration = async (item: Iteration) => {
  selectedId.value = item.id
  // 重新获取完整数据，确保包含 developmentNotes 和 releaseNotes
  try {
    await iterationStore.fetchIteration(item.id)
  } catch (error) {
    console.error('获取迭代详情失败:', error)
  }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formData, {
    id: 0,
    issueNumber: '',
    projectCode: '',
    title: '',
    issueUrl: '',
    status: 'TODO',
    priority: 'MEDIUM'
  })
  dialogVisible.value = true
}

const handleEdit = (iteration: Iteration) => {
  isEdit.value = true
  Object.assign(formData, {
    id: iteration.id,
    issueNumber: iteration.issueNumber,
    projectCode: iteration.projectCode,
    title: iteration.title,
    issueUrl: iteration.issueUrl || '',
    status: iteration.status,
    priority: iteration.priority
  })
  dialogVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该需求吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const success = await iterationStore.removeIteration(id)
    if (success) {
      ElMessage.success('删除成功')
      if (selectedId.value === id) {
        selectedId.value = null
      }
      fetchList()
    }
  } catch {
    // 用户取消
  }
}

const handleUpdateStatus = async (id: number, status: string) => {
  const success = await iterationStore.editIteration(id, { status } as any)
  if (success) {
    ElMessage.success('状态已更新')
    fetchList()
  }
}

const handleUpdatePriority = async (id: number, priority: string) => {
  const success = await iterationStore.editIteration(id, { priority } as any)
  if (success) {
    ElMessage.success('优先级已更新')
  }
}

const handleAutoSave = async (data: Partial<Iteration>) => {
  if (data.id) {
    await iterationStore.editIteration(data.id, data)
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await iterationStore.editIteration(formData.id, {
        issueNumber: formData.issueNumber,
        projectCode: formData.projectCode,
        title: formData.title,
        issueUrl: formData.issueUrl,
        status: formData.status,
        priority: formData.priority
      })
      ElMessage.success('更新成功')
    } else {
      await iterationStore.addIteration({
        issueNumber: formData.issueNumber,
        projectCode: formData.projectCode,
        title: formData.title,
        issueUrl: formData.issueUrl,
        status: formData.status as any,
        priority: formData.priority as any
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

const showImportDialog = async () => {
  try {
    const response = await getImportConfig()
    if (response.data) {
      importDirPath.value = response.data.baseDirPath
    }
  } catch (error) {
    console.error('获取导入配置失败:', error)
  }
  importDialogVisible.value = true
}

const handleImport = async () => {
  if (!importDirPath.value) {
    ElMessage.warning('请输入目录路径')
    return
  }
  importing.value = true
  try {
    const response = await importFromDirectory(importDirPath.value)
    ElMessage.success(response.message || '导入成功')
    importDialogVisible.value = false
    fetchList()
  } catch (error: any) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    importing.value = false
  }
}

const handleRefreshDirs = async () => {
  refreshing.value = true
  try {
    // 检测新文件夹并导入
    const checkResponse = await checkNewFolders()
    if (checkResponse.data && checkResponse.data.imported > 0) {
      ElMessage.success(`发现 ${checkResponse.data.imported} 个新文件夹已导入`)
    } else if (checkResponse.data && checkResponse.data.message) {
      // 显示后端返回的消息（如"未配置导入目录"）
      ElMessage.info(checkResponse.data.message)
    }

    // 刷新所有需求的文件列表
    const refreshResponse = await refreshFileLists()
    if (refreshResponse.data && refreshResponse.data.refreshed > 0) {
      ElMessage.success(`已刷新 ${refreshResponse.data.refreshed} 个需求的文件列表`)
    }

    // 刷新列表
    fetchList()
  } catch (error: any) {
    console.error('刷新目录失败:', error)
    ElMessage.error(error.message || '刷新失败')
  } finally {
    refreshing.value = false
  }
}

const performAutoSync = async () => {
  try {
    const response = await autoSync()
    if (response.data && response.data.synced > 0) {
      fetchList()
    }
  } catch (error) {
    console.error('自动同步失败:', error)
  }
}

const checkNewFoldersPeriodically = async () => {
  try {
    const response = await checkNewFolders()
    if (response.data && response.data.imported > 0) {
      ElMessage.info(`发现 ${response.data.imported} 个新文件夹已自动导入`)
      fetchList()
    }
  } catch (error) {
    console.error('检测新文件夹失败:', error)
  }
}

onMounted(() => {
  fetchList()
  // 禁用自动同步，避免覆盖用户手动关联的文件路径
  // autoSyncInterval = setInterval(performAutoSync, 10000)
  // folderCheckInterval = setInterval(checkNewFoldersPeriodically, 10000)
})

onUnmounted(() => {
  if (autoSyncInterval) {
    clearInterval(autoSyncInterval)
  }
  if (folderCheckInterval) {
    clearInterval(folderCheckInterval)
  }
})
</script>

<style scoped>
.iteration-board {
  display: flex;
  height: calc(100vh - 120px);
  min-height: 500px;
  background: #f7f8fa;
  border-radius: 8px;
  overflow: hidden;
}

/* 左侧面板 */
.board-left {
  width: 340px;
  min-width: 300px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.left-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.board-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.left-filters {
  padding: 12px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-row {
  display: flex;
  gap: 8px;
}

.filter-row .el-select {
  flex: 1;
}

.issue-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
}

.issue-card {
  padding: 8px 8px;
  margin-bottom: 6px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.issue-card:hover {
  border-color: #c9d1db;
  background: #fafbfc;
}

.issue-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.issue-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.issue-number {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
}

.issue-project {
  font-size: 12px;
  color: #86909c;
}

.todos-tag {
  margin-left: auto;
  font-weight: 600;
}

.issue-card-title {
  font-size: 14px;
  color: #1d2129;
  line-height: 1.5;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.issue-card-title.status-done {
  color: #909399;
}

.issue-card-footer {
  display: flex;
  align-items: center;
  gap: 6px;
}

.issue-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-left: auto;
}

.left-pagination {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
}

/* 右侧面板 */
.board-right {
  flex: 1;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
