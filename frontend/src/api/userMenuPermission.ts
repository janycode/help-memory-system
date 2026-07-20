import request from './request'

export interface UserPermissionDTO {
  userId: number
  username: string
  fullName: string
  allowedMenus: string[]
}

export const userMenuPermissionApi = {
  getMenus(userId: number) {
    return request.get<string[]>(`/user-menu-permissions/${userId}`)
  },

  saveMenus(userId: number, menus: string[]) {
    return request.put(`/user-menu-permissions/${userId}`, menus)
  },

  getAllUsersPermissions() {
    return request.get<UserPermissionDTO[]>('/user-menu-permissions/all-users')
  }
}
