<template>
  <el-container class="main-layout">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="logo">
        <h2 v-if="!isCollapsed">{{ systemTitle }}</h2>
        <span v-else class="logo-icon">{{ systemTitle.charAt(0) }}</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="menu"
        router
        :collapse="isCollapsed"
      >
        <el-menu-item index="/">
          <el-icon><House /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <el-menu-item index="/iterations">
          <el-icon><List /></el-icon>
          <template #title>迭代管理</template>
        </el-menu-item>

        <el-menu-item index="/search">
          <el-icon><Search /></el-icon>
          <template #title>全局搜索</template>
        </el-menu-item>

        <el-menu-item index="/database">
          <el-icon><Coin /></el-icon>
          <template #title>本地数据库</template>
        </el-menu-item>

        <el-sub-menu index="biz">
          <template #title>
            <el-icon><Grid /></el-icon>
            <span>业务管理</span>
          </template>
          <el-menu-item index="/environments">
            <el-icon><Monitor /></el-icon>
            环境管理
          </el-menu-item>
          <el-menu-item index="/components">
            <el-icon><Cpu /></el-icon>
            技术组件
          </el-menu-item>
          <el-menu-item index="/processes">
            <el-icon><Document /></el-icon>
            业务流程
          </el-menu-item>
          <el-menu-item index="/repositories">
            <el-icon><Folder /></el-icon>
            代码仓库
          </el-menu-item>
          <el-menu-item index="/snippets">
            <el-icon><DocumentCopy /></el-icon>
            代码片段
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="sys">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/dict">
            <el-icon><Collection /></el-icon>
            字典管理
          </el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/users">
            <el-icon><User /></el-icon>
            用户管理
          </el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/system">
            <el-icon><Tools /></el-icon>
            系统配置
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <el-button class="collapse-btn" text @click="toggleCollapse">
            <el-icon :size="18"><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
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
            <el-button class="theme-toggle" text @click="themeStore.toggle">
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

      <el-footer class="footer">
        <span class="motto">💪 日拱一卒，功不唐捐</span>
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
const isCollapsed = ref(localStorage.getItem('sidebar-collapsed') === 'true')

const activeMenu = computed(() => {
  const { path } = route
  if (path.startsWith('/environments')) return '/environments'
  if (path.startsWith('/components')) return '/components'
  if (path.startsWith('/processes')) return '/processes'
  if (path.startsWith('/repositories')) return '/repositories'
  if (path.startsWith('/iterations')) return '/iterations'
  if (path.startsWith('/snippets')) return '/snippets'
  if (path.startsWith('/dict')) return '/dict'
  if (path.startsWith('/users')) return '/users'
  if (path.startsWith('/system')) return '/system'
  if (path.startsWith('/profile')) return '/profile'
  if (path.startsWith('/search')) return '/search'
  if (path.startsWith('/database')) return '/database'
  return '/'
})

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem('sidebar-collapsed', String(isCollapsed.value))
}

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
      systemTitle.value = '新人筑基丹'
    }
  } catch (error) {
    console.error('获取系统标题失败:', error)
    systemTitle.value = '新人筑基丹'
  }
}

useGlobalShortcuts()

const handlePageFocus = () => {
  fetchSystemTitle()
}

onMounted(() => {
  fetchSystemTitle()
  window.addEventListener('focus', handlePageFocus)
})

onUnmounted(() => {
  window.removeEventListener('focus', handlePageFocus)
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

/* ===== 侧边栏 ===== */
.sidebar {
  background-color: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.28s ease;
  overflow: hidden;
  border-right: none;
}

html.dark .sidebar {
  background-color: #0d1117;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo h2 {
  color: #ffffff;
  font-size: 17px;
  margin: 0;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  letter-spacing: 0.5px;
}

.logo-icon {
  color: #ffffff;
  font-size: 20px;
  font-weight: 700;
}

/* ===== 菜单样式 ===== */
.menu {
  flex: 1;
  border-right: none !important;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
}

.menu::-webkit-scrollbar {
  width: 4px;
}

.menu::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.12);
  border-radius: 2px;
}

/* 覆盖 Element Plus 菜单默认样式 */
:deep(.el-menu) {
  background-color: transparent !important;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  color: var(--sidebar-text) !important;
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 8px;
  transition: all 0.2s ease !important;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: var(--sidebar-hover-bg) !important;
  color: #ffffff !important;
}

:deep(.el-menu-item.is-active) {
  background-color: var(--sidebar-active-bg) !important;
  color: var(--sidebar-active-text) !important;
  font-weight: 600;
}

:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #ffffff !important;
}

:deep(.el-sub-menu .el-menu) {
  background-color: transparent !important;
}

:deep(.el-sub-menu .el-menu-item) {
  height: 40px;
  line-height: 40px;
  min-width: auto !important;
  padding-left: 52px !important;
  margin: 1px 12px 1px 8px;
  border-radius: 6px;
}

:deep(.el-sub-menu .el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.04) !important;
}

:deep(.el-sub-menu .el-menu-item.is-active) {
  background-color: rgba(64, 158, 255, 0.12) !important;
}

:deep(.el-sub-menu__icon-arrow) {
  color: var(--sidebar-text);
}

/* 折叠状态 */
.sidebar.collapsed .logo {
  justify-content: center;
  padding: 0;
}

:deep(.el-menu--collapse) {
  width: 64px;
}

:deep(.el-menu--collapse .el-menu-item),
:deep(.el-menu--collapse .el-sub-menu__title) {
  margin: 2px 8px;
  padding: 0 !important;
  justify-content: center;
}

:deep(.el-menu--collapse .el-sub-menu__icon-arrow) {
  display: none;
}

/* ===== 主容器 ===== */
.main-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== 头部 ===== */
.header {
  height: 56px;
  background-color: var(--header-bg);
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

html.dark .header {
  background-color: #161618;
  border-bottom-color: #2c2c2e;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.collapse-btn {
  border: none;
  padding: 8px;
  color: #606266;
}

.collapse-btn:hover {
  color: #303133;
  background-color: #f5f7fa;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.theme-toggle {
  border: none;
  font-size: 18px;
  padding: 8px;
  color: #606266;
}

.theme-toggle:hover {
  color: #409eff;
  background-color: #f5f7fa;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 6px;
  transition: background-color 0.25s;
  gap: 6px;
}

.user-info:hover {
  background-color: #f5f7fa;
}

html.dark .user-info:hover {
  background-color: #2c2c2e;
}

.avatar {
  background: linear-gradient(135deg, #409eff, #53a8ff) !important;
  font-size: 13px;
  flex-shrink: 0;
}

.username {
  font-size: 14px;
  color: #4e5969;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

html.dark .username {
  color: #a0a0a0;
}

/* ===== 内容区 ===== */
.content {
  flex: 1;
  background-color: var(--content-bg);
  padding: 0;
  overflow: auto;
}

html.dark .content {
  background-color: #141414;
}

/* ===== 底部 ===== */
.footer {
  height: 36px;
  background-color: var(--header-bg);
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  font-size: 12px;
  color: #86909c;
  flex-shrink: 0;
}

html.dark .footer {
  background-color: #161618;
  border-top-color: #2c2c2e;
  color: #666;
}

.motto {
  font-weight: 500;
  color: #4e5969;
}

.copyright {
  opacity: 0.75;
}
</style>
