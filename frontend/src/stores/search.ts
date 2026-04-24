import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { SearchResult } from '@/types/search'
import { globalSearch } from '@/api/search'

export const useSearchStore = defineStore('search', () => {
  const searchResult = ref<SearchResult | null>(null)
  const isSearching = ref(false)
  const searchError = ref<string | null>(null)
  const searchHistory = ref<string[]>([])

  const search = async (keyword: string) => {
    if (!keyword.trim()) {
      searchResult.value = null
      return
    }

    isSearching.value = true
    searchError.value = null

    try {
      const response = await globalSearch(keyword)
      searchResult.value = response.data

      // 添加到搜索历史
      if (!searchHistory.value.includes(keyword)) {
        searchHistory.value.unshift(keyword)
        if (searchHistory.value.length > 10) {
          searchHistory.value.pop()
        }
        localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
      }

      return response.data
    } catch (e: any) {
      searchError.value = e.message || '搜索失败'
      return null
    } finally {
      isSearching.value = false
    }
  }

  const clearSearch = () => {
    searchResult.value = null
    searchError.value = null
  }

  const clearHistory = () => {
    searchHistory.value = []
    localStorage.removeItem('searchHistory')
  }

  // 从 localStorage 恢复搜索历史
  const savedHistory = localStorage.getItem('searchHistory')
  if (savedHistory) {
    try {
      searchHistory.value = JSON.parse(savedHistory)
    } catch {
      // ignore
    }
  }

  return {
    searchResult,
    isSearching,
    searchError,
    searchHistory,
    search,
    clearSearch,
    clearHistory
  }
})
