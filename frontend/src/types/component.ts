export type ComponentCategory = 'Database' | 'Cache' | 'MessageQueue' | 'API' | 'Storage' | 'Monitoring' | 'Authentication' | 'Other'

export interface TechnicalComponent {
  id: number
  name: string
  description: string
  category: string
  url: string
  username: string
  password: string
  version: string
  environmentId: number
  environmentType?: string
  createdBy: number
  createdAt: string
  updatedAt: string
  configuration: string
  notes: string
  active: boolean
}

export interface ComponentCreateRequest {
  name: string
  description?: string
  category: string
  url?: string
  username?: string
  password?: string
  version?: string
  environmentId?: number
  configuration?: string
  notes?: string
}

export interface ComponentUpdateRequest extends ComponentCreateRequest {
  id: number
}
