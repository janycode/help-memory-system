import type { Environment } from './environment'
import type { TechnicalComponent } from './component'
import type { BusinessProcess } from './process'
import type { Project } from './project'
import type { CodeSnippet } from './snippet'

// 从 request.ts 重新导出 PageResponse 类型
export type { PageResponse } from '@/api/request'

/**
 * 全局搜索结果
 */
export interface SearchResult {
  keyword: string
  environments: Environment[]
  components: TechnicalComponent[]
  processes: BusinessProcess[]
  projects: Project[]
  snippets: CodeSnippet[]
  total: number
}

/**
 * 分页请求参数
 */
export interface PageRequest {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}
