import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { Environment, EnvironmentType } from '@/types/environment'
import {
  getEnvironments,
  getActiveEnvironments,
  getEnvironment,
  createEnvironment,
  updateEnvironment,
  deleteEnvironment,
  searchEnvironments
} from '@/api/environment'
import type { PageResponse } from '@/types/search'

export const useEnvironmentStore = defineStore('environment', () => {
  const environments = ref<Environment[]>([])
  const currentEnvironment = ref<Environment | null>(null)
  const pagination = ref<PageResponse<Environment> | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  const activeEnvironments = computed(() =>
    environments.value.filter(e => e.active)
  )

  const environmentsByType = computed(() => {
    const map = new Map<EnvironmentType, Environment[]>()
    environments.value.forEach(e => {
      const list = map.get(e.type) || []
      list.push(e)
      map.set(e.type, list)
    })
    return map
  })

  const fetchEnvironments = async (params?: {
    page?: number
    size?: number
    sortBy?: string
    sortDir?: string
  }) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getEnvironments(params || {})
      pagination.value = response.data
      environments.value = response.data.content
    } catch (e: any) {
      error.value = e.message || '获取环境列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchActiveEnvironments = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getActiveEnvironments()
      environments.value = response.data
    } catch (e: any) {
      error.value = e.message || '获取环境列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchEnvironment = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getEnvironment(id)
      currentEnvironment.value = response.data
      return response.data
    } catch (e: any) {
      error.value = e.message || '获取环境详情失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const searchEnvironmentList = async (keyword: string) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await searchEnvironments(keyword)
      environments.value = response.data
    } catch (e: any) {
      error.value = e.message || '搜索环境失败'
    } finally {
      isLoading.value = false
    }
  }

  const addEnvironment = async (data: Partial<Environment>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await createEnvironment(data as any)
      environments.value.unshift(response.data)
      return response.data
    } catch (e: any) {
      error.value = e.message || '创建环境失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const editEnvironment = async (id: number, data: Partial<Environment>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await updateEnvironment(id, data as any)
      const index = environments.value.findIndex(e => e.id === id)
      if (index !== -1) {
        environments.value[index] = response.data
      }
      if (currentEnvironment.value?.id === id) {
        currentEnvironment.value = response.data
      }
      return response.data
    } catch (e: any) {
      error.value = e.message || '更新环境失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const removeEnvironment = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      await deleteEnvironment(id)
      environments.value = environments.value.filter(e => e.id !== id)
      if (currentEnvironment.value?.id === id) {
        currentEnvironment.value = null
      }
      return true
    } catch (e: any) {
      error.value = e.message || '删除环境失败'
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    environments,
    currentEnvironment,
    pagination,
    isLoading,
    error,
    activeEnvironments,
    environmentsByType,
    fetchEnvironments,
    fetchActiveEnvironments,
    fetchEnvironment,
    searchEnvironmentList,
    addEnvironment,
    editEnvironment,
    removeEnvironment
  }
})
