<template>
  <div class="component-list">
    <div class="page-header">
      <h2>技术组件</h2>
      <div style="display: flex; gap: 8px; align-items: center;">
        <ColumnSelector :columns="columnDefs" @toggle="toggle" @reset="reset" />
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增组件
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-inputs">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索组件名称、描述..."
          clearable
          @keyup.enter="handleSearch"
          class="search-input"
        />
        <el-select v-model="filterCategory" placeholder="组件分类" clearable @change="handleFilter" class="filter-select">
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

    <!-- 组件列表 -->
    <el-table :data="componentStore.components" v-loading="componentStore.isLoading" stripe>
      <el-table-column prop="name" label="组件名称" min-width="150" v-if="isVisible('name')" />
      <el-table-column prop="environmentType" label="环境" max-width="110" v-if="isVisible('environment')">
        <template #default="{ row }">
          <el-tag v-if="row.environmentType" :type="getEnvironmentTypeTagType(row.environmentType)">
            {{ getEnvironmentTypeLabel(row.environmentType) }}
          </el-tag>
          <el-tag v-else-if="row.environmentId" :type="getEnvironmentTagType(row.environmentId)">
            {{ getEnvironmentLabel(row.environmentId) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" min-width="150" v-if="isVisible('category')">
        <template #default="{ row }">
          <el-tag :type="getCategoryTagType(row.category)">{{ getCategoryLabel(row.category) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="100" v-if="isVisible('version')" />
      <el-table-column prop="url" label="访问地址" min-width="400" v-if="isVisible('url')">
        <template #default="{ row }">
          <UrlLink v-if="row.url" :url="row.url" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="用户名" width="150" v-if="isVisible('username')">
        <template #default="{ row }">
          <span v-if="row.username" class="copyable-text" @click="copyToClipboard(row.username)">
            {{ row.username }}
            <el-icon class="copy-icon"><CopyDocument /></el-icon>
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="密码" width="150" v-if="isVisible('password')">
        <template #default="{ row }">
          <PasswordDisplay v-if="row.password" :password="row.password" />
          <span v-else>-</span>
        </template>
      </el-table-column>
<!--      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />-->
      <el-table-column label="操作" width="350" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleView(row)">查看</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="primary" link @click="showConnectionStrings(row)">连接</el-button>
          <el-button type="primary" link @click="copyComponentInfo(row)">文本复制</el-button>
          <el-popconfirm title="确定删除该组件吗？" @confirm="handleDelete(row.id)">
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
        :total="componentStore.pagination?.totalElements || 0"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 新增/编辑/查看对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="viewMode ? '技术组件详情' : isEdit ? '编辑组件' : '新增组件'"
      width="600px"
      destroy-on-close
    >
      <el-form :model="formData" :rules="viewMode ? {} : rules" ref="formRef" label-width="100px">
        <el-form-item label="组件名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入组件名称" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="组件分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择组件分类" :disabled="viewMode">
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="环境" prop="environmentType">
          <el-select v-model="formData.environmentType" placeholder="请选择环境类型" clearable :disabled="viewMode" @change="handleEnvironmentTypeChange">
            <el-option
              v-for="item in environmentTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input v-model="formData.version" placeholder="请输入版本号" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="访问地址" prop="url">
          <el-input v-model="formData.url" placeholder="请输入访问地址" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password
            :disabled="viewMode"
            v-if="!viewMode"
          />
          <PasswordDisplay v-else :password="formData.password || ''" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述" :disabled="viewMode" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" v-if="!viewMode">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting" v-if="!viewMode">确定</el-button>
        <el-button @click="dialogVisible = false" v-else>关闭</el-button>
      </template>
    </el-dialog>

    <!-- 连接信息对话框 -->
    <ConnectionStringDialog ref="connectionDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, CopyDocument } from '@element-plus/icons-vue'
import { useComponentStore } from '@/stores/component'
import { useEnvironmentStore } from '@/stores/environment'
import { useDictStore } from '@/stores/dict'
import { useKeyboardShortcuts } from '@/composables/useKeyboardShortcuts'
import type { TechnicalComponent } from '@/types/component'
import type { Shortcut } from '@/composables/useKeyboardShortcuts'
import UrlLink from '@/components/UrlLink.vue'
import PasswordDisplay from '@/components/PasswordDisplay.vue'
import ConnectionStringDialog from '@/components/ConnectionStringDialog.vue'
import ColumnSelector from '@/components/ColumnSelector.vue'
import { useColumnVisibility, type ColumnDef } from '@/composables/useColumnVisibility'

const componentStore = useComponentStore()
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
const connectionDialogRef = ref()

// 列自定义
const columnDefs: ColumnDef[] = [
  { key: 'name', label: '组件名称', visible: true, minWidth: '150' },
  { key: 'environment', label: '环境', visible: true, width: '110' },
  { key: 'category', label: '分类', visible: true, minWidth: '150' },
  { key: 'version', label: '版本', visible: true, width: '100' },
  { key: 'url', label: '访问地址', visible: true, minWidth: '400' },
  { key: 'username', label: '用户名', visible: true, width: '150' },
  { key: 'password', label: '密码', visible: true, width: '150' }
]
const { isVisible, toggle, reset } = useColumnVisibility('component-list', columnDefs)

const formData = reactive({
  id: 0,
  name: '',
  category: '' as any,
  environmentId: null as number | null,
  environmentType: '',
  version: '',
  url: '',
  username: '',
  password: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入组件名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择组件分类', trigger: 'change' }]
}

// 从字典获取选项
const categoryOptions = computed(() => {
  return dictStore.getDictOptions('component_category')
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
    Database: 'danger',
    Cache: 'warning',
    MessageQueue: 'success',
    API: '',
    Storage: 'info',
    Monitoring: 'warning',
    Authentication: 'danger',
    Other: 'info'
  }
  return map[category] || 'info'
}

const getEnvironmentLabel = (environmentId: number) => {
  const env = environmentStore.environments.find(e => e.id === environmentId)
  if (!env) return '未关联'

  const typeLabel = getEnvironmentTypeLabel(env.type)
  return `${env.name} (${typeLabel})`
}

const getEnvironmentTagType = (environmentId: number) => {
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


const fetchList = () => {
  componentStore.fetchComponents({
    page: currentPage.value - 1,
    size: pageSize.value,
    category: filterCategory.value || undefined,
    environmentType: filterEnvironmentType.value || undefined
  })
}

const handleSearch = () => {
  if (searchKeyword.value) {
    componentStore.searchComponentList(searchKeyword.value)
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
    version: '',
    url: '',
    username: '',
    password: '',
    description: ''
  })
  dialogVisible.value = true
}

const handleView = (row: TechnicalComponent) => {
  isEdit.value = false
  viewMode.value = true
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    category: row.category,
    environmentId: row.environmentId,
    environmentType: row.environmentType || '',
    version: row.version,
    url: row.url,
    username: row.username,
    password: row.password,
    description: row.description
  })
  dialogVisible.value = true
}

const handleEdit = (row: TechnicalComponent) => {
  isEdit.value = true
  viewMode.value = false
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    category: row.category,
    environmentId: row.environmentId,
    environmentType: row.environmentType || '',
    version: row.version,
    url: row.url,
    username: row.username,
    password: row.password,
    description: row.description
  })
  dialogVisible.value = true
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

const showConnectionStrings = (row: TechnicalComponent) => {
  connectionDialogRef.value?.open(row)
}

const copyComponentInfo = async (row: TechnicalComponent) => {
  const categoryLabel = getCategoryLabel(row.category)
  const envLabel = getEnvironmentLabel(row.environmentId)
  const info = `
${row.name}-${categoryLabel}
地址: \`${row.url || '-'}\`
账号: ${row.username || '-'}
密码: ${row.password || '-'}
版本: ${row.version || '-'}
环境: ${envLabel}
  `.trim()
  await copyToClipboard(info)
}

const handleDelete = async (id: number) => {
  const success = await componentStore.removeComponent(id)
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
      await componentStore.editComponent(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await componentStore.addComponent(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  // 加载字典数据
  await dictStore.fetchDictDataByType('component_category')
  await dictStore.fetchDictDataByType('environment_type')
  // 加载环境数据
  await environmentStore.fetchEnvironments()
  // 加载组件列表
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
    description: '新增组件'
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
.component-list {
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

.copyable-text {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.copyable-text:hover {
  color: #409eff;
}

.copy-icon {
  font-size: 14px;
  opacity: 0.7;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
