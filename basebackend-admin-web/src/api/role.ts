import request from '@/utils/request'
import { Role, Result, PageResult, User } from '@/types'

// 分页查询角色列表
export const getRolePage = (params: any) => {
  return request.get<PageResult<Role>>('/admin/roles', { params })
}

// 根据ID查询角色
export const getRoleById = (id: string) => {
  return request.get<Result<Role>>(`/admin/roles/${id}`)
}

// 获取角色树
export const getRoleTree = (appId?: string) => {
  return request.get<Result<Role[]>>('/admin/roles/tree', {
    params: { appId },
  })
}

// 创建角色
export const createRole = (data: Role) => {
  return request.post<Result<string>>('/admin/roles', data)
}

// 更新角色
export const updateRole = (id: string, data: Role) => {
  return request.put<Result<string>>(`/admin/roles/${id}`, data)
}

// 删除角色
export const deleteRole = (id: string) => {
  return request.delete<Result<string>>(`/admin/roles/${id}`)
}

// 分配菜单
export const assignRoleMenus = (id: string, menuIds: string[]) => {
  return request.put<Result<string>>(`/admin/roles/${id}/menus`, menuIds)
}

// 分配权限
export const assignRolePermissions = (id: string, permissionIds: string[]) => {
  return request.put<Result<string>>(`/admin/roles/${id}/permissions`, permissionIds)
}

// 分配应用资源
export const assignRoleResources = (id: string, resourceIds: string[]) => {
  return request.put<Result<string>>(`/admin/roles/${id}/resources`, resourceIds)
}

// 获取角色菜单列表
export const getRoleMenus = (id: string) => {
  return request.get<Result<string[]>>(`/admin/roles/${id}/menus`)
}

// 获取角色权限列表
export const getRolePermissions = (id: string) => {
  return request.get<Result<string[]>>(`/admin/roles/${id}/permissions`)
}

// 获取角色资源列表
export const getRoleResources = (id: string) => {
  return request.get<Result<string[]>>(`/admin/roles/${id}/resources`)
}

// 获取角色用户列表
export const getRoleUsers = (id: string, username?: string) => {
  return request.get<Result<User[]>>(`/admin/roles/${id}/users`, {
    params: { username },
  })
}

// 批量关联用户到角色
export const assignUsersToRole = (id: string, userIds: string[]) => {
  return request.post<Result<string>>(`/admin/roles/${id}/users`, userIds)
}

// 取消用户角色关联
export const removeUserFromRole = (roleId: string, userId: string) => {
  return request.delete<Result<string>>(`/admin/roles/${roleId}/users/${userId}`)
}

// 配置列表操作权限
export const configureListOperations = (
  id: string,
  data: { resourceType: string; operationIds: string[] }
) => {
  return request.put<Result<string>>(`/admin/roles/${id}/list-operations`, data)
}

// 获取角色列表操作权限
export const getRoleListOperations = (id: string, resourceType: string) => {
  return request.get<Result<string[]>>(`/admin/roles/${id}/list-operations`, {
    params: { resourceType },
  })
}

// 配置数据权限
export const configureDataPermissions = (id: string, filterRule: string) => {
  return request.put<Result<string>>(`/admin/roles/${id}/data-permissions`, {
    filterRule,
  })
}

// 检查角色名称唯一性
export const checkRoleNameUnique = (roleName: string, roleId?: string) => {
  return request.get<Result<boolean>>('/admin/roles/check-role-name', {
    params: { roleName, roleId },
  })
}

// 检查角色标识唯一性
export const checkRoleKeyUnique = (roleKey: string, roleId?: string) => {
  return request.get<Result<boolean>>('/admin/roles/check-role-key', {
    params: { roleKey, roleId },
  })
}
