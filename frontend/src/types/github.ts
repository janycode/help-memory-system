// GitHub 指派任务相关类型

export interface GitHubConfig {
  baseUrl?: string
  owner?: string
  repo?: string
  token?: string
  tokenConfigured?: boolean
  proxyHost?: string
  proxyPort?: string
  bugLabels?: string
}

export interface GitHubIssue {
  number: number
  title: string
  state: string
  labels: string[]
  isBug: boolean
  htmlUrl: string
  createdAt: string
  updatedAt: string
  assignee: string
}

export interface GitHubComment {
  author: string
  createdAt: string
  body: string
}

export interface GitHubImage {
  sourceUrl: string
  localPath: string
  downloaded: boolean
  note: string
}

export interface GitHubIssueDetail {
  number: number
  title: string
  state: string
  labels: string[]
  isBug: boolean
  htmlUrl: string
  body: string
  assignee: string
  createdAt: string
  updatedAt: string
  comments: GitHubComment[]
  images: GitHubImage[]
  warnings: string[]
  imageWarnings: string[]
}

export interface GitHubPromptResult {
  prompt: string
  number: number
  title: string
  htmlUrl: string
  warnings: string[]
}