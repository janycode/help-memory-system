export interface SysDictType {
  id: number
  typeCode: string
  typeName: string
  description?: string
  status: boolean
  createdBy?: number
  createdAt: string
  updatedAt: string
}

export interface SysDictData {
  id: number
  typeCode: string
  dataValue: string
  dataLabel: string
  sortOrder: number
  status: boolean
  remark?: string
  createdBy?: number
  createdAt: string
  updatedAt: string
}

export interface DictOption {
  label: string
  value: string
}
