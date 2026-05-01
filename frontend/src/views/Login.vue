<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>{{ systemTitle }}</h1>
        <p>登录到您的账户</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <p>首次使用？请联系管理员创建账户</p>
      </div>

      <div class="login-bottom-footer">
        <span class="motto">💪 日拱一卒，功不唐捐</span>
        <span class="copyright">© 2026 Help Memory System. All Rights Reserved.</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { systemApi } from '@/api/system'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const systemTitle = ref('助记单系统')

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名至少3个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(loginForm.username, loginForm.password)
        ElMessage.success('登录成功')
        router.push('/')
      } catch (error) {
        ElMessage.error('登录失败，请检查用户名和密码')
      } finally {
        loading.value = false
      }
    }
  })
}

const fetchSystemConfig = async () => {
  try {
    const response = await systemApi.getConfigByKey('system.name')
    if (response.data && response.data.configValue) {
      systemTitle.value = response.data.configValue + '系统'
    } else {
      systemTitle.value = '新人筑基丹系统'
    }
  } catch (error) {
    systemTitle.value = '新人筑基丹系统'
  }
}

const handlePageFocus = () => {
  fetchSystemConfig()
}

onMounted(() => {
  fetchSystemConfig()
  window.addEventListener('focus', handlePageFocus)
})

onUnmounted(() => {
  window.removeEventListener('focus', handlePageFocus)
})
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.login-container::before {
  content: '';
  position: absolute;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.12) 0%, transparent 70%);
  top: -150px;
  right: -100px;
  pointer-events: none;
}

.login-container::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(103, 194, 58, 0.08) 0%, transparent 70%);
  bottom: -120px;
  left: -80px;
  pointer-events: none;
}

.login-box {
  width: 400px;
  padding: 40px 36px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  font-size: 26px;
  color: #1d2129;
  margin: 0 0 8px 0;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.login-header p {
  color: #86909c;
  margin: 0;
  font-size: 14px;
}

.login-form {
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px !important;
  font-weight: 600;
  letter-spacing: 2px;
}

/* 输入框圆角 */
.login-form :deep(.el-input__wrapper) {
  border-radius: 8px !important;
}

.login-footer {
  text-align: center;
  color: #86909c;
  font-size: 13px;
}

.login-footer p {
  margin: 0;
}

.login-bottom-footer {
  text-align: center;
  padding-top: 18px;
  border-top: 1px solid #f0f0f0;
  margin-top: 12px;
  font-size: 12px;
  color: #c0c4cc;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.login-bottom-footer .motto {
  font-weight: 500;
  color: #909399;
}

.login-bottom-footer .copyright {
  opacity: 0.75;
}

@media (max-width: 480px) {
  .login-box {
    width: 92%;
    margin: 20px;
    padding: 30px 24px;
  }

  .login-header h1 {
    font-size: 24px;
  }
}
</style>
