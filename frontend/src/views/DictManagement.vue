<template>
  <div class="dict-management">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">字典管理</h1>
        <p class="page-description">管理系统字典类型和数据</p>
        <el-button type="primary" @click="showAddTypeDialog">
          <el-icon><Plus /></el-icon>
          新建字典类型
        </el-button>
      </div>

      <el-card class="content-card">
        <!-- 字典类型列表 -->
        <el-table :data="dictStore.dictTypes" style="width: 100%" stripe>
          <el-table-column prop="typeCode" label="类型编码" width="150" />
          <el-table-column prop="typeName" label="类型名称" width="150" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status ? 'success' : 'danger'">
                {{ scope.row.status ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column label="操作" width="250">
            <template #default="scope">
              <el-button size="small" @click="showEditTypeDialog(scope.row)">编辑</el-button>
              <el-button size="small" type="primary" @click="showDataManagement(scope.row)">数据管理</el-button>
              <el-button size="small" type="danger" @click="handleDeleteType(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 字典类型编辑对话框 -->
      <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="500px">
        <el-form :model="typeForm" :rules="typeRules" ref="typeFormRef" label-width="100px">
          <el-form-item label="类型编码" prop="typeCode">
            <el-input v-model="typeForm.typeCode" placeholder="请输入类型编码" />
          </el-form-item>
          <el-form-item label="类型名称" prop="typeName">
            <el-input v-model="typeForm.typeName" placeholder="请输入类型名称" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="typeForm.description" type="textarea" placeholder="请输入描述" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="typeForm.status" />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="typeDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="saveDictType">确定</el-button>
          </span>
        </template>
      </el-dialog>

      <!-- 字典数据管理对话框 -->
      <el-dialog v-model="dataDialogVisible" title="字典数据管理" width="800px">
        <template #header>
          <div class="dialog-header">
            <span>字典数据管理 - {{ currentType?.typeName }}</span>
            <el-button type="primary" size="small" @click="showAddDataDialog">新建数据</el-button>
          </div>
        </template>

        <el-table :data="currentTypeData" style="width: 100%" stripe>
          <el-table-column prop="dataValue" label="数据值" width="120" />
          <el-table-column prop="dataLabel" label="数据标签" width="150" />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.status ? 'success' : 'danger'">
                {{ scope.row.status ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" />
          <el-table-column label="操作" width="150">
            <template #default="scope">
              <el-button size="small" @click="showEditDataDialog(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteData(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-dialog>

      <!-- 字典数据编辑对话框 -->
      <el-dialog v-model="dataItemDialogVisible" :title="dataItemDialogTitle" width="500px">
        <el-form :model="dataForm" :rules="dataRules" ref="dataFormRef" label-width="100px">
          <el-form-item label="数据值" prop="dataValue">
            <el-input v-model="dataForm.dataValue" placeholder="请输入数据值" />
          </el-form-item>
          <el-form-item label="数据标签" prop="dataLabel">
            <el-input v-model="dataForm.dataLabel" placeholder="请输入数据标签" />
          </el-form-item>
          <el-form-item label="排序" prop="sortOrder">
            <el-input-number v-model="dataForm.sortOrder" :min="0" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="dataForm.status" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="dataForm.remark" type="textarea" placeholder="请输入备注" />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dataItemDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="saveDictData">确定</el-button>
          </span>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useDictStore } from '@/stores/dict'
import type { SysDictType, SysDictData } from '@/types/dict'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const dictStore = useDictStore()

// 字典类型相关
const typeDialogVisible = ref(false)
const typeDialogTitle = ref('新建字典类型')
const typeFormRef = ref()
const typeForm = ref<Partial<SysDictType>>({
  typeCode: '',
  typeName: '',
  description: '',
  status: true
})
const typeRules = {
  typeCode: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }]
}

// 字典数据相关
const dataDialogVisible = ref(false)
const currentType = ref<SysDictType | null>(null)
const currentTypeData = ref<SysDictData[]>([])

const dataItemDialogVisible = ref(false)
const dataItemDialogTitle = ref('新建字典数据')
const dataFormRef = ref()
const dataForm = ref<Partial<SysDictData>>({
  typeCode: '',
  dataValue: '',
  dataLabel: '',
  sortOrder: 0,
  status: true,
  remark: ''
})
const dataRules = {
  dataValue: [{ required: true, message: '请输入数据值', trigger: 'blur' }],
  dataLabel: [{ required: true, message: '请输入数据标签', trigger: 'blur' }]
}

// 加载数据
onMounted(() => {
  dictStore.fetchDictTypes()
})

// 显示新建类型对话框
const showAddTypeDialog = () => {
  typeDialogTitle.value = '新建字典类型'
  typeForm.value = {
    typeCode: '',
    typeName: '',
    description: '',
    status: true
  }
  typeDialogVisible.value = true
}

// 显示编辑类型对话框
const showEditTypeDialog = (type: SysDictType) => {
  typeDialogTitle.value = '编辑字典类型'
  typeForm.value = { ...type }
  typeDialogVisible.value = true
}

// 保存字典类型
const saveDictType = async () => {
  if (!typeFormRef.value) return

  await typeFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (typeForm.value.id) {
          await dictStore.editDictType(typeForm.value.id, typeForm.value)
          ElMessage.success('更新成功')
        } else {
          await dictStore.addDictType(typeForm.value)
          ElMessage.success('创建成功')
        }
        typeDialogVisible.value = false
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }
  })
}

