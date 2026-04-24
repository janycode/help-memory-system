export interface User {
  id: number
  username: string
  email: string
  fullName: string
  department: string
  position: string
  roles: string[]
  enabled: boolean
  createdAt: string
  updatedAt: string
  lastLoginAt: string
}

export interface UserCreateRequest {
  username: string
  email: string
  password: string
  fullName?: string
  department?: string
  position?: string
  roles?: string[]
}

export interface UserUpdateRequest {
  email?: string
  fullName?: string
  department?: string
  position?: string
  roles?: string[]
  enabled?: boolean
}

export interface PasswordChangeRequest {
  oldPassword: string
  newPassword: string
}
