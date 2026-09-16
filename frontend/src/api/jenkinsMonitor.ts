import request, { type ApiResponse } from './request'
import type { JenkinsJobConfig, JenkinsTestResult } from '@/types/jenkinsMonitor'

/** 查询监控配置列表 */
export function getJenkinsMonitorConfigs(): Promise<ApiResponse<JenkinsJobConfig[]>> {
  return request.get('/jenkins-monitor')
}

/** 新增监控配置 */
export function createJenkinsMonitorConfig(data: JenkinsJobConfig): Promise<ApiResponse<JenkinsJobConfig>> {
  return request.post('/jenkins-monitor', data)
}

/** 更新监控配置 */
export function updateJenkinsMonitorConfig(id: number, data: Partial<JenkinsJobConfig>): Promise<ApiResponse<JenkinsJobConfig>> {
  return request.put(`/jenkins-monitor/${id}`, data)
}

/** 删除监控配置 */
export function deleteJenkinsMonitorConfig(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/jenkins-monitor/${id}`)
}

/** 测试 Jenkins 连接（密码留空时按 id 使用已保存密码；传 jobName 会校验 job 是否存在） */
export function testJenkinsConnection(data: {
  id?: number
  jenkinsUrl: string
  username: string
  password: string
  jobName?: string
}): Promise<ApiResponse<JenkinsTestResult>> {
  return request.post('/jenkins-monitor/test', data)
}