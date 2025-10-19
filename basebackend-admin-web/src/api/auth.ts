import request from '@/utils/request'
import { LoginRequest, LoginResponse, Result, UserInfo } from '@/types'

// 用户登录
export const login = (data: LoginRequest) => {
  return request.post<Result<LoginResponse>>('/admin/auth/login', data)
}

// 用户登出
export const logout = () => {
  return request.post<Result<string>>('/admin/auth/logout')
}

// 刷新Token
export const refreshToken = (refreshToken: string) => {
  return request.post<Result<LoginResponse>>('/admin/auth/refresh', null, {
    params: { refreshToken },
  })
}

// 获取当前用户信息
export const getCurrentUserInfo = () => {
  return request.get<Result<UserInfo>>('/admin/auth/info')
}

// 修改密码
export const changePassword = (data: {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}) => {
  return request.put<Result<string>>('/admin/auth/password', data)
}
