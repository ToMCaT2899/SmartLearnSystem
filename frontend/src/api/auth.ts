import request from '@/utils/request'
import type { LoginForm, RegisterForm, ChangePasswordForm, UserInfo, ApiResult } from '@/types'

export function login(data: LoginForm) {
  return request.post<ApiResult<UserInfo>>('/auth/login', data)
}

export function register(data: RegisterForm) {
  return request.post<ApiResult>('/auth/register', data)
}

export function getCurrentUser() {
  return request.get<ApiResult<UserInfo>>('/auth/me')
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put<ApiResult>('/auth/password', data)
}
