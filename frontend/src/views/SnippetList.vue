<template>
  <div class="snippet-list">
    <div class="page-header">
      <h2>代码片段</h2>
      <div style="display: flex; gap: 8px; align-items: center;">
        <ColumnSelector :columns="columnDefs" @toggle="toggle" @reset="reset" />
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增代码片段
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <div class="search-inputs">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索代码片段标题、描述、标签..."
          clearable
          @keyup.enter="handleSearch"
          class="search-input"
        />
        <el-select v-model="filterLanguage" placeholder="编程语言" clearable @change="handleFilter" class="filter-select">
          <el-option label="Java" value="java" />
          <el-option label="Vue" value="vue" />
          <el-option label="JavaScript" value="javascript" />
          <el-option label="SQL" value="sql" />
        </el-select>
      </div>
      <el-button type="primary" @click="handleSearch" class="search-button">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
    </div>

    <el-table :data="snippetStore.snippets" v-loading="snippetStore.isLoading" stripe>
      <el-table-column prop="title" label="标题" min-width="150" v-if="isVisible('title')" />
      <el-table-column prop="language" label="语言" width="120" v-if="isVisible('language')">
        <template #default="{ row }">
          <el-tag :type="getLanguageTagType(row.language)">{{ getLanguageLabel(row.language) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="tags" label="标签" width="150" show-overflow-tooltip v-if="isVisible('tags')" />
      <el-table-column label="代码预览" width="200" v-if="isVisible('codePreview')">
        <template #default="{ row }">
          <el-popover
            placement="top"
            trigger="hover"
            :width="500"
            :show-after="300"
            :hide-after="100"
          >
            <template #reference>
              <div class="code-preview-cell">
                {{ getCodePreview(row.code) }}
              </div>
            </template>
            <div class="code-popover">
              <pre class="code-content"><code :class="`language-${row.language}`" v-html="getHighlightedCode(row.code, row.language)"></code></pre>
            </div>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip v-if="isVisible('description')" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleView(row)">查看</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="primary" link @click="copyCode(row)">复制代码</el-button>
          <el-popconfirm title="确定删除该代码片段吗？" @confirm="handleDelete(row.id)">
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
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="viewMode ? '代码片段详情' : isEdit ? '编辑代码片段' : '新增代码片段'"
      width="800px"
      destroy-on-close
    >
      <el-form :model="formData" :rules="viewMode ? {} : rules" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入标题" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="编程语言" prop="language">
          <el-select v-model="formData.language" placeholder="请选择编程语言" :disabled="viewMode" class="full-width">
            <el-option label="Java" value="java" />
            <el-option label="Vue" value="vue" />
            <el-option label="JavaScript" value="javascript" />
            <el-option label="SQL" value="sql" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input v-model="formData.tags" placeholder="请输入标签，多个标签用逗号分隔" :disabled="viewMode" />
        </el-form-item>
        <el-form-item label="代码" prop="code">
          <div v-if="viewMode" class="code-preview">
            <div class="code-header">
              <span class="language-tag">{{ getLanguageLabel(formData.language) }}</span>
              <el-button type="primary" link @click="copyCodeToClipboard(formData.code)">复制代码</el-button>
            </div>
            <pre><code :class="`language-${formData.language}`" v-html="highlightedCode"></code></pre>
          </div>
          <el-input
            v-else
            v-model="formData.code"
            type="textarea"
            :rows="15"
            placeholder="请输入代码"
            class="code-textarea"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="请输入描述" :disabled="viewMode" />
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
import ColumnSelector from '@/components/ColumnSelector.vue'
import { useColumnVisibility, type ColumnDef } from '@/composables/useColumnVisibility'
import { useSnippetStore } from '@/stores/snippet'
import type { CodeSnippet } from '@/api/snippet'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const searchKeyword = ref('')
const filterLanguage = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const viewMode = ref(false)
const submitting = ref(false)
const formRef = ref()

// 列自定义
const columnDefs: ColumnDef[] = [
  { key: 'title', label: '标题', visible: true, minWidth: '150' },
  { key: 'language', label: '语言', visible: true, width: '120' },
  { key: 'tags', label: '标签', visible: true, width: '150' },
  { key: 'codePreview', label: '代码预览', visible: true, width: '200' },
  { key: 'description', label: '描述', visible: true, minWidth: '200' }
]
const { isVisible, toggle, reset } = useColumnVisibility('snippet-list', columnDefs)

const snippetStore = useSnippetStore()

const formData = reactive({
  id: 0,
  title: '',
  description: '',
  language: '',
  code: '',
  tags: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  language: [{ required: true, message: '请选择编程语言', trigger: 'change' }],
  code: [{ required: true, message: '请输入代码', trigger: 'blur' }]
}

const highlightedCode = computed(() => {
  if (!formData.code || !formData.language) {
    return formData.code
  }

  try {
    const language = formData.language === 'javascript' ? 'javascript' :
                    formData.language === 'vue' ? 'xml' :
                    formData.language

    return hljs.highlight(formData.code, { language }).value
  } catch (error) {
    return formData.code
  }
})

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


const fetchSnippets = async () => {
  await snippetStore.fetchSnippets({
    page: currentPage.value - 1,
    size: pageSize.value
  })
  if (snippetStore.pagination) {
    total.value = snippetStore.pagination.totalElements
  }
}

const handleSearch = async () => {
  if (searchKeyword.value) {
    await snippetStore.searchSnippetList(searchKeyword.value)
    total.value = snippetStore.snippets.length
  } else {
    fetchSnippets()
  }
}

const handleFilter = async () => {
  if (filterLanguage.value) {
    await snippetStore.getSnippetsByLanguageType(filterLanguage.value)
    total.value = snippetStore.snippets.length
  } else {
    fetchSnippets()
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  fetchSnippets()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchSnippets()
}

const handleAdd = () => {
  isEdit.value = false
  viewMode.value = false
  Object.assign(formData, {
    id: 0,
    title: '',
    description: '',
    language: '',
    code: '',
    tags: ''
  })
  dialogVisible.value = true
}

const handleView = (row: CodeSnippet) => {
  isEdit.value = false
  viewMode.value = true
  Object.assign(formData, {
    id: row.id,
    title: row.title,
    description: row.description,
    language: row.language,
    code: row.code,
    tags: row.tags
  })
  dialogVisible.value = true
}

const handleEdit = (row: CodeSnippet) => {
  isEdit.value = true
  viewMode.value = false
  Object.assign(formData, {
    id: row.id,
    title: row.title,
    description: row.description,
    language: row.language,
    code: row.code,
    tags: row.tags
  })
  dialogVisible.value = true
}

const handleDelete = async (id: number) => {
  const success = await snippetStore.removeSnippet(id)
  if (success) {
    ElMessage.success('删除成功')
    fetchSnippets()
  } else {
    ElMessage.error('删除失败')
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true

  try {
    if (isEdit.value) {
      const result = await snippetStore.editSnippet(formData.id, formData)
      if (result) {
        ElMessage.success('更新成功')
      } else {
        ElMessage.error('更新失败')
      }
    } else {
      const result = await snippetStore.addSnippet(formData)
      if (result) {
        ElMessage.success('创建成功')
      } else {
        ElMessage.error('创建失败')
      }
    }
    dialogVisible.value = false
    fetchSnippets()
  } finally {
    submitting.value = false
  }
}

const copyCode = async (row: CodeSnippet) => {
  try {
    await navigator.clipboard.writeText(row.code)
    ElMessage.success('代码已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
}

const getCodePreview = (code: string) => {
  if (!code) return '-'
  // 取代码的前50个字符作为预览
  const preview = code.substring(0, 50)
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

const copyCodeToClipboard = async (code: string) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('代码已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
}

onMounted(() => {
  fetchSnippets()
})
</script>

<style scoped>
.snippet-list {
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
  width: 400px;
}

.filter-select {
  width: 150px;
}

.search-button {
  margin-left: auto;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.full-width {
  width: 100%;
}

.code-textarea :deep(textarea) {
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
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

.code-preview-cell {
  font-family: monospace;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  padding: 4px 8px;
  background-color: #f5f5f5;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  max-height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-preview-cell:hover {
  background-color: #e8f4fd;
  border-color: #409eff;
}

.code-popover {
  max-height: 300px;
  overflow: auto;
}

.code-content {
  margin: 0;
  font-family: monospace;
  font-size: 12px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-all;
  color: #333;
}
</style>
