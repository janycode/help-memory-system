import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { BusinessProcess, ProcessCategory } from '@/types/process'
import {
  getProcesses,
  getActiveProcesses,
  getProcess,
  createProcess,
  updateProcess,
  deleteProcess,
  searchProcesses
} from '@/api/process'
import type { PageResponse } from '@/types/search'

export const useProcessStore = defineStore('process', () => {
  const processes = ref<BusinessProcess[]>([])
  const currentProcess = ref<BusinessProcess | null>(null)
  const pagination = ref<PageResponse<BusinessProcess> | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  const activeProcesses = computed(() =>
    processes.value.filter(p => p.active)
  )

  const processesByCategory = computed(() => {
    const map = new Map<ProcessCategory, BusinessProcess[]>()
    processes.value.forEach(p => {
      const list = map.get(p.category) || []
      list.push(p)
      map.set(p.category, list)
    })
    return map
  })

  const sortedByPriority = computed(() =>
    [...processes.value].sort((a, b) => (b.priority || 0) - (a.priority || 0))
  )

  const fetchProcesses = async (params?: {
    page?: number
    size?: number
    sortBy?: string
    sortDir?: string
  }) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getProcesses(params || {})
      pagination.value = response.data
      processes.value = response.data.content
    } catch (e: any) {
      error.value = e.message || '获取流程列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchActiveProcesses = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getActiveProcesses()
      processes.value = response.data
    } catch (e: any) {
      error.value = e.message || '获取流程列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchProcess = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getProcess(id)
      currentProcess.value = response.data
      return response.data
    } catch (e: any) {
      error.value = e.message || '获取流程详情失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const searchProcessList = async (keyword: string) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await searchProcesses(keyword)
      processes.value = response.data
    } catch (e: any) {
      error.value = e.message || '搜索流程失败'
    } finally {
      isLoading.value = false
    }
  }

  const addProcess = async (data: Partial<BusinessProcess>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await createProcess(data as any)
      processes.value.unshift(response.data)
      return response.data
    } catch (e: any) {
      error.value = e.message || '创建流程失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const editProcess = async (id: number, data: Partial<BusinessProcess>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await updateProcess(id, data as any)
      const index = processes.value.findIndex(p => p.id === id)
      if (index !== -1) {
        processes.value[index] = response.data
      }
      if (currentProcess.value?.id === id) {
        currentProcess.value = response.data
      }
      return response.data
    } catch (e: any) {
      error.value = e.message || '更新流程失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const removeProcess = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      await deleteProcess(id)
      processes.value = processes.value.filter(p => p.id !== id)
      if (currentProcess.value?.id === id) {
        currentProcess.value = null
      }
      return true
    } catch (e: any) {
      error.value = e.message || '删除流程失败'
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    processes,
    currentProcess,
    pagination,
    isLoading,
    error,
    activeProcesses,
    processesByCategory,
    sortedByPriority,
    fetchProcesses,
    fetchActiveProcesses,
    fetchProcess,
    searchProcessList,
    addProcess,
    editProcess,
    removeProcess
  }
})
