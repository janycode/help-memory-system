<template>
  <div class="process-list">
    <div class="page-header">
      <h2>业务流程</h2>
      <div style="display: flex; gap: 8px; align-items: center;">
        <ColumnSelector :columns="columnDefs" @toggle="toggle" @reset="reset" />
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增流程
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-inputs">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索流程名称、描述..."
          clearable
          @keyup.enter="handleSearch"
          class="search-input"
        />
        <el-select v-model="filterCategory" placeholder="流程分类" clearable @change="handleFilter" class="filter-select">
          <el-option
            v-for="item in categoryOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filterEnvironmentType" placeholder="环境类型" clearable @change="handleFilter" class="filter-select">
          <el-option
            v-for="item in environmentTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </div>
      <el-button type="primary" @click="handleSearch" class="search-button">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
    </div>

    <!-- 流程列表 -->
    <el-table :data="processStore.processes" v-loading="processStore.isLoading" stripe>
      <el-table-column prop="name" label="流程名称" min-width="100" v-if="isVisible('name')" />
      <el-table-column prop="environmentType" label="环境" width="110" v-if="isVisible('environment')">
        <template #default="{ row }">
          <el-tag v-if="row.environmentType" :type="getEnvironmentTypeTagType(row.environmentType)">
            {{ getEnvironmentTypeLabel(row.environmentType) }}
          </el-tag>
          <el-tag v-else-if="row.environmentId" :type="getEnvironmentTagType(row.environmentId)">
            {{ getEnvironmentLabel(row.environmentId) }}
          </el-tag>
          <span v-else>不区分环境</span>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="120" v-if="isVisible('category')">
        <template #default="{ row }">
          <el-tag :type="getCategoryTagType(row.category)">{{ getCategoryLabel(row.category) }}</el-tag>
        </template>
      </el-table-column>
<!--      <el-table-column prop="priority" label="优先级" width="80" />-->
      <el-table-column prop="description" label="描述" min-width="100" show-overflow-tooltip v-if="isVisible('description')" />
      <el-table-column prop="processFlow" label="流程步骤" min-width="200" show-overflow-tooltip v-if="isVisible('processFlow')" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleView(row)">查看</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="primary" link @click="copyProcessInfo(row)">文本复制</el-button>
          <el-popconfirm title="确定删除该流程吗？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="processStore.pagination?.totalElements || 0"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 新增/编辑/查看对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="viewMode ? '业务流程详情' : isEdit ? '编辑流程' : '新增流程'"
      width="700px"
      destroy-on-close
    >
      <el-form :model="formData" :rules="viewMode ? {} : rules" ref="formRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="流程名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入流程名称" :disabled="viewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="流程分类" prop="category">
              <el-select v-model="formData.category" placeholder="请选择流程分类" :disabled="viewMode">
                <el-option
                  v-for="item in categoryOptions"
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
            <el-form-item label="环境" prop="environmentType">
              <el-select v-model="formData.environmentType" placeholder="请选择环境类型" clearable :disabled="viewMode" @change="handleEnvironmentTypeChange">
                <el-option label="不区分环境" :value="''" />
                <el-option
                  v-for="item in environmentTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="formData.priority" :min="0" :max="100" :disabled="viewMode" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="流程步骤" prop="processFlow">
          <el-input v-model="formData.processFlow" type="textarea" :rows="5" placeholder="请输入流程步骤说明" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="注意事项" prop="precautions">
          <el-input v-model="formData.precautions" type="textarea" :rows="3" placeholder="请输入注意事项" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="检查清单" prop="checklist">
          <el-input v-model="formData.checklist" type="textarea" :rows="3" placeholder="请输入检查清单" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="相关文档" prop="relatedDocuments">
          <el-input v-model="formData.relatedDocuments" type="textarea" :rows="2" placeholder="请输入相关文档链接" :disabled="viewMode" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" v-if="!viewMode">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting" v-if="!viewMode">确定</el-button>
        <el-button @click="dialogVisible = false" v-else>关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useProcessStore } from '@/stores/process'
import { useEnvironmentStore } from '@/stores/environment'
import { useDictStore } from '@/stores/dict'
import { useKeyboardShortcuts } from '@/composables/useKeyboardShortcuts'
import ColumnSelector from '@/components/ColumnSelector.vue'
import { useColumnVisibility, type ColumnDef } from '@/composables/useColumnVisibility'
import type { BusinessProcess } from '@/types/process'
import type { Shortcut } from '@/composables/useKeyboardShortcuts'

const processStore = useProcessStore()
const environmentStore = useEnvironmentStore()
const dictStore = useDictStore()

const searchKeyword = ref('')
const filterCategory = ref('')
const filterEnvironmentType = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const isEdit = ref(false)
const viewMode = ref(false)
const submitting = ref(false)
const formRef = ref()

// 列自定义
const columnDefs: ColumnDef[] = [
  { key: 'name', label: '流程名称', visible: true, minWidth: '100' },
  { key: 'environment', label: '环境', visible: true, width: '110' },
  { key: 'category', label: '分类', visible: true, width: '120' },
  { key: 'description', label: '描述', visible: true, minWidth: '100' },
  { key: 'processFlow', label: '流程步骤', visible: true, minWidth: '200' }
]
const { isVisible, toggle, reset } = useColumnVisibility('process-list', columnDefs)

const formData = reactive({
  id: 0,
  name: '',
  category: '' as any,
  environmentId: null as number | null,
  environmentType: '',
  priority: 0,
  description: '',
  processFlow: '',
  precautions: '',
  checklist: '',
  relatedDocuments: ''
})

