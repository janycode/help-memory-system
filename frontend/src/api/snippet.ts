import request from './request'

export interface CodeSnippet {
  id: number
  title: string
  description: string
  language: string
  code: string
  tags: string
  createdBy: number
  createdAt: string
  updatedAt: string
}

export const getSnippets = (page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc') => {
  return request.get(`/snippets?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`)
}

export const getAllSnippets = () => {
  return request.get('/snippets/list')
}

export const getSnippetById = (id: number) => {
  return request.get(`/snippets/${id}`)
}

export const getSnippetsByLanguage = (language: string) => {
  return request.get(`/snippets/language/${language}`)
}

export const searchSnippets = (keyword: string) => {
  return request.get(`/snippets/search?keyword=${encodeURIComponent(keyword)}`)
}

export const createSnippet = (data: Partial<CodeSnippet>) => {
  return request.post('/snippets', data)
}

export const updateSnippet = (id: number, data: Partial<CodeSnippet>) => {
  return request.put(`/snippets/${id}`, data)
}

export const deleteSnippet = (id: number) => {
  return request.delete(`/snippets/${id}`)
}

export const snippetApi = {
  getSnippets,
  getAllSnippets,
  getSnippetById,
  getSnippetsByLanguage,
  searchSnippets,
  createSnippet,
  updateSnippet,
  deleteSnippet
}
