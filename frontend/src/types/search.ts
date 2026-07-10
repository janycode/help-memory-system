import type { Environment } from './environment'
import type { TechnicalComponent } from './component'
import type { BusinessProcess } from './process'
import type { Repository } from './project'
import type { CodeSnippet } from './snippet'
import type { Iteration } from './iteration'

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
  projects: Repository[]
  snippets: CodeSnippet[]
  iterations: Iteration[]
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
