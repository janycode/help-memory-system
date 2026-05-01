<template>
  <div class="project-list">
    <div class="page-header">
      <h2>代码仓库</h2>
      <div style="display: flex; gap: 8px; align-items: center;">
        <ColumnSelector :columns="columnDefs" @toggle="toggle" @reset="reset" />
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增代码仓库
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <div class="search-inputs">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索代码仓库名称、描述..."
          clearable
          @keyup.enter="handleSearch"
          class="search-input"
        />
        <el-select v-model="filterStatus" placeholder="代码仓库状态" clearable @change="handleFilter" class="filter-select">
          <el-option
            v-for="item in repositoryStatusOptions"
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

    <el-table :data="projectStore.repositories" v-loading="projectStore.isLoading" stripe>
      <el-table-column prop="name" label="代码仓库名称" min-width="150" v-if="isVisible('name')" />
      <el-table-column prop="status" label="状态" width="110" v-if="isVisible('status')">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="projectFullName" label="项目全称" width="120" v-if="isVisible('projectFullName')" />
      <el-table-column prop="port" label="端口号" min-width="100" v-if="isVisible('port')" />
      <el-table-column prop="codeRepository" label="代码仓库" width="150" v-if="isVisible('codeRepository')">
        <template #default="{ row }">
          <UrlLink v-if="row.codeRepository" :url="row.codeRepository" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleView(row)">查看</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="primary" link @click="copyProjectInfo(row)">文本复制</el-button>
          <el-popconfirm title="确定删除该项目吗？" @confirm="handleDelete(row.id)">
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
        :total="projectStore.pagination?.totalElements || 0"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="viewMode ? '代码仓库详情' : isEdit ? '编辑代码仓库' : '新增代码仓库'"
      width="700px"
      destroy-on-close
    >
      <el-form :model="formData" :rules="viewMode ? {} : rules" ref="formRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="代码名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入代码名称" :disabled="viewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="代码状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择代码状态" :disabled="viewMode">
                <el-option
                  v-for="item in repositoryStatusOptions"
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
              <el-input v-model="formData.projectFullName" placeholder="请输入项目全称" :disabled="viewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="端口号" prop="port">
              <el-input v-model="formData.port" placeholder="请输入端口号" :disabled="viewMode" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="代码仓库" prop="codeRepository">
              <el-input v-model="formData.codeRepository" placeholder="请输入代码仓库地址" :disabled="viewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部署路径" prop="deploymentPath">
              <el-input v-model="formData.deploymentPath" placeholder="请输入部署路径" :disabled="viewMode" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="文档位置" prop="documentPath">
          <el-input v-model="formData.documentPath" placeholder="请输入文档位置" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="团队成员" prop="teamMembers">
          <el-input v-model="formData.teamMembers" type="textarea" :rows="2" placeholder="请输入团队成员" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="备注" prop="notes">
          <el-input v-model="formData.notes" type="textarea" :rows="2" placeholder="请输入备注" :disabled="viewMode" />
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
import { Plus, Search } from '@element-plus/icons-vue'
import { useProjectStore } from '@/stores/project'
import { useDictStore } from '@/stores/dict'
import type { Repository } from '@/types/project'
import ColumnSelector from '@/components/ColumnSelector.vue'
import { useColumnVisibility, type ColumnDef } from '@/composables/useColumnVisibility'
import UrlLink from '@/components/UrlLink.vue'

const projectStore = useProjectStore()
const dictStore = useDictStore()

const searchKeyword = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const isEdit = ref(false)
const viewMode = ref(false)
const submitting = ref(false)
const formRef = ref()

// 列自定义
const columnDefs: ColumnDef[] = [
  { key: 'name', label: '代码仓库名称', visible: true, minWidth: '150' },
  { key: 'status', label: '状态', visible: true, width: '110' },
  { key: 'projectFullName', label: '项目全称', visible: true, width: '120' },
  { key: 'port', label: '端口号', visible: true, minWidth: '100' },
  { key: 'codeRepository', label: '代码仓库', visible: true, width: '150' }
]
const { isVisible, toggle, reset } = useColumnVisibility('project-list', columnDefs)

const formData = reactive({
  id: 0,
  name: '',
  projectFullName: '',
  status: '',
  codeRepository: '',
  documentPath: '',
  deploymentPath: '',
  port: '',
  teamMembers: '',
  notes: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入代码仓库名称', trigger: 'blur' }]
}

const repositoryStatusOptions = computed(() => {
  return dictStore.getDictOptions('project_status')
})

const getStatusTagType = (status: string) => {
  const map: Record<string, string> = {
    planning: 'info',
    developing: '',
    testing: 'warning',
    released: 'success',
    maintenance: '',
    deprecated: 'danger'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: string) => {
  const option = repositoryStatusOptions.value.find(o => o.value === status)
  return option?.label || status || '未设置'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const fetchList = () => {
  projectStore.fetchProjects({
    page: currentPage.value - 1,
    size: pageSize.value
  })
}

const handleSearch = () => {
  if (searchKeyword.value) {
    projectStore.searchProjectList(searchKeyword.value)
  } else {
    fetchList()
  }
}

const handleFilter = () => {
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

const handleAdd = () => {
  isEdit.value = false
  viewMode.value = false
  Object.assign(formData, {
    id: 0,
    name: '',
    projectFullName: '',
    status: '',
    codeRepository: '',
    documentPath: '',
    deploymentPath: '',
    port: '',
    teamMembers: '',
    notes: '',
    description: ''
  })
  dialogVisible.value = true
}

const handleView = (row: Repository) => {
  isEdit.value = false
  viewMode.value = true
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    projectFullName: row.projectFullName,
    status: row.status,
    codeRepository: row.codeRepository,
    documentPath: row.documentPath,
    deploymentPath: row.deploymentPath || '',
    port: row.port,
    teamMembers: row.teamMembers || '',
    notes: row.notes || '',
    description: row.description
  })
  dialogVisible.value = true
}

const handleEdit = (row: Repository) => {
  isEdit.value = true
  viewMode.value = false
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    projectFullName: row.projectFullName,
    status: row.status,
    codeRepository: row.codeRepository,
    documentPath: row.documentPath,
    deploymentPath: row.deploymentPath || '',
    port: row.port,
    teamMembers: row.teamMembers || '',
    notes: row.notes || '',
    description: row.description
  })
  dialogVisible.value = true
}

const handleDelete = async (id: number) => {
  const success = await projectStore.removeProject(id)
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
      await projectStore.editProject(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await projectStore.addProject(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
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

const copyProjectInfo = async (row: Repository) => {
  const statusLabel = getStatusLabel(row.status)
  const info = `
${row.name}-${statusLabel}
项目全称: ${row.projectFullName || '-'}
地址: \`${row.codeRepository || '-'}\`
端口号: ${row.port || '-'}
文档位置: ${row.documentPath || '-'}
描述: ${row.description || '-'}
创建时间: ${formatDate(row.createdAt)}
  `.trim()
  await copyToClipboard(info)
}

onMounted(() => {
  dictStore.fetchDictDataByType('project_status')
  fetchList()
})
</script>

<style scoped>
.project-list {
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

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
