import request, { type ApiResponse } from './request'
import type { SysDictType, SysDictData } from '@/types/dict'

/**
 * 字典管理 API
 */

// ==================== 字典类型 API ====================

export function getDictTypes(): Promise<ApiResponse<SysDictType[]>> {
  return request.get('/dicts/types')
}

export function getActiveDictTypes(): Promise<ApiResponse<SysDictType[]>> {
  return request.get('/dicts/types/active')
}

export function getDictTypeById(id: number): Promise<ApiResponse<SysDictType>> {
  return request.get(`/dicts/types/${id}`)
}

export function createDictType(data: Partial<SysDictType>): Promise<ApiResponse<SysDictType>> {
  return request.post('/dicts/types', data)
}

export function updateDictType(id: number, data: Partial<SysDictType>): Promise<ApiResponse<SysDictType>> {
  return request.put(`/dicts/types/${id}`, data)
}

export function deleteDictType(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/dicts/types/${id}`)
}

// ==================== 字典数据 API ====================

export function getDictDataByType(typeCode: string): Promise<ApiResponse<SysDictData[]>> {
  return request.get(`/dicts/data/${typeCode}`)
}

export function getAllDictDataByType(typeCode: string): Promise<ApiResponse<SysDictData[]>> {
  return request.get(`/dicts/data/${typeCode}/all`)
}

export function getDictDataById(id: number): Promise<ApiResponse<SysDictData>> {
  return request.get(`/dicts/data/id/${id}`)
}

export function createDictData(data: Partial<SysDictData>): Promise<ApiResponse<SysDictData>> {
  return request.post('/dicts/data', data)
}

export function updateDictData(id: number, data: Partial<SysDictData>): Promise<ApiResponse<SysDictData>> {
  return request.put(`/dicts/data/id/${id}`, data)
}

export function deleteDictData(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/dicts/data/id/${id}`)
}
