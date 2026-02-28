import request from '@/api/request';
import type { UserDTO, UserCreateDTO, UserQueryDTO, PageResult } from '@/types';

/**
 * 用户管理 API
 * 对接后端 /api/user/users/* 接口
 */
export const userApi = {
  /** 分页查询用户列表 */
  page: (params: UserQueryDTO & { current: number; size: number }): Promise<PageResult<UserDTO>> =>
    request.get('/api/user/users', { params }),

  /** 根据 ID 获取用户详情 */
  getById: (id: number): Promise<UserDTO> =>
    request.get(`/api/user/users/${id}`),

  /** 创建用户 */
  create: (data: UserCreateDTO): Promise<void> =>
    request.post('/api/user/users', data),

  /** 更新用户 */
  update: (id: number, data: Partial<UserDTO>): Promise<void> =>
    request.put(`/api/user/users/${id}`, data),

  /** 删除用户 */
  delete: (id: number): Promise<void> =>
    request.delete(`/api/user/users/${id}`),

  /** 重置用户密码 */
  resetPassword: (id: number, newPassword: string): Promise<void> =>
    request.put(`/api/user/users/${id}/reset-password`, null, { params: { newPassword } }),

  /** 分配角色 */
  assignRoles: (id: number, roleIds: number[]): Promise<void> =>
    request.put(`/api/user/users/${id}/roles`, roleIds),

  /** 切换用户状态 */
  changeStatus: (id: number, status: number): Promise<void> =>
    request.put(`/api/user/users/${id}/status`, null, { params: { status } }),
};
