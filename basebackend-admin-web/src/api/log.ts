import request from '@/utils/request'
import { LoginLog, OperationLog, Result, PageResult } from '@/types'

// ============ 登录日志接口 ============

// 分页查询登录日志
export const getLoginLogPage = (params: any) => {
  return request.get<PageResult<LoginLog>>('/basebackend-system-api/api/system/logs/login', { params })
}

// 根据ID查询登录日志
export const getLoginLogById = (id: string) => {
  return request.get<Result<LoginLog>>(`/basebackend-system-api/api/system/logs/login/${id}`)
}

// 删除登录日志
export const deleteLoginLog = (id: string) => {
  return request.delete<Result<string>>(`/basebackend-system-api/api/system/logs/login/${id}`)
}

// 批量删除登录日志
export const deleteLoginLogBatch = (ids: string[]) => {
  return request.delete<Result<string>>('/basebackend-system-api/api/system/logs/login/batch', { data: ids })
}

// 清空登录日志
export const cleanLoginLog = () => {
  return request.delete<Result<string>>('/basebackend-system-api/api/system/logs/login/clean')
}

// ============ 操作日志接口 ============

// 分页查询操作日志
export const getOperationLogPage = (params: any) => {
  return request.get<PageResult<OperationLog>>('/basebackend-system-api/api/system/logs/operation', { params })
}

// 根据ID查询操作日志
export const getOperationLogById = (id: string) => {
  return request.get<Result<OperationLog>>(`/basebackend-system-api/api/system/logs/operation/${id}`)
}

// 删除操作日志
export const deleteOperationLog = (id: string) => {
  return request.delete<Result<string>>(`/basebackend-system-api/api/system/logs/operation/${id}`)
}

// 批量删除操作日志
export const deleteOperationLogBatch = (ids: string[]) => {
  return request.delete<Result<string>>('/basebackend-system-api/api/system/logs/operation/batch', { data: ids })
}

// 清空操作日志
export const cleanOperationLog = () => {
  return request.delete<Result<string>>('/basebackend-system-api/api/system/logs/operation/clean')
}
