export type RocketMqEnvironment = 'dev' | 'test'

export interface JavaParseResult {
  tag: string
  messageBody: Record<string, any>
}

export interface TopicListResult {
  topics: string[]
  rawResponse: any
}

export interface SendMessageRequest {
  env: RocketMqEnvironment
  proxyHeaders: Record<string, string>
  topic: string
  key: string
  tag: string
  messageBody: string
  traceEnabled: boolean
}

export interface SendMessageResponse {
  status: number
  data: {
    sendStatus: string
    msgId: string
    messageQueue: {
      topic: string
      brokerName: string
      queueId: number
    }
    queueOffset: number
    transactionId: string
    offsetMsgId: string
    regionId: string
    traceOn: boolean
  }
  errMsg: string | null
}