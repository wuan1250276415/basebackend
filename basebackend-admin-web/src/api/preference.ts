import request from '@/utils/request'
import { Result } from '@/types'

/**
 * 用户偏好设置
 */
export interface UserPreference {
  theme: string
  language: string
  emailNotification: number
  smsNotification: number
  systemNotification: number
}

/**
 * 更新偏好设置请求
 */
export interface UpdatePreferenceRequest {
  theme?: string
  language?: string
  emailNotification?: number
  smsNotification?: number
  systemNotification?: number
}

/**
 * 获取当前用户偏好设置
 */
export const getPreference = () => {
  return request.get<Result<UserPreference>>('/admin/preference')
}

/**
 * 更新当前用户偏好设置
 */
export const updatePreference = (data: UpdatePreferenceRequest) => {
  return request.put<Result<void>>('/admin/preference', data)
}
