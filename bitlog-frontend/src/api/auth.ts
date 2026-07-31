import request from '@/utils/request'

export interface LoginReq {
  email: string
  password: string
}

export interface RegisterReq {
  email: string
  username: string
  password: string
  code: string
}

export interface PasswordResetReq {
  email: string
  code: string
  newPassword: string
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

export async function sendRegisterCode(email: string): Promise<void> {
  await request.post('/v1/auth/register/code', { email })
}

export async function register(data: RegisterReq): Promise<void> {
  await request.post('/v1/auth/register', data)
}

export async function sendResetPasswordCode(email: string): Promise<void> {
  await request.post('/v1/auth/password/reset/code', { email })
}

export async function resetPassword(data: PasswordResetReq): Promise<void> {
  await request.post('/v1/auth/password/reset', data)
}
