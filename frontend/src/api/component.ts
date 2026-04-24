import request, { type ApiResponse, type PageResponse } from './request'
import type { TechnicalComponent, ComponentCreateRequest, ComponentUpdateRequest } from '@/types/component'

export interface HealthCheckResult {
  reachable: boolean
  latencyMs: number
  message: string
}

/**
 * 技术组件 API
 */

/**
 * 分页查询技术组件列表
 */
export function getComponents(params: {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
}): Promise<ApiResponse<PageResponse<TechnicalComponent>>> {
  return request.get('/components', { params } as any)
}

/**
 * 查询所有活跃技术组件
 */
export function getActiveComponents(): Promise<ApiResponse<TechnicalComponent[]>> {
  return request.get('/components/active')
}

/**
 * 根据分类查询技术组件
 */
export function getComponentsByCategory(category: string): Promise<ApiResponse<TechnicalComponent[]>> {
  return request.get(`/components/category/${category}`)
}

/**
 * 根据环境ID查询技术组件
 */
export function getComponentsByEnvironment(environmentId: number): Promise<ApiResponse<TechnicalComponent[]>> {
  return request.get(`/components/environment/${environmentId}`)
}

/**
 * 根据ID查询技术组件
 */
export function getComponent(id: number): Promise<ApiResponse<TechnicalComponent>> {
  return request.get(`/components/${id}`)
}

/**
 * 搜索技术组件
 */
export function searchComponents(keyword: string): Promise<ApiResponse<TechnicalComponent[]>> {
  return request.get('/components/search', { params: { keyword } } as any)
}

/**
 * 创建技术组件
 */
export function createComponent(data: ComponentCreateRequest): Promise<ApiResponse<TechnicalComponent>> {
  return request.post('/components', data)
}

/**
 * 更新技术组件
 */
export function updateComponent(id: number, data: ComponentUpdateRequest): Promise<ApiResponse<TechnicalComponent>> {
  return request.put(`/components/${id}`, data)
}

/**
 * 删除技术组件（软删除）
 */
export function deleteComponent(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/components/${id}`)
}

/**
 * 永久删除技术组件
 */
export function permanentlyDeleteComponent(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/components/${id}/permanent`)
}

/**
 * 健康检查（连通性检测）
 */
export function checkComponentHealth(id: number): Promise<ApiResponse<HealthCheckResult>> {
  return request.get(`/components/${id}/health`)
}
