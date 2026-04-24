import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { TechnicalComponent, ComponentCategory } from '@/types/component'
import {
  getComponents,
  getActiveComponents,
  getComponent,
  createComponent,
  updateComponent,
  deleteComponent,
  searchComponents
} from '@/api/component'
import type { PageResponse } from '@/types/search'

export const useComponentStore = defineStore('component', () => {
  const components = ref<TechnicalComponent[]>([])
  const currentComponent = ref<TechnicalComponent | null>(null)
  const pagination = ref<PageResponse<TechnicalComponent> | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  const activeComponents = computed(() =>
    components.value.filter(c => c.active)
  )

  const componentsByCategory = computed(() => {
    const map = new Map<ComponentCategory, TechnicalComponent[]>()
    components.value.forEach(c => {
      const list = map.get(c.category) || []
      list.push(c)
      map.set(c.category, list)
    })
    return map
  })

  const fetchComponents = async (params?: {
    page?: number
    size?: number
    sortBy?: string
    sortDir?: string
  }) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getComponents(params || {})
      pagination.value = response.data
      components.value = response.data.content
    } catch (e: any) {
      error.value = e.message || '获取组件列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchActiveComponents = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getActiveComponents()
      components.value = response.data
    } catch (e: any) {
      error.value = e.message || '获取组件列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchComponent = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getComponent(id)
      currentComponent.value = response.data
      return response.data
    } catch (e: any) {
      error.value = e.message || '获取组件详情失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const searchComponentList = async (keyword: string) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await searchComponents(keyword)
      components.value = response.data
    } catch (e: any) {
      error.value = e.message || '搜索组件失败'
    } finally {
      isLoading.value = false
    }
  }

  const addComponent = async (data: Partial<TechnicalComponent>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await createComponent(data as any)
      components.value.unshift(response.data)
      return response.data
    } catch (e: any) {
      error.value = e.message || '创建组件失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const editComponent = async (id: number, data: Partial<TechnicalComponent>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await updateComponent(id, data as any)
      const index = components.value.findIndex(c => c.id === id)
      if (index !== -1) {
        components.value[index] = response.data
      }
      if (currentComponent.value?.id === id) {
        currentComponent.value = response.data
      }
      return response.data
    } catch (e: any) {
      error.value = e.message || '更新组件失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const removeComponent = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      await deleteComponent(id)
      components.value = components.value.filter(c => c.id !== id)
      if (currentComponent.value?.id === id) {
        currentComponent.value = null
      }
      return true
    } catch (e: any) {
      error.value = e.message || '删除组件失败'
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    components,
    currentComponent,
    pagination,
    isLoading,
    error,
    activeComponents,
    componentsByCategory,
    fetchComponents,
    fetchActiveComponents,
    fetchComponent,
    searchComponentList,
    addComponent,
    editComponent,
    removeComponent
  }
})
