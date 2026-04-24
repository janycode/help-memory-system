import request from './request'
import type { UserCreateRequest, UserUpdateRequest } from '@/types/user'

export const userApi = {
  getUsers(params: { page?: number; size?: number }) {
    return request.get('/admin/users', { params } as any)
  },

  getAllUsers() {
    return request.get('/admin/users/all')
  },

  getUserById(id: number) {
    return request.get(`/admin/users/${id}`)
  },

  createUser(data: UserCreateRequest) {
    return request.post('/admin/users', data)
  },

  updateUser(id: number, data: UserUpdateRequest) {
    return request.put(`/admin/users/${id}`, data)
  },

  deleteUser(id: number) {
    return request.delete(`/admin/users/${id}`)
  },

  resetPassword(id: number, newPassword: string) {
    return request.put(`/admin/users/${id}/reset-password`, { newPassword })
  },

  toggleUserStatus(id: number) {
    return request.put(`/admin/users/${id}/toggle-status`)
  }
}

export const authApi = {
  changePassword(data: { oldPassword: string; newPassword: string }) {
    return request.put('/auth/password', data)
  },

  logout() {
    return request.post('/auth/logout')
  },

  getCurrentUser() {
    return request.get('/auth/me')
  }
}
