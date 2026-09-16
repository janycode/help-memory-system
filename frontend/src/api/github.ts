import request from './request'
import type {
  GitHubConfig,
  GitHubIssue,
  GitHubIssueDetail,
  GitHubPromptResult
} from '@/types/github'

// GitHub API 链路耗时较高，单独放宽超时时间
const GITHUB_TIMEOUT = 60000

export function getGitHubConfig(): Promise<{ code: number; message: string; data: GitHubConfig }> {
  return request.get('/github/config')
}

export function saveGitHubConfig(data: GitHubConfig): Promise<{ code: number; message: string; data: GitHubConfig }> {
  return request.post('/github/config', data)
}

export function testGitHubConnection(): Promise<{ code: number; message: string; data: { login: string } }> {
  return request.post('/github/connection-test', undefined, { timeout: GITHUB_TIMEOUT } as any)
}

export function getGitHubIssues(): Promise<{ code: number; message: string; data: GitHubIssue[] }> {
  return request.get('/github/issues', { timeout: GITHUB_TIMEOUT } as any)
}

export function getGitHubIssueDetail(
  number: number
): Promise<{ code: number; message: string; data: GitHubIssueDetail }> {
  return request.get(`/github/issues/${number}`, { timeout: GITHUB_TIMEOUT } as any)
}

export function buildGitHubPrompt(
  number: number
): Promise<{ code: number; message: string; data: GitHubPromptResult }> {
  return request.post(`/github/issues/${number}/prompt`, undefined, { timeout: GITHUB_TIMEOUT } as any)
}

export function getCustomPrompt(number: number): Promise<{ code: number; message: string; data: { prompt: string } }> {
  return request.get(`/github/issues/${number}/prompt/custom`)
}

export function saveCustomPrompt(number: number, prompt: string): Promise<{ code: number; message: string }> {
  return request.put(`/github/issues/${number}/prompt/custom`, { prompt })
}