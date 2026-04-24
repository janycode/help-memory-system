import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { Repository } from '@/types/project'
import {
  getRepositories,
  getActiveRepositories,
  getRepository,
  createRepository,
  updateRepository,
  deleteRepository,
  searchRepositories
} from '@/api/project'
import type { PageResponse } from '@/types/search'

export const useProjectStore = defineStore('project', () => {
  const repositories = ref<Repository[]>([])
  const currentRepository = ref<Repository | null>(null)
  const pagination = ref<PageResponse<Repository> | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  const activeRepositories = computed(() =>
    repositories.value.filter(p => p.active)
  )

  const fetchProjects = async (params?: {
    page?: number
    size?: number
    sortBy?: string
    sortDir?: string
  }) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getRepositories(params || {})
      pagination.value = response.data
      repositories.value = response.data.content
    } catch (e: any) {
      error.value = e.message || '获取代码仓库列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchActiveProjects = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getActiveRepositories()
      repositories.value = response.data
    } catch (e: any) {
      error.value = e.message || '获取代码仓库列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchProject = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getRepository(id)
      currentRepository.value = response.data
      return response.data
    } catch (e: any) {
      error.value = e.message || '获取代码仓库详情失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const searchProjectList = async (keyword: string) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await searchRepositories(keyword)
      repositories.value = response.data
    } catch (e: any) {
      error.value = e.message || '搜索代码仓库失败'
    } finally {
      isLoading.value = false
    }
  }

  const addProject = async (data: Partial<Repository>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await createRepository(data as any)
      repositories.value.unshift(response.data)
      return response.data
    } catch (e: any) {
      error.value = e.message || '创建代码仓库失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const editProject = async (id: number, data: Partial<Repository>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await updateRepository(id, data as any)
      const index = repositories.value.findIndex(p => p.id === id)
      if (index !== -1) {
        repositories.value[index] = response.data
      }
      if (currentRepository.value?.id === id) {
        currentRepository.value = response.data
      }
      return response.data
    } catch (e: any) {
      error.value = e.message || '更新代码仓库失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const removeProject = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      await deleteRepository(id)
      repositories.value = repositories.value.filter(p => p.id !== id)
      if (currentRepository.value?.id === id) {
        currentRepository.value = null
      }
      return true
    } catch (e: any) {
      error.value = e.message || '删除代码仓库失败'
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    repositories,
    currentRepository,
    pagination,
    isLoading,
    error,
    activeRepositories,
    fetchProjects,
    fetchActiveProjects,
    fetchProject,
    searchProjectList,
    addProject,
    editProject,
    removeProject
  }
})
