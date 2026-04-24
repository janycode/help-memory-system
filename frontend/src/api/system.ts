import request from './request'

export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  description: string
  editable: boolean
  createdAt: string
  updatedAt: string
}

export const systemApi = {
  getConfigs() {
    return request.get('/system/config')
  },

  getConfigByKey(key: string) {
    return request.get(`/system/config/${key}`)
  },

  getSystemTitle() {
    return request.get('/system/title')
  },

  updateConfig(id: number, value: string) {
    return request.put(`/system/config/${id}`, { value })
  },

  createConfig(data: { configKey: string; configValue: string; description?: string }) {
    return request.post('/system/config', data)
  },

  deleteConfig(id: number) {
    return request.delete(`/system/config/${id}`)
  },

  getDatabasePath() {
    return request.get('/system/database/path')
  }
}
