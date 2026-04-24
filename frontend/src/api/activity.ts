import request from './request'

export interface ActivityLog {
  id: number
  userId: number
  username: string
  operation: string
  module: string
  description: string
  details: string
  ipAddress: string
  userAgent: string
  success: boolean
  createdAt: string
}

export const activityApi = {
  getActivities(params: { page?: number; size?: number }) {
    return request.get('/activities', { params })
  },

  getRecentActivities(limit: number = 10) {
    return request.get('/activities/recent', { params: { limit } })
  },

  getMyActivities() {
    return request.get('/activities/my')
  },

  getActivitiesByModule(module: string) {
    return request.get(`/activities/module/${module}`)
  }
}
