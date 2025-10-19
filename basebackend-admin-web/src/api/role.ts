import request from '@/utils/request'
import { Role, Result, PageResult } from '@/types'

// 分页查询角色列表
export const getRolePage = (params: any) => {
  return request.get<PageResult<Role>>('/admin/roles', { params })
}

// 根据ID查询角色
export const getRoleById = (id: string) => {
  return request.get<Result<Role>>(`/admin/roles/${id}`)
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

// 获取角色菜单列表
export const getRoleMenus = (id: string) => {
  return request.get<Result<string[]>>(`/admin/roles/${id}/menus`)
}

// 获取角色权限列表
export const getRolePermissions = (id: string) => {
  return request.get<Result<string[]>>(`/admin/roles/${id}/permissions`)
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
