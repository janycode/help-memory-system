export interface Iteration {
  id: number
  issueNumber: string
  projectCode: string
  title: string
  issueUrl: string
  status: IterationStatus
  priority: IterationPriority
  developmentNotes: string
  releaseNotes: string
  flowchartPath: string
  assigneeId: number | null
  estimatedTime: string | null
  actualTime: string | null
  apiDocUrl: string
  impactScope: string
  todos: string
  createdBy: number | null
  createdAt: string
  updatedAt: string
  statusChangedAt: string | null
  completedAt: string | null
  localDirPath: string | null
  hasTodos: boolean | null
  active: boolean
}

export type IterationStatus = 'TODO' | 'IN_PROGRESS' | 'TESTING' | 'DONE'

export type IterationPriority = 'HIGH' | 'MEDIUM' | 'LOW'

export interface IterationCreateRequest {
  issueNumber: string
  projectCode: string
  title: string
  issueUrl?: string
  status: IterationStatus
  priority: IterationPriority
  developmentNotes?: string
  releaseNotes?: string
  flowchartPath?: string
  assigneeId?: number
  estimatedTime?: string
  actualTime?: string
  apiDocUrl?: string
  impactScope?: string
  todos?: string
}

export interface IterationUpdateRequest extends Partial<IterationCreateRequest> {
  id: number
}

export interface IterationQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
  status?: string
  priority?: string
}

export interface IterationStatusInfo {
  value: IterationStatus
  label: string
  color: string
  order: number
}

export interface IterationPriorityInfo {
  value: IterationPriority
  label: string
  color: string
}

export const IterationStatusMap: Record<IterationStatus, IterationStatusInfo> = {
  TODO: { value: 'TODO', label: '待开发', color: '#909399', order: 1 },
  IN_PROGRESS: { value: 'IN_PROGRESS', label: '开发中', color: '#409EFF', order: 2 },
  TESTING: { value: 'TESTING', label: '测试中', color: '#13C2C2', order: 3 },
  DONE: { value: 'DONE', label: '已完成', color: '#67C23A', order: 4 }
}

export const IterationPriorityMap: Record<IterationPriority, IterationPriorityInfo> = {
  HIGH: { value: 'HIGH', label: '高', color: '#F56C6C' },
  MEDIUM: { value: 'MEDIUM', label: '中', color: '#E6A23C' },
  LOW: { value: 'LOW', label: '低', color: '#67C23A' }
}

export interface IterationImportConfig {
  id: number
  baseDirPath: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface IterationSyncHistory {
  id: number
  iterationId: number
  syncType: 'PAGE_TO_LOCAL' | 'LOCAL_TO_PAGE'
  fieldName: string
  oldValue: string | null
  newValue: string | null
  syncedAt: string
  createdAt: string
}

export interface OtherFile {
  name: string
  path: string
}
