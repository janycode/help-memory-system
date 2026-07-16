import request, { type ApiResponse } from './request'

export function readFile(path: string): Promise<ApiResponse<{ content: string; path: string }>> {
  return request.post('/files/read', { path })
}

export function writeFile(path: string, content: string): Promise<ApiResponse<void>> {
  return request.post('/files/write', { path, content })
}

export function checkFileUpdate(path: string, lastModified: number): Promise<ApiResponse<{ exists: boolean; updated: boolean; lastModified: number }>> {
  return request.post('/files/check', { path, lastModified })
}

export function openFile(path: string): Promise<ApiResponse<void>> {
  return request.post('/files/open', { path })
}
