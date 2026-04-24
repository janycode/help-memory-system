import request, { type ApiResponse, type PageResponse } from './request'
import type { Repository, RepositoryCreateRequest, RepositoryUpdateRequest } from '@/types/project'

/**
 * 代码仓库管理 API
 */

/**
 * 分页查询代码仓库列表
 */
export function getRepositories(params: {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
}): Promise<ApiResponse<PageResponse<Repository>>> {
  return request.get('/repositories', { params } as any)
}

/**
 * 查询所有活跃代码仓库
 */
export function getActiveRepositories(): Promise<ApiResponse<Repository[]>> {
  return request.get('/repositories/active')
}

/**
 * 根据代码仓库全称查询代码仓库
 */
export function getRepositoriesByFullName(fullname: string): Promise<ApiResponse<Repository[]>> {
  return request.get(`/repositories/fullname/${fullname}`)
}

/**
 * 根据ID查询代码仓库
 */
export function getRepository(id: number): Promise<ApiResponse<Repository>> {
  return request.get(`/repositories/${id}`)
}

/**
 * 搜索代码仓库
 */
export function searchRepositories(keyword: string): Promise<ApiResponse<Repository[]>> {
  return request.get('/repositories/search', { params: { keyword } } as any)
}

/**
 * 创建代码仓库
 */
export function createRepository(data: RepositoryCreateRequest): Promise<ApiResponse<Repository>> {
  return request.post('/repositories', data)
}

/**
 * 更新代码仓库
 */
export function updateRepository(id: number, data: RepositoryUpdateRequest): Promise<ApiResponse<Repository>> {
  return request.put(`/repositories/${id}`, data)
}

/**
 * 删除代码仓库（软删除）
 */
export function deleteRepository(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/repositories/${id}`)
}

/**
 * 永久删除代码仓库
 */
export function permanentlyDeleteRepository(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/repositories/${id}/permanent`)
}
