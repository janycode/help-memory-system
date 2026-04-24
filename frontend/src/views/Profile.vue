<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="profile-card">
          <template #header>
            <div class="card-header">
              <span>个人信息</span>
            </div>
          </template>
          <div class="user-avatar">
            <el-avatar :size="80">{{ userInitials }}</el-avatar>
          </div>
          <div class="user-info">
            <div class="info-item">
              <label>用户名：</label>
              <span>{{ userStore.currentUser?.username }}</span>
            </div>
            <div class="info-item">
              <label>姓名：</label>
              <span>{{ userStore.currentUser?.fullName || '-' }}</span>
            </div>
            <div class="info-item">
              <label>邮箱：</label>
              <span>{{ userStore.currentUser?.email }}</span>
            </div>
            <div class="info-item">
              <label>部门：</label>
              <span>{{ userStore.currentUser?.department || '-' }}</span>
            </div>
            <div class="info-item">
              <label>职位：</label>
              <span>{{ userStore.currentUser?.position || '-' }}</span>
            </div>
            <div class="info-item">
              <label>角色：</label>
              <span>
                <el-tag v-for="role in userStore.currentUser?.roles" :key="role" size="small" style="margin-right: 4px">
                  {{ role === 'ADMIN' ? '管理员' : '用户' }}
                </el-tag>
              </span>
            </div>
            <div class="info-item">
              <label>最后登录：</label>
              <span>{{ userStore.currentUser?.lastLoginAt ? formatDate(userStore.currentUser.lastLoginAt) : '从未登录' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card class="settings-card">
          <template #header>
            <div class="card-header">
              <span>修改密码</span>
            </div>
          </template>
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px" style="max-width: 400px">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="submitting">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="activity-card" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>最近活动</span>
            </div>
          </template>
          <el-timeline v-if="activities.length > 0">
            <el-timeline-item
              v-for="activity in activities"
              :key="activity.id"
              :timestamp="formatDate(activity.createdAt)"
              placement="top"
            >
              <el-card>
                <div class="activity-content">
                  <el-tag :type="getOperationTagType(activity.operation)" size="small">{{ activity.operation }}</el-tag>
                  <span class="activity-module">{{ activity.module }}</span>
                  <span class="activity-desc">{{ activity.description }}</span>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无活动记录" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/user'
import { activityApi, type ActivityLog } from '@/api/activity'

const userStore = useUserStore()
const passwordFormRef = ref()
const submitting = ref(false)
const activities = ref<ActivityLog[]>([])

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const userInitials = computed(() => {
  const user = userStore.currentUser
  if (!user) return 'U'
  const name = user.fullName || user.username || ''
  return name ? name.charAt(0).toUpperCase() : 'U'
})

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getOperationTagType = (operation: string) => {
  const map: Record<string, string> = {
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    LOGIN: 'primary',
    LOGOUT: 'info'
  }
  return map[operation] || 'info'
}

const handleChangePassword = async () => {
  await passwordFormRef.value.validate()
  submitting.value = true

  try {
    await authApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordFormRef.value.resetFields()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '修改失败')
  } finally {
    submitting.value = false
  }
}

const fetchActivities = async () => {
  try {
    const response = await activityApi.getMyActivities()
    activities.value = response.data
  } catch (error) {
    console.error('获取活动记录失败:', error)
  }
}

onMounted(() => {
  fetchActivities()
})
</script>

<style scoped>
.profile-page {
  padding: 20px;
}

.profile-card {
  text-align: center;
}

.user-avatar {
  margin-bottom: 20px;
}

.user-info {
  text-align: left;
}

.info-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item label {
  color: #666;
  margin-right: 10px;
}

.card-header {
  font-weight: bold;
}

.activity-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.activity-module {
  color: #409eff;
}

.activity-desc {
  color: #666;
}
</style>
