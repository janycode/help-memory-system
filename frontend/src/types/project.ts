export interface Repository {
  id: number
  name: string
  description: string
  codeRepository: string
  documentPath: string
  deploymentPath: string
  port: string
  teamMembers: string
  projectFullName: string
  status: string
  createdBy: number
  createdAt: string
  updatedAt: string
  notes: string
  active: boolean
}

export interface RepositoryCreateRequest {
  name: string
  description?: string
  codeRepository?: string
  documentPath?: string
  deploymentPath?: string
  port?: string
  teamMembers?: string
  projectFullName?: string
  status?: string
  notes?: string
}

export interface RepositoryUpdateRequest extends RepositoryCreateRequest {
  id: number
}

export interface TeamMember {
  id: string
  name: string
  role: string
  email?: string
}
