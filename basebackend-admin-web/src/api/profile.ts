import request from '@/utils/request'
import { Result } from '@/types'

/**
 * 个人资料详情
 */
export interface ProfileDetail {
  userId: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  gender?: number
  birthday?: string
  deptId?: number
  deptName?: string
  userType: number
  status: number
  loginIp?: string
  loginTime?: string
  createTime: string
}

/**
 * 更新个人资料请求
 */
export interface UpdateProfileRequest {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  gender?: number
  birthday?: string
}

/**
 * 修改密码请求
 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

/**
 * 获取当前用户的个人资料
 */
export const getProfile = () => {
  return request.get<Result<ProfileDetail>>('/admin/profile/info')
}

/**
 * 更新当前用户的个人资料
 */
export const updateProfile = (data: UpdateProfileRequest) => {
  return request.put<Result<void>>('/admin/profile/info', data)
}

/**
 * 修改当前用户密码
 */
export const changePassword = (data: ChangePasswordRequest) => {
  return request.put<Result<void>>('/admin/profile/password', data)
}
