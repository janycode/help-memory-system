import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { Iteration, IterationCreateRequest, IterationQueryParams } from '@/types/iteration'
import {
  getIterations,
  getActiveIterations,
  getIteration,
  createIteration,
  updateIteration,
  deleteIteration,
  searchIterations,
  getIterationsByStatus,
  getIterationsByPriority
} from '@/api/iteration'
import type { PageResponse } from '@/types/search'

export const useIterationStore = defineStore('iteration', () => {
  const iterations = ref<Iteration[]>([])
  const currentIteration = ref<Iteration | null>(null)
  const pagination = ref<PageResponse<Iteration> | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  const activeIterations = computed(() =>
    iterations.value.filter(i => i.active)
  )

  const fetchIterations = async (params?: IterationQueryParams) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getIterations(params || {})
      pagination.value = response.data
      iterations.value = response.data.content
    } catch (e: any) {
      error.value = e.message || '获取迭代列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchActiveIterations = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getActiveIterations()
      iterations.value = response.data
    } catch (e: any) {
      error.value = e.message || '获取迭代列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchIteration = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getIteration(id)
      currentIteration.value = response.data
      return response.data
    } catch (e: any) {
      error.value = e.message || '获取迭代详情失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const searchIterationList = async (keyword: string) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await searchIterations(keyword)
      iterations.value = response.data
    } catch (e: any) {
      error.value = e.message || '搜索迭代失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchIterationsByStatus = async (status: string) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getIterationsByStatus(status)
      iterations.value = response.data
    } catch (e: any) {
      error.value = e.message || '获取迭代列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchIterationsByPriority = async (priority: string) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getIterationsByPriority(priority)
      iterations.value = response.data
    } catch (e: any) {
      error.value = e.message || '获取迭代列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const addIteration = async (data: IterationCreateRequest) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await createIteration(data)
      iterations.value.unshift(response.data)
      return response.data
    } catch (e: any) {
      error.value = e.message || '创建迭代失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const editIteration = async (id: number, data: Partial<Iteration>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await updateIteration(id, data as any)
      const index = iterations.value.findIndex(i => i.id === id)
      if (index !== -1) {
        iterations.value[index] = response.data
      }
      if (currentIteration.value?.id === id) {
        currentIteration.value = response.data
      }
      return response.data
    } catch (e: any) {
      error.value = e.message || '更新迭代失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const removeIteration = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      await deleteIteration(id)
      iterations.value = iterations.value.filter(i => i.id !== id)
      if (currentIteration.value?.id === id) {
        currentIteration.value = null
      }
      return true
    } catch (e: any) {
      error.value = e.message || '删除迭代失败'
      return false
    } finally {
      isLoading.value = false
    }
  }

  const resetError = () => {
    error.value = null
  }

  return {
    iterations,
    currentIteration,
    pagination,
    isLoading,
    error,
    activeIterations,
    fetchIterations,
    fetchActiveIterations,
    fetchIteration,
    searchIterationList,
    fetchIterationsByStatus,
    fetchIterationsByPriority,
    addIteration,
    editIteration,
    removeIteration,
    resetError
  }
})