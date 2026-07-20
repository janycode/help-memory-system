<template>
  <div class="menu-permission-management">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">菜单权限管理</h1>
        <p class="page-description">配置不同用户可访问的菜单，管理员可访问所有菜单</p>
      </div>

      <el-card class="content-card">
        <div class="toolbar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户名或姓名..."
            clearable
            style="width: 240px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <div class="toolbar-right">
            <el-tag type="info" size="small">共 {{ filteredUsers.length }} 个用户</el-tag>
          </div>
        </div>

        <el-table :data="filteredUsers" v-loading="loading" stripe>
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="fullName" label="姓名" width="120" />
          <el-table-column label="菜单权限" min-width="500">
            <template #default="{ row }">
              <el-checkbox-group v-model="row.allowedMenus" :disabled="row.isAdmin">
                <el-checkbox
                  v-for="menu in allMenus"
                  :key="menu.code"
                  :label="menu.code"
                >
                  {{ menu.name }}
                </el-checkbox>
              </el-checkbox-group>
              <el-tag v-if="row.isAdmin" type="success" size="small" style="margin-left: 8px">
                管理员（全部权限）
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                @click="savePermissions(row)"
                :loading="row.saving"
                :disabled="row.isAdmin"
              >
                保存
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { userMenuPermissionApi, type UserPermissionDTO } from '@/api/userMenuPermission'

const loading = ref(false)
const searchKeyword = ref('')
const usersWithPermissions = ref<(UserPermissionDTO & { saving?: boolean; isAdmin?: boolean })[]>([])

const allMenus = [
  { code: 'home', name: '首页' },
  { code: 'iterations', name: '迭代管理' },
  { code: 'search', name: '全局搜索' },
  { code: 'database', name: '本地数据库' },
  { code: 'environments', name: '环境管理' },
  { code: 'components', name: '技术组件' },
  { code: 'processes', name: '业务流程' },
  { code: 'repositories', name: '代码仓库' },
  { code: 'snippets', name: '代码片段' },
  { code: 'batch-so', name: 'SO批量新建' },
  { code: 'mq-send', name: 'MQ自动称重' },
  { code: 'dict', name: '字典管理' },
  { code: 'users', name: '用户管理' },
  { code: 'menu-permissions', name: '菜单权限' },
  { code: 'system', name: '系统配置' }
]

const filteredUsers = computed(() => {
  if (!searchKeyword.value) return usersWithPermissions.value
  const kw = searchKeyword.value.toLowerCase()
  return usersWithPermissions.value.filter(
    u => u.username.toLowerCase().includes(kw) || u.fullName?.toLowerCase().includes(kw)
  )
})

const fetchPermissions = async () => {
  loading.value = true
  try {
    const response = await userMenuPermissionApi.getAllUsersPermissions()
    usersWithPermissions.value = response.data.map(item => ({
      ...item,
      saving: false,
      isAdmin: item.username === 'admin'
    }))
  } catch (error) {
    console.error('获取用户权限失败:', error)
    ElMessage.error('获取用户权限失败')
  } finally {
    loading.value = false
  }
}

const savePermissions = async (row: UserPermissionDTO & { saving?: boolean; isAdmin?: boolean }) => {
  row.saving = true
  try {
    await userMenuPermissionApi.saveMenus(row.userId, row.allowedMenus)
    ElMessage.success(`已保存 ${row.username} 的菜单权限`)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    row.saving = false
  }
}

onMounted(() => {
  fetchPermissions()
})
</script>

<style scoped>
.menu-permission-management {
  min-height: 100vh;
  padding: 20px;
  background-color: #f5f7fa;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.page-description {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.content-card {
  transition: all 0.3s;
}

.content-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-checkbox-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

:deep(.el-checkbox) {
  margin-right: 0;
}
</style>
