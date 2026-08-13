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

  previewJavaFileUpload(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/tools/mq-preview-java-upload', formData)
  },

  parseJavaFileUpload(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/tools/mq-parse-java-upload', formData)
  },

  sendMessage(data: SendMessageRequest & { targetUrl: string }) {
    return request.post('/tools/mq-send', data)
  }
}