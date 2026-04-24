export type ProcessCategory = 'release' | 'business' | 'operation' | 'incident' | 'maintenance' | 'security' | 'other'

export interface BusinessProcess {
  id: number
  name: string
  description: string
  category: string
  environmentId?: number
  environmentType?: string
  processFlow: string
  flowchartPath: string
  steps: string
  precautions: string
  checklist: string
  createdBy: number
  createdAt: string
  updatedAt: string
  relatedDocuments: string
  priority: number
  active: boolean
}

export interface ProcessCreateRequest {
  name: string
  description?: string
  category: string
  environmentId?: number
  processFlow?: string
  flowchartPath?: string
  steps?: string
  precautions?: string
  checklist?: string
  relatedDocuments?: string
  priority?: number
  notes?: string
}

export interface ProcessUpdateRequest extends ProcessCreateRequest {
  id: number
}

export interface ProcessStep {
  order: number
  title: string
  description: string
  notes?: string
}

export interface ChecklistItem {
  id: string
  content: string
  checked: boolean
}
