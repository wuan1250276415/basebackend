import request from '@/api/request'

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

const BASE_URL = '/basebackend-user-api/api/user/preference'
/**
 * 获取当前用户偏好设置
 */
export const getPreference = () => {
  return request.get<UserPreference>(`${BASE_URL}`)
}

/**
 * 更新当前用户偏好设置
 */
export const updatePreference = (data: UpdatePreferenceRequest) => {
  return request.put<void>(`${BASE_URL}`, data)
}
