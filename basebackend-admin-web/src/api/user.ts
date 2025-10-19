import request from '@/utils/request'
import { User, Result, PageResult } from '@/types'

// 分页查询用户列表
export const getUserPage = (params: any) => {
  return request.get<PageResult<User>>('/admin/users', { params })
}

// 根据ID查询用户
export const getUserById = (id: string) => {
  return request.get<Result<User>>(`/admin/users/${id}`)
}

// 创建用户
export const createUser = (data: User & { password: string }) => {
  return request.post<Result<string>>('/admin/users', data)
}

// 更新用户
export const updateUser = (id: string, data: User) => {
  return request.put<Result<string>>(`/admin/users/${id}`, data)
}

// 删除用户
export const deleteUser = (id: string) => {
  return request.delete<Result<string>>(`/admin/users/${id}`)
}

// 批量删除用户
export const deleteUserBatch = (ids: string[]) => {
  return request.delete<Result<string>>('/admin/users/batch', { data: ids })
}

// 重置密码
export const resetUserPassword = (id: string, newPassword: string) => {
  return request.put<Result<string>>(`/admin/users/${id}/reset-password`, null, {
    params: { newPassword },
  })
}

// 分配角色
export const assignUserRoles = (id: string, roleIds: string[]) => {
  return request.put<Result<string>>(`/admin/users/${id}/roles`, roleIds)
}

// 修改用户状态
export const changeUserStatus = (id: string, status: number) => {
  return request.put<Result<string>>(`/admin/users/${id}/status`, null, {
    params: { status },
  })
}

// 导出用户
export const exportUsers = (params: any) => {
  return request.get<Result<User[]>>('/admin/users/export', { params })
}

// 获取用户角色列表
export const getUserRoles = (id: string) => {
  return request.get<Result<string[]>>(`/admin/users/${id}/roles`)
}

// 检查用户名唯一性
export const checkUsernameUnique = (username: string, userId?: string) => {
  return request.get<Result<boolean>>('/admin/users/check-username', {
    params: { username, userId },
  })
}

// 检查邮箱唯一性
export const checkEmailUnique = (email: string, userId?: string) => {
  return request.get<Result<boolean>>('/admin/users/check-email', {
    params: { email, userId },
  })
}

// 检查手机号唯一性
export const checkPhoneUnique = (phone: string, userId?: string) => {
  return request.get<Result<boolean>>('/admin/users/check-phone', {
    params: { phone, userId },
  })
}
