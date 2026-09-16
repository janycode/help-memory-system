// Jenkins 监控配置类型定义
export interface JenkinsJobConfig {
  id?: number
  /** Jenkins job 名称 */
  jobName: string
  /** Jenkins 地址 */
  jenkinsUrl: string
  /** Jenkins 账号 */
  username: string
  /** Jenkins 密码（明文存储，编辑时会回显） */
  password: string
  /** 部署环境标识（如 DEV/TEST/PROD，通知消息中展示） */
  environment?: string
  /** 项目本地 git 目录（绝对路径），用于取最近提交日志，留空则不展示 */
  localDir?: string
  /** 企业微信机器人 webhook 地址 */
  webhookUrl: string
  /** 需 @ 的手机号（逗号分隔，可空） */
  atMobiles?: string
  /** 是否启用监控 */
  enabled: boolean
  /** 已通知的最大构建号 */
  lastNotifiedBuild?: number | null
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/** Jenkins 连接测试结果 */
export interface JenkinsTestResult {
  ok: boolean
  /** 该账号可见的 job 列表 */
  jobs: string[]
}