<template>
  <div class="user-management">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">用户管理</h1>
        <p class="page-description">管理系统用户账号</p>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
      </div>

      <el-card class="content-card">
        <el-table :data="users" v-loading="loading" stripe>
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="fullName" label="姓名" width="120" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column prop="department" label="部门" width="120" />
          <el-table-column prop="position" label="职位" width="120" />
          <el-table-column prop="roles" label="角色" width="120">
            <template #default="{ row }">
              <el-tag v-for="role in row.roles" :key="role" :type="role === 'ADMIN' ? 'danger' : 'primary'" size="small" style="margin-right: 4px">
                {{ role === 'ADMIN' ? '管理员' : '用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginAt" label="最后登录" width="180">
            <template #default="{ row }">
              {{ row.lastLoginAt ? formatDate(row.lastLoginAt) : '从未登录' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
              <el-button :type="row.enabled ? 'danger' : 'success'" link @click="handleToggleStatus(row)">
                {{ row.enabled ? '禁用' : '启用' }}
              </el-button>
              <el-popconfirm title="确定删除该用户吗？" @confirm="handleDelete(row.id)">
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
            @size-change="fetchUsers"
            @current-change="fetchUsers"
          />
        </div>
      </el-card>

      <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px" destroy-on-close>
        <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px">
          <el-form-item label="用户名" prop="username" v-if="!isEdit">
            <el-input v-model="formData.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="用户名" v-else>
            <el-input :value="formData.username" disabled />
          </el-form-item>
          <el-form-item label="密码" prop="password" v-if="!isEdit">
            <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-form-item label="姓名" prop="fullName">
            <el-input v-model="formData.fullName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="部门" prop="department">
            <el-input v-model="formData.department" placeholder="请输入部门" />
          </el-form-item>
          <el-form-item label="职位" prop="position">
            <el-input v-model="formData.position" placeholder="请输入职位" />
          </el-form-item>
          <el-form-item label="角色" prop="roles">
            <el-checkbox-group v-model="formData.roles">
              <el-checkbox label="ADMIN">管理员</el-checkbox>
              <el-checkbox label="USER">普通用户</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="resetPasswordVisible" title="重置密码" width="400px">
        <el-form :model="resetPasswordData" :rules="resetPasswordRules" ref="resetPasswordFormRef" label-width="80px">
          <el-form-item label="用户">
            <el-input :value="resetPasswordData.username" disabled />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="resetPasswordData.newPassword" type="password" placeholder="请输入新密码" show-password />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="resetPasswordVisible = false">取消</el-button>
          <el-button type="primary" @click="handleResetPasswordSubmit" :loading="submitting">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
import type { User, UserCreateRequest, UserUpdateRequest } from '@/types/user'

const loading = ref(false)
const users = ref<User[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const resetPasswordVisible = ref(false)
const resetPasswordFormRef = ref()

const formData = reactive({
  id: 0,
  username: '',
  password: '',
  email: '',
  fullName: '',
  department: '',
  position: '',
  roles: [] as string[]
})

const resetPasswordData = reactive({
  id: 0,
  username: '',
  newPassword: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }]
}

const resetPasswordRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const response = await userApi.getUsers({
      page: currentPage.value - 1,
      size: pageSize.value
    })
    users.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formData, {
    id: 0,
    username: '',
    password: '',
    email: '',
    fullName: '',
    department: '',
    position: '',
    roles: ['USER']
  })
  dialogVisible.value = true
}

const handleEdit = (row: User) => {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    username: row.username,
    email: row.email,
    fullName: row.fullName,
    department: row.department,
    position: row.position,
    roles: row.roles || ['USER']
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true

  try {
    if (isEdit.value) {
      const data: UserUpdateRequest = {
        email: formData.email,
        fullName: formData.fullName,
        department: formData.department,
        position: formData.position,
        roles: formData.roles
      }
      await userApi.updateUser(formData.id, data)
      ElMessage.success('更新成功')
    } else {
      const data: UserCreateRequest = {
        username: formData.username,
        password: formData.password,
        email: formData.email,
        fullName: formData.fullName,
        department: formData.department,
        position: formData.position,
        roles: formData.roles
      }
      await userApi.createUser(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchUsers()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await userApi.deleteUser(id)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

const handleToggleStatus = async (row: User) => {
  try {
    await userApi.toggleUserStatus(row.id)
    ElMessage.success(row.enabled ? '已禁用' : '已启用')
    fetchUsers()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

const handleResetPassword = (row: User) => {
  resetPasswordData.id = row.id
  resetPasswordData.username = row.username
  resetPasswordData.newPassword = ''
  resetPasswordVisible.value = true
}

const handleResetPasswordSubmit = async () => {
  await resetPasswordFormRef.value.validate()
  submitting.value = true

  try {
    await userApi.resetPassword(resetPasswordData.id, resetPasswordData.newPassword)
    ElMessage.success('密码重置成功')
    resetPasswordVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '重置失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-management {
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

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
