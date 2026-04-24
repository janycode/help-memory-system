export interface CodeSnippet {
  id: number
  title: string
  description: string
  language: string
  code: string
  tags: string
  status?: boolean
  createdBy: number
  createdAt: string
  updatedAt: string
}