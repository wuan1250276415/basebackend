import request from '@/utils/request'
import { Application, ApplicationResource, Result,Menu } from '@/types'

// ==================== 应用管理 ====================

// 查询应用列表
export const getApplicationList = () => {
  return request.get<Result<Application[]>>('/basebackend-system-api/api/system/application/list')
}

// 查询启用的应用列表
export const getEnabledApplications = () => {
  return request.get<Result<Application[]>>('/basebackend-system-api/api/system/application/enabled')
}

// 根据ID查询应用
export const getApplicationById = (id: string) => {
  return request.get<Result<Application>>(`/basebackend-system-api/api/system/application/${id}`)
}

// 根据编码查询应用
export const getApplicationByCode = (appCode: string) => {
  return request.get<Result<Application>>(`/basebackend-system-api/api/system/application/code/${appCode}`)
}

// 创建应用
export const createApplication = (data: Application) => {
  return request.post<Result<void>>('/basebackend-system-api/api/system/application', data)
}

// 更新应用
export const updateApplication = (data: Application) => {
  return request.put<Result<void>>('/basebackend-system-api/api/system/application', data)
}

// 删除应用
export const deleteApplication = (id: string) => {
  return request.delete<Result<void>>(`/basebackend-system-api/api/system/application/${id}`)
}

// 修改应用状态
export const updateApplicationStatus = (id: string, status: number) => {
  return request.put<Result<void>>(`/basebackend-system-api/api/system/application/${id}/status/${status}`)
}

// ==================== 应用资源管理 ====================

// 查询应用的资源树
export const getResourceTree = (appId: string) => {
  return request.get<Result<ApplicationResource[]>>(`/basebackend-system-api/api/system/application/resource/tree/${appId}`)
}

// 查询用户的资源树
export const getUserResourceTree = (appId: string) => {
  return request.get<Result<ApplicationResource[]>>(`/basebackend-system-api/api/system/application/resource/user/tree/${appId}`)
}

// 根据ID查询资源
export const getResourceById = (id: string) => {
  return request.get<Result<ApplicationResource>>(`/basebackend-system-api/api/system/application/resource/${id}`)
}

// 创建资源
export const createResource = (data: ApplicationResource) => {
  return request.post<Result<void>>('/basebackend-system-api/api/system/application/resource', data)
}

// 更新资源
export const updateResource = (data: ApplicationResource) => {
  return request.put<Result<void>>('/basebackend-system-api/api/system/application/resource', data)
}

// 删除资源
export const deleteResource = (id: string) => {
  return request.delete<Result<void>>(`/basebackend-system-api/api/system/application/resource/${id}`)
}

// 查询角色的资源ID列表
export const getResourceIdsByRoleId = (roleId: string) => {
  return request.get<Result<string[]>>(`/basebackend-system-api/api/system/application/resource/role/${roleId}`)
}

// 分配角色资源
export const assignRoleResources = (roleId: string, resourceIds: string[]) => {
  return request.post<Result<void>>(`/basebackend-system-api/api/system/application/resource/role/${roleId}/assign`, resourceIds)
}

// 获取当前登录用户的菜单树
export const getCurrentUserMenuTree = () => {
  return request.get<Menu[]>('/basebackend-system-api/api/system/application/resource/current-user')
}
