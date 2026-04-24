import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { CodeSnippet } from '@/api/snippet'
import {
  getSnippets,
  getAllSnippets,
  getSnippetById,
  getSnippetsByLanguage,
  searchSnippets,
  createSnippet,
  updateSnippet,
  deleteSnippet
} from '@/api/snippet'
import type { PageResponse } from '@/types/search'

export const useSnippetStore = defineStore('snippet', () => {
  const snippets = ref<CodeSnippet[]>([])
  const currentSnippet = ref<CodeSnippet | null>(null)
  const pagination = ref<PageResponse<CodeSnippet> | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  const snippetsByLanguage = computed(() => {
    const map = new Map<string, CodeSnippet[]>()
    snippets.value.forEach(snippet => {
      const list = map.get(snippet.language) || []
      list.push(snippet)
      map.set(snippet.language, list)
    })
    return map
  })

  const fetchSnippets = async (params: {
    page?: number
    size?: number
    sortBy?: string
    sortDir?: string
  }) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getSnippets(params.page || 0, params.size || 10, params.sortBy || 'createdAt', params.sortDir || 'desc')
      if (response.data) {
        pagination.value = response.data
        snippets.value = response.data.content
      }
    } catch (e: any) {
      error.value = e.message || '获取代码片段失败'
    } finally {
      isLoading.value = false
    }
  }

  const getAllSnippetsList = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getAllSnippets()
      if (response.data) {
        snippets.value = response.data
      }
    } catch (e: any) {
      error.value = e.message || '获取代码片段列表失败'
    } finally {
      isLoading.value = false
    }
  }

  const fetchSnippetById = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getSnippetById(id)
      if (response.data) {
        currentSnippet.value = response.data
      }
    } catch (e: any) {
      error.value = e.message || '获取代码片段详情失败'
    } finally {
      isLoading.value = false
    }
  }

  const getSnippetsByLanguageType = async (language: string) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await getSnippetsByLanguage(language)
      if (response.data) {
        snippets.value = response.data
      }
    } catch (e: any) {
      error.value = e.message || '按语言筛选代码片段失败'
    } finally {
      isLoading.value = false
    }
  }

  const searchSnippetList = async (keyword: string) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await searchSnippets(keyword)
      if (response.data) {
        snippets.value = response.data
      }
    } catch (e: any) {
      error.value = e.message || '搜索代码片段失败'
    } finally {
      isLoading.value = false
    }
  }

  const addSnippet = async (data: Partial<CodeSnippet>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await createSnippet(data)
      if (response.data) {
        snippets.value.unshift(response.data)
        return response.data
      }
      return null
    } catch (e: any) {
      error.value = e.message || '创建代码片段失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const editSnippet = async (id: number, data: Partial<CodeSnippet>) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await updateSnippet(id, data)
      if (response.data) {
        const index = snippets.value.findIndex(s => s.id === id)
        if (index !== -1) {
          snippets.value[index] = response.data
        }
        return response.data
      }
      return null
    } catch (e: any) {
      error.value = e.message || '更新代码片段失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const removeSnippet = async (id: number) => {
    isLoading.value = true
    error.value = null
    try {
      await deleteSnippet(id)
      snippets.value = snippets.value.filter(s => s.id !== id)
      return true
    } catch (e: any) {
      error.value = e.message || '删除代码片段失败'
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    snippets,
    currentSnippet,
    pagination,
    isLoading,
    error,
    snippetsByLanguage,
    fetchSnippets,
    getAllSnippetsList,
    fetchSnippetById,
    getSnippetsByLanguageType,
    searchSnippetList,
    addSnippet,
    editSnippet,
    removeSnippet
  }
})