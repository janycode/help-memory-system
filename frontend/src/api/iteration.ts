import request, { type ApiResponse, type PageResponse } from './request'
import type { Iteration, IterationCreateRequest, IterationQueryParams, IterationImportConfig, IterationSyncHistory } from '@/types/iteration'

export function getIterations(params: IterationQueryParams): Promise<ApiResponse<PageResponse<Iteration>>> {
  return request.get('/iterations', { params } as any)
}

export function getActiveIterations(): Promise<ApiResponse<Iteration[]>> {
  return request.get('/iterations/active')
}

export function getIteration(id: number): Promise<ApiResponse<Iteration>> {
  return request.get(`/iterations/${id}`)
}

export function searchIterations(keyword: string): Promise<ApiResponse<Iteration[]>> {
  return request.get('/iterations/search', { params: { keyword } } as any)
}

export function getIterationsByStatus(status: string): Promise<ApiResponse<Iteration[]>> {
  return request.get(`/iterations/status/${status}`)
}

export function getIterationsByPriority(priority: string): Promise<ApiResponse<Iteration[]>> {
  return request.get(`/iterations/priority/${priority}`)
}

export function createIteration(data: IterationCreateRequest): Promise<ApiResponse<Iteration>> {
  return request.post('/iterations', data)
}

export function updateIteration(id: number, data: Partial<IterationCreateRequest>): Promise<ApiResponse<Iteration>> {
  return request.put(`/iterations/${id}`, data)
}

export function deleteIteration(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/iterations/${id}`)
}

export function permanentlyDeleteIteration(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/iterations/${id}/permanent`)
}

export function getImportConfig(): Promise<ApiResponse<IterationImportConfig | null>> {
  return request.get('/iterations/import/config')
}

export function importFromDirectory(dirPath: string): Promise<ApiResponse<Iteration[]>> {
  return request.post('/iterations/import', { dirPath })
}

export function syncIterationToLocal(id: number): Promise<ApiResponse<void>> {
  return request.post(`/iterations/${id}/sync`)
}

export function syncAllToLocal(): Promise<ApiResponse<void>> {
  return request.post('/iterations/sync-all')
}

export function autoSync(): Promise<ApiResponse<{ synced: number; timestamp: string }>> {
  return request.post('/iterations/auto-sync')
}

export function checkNewFolders(): Promise<ApiResponse<{ imported: number; names: string[]; timestamp: string }>> {
  return request.post('/iterations/check-new-folders')
}

export function refreshFileLists(): Promise<ApiResponse<{ refreshed: number; timestamp: string }>> {
  return request.post('/iterations/refresh-file-lists')
}

export function getSyncHistory(id: number): Promise<ApiResponse<IterationSyncHistory[]>> {
  return request.get(`/iterations/${id}/sync-history`)
}