const rules = {
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择流程分类', trigger: 'change' }]
}

// 从字典获取选项
const categoryOptions = computed(() => {
  return dictStore.getDictOptions('process_category')
})

const environmentTypeOptions = computed(() => {
  return dictStore.getDictOptions('environment_type')
})

const getCategoryLabel = (category: string) => {
  const option = categoryOptions.value.find(o => o.value === category)
  return option?.label || category
}

const getCategoryTagType = (category: string) => {
  const map: Record<string, string> = {
    release: 'success',
    business: '',
    operation: 'warning',
    incident: 'danger',
    maintenance: 'info',
    security: 'danger',
    other: 'info'
  }
  return map[category] || 'info'
}

const getEnvironmentLabel = (environmentId?: number) => {
  if (!environmentId) {
    return '不区分环境'
  }
  const env = environmentStore.environments.find(e => e.id === environmentId)
  if (!env) return '未知环境'

  const typeLabel = getEnvironmentTypeLabel(env.type)
  return `${env.name} (${typeLabel})`
}

const getEnvironmentTagType = (environmentId?: number) => {
  if (!environmentId) return 'info'

  const env = environmentStore.environments.find(e => e.id === environmentId)
  if (!env) return 'info'

  return getEnvironmentTypeTagType(env.type)
}

const getEnvironmentTypeTagType = (type: string) => {
  const map: Record<string, string> = {
    DEV: 'success',
    TEST: 'warning',
    STAGING: 'info',
    PROD: 'danger',
    DEMO: ''
  }
  return map[type] || 'info'
}

const getEnvironmentTypeLabel = (type: string) => {
  const environmentTypeOptions = dictStore.getDictOptions('environment_type')
  const option = environmentTypeOptions.find(o => o.value === type)
  return option?.label || type
}


const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString()
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

const copyProcessInfo = async (row: BusinessProcess) => {
  const categoryLabel = getCategoryLabel(row.category)
  const envLabel = getEnvironmentLabel(row.environmentId)
  const info = `
${row.name}-${categoryLabel}
描述: ${row.description || '-'}
流程步骤: ${row.processFlow || '-'}
注意事项: ${row.precautions || '-'}
优先级: ${row.priority}
环境: ${envLabel}
创建时间: ${formatDate(row.createdAt)}
  `.trim()
  await copyToClipboard(info)
}

const fetchList = () => {
  processStore.fetchProcesses({
    page: currentPage.value - 1,
    size: pageSize.value,
    category: filterCategory.value || undefined,
    environmentType: filterEnvironmentType.value || undefined
  })
}

const handleSearch = () => {
  if (searchKeyword.value) {
    processStore.searchProcessList(searchKeyword.value)
  } else {
    fetchList()
  }
}

const handleFilter = () => {
  fetchList()
}

const handleEnvironmentTypeChange = (environmentType: string) => {
  formData.environmentType = environmentType
  if (environmentType) {
    const env = environmentStore.environments.find(e => e.type === environmentType)
    formData.environmentId = env?.id ?? null
  } else {
    formData.environmentId = null
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  fetchList()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchList()
}

const handleAdd = () => {
  isEdit.value = false
  viewMode.value = false
  Object.assign(formData, {
    id: 0,
    name: '',
    category: '',
    environmentId: null,
    environmentType: '',
    priority: 0,
    description: '',
    processFlow: '',
    precautions: '',
    checklist: '',
    relatedDocuments: ''
  })
  dialogVisible.value = true
}

const handleView = (row: BusinessProcess) => {
  isEdit.value = false
  viewMode.value = true
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    category: row.category,
    environmentId: row.environmentId,
    environmentType: row.environmentType || '',
    priority: row.priority,
    description: row.description,
    processFlow: row.processFlow,
    precautions: row.precautions,
    checklist: row.checklist || '',
    relatedDocuments: row.relatedDocuments || ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: BusinessProcess) => {
  isEdit.value = true
  viewMode.value = false
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    category: row.category,
    environmentId: row.environmentId,
    environmentType: row.environmentType || '',
    priority: row.priority,
    description: row.description,
    processFlow: row.processFlow,
    precautions: row.precautions,
    checklist: row.checklist || '',
    relatedDocuments: row.relatedDocuments || ''
  })
  dialogVisible.value = true
}

const handleDelete = async (id: number) => {
  const success = await processStore.removeProcess(id)
  if (success) {
    ElMessage.success('删除成功')
    fetchList()
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true

  try {
    if (isEdit.value) {
      await processStore.editProcess(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await processStore.addProcess(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  // 加载字典数据
  dictStore.fetchDictDataByType('process_category')
  dictStore.fetchDictDataByType('environment_type')
  // 加载环境数据
  environmentStore.fetchEnvironments()
  // 加载流程列表
  fetchList()
})

// 页面快捷键
const pageShortcuts: Shortcut[] = [
  {
    key: 'n',
    ctrl: true,
    handler: () => {
      if (!dialogVisible.value) handleAdd()
    },
    description: '新增流程'
  },
  {
    key: 's',
    ctrl: true,
    handler: () => {
      if (dialogVisible.value && !viewMode.value) handleSubmit()
    },
    description: '保存'
  }
]
useKeyboardShortcuts(pageShortcuts)
</script>

<style scoped>
.process-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.search-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.search-inputs {
  display: flex;
  gap: 16px;
  flex: 1;
}

.search-input {
  width: 600px;
}

.filter-select {
  width: auto;
  min-width: 120px;
}

.search-button {
  margin-left: auto;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
