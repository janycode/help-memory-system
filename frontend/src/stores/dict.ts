import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { SysDictType, SysDictData, DictOption } from '@/types/dict'
import {
  getDictTypes,
  getActiveDictTypes,
  getDictDataByType,
  getAllDictDataByType,
  createDictType,
  updateDictType,
  deleteDictType,
  createDictData,
  updateDictData,
  deleteDictData
} from '@/api/dict'

export const useDictStore = defineStore('dict', () => {
  const dictTypes = ref<SysDictType[]>([])
  const dictDataMap = ref<Map<string, SysDictData[]>>(new Map())
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // 获取字典数据选项（用于下拉框）
  const getDictOptions = computed(() => {
    return (typeCode: string): DictOption[] => {
      const dataList = dictDataMap.value.get(typeCode) || []
      return dataList
        .filter(item => item.status)
        .map(item => ({
          label: item.dataLabel,
          value: item.dataValue
        }))
    }
  })

  const fetchDictTypes = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getDictTypes()
      dictTypes.value = response.data
    } catch (e: any) {
      error.value = e.message || '获取字典类型失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchActiveDictTypes = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getActiveDictTypes()
      dictTypes.value = response.data
    } catch (e: any) {
      error.value = e.message || '获取字典类型失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchDictDataByType = async (typeCode: string) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getDictDataByType(typeCode)
      dictDataMap.value.set(typeCode, response.data)
    } catch (e: any) {
      error.value = e.message || '获取字典数据失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchAllDictDataByType = async (typeCode: string) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getAllDictDataByType(typeCode)
      dictDataMap.value.set(typeCode, response.data)
    } catch (e: any) {
      error.value = e.message || '获取字典数据失败'
    } finally {
      isLoading.value = false
    }
  }

  const addDictType = async (data: Partial<SysDictType>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await createDictType(data)
      dictTypes.value.unshift(response.data)
      return response.data
    } catch (e: any) {
      error.value = e.message || '创建字典类型失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const editDictType = async (id: number, data: Partial<SysDictType>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await updateDictType(id, data)
      const index = dictTypes.value.findIndex(t => t.id === id)
      if (index !== -1) {
        dictTypes.value[index] = response.data
      }
      return response.data
    } catch (e: any) {
      error.value = e.message || '更新字典类型失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const removeDictType = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      await deleteDictType(id)
      dictTypes.value = dictTypes.value.filter(t => t.id !== id)
      return true
    } catch (e: any) {
      error.value = e.message || '删除字典类型失败'
      return false
    } finally {
      isLoading.value = false
    }
  }

  const addDictData = async (data: Partial<SysDictData>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await createDictData(data)
      const typeCode = data.typeCode!
      if (!dictDataMap.value.has(typeCode)) {
        dictDataMap.value.set(typeCode, [])
      }
      dictDataMap.value.get(typeCode)!.push(response.data)
      return response.data
    } catch (e: any) {
      error.value = e.message || '创建字典数据失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const editDictData = async (id: number, data: Partial<SysDictData>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await updateDictData(id, data)
      const typeCode = data.typeCode!
      const dataList = dictDataMap.value.get(typeCode) || []
      const index = dataList.findIndex(d => d.id === id)
      if (index !== -1) {
        dataList[index] = response.data
      }
      return response.data
    } catch (e: any) {
      error.value = e.message || '更新字典数据失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const removeDictData = async (id: number, typeCode: string) => {
    isLoading.value = true
    error.value = null
    try {
      await deleteDictData(id)
      const dataList = dictDataMap.value.get(typeCode) || []
      dictDataMap.value.set(typeCode, dataList.filter(d => d.id !== id))
      return true
    } catch (e: any) {
      error.value = e.message || '删除字典数据失败'
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    dictTypes,
    dictDataMap,
    isLoading,
    error,
    getDictOptions,
    fetchDictTypes,
    fetchActiveDictTypes,
    fetchDictDataByType,
    fetchAllDictDataByType,
    addDictType,
    editDictType,
    removeDictType,
    addDictData,
    editDictData,
    removeDictData
  }
})
