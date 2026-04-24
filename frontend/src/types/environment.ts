export type EnvironmentType = 'DEV' | 'TEST' | 'STAGING' | 'PROD' | 'DEMO'

export interface Environment {
  id: number
  name: string
  description: string
  type: string
  url: string
  username: string
  password: string
  databaseUrl: string
  databaseUsername: string
  databasePassword: string
  redisUrl: string
  redisPassword: string
  mqUrl: string
  mqUsername: string
  mqPassword: string
  createdBy: number
  createdAt: string
  updatedAt: string
  notes: string
  active: boolean
}

export interface EnvironmentCreateRequest {
  name: string
  description?: string
  type: string
  url?: string
  username?: string
  password?: string
  databaseUrl?: string
  databaseUsername?: string
  databasePassword?: string
  redisUrl?: string
  redisPassword?: string
  mqUrl?: string
  mqUsername?: string
  mqPassword?: string
  notes?: string
}

export interface EnvironmentUpdateRequest extends EnvironmentCreateRequest {
  id: number
}
