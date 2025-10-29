import request from '@/utils/request'
import { Result } from '@/types'

/**
 * 用户设备信息
 */
export interface UserDevice {
  id: number
  deviceType: string
  deviceName: string
  browser: string
  os: string
  ipAddress: string
  location?: string
  isTrusted: number
  lastActiveTime: string
  firstLoginTime: string
}

/**
 * 用户操作日志
 */
export interface UserOperationLog {
  id: number
  operationType: string
  operationDesc: string
  ipAddress: string
  location?: string
  browser: string
  os: string
  status: number
  errorMsg?: string
  createTime: string
}

/**
 * 双因素认证配置
 */
export interface User2FAConfig {
  id?: number
  type?: string
  enabled: number
  verifyPhone?: string
  verifyEmail?: string
  lastVerifyTime?: string
  createTime?: string
}

/**
 * 获取当前用户设备列表
 */
export const getUserDevices = () => {
  return request.get<Result<UserDevice[]>>('/admin/security/devices')
}

/**
 * 移除设备
 */
export const removeDevice = (deviceId: number) => {
  return request.delete<Result<void>>(`/admin/security/devices/${deviceId}`)
}

/**
 * 信任设备
 */
export const trustDevice = (deviceId: number) => {
  return request.put<Result<void>>(`/admin/security/devices/${deviceId}/trust`)
}

/**
 * 获取操作日志
 */
export const getOperationLogs = (limit?: number) => {
  return request.get<Result<UserOperationLog[]>>('/admin/security/operation-logs', {
    params: { limit },
  })
}

/**
 * 获取2FA配置
 */
export const get2FAConfig = () => {
  return request.get<Result<User2FAConfig>>('/admin/security/2fa')
}

/**
 * 启用2FA
 */
export const enable2FA = (type: string, verifyCode: string) => {
  return request.post<Result<void>>('/admin/security/2fa/enable', null, {
    params: { type, verifyCode },
  })
}

/**
 * 禁用2FA
 */
export const disable2FA = (verifyCode: string) => {
  return request.post<Result<void>>('/admin/security/2fa/disable', null, {
    params: { verifyCode },
  })
}
