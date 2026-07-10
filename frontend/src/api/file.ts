import request, { type ApiResponse } from './request'

export function readFile(path: string): Promise<ApiResponse<{ content: string; path: string }>> {
  return request.get('/files/read', { params: { path } } as any)
}

export function writeFile(path: string, content: string): Promise<ApiResponse<void>> {
  return request.post('/files/write', { path, content })
}

export function checkFileUpdate(path: string, lastModified: number): Promise<ApiResponse<{ exists: boolean; updated: boolean; lastModified: number }>> {
  return request.get('/files/check', { params: { path, lastModified } } as any)
}
