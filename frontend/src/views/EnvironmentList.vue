<template>
  <div class="environment-list">
    <div class="page-header">
      <h2>环境管理</h2>
      <div style="display: flex; gap: 8px; align-items: center;">
        <ColumnSelector :columns="columnDefs" @toggle="toggle" @reset="reset" />
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增环境
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <div class="search-inputs">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索环境名称、描述..."
          clearable
          @keyup.enter="handleSearch"
          class="search-input"
        />
        <el-select v-model="filterType" placeholder="环境类型" clearable @change="handleFilter" class="filter-select">
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

    <el-table :data="environmentStore.environments" v-loading="environmentStore.isLoading" stripe>
      <el-table-column prop="name" label="环境名称" max-width="100" v-if="isVisible('name')" />
      <el-table-column prop="type" label="环境" width="110" v-if="isVisible('type')">
        <template #default="{ row }">
          <el-tag :type="getTypeTagType(row.type)">{{ getTypeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="url" label="访问地址" min-width="200" v-if="isVisible('url')">
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
      <el-table-column label="密码" width="200" v-if="isVisible('password')">
        <template #default="{ row }">
          <PasswordDisplay v-if="row.password" :password="row.password" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleView(row)">查看</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="primary" link @click="copyEnvironmentInfo(row)">文本复制</el-button>
          <el-popconfirm title="确定删除该环境吗？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="environmentStore.pagination?.totalElements || 0"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="viewMode ? '环境详情' : isEdit ? '编辑环境' : '新增环境'"
      width="600px"
      destroy-on-close
    >
      <el-form :model="formData" :rules="viewMode ? {} : rules" ref="formRef" label-width="100px">
        <el-form-item label="环境名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入环境名称" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="环境类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择环境类型" :disabled="viewMode">
            <el-option
              v-for="item in environmentTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Plus, Search } from '@element-plus/icons-vue'
import { useEnvironmentStore } from '@/stores/environment'
import { useDictStore } from '@/stores/dict'
import type { Environment } from '@/types/environment'
import ColumnSelector from '@/components/ColumnSelector.vue'
import { useColumnVisibility, type ColumnDef } from '@/composables/useColumnVisibility'
import PasswordDisplay from '@/components/PasswordDisplay.vue'
import UrlLink from '@/components/UrlLink.vue'

const environmentStore = useEnvironmentStore()
const dictStore = useDictStore()

const searchKeyword = ref('')
const filterType = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const isEdit = ref(false)
const viewMode = ref(false)
const submitting = ref(false)
const formRef = ref()

// 列自定义
const columnDefs: ColumnDef[] = [
  { key: 'name', label: '环境名称', visible: true, width: '100' },
  { key: 'type', label: '环境', visible: true, width: '110' },
  { key: 'url', label: '访问地址', visible: true, minWidth: '200' },
  { key: 'username', label: '用户名', visible: true, width: '150' },
  { key: 'password', label: '密码', visible: true, width: '200' }
]
const { isVisible, toggle, reset } = useColumnVisibility('environment-list', columnDefs)

const formData = reactive({
  id: 0,
  name: '',
  type: '' as any,
  url: '',
  username: '',
  password: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入环境名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择环境类型', trigger: 'change' }]
}

const environmentTypeOptions = computed(() => {
  return dictStore.getDictOptions('environment_type')
})

const getTypeTagType = (type: string) => {
  const map: Record<string, string> = {
    DEV: 'success',
    TEST: 'warning',
    STAGING: 'info',
    PROD: 'danger',
    DEMO: ''
  }
  return map[type] || 'info'
}

const getTypeLabel = (type: string) => {
  const option = environmentTypeOptions.value.find(o => o.value === type)
  return option?.label || type
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

const copyEnvironmentInfo = async (row: Environment) => {
  const typeLabel = getTypeLabel(row.type)
  const info = `
${row.name}-${typeLabel}
地址: \`${row.url || '-'}\`
账号: ${row.username || '-'}
密码: ${row.password || '-'}
  `.trim()
  await copyToClipboard(info)
}

const fetchList = () => {
  environmentStore.fetchEnvironments({
    page: currentPage.value - 1,
    size: pageSize.value
  })
}

const handleSearch = () => {
  if (searchKeyword.value) {
    environmentStore.searchEnvironmentList(searchKeyword.value)
  } else {
    fetchList()
  }
}

const handleFilter = () => {
  if (filterType.value) {
    environmentStore.fetchEnvironments({ page: 0, size: pageSize.value })
  } else {
    fetchList()
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
    type: '',
    url: '',
    username: '',
    password: '',
    description: ''
  })
  dialogVisible.value = true
}

const handleView = (row: Environment) => {
  isEdit.value = false
  viewMode.value = true
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    type: row.type,
    url: row.url,
    username: row.username,
    password: row.password,
    description: row.description
  })
  dialogVisible.value = true
}

const handleEdit = (row: Environment) => {
  isEdit.value = true
  viewMode.value = false
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    type: row.type,
    url: row.url,
    username: row.username,
    password: row.password,
    description: row.description
  })
  dialogVisible.value = true
}

const handleDelete = async (id: number) => {
  const success = await environmentStore.removeEnvironment(id)
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
      await environmentStore.editEnvironment(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await environmentStore.addEnvironment(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  dictStore.fetchDictDataByType('environment_type')
  fetchList()
})
</script>

<style scoped>
.environment-list {
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
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
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
