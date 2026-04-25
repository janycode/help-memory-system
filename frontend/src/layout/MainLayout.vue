<template>
  <el-container class="main-layout">
    <el-aside width="240px" class="sidebar">
      <div class="logo">
        <h2>{{ systemTitle }}</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="menu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-divider class="nav-divider" />

        <el-menu-item index="/environments">
          <el-icon><Monitor /></el-icon>
          <span>环境管理</span>
        </el-menu-item>

        <el-menu-item index="/components">
          <el-icon><Cpu /></el-icon>
          <span>技术组件</span>
        </el-menu-item>

        <el-menu-item index="/processes">
          <el-icon><Document /></el-icon>
          <span>业务流程</span>
        </el-menu-item>

        <el-menu-item index="/repositories">
          <el-icon><Folder /></el-icon>
          <span>代码仓库</span>
        </el-menu-item>

        <el-menu-item index="/snippets">
          <el-icon><DocumentCopy /></el-icon>
          <span>代码片段</span>
        </el-menu-item>
        <el-divider class="nav-divider" />

        <el-menu-item index="/dict">
          <el-icon><Setting /></el-icon>
          <span>字典管理</span>
        </el-menu-item>

        <el-menu-item v-if="userStore.isAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>

        <el-menu-item v-if="userStore.isAdmin" index="/system">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
        <el-divider class="nav-divider" />

        <el-menu-item index="/search">
          <el-icon><Search /></el-icon>
          <span>全局搜索</span>
        </el-menu-item>

        <el-menu-item index="/database">
          <el-icon><DataBoard /></el-icon>
          <span>本地数据库</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb>
            <el-breadcrumb-item
              v-for="item in breadcrumbs"
              :key="item.path"
              :to="item.path"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tooltip :content="themeStore.isDark ? '切换亮色模式' : '切换暗色模式'">
            <el-button class="theme-toggle" @click="themeStore.toggle" text>
              <el-icon :size="18"><Moon v-if="!themeStore.isDark" /><Sunny v-else /></el-icon>
            </el-button>
          </el-tooltip>
          <el-dropdown>
            <span class="user-info">
              <el-avatar size="small" class="avatar">
                {{ userInitials }}
              </el-avatar>
              <span class="username">{{ userStore.currentUser?.fullName || userStore.currentUser?.username }}</span>
              <el-icon><CaretBottom /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="content">
        <router-view />
      </el-main>

      <!-- 底部 -->
      <el-footer class="footer">
        <span class="motto">🎯 代码千行，注释一行</span>
        <span class="copyright">© 2026 Help Memory System. All Rights Reserved.</span>
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { ElMessageBox } from 'element-plus'
import { systemApi } from '@/api/system'
import { useGlobalShortcuts } from '@/composables/useKeyboardShortcuts'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const systemTitle = ref('助记单')

const activeMenu = computed(() => {
  const { path } = route
  if (path.startsWith('/environments')) return '/environments'
  if (path.startsWith('/components')) return '/components'
  if (path.startsWith('/processes')) return '/processes'
  if (path.startsWith('/repositories')) return '/repositories'
  if (path.startsWith('/snippets')) return '/snippets'
  if (path.startsWith('/dict')) return '/dict'
  if (path.startsWith('/users')) return '/users'
  if (path.startsWith('/system')) return '/system'
  if (path.startsWith('/profile')) return '/profile'
  if (path.startsWith('/search')) return '/search'
  if (path.startsWith('/database')) return '/database'
  return '/'
})

const userInitials = computed(() => {
  const user = userStore.currentUser
  if (!user) return 'U'

  const name = user.fullName || user.username || ''
  return name ? name.charAt(0).toUpperCase() : 'U'
})

const breadcrumbs = computed(() => {
  const matched = route.matched.filter((item) => item.meta && item.meta.title)
  return matched.map((item) => ({
    path: item.path,
    title: item.meta.title as string
  }))
})

const handleLogout = async () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await userStore.logout()
    router.push('/login')
  })
}

const fetchSystemTitle = async () => {
  try {
    const response = await systemApi.getConfigByKey('system.name')
    if (response.data && response.data.configValue) {
      systemTitle.value = response.data.configValue
    } else {
      // 默认值
      systemTitle.value = '新人筑基丹'
    }
  } catch (error) {
    console.error('获取系统标题失败:', error)
    // 出错时使用默认值
    systemTitle.value = '新人筑基丹'
  }
}

// 全局快捷键
useGlobalShortcuts()

// 页面获得焦点时刷新配置
const handlePageFocus = () => {
  fetchSystemTitle()
}

onMounted(() => {
  fetchSystemTitle()
  
  // 监听页面获得焦点事件
  window.addEventListener('focus', handlePageFocus)
})

onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('focus', handlePageFocus)
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  display: flex;
  flex-direction: column;
}

html.dark .sidebar {
  background-color: #1d1e1f;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #1f2d3d;
}

html.dark .logo {
  border-bottom-color: #333;
}

.logo h2 {
  color: #fff;
  font-size: 18px;
  margin: 0;
}

.menu {
  flex: 1;
  border-right: none;
}

.nav-divider {
  margin: 8px 0;
  background-color: rgba(255, 255, 255, 0.1);
}

.main-container {
  display: flex;
  flex-direction: column;
}

.header {
  height: 60px;
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

html.dark .header {
  background-color: #1d1e1f;
  border-bottom-color: #333;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.theme-toggle {
  border: none;
  font-size: 18px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

html.dark .user-info:hover {
  background-color: #2c2c2e;
}

.avatar {
  margin-right: 8px;
  background-color: #409eff;
}

.username {
  margin-right: 4px;
  font-size: 14px;
  color: #606266;
}

html.dark .username {
  color: #a0a0a0;
}

.content {
  flex: 1;
  background-color: #f5f7fa;
  padding: 0;
  overflow: auto;
}

html.dark .content {
  background-color: #141414;
}

.footer {
  height: 40px;
  background-color: #fff;
  border-top: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  font-size: 12px;
  color: #909399;
}

html.dark .footer {
  background-color: #1d1e1f;
  border-top-color: #333;
  color: #666;
}

.motto {
  font-weight: 500;
}

.copyright {
  opacity: 0.8;
}
</style>
