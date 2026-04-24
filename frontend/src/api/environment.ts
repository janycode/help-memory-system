import request, { type ApiResponse, type PageResponse } from './request'
import type { Environment, EnvironmentCreateRequest, EnvironmentUpdateRequest } from '@/types/environment'

/**
 * 环境管理 API
 */

/**
 * 分页查询环境列表
 */
export function getEnvironments(params: {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
}): Promise<ApiResponse<PageResponse<Environment>>> {
  return request.get('/environments', { params } as any)
}

/**
 * 查询所有活跃环境
 */
export function getActiveEnvironments(): Promise<ApiResponse<Environment[]>> {
  return request.get('/environments/active')
}

/**
 * 根据类型查询环境
 */
export function getEnvironmentsByType(type: string): Promise<ApiResponse<Environment[]>> {
  return request.get(`/environments/type/${type}`)
}

/**
 * 根据ID查询环境
 */
export function getEnvironment(id: number): Promise<ApiResponse<Environment>> {
  return request.get(`/environments/${id}`)
}

/**
 * 搜索环境
 */
export function searchEnvironments(keyword: string): Promise<ApiResponse<Environment[]>> {
  return request.get('/environments/search', { params: { keyword } } as any)
}

/**
 * 创建环境
 */
export function createEnvironment(data: EnvironmentCreateRequest): Promise<ApiResponse<Environment>> {
  return request.post('/environments', data)
}

/**
 * 更新环境
 */
export function updateEnvironment(id: number, data: EnvironmentUpdateRequest): Promise<ApiResponse<Environment>> {
  return request.put(`/environments/${id}`, data)
}

/**
 * 删除环境（软删除）
 */
export function deleteEnvironment(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/environments/${id}`)
}

/**
 * 永久删除环境
 */
export function permanentlyDeleteEnvironment(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/environments/${id}/permanent`)
}
