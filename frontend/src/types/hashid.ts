export interface HashIdRequest {
  input: string | number
  salt: string
  length: number
  mode: 'encode' | 'decode'
}

export interface HashIdResponse {
  result: string | number[]
}
