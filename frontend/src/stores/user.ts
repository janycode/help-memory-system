import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '@/types/user'
import request from '@/api/request'

export const useUserStore = defineStore('user', () => {
  const currentUser = ref<User | null>(null)
  const token = ref<string | null>(null)
  const isLoading = ref(false)

  const savedToken = localStorage.getItem('token')
  const savedUser = localStorage.getItem('user')

  if (savedToken && savedUser) {
    token.value = savedToken
    currentUser.value = JSON.parse(savedUser)
  }

  const isAuthenticated = computed(() => !!token.value && !!currentUser.value)
  
  const isAdmin = computed(() => {
    if (!currentUser.value || !currentUser.value.roles) return false
    return currentUser.value.roles.includes('ADMIN')
  })

  const login = async (username: string, password: string) => {
    isLoading.value = true
    try {
      const response = await request.post('/auth/login', { username, password })
      const { token: authToken, user } = response.data

      token.value = authToken
      currentUser.value = user

      localStorage.setItem('token', authToken)
      localStorage.setItem('user', JSON.stringify(user))

      return response
    } finally {
      isLoading.value = false
    }
  }

  const logout = async () => {
    // 如果有token，才调用logout接口
    if (token.value) {
      try {
        await request.post('/auth/logout')
      } catch (error) {
        console.error('Logout error:', error)
      }
    }
    currentUser.value = null
    token.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const updateUser = (user: User) => {
    currentUser.value = user
    localStorage.setItem('user', JSON.stringify(user))
  }

  const fetchCurrentUser = async () => {
    if (!token.value) return
    try {
      const response = await request.get('/auth/me')
      currentUser.value = response.data
      localStorage.setItem('user', JSON.stringify(response.data))
    } catch (error) {
      console.error('Fetch user error:', error)
    }
  }

  return {
    currentUser,
    token,
    isLoading,
    isAuthenticated,
    isAdmin,
    login,
    logout,
    updateUser,
    fetchCurrentUser
  }
})
