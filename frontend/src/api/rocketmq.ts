import request from './request'
import type { SendMessageRequest } from '@/types/rocketmq'

export const rocketmqApi = {
  getTopics(targetUrl: string, proxyHeaders: Record<string, string>, env?: string) {
    return request.post('/tools/mq-topics', { targetUrl, proxyHeaders, env })
  },

  previewJavaFile(filePath: string) {
    return request.post('/tools/mq-preview-java', { filePath })
  },

  parseJavaFile(filePath: string) {
    return request.post('/tools/mq-parse-java', { filePath })
  },

  sendMessage(data: SendMessageRequest & { targetUrl: string }) {
    return request.post('/tools/mq-send', data)
  }
}