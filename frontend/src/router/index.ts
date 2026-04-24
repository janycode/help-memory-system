import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      name: 'layout',
      component: () => import('@/layout/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: '首页', requiresAuth: true }
        },
        {
          path: 'environments/',
          name: 'environments',
          component: () => import('@/views/EnvironmentList.vue'),
          meta: { title: '环境管理', requiresAuth: true }
        },
        {
          path: 'environments/:id',
          name: 'environment-detail',
          component: () => import('@/views/EnvironmentList.vue'),
          meta: { title: '环境详情', requiresAuth: true }
        },
        {
          path: 'components/',
          name: 'components',
          component: () => import('@/views/ComponentList.vue'),
          meta: { title: '技术组件', requiresAuth: true }
        },
        {
          path: 'components/:id',
          name: 'component-detail',
          component: () => import('@/views/ComponentList.vue'),
          meta: { title: '组件详情', requiresAuth: true }
        },
        {
          path: 'processes/',
          name: 'processes',
          component: () => import('@/views/ProcessList.vue'),
          meta: { title: '业务流程', requiresAuth: true }
        },
        {
          path: 'processes/:id',
          name: 'process-detail',
          component: () => import('@/views/ProcessList.vue'),
          meta: { title: '流程详情', requiresAuth: true }
        },
        {
          path: 'repositories/',
          name: 'repositories',
          component: () => import('@/views/ProjectList.vue'),
          meta: { title: '代码仓库', requiresAuth: true }
        },
        {
          path: 'repositories/:id',
          name: 'repository-detail',
          component: () => import('@/views/ProjectList.vue'),
          meta: { title: '代码仓库详情', requiresAuth: true }
        },
        {
          path: 'snippets/',
          name: 'snippets',
          component: () => import('@/views/SnippetList.vue'),
          meta: { title: '代码片段', requiresAuth: true }
        },
        {
          path: 'snippets/:id',
          name: 'snippet-detail',
          component: () => import('@/views/SnippetList.vue'),
          meta: { title: '代码片段详情', requiresAuth: true }
        },
        {
          path: 'dict/',
          name: 'dict',
          component: () => import('@/views/DictManagement.vue'),
          meta: { title: '字典管理', requiresAuth: true }
        },
        {
          path: 'users/',
          name: 'users',
          component: () => import('@/views/UserManagement.vue'),
          meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'profile/',
          name: 'profile',
          component: () => import('@/views/Profile.vue'),
          meta: { title: '个人中心', requiresAuth: true }
        },
        {
          path: 'search/',
          name: 'search',
          component: () => import('@/views/Search.vue'),
          meta: { title: '全局搜索', requiresAuth: true }
        },
        {
          path: 'database/',
          name: 'database',
          component: () => import('@/views/Database.vue'),
          meta: { title: '本地数据库', requiresAuth: true }
        },
        {
          path: 'system/',
          name: 'system',
          component: () => import('@/views/SystemConfig.vue'),
          meta: { title: '系统配置', requiresAuth: true, requiresAdmin: true }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFound.vue'),
      meta: { requiresAuth: false }
    }
  ]
})

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const isAuthenticated = userStore.isAuthenticated

  if (to.meta.title) {
    document.title = `${to.meta.title} - 助记单`
  }

  const requiresAuth = to.meta.requiresAuth !== false
  const requiresAdmin = to.meta.requiresAdmin === true

  if (requiresAuth && !isAuthenticated) {
    next('/login')
  } else if (to.path === '/login' && isAuthenticated) {
    next('/')
  } else if (requiresAdmin && !userStore.isAdmin) {
    next('/')
  } else {
    next()
  }
})

export default router
