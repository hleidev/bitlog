import request from '@/utils/request'

export interface LoginReq {
  username: string
  password: string
}

export interface LoginRes {
  accessToken: string
}

export async function login(data: LoginReq): Promise<LoginRes> {
  return request.post<never, LoginRes>('/v1/auth/login', data)
}

export async function refresh(): Promise<LoginRes> {
  return request.post<never, LoginRes>('/v1/auth/refresh')
}

export async function logout(): Promise<void> {
  await request.post('/v1/auth/logout')
}
