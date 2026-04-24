import request, { type ApiResponse, type PageResponse } from './request'
import type { BusinessProcess, ProcessCreateRequest, ProcessUpdateRequest } from '@/types/process'

/**
 * 业务流程 API
 */

/**
 * 分页查询业务流程列表
 */
export function getProcesses(params: {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
}): Promise<ApiResponse<PageResponse<BusinessProcess>>> {
  return request.get('/processes', { params } as any)
}

/**
 * 查询所有活跃业务流程
 */
export function getActiveProcesses(): Promise<ApiResponse<BusinessProcess[]>> {
  return request.get('/processes/active')
}

/**
 * 查询按优先级排序的业务流程
 */
export function getProcessesByPriority(): Promise<ApiResponse<BusinessProcess[]>> {
  return request.get('/processes/priority')
}

/**
 * 根据分类查询业务流程
 */
export function getProcessesByCategory(category: string): Promise<ApiResponse<BusinessProcess[]>> {
  return request.get(`/processes/category/${category}`)
}

/**
 * 根据ID查询业务流程
 */
export function getProcess(id: number): Promise<ApiResponse<BusinessProcess>> {
  return request.get(`/processes/${id}`)
}

/**
 * 搜索业务流程
 */
export function searchProcesses(keyword: string): Promise<ApiResponse<BusinessProcess[]>> {
  return request.get('/processes/search', { params: { keyword } } as any)
}

/**
 * 创建业务流程
 */
export function createProcess(data: ProcessCreateRequest): Promise<ApiResponse<BusinessProcess>> {
  return request.post('/processes', data)
}

/**
 * 更新业务流程
 */
export function updateProcess(id: number, data: ProcessUpdateRequest): Promise<ApiResponse<BusinessProcess>> {
  return request.put(`/processes/${id}`, data)
}

/**
 * 删除业务流程（软删除）
 */
export function deleteProcess(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/processes/${id}`)
}

/**
 * 永久删除业务流程
 */
export function permanentlyDeleteProcess(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/processes/${id}/permanent`)
}
