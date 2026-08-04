import request from './request'

export const hashidApi = {
  encode(input: number, salt: string, length: number) {
    return request.post('/tools/hashid/encode', { input, salt, length })
  },
  decode(input: string, salt: string, length: number) {
    return request.post('/tools/hashid/decode', { input, salt, length })
  }
}
