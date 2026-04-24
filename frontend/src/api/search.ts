import request, { type ApiResponse } from './request'
import type { SearchResult } from '@/types/search'
import { snippetApi } from './snippet'

/**
 * 全局搜索 API
 */

/**
 * 搜索代码片段（包含标题和代码内容）
 */
async function searchSnippetsInContent(keyword: string) {
  try {
    // 先获取所有代码片段
    const response = await snippetApi.getAllSnippets()
    const allSnippets = response.data || []

    // 在前端过滤包含关键词的代码片段（搜索标题、描述和代码内容）
    const filteredSnippets = allSnippets.filter((snippet: any) => {
      const searchText = keyword.toLowerCase()
      return (
        (snippet.title && snippet.title.toLowerCase().includes(searchText)) ||
        (snippet.description && snippet.description.toLowerCase().includes(searchText)) ||
        (snippet.code && snippet.code.toLowerCase().includes(searchText))
      )
    })

    return { data: filteredSnippets }
  } catch (error) {
    console.error('搜索代码片段失败:', error)
    return { data: [] }
  }
}

/**
 * 全局搜索
 */
export function globalSearch(keyword: string): Promise<ApiResponse<SearchResult>> {
  // 并行搜索主API和代码片段
  return Promise.all([
    request.get('/search', { params: { q: keyword } } as any),
    searchSnippetsInContent(keyword)
  ]).then(([mainResponse, snippetResponse]) => {
    const result = mainResponse.data || {
      keyword,
      environments: [],
      components: [],
      processes: [],
      projects: [],
      snippets: [],
      total: 0
    }

    // 合并代码片段搜索结果
    const snippets = Array.isArray(snippetResponse.data) ? snippetResponse.data : []
    result.snippets = snippets
    result.total = (result.environments?.length || 0) +
                  (result.components?.length || 0) +
                  (result.processes?.length || 0) +
                  (result.projects?.length || 0) +
                  snippets.length

    return {
      ...mainResponse,
      data: result
    }
  })
}