// 删除字典类型
const handleDeleteType = (type: SysDictType) => {
  ElMessageBox.confirm(`确定删除字典类型 "${type.typeName}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const success = await dictStore.removeDictType(type.id)
    if (success) {
      ElMessage.success('删除成功')
    } else {
      ElMessage.error('删除失败')
    }
  })
}

// 显示数据管理
const showDataManagement = async (type: SysDictType) => {
  currentType.value = type
  await dictStore.fetchAllDictDataByType(type.typeCode)
  currentTypeData.value = dictStore.dictDataMap.get(type.typeCode) || []
  dataDialogVisible.value = true
}

// 显示新建数据对话框
const showAddDataDialog = () => {
  dataItemDialogTitle.value = '新建字典数据'
  dataForm.value = {
    typeCode: currentType.value?.typeCode || '',
    dataValue: '',
    dataLabel: '',
    sortOrder: 0,
    status: true,
    remark: ''
  }
  dataItemDialogVisible.value = true
}

// 显示编辑数据对话框
const showEditDataDialog = (data: SysDictData) => {
  dataItemDialogTitle.value = '编辑字典数据'
  dataForm.value = { ...data }
  dataItemDialogVisible.value = true
}

// 保存字典数据
const saveDictData = async () => {
  if (!dataFormRef.value) return

  await dataFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (dataForm.value.id) {
          await dictStore.editDictData(dataForm.value.id, dataForm.value)
          ElMessage.success('更新成功')
        } else {
          await dictStore.addDictData(dataForm.value)
          ElMessage.success('创建成功')
        }
        // 刷新数据列表
        if (currentType.value) {
          await dictStore.fetchAllDictDataByType(currentType.value.typeCode)
          currentTypeData.value = dictStore.dictDataMap.get(currentType.value.typeCode) || []
        }
        dataItemDialogVisible.value = false
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }
  })
}

// 删除字典数据
const handleDeleteData = (data: SysDictData) => {
  ElMessageBox.confirm(`确定删除字典数据 "${data.dataLabel}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const success = await dictStore.removeDictData(data.id, data.typeCode)
    if (success) {
      // 刷新数据列表
      if (currentType.value) {
        currentTypeData.value = dictStore.dictDataMap.get(currentType.value.typeCode) || []
      }
      ElMessage.success('删除成功')
    } else {
      ElMessage.error('删除失败')
    }
  })
}
</script>

<style scoped>
.dict-management {
  min-height: 100vh;
  padding: 20px;
  background-color: #f5f7fa;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-header .el-button {
  align-self: flex-start;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.page-description {
  font-size: 14px;
  color: #606266;
  margin: 0;
}

.content-card {
  margin-bottom: 20px;
  transition: all 0.3s;
}

.content-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
